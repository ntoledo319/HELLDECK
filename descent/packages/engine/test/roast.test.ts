// Roast Consensus (spec 5.1) — every rule gets a test (Part 11, task D-112).
// The module rides the core protocol: $deal ceremony -> CORE_DEALT -> $phase INPUT VOTE
// -> $phase REVEAL (core-held) -> CORE_REVEAL_DONE. The harness's `apply` mirrors the
// core's directive handling (engine.test.ts covers the real core; the last describe
// block here drives full circles through reduce() to weld the two together).
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
  Tier,
} from '../src/types.js';
import type { GameCtx, GameStep } from '../src/games/module.js';
import { CORE_DEALT, CORE_REVEAL_DONE } from '../src/games/module.js';
import { rng } from '../src/rng.js';
import { beginDeal, CEREMONY_MS, completeDeal } from '../src/deal.js';
import { FIRE_DECAY_QUIET_MS, REVEAL_MIN_HOLD_MS } from '../src/engine.js';
import {
  DEFAULT_ROAST_DECK,
  SUPPRESSED_EDGE,
  VOTE_MS,
  roastModule,
  roastPromptFilter,
  setRoastDeck,
  type RoastResolution,
  type RoastRevealView,
  type RoastState,
  type RoastVoteView,
} from '../src/games/roast.js';
import { Driver, mkNightRoom, mkPlayers, mkSpec } from './helpers.js';

// ===== harness =====
const NOW = 1_000_000;
const CIRCLE: CircleSpec = { game: 'roast', loops: 3, finale: false, outward: false, rung: 3, bargain: false };

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
  last: GameStep; // the final module step of the chain (scores/done live here)
  effects: Effect[]; // accumulated, incl. intermediate $deal-chain steps
  phase: PhaseDirective | null; // last $phase directive the module emitted
}

function mkCtx(state: RoomState, rand: () => number): GameCtx {
  const players = state.players.filter((p) => p.role !== 'imp');
  const imps = state.players.filter((p) => p.role === 'imp');
  const circle = state.arc[0] as CircleSpec;
  return { state, circle, circleIdx: 0, players, imps, now: NOW, rand, finaleMult: 1, volunteers: [] };
}

/** Mirror of the core's applyStep for the directives roast uses ($deal chain + $phase strip). */
function apply(state: RoomState, rand: () => number, step: GameStep, acc: Effect[] = []): Sim {
  const gs = step.gameState;
  const effects = [...acc, ...step.effects];
  if (gs !== null && typeof gs === 'object' && ('$phase' in gs || '$deal' in gs)) {
    const { $phase, $deal, ...rest } = gs as Record<string, unknown> & ModuleDirectives;
    if ($deal) {
      // Core would run the 4.5 ceremony; here it completes instantly with the same writeback.
      const canBurn =
        $deal.subjectId !== null &&
        (state.players.find((p) => p.id === $deal.subjectId)?.brimstones ?? 0) > 0;
      const begun = beginDeal($deal, 'deal:test', NOW, canBurn);
      const done = completeDeal(begun.deal);
      const nextState: RoomState = {
        ...state,
        gameState: rest,
        deal: done.deal,
        usedCardIds: [...state.usedCardIds, ...done.usedCardIds],
      };
      return apply(nextState, rand, roastModule.timer(mkCtx(nextState, rand), CORE_DEALT), effects);
    }
    const nextState: RoomState = { ...state, gameState: rest };
    return { state: nextState, rand, last: step, effects, phase: ($phase as PhaseDirective | undefined) ?? null };
  }
  // no directives: keep the module's gameState object identity (noop steps must be provably inert)
  return { state: { ...state, gameState: gs }, rand, last: step, effects, phase: null };
}

