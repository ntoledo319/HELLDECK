// OVER/UNDER (spec 5.3 + HDRealRules2 §3) — every rule gets a test (Part 11, task D-122).
// The module rides the core protocol: $deal {subjectId} ceremony -> CORE_DEALT ->
// $phase INPUT DEBATE (scribe dial) -> $phase INPUT BET (skippable) -> blocking TRUTH
// (4.7 terminals via the engine helpers) -> $phase REVEAL (core-held) -> CORE_REVEAL_DONE.
// The harness's `apply` mirrors the core's directive handling; the last describe blocks
// drive full circles through reduce() to weld module and core together.
import { beforeEach, describe, expect, it } from 'vitest';
import type {
  CircleSpec,
  Effect,
  ModuleDirectives,
  OverUnderCard,
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
import { beginDeal, completeDeal, PREVIEW_MS } from '../src/deal.js';
import { beginSpotlight, completeSpotlight, handoffSpotlight, spotlightResolution } from '../src/spotlight.js';
import {
  BLOCKING_GRACE_MS,
  BLOCKING_PIT_MS,
  blockingGraceTimer,
  blockingPitTimer,
  registerModule,
  SEAT_HOLD_MS,
} from '../src/engine.js';
import {
  BET_MS,
  betTimerId,
  DEBATE_MS,
  debateTimerId,
  DEFAULT_OVERUNDER_DECK,
  FIRST_IMPRESSION_TIMEBOXES,
  isPersonalHistory,
  overunderModule,
  setOverUnderDeck,
  truthKey,
  type Bet,
  type OverUnderBetView,
  type OverUnderDebateView,
  type OverUnderResolution,
  type OverUnderRevealView,
  type OverUnderState,
  type OverUnderTruthView,
  type OverUnderView,
} from '../src/games/overunder.js';
import { Driver, mkNightRoom, mkPlayers, mkSpec } from './helpers.js';

// ===== harness (mirror of roast.test.ts, adapted to the overunder chain) =====
const NOW = 1_000_000;
const CIRCLE: CircleSpec = { game: 'overunder', loops: 2, finale: false, outward: false, rung: 5, bargain: false };

function mkPlayer(id: string, role: Role, seat: number, over: Partial<Player> = {}): Player {
  return {
    id,
    name: id,
    avatar: seat % 16,
    role,
    seat,
    connected: true,
    lastSeenAt: 0,
    heatCeiling: 5,
    ceilingSet: true,
    attested18: true,
    brimstones: 2,
    score: 0,
    spotlightCount: 0,
    freshMeat: false,
    ...over,
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
  last: GameStep; // final module step of the chain (scores/done live here)
  effects: Effect[]; // accumulated, incl. intermediate $deal-chain steps
  phase: PhaseDirective | null; // last $phase directive the module emitted
}

function mkCtx(state: RoomState, rand: () => number, spotlight?: SpotlightResolution): GameCtx {
  const players = state.players.filter((p) => p.role !== 'imp');
  const imps = state.players.filter((p) => p.role === 'imp');
  const circle = state.arc[0] as CircleSpec;
  return { state, circle, circleIdx: 0, players, imps, now: NOW, rand, finaleMult: 1, volunteers: [], ...(spotlight ? { spotlight } : {}) };
}

/** Mirror of the core's applyStep for the directives overunder uses ($deal chain + $phase strip). */
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
      return apply(nextState, rand, overunderModule.timer(mkCtx(nextState, rand, resolution), CORE_SPOTLIGHT_DONE), [
        ...effects,
        ...begun.effects,
        ...handed.effects,
      ]);
    }
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
      return apply(nextState, rand, overunderModule.timer(mkCtx(nextState, rand), CORE_DEALT), [
        ...effects,
        ...begun.effects,
      ]);
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
  return apply(state, rand, overunderModule.start(mkCtx(state, rand)));
}
/** Deterministic probe: subject/scribe are seeded-random — walk seeds until pred holds. */
function beginSuchThat(
  nPlayers: number,
  pred: (sim: Sim) => boolean,
  nImps = 0,
  over: Partial<RoomState> = {},
): Sim {
  for (let i = 0; i < 64; i++) {
    const sim = begin(nPlayers, nImps, `probe-${i}`, over);
    if (pred(sim)) return sim;
  }
  throw new Error('no seed satisfied the predicate');
}
const input = (sim: Sim, who: string, payload: unknown): Sim =>
  apply(sim.state, sim.rand, overunderModule.input(mkCtx(sim.state, sim.rand), who, payload));
const fire = (sim: Sim, timerId: string): Sim =>
  apply(sim.state, sim.rand, overunderModule.timer(mkCtx(sim.state, sim.rand), timerId));
const control = (sim: Sim, who: string, kind: 'REST' | 'SKIPEM' | 'FIFTH' | 'VOID'): Sim =>
  apply(sim.state, sim.rand, overunderModule.control(mkCtx(sim.state, sim.rand), who, kind));

const st = (sim: Sim): OverUnderState => sim.state.gameState as OverUnderState;
const subjOf = (sim: Sim): string => st(sim).subjectId;
function scribeOf(sim: Sim): string {
  const s = st(sim).scribeId;
  if (!s) throw new Error('no scribe seated');
  return s;
}
/** Bettor ids in seat order (players + imps, minus the subject). */
const bettorIds = (sim: Sim): string[] =>
  sim.state.players.filter((p) => p.id !== subjOf(sim)).map((p) => p.id);

const lock = (sim: Sim, line: number, who = scribeOf(sim)): Sim => input(sim, who, { line, lock: true });
const dial = (sim: Sim, line: number, who = scribeOf(sim)): Sim => input(sim, who, { line });
const bet = (sim: Sim, who: string, side: Bet): Sim => input(sim, who, { bet: side });
/** Everyone eligible bets — the last ballot early-advances to TRUTH. */
function allBets(sim: Sim, side: Bet | ((id: string, i: number) => Bet)): Sim {
  return bettorIds(sim).reduce((s, id, i) => bet(s, id, typeof side === 'function' ? side(id, i) : side), sim);
}
const toBet = (sim: Sim, line = 10): Sim => lock(sim, line);
const toTruth = (sim: Sim, side: Bet | ((id: string, i: number) => Bet) = 'over', line = 10): Sim =>
  allBets(toBet(sim, line), side);
const truth = (sim: Sim, n: number): Sim => input(sim, subjOf(sim), { truth: n });
/** Core hold ends (DESCEND / fire-decay): CORE_REVEAL_DONE -> next subject or done. */
const advance = (sim: Sim): Sim => fire(sim, CORE_REVEAL_DONE);

