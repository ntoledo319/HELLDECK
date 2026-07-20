// Confession or Cap (spec 5.4) — every rule gets a test (Part 11, task D-123).
// The module rides the core protocol: $phase INPUT PICK (hand of 3, confessor-only)
// -> $deal ceremony on the CHOSEN sin (subject pre-view + burn window, dedup
// writeback for the chosen card only) -> CORE_DEALT -> blocking LOCK (paused
// deadline, 4.7 terminals) -> PERFORM (REST ends early) -> JURY (skippable)
// -> core-held REVEAL -> CORE_REVEAL_DONE. The harness's `apply` mirrors the
// core's directive handling; the last describe block drives full circles
// through reduce() to weld the two together.
import { beforeEach, describe, expect, it } from 'vitest';
import type {
  CardBase,
  CircleSpec,
  Effect,
  ModuleDirectives,
  PhaseDirective,
  Player,
  Role,
  RoomState,
  SpotlightResolution,
  Tier,
} from '../src/types.js';
import type { GameCtx, GameStep } from '../src/games/module.js';
import { CORE_DEALT, CORE_REVEAL_DONE, CORE_SPOTLIGHT_DONE } from '../src/games/module.js';
import { rng } from '../src/rng.js';
import { beginDeal, completeDeal } from '../src/deal.js';
import { beginSpotlight, completeSpotlight, handoffSpotlight, spotlightResolution } from '../src/spotlight.js';
import { blockingGraceTimer, blockingPitTimer, registerModule } from '../src/engine.js';
import {
  confessionModule,
  DEFAULT_CONFESSION_DECK,
  HAND_SIZE,
  JURY_MS,
  lockKey,
  PERFORM_MS,
  PICK_MS,
  pickTimerId,
  performTimerId,
  juryTimerId,
  setConfessionDeck,
  type ConfessionJuryView,
  type ConfessionLockView,
  type ConfessionPickView,
  type ConfessionResolution,
  type ConfessionRevealView,
  type ConfessionState,
  type ConfessionView,
  type JuryVote,
} from '../src/games/confession.js';
import { Driver, mkNightRoom, mkPlayers, mkSpec } from './helpers.js';

// ===== harness (mirrors roast.test.ts) =====
const NOW = 1_000_000;
const CIRCLE: CircleSpec = { game: 'confession', loops: 2, finale: false, outward: false, rung: 3, bargain: false };

function mkPlayer(id: string, role: Role, seat: number, heatCeiling: Tier = 5): Player {
  return {
    id,
    name: id,
    avatar: seat % 16,
    role,
    seat,
    connected: true,
    lastSeenAt: 0,
    heatCeiling,
    ceilingSet: true,
    attested18: true,
    brimstones: 2,
    score: 0,
    spotlightCount: 0,
    freshMeat: false,
  };
}

function mkState(nPlayers: number, nImps = 0, over: Partial<RoomState> = {}): RoomState {
  const players = Array.from({ length: nPlayers }, (_, i) => mkPlayer(`P${i}`, i === 0 ? 'host' : 'player', i));
  const imps = Array.from({ length: nImps }, (_, i) => mkPlayer(`I${i}`, 'imp', nPlayers + i));
  return {
    code: 'HELL',
    createdAt: 0,
    config: null,
    players: [...players, ...imps],
    phase: { k: 'DEAL', circle: 0 },
    arc: [CIRCLE],
    circleIdx: 0,
    gameState: null,
    usedCardIds: [],
    usedSkeletons: {},
    devilsBargain: null,
    epoch: 1,
    entitled: true,
    telemetry: [],
    deal: null,
    moduleTimers: {},
    lastFireAt: null,
    nightStats: {},
    judgment: null,
    circleStartScores: {},
    volunteers: [],
    ...over,
  };
}

interface Sim {
  state: RoomState;
  rand: () => number;
  last: GameStep;
  effects: Effect[]; // accumulated, incl. intermediate $deal-chain steps
  phase: PhaseDirective | null; // last $phase directive the module emitted
}

function mkCtx(state: RoomState, rand: () => number, spotlight?: SpotlightResolution): GameCtx {
  const players = state.players.filter((p) => p.role !== 'imp');
  const imps = state.players.filter((p) => p.role === 'imp');
  const circle = state.arc[0] as CircleSpec;
  return { state, circle, circleIdx: 0, players, imps, now: NOW, rand, finaleMult: 1, volunteers: [], ...(spotlight ? { spotlight } : {}) };
}

/** Mirror of the core's applyStep for the directives confession uses ($deal chain + $phase strip). */
function apply(state: RoomState, rand: () => number, step: GameStep, acc: Effect[] = []): Sim {
  const gs = step.gameState;
  const effects = [...acc, ...step.effects];
  if (gs !== null && typeof gs === 'object' && ('$phase' in gs || '$deal' in gs || '$spotlight' in gs)) {
    const { $phase, $deal, $spotlight, ...rest } = gs as Record<string, unknown> & ModuleDirectives;
    if ($spotlight) {
      const players = state.players.filter((p) => p.role !== 'imp');
      const begun = beginSpotlight($spotlight, players, [], rand, 'test', NOW);
      const handed = handoffSpotlight(begun.spotlight, players);
      const done = completeSpotlight(handed.spotlight);
      const resolution = spotlightResolution(done);
      const nextState: RoomState = { ...state, gameState: rest, spotlight: done };
      return apply(nextState, rand, confessionModule.timer(mkCtx(nextState, rand, resolution), CORE_SPOTLIGHT_DONE), [
        ...effects,
        ...begun.effects,
        ...handed.effects,
      ]);
    }
    if ($deal) {
      // Core would run the 4.5 ceremony; here it completes instantly with the same writeback.
      const begun = beginDeal($deal, 'deal:test', NOW);
      const done = completeDeal(begun.deal);
      const nextState: RoomState = {
        ...state,
        gameState: rest,
        deal: done.deal,
        usedCardIds: [...state.usedCardIds, ...done.usedCardIds],
      };
      return apply(nextState, rand, confessionModule.timer(mkCtx(nextState, rand), CORE_DEALT), effects);
    }
    const nextState: RoomState = { ...state, gameState: rest };
    return { state: nextState, rand, last: step, effects, phase: ($phase as PhaseDirective | undefined) ?? null };
  }
  return { state: { ...state, gameState: gs }, rand, last: step, effects, phase: null };
}