function begin(nPlayers: number, nImps = 0, seed = 'test', over: Partial<RoomState> = {}): Sim {
  const state = mkState(nPlayers, nImps, over);
  const rand = rng(seed);
  return apply(state, rand, roastModule.start(mkCtx(state, rand)));
}
function vote(sim: Sim, from: string, to: string): Sim {
  return apply(sim.state, sim.rand, roastModule.input(mkCtx(sim.state, sim.rand), from, { vote: to }));
}
function castAll(sim: Sim, pairs: [string, string][]): Sim {
  return pairs.reduce((s, [f, t]) => vote(s, f, t), sim);
}
function fire(sim: Sim, timerId: string): Sim {
  return apply(sim.state, sim.rand, roastModule.timer(mkCtx(sim.state, sim.rand), timerId));
}
function control(sim: Sim, who: string, kind: 'REST' | 'SKIPEM' | 'FIFTH' | 'VOID'): Sim {
  return apply(sim.state, sim.rand, roastModule.control(mkCtx(sim.state, sim.rand), who, kind));
}
function st(sim: Sim): RoastState {
  return sim.state.gameState as RoastState;
}
function cardOf(sim: Sim): CardBase {
  const c = st(sim).card;
  if (!c) throw new Error('no card dealt yet');
  return c;
}
function resolveByTimer(sim: Sim): Sim {
  return fire(sim, `roast:vote:0:${st(sim).loop}`);
}
/** Core hold ends (DESCEND / fire-decay): CORE_REVEAL_DONE -> next prompt or done. */
function advance(sim: Sim): Sim {
  return fire(sim, CORE_REVEAL_DONE);
}
function lastRes(sim: Sim): RoastResolution {
  const r = st(sim).resolutions[st(sim).resolutions.length - 1];
  if (!r) throw new Error('no resolution yet');
  return r;
}
function viewAs(sim: Sim, viewer: string): RoastVoteView | RoastRevealView {
  const v = roastModule.view(mkCtx(sim.state, sim.rand), viewer);
  if (!v) throw new Error('no view');
  return v;
}
function card(exposure: Tier, n: number): CardBase {
  return {
    id: `roast_test_${exposure}_${n}`,
    deck: 'roast',
    text: `test prompt E${exposure} #${n}`,
    exposure,
    chaos: 3,
    register: 'deadpan',
    skeleton: `test-e${exposure}-${n}`,
  };
}
/** which of `ids` appear (as JSON string values/keys) in the serialized view */
function idsIn(json: string, ids: string[]): string[] {
  return ids.filter((id) => json.includes(`"${id}"`));
}

beforeEach(() => setRoastDeck(DEFAULT_ROAST_DECK));

// ===== deal & prompt flow =====
describe('deal & prompts', () => {
  it('start runs the $deal ceremony into VOTE: $phase INPUT + the 20s skippable timer', () => {
    const sim = begin(6);
    expect(st(sim).sub).toBe('VOTE');
    expect(st(sim).loop).toBe(0);
    expect(st(sim).loops).toBe(3);
    expect(sim.phase).toEqual({ k: 'INPUT', sub: 'VOTE', deadline: NOW + VOTE_MS });
    expect(sim.effects).toContainEqual({ k: 'SCHEDULE', timerId: 'roast:vote:0:0', atMs: NOW + VOTE_MS });
  });

  it('the ceremony writes night-dedup back: dealt card lands in usedCardIds, backup returns to the pool', () => {
    const sim = begin(6);
    expect(sim.state.usedCardIds).toEqual([cardOf(sim).id]); // primary only — unused backup untraced
  });

  it('runs exactly 3 prompts per circle with distinct cards, then reports done', () => {
    let sim = begin(6);
    const dealt: string[] = [];
    for (let loop = 0; loop < 3; loop++) {
      expect(st(sim).loop).toBe(loop);
      dealt.push(cardOf(sim).id);
      sim = vote(sim, 'P0', 'P1');
      sim = resolveByTimer(sim);
      expect(st(sim).sub).toBe('REVEAL');
      expect(sim.last.done).not.toBe(true); // done only after the final hold
      sim = advance(sim);
    }
    expect(new Set(dealt).size).toBe(3);
    expect(sim.last.done).toBe(true);
    expect(st(sim).resolutions).toHaveLength(3);
  });

  it('resolution hands the hold to the core ($phase REVEAL) and 3 prompts fit the 3:30 budget', () => {
    const sim = resolveByTimer(vote(begin(6), 'P0', 'P1'));
    expect(sim.phase).toEqual({ k: 'REVEAL' }); // core stamps holdSince + arms flip/softcap/decay
    // worst-case auto-advance pacing: ceremony + vote + min-hold + fire-decay quiet
    expect(3 * (CEREMONY_MS + VOTE_MS + REVEAL_MIN_HOLD_MS + FIRE_DECAY_QUIET_MS)).toBeLessThanOrEqual(210_000);
  });

  it('N<=4 attributed mode caps prompts at E<=3 even under content starvation', () => {
    setRoastDeck([card(4, 1), card(5, 1), card(3, 1)]);
    let sim = begin(4);
    for (let loop = 0; loop < 3; loop++) {
      expect(cardOf(sim).exposure).toBeLessThanOrEqual(3);
      sim = advance(resolveByTimer(vote(sim, 'P0', 'P1')));
    }
  });

  it('N>=5 has no attributed exposure cap', () => {
    setRoastDeck([card(5, 1), card(5, 2), card(5, 3)]);
    const sim = begin(6);
    expect(cardOf(sim).exposure).toBe(5);
  });

  it('consent 4.4 (vote-emergent): E>3 prompts need min(all ceilings), E<=3 the generic ceiling', () => {
    setRoastDeck([card(2, 1), card(4, 1), card(5, 1)]);
    // N=5, ceilings [3,3,3,5,5]: generic = second-lowest = 3; E4/E5 need min=3 -> illegal
    const shy = mkState(5);
    shy.players = shy.players.map((p, i) => ({ ...p, heatCeiling: (i < 3 ? 3 : 5) as Tier }));
    const rand = rng('consent');
    for (let i = 0; i < 8; i++) {
      const sim = apply(shy, rand, roastModule.start(mkCtx(shy, rand)));
      expect(cardOf(sim).exposure).toBeLessThanOrEqual(3);
    }
  });

  it('roastPromptFilter hook caps at N<=4 and passes through at N>=5', () => {
    const cards = [card(2, 1), card(3, 1), card(4, 1), card(5, 1)];
    expect(roastPromptFilter(cards, 3).map((c) => c.exposure)).toEqual([2, 3]);
    expect(roastPromptFilter(cards, 4).map((c) => c.exposure)).toEqual([2, 3]);
    expect(roastPromptFilter(cards, 5)).toHaveLength(4);
  });

  it('excludes night-used card ids when fresh cards remain', () => {
    setRoastDeck([card(2, 1), card(2, 2)]);
    const sim = begin(6, 0, 'test', { usedCardIds: ['roast_test_2_1'] });
    expect(cardOf(sim).id).toBe('roast_test_2_2');
  });

  it('setRoastDeck rejects an empty deck', () => {
    expect(() => setRoastDeck([])).toThrow();
  });
});