function lastRes(sim: Sim): OverUnderResolution {
  const r = st(sim).resolutions[st(sim).resolutions.length - 1];
  if (!r) throw new Error('no resolution yet');
  return r;
}
function viewAs(sim: Sim, viewer: string): OverUnderView {
  const v = overunderModule.view(mkCtx(sim.state, sim.rand), viewer);
  if (!v) throw new Error('no view');
  return v;
}
function card(id: string, exposure: Tier, timebox = 'right now', over: Partial<OverUnderCard> = {}): OverUnderCard {
  return {
    id,
    deck: 'overunder',
    text: `stat ${id}`,
    exposure,
    chaos: 3,
    register: 'deadpan',
    skeleton: `sk-${id}`,
    receiptSurface: 'Settings → Somewhere Shameful',
    timebox,
    ...over,
  };
}
/** which of `ids` appear (as JSON string values/keys) in the serialized view */
function idsIn(json: string, ids: string[]): string[] {
  return ids.filter((id) => json.includes(`"${id}"`));
}
/** strip every viewer-personal field (you*, recursively) — what remains must be identical per socket */
function scrubYou(v: unknown): unknown {
  if (Array.isArray(v)) return v.map(scrubYou);
  if (v !== null && typeof v === 'object') {
    const out: Record<string, unknown> = {};
    for (const [k, val] of Object.entries(v)) if (!k.startsWith('you')) out[k] = scrubYou(val);
    return out;
  }
  return v;
}

beforeEach(() => setOverUnderDeck(DEFAULT_OVERUNDER_DECK));

// ===== deal, subject selection & consent =====
describe('deal & subject selection', () => {
  it('start runs the $deal ceremony (subjectId attached) into DEBATE: $phase INPUT + 25s timer', () => {
    const sim = begin(5);
    expect(st(sim).sub).toBe('DEBATE');
    expect(st(sim).loop).toBe(0);
    expect(st(sim).loops).toBe(2);
    expect(st(sim).card).not.toBeNull();
    expect(sim.phase).toEqual({ k: 'INPUT', sub: 'DEBATE', deadline: NOW + DEBATE_MS });
    expect(sim.effects).toContainEqual({ k: 'SCHEDULE', timerId: debateTimerId(0, 0), atMs: NOW + DEBATE_MS });
    // the ceremony carried the subject: the core sent them the PRIVATE pre-view
    expect(sim.effects).toContainEqual(
      expect.objectContaining({ k: 'SEND', to: subjOf(sim), kind: 'preview' }),
    );
  });

  it('the ceremony writes night-dedup back: dealt card lands in usedCardIds', () => {
    const sim = begin(5);
    expect(sim.state.usedCardIds).toEqual([st(sim).card?.id]);
  });

  it('excludes night-used card ids when fresh cards remain', () => {
    setOverUnderDeck([card('ou_a', 2), card('ou_b', 2)]);
    const sim = begin(5, 0, 'test', { usedCardIds: ['ou_a'] });
    expect(st(sim).card?.id).toBe('ou_b');
  });

  it('spotlight fairness: the lowest spotlightCount eats most of the subject picks', () => {
    let low = 0;
    for (let i = 0; i < 60; i++) {
      const state = mkState(4);
      state.players = state.players.map((p) => ({ ...p, spotlightCount: p.id === 'P2' ? 0 : 9 }));
      const rand = rng(`fair-${i}`);
      const sim = apply(state, rand, overunderModule.start(mkCtx(state, rand)));
      if (subjOf(sim) === 'P2') low++;
    }
    // weights 10:1:1:1 -> P2 expected ~77% of 60; anything past 40 proves the lean
    expect(low).toBeGreaterThan(40);
  });

  it('two loops spotlight two DIFFERENT subjects', () => {
    let sim = advance(truth(toTruth(begin(5)), 25));
    expect(st(sim).loop).toBe(1);
    expect(st(sim).sub).toBe('DEBATE');
    const [first, second] = st(sim).subjectsUsed;
    expect(second).toBeDefined();
    expect(first).not.toBe(second);
    expect(subjOf(sim)).toBe(second);
  });

  it('the card never runs hotter than its NAMED subject agreed to (per-target ceiling, 4.4)', () => {
    setOverUnderDeck([card('e1', 1), card('e2', 2), card('e3', 3), card('e4', 4), card('e5', 5)]);
    const ceilings: Tier[] = [5, 1, 3, 2, 4];
    for (let i = 0; i < 25; i++) {
      const state = mkState(5);
      state.players = state.players.map((p, idx) => ({ ...p, heatCeiling: ceilings[idx] as Tier }));
      const rand = rng(`ceil-${i}`);
      const sim = apply(state, rand, overunderModule.start(mkCtx(state, rand)));
      const subject = state.players.find((p) => p.id === subjOf(sim));
      expect(st(sim).card?.exposure).toBeLessThanOrEqual(subject?.heatCeiling ?? 0);
    }
  });

  it('fresh meat only ever draws first-impression stats (personal history excluded)', () => {
    setOverUnderDeck([card('live', 2, 'right now'), card('week', 2, 'this week'), card('month', 2, 'this month')]);
    for (let i = 0; i < 15; i++) {
      const state = mkState(4);
      state.players = state.players.map((p) => ({ ...p, freshMeat: true }));
      const rand = rng(`fm-${i}`);
      const sim = apply(state, rand, overunderModule.start(mkCtx(state, rand)));
      expect(st(sim).card?.timebox).toBe('right now');
    }
    // control group: a crew with history CAN draw history
    let sawHistory = false;
    for (let i = 0; i < 15 && !sawHistory; i++) {
      const sim = begin(4, 0, `vet-${i}`);
      sawHistory = isPersonalHistory(st(sim).card as OverUnderCard);
    }
    expect(sawHistory).toBe(true);
  });

  it('isPersonalHistory: only the first-impression timeboxes survive fresh meat', () => {
    expect(FIRST_IMPRESSION_TIMEBOXES).toContain('right now');
    expect(FIRST_IMPRESSION_TIMEBOXES).toContain('today');
    expect(isPersonalHistory(card('x', 2, 'right now'))).toBe(false);
    expect(isPersonalHistory(card('x', 2, 'today'))).toBe(false);
    expect(isPersonalHistory(card('x', 2, 'this week'))).toBe(true);
    expect(isPersonalHistory(card('x', 2, 'since the breakup'))).toBe(true);
  });

  it('a candidate with NO legal cards is skipped — consent beats fairness', () => {
    setOverUnderDeck([card('e3', 3)]);
    for (let i = 0; i < 20; i++) {
      const state = mkState(4);
      state.players = state.players.map((p) => ({
        ...p,
        heatCeiling: (p.id === 'P1' ? 1 : 5) as Tier,
        spotlightCount: p.id === 'P1' ? 0 : 9, // fairness screams P1; consent says never
      }));
      const rand = rng(`skip-${i}`);
      const sim = apply(state, rand, overunderModule.start(mkCtx(state, rand)));
      expect(subjOf(sim)).not.toBe('P1');
      expect(st(sim).card?.id).toBe('e3');
    }
  });

  it('prefers cards at or under the circle rung when any exist', () => {
    setOverUnderDeck([card('mild', 1), card('hot', 4)]);
    for (let i = 0; i < 10; i++) {
      const sim = begin(5, 0, `rung-${i}`, { arc: [{ ...CIRCLE, rung: 2 }] });
      expect(st(sim).card?.id).toBe('mild');
    }
  });

  it('setOverUnderDeck rejects an empty deck; the placeholder deck is receipt-legal', () => {
    expect(() => setOverUnderDeck([])).toThrow();
    for (const c of DEFAULT_OVERUNDER_DECK) {
      expect(c.deck).toBe('overunder');
      expect(c.chaos).toBeGreaterThanOrEqual(3);
      expect(c.receiptSurface.length).toBeGreaterThan(0);
      expect(c.timebox.length).toBeGreaterThan(0);
    }
    // fresh meat must have something to draw even from the stub deck
    expect(DEFAULT_OVERUNDER_DECK.some((c) => !isPersonalHistory(c))).toBe(true);
  });
});