function begin(nPlayers: number, nImps = 0, seed = 'test', over: Partial<RoomState> = {}): Sim {
  const state = mkState(nPlayers, nImps, over);
  const rand = rng(seed);
  return apply(state, rand, confessionModule.start(mkCtx(state, rand)));
}
function input(sim: Sim, who: string, payload: unknown): Sim {
  return apply(sim.state, sim.rand, confessionModule.input(mkCtx(sim.state, sim.rand), who, payload));
}
function fire(sim: Sim, timerId: string): Sim {
  return apply(sim.state, sim.rand, confessionModule.timer(mkCtx(sim.state, sim.rand), timerId));
}
function control(sim: Sim, who: string, kind: 'REST' | 'SKIPEM' | 'FIFTH' | 'VOID'): Sim {
  return apply(sim.state, sim.rand, confessionModule.control(mkCtx(sim.state, sim.rand), who, kind));
}
function st(sim: Sim): ConfessionState {
  return sim.state.gameState as ConfessionState;
}
function confessorOf(sim: Sim): string {
  return st(sim).confessorId;
}
function juryIds(sim: Sim): string[] {
  return sim.state.players.filter((p) => p.id !== confessorOf(sim)).map((p) => p.id);
}
function lastRes(sim: Sim): ConfessionResolution {
  const r = st(sim).resolutions[st(sim).resolutions.length - 1];
  if (!r) throw new Error('no resolution yet');
  return r;
}
function viewAs(sim: Sim, viewer: string): ConfessionView {
  const v = confessionModule.view(mkCtx(sim.state, sim.rand), viewer);
  if (!v) throw new Error('no view');
  return v;
}
function pickSin(sim: Sim, idx: number): Sim {
  return input(sim, confessorOf(sim), { pick: idx });
}
function lockTruth(sim: Sim, truth: boolean): Sim {
  return input(sim, confessorOf(sim), { truth });
}
function vote(sim: Sim, who: string, v: JuryVote): Sim {
  return input(sim, who, { vote: v });
}
/** PICK -> (ceremony) -> LOCK -> PERFORM -> JURY, ready for ballots. */
function toJury(sim: Sim, truth: boolean): Sim {
  let s = pickSin(sim, 0);
  s = lockTruth(s, truth);
  return fire(s, performTimerId(0, st(s).loop));
}
/** Core hold ends (DESCEND / fire-decay): CORE_REVEAL_DONE -> next confessor or done. */
function advance(sim: Sim): Sim {
  return fire(sim, CORE_REVEAL_DONE);
}
/** Run a full loop scorelessly (auto-pick, lock, timers) and descend into the next. */
function throughLoop(sim: Sim, truth = true): Sim {
  let s = toJury(sim, truth);
  s = fire(s, juryTimerId(0, st(s).loop));
  return advance(s);
}
function card(exposure: Tier, n: number): CardBase {
  return {
    id: `confession_test_${exposure}_${n}`,
    deck: 'confession',
    text: `test sin E${exposure} #${n}`,
    exposure,
    chaos: 3,
    register: 'deadpan',
    skeleton: `test-e${exposure}-${n}`,
  };
}
/** Deterministic seed hunt: first seed whose confessor is NOT the host (for lapse paths). */
function beginNonHostConfessor(nPlayers: number): Sim {
  for (const seed of ['a', 'b', 'c', 'd', 'e', 'f', 'g', 'h']) {
    const sim = begin(nPlayers, 0, seed);
    if (confessorOf(sim) !== 'P0') return sim;
  }
  throw new Error('no seed produced a non-host confessor');
}

beforeEach(() => setConfessionDeck(DEFAULT_CONFESSION_DECK));

