// OVER/UNDER — spec 5.3 + HDRealRules2 Part III §3. Task D-122 [dep: D-115].
// Two subjects per circle. A stat about a NAMED subject hits the table; the room
// argues the line OUT LOUD (the argument IS the game) while the scribe — next seat
// after the subject — rides the dial and locks the room's line; everyone else
// secretly bets OVER or UNDER; then the subject fetches their receipts. That fetch
// is a BLOCKING input with the full 4.7 shame machinery: paused deadline, WAITING_ON,
// pit vote, FIFTH, host VOID, seat-lapse — and NEVER a fabricated number.
// Synced reveal: number vs line, gasp or vindication. Exact line = push, everyone +1,
// subject roasted for being median.
//
// Module law (games/module.ts): state lives opaquely in RoomState.gameState;
// view() is the ONLY serialization surface, so every redaction rule lives there.
// Pure module: time arrives as ctx.now, randomness as ctx.rand. Never a clock.
//
// Core integration (engine.ts "module step protocol"), per loop — built like roast:
//   $spotlight {role: subject} (fixed private assignment + dodge windows)
//   -> $deal {subjectId} (4.5: 10s PRIVATE card pre-view + burn window,
//   night-dedup writeback) -> CORE_DEALT
//   -> $phase INPUT "DEBATE" (25s; scribe dial, lock early or lock at deadline)
//   -> $phase INPUT "BET" (12s skippable; subject excluded; early-advance when all in)
//   -> blockingBegin: $phase INPUT "TRUTH" deadline null (timer provably PAUSED)
//      -> grace -> WAITING_ON -> pit vote / FIFTH / host VOID / seat-lapse terminals
//   -> $phase REVEAL (core-held hold: flip beat, DESCEND, fire-decay)
//   -> CORE_REVEAL_DONE -> next subject or done.
import type { DealRequest, Effect, OverUnderCard, Player, PlayerId, SpotlightRequest } from '../types.js';
import { cardLegal } from '../consent.js';
import { pick } from '../rng.js';
import { points } from '../scoring.js';
import {
  blockingBegin,
  blockingControl,
  blockingPitVote,
  blockingResolveInput,
  blockingTimerFired,
  type BlockingJuror,
  type BlockingState,
} from '../engine.js';
import {
  CORE_DEALT,
  CORE_REVEAL_DONE,
  CORE_SPOTLIGHT_DONE,
  type GameCtx,
  type GameModule,
  type GameStep,
} from './module.js';

// ===== tuning (spec 5.3 / 4.6 / 4.7 / 4.8) =====
export const SUBJECTS_PER_CIRCLE = 2;
export const DEBATE_MS = 25_000; // LINE DEBATE — verbal; the scribe locks the line
export const BET_MS = 12_000; // skippable: deadline -> non-bettors auto-abstain
export const IMP_WEIGHT = 0.5; // informational only here (4.8): bets score full, weight colors the split
export const MAX_COUNT = 1_000_000_000; // number-pad sanity bound (lines and truths are receipts: counts)
/** Fresh Meat protocol (HDRealRules2 §10): first-nighters draw first-impression stats
 *  only — anything with history ("this week", "this month", bounded-event) needs a
 *  crew that has actually lived with you. */
export const FIRST_IMPRESSION_TIMEBOXES: readonly string[] = ['right now', 'today'];

export type Bet = 'over' | 'under';

// ===== module state (opaque to core; view() redacts) =====
export interface OverUnderResolution {
  cardId: string | null; // null only when the loop died during the ceremony
  subjectId: PlayerId;
  scribeId: PlayerId | null;
  line: number | null;
  truth: number | null; // a number ONLY ever arrives from a human (D-115)
  unverified: boolean; // scribe-downgrade: host-relayed verbal claim, no receipt bonus
  push: boolean; // truth == line -> everyone +1, subject roasted for being median
  voided: boolean;
  winners: PlayerId[]; // correct bettors — public AT reveal ("winners flash"), never before
  split: { over: number; under: number }; // weighted spread (imps 0.5) — informational only
}

export interface OverUnderState {
  sub: 'DEAL' | 'DEBATE' | 'BET' | 'TRUTH' | 'REVEAL'; // DEAL = core ceremony running, card still secret
  loop: number; // 0-based subject index
  loops: number; // 2 per spec
  subjectId: PlayerId;
  scribeId: PlayerId | null; // null until CORE_DEALT seats the scribe
  card: OverUnderCard | null; // null until CORE_DEALT (the ceremony owns the secret)
  line: number | null; // provisional dial during DEBATE; the locked room line from BET on
  debateDeadline: number; // view mirror; authoritative deadline is the SCHEDULE effect
  debateReassigns: number; // silent-scribe hand-offs this loop (capped, then void)
  bets: Record<PlayerId, Bet>; // SECRET until reveal — never leaves view() attributed before then
  betDeadline: number;
  blocking: BlockingState | null; // 4.7 machinery, TRUTH only
  resolutions: OverUnderResolution[];
  participation: PlayerId[]; // once-per-circle +1 already paid (bettors AND subject receipts)
  subjectsUsed: PlayerId[]; // this circle's subjects incl. current — loop 2 excludes loop 1
}