// ===== line debate & the scribe =====
describe('line debate & scribe', () => {
  it('the scribe is the next seat after the subject (wrapping, never the subject)', () => {
    for (let i = 0; i < 10; i++) {
      const sim = begin(5, 0, `seat-${i}`);
      const subject = sim.state.players.find((p) => p.id === subjOf(sim)) as Player;
      const expected = sim.state.players.find((p) => p.seat === (subject.seat + 1) % 5) as Player;
      expect(scribeOf(sim)).toBe(expected.id);
    }
  });

  it('only the scribe holds the dial: everyone else’s line input is inert', () => {
    const sim = begin(5);
    const notScribe = sim.state.players.map((p) => p.id).filter((id) => id !== scribeOf(sim));
    for (const id of notScribe) {
      const next = input(sim, id, { line: 10, lock: true });
      expect(st(next)).toBe(st(sim));
    }
  });

  it('malformed dial payloads die silently', () => {
    const sim = begin(5);
    for (const bad of [null, 42, { line: -1 }, { line: 3.5 }, { line: 'ten' }, { line: Number.MAX_SAFE_INTEGER }, {}]) {
      const next = input(sim, scribeOf(sim), bad);
      expect(st(next)).toBe(st(sim));
    }
  });

  it('dial updates broadcast the provisional line (the argument is out loud anyway)', () => {
    const sim = dial(begin(5), 7);
    expect(st(sim).sub).toBe('DEBATE');
    expect(st(sim).line).toBe(7);
    expect((viewAs(sim, 'P0') as OverUnderDebateView).line).toBe(7);
    expect(sim.last.effects).toEqual([{ k: 'BROADCAST' }]);
  });

  it('scribe lock opens the 12s BET and cancels the debate timer', () => {
    const sim = lock(begin(5), 10);
    expect(st(sim).sub).toBe('BET');
    expect(st(sim).line).toBe(10);
    expect(sim.phase).toEqual({ k: 'INPUT', sub: 'BET', deadline: NOW + BET_MS });
    expect(sim.effects).toContainEqual({ k: 'CANCEL', timerId: debateTimerId(0, 0) });
    expect(sim.effects).toContainEqual({ k: 'SCHEDULE', timerId: betTimerId(0, 0), atMs: NOW + BET_MS });
  });

  it('the debate deadline locks the dial where the argument left it', () => {
    const sim = fire(dial(begin(5), 13), debateTimerId(0, 0));
    expect(st(sim).sub).toBe('BET');
    expect(st(sim).line).toBe(13);
  });

  it('deadline with an untouched dial passes the dial to the next seat with a fresh 25s', () => {
    const sim = begin(5);
    const before = scribeOf(sim);
    const next = fire(sim, debateTimerId(0, 0));
    expect(st(next).sub).toBe('DEBATE');
    expect(scribeOf(next)).not.toBe(before);
    expect(st(next).debateReassigns).toBe(1);
    expect(next.phase).toEqual({ k: 'INPUT', sub: 'DEBATE', deadline: NOW + DEBATE_MS });
    const subject = next.state.players.find((p) => p.id === subjOf(next)) as Player;
    const oldScribe = next.state.players.find((p) => p.id === before) as Player;
    const expectSeat = next.state.players
      .filter((p) => p.id !== subject.id)
      .sort((a, b) => a.seat - b.seat)
      .find((p) => p.seat > oldScribe.seat);
    expect(scribeOf(next)).toBe((expectSeat ?? next.state.players.filter((p) => p.id !== subject.id)[0])?.id);
  });

  it('a DISCONNECTED scribe is skipped at reassignment (next living seat takes the dial)', () => {
    let sim = begin(6);
    const first = scribeOf(sim);
    const subject = sim.state.players.find((p) => p.id === subjOf(sim)) as Player;
    const ring = sim.state.players.filter((p) => p.id !== subject.id).sort((a, b) => a.seat - b.seat);
    const firstSeat = (ring.find((p) => p.id === first) as Player).seat;
    const after = [...ring.filter((p) => p.seat > firstSeat), ...ring.filter((p) => p.seat <= firstSeat)];
    const secondLiving = after.find((p) => p.id !== first && p.id !== after[0]?.id) as Player; // seat after the dead one
    // the would-be next scribe is gone: the dial must skip them
    sim = {
      ...sim,
      state: {
        ...sim.state,
        players: sim.state.players.map((p) => (p.id === after[0]?.id ? { ...p, connected: false } : p)),
      },
    };
    const next = fire(sim, debateTimerId(0, 0));
    expect(scribeOf(next)).toBe(secondLiving.id);
    expect(scribeOf(next)).not.toBe(after[0]?.id);
  });

  it('scribe seat-lapse mid-debate (core VOID convention) reassigns instead of voiding', () => {
    let sim = begin(5);
    const fled = scribeOf(sim);
    sim = {
      ...sim,
      state: {
        ...sim.state,
        players: sim.state.players.map((p) => (p.id === fled ? { ...p, connected: false } : p)),
      },
    };
    const next = control(sim, fled, 'VOID'); // handleSeatLapse routes exactly this
    expect(st(next).sub).toBe('DEBATE'); // loop survives
    expect(scribeOf(next)).not.toBe(fled);
    expect(next.phase).toEqual({ k: 'INPUT', sub: 'DEBATE', deadline: NOW + DEBATE_MS });
  });

  it('once every seat has shrugged at the dial, the loop dies loud (void, scoreless)', () => {
    let sim = begin(3); // ring of 2 scribes: one reassign allowed, then the void
    sim = fire(sim, debateTimerId(0, 0));
    expect(st(sim).sub).toBe('DEBATE');
    sim = fire(sim, debateTimerId(0, 0));
    expect(st(sim).sub).toBe('REVEAL');
    expect(lastRes(sim).voided).toBe(true);
    expect(lastRes(sim).line).toBeNull();
    expect(sim.last.scores).toEqual({});
    expect(sim.effects).toContainEqual({ k: 'AUDIO', sting: 'void' });
  });
});

