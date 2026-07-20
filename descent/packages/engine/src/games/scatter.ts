// SCATTERBLAST — spec 5.5 + HDRealRules2 Part III §5. Task D-131.
// The physical spike mid-descent. Category + one letter go huge on every phone; a
// bomb with a HIDDEN fuse ticks while the turn passes AROUND THE TABLE by voice —
// the app tracks nothing, it just waits. BOOM: all phones flash "WHO DIED?" and the
// room fingers whoever was holding it. Majority tap is the corpse; "THE TABLE HAS
// SPOKEN." Best of three bombs. The app never watches the turn order — humans do.
//
// Module law (games/module.ts): state lives opaquely in RoomState.gameState;
// view() is the ONLY serialization surface, so every redaction rule lives there.
// Pure module: time arrives as ctx.now, randomness as ctx.rand. Never a clock —
// the fuse is a SEEDED draw, not a wall-clock roll.
//
// Core integration (engine.ts "module step protocol"), per bomb — built like roast:
//   $deal (4.5 ceremony, 5.5s, no subject — the corpse is vote-emergent, night-dedup
//     writeback) -> CORE_DEALT
//   -> $phase INPUT "BOMB": category + letter public, a self-scheduled fuse timer
//      (uniform 15–45s + 5s bias per bomb) armed. Phase deadline is NULL on purpose:
//      the fuse is the whole secret, so no countdown ever hits the wire. No input.
//   -> fuse fires -> $phase INPUT "BOOM" (5s skippable; everyone taps the holder)
//   -> BOOM deadline / all tapped -> $phase REVEAL (core owns the hold: flip beat,
//      DESCEND, fire-decay) — the loser's name + "THE TABLE HAS SPOKEN"
//   -> CORE_REVEAL_DONE -> next bomb or done.
import type { DealRequest, Effect, Player, PlayerId, ScatterCard } from '../types.js';
import { cardLegal } from '../consent.js';
import { pick } from '../rng.js';
import { points } from '../scoring.js';
import { CORE_DEALT, CORE_REVEAL_DONE, type GameCtx, type GameModule, type GameStep } from './module.js';

// ===== tuning (spec 5.5 / 4.6 / 4.8) =====
export const BOMBS_PER_CIRCLE = 3; // "best of three bombs"
export const BOOM_MS = 5_000; // WHO DIED window: 5s, skippable (deadline -> whatever's in)
export const FUSE_MIN_MS = 15_000; // fuse floor
export const FUSE_SPAN_MS = 30_000; // uniform 15–45s (min + [0,span))
export const FUSE_BIAS_PER_BOMB_MS = 5_000; // +5s per bomb INDEX (0-based) — bomb 3 burns longest
export const IMP_WEIGHT = 0.5; // 4.8: imps tap at half weight; a tie is never DECIDED by imps

// ===== module state (opaque to core; view() redacts) =====
export interface ScatterResolution {
  cardId: string;
  category: string;
  letter: string;
  loser: PlayerId | null; // 0 taps or host-voided -> null (a dud bomb: no corpse)
  spread: { target: PlayerId; weight: number }[]; // anonymous bars, desc — counts only, never who
}

export interface ScatterState {
  sub: 'DEAL' | 'BOMB' | 'BOOM' | 'REVEAL'; // DEAL = core ceremony running, card still secret
  loop: number; // 0-based bomb index
  loops: number; // 3 per spec
  card: ScatterCard | null; // null until CORE_DEALT (the ceremony owns the announce)
  taps: Record<PlayerId, PlayerId>; // tapper -> the accused holder. SECRET — the MAP never leaves view()
  tapOrder: PlayerId[]; // accused ids in arrival order — the earliest-tap tie-break's only fuel
  boomDeadline: number; // view mirror; authoritative deadline is the SCHEDULE effect
  resolutions: ScatterResolution[];
}