// ===== DEAL-3: the hand, the pick, the ceremony =====
describe('deal-3 & pick', () => {
  it('start opens PICK: $phase INPUT + the 20s pick timer, hand of 3 distinct legal sins', () => {
    const sim = begin(6);
    expect(st(sim).sub).toBe('PICK');
    expect(st(sim).loop).toBe(0);
    expect(st(sim).loops).toBe(2);
    expect(sim.phase).toEqual({ k: 'INPUT', sub: 'PICK', deadline: NOW + PICK_MS });
    expect(sim.effects).toContainEqual({ k: 'SCHEDULE', timerId: pickTimerId(0, 0), atMs: NOW + PICK_MS });
    const hand = st(sim).hand;
    expect(hand).toHaveLength(HAND_SIZE);
    expect(new Set(hand.map((c) => c.id)).size).toBe(HAND_SIZE);
    expect(sim.state.usedCardIds).toEqual([]); // nothing is marked used at PICK — nothing was dealt
  });

  it('picking a sin runs the $deal ceremony: chosen card marked used, unchosen return to the pool', () => {
    const sim = begin(6);
    const hand = st(sim).hand;
    const chosen = hand[1] as CardBase;
    const next = pickSin(sim, 1);
    expect(st(next).sub).toBe('LOCK');
    expect(st(next).card?.id).toBe(chosen.id);
    expect(next.state.usedCardIds).toEqual([chosen.id]); // chosen ONLY — the 5.4 acceptance
    for (const c of hand) {
      if (c.id !== chosen.id) expect(next.state.usedCardIds).not.toContain(c.id);
    }
    expect(next.effects).toContainEqual({ k: 'CANCEL', timerId: pickTimerId(0, 0) });
  });

  it('auto-picks sin #1 at the pick deadline', () => {
    const sim = begin(5);
    const first = st(sim).hand[0] as CardBase;
    const next = fire(sim, pickTimerId(0, 0));
    expect(st(next).sub).toBe('LOCK');
    expect(st(next).card?.id).toBe(first.id);
    expect(next.state.usedCardIds).toEqual([first.id]);
  });

  it('the ceremony hands the confessor the burn window: $deal names them subject', () => {
    const sim = begin(5);
    const c = confessorOf(sim);
    const step = confessionModule.input(mkCtx(sim.state, sim.rand), c, { pick: 0 });
    const dirs = step.gameState as Record<string, unknown> & ModuleDirectives;
    expect(dirs.$deal?.subjectId).toBe(c); // private pre-view + 10s burn window (4.5)
    expect(dirs.$deal?.backup.id).not.toBe(dirs.$deal?.primary.id); // reserved swap, zero-cost burn
  });

  it('unchosen sins are re-dealable later the same night', () => {
    setConfessionDeck([card(2, 1), card(2, 2), card(2, 3), card(2, 4)]);
    let sim = begin(5);
    const hand1 = st(sim).hand.map((c) => c.id);
    sim = pickSin(sim, 1);
    const chosen = st(sim).card?.id as string;
    sim = lockTruth(sim, true);
    sim = fire(sim, performTimerId(0, 0));
    sim = fire(sim, juryTimerId(0, 0));
    sim = advance(sim); // -> loop 2 PICK
    const hand2 = st(sim).hand.map((c) => c.id);
    expect(hand2).not.toContain(chosen); // night-dedup: the dealt sin is spent
    for (const id of hand1) {
      if (id !== chosen) expect(hand2).toContain(id); // unchosen came straight back
    }
  });

  it('only the confessor picks; malformed and out-of-range picks are inert', () => {
    const sim = begin(5);
    const c = confessorOf(sim);
    const stranger = juryIds(sim)[0] as string;
    for (const [who, payload] of [
      [stranger, { pick: 0 }],
      ['GHOST', { pick: 0 }],
      [c, { pick: HAND_SIZE }],
      [c, { pick: -1 }],
      [c, { pick: 0.5 }],
      [c, { pick: '0' }],
      [c, null],
      [c, 42],
    ] as const) {
      const next = input(sim, who as string, payload);
      expect(st(next).sub).toBe('PICK');
      expect(st(next)).toBe(st(sim)); // provably inert
    }
  });

  it("consent 4.4: no sin in the hand runs hotter than the confessor's ceiling", () => {
    setConfessionDeck([card(2, 1), card(2, 2), card(2, 3), card(5, 1), card(5, 2), card(5, 3)]);
    const state = mkState(5);
    state.players = state.players.map((p) => ({ ...p, heatCeiling: 2 as Tier }));
    const rand = rng('consent');
    for (let i = 0; i < 6; i++) {
      const sim = apply(state, rand, confessionModule.start(mkCtx(state, rand)));
      for (const c of st(sim).hand) expect(c.exposure).toBeLessThanOrEqual(2);
    }
  });

  it('a thin deck deals a thin hand rather than duplicating sins', () => {
    setConfessionDeck([card(2, 1), card(2, 2)]);
    const sim = begin(5);
    expect(st(sim).hand).toHaveLength(2);
    expect(new Set(st(sim).hand.map((c) => c.id)).size).toBe(2);
  });

  it('two confessors per circle, never the same sinner twice, then done', () => {
    let sim = begin(6);
    const c1 = confessorOf(sim);
    sim = throughLoop(sim);
    expect(st(sim).sub).toBe('PICK');
    expect(st(sim).loop).toBe(1);
    const c2 = confessorOf(sim);
    expect(c2).not.toBe(c1); // spotlight fairness: loop 2 excludes loop 1's confessor
    sim = toJury(sim, false);
    sim = fire(sim, juryTimerId(0, 1));
    expect(sim.last.done).not.toBe(true); // done only after the final hold
    sim = advance(sim);
    expect(sim.last.done).toBe(true);
    expect(st(sim).resolutions).toHaveLength(2);
  });

  it('setConfessionDeck rejects an empty deck', () => {
    expect(() => setConfessionDeck([])).toThrow();
  });
});