// ===== bets =====
describe('bets', () => {
  it('the subject NEVER bets on their own number', () => {
    const sim = toBet(begin(5));
    const next = bet(sim, subjOf(sim), 'over');
    expect(st(next)).toBe(st(sim));
  });

  it('ghosts and malformed payloads are inert', () => {
    const sim = toBet(begin(5));
    for (const [who, payload] of [
      ['GHOST', { bet: 'over' }],
      ['P0', { bet: 'sideways' }],
      ['P0', null],
      ['P0', {}],
    ] as const) {
      const next = input(sim, who, payload);
      expect(st(next)).toBe(st(sim));
    }
  });

  it('re-bet before the deadline replaces the ballot (last one wins)', () => {
    const sim = toBet(begin(5));
    const bettor = bettorIds(sim)[0] as string;
    const next = bet(bet(sim, bettor, 'over'), bettor, 'under');
    expect(st(next).bets[bettor]).toBe('under');
    expect((viewAs(next, bettor) as OverUnderBetView).youBet).toBe('under');
    expect((viewAs(next, bettor) as OverUnderBetView).betCount).toBe(1);
  });

  it('imps bet too — their ballots count and their weight colors the split at 0.5', () => {
    let sim = toBet(begin(4, 1));
    for (const id of bettorIds(sim)) sim = bet(sim, id, id.startsWith('I') ? 'under' : 'over');
    expect(st(sim).sub).toBe('TRUTH'); // imp ballot completed the room
    sim = truth(sim, 25);
    expect(lastRes(sim).split).toEqual({ over: 3, under: 0.5 });
  });

  it('all bettors in -> early-advance to TRUTH, bet timer cancelled', () => {
    const sim = toTruth(begin(5));
    expect(st(sim).sub).toBe('TRUTH');
    expect(sim.effects).toContainEqual({ k: 'CANCEL', timerId: betTimerId(0, 0) });
  });

  it('the bet deadline auto-abstains the silent — the game never waits', () => {
    let sim = toBet(begin(5));
    const bettor = bettorIds(sim)[0] as string;
    sim = bet(sim, bettor, 'over');
    sim = fire(sim, betTimerId(0, 0));
    expect(st(sim).sub).toBe('TRUTH');
    sim = truth(sim, 25);
    expect(lastRes(sim).winners).toEqual([bettor]);
    expect(lastRes(sim).split).toEqual({ over: 1, under: 0 });
  });

  it('bets after the phase closed are dropped', () => {
    const sim = toTruth(begin(5));
    const bettor = bettorIds(sim)[0] as string;
    const next = input(sim, bettor, { bet: 'under' });
    expect(st(next)).toBe(st(sim));
  });
});

// ===== TRUTH: the blocking terminal machine (4.7) =====
describe('truth blocking (4.7)', () => {
  it('TRUTH is PAUSED: INPUT directive deadline null; only shame timers are armed', () => {
    const sim = toTruth(begin(5));
    expect(sim.phase).toEqual({ k: 'INPUT', sub: 'TRUTH', deadline: null });
    const step = sim.last.effects.filter((e): e is Extract<Effect, { k: 'SCHEDULE' }> => e.k === 'SCHEDULE');
    expect(step).toEqual([
      { k: 'SCHEDULE', timerId: blockingGraceTimer(truthKey(0, 0)), atMs: NOW + BLOCKING_GRACE_MS },
      { k: 'SCHEDULE', timerId: blockingPitTimer(truthKey(0, 0)), atMs: NOW + BLOCKING_PIT_MS },
    ]);
  });

  it('12s grace -> WAITING_ON the subject; 30s -> the pit buttons open', () => {
    let sim = toTruth(begin(5));
    sim = fire(sim, blockingGraceTimer(truthKey(0, 0)));
    expect(sim.phase).toEqual({ k: 'WAITING_ON', who: subjOf(sim), since: NOW });
    sim = fire(sim, blockingPitTimer(truthKey(0, 0)));
    expect(st(sim).blocking?.pitOpen).toBe(true);
    expect((viewAs(sim, 'P0') as OverUnderTruthView).pitOpen).toBe(true);
  });

  it('pit votes before the pit opens are inert; the subject can never pit-vote', () => {
    let sim = toTruth(begin(5));
    const juror = bettorIds(sim)[0] as string;
    expect(st(input(sim, juror, { pit: 'pit' }))).toBe(st(sim));
    sim = fire(sim, blockingPitTimer(truthKey(0, 0)));
    expect(st(input(sim, subjOf(sim), { pit: 'pit' }))).toBe(st(sim));
  });

  it('>=60% weighted PIT voids the loop scoreless; DRAG votes never void', () => {
    let sim = toTruth(begin(5)); // 4 jurors, weight 1 each -> need 2.4 -> 3 pit votes
    sim = fire(sim, blockingPitTimer(truthKey(0, 0)));
    const jurors = bettorIds(sim);
    sim = input(sim, jurors[0] as string, { pit: 'pit' });
    sim = input(sim, jurors[1] as string, { pit: 'drag' });
    sim = input(sim, jurors[2] as string, { pit: 'pit' }); // 2/4 = 50%
    expect(st(sim).sub).toBe('TRUTH');
    sim = input(sim, jurors[3] as string, { pit: 'pit' }); // 3/4 = 75%
    expect(st(sim).sub).toBe('REVEAL');
    expect(lastRes(sim).voided).toBe(true);
    expect(lastRes(sim).truth).toBeNull();
    expect(sim.last.scores).toEqual({});
    expect(sim.effects).toContainEqual({ k: 'AUDIO', sting: 'void' });
    expect(sim.phase).toEqual({ k: 'REVEAL' }); // the void still holds — the room hears the sting
  });

  it('imp jurors weigh 0.5 in the pit', () => {
    let sim = toTruth(begin(3, 1)); // jurors: 2 players + 1 imp = 2.5 -> need 1.5
    sim = fire(sim, blockingPitTimer(truthKey(0, 0)));
    const [p1, p2] = bettorIds(sim).filter((id) => id.startsWith('P'));
    sim = input(sim, 'I0', { pit: 'pit' }); // 0.5 < 1.5
    expect(st(sim).sub).toBe('TRUTH');
    sim = input(sim, p1 as string, { pit: 'pit' }); // 1.5 >= 1.5
    expect(st(sim).sub).toBe('REVEAL');
    expect(lastRes(sim).voided).toBe(true);
    expect(p2).toBeDefined();
  });

  it('PLEAD THE FIFTH: subject only — voids the loop scoreless, night moves on', () => {
    let sim = toTruth(begin(5));
    const notSubject = bettorIds(sim)[0] as string;
    expect(st(control(sim, notSubject, 'FIFTH'))).toBe(st(sim)); // only the witness may fold
    sim = control(sim, subjOf(sim), 'FIFTH');
    expect(st(sim).sub).toBe('REVEAL');
    expect(lastRes(sim).voided).toBe(true);
    expect(lastRes(sim).truth).toBeNull();
    expect(sim.last.scores).toEqual({});
    expect(sim.effects).toContainEqual({ k: 'AUDIO', sting: 'fifth' });
    // the coward costs one loop, never the night: subject 2 deals next
    const next = advance(sim);
    expect(st(next).loop).toBe(1);
    expect(st(next).sub).toBe('DEBATE');
    expect(subjOf(next)).not.toBe(subjOf(sim));
  });

  it('host VOID kills a blocked loop through the terminal machine', () => {
    const sim = control(toTruth(begin(5)), 'P0', 'VOID');
    expect(st(sim).sub).toBe('REVEAL');
    expect(lastRes(sim).voided).toBe(true);
    expect(sim.effects).toContainEqual({ k: 'AUDIO', sting: 'void' });
    expect(sim.effects).toContainEqual({ k: 'CANCEL', timerId: blockingGraceTimer(truthKey(0, 0)) });
    expect(sim.effects).toContainEqual({ k: 'CANCEL', timerId: blockingPitTimer(truthKey(0, 0)) });
  });

  it("subject seat-lapse auto-voids: THE WITNESS FLED", () => {
    const sim0 = toTruth(begin(5));
    const sim = control(sim0, subjOf(sim0), 'VOID'); // core's seat-lapse convention (non-host id)
    expect(st(sim).sub).toBe('REVEAL');
    expect(lastRes(sim).voided).toBe(true);
    expect(sim.effects).toContainEqual({ k: 'AUDIO', sting: 'fled' });
  });

  it('subject fleeing during DEBATE or BET also voids — no witness, no number, ever', () => {
    const debate = begin(5);
    const d = control(debate, subjOf(debate), 'VOID');
    expect(lastRes(d).voided).toBe(true);
    expect(d.effects).toContainEqual({ k: 'CANCEL', timerId: debateTimerId(0, 0) });
    const betting = toBet(begin(5));
    const b = control(betting, subjOf(betting), 'VOID');
    expect(lastRes(b).voided).toBe(true);
    expect(b.effects).toContainEqual({ k: 'CANCEL', timerId: betTimerId(0, 0) });
  });

  it('a real receipt resolves: number vs line, blocking timers cancelled, boom', () => {
    let sim = toTruth(begin(5), (id, i) => (i % 2 === 0 ? 'over' : 'under'), 10);
    sim = truth(sim, 25);
    expect(st(sim).sub).toBe('REVEAL');
    expect(sim.phase).toEqual({ k: 'REVEAL' });
    const res = lastRes(sim);
    expect(res.truth).toBe(25);
    expect(res.line).toBe(10);
    expect(res.voided).toBe(false);
    expect(res.unverified).toBe(false);
    expect(sim.effects).toContainEqual({ k: 'AUDIO', sting: 'boom' });
    expect(sim.effects).toContainEqual({ k: 'CANCEL', timerId: blockingGraceTimer(truthKey(0, 0)) });
    expect(sim.effects).toContainEqual({ k: 'CANCEL', timerId: blockingPitTimer(truthKey(0, 0)) });
  });

  it('scribe-downgrade: the HOST types the verbal claim — flagged UNVERIFIED, no receipt bonus', () => {
    const sim0 = beginSuchThat(5, (s) => subjOf(s) !== 'P0'); // host must not be the subject here
    let sim = toTruth(sim0, 'over', 10);
    sim = input(sim, 'P0', { claim: 25 });
    expect(st(sim).sub).toBe('REVEAL');
    const res = lastRes(sim);
    expect(res.unverified).toBe(true);
    expect(res.truth).toBe(25);
    expect((viewAs(sim, 'P3') as OverUnderRevealView).unverified).toBe(true);
    // no receipt bonus: the subject earns NOTHING from a host-relayed number
    expect(sim.last.scores?.[subjOf(sim)]).toBeUndefined();
  });

  it('a claim from a non-host, or a number from a non-subject, is inert (never a fabricated truth)', () => {
    const sim0 = beginSuchThat(5, (s) => subjOf(s) !== 'P0');
    const sim = toTruth(sim0);
    const stranger = bettorIds(sim).find((id) => id !== 'P0') as string;
    expect(st(input(sim, stranger, { claim: 5 }))).toBe(st(sim));
    expect(st(input(sim, stranger, { truth: 5 }))).toBe(st(sim));
    expect(st(input(sim, subjOf(sim), { truth: -3 }))).toBe(st(sim)); // receipts are counts
  });
});