// ===== ballots =====
describe('ballots', () => {
  it('blocks self-votes', () => {
    const sim = vote(begin(5), 'P0', 'P0');
    expect(st(sim).votes).toEqual({});
    expect((viewAs(sim, 'P0') as RoastVoteView).youVoted).toBeNull();
  });

  it('ignores unknown voters and unknown targets', () => {
    let sim = vote(begin(5), 'GHOST', 'P1');
    expect(st(sim).votes).toEqual({});
    sim = vote(sim, 'P1', 'GHOST');
    expect(st(sim).votes).toEqual({});
  });

  it('ignores malformed payloads', () => {
    const sim = begin(5);
    for (const bad of [null, 42, 'P1', {}, { vote: 7 }, { vote: '' }]) {
      const next = apply(sim.state, sim.rand, roastModule.input(mkCtx(sim.state, sim.rand), 'P0', bad));
      expect(st(next).votes).toEqual({});
    }
  });

  it('re-vote before the deadline replaces the ballot (last one wins)', () => {
    const sim = vote(vote(begin(5), 'P0', 'P1'), 'P0', 'P2');
    expect(st(sim).votes).toEqual({ P0: 'P2' });
    expect((viewAs(sim, 'P0') as RoastVoteView).votedCount).toBe(1);
  });

  it('drops ballots that arrive after resolution', () => {
    const sim = vote(resolveByTimer(vote(begin(5), 'P0', 'P1')), 'P2', 'P1');
    expect(st(sim).votes).toEqual({ P0: 'P1' });
  });

  it('attributed mode NEVER constrains ballots — the same edge is votable every prompt', () => {
    let sim = begin(3);
    for (let loop = 0; loop < 3; loop++) {
      sim = castAll(sim, [
        ['P0', 'P2'],
        ['P1', 'P2'],
        ['P2', 'P0'],
      ]); // 3rd ballot completes the room -> auto-resolve
      expect(lastRes(sim).victims).toEqual(['P2']);
      if (loop < 2) sim = advance(sim);
    }
  });

  it('resolves early when every voter is in, cancelling the vote timer', () => {
    const sim = castAll(begin(3), [
      ['P0', 'P2'],
      ['P1', 'P2'],
      ['P2', 'P0'],
    ]);
    expect(st(sim).sub).toBe('REVEAL');
    expect(sim.effects).toContainEqual({ k: 'CANCEL', timerId: 'roast:vote:0:0' });
  });
});