interface OverUnderAssignState {
  sub: 'ASSIGN';
  loop: number;
  loops: number;
  resolutions: OverUnderResolution[];
  participation: PlayerId[];
  subjectsUsed: PlayerId[];
}

type OverUnderInternalState = OverUnderAssignState | OverUnderState;

// ===== views =====
export interface OverUnderCardFace {
  id: string;
  text: string;
  receiptSurface: string; // "Settings → Screen Time" — where the subject digs
  timebox: string;
}
interface OverUnderViewBase {
  deck: 'overunder';
  loop: number;
  loops: number;
  subjectId: PlayerId; // public from CORE_DEALT on — the whole room stares at one name
  scribeId: PlayerId | null;
}
export type OverUnderDebateView = OverUnderViewBase & {
  sub: 'DEBATE';
  card: OverUnderCardFace;
  line: number | null; // the dial is public — the argument is out loud anyway
  deadline: number;
  youAreScribe: boolean; // only the scribe's phone renders the line-setter dial
};
export type OverUnderBetView = OverUnderViewBase & {
  sub: 'BET';
  card: OverUnderCardFace;
  line: number;
  deadline: number;
  eligible: number;
  betCount: number; // counts only — never who
  youBet: Bet | null; // your own bet, nobody else's
  youAreSubject: boolean; // subject's phone shows "fetch your receipt" instead of buttons
};
export type OverUnderTruthView = OverUnderViewBase & {
  sub: 'TRUTH';
  card: OverUnderCardFace;
  line: number;
  waitingOn: PlayerId;
  pitOpen: boolean; // 30s in: DRAG THEM BACK / FEED THEM TO THE PIT buttons unlock
  youPitVoted: 'drag' | 'pit' | null;
  youBet: Bet | null;
  youAreSubject: boolean;
};
export type OverUnderRevealView = OverUnderViewBase & {
  sub: 'REVEAL';
  card: OverUnderCardFace | null;
  line: number | null;
  truth: number | null; // the true number exists on the wire HERE and nowhere earlier
  push: boolean;
  unverified: boolean;
  voided: boolean;
  winners: PlayerId[];
  split: { over: number; under: number };
  youBet: Bet | null;
};
export type OverUnderView = OverUnderDebateView | OverUnderBetView | OverUnderTruthView | OverUnderRevealView;

// ===== deck registry =====
// Card plumbing arrives with content integration (D-127/8.3); until then the core
// (or a test) injects a deck here. Stubs keep dev nights honest: every stat is a
// number the subject's own phone can rat out.
export const DEFAULT_OVERUNDER_DECK: OverUnderCard[] = [
  stub('001', 'How many unread emails is {NAME} letting rot right now?', 'Mail badge — hold the phone up', 'right now', 1, 3, 'deadpan', 'unread-rot'),
  stub('002', 'Hours of screen time {NAME} burned today while claiming to be busy?', 'Settings → Screen Time', 'today', 2, 3, 'observational', 'screen-time-alibi'),
  stub('003', 'How many times did {NAME} open Instagram today?', 'Screen Time → See All App Activity', 'today', 2, 3, 'observational', 'ig-pickups'),
  stub('004', "Photos of {NAME}'s own face taken this month. Just this month.", 'Photos → Albums → Selfies', 'this month', 2, 4, 'deadpan', 'selfie-census'),
  stub('005', 'How many people has {NAME} left on read this week?', 'Messages — count the corpses', 'this week', 3, 4, 'menace', 'left-on-read'),
  stub('006', "Notifications {NAME}'s phone is hiding behind Do Not Disturb right now?", 'Notification Center, one swipe', 'right now', 1, 3, 'deadpan', 'dnd-backlog'),
  stub('007', 'How many hours did {NAME} actually sleep last night?', 'Health → Sleep — the app saw everything', 'today', 3, 3, 'observational', 'sleep-receipts'),
  stub('008', 'Dating apps {NAME} has installed OR reinstalled this month?', 'App Library — search your shame', 'this month', 4, 4, 'table-aware', 'dating-app-relapse'),
  stub('009', 'Dollars {NAME} spent on delivery this week while owning a stove?', 'Bank app → recent transactions', 'this week', 3, 4, 'petty-domestic', 'delivery-vs-stove'),
  stub('010', 'How many browser tabs does {NAME} have open right now, phone only?', 'Tab counter — show the number', 'right now', 1, 3, 'absurdist', 'tab-hoarder'),
];
function stub(
  n: string,
  text: string,
  receiptSurface: string,
  timebox: string,
  exposure: OverUnderCard['exposure'],
  chaos: OverUnderCard['chaos'],
  register: OverUnderCard['register'],
  skeleton: string,
): OverUnderCard {
  return { id: `overunder_stub_${n}`, deck: 'overunder', text, exposure, chaos, register, skeleton, receiptSurface, timebox };
}