// ===== views =====
interface ScatterViewBase {
  deck: 'scatter';
  sub: string;
  loop: number;
  loops: number;
  category: string;
  letter: string;
}
export type ScatterBombView = ScatterViewBase & {
  sub: 'BOMB'; // no deadline on the wire — the fuse is hidden by law (5.5)
};
export type ScatterBoomView = ScatterViewBase & {
  sub: 'BOOM';
  deadline: number;
  eligible: number;
  tappedCount: number; // counts only — never who
  youTapped: PlayerId | null; // your own finger, nobody else's
};
export type ScatterRevealView = ScatterViewBase & {
  sub: 'REVEAL';
  loser: PlayerId | null;
  spread?: { target: PlayerId; weight: number }[]; // anonymous bars; absent on a dud bomb
};
export type ScatterView = ScatterBombView | ScatterBoomView | ScatterRevealView;

// ===== deck registry =====
// Card plumbing arrives with content integration (D-127/8.3); until then the core
// (or a test) injects a deck here. Stubs keep dev nights feral: every category is a
// shout-answers prompt, single-letter constraint, first-person menace — the table
// screaming over each other is the whole point.
export const DEFAULT_SCATTER_DECK: ScatterCard[] = [
  stub('001', "Things you've screamed at a Roomba", 'B', 1, 4, 'absurdist', 'things-screamed-at-thing'),
  stub('002', 'Reasons your ex has you blocked on everything', 'S', 3, 4, 'menace', 'reasons-ex-blocked-you'),
  stub('003', 'Cursed toppings for a wedding cake', 'G', 1, 4, 'gross', 'cursed-toppings-for-thing'),
  stub('004', 'Apps to delete before your mom borrows your phone', 'T', 2, 3, 'table-aware', 'apps-to-delete-before'),
  stub('005', 'Lies you have told a dentist to their face', 'F', 2, 3, 'deadpan', 'lies-told-to-[person]'),
  stub('006', 'Ways to get escorted out of a Chuck E. Cheese', 'P', 2, 4, 'physical', 'ways-escorted-out-of'),
  stub('007', 'Excuses for why the cops are already here', 'D', 3, 5, 'menace', 'excuses-cops-are-here'),
  stub('008', 'Things that are somehow still technically legal', 'H', 2, 4, 'absurdist', 'things-technically-legal'),
  stub('009', 'Group chats you would nuke from orbit', 'C', 3, 4, 'table-aware', 'group-chats-to-nuke'),
];
function stub(
  n: string,
  category: string,
  letter: string,
  exposure: ScatterCard['exposure'],
  chaos: ScatterCard['chaos'],
  register: ScatterCard['register'],
  skeleton: string,
): ScatterCard {
  // Card law (5.7 precedent): display text IS the only joke source — text === category.
  return { id: `scatter_stub_${n}`, deck: 'scatter', text: category, category, letter, exposure, chaos, register, skeleton };
}

let deckCards: ScatterCard[] = DEFAULT_SCATTER_DECK;
export function setScatterDeck(cards: readonly ScatterCard[]): void {
  if (cards.length === 0) throw new Error('scatter deck cannot be empty');
  deckCards = [...cards];
}

// ===== helpers =====
const fuseTimer = (circleIdx: number, loop: number): string => `scatter:fuse:${circleIdx}:${loop}`;
const boomTimer = (circleIdx: number, loop: number): string => `scatter:boom:${circleIdx}:${loop}`;
const votersOf = (ctx: GameCtx): Player[] => [...ctx.players, ...ctx.imps]; // imps are citizens: they tap (at half) and can hold the bomb
const weightOf = (p: Player): number => (p.role === 'imp' ? IMP_WEIGHT : 1);

/** Seeded fuse: uniform 15–45s, +5s per bomb index so the finale bomb 3 burns longest (5.5). */
function fuseMs(ctx: GameCtx, loop: number): number {
  return FUSE_MIN_MS + Math.floor(ctx.rand() * FUSE_SPAN_MS) + FUSE_BIAS_PER_BOMB_MS * loop;
}