// ===== the truth lock (blocking 4.7) =====
describe('truth lock', () => {
  it('LOCK pauses the clock: $phase INPUT deadline null, grace + pit timers armed', () => {
    const sim = pickSin(begin(5), 0);
    expect(st(sim).sub).toBe('LOCK');
    expect(sim.phase).toEqual({ k: 'INPUT', sub: 'LOCK', deadline: null }); // provably paused
    const key = lockKey(0, 0);
    expect(sim.effects).toContainEqual({ k: 'SCHEDULE', timerId: blockingGraceTimer(key), atMs: NOW + 12_000 });
    expect(sim.effects).toContainEqual({ k: 'SCHEDULE', timerId: blockingPitTimer(key), atMs: NOW + 30_000 });
  });

  it('locking TRUE/FALSE opens the 45s PERFORM and cancels the shame machinery', () => {
    const sim = lockTruth(pickSin(begin(5), 0), true);
    expect(st(sim).sub).toBe('PERFORM');
    expect(st(sim).truthLock).toBe(true);
    expect(sim.phase).toEqual({ k: 'INPUT', sub: 'PERFORM', deadline: NOW + PERFORM_MS });
    expect(sim.effects).toContainEqual({ k: 'CANCEL', timerId: blockingGraceTimer(lockKey(0, 0)) });
    expect(sim.effects).toContainEqual({ k: 'CANCEL', timerId: blockingPitTimer(lockKey(0, 0)) });
    expect(sim.effects).toContainEqual({ k: 'SCHEDULE', timerId: performTimerId(0, 0), atMs: NOW + PERFORM_MS });
  });

  it('12s grace -> public WAITING_ON the confessor', () => {
    const sim = fire(pickSin(begin(5), 0), blockingGraceTimer(lockKey(0, 0)));
    expect(sim.phase).toEqual({ k: 'WAITING_ON', who: confessorOf(sim), since: NOW });
    expect(st(sim).sub).toBe('LOCK'); // still their move; the clock is still dead
  });

  it('30s -> pit vote opens; >=60% PIT voids the loop with the lock unstamped', () => {
    let sim = fire(pickSin(begin(4), 0), blockingPitTimer(lockKey(0, 0)));
    const jury = juryIds(sim);
    expect((viewAs(sim, jury[0] as string) as ConfessionLockView).pitOpen).toBe(true);
    sim = input(sim, jury[0] as string, { pit: 'pit' });
    expect(st(sim).sub).toBe('LOCK'); // 1/3 is not a mob yet
    sim = input(sim, jury[1] as string, { pit: 'pit' }); // 2/3 >= 60%
    expect(st(sim).sub).toBe('REVEAL');
    const res = lastRes(sim);
    expect(res.voided).toBe(true);
    expect(res.truth).toBeNull(); // no human locked a value — none exists (D-115)
    expect(st(sim).truthLock).toBeNull();
    expect(sim.last.scores).toEqual({});
  });

  it('PLEAD THE FIFTH: confessor only, loop dies scoreless, fifth sting, truth never exists', () => {
    let sim = pickSin(begin(5), 0);
    const bystander = juryIds(sim)[0] as string;
    expect(st(control(sim, bystander, 'FIFTH'))).toBe(st(sim)); // not their right
    sim = control(sim, confessorOf(sim), 'FIFTH');
    expect(st(sim).sub).toBe('REVEAL');
    expect(sim.phase).toEqual({ k: 'REVEAL' }); // the void still holds — the room hears the sting
    expect(sim.effects).toContainEqual({ k: 'AUDIO', sting: 'fifth' });
    const res = lastRes(sim);
    expect(res.voided).toBe(true);
    expect(res.truth).toBeNull();
    expect(res.verdict).toBeNull();
    expect(sim.last.scores).toEqual({});
    // the loop is spent: advancing deals the NEXT confessor
    const next = advance(sim);
    expect(st(next).loop).toBe(1);
  });

  it('host VOID works at LOCK, PERFORM and JURY; the truth dies with the loop', () => {
    const atLock = control(pickSin(begin(5), 0), 'P0', 'VOID');
    expect(lastRes(atLock).voided).toBe(true);
    expect(lastRes(atLock).truth).toBeNull();

    const atPerform = control(lockTruth(pickSin(begin(5), 0), true), 'P0', 'VOID');
    expect(lastRes(atPerform).voided).toBe(true);
    expect(lastRes(atPerform).truth).toBeNull(); // even a LOCKED truth is never stamped on a void
    expect(st(atPerform).truthLock).toBeNull(); // scrubbed server-side too

    const atJury = control(toJury(begin(5), true), 'P0', 'VOID');
    expect(lastRes(atJury).voided).toBe(true);
    expect(atJury.effects).toContainEqual({ k: 'CANCEL', timerId: juryTimerId(0, 0) });
  });

  it('seat lapse (non-host VOID convention): the witness fled, the loop dies', () => {
    let sim = beginNonHostConfessor(5);
    const c = confessorOf(sim);
    sim = pickSin(sim, 0);
    const out = control(sim, c, 'VOID');
    expect(st(out).sub).toBe('REVEAL');
    expect(lastRes(out).voided).toBe(true);
    expect(out.effects).toContainEqual({ k: 'AUDIO', sting: 'fled' });
  });

  it('a lapsed juror voids nothing; a lapsed confessor mid-JURY voids nothing (testimony is in)', () => {
    let sim = beginNonHostConfessor(5);
    const c = confessorOf(sim);
    sim = toJury(sim, true);
    const juror = juryIds(sim).find((j) => j !== 'P0') as string;
    expect(st(control(sim, juror, 'VOID'))).toBe(st(sim));
    expect(st(control(sim, c, 'VOID'))).toBe(st(sim)); // the jury can convict a ghost
  });

  it('only the confessor can lock; bad payloads and double-locks are inert', () => {
    const sim = pickSin(begin(5), 0);
    const juror = juryIds(sim)[0] as string;
    expect(st(input(sim, juror, { truth: true }))).toBe(st(sim));
    for (const bad of [null, 7, { truth: 'true' }, { truth: 1 }, {}]) {
      expect(st(input(sim, confessorOf(sim), bad))).toBe(st(sim));
    }
    const locked = lockTruth(sim, false);
    expect(st(locked).truthLock).toBe(false);
    expect(st(input(locked, confessorOf(locked), { truth: true }))).toBe(st(locked)); // PERFORM: no re-lock
  });
});

// ===== perform & I REST MY CASE =====
describe('perform', () => {
  it('the 45s timer closes the case into the 12s JURY', () => {
    const sim = fire(lockTruth(pickSin(begin(5), 0), true), performTimerId(0, 0));
    expect(st(sim).sub).toBe('JURY');
    expect(sim.phase).toEqual({ k: 'INPUT', sub: 'JURY', deadline: NOW + JURY_MS });
    expect(sim.effects).toContainEqual({ k: 'SCHEDULE', timerId: juryTimerId(0, 0), atMs: NOW + JURY_MS });
  });

  it('I REST MY CASE ends the performance early: timer cancelled, jury seated', () => {
    const performing = lockTruth(pickSin(begin(5), 0), true);
    const sim = control(performing, confessorOf(performing), 'REST');
    expect(st(sim).sub).toBe('JURY');
    expect(sim.effects).toContainEqual({ k: 'CANCEL', timerId: performTimerId(0, 0) });
    expect(sim.phase).toEqual({ k: 'INPUT', sub: 'JURY', deadline: NOW + JURY_MS });
  });

  it('REST belongs to the performer mid-performance — nobody else, nowhere else', () => {
    const performing = lockTruth(pickSin(begin(5), 0), true);
    const juror = juryIds(performing)[0] as string;
    expect(st(control(performing, juror, 'REST'))).toBe(st(performing));
    const atPick = begin(5);
    expect(st(control(atPick, confessorOf(atPick), 'REST'))).toBe(st(atPick));
    const atJury = toJury(begin(5), true);
    expect(st(control(atJury, confessorOf(atJury), 'REST'))).toBe(st(atJury));
  });
});