// ===== resolution & scoring (4.6) =====
describe('resolution & scoring', () => {
  it('OVER wins: +2 correct bet, +1 participation; wrong side keeps participation only; subject +1 receipt bonus', () => {
    let sim = toTruth(begin(5), (id, i) => (i < 2 ? 'over' : 'under'), 10);
    const [w1, w2, l1, l2] = bettorIds(sim);
    sim = truth(sim, 99);
    expect(lastRes(sim).winners.sort()).toEqual([w1, w2].sort());
    expect(sim.last.scores).toEqual({
      [w1 as string]: 3,
      [w2 as string]: 3,
      [l1 as string]: 1,
      [l2 as string]: 1,
      [subjOf(sim)]: 1, // the receipt bonus: producing real receipts IS participation
    });
  });

  it('UNDER wins symmetrically', () => {
    let sim = toTruth(begin(4), (id, i) => (i === 0 ? 'under' : 'over'), 10);
    const [winner] = bettorIds(sim);
    sim = truth(sim, 2);
    expect(lastRes(sim).winners).toEqual([winner]);
    expect(sim.last.scores?.[winner as string]).toBe(3);
  });

  it('PUSH: number == line -> EVERYONE +1 (subject and imps included), bettors keep participation', () => {
    let sim = toTruth(begin(4, 1), (id, i) => (i % 2 === 0 ? 'over' : 'under'), 10);
    sim = truth(sim, 10);
    const res = lastRes(sim);
    expect(res.push).toBe(true);
    expect(res.winners).toEqual([]);
    const expected: Record<string, number> = {};
    for (const id of bettorIds(sim)) expected[id] = 2; // push +1, participation +1
    expected[subjOf(sim)] = 2; // push +1, receipt bonus +1
    expect(sim.last.scores).toEqual(expected);
  });

  it('participation pays once per CIRCLE, not once per loop', () => {
    let sim = truth(toTruth(begin(4), 'over', 10), 25); // loop 0: everyone banks participation
    sim = advance(sim);
    const s2 = subjOf(sim);
    let loop1 = toTruth(sim, (id, i) => (i === 0 ? 'under' : 'over'), 5);
    const [winner] = bettorIds(loop1);
    loop1 = truth(loop1, 3);
    // winner already participated in loop 0 (as bettor or as loop-0 subject) -> bare +2;
    // losers and the loop-1 subject add NOTHING new
    expect(loop1.last.scores).toEqual({ [winner as string]: 2 });
    expect(subjOf(loop1)).toBe(s2);
  });

  it('scores land in GameStep.scores pre-multiplier (core owns finale x3)', () => {
    const sim = truth(toTruth(begin(4)), 25);
    expect(sim.last.scores).toBeDefined();
    expect(Object.values(sim.last.scores ?? {}).every((v) => Number.isInteger(v) && v > 0)).toBe(true);
  });

  it('a voided loop pays 0 to all — no participation, no receipt bonus, nothing', () => {
    const sim = control(toTruth(begin(5)), subjOf(toTruth(begin(5))), 'FIFTH');
    expect(sim.last.scores).toEqual({});
  });
});