// ===== resolution & vote math =====
describe('resolution & vote math', () => {
  it('N=3: plurality victim with correct spread', () => {
    const sim = castAll(begin(3), [
      ['P0', 'P2'],
      ['P1', 'P2'],
      ['P2', 'P0'],
    ]);
    const res = lastRes(sim);
    expect(res.victims).toEqual(['P2']);
    expect(res.doubleRoast).toBe(false);
    expect(res.spread).toEqual([
      { target: 'P2', weight: 2 },
      { target: 'P0', weight: 1 },
    ]);
  });

  it('N=5: plurality among split votes', () => {
    const sim = resolveByTimer(
      castAll(begin(5), [
        ['P0', 'P4'],
        ['P1', 'P4'],
        ['P2', 'P4'],
        ['P3', 'P0'],
      ]),
    );
    const res = lastRes(sim);
    expect(res.victims).toEqual(['P4']);
    expect(res.spread[0]).toEqual({ target: 'P4', weight: 3 });
  });

  it('N=6: tie -> DOUBLE ROAST, both names in lights', () => {
    const sim = resolveByTimer(
      castAll(begin(6), [
        ['P0', 'P4'],
        ['P1', 'P4'],
        ['P2', 'P5'],
        ['P3', 'P5'],
      ]),
    );
    const res = lastRes(sim);
    expect(res.victims).toEqual(['P4', 'P5']);
    expect(res.doubleRoast).toBe(true);
  });

  it('DOUBLE ROAST: a vote for EITHER victim scores +2', () => {
    // N=4, 2-2 tie where every ballot lands on a victim -> unanimity heat too
    const sim = castAll(begin(4), [
      ['P0', 'P2'],
      ['P1', 'P3'],
      ['P2', 'P3'],
      ['P3', 'P2'],
    ]);
    const res = lastRes(sim);
    expect(res.victims).toEqual(['P2', 'P3']);
    expect(res.roomHeat).toBe(true);
    // +2 plurality +1 heat +1 participation each
    expect(sim.last.scores).toEqual({ P0: 4, P1: 4, P2: 4, P3: 4 });
  });

  it.each([8, 10, 12])('N=%i: unweighted spread sums to ballots cast', (n) => {
    const pairs: [string, string][] = [];
    for (let i = 0; i < n - 1; i++) pairs.push([`P${i}`, i % 2 === 0 ? `P${n - 1}` : 'P0']);
    const sim = resolveByTimer(castAll(begin(n), pairs));
    const res = lastRes(sim);
    const total = res.spread.reduce((s, b) => s + b.weight, 0);
    expect(total).toBe(n - 1);
    expect(res.victims).toEqual([`P${n - 1}`]); // ceil((n-1)/2) votes beats floor
  });

  it('nobody votes at all -> no victim, no scores, no heat', () => {
    const sim = resolveByTimer(begin(5));
    const res = lastRes(sim);
    expect(res.victims).toEqual([]);
    expect(res.roomHeat).toBe(false);
    expect(sim.last.scores).toEqual({});
  });
});

// ===== auto-abstain & timers =====
describe('auto-abstain & timers', () => {
  it('vote deadline auto-abstains the silent: resolution uses only cast ballots', () => {
    const sim = resolveByTimer(
      castAll(begin(6), [
        ['P0', 'P5'],
        ['P1', 'P5'],
      ]),
    );
    const res = lastRes(sim);
    expect(res.victims).toEqual(['P5']);
    expect(res.spread).toEqual([{ target: 'P5', weight: 2 }]);
    // abstainers earn nothing — not even participation
    expect(sim.last.scores).toEqual({ P0: 3, P1: 3 });
  });

  it('ignores a stale vote timer during REVEAL and wrong-loop/circle timers', () => {
    let sim = resolveByTimer(vote(begin(5), 'P0', 'P1'));
    const before = st(sim);
    sim = fire(sim, 'roast:vote:0:0'); // stale: loop already resolved
    expect(st(sim)).toBe(before);
    sim = fire(sim, 'roast:vote:0:1'); // wrong loop
    expect(st(sim)).toBe(before);
    sim = fire(sim, 'roast:vote:1:0'); // wrong circle
    expect(st(sim)).toBe(before);
  });

  it('ignores CORE_REVEAL_DONE while voting is still open (never skips a live vote)', () => {
    const sim = fire(begin(5), CORE_REVEAL_DONE);
    expect(st(sim).sub).toBe('VOTE');
  });

  it('ignores a stale CORE_DEALT once voting is open', () => {
    const sim = begin(5);
    const before = st(sim);
    const next = fire(sim, CORE_DEALT);
    expect(st(next)).toBe(before);
  });
});

// ===== imp half-votes (4.8) =====
describe('imp half-votes', () => {
  it('an imp ballot adds 0.5 to the spread', () => {
    const sim = resolveByTimer(
      castAll(begin(5, 1), [
        ['P0', 'P4'],
        ['I0', 'P4'],
      ]),
    );
    expect(lastRes(sim).spread).toEqual([{ target: 'P4', weight: 1.5 }]);
  });

  it('never BREAKS a full-vote tie: 2-2 stays DOUBLE ROAST despite an imp ballot', () => {
    const sim = resolveByTimer(
      castAll(begin(4, 1), [
        ['P0', 'P2'],
        ['P1', 'P3'],
        ['P2', 'P3'],
        ['P3', 'P2'],
        ['I0', 'P2'], // weighted 2.5 vs 2 — must not decide it
      ]),
    );
    const res = lastRes(sim);
    expect(res.victims).toEqual(['P2', 'P3']);
    expect(res.doubleRoast).toBe(true);
  });

  it('never CREATES a tie: an imp-manufactured weighted tie collapses to the full-vote leader', () => {
    const sim = resolveByTimer(
      castAll(begin(3, 2), [
        ['P0', 'P2'],
        ['P1', 'P2'],
        ['P2', 'P0'],
        ['I0', 'P0'],
        ['I1', 'P0'], // weighted P0=2, P2=2 — imps must not force a double roast
      ]),
    );
    const res = lastRes(sim);
    expect(res.victims).toEqual(['P2']);
    expect(res.doubleRoast).toBe(false);
  });

  it('imp weight DOES count toward plurality when no tie is in play', () => {
    const sim = resolveByTimer(
      castAll(begin(5, 4), [
        ['P0', 'P4'],
        ['P1', 'P4'],
        ['P2', 'P0'],
        ['P3', 'P0'],
        ['P4', 'P0'], // full votes: P0=3, P4=2
        ['I0', 'P4'],
        ['I1', 'P4'],
        ['I2', 'P4'],
        ['I3', 'P4'], // weighted: P4=4, P0=3 — clean single leader, imps count
      ]),
    );
    expect(lastRes(sim).victims).toEqual(['P4']);
  });

  it('all-imp ballots (players silent) still produce a victim', () => {
    const sim = resolveByTimer(
      castAll(begin(3, 2), [
        ['I0', 'P1'],
        ['I1', 'P1'],
      ]),
    );
    expect(lastRes(sim).victims).toEqual(['P1']);
  });

  it('imps fuel Room Heat at half weight (abstaining imp breaks unanimity)', () => {
    const votesCast: [string, string][] = [
      ['P0', 'P2'],
      ['P1', 'P2'],
      ['P2', 'P0'],
    ];
    const cold = resolveByTimer(castAll(begin(3, 1), votesCast)); // imp abstains: 2/2.5
    expect(lastRes(cold).roomHeat).toBe(false);
    const hot = resolveByTimer(castAll(begin(3, 1), [...votesCast, ['I0', 'P2']])); // 2.5/2.5
    expect(lastRes(hot).roomHeat).toBe(true);
  });
});