// ===== jury & resolution =====
describe('jury vote & resolution', () => {
  it('ballots: re-vote allowed, confessor excluded, ghosts and junk ignored', () => {
    let sim = toJury(begin(5), true);
    const jury = juryIds(sim);
    const j0 = jury[0] as string;
    sim = vote(sim, j0, 'believe');
    sim = vote(sim, j0, 'cap'); // last ballot wins
    expect(st(sim).votes).toEqual({ [j0]: 'cap' });
    expect((viewAs(sim, j0) as ConfessionJuryView).votedCount).toBe(1);
    expect(st(input(sim, confessorOf(sim), { vote: 'believe' }))).toBe(st(sim));
    expect(st(input(sim, 'GHOST', { vote: 'cap' }))).toBe(st(sim));
    expect(st(input(sim, j0, { vote: 'BELIEVE' }))).toBe(st(sim));
  });

  it('resolves early when the whole jury is in, cancelling the vote timer', () => {
    let sim = toJury(begin(4), true);
    for (const j of juryIds(sim)) sim = vote(sim, j, 'believe');
    expect(st(sim).sub).toBe('REVEAL');
    expect(sim.effects).toContainEqual({ k: 'CANCEL', timerId: juryTimerId(0, 0) });
    expect(sim.phase).toEqual({ k: 'REVEAL' }); // core stamps holdSince + arms flip/softcap/decay
  });

  it.each([4, 6, 9])('N=%i: jury majority wrong -> FOOLED, confessor +3, correct voters +1(+1)', (n) => {
    let sim = toJury(begin(n), true); // truth locked TRUE -> correct = believe
    const c = confessorOf(sim);
    const jury = juryIds(sim);
    const wrong = jury.slice(0, Math.floor(jury.length / 2) + 1);
    const right = jury.slice(wrong.length);
    for (const j of wrong) sim = vote(sim, j, 'cap');
    for (const j of right) sim = vote(sim, j, 'believe'); // final ballot completes the jury
    expect(st(sim).sub).toBe('REVEAL');
    const res = lastRes(sim);
    expect(res.verdict).toBe('FOOLED');
    expect(res.truth).toBe(true);
    expect(res.spread).toEqual({ believe: right.length, cap: wrong.length });
    const expected: Record<string, number> = { [c]: 3 }; // the liar walks with the pot — and NO participation: 4.6 pays votes cast
    for (const j of wrong) expected[j] = 1; // participation only
    for (const j of right) expected[j] = 2; // +1 correct call, +1 participation
    expect(sim.last.scores).toEqual(expected);
  });

  it('jury majority right -> CAUGHT: confessor gets nothing', () => {
    let sim = toJury(begin(6), false); // truth locked FALSE -> correct = cap
    const c = confessorOf(sim);
    const jury = juryIds(sim);
    for (const [i, j] of jury.entries()) sim = vote(sim, j, i < 4 ? 'cap' : 'believe');
    const res = lastRes(sim);
    expect(res.verdict).toBe('CAUGHT');
    expect(res.truth).toBe(false);
    expect(sim.last.scores?.[c]).toBeUndefined(); // get caught lying, walk with nothing
    expect(sim.last.scores?.[jury[0] as string]).toBe(2);
    expect(sim.last.scores?.[jury[4] as string]).toBe(1);
  });

  it('HUNG JURY — THE LIAR WALKS: a tie pays the confessor like a clean con', () => {
    let sim = toJury(begin(5), true);
    const c = confessorOf(sim);
    const jury = juryIds(sim); // 4 jurors: 2-2
    sim = vote(sim, jury[0] as string, 'believe');
    sim = vote(sim, jury[1] as string, 'believe');
    sim = vote(sim, jury[2] as string, 'cap');
    sim = vote(sim, jury[3] as string, 'cap');
    const res = lastRes(sim);
    expect(res.verdict).toBe('HUNG');
    expect(sim.last.scores?.[c]).toBe(3);
    // correct calls still pay even on a hung jury
    expect(sim.last.scores?.[jury[0] as string]).toBe(2);
    expect(sim.last.scores?.[jury[2] as string]).toBe(1);
  });

  it('an empty jury box hangs too: nobody convicted anybody, the liar walks', () => {
    const sim = fire(toJury(begin(5), true), juryTimerId(0, 0));
    const res = lastRes(sim);
    expect(res.verdict).toBe('HUNG');
    expect(res.spread).toEqual({ believe: 0, cap: 0 });
    expect(sim.last.scores).toEqual({ [confessorOf(sim)]: 3 });
  });

  it('participation pays +1 once per CIRCLE, not once per loop', () => {
    let sim = begin(6);
    const c1 = confessorOf(sim);
    sim = toJury(sim, true);
    const jury1 = juryIds(sim);
    for (const j of jury1) sim = vote(sim, j, 'believe'); // CAUGHT: everyone correct
    expect(sim.last.scores?.[jury1[0] as string]).toBe(2); // +1 correct +1 participation
    sim = advance(sim);
    const c2 = confessorOf(sim);
    sim = toJury(sim, true);
    const repeat = juryIds(sim).find((j) => j !== c1 && j !== c2) as string;
    sim = vote(sim, repeat, 'believe');
    sim = fire(sim, juryTimerId(0, 1));
    expect(sim.last.scores?.[repeat]).toBe(1); // +1 correct only — participation already spent
  });

  it('auto-abstain at the deadline: only cast ballots count', () => {
    let sim = toJury(begin(6), true);
    const jury = juryIds(sim);
    sim = vote(sim, jury[0] as string, 'cap');
    sim = vote(sim, jury[1] as string, 'cap');
    sim = fire(sim, juryTimerId(0, 0));
    const res = lastRes(sim);
    expect(res.verdict).toBe('FOOLED'); // 2-0 wrong majority among those who showed up
    expect(res.spread).toEqual({ believe: 0, cap: 2 });
  });
});