// ===== view redaction (5.3 + 3.4) =====
describe('view redaction', () => {
  it('during the DEAL ceremony the view is null — the stat never pre-leaks', () => {
    const state = mkState(5, 0, {
      gameState: {
        sub: 'DEAL',
        loop: 0,
        loops: 2,
        subjectId: 'P1',
        scribeId: null,
        card: null,
        line: null,
        debateDeadline: 0,
        debateReassigns: 0,
        bets: {},
        betDeadline: 0,
        blocking: null,
        resolutions: [],
        participation: [],
        subjectsUsed: ['P1'],
      } satisfies OverUnderState,
    });
    expect(overunderModule.view(mkCtx(state, rng('x')), 'P0')).toBeNull();
  });

  it("the subject's stat is public on EVERY phone once the ceremony ends", () => {
    const sim = begin(5);
    for (const p of sim.state.players) {
      const v = viewAs(sim, p.id) as OverUnderDebateView;
      expect(v.card.text).toContain('{NAME}');
      expect(v.card.receiptSurface.length).toBeGreaterThan(0);
      expect(v.subjectId).toBe(subjOf(sim));
      expect(v.youAreScribe).toBe(p.id === scribeOf(sim));
    }
  });

  it('BET frames: bets invisible — every viewer sees the identical public frame + only their own ballot', () => {
    let sim = toBet(begin(5), 10);
    const [b1, , b3] = bettorIds(sim);
    sim = bet(bet(sim, b1 as string, 'over'), b3 as string, 'under');
    const ids = sim.state.players.map((p) => p.id);
    const frames = ids.map((id) => viewAs(sim, id));
    for (const f of frames) {
      const json = JSON.stringify(f);
      expect(json).not.toContain('"bets"');
      // the only names in a BET frame are the subject and the scribe — never a bettor
      expect(
        idsIn(json, ids).every((id) => id === subjOf(sim) || id === scribeOf(sim)),
      ).toBe(true);
    }
    expect(new Set(frames.map((f) => JSON.stringify(scrubYou(f)))).size).toBe(1);
    expect((frames[ids.indexOf(b1 as string)] as OverUnderBetView).youBet).toBe('over');
    expect((frames[ids.indexOf(b3 as string)] as OverUnderBetView).youBet).toBe('under');
    expect((frames[ids.indexOf(subjOf(sim))] as OverUnderBetView).youBet).toBeNull();
    expect((frames[ids.indexOf(subjOf(sim))] as OverUnderBetView).youAreSubject).toBe(true);
    expect((frames[ids.indexOf(b1 as string)] as OverUnderBetView).betCount).toBe(2);
  });

  it('TRUTH frames: still no bets, no truth field, pit ballots reduced to your own choice', () => {
    let sim = toTruth(begin(5), (id, i) => (i % 2 === 0 ? 'over' : 'under'));
    sim = fire(sim, blockingPitTimer(truthKey(0, 0)));
    const juror = bettorIds(sim)[0] as string;
    sim = input(sim, juror, { pit: 'pit' });
    const ids = sim.state.players.map((p) => p.id);
    const frames = ids.map((id) => viewAs(sim, id) as OverUnderTruthView);
    for (const f of frames) {
      const json = JSON.stringify(f);
      expect(f.sub).toBe('TRUTH');
      expect(json).not.toContain('"bets"');
      expect(json).not.toContain('"truth"'); // the number does not exist yet, anywhere
      expect(json).not.toContain('"pitVotes"');
      expect(
        idsIn(json, ids).every((id) => id === subjOf(sim) || id === scribeOf(sim)),
      ).toBe(true);
    }
    expect(new Set(frames.map((f) => JSON.stringify(scrubYou(f)))).size).toBe(1);
    expect(frames[ids.indexOf(juror)]?.youPitVoted).toBe('pit');
    expect(frames[ids.indexOf(subjOf(sim))]?.youPitVoted).toBeNull();
  });

  it('REVEAL: the true number, winners and weighted split go public — and only here', () => {
    let sim = toTruth(begin(4, 1), (id) => (id.startsWith('I') ? 'under' : 'over'), 10);
    sim = truth(sim, 4242);
    for (const p of sim.state.players) {
      const v = viewAs(sim, p.id) as OverUnderRevealView;
      expect(v.truth).toBe(4242);
      expect(v.line).toBe(10);
      expect(v.winners.sort()).toEqual(bettorIds(sim).filter((id) => id.startsWith('P')).sort());
      expect(v.split).toEqual({ over: 3, under: 0.5 });
      expect(v.youBet).toBe(st(sim).bets[p.id] ?? null);
    }
  });

  it('a voided reveal carries no number and buries the bets forever', () => {
    let sim = toTruth(begin(5), 'over');
    sim = control(sim, subjOf(sim), 'FIFTH');
    const v = viewAs(sim, 'P0') as OverUnderRevealView;
    expect(v.voided).toBe(true);
    expect(v.truth).toBeNull();
    expect(v.winners).toEqual([]);
    expect(v.split).toEqual({ over: 0, under: 0 });
  });

  it('no frame in any phase carries a heatCeiling or raw score delta', () => {
    let sim = toTruth(begin(5), 'over', 10);
    const phases: Sim[] = [begin(5), toBet(begin(5)), sim, truth(sim, 25)];
    for (const s of phases) {
      for (const p of s.state.players) {
        const json = JSON.stringify(viewAs(s, p.id));
        expect(json).not.toContain('heatCeiling');
        expect(json).not.toContain('"scores"');
      }
    }
  });
});

// ===== stale timers & foreign noise =====
describe('stale timers', () => {
  it('CORE_REVEAL_DONE while the loop is live never skips a phase', () => {
    for (const sim of [begin(5), toBet(begin(5)), toTruth(begin(5))]) {
      const next = fire(sim, CORE_REVEAL_DONE);
      expect(st(next)).toBe(st(sim));
    }
  });

  it('a stale CORE_DEALT after the debate opened is inert', () => {
    const sim = begin(5);
    expect(st(fire(sim, CORE_DEALT))).toBe(st(sim));
  });

  it('wrong-loop and wrong-circle timers are inert', () => {
    const sim = begin(5);
    for (const id of [debateTimerId(0, 1), debateTimerId(1, 0), betTimerId(0, 0)]) {
      expect(st(fire(sim, id))).toBe(st(sim));
    }
  });

  it('blocking timers for a resolved loop are inert', () => {
    const sim = truth(toTruth(begin(5)), 25);
    for (const id of [blockingGraceTimer(truthKey(0, 0)), blockingPitTimer(truthKey(0, 0))]) {
      expect(st(fire(sim, id))).toBe(st(sim));
    }
  });

  it('REST and SKIPEM are inert — over/under has no performer', () => {
    const sim = toBet(begin(5));
    for (const kind of ['REST', 'SKIPEM'] as const) {
      expect(st(control(sim, 'P0', kind))).toBe(st(sim));
    }
  });

  it('non-host, non-lapse VOIDs are inert (a random bettor cannot kill the loop)', () => {
    const sim = toTruth(begin(5));
    const bystander = bettorIds(sim).find((id) => id !== 'P0') as string;
    expect(st(control(sim, bystander, 'VOID'))).toBe(st(sim));
  });
});