function readState(ctx: GameCtx): ScatterState | null {
  const gs = ctx.state.gameState as ScatterState | null | undefined;
  return gs && (gs.sub === 'DEAL' || gs.sub === 'BOMB' || gs.sub === 'BOOM' || gs.sub === 'REVEAL') ? gs : null;
}

function noop(ctx: GameCtx): GameStep {
  return { gameState: ctx.state.gameState, effects: [] };
}

function parseTap(payload: unknown): PlayerId | null {
  if (typeof payload !== 'object' || payload === null) return null;
  const v = (payload as Record<string, unknown>)['tap'];
  return typeof v === 'string' && v.length > 0 ? v : null;
}

// Consent (4.4): scatter categories name nobody — pure generic content, so the
// room's genericCeiling caps exposure. Rung preference (4.3) and night-dedup ride
// on top. No subject, no vote-emergent clause: the corpse is decided AFTER the card,
// by fingers, never by the card's exposure.
function legalPool(ctx: GameCtx): ScatterCard[] {
  const ceilings = ctx.players.map((p) => p.heatCeiling);
  let legal = deckCards.filter((c) => cardLegal(c.exposure, { ceilings }));
  if (legal.length === 0) {
    // Content bug (nothing legal): degrade to the mildest cards rather than deadlock
    // the night. The arc builder gates depth on legal content; this is the backstop.
    const minE = Math.min(...deckCards.map((c) => c.exposure));
    legal = deckCards.filter((c) => c.exposure === minE);
  }
  // E-curve (4.3): prefer cards at or under this circle's rung when any exist.
  const underRung = legal.filter((c) => c.exposure <= ctx.circle.rung);
  return underRung.length > 0 ? underRung : legal;
}

/** Pick primary + reserved backup (4.5), night-deduped via usedCardIds (core writes back on ceremony completion). */
function pickCardPair(ctx: GameCtx): { primary: ScatterCard; backup: ScatterCard } {
  const legal = legalPool(ctx);
  const fresh = legal.filter((c) => !ctx.state.usedCardIds.includes(c.id));
  const pool = fresh.length > 0 ? fresh : legal; // deck exhausted: a repeat beats a dead night
  const primary = pick(ctx.rand, pool);
  const rest = pool.filter((c) => c.id !== primary.id);
  const fallback = legal.filter((c) => c.id !== primary.id);
  const backup = rest.length > 0 ? pick(ctx.rand, rest) : fallback.length > 0 ? pick(ctx.rand, fallback) : primary;
  return { primary, backup };
}

/** Hand the category to the core's 4.5 ceremony. No subject: the corpse is fingered later, not named now. */
function deal(ctx: GameCtx, loop: number, loops: number, carried: Pick<ScatterState, 'resolutions'>): GameStep {
  const { primary, backup } = pickCardPair(ctx);
  const $deal: DealRequest = { primary, backup, subjectId: null };
  const gameState: ScatterState & { $deal: DealRequest } = {
    sub: 'DEAL',
    loop,
    loops,
    card: null, // secret until the ceremony completes (CORE_DEALT)
    taps: {},
    tapOrder: [],
    boomDeadline: 0,
    resolutions: carried.resolutions,
    $deal,
  };
  return { gameState, effects: [] };
}

/**
 * Ceremony done: category + letter are public. Arm the HIDDEN fuse — a plain module
 * timer (no ':hold:' in the id, so DESCEND can never rush a live bomb) — and open the
 * BOMB phase with a NULL deadline so the fuse length never touches the wire. Turn
 * passing is verbal from here; the app accepts nothing and watches nothing.
 */
function openBomb(ctx: GameCtx, st: ScatterState): GameStep {
  const dealt = ctx.state.deal?.done === true ? ctx.state.deal.card : null;
  if (dealt === null || dealt.deck !== 'scatter') return noop(ctx); // stale CORE_DEALT
  const card = dealt as ScatterCard; // honors a burn swap: whatever survived the ceremony
  const gameState: ScatterState & { $phase: { k: 'INPUT'; sub: string; deadline: number | null } } = {
    ...st,
    sub: 'BOMB',
    card,
    taps: {},
    tapOrder: [],
    boomDeadline: 0,
    $phase: { k: 'INPUT', sub: 'BOMB', deadline: null }, // null = no countdown on the wire: the fuse is the whole secret
  };
  return {
    gameState,
    effects: [{ k: 'SCHEDULE', timerId: fuseTimer(ctx.circleIdx, st.loop), atMs: ctx.now + fuseMs(ctx, st.loop) }],
  };
}