// ===== imp half-votes (4.8) =====
describe('imp half-votes', () => {
  it('an imp ballot lands at 0.5 in the spread but pays FULL correct-call points', () => {
    let sim = toJury(begin(4, 1), true);
    sim = vote(sim, 'I0', 'believe');
    sim = fire(sim, juryTimerId(0, 0));
    expect(lastRes(sim).spread).toEqual({ believe: 0.5, cap: 0 });
    expect(sim.last.scores?.['I0']).toBe(2); // +1 correct +1 participation — weight shapes math, not payout
  });

  it('never BREAKS a full-vote tie: players 2-2 hangs no matter what the imp did', () => {
    let sim = toJury(begin(5, 1), true);
    const jury = juryIds(sim).filter((j) => j !== 'I0');
    sim = vote(sim, jury[0] as string, 'believe');
    sim = vote(sim, jury[1] as string, 'believe');
    sim = vote(sim, jury[2] as string, 'cap');
    sim = vote(sim, jury[3] as string, 'cap');
    sim = vote(sim, 'I0', 'cap'); // weighted 2.5 cap vs 2 believe — must not decide it
    expect(lastRes(sim).verdict).toBe('HUNG');
  });

  it('never CREATES a tie: an imp-manufactured weighted tie collapses to the players', () => {
    let sim = toJury(begin(4, 2), false); // correct = cap
    const jury = juryIds(sim).filter((j) => !j.startsWith('I'));
    sim = vote(sim, jury[0] as string, 'believe');
    sim = vote(sim, jury[1] as string, 'believe');
    sim = vote(sim, jury[2] as string, 'cap'); // players: believe 2, cap 1
    sim = vote(sim, 'I0', 'cap');
    sim = vote(sim, 'I1', 'cap'); // weighted: believe 2, cap 2 — imps must not hang it
    const res = lastRes(sim);
    expect(res.verdict).toBe('FOOLED'); // player majority (believe) was wrong
    expect(res.spread).toEqual({ believe: 2, cap: 2 });
  });

  it('imp weight DOES count when no tie is in play', () => {
    let sim = toJury(begin(4, 3), true); // correct = believe
    const jury = juryIds(sim).filter((j) => !j.startsWith('I'));
    sim = vote(sim, jury[0] as string, 'believe');
    sim = vote(sim, jury[1] as string, 'believe');
    sim = vote(sim, jury[2] as string, 'cap'); // players: believe 2, cap 1
    sim = vote(sim, 'I0', 'cap');
    sim = vote(sim, 'I1', 'cap');
    sim = vote(sim, 'I2', 'cap'); // weighted: cap 2.5 > believe 2 — clean lead, imps carry it
    const res = lastRes(sim);
    expect(res.verdict).toBe('FOOLED'); // majority (cap) wrong -> the imps got played
  });

  it('players silent, imps alone still reach a verdict', () => {
    let sim = toJury(begin(4, 2), false); // correct = cap
    sim = vote(sim, 'I0', 'believe');
    sim = vote(sim, 'I1', 'believe');
    sim = fire(sim, juryTimerId(0, 0));
    expect(lastRes(sim).verdict).toBe('FOOLED');
  });
});