let deckCards: OverUnderCard[] = DEFAULT_OVERUNDER_DECK;
export function setOverUnderDeck(cards: readonly OverUnderCard[]): void {
  if (cards.length === 0) throw new Error('overunder deck cannot be empty');
  deckCards = [...cards];
}

/** Personal-history filter hook: knowledge-dependent stats never land on Fresh Meat. */
export function isPersonalHistory(card: OverUnderCard): boolean {
  return !FIRST_IMPRESSION_TIMEBOXES.includes(card.timebox);
}

// ===== helpers =====
export const debateTimerId = (circleIdx: number, loop: number): string => `overunder:debate:${circleIdx}:${loop}`;
export const betTimerId = (circleIdx: number, loop: number): string => `overunder:bet:${circleIdx}:${loop}`;
export const truthKey = (circleIdx: number, loop: number): string => `overunder:${circleIdx}:${loop}`;

const weightOf = (p: Player): number => (p.role === 'imp' ? IMP_WEIGHT : 1);
const hostIdOf = (ctx: GameCtx): PlayerId | null => ctx.players.find((p) => p.role === 'host')?.id ?? null;
/** Everyone except the subject bets — imps too (their weight only colors the split). */
const bettorsOf = (ctx: GameCtx, st: OverUnderState): Player[] =>
  [...ctx.players, ...ctx.imps].filter((p) => p.id !== st.subjectId);
const jurorsOf = (ctx: GameCtx, st: OverUnderState): BlockingJuror[] =>
  bettorsOf(ctx, st).map((p) => ({ id: p.id, weight: weightOf(p) }));

function readState(ctx: GameCtx): OverUnderInternalState | null {
  const gs = ctx.state.gameState as OverUnderInternalState | null | undefined;
  return gs &&
    (gs.sub === 'ASSIGN' || gs.sub === 'DEAL' || gs.sub === 'DEBATE' || gs.sub === 'BET' || gs.sub === 'TRUTH' || gs.sub === 'REVEAL')
    ? gs
    : null;
}

function noop(ctx: GameCtx): GameStep {
  return { gameState: ctx.state.gameState, effects: [] };
}

// ===== payload parsing (numbers are receipts: non-negative integers, bounded) =====
function parseCount(payload: unknown, key: 'line' | 'truth' | 'claim'): number | null {
  if (typeof payload !== 'object' || payload === null) return null;
  const v = (payload as Record<string, unknown>)[key];
  return typeof v === 'number' && Number.isInteger(v) && v >= 0 && v <= MAX_COUNT ? v : null;
}
function parseLine(payload: unknown): { line: number; lock: boolean } | null {
  const line = parseCount(payload, 'line');
  if (line === null) return null;
  return { line, lock: (payload as Record<string, unknown>)['lock'] === true };
}
function parseBet(payload: unknown): Bet | null {
  if (typeof payload !== 'object' || payload === null) return null;
  const v = (payload as Record<string, unknown>)['bet'];
  return v === 'over' || v === 'under' ? v : null;
}
function parsePit(payload: unknown): 'drag' | 'pit' | null {
  if (typeof payload !== 'object' || payload === null) return null;
  const v = (payload as Record<string, unknown>)['pit'];
  return v === 'drag' || v === 'pit' ? v : null;
}

// ===== subject + card selection =====
// Consent (4.4): the subject is NAMED, so the per-target ceiling applies — the card
// runs no hotter than the person it's about agreed to. Fresh Meat (§10) then strips
// personal-history stats. Rung preference (4.3) and night-dedup ride on top.
function legalFor(ctx: GameCtx, subject: Player): OverUnderCard[] {
  const ceilings = ctx.players.map((p) => p.heatCeiling);
  let legal = deckCards.filter((c) => cardLegal(c.exposure, { ceilings, subjectCeilings: [subject.heatCeiling] }));
  if (subject.freshMeat) legal = legal.filter((c) => !isPersonalHistory(c));
  if (legal.length === 0) return [];
  const underRung = legal.filter((c) => c.exposure <= ctx.circle.rung);
  return underRung.length > 0 ? underRung : legal;
}

/** Only candidates with subject-legal content enter the core-owned ceremony. */
function eligibleSubjectIds(ctx: GameCtx, exclude: readonly PlayerId[]): PlayerId[] {
  let candidates = ctx.players.filter((p) => !exclude.includes(p.id));
  if (candidates.length === 0) candidates = [...ctx.players]; // tiny room: repeats beat a dead night
  const legal = candidates.filter((p) => legalFor(ctx, p).length > 0);
  return (legal.length > 0 ? legal : candidates).map((p) => p.id);
}