/** The fuse blew: full-screen WHO DIED flash + the 5s tap window. The bang lands here. */
function openBoom(ctx: GameCtx, st: ScatterState): GameStep {
  const deadline = ctx.now + BOOM_MS;
  const gameState: ScatterState & { $phase: { k: 'INPUT'; sub: string; deadline: number } } = {
    ...st,
    sub: 'BOOM',
    taps: {},
    tapOrder: [],
    boomDeadline: deadline,
    $phase: { k: 'INPUT', sub: 'BOOM', deadline },
  };
  return {
    gameState,
    effects: [
      { k: 'SCHEDULE', timerId: boomTimer(ctx.circleIdx, st.loop), atMs: deadline },
      { k: 'AUDIO', sting: 'boom' }, // the explosion, synced to the flash (≤150ms skew is the DO's job)
    ],
  };
}

// ===== tap math =====
function leaders(t: Map<PlayerId, number>): PlayerId[] {
  let max = 0;
  for (const v of t.values()) if (v > max) max = v;
  if (max <= 0) return [];
  return [...t.entries()]
    .filter(([, v]) => v === max)
    .map(([k]) => k)
    .sort();
}

/** Among tied accused, the one the room fingered EARLIEST (first appearance in tapOrder). Deterministic, never imp-decided. */
function earliestTapped(pool: readonly PlayerId[], tapOrder: readonly PlayerId[]): PlayerId {
  let best = pool[0] as PlayerId;
  let bestIdx = Number.POSITIVE_INFINITY;
  for (const id of pool) {
    const seen = tapOrder.indexOf(id);
    const at = seen < 0 ? Number.POSITIVE_INFINITY : seen;
    if (at < bestIdx || (at === bestIdx && id.localeCompare(best) < 0)) {
      best = id;
      bestIdx = at;
    }
  }
  return best;
}

// Majority tap with the imp clause (4.8): imps count at 0.5, but a tie is never
// DECIDED by imps — a full-vote tie among the accused is broken by earliest-tap (a
// table fact), and an imp-manufactured weighted tie collapses back to the full-vote
// leader. Imp weight still counts whenever no tie is in play. Unlike roast there is
// exactly ONE corpse: any surviving tie resolves through the room's fastest fingers.
function decideCorpse(
  weighted: Map<PlayerId, number>,
  full: Map<PlayerId, number>,
  tapOrder: readonly PlayerId[],
): PlayerId | null {
  const fullLeaders = leaders(full);
  let pool: PlayerId[];
  if (fullLeaders.length >= 2) {
    pool = fullLeaders; // real full-vote tie — imps don't get to break it
  } else if (fullLeaders.length === 1 && leaders(weighted).length >= 2) {
    pool = fullLeaders; // imps manufactured a weighted tie — collapse to the human leader
  } else {
    pool = leaders(weighted); // single weighted leader, or the all-imp-fingers edge case
  }
  if (pool.length === 0) return null; // nobody tapped: a dud bomb, no corpse
  if (pool.length === 1) return pool[0] as PlayerId;
  return earliestTapped(pool, tapOrder);
}

function tally(
  taps: Record<PlayerId, PlayerId>,
  voters: Player[],
  tapOrder: readonly PlayerId[],
): { loser: PlayerId | null; spread: ScatterResolution['spread'] } {
  const weighted = new Map<PlayerId, number>();
  const full = new Map<PlayerId, number>();
  for (const voter of voters) {
    const to = taps[voter.id];
    if (to === undefined) continue;
    weighted.set(to, (weighted.get(to) ?? 0) + weightOf(voter));
    if (voter.role !== 'imp') full.set(to, (full.get(to) ?? 0) + 1);
  }
  const spread = [...weighted.entries()]
    .map(([target, weight]) => ({ target, weight }))
    .sort((a, b) => b.weight - a.weight || a.target.localeCompare(b.target));
  return { loser: decideCorpse(weighted, full, tapOrder), spread };
}