// ===== view redaction (5.4 + 3.4) =====
describe('view redaction', () => {
  it("DEAL-3: the hand exists ONLY in the confessor's frame; the room doesn't even learn who", () => {
    const sim = begin(6);
    const c = confessorOf(sim);
    const hand = st(sim).hand;
    const mine = viewAs(sim, c) as ConfessionPickView;
    expect(mine.youAreConfessor).toBe(true);
    expect(mine.hand?.map((x) => x.id)).toEqual(hand.map((x) => x.id));
    for (const p of sim.state.players) {
      if (p.id === c) continue;
      const v = viewAs(sim, p.id) as ConfessionPickView;
      expect(v.youAreConfessor).toBe(false);
      const json = JSON.stringify(v);
      expect(json).not.toContain('hand');
      expect(json).not.toContain(`"${c}"`); // private assignment (4.5): no confessor id on the wire
      for (const cd of hand) {
        expect(json).not.toContain(cd.id);
        expect(json).not.toContain(cd.text);
      }
    }
  });

  it('the truth lock is in NO frame — any viewer, any phase — until the reveal (serialize-all)', () => {
    let sim = begin(6, 1);
    const viewers = sim.state.players.map((p) => p.id);
    const assertNoTruth = (s: Sim): void => {
      for (const who of viewers) {
        const json = JSON.stringify(confessionModule.view(mkCtx(s.state, s.rand), who) ?? null);
        expect(json).not.toContain('truthLock');
        expect(json).not.toContain('"truth"');
      }
    };
    assertNoTruth(sim); // PICK
    sim = pickSin(sim, 0);
    assertNoTruth(sim); // LOCK
    sim = lockTruth(sim, true);
    assertNoTruth(sim); // PERFORM — the lock exists in state, in no frame (not even the confessor's own)
    sim = fire(sim, performTimerId(0, 0));
    assertNoTruth(sim); // JURY
    sim = fire(sim, juryTimerId(0, 0));
    for (const who of viewers) {
      const v = viewAs(sim, who) as ConfessionRevealView;
      expect(v.sub).toBe('REVEAL');
      expect(v.truth).toBe(true); // the stamp goes public at last, everywhere at once
    }
  });

  it('during the DEAL ceremony the view is null — the pre-view travels privately', () => {
    const state = mkState(5, 0, {
      gameState: {
        sub: 'DEAL',
        loop: 0,
        loops: 2,
        confessorId: 'P1',
        hand: [],
        card: null,
        truthLock: null,
        votes: {},
        pickDeadline: 0,
        performDeadline: 0,
        juryDeadline: 0,
        blocking: null,
        resolutions: [],
        participation: [],
        confessorsUsed: ['P1'],
      } satisfies ConfessionState,
    });
    expect(confessionModule.view(mkCtx(state, rng('x')), 'P0')).toBeNull();
    expect(confessionModule.view(mkCtx(state, rng('x')), 'P1')).toBeNull(); // even the confessor waits for CORE_DEALT
  });

  it('jury ballots stay dark until reveal: counts + your OWN vote only', () => {
    let sim = toJury(begin(6), true);
    const jury = juryIds(sim);
    const [j0, j1] = [jury[0] as string, jury[1] as string];
    sim = vote(sim, j0, 'cap');
    sim = vote(sim, j1, 'believe');
    const mine = viewAs(sim, j0) as ConfessionJuryView;
    expect(mine.votedCount).toBe(2);
    expect(mine.youVoted).toBe('cap');
    const other = JSON.stringify(viewAs(sim, jury[2] as string));
    expect(other).toContain('"youVoted":null');
    expect(other).not.toContain('"votes"');
    expect(other).not.toContain('"cap"'); // no ballot value leaks into a stranger's frame
    expect(other).not.toContain('spread'); // the spread exists at REVEAL and nowhere earlier
    expect(other).not.toContain('"scores"');
  });

  it('pit ballots are equally private: your own choice only', () => {
    let sim = fire(pickSin(begin(5), 0), blockingPitTimer(lockKey(0, 0)));
    const jury = juryIds(sim);
    sim = input(sim, jury[0] as string, { pit: 'pit' });
    expect((viewAs(sim, jury[0] as string) as ConfessionLockView).youPitVoted).toBe('pit');
    const other = JSON.stringify(viewAs(sim, jury[1] as string));
    expect(other).toContain('"youPitVoted":null');
    expect(other).not.toContain('pitVotes');
  });

  it('reveal: spread is weighted counts — no voter-vote pair ever leaves the server', () => {
    let sim = toJury(begin(6), true);
    const jury = juryIds(sim);
    for (const [i, j] of jury.entries()) sim = vote(sim, j, i < 3 ? 'cap' : 'believe');
    for (const who of sim.state.players.map((p) => p.id)) {
      const v = viewAs(sim, who) as ConfessionRevealView;
      const json = JSON.stringify(v);
      // the only player id in the frame is the confessor — jurors are unrecoverable
      for (const j of jury) {
        if (j !== who) expect(json).not.toContain(`"${j}"`);
      }
      expect(v.spread).toEqual({ believe: 2, cap: 3 });
      expect(json).not.toContain('"votes"');
    }
  });

  it('a voided reveal stamps nothing: truth null, verdict null', () => {
    const sim = control(lockTruth(pickSin(begin(5), 0), true), 'P0', 'VOID');
    const v = viewAs(sim, 'P1') as ConfessionRevealView;
    expect(v.voided).toBe(true);
    expect(v.truth).toBeNull();
    expect(v.verdict).toBeNull();
  });
});

// ===== stale timers & wrong-phase noise =====
describe('stale timers', () => {
  it('CORE_REVEAL_DONE never skips a live jury; stale pick/jury timers are inert', () => {
    const atJury = toJury(begin(5), true);
    expect(st(fire(atJury, CORE_REVEAL_DONE))).toBe(st(atJury));
    expect(st(fire(atJury, pickTimerId(0, 0)))).toBe(st(atJury));
    expect(st(fire(atJury, juryTimerId(0, 1)))).toBe(st(atJury)); // wrong loop
    expect(st(fire(atJury, juryTimerId(1, 0)))).toBe(st(atJury)); // wrong circle
  });

  it('a stale CORE_DEALT once the lock is open is inert', () => {
    const sim = pickSin(begin(5), 0);
    expect(st(fire(sim, CORE_DEALT))).toBe(st(sim));
  });

  it('SKIPEM is inert everywhere — you chose your sin, you sell it', () => {
    const sim = lockTruth(pickSin(begin(5), 0), true);
    expect(st(control(sim, confessorOf(sim), 'SKIPEM'))).toBe(st(sim));
    expect(st(control(sim, 'P0', 'SKIPEM'))).toBe(st(sim));
  });
});