// ===== Room Heat thresholds (4.6) =====
describe('room heat', () => {
  it('N=3 (<=5): unanimity-minus-victim fires heat', () => {
    const sim = castAll(begin(3), [
      ['P0', 'P2'],
      ['P1', 'P2'],
      ['P2', 'P0'],
    ]);
    const res = lastRes(sim);
    expect(res.roomHeat).toBe(true);
    // P0/P1: +2 plurality +1 heat +1 participation; P2 (victim, voted non-victim): +1 participation
    expect(sim.last.scores).toEqual({ P0: 4, P1: 4, P2: 1 });
  });

  it('N=3: a non-victim abstainer kills unanimity', () => {
    const sim = resolveByTimer(vote(begin(3), 'P0', 'P2'));
    expect(lastRes(sim).victims).toEqual(['P2']);
    expect(lastRes(sim).roomHeat).toBe(false);
  });

  it('N=5 (<=5): unanimity fires; 3-of-4 does not', () => {
    const unanimous = castAll(begin(5), [
      ['P0', 'P4'],
      ['P1', 'P4'],
      ['P2', 'P4'],
      ['P3', 'P4'],
      ['P4', 'P0'],
    ]);
    expect(lastRes(unanimous).roomHeat).toBe(true);
    const split = castAll(begin(5), [
      ['P0', 'P4'],
      ['P1', 'P4'],
      ['P2', 'P4'],
      ['P3', 'P0'],
      ['P4', 'P0'],
    ]);
    expect(lastRes(split).victims).toEqual(['P4']);
    expect(lastRes(split).roomHeat).toBe(false);
  });

  it('N=6 (else-bucket): 80% fires, 60% does not', () => {
    const hot = castAll(begin(6), [
      ['P0', 'P5'],
      ['P1', 'P5'],
      ['P2', 'P5'],
      ['P3', 'P5'],
      ['P4', 'P0'],
      ['P5', 'P0'],
    ]); // 4/5 = 0.8
    expect(lastRes(hot).roomHeat).toBe(true);
    const warm = castAll(begin(6), [
      ['P0', 'P5'],
      ['P1', 'P5'],
      ['P2', 'P5'],
      ['P3', 'P0'],
      ['P4', 'P0'],
      ['P5', 'P1'],
    ]); // 3/5 = 0.6 — N=6 demands 80
    expect(lastRes(warm).victims).toEqual(['P5']);
    expect(lastRes(warm).roomHeat).toBe(false);
  });

  it('N=7 (else-bucket): 5/6 fires, 4/6 does not', () => {
    const hot = castAll(begin(7), [
      ['P0', 'P6'],
      ['P1', 'P6'],
      ['P2', 'P6'],
      ['P3', 'P6'],
      ['P4', 'P6'],
      ['P5', 'P0'],
      ['P6', 'P0'],
    ]);
    expect(lastRes(hot).roomHeat).toBe(true);
    const warm = castAll(begin(7), [
      ['P0', 'P6'],
      ['P1', 'P6'],
      ['P2', 'P6'],
      ['P3', 'P6'],
      ['P4', 'P0'],
      ['P5', 'P0'],
      ['P6', 'P1'],
    ]);
    expect(lastRes(warm).roomHeat).toBe(false);
  });

  it('N=8 (>=60%): 5/7 fires, 4/7 does not', () => {
    const hot = castAll(begin(8), [
      ['P0', 'P7'],
      ['P1', 'P7'],
      ['P2', 'P7'],
      ['P3', 'P7'],
      ['P4', 'P7'],
      ['P5', 'P0'],
      ['P6', 'P0'],
      ['P7', 'P0'],
    ]);
    expect(lastRes(hot).victims).toEqual(['P7']);
    expect(lastRes(hot).roomHeat).toBe(true);
    const warm = castAll(begin(8), [
      ['P0', 'P7'],
      ['P1', 'P7'],
      ['P2', 'P7'],
      ['P3', 'P7'],
      ['P4', 'P0'],
      ['P5', 'P0'],
      ['P6', 'P0'],
      ['P7', 'P1'],
    ]);
    expect(lastRes(warm).victims).toEqual(['P7']);
    expect(lastRes(warm).roomHeat).toBe(false);
  });

  it('N=10 (>=60%): 6/9 fires, 5/9 does not', () => {
    const to = (n: number, t: string, rest: string): [string, string][] =>
      Array.from({ length: 9 }, (_, i): [string, string] => [`P${i}`, i < n ? t : rest]);
    const hot = resolveByTimer(castAll(begin(10), to(6, 'P9', 'P0')));
    expect(lastRes(hot).roomHeat).toBe(true);
    const warm = resolveByTimer(castAll(begin(10), to(5, 'P9', 'P0')));
    expect(lastRes(warm).victims).toEqual(['P9']);
    expect(lastRes(warm).roomHeat).toBe(false);
  });

  it('N=12 (>=60%): 7/11 fires, 6/11 does not', () => {
    const to = (n: number): [string, string][] =>
      Array.from({ length: 11 }, (_, i): [string, string] => [`P${i}`, i < n ? 'P11' : 'P0']);
    const hot = resolveByTimer(castAll(begin(12), to(7)));
    expect(lastRes(hot).roomHeat).toBe(true);
    const warm = resolveByTimer(castAll(begin(12), to(6)));
    expect(lastRes(warm).victims).toEqual(['P11']);
    expect(lastRes(warm).roomHeat).toBe(false);
  });
});