/** Recompute content after assignment so a burned replacement gets content legal for them. */
function poolForSubject(ctx: GameCtx, subject: Player): OverUnderCard[] {
  const legal = legalFor(ctx, subject);
  if (legal.length > 0) return legal;
  // Content bug (nothing legal): preserve the mildest-card backstop without ever
  // reusing the original assignee's subject-specific pool.
  const base =
    subject.freshMeat && deckCards.some((c) => !isPersonalHistory(c))
      ? deckCards.filter((c) => !isPersonalHistory(c))
      : deckCards;
  const minE = Math.min(...base.map((c) => c.exposure));
  return base.filter((c) => c.exposure === minE);
}

/** Primary + reserved backup (4.5), night-deduped via usedCardIds (core writes back). */
function pickPair(ctx: GameCtx, legal: OverUnderCard[]): { primary: OverUnderCard; backup: OverUnderCard } {
  const fresh = legal.filter((c) => !ctx.state.usedCardIds.includes(c.id));
  const pool = fresh.length > 0 ? fresh : legal; // deck exhausted: a repeat beats a dead night
  const primary = pick(ctx.rand, pool);
  const rest = pool.filter((c) => c.id !== primary.id);
  const fallback = legal.filter((c) => c.id !== primary.id);
  const backup = rest.length > 0 ? pick(ctx.rand, rest) : fallback.length > 0 ? pick(ctx.rand, fallback) : primary;
  return { primary, backup };
}

/**
 * Scribe = next seat after `afterSeat`, wrapping, never the subject, connected
 * preferred (5.3: "scribe = next seat after subject"). Reassignment passes the same
 * ring — the dial walks the table until someone locks a line or the loop dies.
 */
function scribeFor(players: readonly Player[], subjectId: PlayerId, afterSeat: number): Player | null {
  const ring = players.filter((p) => p.id !== subjectId).sort((a, b) => a.seat - b.seat);
  if (ring.length === 0) return null;
  const live = ring.filter((p) => p.connected);
  const pool = live.length > 0 ? live : ring;
  return pool.find((p) => p.seat > afterSeat) ?? pool[0] ?? null;
}

// ===== phase openers =====
interface Carried {
  resolutions: OverUnderResolution[];
  participation: PlayerId[];
  subjectsUsed: PlayerId[];
}

/** Ask core to assign the subject privately; this step has no serializable view. */
function assignSubject(ctx: GameCtx, loop: number, loops: number, carried: Carried): GameStep {
  const eligibleIds = eligibleSubjectIds(ctx, carried.subjectsUsed);
  if (eligibleIds.length === 0) return { gameState: ctx.state.gameState, effects: [], done: true };
  const $spotlight: SpotlightRequest = { roles: ['subject'], eligibleIds };
  const gameState: OverUnderAssignState & { $spotlight: SpotlightRequest } = {
    sub: 'ASSIGN',
    loop,
    loops,
    resolutions: carried.resolutions,
    participation: carried.participation,
    subjectsUsed: carried.subjectsUsed,
    $spotlight,
  };
  return { gameState, effects: [] };
}

/** Assignment settled: choose content for the final subject, then begin the card ceremony. */
function dealAssigned(ctx: GameCtx, pending: OverUnderAssignState, subjectId: PlayerId): GameStep {
  const subject = ctx.players.find((p) => p.id === subjectId);
  if (!subject) return skipAssignment(ctx, pending);
  const { primary, backup } = pickPair(ctx, poolForSubject(ctx, subject));
  const $deal: DealRequest = { primary, backup, subjectId };
  const gameState: OverUnderState & { $deal: DealRequest } = {
    sub: 'DEAL',
    loop: pending.loop,
    loops: pending.loops,
    subjectId,
    scribeId: null,
    card: null, // secret until the ceremony completes (CORE_DEALT)
    line: null,
    debateDeadline: 0,
    debateReassigns: 0,
    bets: {},
    betDeadline: 0,
    blocking: null,
    resolutions: pending.resolutions,
    participation: pending.participation,
    subjectsUsed: [...pending.subjectsUsed, subjectId],
    $deal,
  };
  return { gameState, effects: [] };
}

/** No final subject means no fabricated victim: skip this scoreless loop. */
function skipAssignment(ctx: GameCtx, pending: OverUnderAssignState): GameStep {
  if (pending.loop + 1 < pending.loops) return assignSubject(ctx, pending.loop + 1, pending.loops, pending);
  return { gameState: pending, effects: [], done: true };
}

/** Narrow a dealt Card to an OverUnderCard (deck tag + both required extras present). */
function isOverUnderCard(c: unknown): c is OverUnderCard {
  return (
    typeof c === 'object' &&
    c !== null &&
    (c as { deck?: unknown }).deck === 'overunder' &&
    'receiptSurface' in c &&
    'timebox' in c
  );
}