// ===== through the REAL core (engine.reduce) — the seam test =====
describe('confession through reduce() (core $deal/$phase integration)', () => {
  registerModule(confessionModule); // integrator wiring (D-127); tests weld the seam themselves
  const T = 100_000;
  function nightDriver(n: number): Driver {
    const d = new Driver(mkNightRoom(mkSpec('confession', { loops: 2 }), mkPlayers(n)));
    d.pending.set('intro:1', T); // the intro alarm the core would have armed
    return d;
  }
  const gs = (d: Driver): ConfessionState => d.state.gameState as ConfessionState;
  function toPick(d: Driver): void {
    d.runUntil((s) => s.phase.k === 'INPUT' && s.phase.sub === 'PICK');
  }

  it('full circle: PICK -> ceremony (private pre-view) -> paused LOCK -> PERFORM -> JURY -> REVEAL -> LADDER', () => {
    const d = nightDriver(5);
    toPick(d); // intro -> private assignment -> INPUT PICK
    expect(d.state.phase).toMatchObject({ k: 'INPUT', sub: 'PICK' });
    const c1 = gs(d).confessorId;
    const hand = gs(d).hand.map((x) => x.id);
    const chosen = hand[2] as string;

    // confessor picks sin #3 -> the 4.5 ceremony starts, pre-view SENT privately
    const fx = d.dispatch({ t: 'INPUT', id: c1, payload: { pick: 2 }, at: T + 1_000 });
    expect(d.state.phase).toMatchObject({ k: 'DEAL' });
    expect(fx.some((e) => e.k === 'SEND' && e.to === c1 && e.kind === 'preview')).toBe(true);

    d.fireNext(); // ceremony completes (10s: subject named) -> CORE_DEALT -> LOCK
    const lockPhase = d.state.phase as { k: 'INPUT'; sub: string; deadline: number | null };
    expect(lockPhase).toEqual({ k: 'INPUT', circle: 0, sub: 'LOCK', deadline: null }); // provably paused
    expect(d.state.usedCardIds).toEqual([chosen]); // chosen only — unchosen returned to the pool
    for (const id of hand) {
      if (id !== chosen) expect(d.state.usedCardIds).not.toContain(id);
    }

    d.dispatch({ t: 'INPUT', id: c1, payload: { truth: true }, at: T + 20_000 });
    expect(d.state.phase).toMatchObject({ k: 'INPUT', sub: 'PERFORM' });
    expect(d.pending.has(blockingGraceTimer(lockKey(0, 0)))).toBe(false); // shame machinery stood down

    d.dispatch({ t: 'REST_CASE', id: c1, at: T + 30_000 }); // I REST MY CASE
    expect(d.state.phase).toMatchObject({ k: 'INPUT', sub: 'JURY' });

    // jury: 3 wrong, 1 right -> early resolve -> core-held REVEAL
    const jury = d.state.players.filter((p) => p.id !== c1).map((p) => p.id);
    for (const [i, j] of jury.entries()) {
      d.dispatch({ t: 'INPUT', id: j, payload: { vote: i < 3 ? 'cap' : 'believe' }, at: T + 31_000 });
    }
    expect(d.state.phase.k).toBe('REVEAL');
    const epoch = d.state.epoch;
    expect(d.pending.has(`reveal:flip:${epoch}`)).toBe(true); // synced 3-2-1 beat
    expect(d.pending.has(`reveal:softcap:${epoch}`)).toBe(true);
    expect(d.pending.has(`reveal:decay:${epoch}`)).toBe(true);
    // scores applied through the core: the liar +3, the one correct juror +2, the fooled +1
    expect(d.state.players.find((p) => p.id === c1)!.score).toBe(3);
    expect(d.state.players.find((p) => p.id === jury[3])!.score).toBe(2);
    expect(d.state.players.find((p) => p.id === jury[0])!.score).toBe(1);

    d.dispatch({ t: 'DESCEND', id: 'P0', at: T + 60_000 }); // host descends the hold -> loop 2
    toPick(d);
    expect(d.state.phase).toMatchObject({ k: 'INPUT', sub: 'PICK' });
    const c2 = gs(d).confessorId;
    expect(c2).not.toBe(c1);

    d.fireNext(); // pick deadline: auto-pick sin #1 -> ceremony
    d.fireNext(); // ceremony -> LOCK
    const c2ScoreBefore = d.state.players.find((p) => p.id === c2)!.score; // earned as a loop-1 juror
    d.dispatch({ t: 'PLEAD_FIFTH', id: c2, at: T + 90_000 }); // the witness takes the fifth
    expect(d.state.phase.k).toBe('REVEAL');
    expect((gs(d).resolutions[1] as ConfessionResolution).voided).toBe(true);
    expect(d.state.nightStats[c2]?.fifths).toBe(1); // COWARDICE NOTED
    expect(d.state.players.find((p) => p.id === c2)!.score).toBe(c2ScoreBefore); // the voided loop paid 0

    d.dispatch({ t: 'DESCEND', id: 'P0', at: T + 100_000 });
    expect(d.state.phase.k).toBe('LADDER'); // 2 loops done -> core advances
    expect(d.state.usedCardIds).toHaveLength(2); // one chosen sin per loop, nothing else
  });

  it('burn window through the core: the chosen sin swaps to the reserved backup, no trace', () => {
    const d = nightDriver(4);
    toPick(d);
    const c = gs(d).confessorId;
    const hand = gs(d).hand.map((x) => x.id);
    d.dispatch({ t: 'INPUT', id: c, payload: { pick: 0 }, at: T + 1_000 });
    const backupId = d.state.deal!.backup!.id;
    const chosenId = d.state.deal!.card.id;
    expect(chosenId).toBe(hand[0]);

    const fx = d.dispatch({ t: 'BURN', id: c, kind: 'card', at: T + 2_000 }); // inside the 10s window
    expect(fx).toEqual([]); // burned and clean ceremonies are byte-identical on the wire (4.5)

    d.fireNext(); // ceremony completes on the ORIGINAL schedule
    expect(gs(d).sub).toBe('LOCK');
    expect(gs(d).card?.id).toBe(backupId); // the swap
    expect(d.state.usedCardIds).toContain(backupId); // dealt
    expect(d.state.usedCardIds).toContain(chosenId); // vetoed: quarantined for the night
    for (const id of hand.slice(1)) expect(d.state.usedCardIds).not.toContain(id); // unchosen untouched
  });

  it('the shame choreography through the core: grace -> WAITING_ON, host VOID kills the loop', () => {
    const d = nightDriver(4);
    toPick(d);
    const c = gs(d).confessorId;
    d.dispatch({ t: 'INPUT', id: c, payload: { pick: 0 }, at: T + 1_000 });
    d.fireNext(); // ceremony -> LOCK (deadline null, grace + pit armed)
    d.fireNext(); // 12s grace -> WAITING_ON the confessor
    expect(d.state.phase).toMatchObject({ k: 'WAITING_ON', who: c });

    d.dispatch({ t: 'VOID_ROUND', id: 'P0', at: T + 40_000 });
    expect(d.state.phase.k).toBe('REVEAL');
    const res = (d.state.gameState as ConfessionState).resolutions[0] as ConfessionResolution;
    expect(res.voided).toBe(true);
    expect(res.truth).toBeNull(); // no code path fabricates a lock (D-115)
  });
});