// ===== the full circle, module-level =====
describe('full 2-subject circle', () => {
  it('deal -> debate -> bet -> truth -> reveal, twice, with distinct subjects and fresh cards, then done', () => {
    let sim = begin(6);
    const seen: { subject: string; card: string }[] = [];
    for (let loop = 0; loop < 2; loop++) {
      expect(st(sim).loop).toBe(loop);
      expect(st(sim).sub).toBe('DEBATE');
      seen.push({ subject: subjOf(sim), card: (st(sim).card as OverUnderCard).id });
      sim = truth(toTruth(sim, (id, i) => (i % 2 === 0 ? 'over' : 'under'), 10), 25);
      expect(st(sim).sub).toBe('REVEAL');
      expect(sim.last.done).not.toBe(true);
      sim = advance(sim);
    }
    expect(sim.last.done).toBe(true);
    expect(st(sim).resolutions).toHaveLength(2);
    expect(seen[0]?.subject).not.toBe(seen[1]?.subject);
    expect(seen[0]?.card).not.toBe(seen[1]?.card); // night-dedup: subject 2 gets a fresh stat
    expect(sim.state.usedCardIds).toEqual(seen.map((s) => s.card));
  });

  it('a voided first loop still hands subject 2 a clean second loop', () => {
    let sim = control(toTruth(begin(4)), 'P0', 'VOID');
    sim = advance(sim);
    expect(st(sim).loop).toBe(1);
    sim = truth(toTruth(sim, 'over', 10), 25);
    expect(lastRes(sim).voided).toBe(false);
    expect(advance(sim).last.done).toBe(true);
  });
});