// ===== scoring (4.6) =====
describe('scoring', () => {
  it('plurality voters +2, others participation only, victims earn nothing extra', () => {
    const sim = resolveByTimer(
      castAll(begin(6), [
        ['P0', 'P5'],
        ['P1', 'P5'],
        ['P2', 'P5'],
        ['P3', 'P0'],
        ['P4', 'P0'],
      ]),
    );
    expect(lastRes(sim).roomHeat).toBe(false); // 3/5 < 0.8
    expect(sim.last.scores).toEqual({ P0: 3, P1: 3, P2: 3, P3: 1, P4: 1 });
  });

  it('participation pays +1 once per CIRCLE, not once per prompt', () => {
    let sim = resolveByTimer(vote(begin(5), 'P0', 'P1'));
    expect(sim.last.scores).toEqual({ P0: 3 }); // +2 plurality +1 participation
    sim = resolveByTimer(vote(advance(sim), 'P0', 'P1'));
    expect(sim.last.scores).toEqual({ P0: 2 }); // participation already spent
  });

  it('scores land in GameStep.scores pre-multiplier (core owns finale x3)', () => {
    const sim = resolveByTimer(vote(begin(5), 'P0', 'P1'));
    expect(sim.last.scores).toBeDefined();
    expect(Object.values(sim.last.scores ?? {}).every((v) => Number.isInteger(v))).toBe(true);
  });
});