/** Ceremony done: the stat is public to ALL. Seat the scribe, open the 25s LINE DEBATE. */
function openDebate(ctx: GameCtx, st: OverUnderState): GameStep {
  const dealt = ctx.state.deal?.done === true ? ctx.state.deal.card : null;
  if (!isOverUnderCard(dealt)) return noop(ctx); // stale CORE_DEALT
  const subject = ctx.players.find((p) => p.id === st.subjectId);
  if (!subject) return noop(ctx);
  const scribe = scribeFor(ctx.players, st.subjectId, subject.seat);
  if (scribe === null) return resolveVoided(ctx, st, [], 'void'); // unreachable at minN 3
  const deadline = ctx.now + DEBATE_MS;
  const gameState: OverUnderState & { $phase: { k: 'INPUT'; sub: string; deadline: number } } = {
    ...st,
    sub: 'DEBATE',
    card: dealt,
    scribeId: scribe.id,
    line: null,
    debateDeadline: deadline,
    $phase: { k: 'INPUT', sub: 'DEBATE', deadline },
  };
  return { gameState, effects: [{ k: 'SCHEDULE', timerId: debateTimerId(ctx.circleIdx, st.loop), atMs: deadline }] };
}

/** Line locked: open the 12s skippable BET. The subject watches; everyone else picks a side. */
function openBet(ctx: GameCtx, st: OverUnderState, pre: Effect[]): GameStep {
  if (st.line === null) return noop(ctx); // never lock a line nobody set
  const deadline = ctx.now + BET_MS;
  const gameState: OverUnderState & { $phase: { k: 'INPUT'; sub: string; deadline: number } } = {
    ...st,
    sub: 'BET',
    bets: {},
    betDeadline: deadline,
    $phase: { k: 'INPUT', sub: 'BET', deadline },
  };
  return { gameState, effects: [...pre, { k: 'SCHEDULE', timerId: betTimerId(ctx.circleIdx, st.loop), atMs: deadline }] };
}

/** Bets are in: the subject goes digging. BLOCKING (4.7) — deadline null, shame machinery armed. */
function openTruth(ctx: GameCtx, st: OverUnderState, pre: Effect[]): GameStep {
  const out = blockingBegin(st.subjectId, 'TRUTH', truthKey(ctx.circleIdx, st.loop), ctx.now);
  const gameState: OverUnderState & { $phase?: unknown } = {
    ...st,
    sub: 'TRUTH',
    blocking: out.blocking,
    ...(out.$phase !== undefined ? { $phase: out.$phase } : {}),
  };
  return { gameState, effects: [...pre, ...out.effects] };
}

/** Silent scribe: pass the dial along the ring, or void once everyone had it and shrugged. */
function reassignScribe(ctx: GameCtx, st: OverUnderState, afterSeat: number): GameStep {
  if (st.debateReassigns >= ctx.players.length - 2) {
    // every eligible scribe held the dial and nobody moved it — the loop dies loud
    return resolveVoided(ctx, st, [], 'void');
  }
  const scribe = scribeFor(ctx.players, st.subjectId, afterSeat);
  if (scribe === null) return resolveVoided(ctx, st, [], 'void');
  const deadline = ctx.now + DEBATE_MS;
  const gameState: OverUnderState & { $phase: { k: 'INPUT'; sub: string; deadline: number } } = {
    ...st,
    scribeId: scribe.id,
    debateReassigns: st.debateReassigns + 1,
    debateDeadline: deadline,
    $phase: { k: 'INPUT', sub: 'DEBATE', deadline }, // fresh 25s for the fresh scribe
  };
  return { gameState, effects: [{ k: 'SCHEDULE', timerId: debateTimerId(ctx.circleIdx, st.loop), atMs: deadline }] };
}

// ===== resolution =====
/** Loop dies scoreless (FIFTH / pit / host VOID / seat-lapse / dead debate): 0 to all (4.6). */
function resolveVoided(ctx: GameCtx, st: OverUnderState, pre: Effect[], sting: 'void' | 'fled' | null): GameStep {
  const cancels: Effect[] =
    st.sub === 'DEBATE'
      ? [{ k: 'CANCEL', timerId: debateTimerId(ctx.circleIdx, st.loop) }]
      : st.sub === 'BET'
        ? [{ k: 'CANCEL', timerId: betTimerId(ctx.circleIdx, st.loop) }]
        : []; // TRUTH terminals cancel their own timers via the blocking helper
  const stingFx: Effect[] = sting === null ? [] : [{ k: 'AUDIO', sting }]; // null = the blocking helper already sang
  const resolution: OverUnderResolution = {
    cardId: st.card?.id ?? null,
    subjectId: st.subjectId,
    scribeId: st.scribeId,
    line: st.line,
    truth: null, // voided loops NEVER carry a number — nothing was fabricated (D-115)
    unverified: false,
    push: false,
    voided: true,
    winners: [],
    split: { over: 0, under: 0 },
  };
  const gameState: OverUnderState & { $phase: { k: 'REVEAL' } } = {
    ...st,
    sub: 'REVEAL',
    resolutions: [...st.resolutions, resolution],
    $phase: { k: 'REVEAL' },
  };
  return { gameState, effects: [...pre, ...cancels, ...stingFx, { k: 'SNAPSHOT' }], scores: {} };
}