function resolve(ctx: GameCtx, st: ScatterState, preEffects: Effect[], voided = false): GameStep {
  if (st.card === null) return noop(ctx); // unreachable: BOOM always has a card
  const voters = votersOf(ctx);
  const taps = voided ? {} : st.taps; // voided loop: no corpse, no survivors credited (4.6)
  const { loser, spread } = tally(taps, voters, st.tapOrder);

  // Scoring (pre-multiplier — core applies finale x3 / Bargain x2): everyone who was
  // NOT fingered the corpse survives (+1), participation folded in; the corpse eats
  // +0 (scatter.holder). A dud bomb (no corpse) pays nobody — there was nothing to
  // survive. Imps score full survive points; their 0.5 weight only shapes the tap math.
  const scores: Record<string, number> = {};
  if (!voided && loser !== null) {
    for (const c of voters) {
      if (c.id === loser) continue; // the holder — points('scatter.holder') is +0
      scores[c.id] = points('scatter.survive');
    }
  }

  const resolution: ScatterResolution = {
    cardId: st.card.id,
    category: st.card.category,
    letter: st.card.letter,
    loser,
    spread,
  };
  // $phase REVEAL: the core stamps holdSince and owns the hold — 3-2-1 flip beat,
  // DESCEND (host anytime / anyone past 45s), fire-decay past the 20s minimum (4.2).
  const gameState: ScatterState & { $phase: { k: 'REVEAL' } } = {
    ...st,
    sub: 'REVEAL',
    resolutions: [...st.resolutions, resolution],
    $phase: { k: 'REVEAL' },
  };
  const sting: Effect[] = voided ? [{ k: 'AUDIO', sting: 'void' }] : []; // the bang already fired at BOOM
  return { gameState, effects: [...preEffects, ...sting, { k: 'SNAPSHOT' }], scores };
}