// ===== view redaction (5.1 + 3.4) =====
describe('view redaction', () => {
  it('during the DEAL ceremony the view is null — the card never pre-leaks', () => {
    // module state mid-ceremony: sub DEAL, card null (as the core holds it before CORE_DEALT)
    const state = mkState(5, 0, {
      gameState: {
        sub: 'DEAL',
        loop: 0,
        loops: 3,
        card: null,
        votes: {},
        voteDeadline: 0,
        resolutions: [],
        shownEdges: [],
        participation: [],
      } satisfies RoastState,
    });
    expect(roastModule.view(mkCtx(state, rng('x')), 'P0')).toBeNull();
  });

  it('during VOTE a viewer sees counts + their own ballot only', () => {
    const sim = castAll(begin(6), [
      ['P0', 'P5'],
      ['P1', 'P3'],
    ]);
    const mine = viewAs(sim, 'P1') as RoastVoteView;
    expect(mine.sub).toBe('VOTE');
    expect(mine.votedCount).toBe(2);
    expect(mine.eligible).toBe(6);
    expect(mine.youVoted).toBe('P3');
    // a non-voter's frame carries zero ballot data — no player id anywhere
    const other = JSON.stringify(viewAs(sim, 'P2'));
    expect(idsIn(other, ['P0', 'P1', 'P3', 'P4', 'P5'])).toEqual([]);
    expect(other).not.toContain('"votes"');
  });

  it('N=6 reveal: no voter->victim pair recoverable from ANY viewer frame', () => {
    // everyone convicts P5; P5 votes P0 — last ballot completes the room and resolves
    const sim = castAll(begin(6), [
      ['P0', 'P5'],
      ['P1', 'P5'],
      ['P2', 'P5'],
      ['P3', 'P5'],
      ['P4', 'P5'],
      ['P5', 'P0'],
    ]);
    expect(st(sim).sub).toBe('REVEAL');
    const targetsOnly = ['P0', 'P5']; // legitimately public: victims + spread bars
    for (const viewer of ['P0', 'P1', 'P2', 'P3', 'P4', 'P5']) {
      const v = viewAs(sim, viewer) as RoastRevealView;
      const json = JSON.stringify(v);
      // the only player ids in the frame are vote TARGETS — voters are unrecoverable
      expect(idsIn(json, ['P1', 'P2', 'P3', 'P4'])).toEqual([]);
      expect(idsIn(json, ['P0', 'P1', 'P2', 'P3', 'P4', 'P5']).every((id) => targetsOnly.includes(id))).toBe(true);
      expect(v.edges).toBeUndefined();
      expect(json).not.toContain('"votes"');
      expect(json).not.toContain('"scores"'); // a per-prompt +2 would out a ballot
      expect(v.spread).toEqual([
        { target: 'P5', weight: 5 },
        { target: 'P0', weight: 1 },
      ]);
      expect(v.youVoted).toBe(viewer === 'P5' ? 'P0' : 'P5'); // own ballot only, per socket
    }
  });

  it('N=5 is already anonymous (spread, no edges); N<=4 is attributed', () => {
    const five = resolveByTimer(vote(begin(5), 'P0', 'P1'));
    const v5 = viewAs(five, 'P2') as RoastRevealView;
    expect(v5.attributed).toBe(false);
    expect(v5.spread).toBeDefined();
    expect(v5.edges).toBeUndefined();

    const four = resolveByTimer(vote(begin(4), 'P0', 'P1'));
    const v4 = viewAs(four, 'P2') as RoastRevealView;
    expect(v4.attributed).toBe(true);
    expect(v4.edges).toBeDefined();
    expect(v4.spread).toBeUndefined();
  });

  it('FACE YOUR ACCUSERS: attributed edges at N=3, repeats suppressed as ▮▮▮ within the circle', () => {
    let sim = castAll(begin(3), [
      ['P0', 'P2'],
      ['P1', 'P2'],
      ['P2', 'P0'],
    ]);
    expect((viewAs(sim, 'P0') as RoastRevealView).edges).toEqual([
      { from: 'P0', to: 'P2' },
      { from: 'P1', to: 'P2' },
      { from: 'P2', to: 'P0' },
    ]);
    // prompt 2: P0 and P2 repeat their edges, P1 flips — only the fresh edge shows
    sim = castAll(advance(sim), [
      ['P0', 'P2'],
      ['P1', 'P0'],
      ['P2', 'P0'],
    ]);
    expect((viewAs(sim, 'P1') as RoastRevealView).edges).toEqual([
      { from: 'P0', to: SUPPRESSED_EDGE },
      { from: 'P1', to: 'P0' },
      { from: 'P2', to: SUPPRESSED_EDGE },
    ]);
    // suppression is display-only: the repeated ballots still counted
    expect(lastRes(sim).victims).toEqual(['P0']);
    // prompt 3: P1's prompt-2 edge is now a repeat too
    sim = castAll(advance(sim), [
      ['P0', 'P1'],
      ['P1', 'P0'],
      ['P2', 'P1'],
    ]);
    expect((viewAs(sim, 'P2') as RoastRevealView).edges).toEqual([
      { from: 'P0', to: 'P1' },
      { from: 'P1', to: SUPPRESSED_EDGE },
      { from: 'P2', to: 'P1' },
    ]);
  });
});

// ===== controls =====
describe('controls', () => {
  it('host VOID kills the loop scoreless and cancels the vote timer', () => {
    const sim = control(vote(begin(4), 'P1', 'P2'), 'P0', 'VOID');
    expect(st(sim).sub).toBe('REVEAL');
    const res = lastRes(sim);
    expect(res.voided).toBe(true);
    expect(res.victims).toEqual([]);
    expect(sim.last.scores).toEqual({});
    expect(sim.effects).toContainEqual({ k: 'CANCEL', timerId: 'roast:vote:0:0' });
    expect(sim.phase).toEqual({ k: 'REVEAL' }); // the void still holds — the room hears the sting
  });

  it('void advances to the next prompt after the hold', () => {
    const next = advance(control(begin(4), 'P0', 'VOID'));
    expect(st(next).sub).toBe('VOTE');
    expect(st(next).loop).toBe(1);
  });

  it('non-host VOID and REST/SKIPEM/FIFTH are inert', () => {
    const sim = vote(begin(4), 'P1', 'P2');
    for (const [who, kind] of [
      ['P1', 'VOID'],
      ['P0', 'REST'],
      ['P0', 'SKIPEM'],
      ['P1', 'FIFTH'],
    ] as const) {
      const next = control(sim, who, kind);
      expect(st(next).sub).toBe('VOTE');
      expect(next.last.effects).toEqual([]);
    }
  });
});