// Scoring (pre-multiplier — core applies finale x3 / Bargain x2):
//   +2 correct bet · push (truth == line) everyone +1 · participation +1 once per
//   circle for any bet cast. The RECEIPT BONUS is the subject's participation +1:
//   producing real receipts is how the subject participates. A scribe-downgraded
//   (UNVERIFIED, host-relayed) number pays the subject nothing — 4.7's "no receipt
//   bonus" — while bettors still settle. Imp bets score FULL points; their 0.5
//   weight is informational only (colors the split, never the payout).
function resolveTruth(ctx: GameCtx, st: OverUnderState, truth: number, unverified: boolean, pre: Effect[]): GameStep {
  if (st.line === null || st.card === null) return noop(ctx); // unreachable: TRUTH follows a locked BET
  const line = st.line;
  const push = truth === line;
  const everyone = [...ctx.players, ...ctx.imps];
  const bettors = everyone.filter((p) => p.id !== st.subjectId && st.bets[p.id] !== undefined);
  const winningSide: Bet | null = push ? null : truth > line ? 'over' : 'under';
  const winners = winningSide === null ? [] : bettors.filter((p) => st.bets[p.id] === winningSide).map((p) => p.id);
  const split = { over: 0, under: 0 };
  for (const p of bettors) {
    const side = st.bets[p.id];
    if (side !== undefined) split[side] += weightOf(p);
  }

  const scores: Record<string, number> = {};
  const add = (id: PlayerId, pts: number): void => {
    if (pts > 0) scores[id] = (scores[id] ?? 0) + pts;
  };
  const participation = [...st.participation];
  const spendParticipation = (id: PlayerId): void => {
    if (!participation.includes(id)) {
      participation.push(id);
      add(id, points('participation'));
    }
  };
  if (push) for (const p of everyone) add(p.id, points('overunder.push')); // exact line: the room wins, the subject is the median
  for (const id of winners) add(id, points('overunder.correctBet'));
  for (const p of bettors) spendParticipation(p.id);
  if (!unverified) spendParticipation(st.subjectId); // the receipt bonus — verified numbers only

  const resolution: OverUnderResolution = {
    cardId: st.card.id,
    subjectId: st.subjectId,
    scribeId: st.scribeId,
    line,
    truth,
    unverified,
    push,
    voided: false,
    winners,
    split,
  };
  // $phase REVEAL: the core stamps holdSince and owns the hold — synced flip beat,
  // DESCEND (host anytime / anyone past 45s), fire-decay past the 20s minimum (4.2).
  const gameState: OverUnderState & { $phase: { k: 'REVEAL' } } = {
    ...st,
    sub: 'REVEAL',
    resolutions: [...st.resolutions, resolution],
    participation,
    $phase: { k: 'REVEAL' },
  };
  return { gameState, effects: [...pre, { k: 'AUDIO', sting: 'boom' }, { k: 'SNAPSHOT' }], scores };
}