// ===== through the REAL core (engine.reduce) — the seam test =====
describe('overunder through reduce() (core $deal/$phase/blocking integration)', () => {
  registerModule(overunderModule); // the integrator's line — tests need the registry wired

  const T = 100_000;
  function nightDriver(n: number): Driver {
    const d = new Driver(mkNightRoom(mkSpec('overunder', { loops: 2, rung: 5 }), mkPlayers(n)));
    d.pending.set('intro:1', T); // the intro alarm the core would have armed
    return d;
  }
  const gs = (d: Driver): OverUnderState => d.state.gameState as OverUnderState;
  const score = (d: Driver, id: string): number => d.state.players.find((p) => p.id === id)?.score ?? -1;
  /** intro -> ceremony -> DEBATE, returning subject/scribe/bettors for this loop. */
  function toDebate(d: Driver): { subject: string; scribe: string; bettors: string[] } {
    d.runUntil((s) => s.phase.k === 'INPUT');
    const state = gs(d);
    expect(d.state.phase).toMatchObject({ k: 'INPUT', sub: 'DEBATE' });
    return {
      subject: state.subjectId,
      scribe: state.scribeId as string,
      bettors: d.state.players.filter((p) => p.id !== state.subjectId).map((p) => p.id),
    };
  }
  function toTruthViaCore(d: Driver, at: number, sides: (id: string, i: number) => Bet, line = 10): { subject: string; scribe: string; bettors: string[] } {
    const cast = toDebate(d);
    d.dispatch({ t: 'INPUT', id: cast.scribe, payload: { line, lock: true }, at });
    expect(d.state.phase).toMatchObject({ k: 'INPUT', sub: 'BET' });
    cast.bettors.forEach((id, i) =>
      d.dispatch({ t: 'INPUT', id, payload: { bet: sides(id, i) }, at: at + 500 + i }),
    );
    expect(d.state.phase).toMatchObject({ k: 'INPUT', sub: 'TRUTH', deadline: null });
    return cast;
  }

  it('full 2-subject circle: ceremony with private pre-view, provable pause, scores, LADDER', () => {
    const d = nightDriver(4);
    d.fireNext(); // intro -> fixed private spotlight assignment
    expect(d.state.phase).toEqual({ k: 'DEAL', circle: 0 });
    const assignedEffect = d.log.flatMap((l) => l.effects).find((e) => e.k === 'SEND' && e.kind === 'spotlight');
    const assignedTo = assignedEffect?.k === 'SEND' ? assignedEffect.to : undefined;

    const cast0 = toTruthViaCore(d, T + 15_000, (_, i) => (i === 0 ? 'under' : 'over'), 10);
    const subject0 = cast0.subject;
    expect(assignedTo).toBe(subject0);
    // after assignment locks, the card pre-view also travels only to the subject
    expect(d.log.flatMap((l) => l.effects)).toContainEqual(
      expect.objectContaining({ k: 'SEND', to: subject0, kind: 'preview' }),
    );
    expect(cast0.subject).toBe(subject0);
    expect(d.state.usedCardIds).toHaveLength(1);
    // the pause is real: nothing pending can auto-resolve the truth — only shame timers
    expect([...d.pending.keys()].every((id) => id.startsWith('blk:'))).toBe(true);

    d.fireNext(); // 12s grace -> the room publicly waits
    expect(d.state.phase).toMatchObject({ k: 'WAITING_ON', who: subject0 });

    d.dispatch({ t: 'INPUT', id: subject0, payload: { truth: 25 }, at: T + 30_000 }); // receipt in hand
    expect(d.state.phase.k).toBe('REVEAL');
    const epoch = d.state.epoch;
    expect(d.pending.has(`reveal:flip:${epoch}`)).toBe(true); // synced 3-2-1 beat armed
    // scores through the core: 2 over-winners +3, 1 under-loser +1, subject +1 receipt bonus
    const [loser, ...winners] = cast0.bettors;
    for (const w of winners) expect(score(d, w)).toBe(3);
    expect(score(d, loser as string)).toBe(1);
    expect(score(d, subject0)).toBe(1);

    d.dispatch({ t: 'DESCEND', id: 'P0', at: T + 32_000 }); // host ends the hold -> subject 2 deals
    const before = Object.fromEntries(d.state.players.map((p) => [p.id, p.score]));
    const cast1 = toTruthViaCore(d, T + 60_000, (_, i) => (i === 0 ? 'under' : 'over'), 8);
    expect(cast1.subject).not.toBe(subject0); // two subjects per circle, never the same
    expect(d.state.usedCardIds).toHaveLength(2);
    d.dispatch({ t: 'INPUT', id: cast1.subject, payload: { truth: 3 }, at: T + 90_000 }); // UNDER
    expect(d.state.phase.k).toBe('REVEAL');
    // only the lone under-bettor gains (+2, participation spent in loop 0)
    for (const p of d.state.players) {
      const delta = p.score - (before[p.id] as number);
      expect(delta).toBe(p.id === cast1.bettors[0] ? 2 : 0);
    }

    d.dispatch({ t: 'DESCEND', id: 'P0', at: T + 92_000 });
    expect(d.state.phase.k).toBe('LADDER'); // circle complete
    expect(gs(d).resolutions.map((r) => r.voided)).toEqual([false, false]);
  });

  it('pit vote through reduce(): the room feeds a frozen witness to the pit', () => {
    const d = nightDriver(4);
    d.fireNext();
    const { subject, scribe, bettors } = toDebate(d);
    d.dispatch({ t: 'INPUT', id: scribe, payload: { line: 10, lock: true }, at: T + 15_000 });
    d.dispatch({ t: 'INPUT', id: bettors[0] as string, payload: { bet: 'over' }, at: T + 16_000 });
    d.fireNext(); // bet deadline -> TRUTH (blocking)
    expect(d.state.phase).toMatchObject({ k: 'INPUT', sub: 'TRUTH', deadline: null });
    d.fireNext(); // grace -> WAITING_ON
    d.fireNext(); // 30s -> pit buttons
    d.dispatch({ t: 'INPUT', id: bettors[0] as string, payload: { pit: 'pit' }, at: T + 60_000 });
    expect(d.state.phase.k).not.toBe('REVEAL'); // 1/3 jurors < 60%
    d.dispatch({ t: 'INPUT', id: bettors[1] as string, payload: { pit: 'pit' }, at: T + 61_000 });
    expect(d.state.phase.k).toBe('REVEAL'); // 2/3 -> void
    expect(gs(d).resolutions[0]).toMatchObject({ voided: true, truth: null, subjectId: subject });
    expect(d.state.players.every((p) => p.score === 0)).toBe(true);
  });

  it('PLEAD_FIFTH through reduce(): scoreless void, fifth counted for Judgment', () => {
    const d = nightDriver(4);
    d.fireNext();
    const cast = toTruthViaCore(d, T + 15_000, () => 'over');
    d.dispatch({ t: 'PLEAD_FIFTH', id: cast.subject, at: T + 25_000 });
    expect(d.state.phase.k).toBe('REVEAL');
    expect(gs(d).resolutions[0]?.voided).toBe(true);
    expect(d.state.players.every((p) => p.score === 0)).toBe(true);
    expect(d.state.nightStats[cast.subject]?.fifths).toBe(1);
  });

  it('subject app-switch (LEAVE + RECONNECT inside the seat hold) leaves the pause intact', () => {
    const d = nightDriver(4);
    d.fireNext();
    const cast = toTruthViaCore(d, T + 15_000, () => 'over');
    d.dispatch({ t: 'LEAVE', id: cast.subject, at: T + 20_000 });
    d.dispatch({ t: 'RECONNECT', id: cast.subject, at: T + 25_000 });
    expect(d.state.phase).toMatchObject({ k: 'INPUT', sub: 'TRUTH', deadline: null }); // phase 4 intact
    d.dispatch({ t: 'INPUT', id: cast.subject, payload: { truth: 42 }, at: T + 30_000 });
    expect(d.state.phase.k).toBe('REVEAL');
    expect(gs(d).resolutions[0]?.truth).toBe(42);
  });

  it('seat lapse through reduce(): the witness flees, the loop auto-voids', () => {
    const d = nightDriver(4);
    d.fireNext();
    const cast = toTruthViaCore(d, T + 15_000, () => 'over');
    d.dispatch({ t: 'LEAVE', id: cast.subject, at: T + 20_000 });
    d.dispatch({ t: 'TIMER', timerId: `seathold:${cast.subject}`, at: T + 20_000 + SEAT_HOLD_MS });
    expect(d.state.phase.k).toBe('REVEAL');
    expect(gs(d).resolutions[0]).toMatchObject({ voided: true, truth: null });
    expect(d.log[d.log.length - 1]?.effects).toContainEqual({ k: 'AUDIO', sting: 'fled' });
  });

  it('host VOID_ROUND through reduce() kills a blocked loop', () => {
    const d = nightDriver(4);
    d.fireNext();
    toTruthViaCore(d, T + 15_000, () => 'over');
    d.dispatch({ t: 'VOID_ROUND', id: 'P0', at: T + 25_000 });
    expect(d.state.phase.k).toBe('REVEAL');
    expect(gs(d).resolutions[0]?.voided).toBe(true);
  });

  it('subject pre-view burn swaps to the backup with zero wire trace (4.5)', () => {
    const run = (burn: boolean): Driver => {
      const d = nightDriver(4);
      d.runUntil((s) => s.deal !== null); // assignment locks, then named card ceremony begins
      if (burn) {
        d.dispatch({
          t: 'BURN',
          id: gs(d).subjectId,
          kind: 'card',
          at: (d.state.deal?.startedAt ?? T) + 3_000,
        });
      }
      d.runUntil((s) => s.phase.k === 'INPUT');
      return d;
    };
    const clean = run(false);
    const burned = run(true);
    expect(gs(burned).subjectId).toBe(gs(clean).subjectId); // same seed, same victim
    expect(gs(burned).card?.id).not.toBe(gs(clean).card?.id); // but the backup came out
    expect(burned.state.deal?.burnedId).toBe(gs(clean).card?.id ?? null);
    expect(burned.state.usedCardIds).toContain(gs(clean).card?.id); // vetoed card quarantined for the night
    // The BURN emits only a correlated PRIVATE acknowledgement. Public effects stay identical.
    const burnLog = burned.log.find((l) => l.event.t === 'BURN');
    expect(burnLog?.effects).toEqual([
      {
        k: 'SEND',
        to: gs(burned).subjectId,
        kind: 'preview',
        payload: { status: 'released', previewId: burned.state.deal!.timerId },
      },
    ]);
    // and the ceremony completed on the SAME schedule (timing-identical law)
    expect(burned.state.deal?.completesAt).toBe(clean.state.deal?.completesAt);
    expect(burned.state.deal?.completesAt).toBe((burned.state.deal?.startedAt ?? T) + PREVIEW_MS);
  });

  it('scribe seat-lapse through reduce() passes the dial to the next living seat', () => {
    const d = nightDriver(5);
    d.fireNext();
    const { scribe } = toDebate(d);
    d.dispatch({ t: 'LEAVE', id: scribe, at: T + 12_000 });
    d.dispatch({ t: 'TIMER', timerId: `seathold:${scribe}`, at: T + 12_000 + SEAT_HOLD_MS });
    expect(d.state.phase).toMatchObject({ k: 'INPUT', sub: 'DEBATE' }); // loop survives
    const newScribe = gs(d).scribeId as string;
    expect(newScribe).not.toBe(scribe);
    d.dispatch({ t: 'INPUT', id: newScribe, payload: { line: 6, lock: true }, at: T + 115_000 });
    expect(d.state.phase).toMatchObject({ k: 'INPUT', sub: 'BET' }); // the new hand works the dial
  });
});