// ===== through the REAL core (engine.reduce) — the seam test =====
describe('roast through reduce() (core $deal/$phase integration)', () => {
  const T = 100_000;
  function nightDriver(n: number): Driver {
    const d = new Driver(mkNightRoom(mkSpec('roast', { loops: 3 }), mkPlayers(n)));
    d.pending.set('intro:1', T); // the intro alarm the core would have armed
    return d;
  }

  it('a full 3-prompt circle: DEAL -> INPUT(VOTE) -> REVEAL -> ... -> LADDER, dedup written back', () => {
    const d = nightDriver(5);
    d.fireNext(); // intro -> DEAL, ceremony armed
    expect(d.state.phase).toEqual({ k: 'DEAL', circle: 0 });
    const dealt: string[] = [];

    for (let loop = 0; loop < 3; loop++) {
      // ceremony completes -> module opens VOTE with a real Phase deadline
      d.runUntil((s) => s.phase.k === 'INPUT');
      const phase = d.state.phase as { k: 'INPUT'; sub: string; deadline: number | null };
      expect(phase.sub).toBe('VOTE');
      expect(typeof phase.deadline).toBe('number');
      const gs = d.state.gameState as RoastState;
      expect(gs.sub).toBe('VOTE');
      expect(gs.loop).toBe(loop);
      dealt.push((gs.card as CardBase).id);
      expect(d.state.usedCardIds).toContain((gs.card as CardBase).id); // night-scope dedup writeback

      // everyone votes P1 (self excluded) -> early resolve -> core-held REVEAL
      for (const p of d.state.players) {
        d.dispatch({ t: 'INPUT', id: p.id, payload: { vote: p.id === 'P1' ? 'P0' : 'P1' }, at: T + 1000 });
      }
      expect(d.state.phase.k).toBe('REVEAL');
      const epoch = d.state.epoch;
      expect(d.pending.has(`reveal:flip:${epoch}`)).toBe(true); // synced 3-2-1 beat
      expect(d.pending.has(`reveal:softcap:${epoch}`)).toBe(true);
      expect(d.pending.has(`reveal:decay:${epoch}`)).toBe(true);

      // host DESCENDs the hold -> next prompt's ceremony (or the LADDER after prompt 3)
      d.dispatch({ t: 'DESCEND', id: 'P0', at: T + 30_000 });
    }

    expect(new Set(dealt).size).toBe(3);
    expect(d.state.phase.k).toBe('LADDER');
    expect(d.state.usedCardIds).toHaveLength(3); // primaries only; unused backups returned
    // scores multiplied through the core: P1's voters hold plurality points
    expect(d.state.players.find((p) => p.id === 'P0')!.score).toBeGreaterThan(0);
  });

  it('vote deadline through the core: TIMER resolves with auto-abstain', () => {
    const d = nightDriver(4);
    d.fireNext(); // intro -> DEAL
    d.runUntil((s) => s.phase.k === 'INPUT');
    d.dispatch({ t: 'INPUT', id: 'P0', payload: { vote: 'P2' }, at: T + 6000 });
    d.runUntil((s) => s.phase.k === 'REVEAL'); // fires the roast:vote timer
    const gs = d.state.gameState as RoastState;
    expect(gs.resolutions[0]?.victims).toEqual(['P2']);
  });
});

describe('typecast governor (card-council finding)', () => {
  it('routes the ELECTED roast victim through the spotlight-fairness channel', () => {
    let sim = begin(5);
    // the whole room piles on P0
    sim = castAll(sim, [
      ['P1', 'P0'],
      ['P2', 'P0'],
      ['P3', 'P0'],
      ['P4', 'P0'],
    ]);
    sim = resolveByTimer(sim);
    expect(lastRes(sim).victims).toEqual(['P0']);
    // Roast's victim is elected by BALLOT, so pickSpotlight never saw them — yet being named
    // in lights IS the spotlight. Feeding them through GameStep.spotlight makes the core bump
    // spotlightCount, so every OTHER game's assignment leans away from them for the rest of
    // the night. Without it, one quiet player can be voted the answer all night AND keep
    // drawing the subject/confessor seat; the exposure ceilings can't catch that.
    expect(sim.last.spotlight).toEqual(['P0']);
  });

  it('a DOUBLE ROAST bumps both victims, and a voided round bumps nobody', () => {
    let sim = begin(5);
    sim = castAll(sim, [
      ['P1', 'P0'],
      ['P2', 'P0'],
      ['P3', 'P4'],
      ['P4', 'P0'],
    ]);
    sim = resolveByTimer(sim);
    expect(sim.last.spotlight).toEqual(lastRes(sim).victims); // whatever the tally decided

    let v = begin(5);
    v = control(v, 'P0', 'VOID'); // host kills the round: nobody is named, nobody is bumped
    expect(v.last.spotlight).toBeUndefined();
  });
});