// ===== the module =====
export const overunderModule = {
  deck: 'overunder',
  minN: 3,

  start(ctx: GameCtx): GameStep {
    const loops = ctx.circle.loops >= 1 ? ctx.circle.loops : SUBJECTS_PER_CIRCLE;
    return assignSubject(ctx, 0, loops, { resolutions: [], participation: [], subjectsUsed: [] });
  },

  input(ctx: GameCtx, playerId: string, payload: unknown): GameStep {
    const st = readState(ctx);
    if (!st) return noop(ctx);

    if (st.sub === 'DEBATE') {
      if (playerId !== st.scribeId) return noop(ctx); // only the scribe holds the dial
      const dial = parseLine(payload);
      if (dial === null) return noop(ctx);
      const next: OverUnderState = { ...st, line: dial.line };
      if (dial.lock) {
        // scribe locked early — the room stops arguing and starts gambling
        return openBet(ctx, next, [{ k: 'CANCEL', timerId: debateTimerId(ctx.circleIdx, st.loop) }]);
      }
      return { gameState: next, effects: [{ k: 'BROADCAST' }] }; // dial is public: the argument is out loud anyway
    }

    if (st.sub === 'BET') {
      const bet = parseBet(payload);
      if (bet === null) return noop(ctx);
      const eligible = bettorsOf(ctx, st);
      const bettor = eligible.find((p) => p.id === playerId);
      if (!bettor) return noop(ctx); // the subject NEVER bets on their own number; ghosts neither
      const bets = { ...st.bets, [playerId]: bet }; // re-bet allowed until deadline: last one wins
      const next: OverUnderState = { ...st, bets };
      if (Object.keys(bets).length >= eligible.length) {
        // Room's all in — send the subject digging, don't make anyone stare at a countdown.
        return openTruth(ctx, next, [{ k: 'CANCEL', timerId: betTimerId(ctx.circleIdx, st.loop) }]);
      }
      return { gameState: next, effects: [{ k: 'BROADCAST' }] }; // count ticks up; view() hides who bet what
    }

    if (st.sub === 'TRUTH' && st.blocking !== null) {
      // Subject's number pad — the ONLY verified path to a truth value.
      if (playerId === st.subjectId) {
        const truth = parseCount(payload, 'truth');
        if (truth === null) return noop(ctx);
        const out = blockingResolveInput(st.blocking);
        if (out.blocking.resolved !== 'input') return noop(ctx); // already terminal
        return resolveTruth(ctx, { ...st, blocking: out.blocking }, truth, false, out.effects);
      }
      // Scribe-downgrade (4.7, overunder only): the HOST relays the subject's VERBAL
      // claim, flagged UNVERIFIED — a real human said the number out loud; the machine
      // fabricated nothing. No receipt bonus. (A host who IS the subject uses `truth`.)
      if (playerId === hostIdOf(ctx)) {
        const claim = parseCount(payload, 'claim');
        if (claim !== null) {
          const out = blockingResolveInput(st.blocking);
          if (out.blocking.resolved !== 'input') return noop(ctx);
          return resolveTruth(ctx, { ...st, blocking: out.blocking }, claim, true, out.effects);
        }
        // fall through — the host may just be pit-voting
      }
      const choice = parsePit(payload);
      if (choice !== null) {
        const out = blockingPitVote(st.blocking, playerId, choice, jurorsOf(ctx, st));
        if (out.blocking === st.blocking) return noop(ctx); // rejected ballot (pit closed / owner / ghost)
        if (out.blocking.resolved === 'voided') {
          return resolveVoided(ctx, { ...st, blocking: out.blocking }, out.effects, null); // helper already sang
        }
        return { gameState: { ...st, blocking: out.blocking }, effects: out.effects };
      }
      return noop(ctx);
    }

    return noop(ctx); // DEAL / REVEAL: nothing to say
  },

  timer(ctx: GameCtx, timerId: string): GameStep {
    const st = readState(ctx);
    if (!st) return noop(ctx);
    if (st.sub === 'ASSIGN' && timerId === CORE_SPOTLIGHT_DONE) {
      const subjectId = ctx.spotlight?.assignments.find((a) => a.role === 'subject')?.playerId ?? null;
      return subjectId === null ? skipAssignment(ctx, st) : dealAssigned(ctx, st, subjectId);
    }
    if (st.sub === 'DEAL' && timerId === CORE_DEALT) {
      return openDebate(ctx, st); // ceremony done: stat public, scribe seated
    }
    if (st.sub === 'DEBATE' && timerId === debateTimerId(ctx.circleIdx, st.loop)) {
      if (st.line !== null) return openBet(ctx, st, []); // deadline locks the dial where the argument left it
      const scribe = ctx.players.find((p) => p.id === st.scribeId);
      return reassignScribe(ctx, st, scribe?.seat ?? -1); // silent scribe: the dial moves on
    }
    if (st.sub === 'BET' && timerId === betTimerId(ctx.circleIdx, st.loop)) {
      return openTruth(ctx, st, []); // skippable: non-bettors auto-abstain — the game never waits
    }
    if (st.sub === 'TRUTH' && st.blocking !== null) {
      // 4.7 choreography: grace -> WAITING_ON the subject; 30s -> pit vote opens.
      const out = blockingTimerFired(st.blocking, timerId, ctx.now);
      if (out.blocking === st.blocking && out.$phase === undefined && out.effects.length === 0) return noop(ctx);
      const gameState: OverUnderState & { $phase?: unknown } = {
        ...st,
        blocking: out.blocking,
        ...(out.$phase !== undefined ? { $phase: out.$phase } : {}),
      };
      return { gameState, effects: out.effects };
    }
    if (st.sub === 'REVEAL' && timerId === CORE_REVEAL_DONE) {
      if (st.loop + 1 < st.loops) return assignSubject(ctx, st.loop + 1, st.loops, st); // next victim
      return { gameState: st, effects: [], done: true }; // core -> LADDER
    }
    return noop(ctx); // stale timer for a dead loop
  },

  control(ctx: GameCtx, playerId: string, kind: 'REST' | 'SKIPEM' | 'FIFTH' | 'VOID'): GameStep {
    const st = readState(ctx);
    if (!st || st.sub === 'ASSIGN') return noop(ctx);
    const hostId = hostIdOf(ctx);

    if (kind === 'FIFTH') {
      // The permanent escape hatch on the subject's blocking screen — nowhere else.
      if (st.sub !== 'TRUTH' || st.blocking === null || playerId !== st.subjectId) return noop(ctx);
      const out = blockingControl(st.blocking, playerId, 'FIFTH', hostId);
      if (out.blocking.resolved !== 'fifth') return noop(ctx);
      return resolveVoided(ctx, { ...st, blocking: out.blocking }, out.effects, null); // fifth sting from the helper
    }

    if (kind === 'VOID') {
      if (hostId !== null && playerId === hostId) {
        // Host kill-switch (4.7): the loop dies scoreless at any live sub-phase.
        if (st.sub === 'TRUTH' && st.blocking !== null) {
          const out = blockingControl(st.blocking, playerId, 'VOID', hostId);
          if (out.blocking.resolved !== 'voided') return noop(ctx);
          return resolveVoided(ctx, { ...st, blocking: out.blocking }, out.effects, null);
        }
        if (st.sub === 'DEBATE' || st.sub === 'BET') return resolveVoided(ctx, st, [], 'void');
        return noop(ctx); // DEAL ceremony rides out its 10s; REVEAL is already resolved
      }
      // Seat-lapse convention (engine 4.7): VOID carrying a NON-host id = that seat lapsed.
      if (playerId === st.subjectId) {
        // The witness fled. Without them there is no truth — the loop dies, never a number.
        if (st.sub === 'TRUTH' && st.blocking !== null) {
          const out = blockingControl(st.blocking, playerId, 'VOID', hostId);
          if (out.blocking.resolved !== 'voided') return noop(ctx);
          return resolveVoided(ctx, { ...st, blocking: out.blocking }, out.effects, null); // 'fled' sting from the helper
        }
        if (st.sub === 'REVEAL') return noop(ctx); // loop already settled
        return resolveVoided(ctx, st, [], 'fled'); // DEAL/DEBATE/BET: subject gone -> loop gone
      }
      if (playerId === st.scribeId && st.sub === 'DEBATE') {
        // Scribe fled mid-debate: the dial passes to the next seat (5.3 acceptance).
        const scribe = ctx.players.find((p) => p.id === st.scribeId);
        return reassignScribe(ctx, st, scribe?.seat ?? -1);
      }
      return noop(ctx); // a lapsed bettor just doesn't bet
    }

    return noop(ctx); // REST / SKIPEM: over/under has no performer
  },

  // The ONLY serialization surface. Redaction law (3.4 + 5.3):
  // - during the DEAL ceremony NOTHING serializes (the subject's pre-view travels
  //   via the core's private SEND, never through here);
  // - from CORE_DEALT on, the card + subject + scribe are public to ALL;
  // - bets are hidden until reveal: during BET/TRUTH a viewer sees counts + their
  //   OWN bet, nothing else; pit ballots likewise (own choice only);
  // - the true number exists on the wire at REVEAL and nowhere earlier;
  // - per-loop score deltas are never in the view (a +2 would out a bet early).
  view(ctx: GameCtx, viewerId: string): OverUnderView | null {
    const st = readState(ctx);
    if (!st || st.sub === 'ASSIGN' || st.sub === 'DEAL') return null;
    const base = {
      deck: 'overunder' as const,
      loop: st.loop,
      loops: st.loops,
      subjectId: st.subjectId,
      scribeId: st.scribeId,
    };
    const face: OverUnderCardFace | null =
      st.card === null
        ? null
        : { id: st.card.id, text: st.card.text, receiptSurface: st.card.receiptSurface, timebox: st.card.timebox };

    if (st.sub === 'DEBATE') {
      if (face === null) return null; // defensive: never leak a half-built loop
      return { ...base, sub: 'DEBATE', card: face, line: st.line, deadline: st.debateDeadline, youAreScribe: viewerId === st.scribeId };
    }
    if (st.sub === 'BET') {
      if (face === null || st.line === null) return null;
      return {
        ...base,
        sub: 'BET',
        card: face,
        line: st.line,
        deadline: st.betDeadline,
        eligible: bettorsOf(ctx, st).length,
        betCount: Object.keys(st.bets).length,
        youBet: st.bets[viewerId] ?? null,
        youAreSubject: viewerId === st.subjectId,
      };
    }
    if (st.sub === 'TRUTH') {
      if (face === null || st.line === null) return null;
      return {
        ...base,
        sub: 'TRUTH',
        card: face,
        line: st.line,
        waitingOn: st.subjectId,
        pitOpen: st.blocking?.pitOpen ?? false,
        youPitVoted: st.blocking?.pitVotes[viewerId] ?? null,
        youBet: st.bets[viewerId] ?? null,
        youAreSubject: viewerId === st.subjectId,
      };
    }
    const res = st.resolutions[st.resolutions.length - 1];
    if (!res) return null; // unreachable by construction; never leak on a bug
    return {
      ...base,
      sub: 'REVEAL',
      card: face,
      line: res.line,
      truth: res.truth,
      push: res.push,
      unverified: res.unverified,
      voided: res.voided,
      winners: res.winners,
      split: res.split,
      youBet: st.bets[viewerId] ?? null,
    };
  },
} satisfies GameModule;