// ===== the module =====
export const scatterModule = {
  deck: 'scatter',
  minN: 3,

  start(ctx: GameCtx): GameStep {
    const loops = ctx.circle.loops >= 1 ? ctx.circle.loops : BOMBS_PER_CIRCLE;
    return deal(ctx, 0, loops, { resolutions: [] });
  },

  input(ctx: GameCtx, playerId: string, payload: unknown): GameStep {
    const st = readState(ctx);
    if (!st || st.sub !== 'BOOM') return noop(ctx); // BOMB accepts NOTHING (verbal turn); DEAL/REVEAL neither
    const target = parseTap(payload);
    if (target === null) return noop(ctx);
    const voters = votersOf(ctx);
    const tapper = voters.find((p) => p.id === playerId);
    const corpse = voters.find((p) => p.id === target);
    if (!tapper || !corpse) return noop(ctx); // ghost finger, or a target who isn't at the table
    // Self-tap is legal: "who was holding it" can honestly be YOU — this is a report, not an accusation to dodge.
    const taps = { ...st.taps, [playerId]: corpse.id }; // re-tap allowed until deadline: last finger wins
    const tapOrder = [...st.tapOrder, corpse.id]; // append every accepted tap: the earliest-tap tie-break's record
    const next: ScatterState = { ...st, taps, tapOrder };
    if (Object.keys(taps).length >= voters.length) {
      // Whole table's fingered someone — don't make them stare at a countdown.
      return resolve(ctx, next, [{ k: 'CANCEL', timerId: boomTimer(ctx.circleIdx, st.loop) }]);
    }
    return { gameState: next, effects: [{ k: 'BROADCAST' }] }; // count ticks up; view() hides who
  },

  timer(ctx: GameCtx, timerId: string): GameStep {
    const st = readState(ctx);
    if (!st) return noop(ctx);
    if (st.sub === 'DEAL' && timerId === CORE_DEALT) {
      return openBomb(ctx, st); // ceremony done: category public, fuse armed
    }
    if (st.sub === 'BOMB' && timerId === fuseTimer(ctx.circleIdx, st.loop)) {
      return openBoom(ctx, st); // the fuse blew: WHO DIED
    }
    if (st.sub === 'BOOM' && timerId === boomTimer(ctx.circleIdx, st.loop)) {
      return resolve(ctx, st, []); // deadline: whatever fingers are in decide the corpse — the game never waits
    }
    if (st.sub === 'REVEAL' && timerId === CORE_REVEAL_DONE) {
      if (st.loop + 1 < st.loops) return deal(ctx, st.loop + 1, st.loops, st);
      return { gameState: st, effects: [], done: true }; // core -> LADDER
    }
    return noop(ctx); // stale timer for a dead loop
  },

  control(ctx: GameCtx, playerId: string, kind: 'REST' | 'SKIPEM' | 'FIFTH' | 'VOID'): GameStep {
    const st = readState(ctx);
    if (!st) return noop(ctx);
    if (kind === 'VOID') {
      // Host kill-switch (4.7): the bomb dies scoreless, blaming nobody. A lapsed
      // seat can't sink scatter — there's no critical performer — so a non-host VOID
      // (the seat-lapse convention) is a no-op; that player just doesn't tap.
      const caller = ctx.players.find((p) => p.id === playerId);
      if (!caller || caller.role !== 'host') return noop(ctx);
      if (st.sub === 'BOMB') {
        return resolve(ctx, st, [{ k: 'CANCEL', timerId: fuseTimer(ctx.circleIdx, st.loop) }], true);
      }
      if (st.sub === 'BOOM') {
        return resolve(ctx, st, [{ k: 'CANCEL', timerId: boomTimer(ctx.circleIdx, st.loop) }], true);
      }
      return noop(ctx); // DEAL rides out its ceremony; REVEAL is already resolved
    }
    return noop(ctx); // REST/SKIPEM/FIFTH: scatter has no performer, no blocking input, no re-deal valve
  },

  // The ONLY serialization surface. Redaction law (3.4 + 5.5):
  // - during the DEAL ceremony NOTHING serializes (the category is still secret);
  // - during BOMB every phone sees the SAME category + letter (the app tracks no turn
  //   and no holder) and NO deadline — the fuse never touches the wire;
  // - during BOOM a viewer sees counts + their OWN tap, nothing else;
  // - after resolution: the loser's name + an ANONYMOUS weighted spread (no
  //   finger-to-face pair ever leaves the server) — the tap MAP never serializes;
  // - per-bomb score deltas are never in the view (a +1 would out who wasn't tapped).
  view(ctx: GameCtx, viewerId: string): ScatterView | null {
    const st = readState(ctx);
    if (!st || st.sub === 'DEAL' || st.card === null) return null;
    const base = {
      deck: 'scatter' as const,
      loop: st.loop,
      loops: st.loops,
      category: st.card.category,
      letter: st.card.letter,
    };
    if (st.sub === 'BOMB') {
      return { ...base, sub: 'BOMB' }; // deliberately deadline-free: the fuse is hidden by law
    }
    if (st.sub === 'BOOM') {
      return {
        ...base,
        sub: 'BOOM',
        deadline: st.boomDeadline,
        eligible: votersOf(ctx).length,
        tappedCount: Object.keys(st.taps).length,
        youTapped: st.taps[viewerId] ?? null,
      };
    }
    const res = st.resolutions[st.resolutions.length - 1];
    if (!res) return null; // unreachable by construction; never leak on a bug
    const reveal: ScatterRevealView = { ...base, sub: 'REVEAL', loser: res.loser };
    if (res.spread.length > 0) reveal.spread = res.spread; // counts-only bars; absent on a dud bomb
    return reveal;
  },
} satisfies GameModule;
