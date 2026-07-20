// CONFESSION OR CAP — spec 5.4 + HDRealRules2 Part III §4. Task D-123.
// Two confessors per circle. Each gets three sins in private, picks the one they
// can sell, locks TRUE or FALSE where nobody can see, and performs it to a jury
// that votes BELIEVE or CAP. Majority wrong -> the liar eats +3. Tie -> the
// confessor wins ("HUNG JURY — THE LIAR WALKS"). The app keeps the secret;
// humans supply the tells.
//
// Module law (games/module.ts): state lives opaquely in RoomState.gameState;
// view() is the ONLY serialization surface, so every redaction rule lives there.
// Pure module: time arrives as ctx.now, randomness as ctx.rand. Never a clock.
//
// Core integration (engine.ts "module step protocol"), per loop — built like roast:
//   $spotlight {role: confessor} (fixed private assignment + dodge windows)
//   -> $phase INPUT "PICK" (20s; the hand of 3 exists ONLY in the confessor's view;
//     the room doesn't even learn WHO is choosing — private assignment, 4.5)
//   -> pick (or auto-pick sin #1 at the deadline) -> $deal {subjectId: confessor}
//     (4.5: 10s ceremony, PRIVATE pre-view + burn window on the CHOSEN card,
//     night-dedup writeback for the chosen card ONLY — the two unchosen sins
//     return to the pool untraced, per the 5.4 acceptance) -> CORE_DEALT
//     (card + confessor go public together)
//   -> blockingBegin: $phase INPUT "LOCK" deadline null (timer provably PAUSED;
//     TRUE/FALSE is ground truth: grace -> WAITING_ON -> pit vote / FIFTH /
//     host VOID / seat-lapse terminals — NEVER fabricated, test D-115)
//   -> $phase INPUT "PERFORM" (45s verbal; [I REST MY CASE] ends it early)
//   -> $phase INPUT "JURY" (12s skippable; BELIEVE/CAP; confessor excluded)
//   -> $phase REVEAL (core-held hold: flip beat, DESCEND, fire-decay) — the
//     TRUE/FALSE stamp + jury spread + fooled/caught banner
//   -> CORE_REVEAL_DONE -> next confessor or done.
import type { CardBase, DealRequest, Effect, Player, PlayerId, SpotlightRequest } from '../types.js';
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

// ===== tuning (spec 5.4 / 4.6 / 4.7 / 4.8) =====
export const CONFESSORS_PER_CIRCLE = 2;
export const HAND_SIZE = 3; // pick-of-3: consent and strategy in one move
export const PICK_MS = 20_000; // DEAL-3: skippable for the game (auto-pick #1), private to the confessor
export const PERFORM_MS = 45_000; // verbal sell; [I REST MY CASE] ends it early
export const JURY_MS = 12_000; // skippable: deadline -> non-voters auto-abstain
export const IMP_WEIGHT = 0.5; // 4.8: imps vote at half weight; ties are never decided by imps

export type JuryVote = 'believe' | 'cap';
export type Verdict = 'FOOLED' | 'CAUGHT' | 'HUNG';

// ===== module state (opaque to core; view() redacts) =====
export interface ConfessionResolution {
  cardId: string | null; // null only when the loop died before a sin was chosen
  cardText: string | null;
  confessorId: PlayerId;
  truth: boolean | null; // stamped HERE and nowhere earlier; null on a voided loop — the lock dies unstamped
  verdict: Verdict | null; // null = voided
  spread: { believe: number; cap: number }; // weighted counts (imps 0.5) — counts only, never who
  voided: boolean;
}

export interface ConfessionState {
  sub: 'PICK' | 'DEAL' | 'LOCK' | 'PERFORM' | 'JURY' | 'REVEAL'; // DEAL = core ceremony running
  loop: number; // 0-based confessor index
  loops: number; // 2 per spec
  confessorId: PlayerId;
  hand: CardBase[]; // PICK only — in NO view but the confessor's; cleared at the ceremony
  card: CardBase | null; // public from CORE_DEALT (the ceremony owns the announce)
  truthLock: boolean | null; // THE secret. In no view — not even the confessor's own — until the reveal
  votes: Record<PlayerId, JuryVote>; // SECRET until reveal — view() shows counts + your own only
  pickDeadline: number; // view mirrors; authoritative deadlines are the SCHEDULE effects
  performDeadline: number;
  juryDeadline: number;
  blocking: BlockingState | null; // 4.7 machinery, LOCK only
  resolutions: ConfessionResolution[];
  participation: PlayerId[]; // once-per-circle +1 already paid (jury ballots cast)
  confessorsUsed: PlayerId[]; // this circle's confessors incl. current — loop 2 excludes loop 1
}

interface ConfessionAssignState {
  sub: 'ASSIGN';
  loop: number;
  loops: number;
  resolutions: ConfessionResolution[];
  participation: PlayerId[];
  confessorsUsed: PlayerId[];
}

type ConfessionInternalState = ConfessionAssignState | ConfessionState;

// ===== views =====
export interface ConfessionCardFace {
  id: string;
  text: string;
}
interface ConfessionViewBase {
  deck: 'confession';
  loop: number;
  loops: number;
}
export type ConfessionPickView = ConfessionViewBase & {
  sub: 'PICK';
  deadline: number;
  youAreConfessor: boolean;
  hand?: ConfessionCardFace[]; // present ONLY on the confessor's own view
};
export type ConfessionLockView = ConfessionViewBase & {
  sub: 'LOCK';
  confessorId: PlayerId; // public from CORE_DEALT on
  card: ConfessionCardFace;
  youAreConfessor: boolean; // your phone renders TRUE/FALSE + [FIFTH]; others wait
  pitOpen: boolean; // 30s in: DRAG THEM BACK / FEED THEM TO THE PIT unlock
  youPitVoted: 'drag' | 'pit' | null;
};
export type ConfessionPerformView = ConfessionViewBase & {
  sub: 'PERFORM';
  confessorId: PlayerId;
  card: ConfessionCardFace;
  deadline: number;
  youAreConfessor: boolean; // "sell it" + [I REST MY CASE] vs "watch their hands" + fire
};
export type ConfessionJuryView = ConfessionViewBase & {
  sub: 'JURY';
  confessorId: PlayerId;
  card: ConfessionCardFace;
  deadline: number;
  eligible: number;
  votedCount: number; // counts only — never who
  youVoted: JuryVote | null; // your own ballot, nobody else's
  youAreConfessor: boolean; // "they're deciding your fate"
};
export type ConfessionRevealView = ConfessionViewBase & {
  sub: 'REVEAL';
  confessorId: PlayerId;
  card: ConfessionCardFace | null;
  truth: boolean | null; // the stamp exists on the wire HERE and nowhere earlier
  verdict: Verdict | null;
  spread: { believe: number; cap: number };
  voided: boolean;
  youVoted: JuryVote | null;
};
export type ConfessionView =
  | ConfessionPickView
  | ConfessionLockView
  | ConfessionPerformView
  | ConfessionJuryView
  | ConfessionRevealView;

// ===== deck registry =====
// Card plumbing arrives with content integration (D-127/8.3); until then the core
// (or a test) injects a deck here. Stubs keep dev nights honest: every sin is
// first-person, sellable straight OR as a cap — ambiguity is the whole game.
export const DEFAULT_CONFESSION_DECK: CardBase[] = [
  stub('001', "I've eaten food out of a trash can as an adult. Sober.", 3, 4, 'gross', 'trash-cuisine'),
  stub('002', 'I once ghosted someone because their laugh embarrassed me in public.', 3, 3, 'petty-domestic', 'laugh-ghost'),
  stub('003', "I still check an ex's horoscope to make sure they're suffering.", 3, 4, 'menace', 'ex-horoscope'),
  stub('004', 'I have cried at a commercial and blamed allergies. This year.', 2, 3, 'deadpan', 'commercial-tears'),
  stub('005', 'I re-gifted a present back to the exact person who gave it to me.', 2, 4, 'petty-domestic', 'regift-boomerang'),
  stub('006', "I've held a full fake phone call to dodge one (1) coworker.", 1, 3, 'observational', 'fake-call'),
  stub('007', "I know a friend's phone passcode and I have used it. Recently.", 4, 4, 'menace', 'passcode-crime'),
  stub('008', 'I faked food poisoning to leave a date early. The food was innocent.', 3, 3, 'deadpan', 'date-poisoning'),
  stub('009', "I've rehearsed a comeback in the mirror for an argument that ended weeks ago.", 2, 3, 'absurdist', 'mirror-rematch'),
  stub('010', 'I have sniffed clothes off the floor and worn them somewhere that mattered.', 2, 3, 'gross', 'floor-formal'),
];
function stub(
  n: string,
  text: string,
  exposure: CardBase['exposure'],
  chaos: CardBase['chaos'],
  register: CardBase['register'],
  skeleton: string,
): CardBase {
  return { id: `confession_stub_${n}`, deck: 'confession', text, exposure, chaos, register, skeleton };
}

let deckCards: CardBase[] = DEFAULT_CONFESSION_DECK;
export function setConfessionDeck(cards: readonly CardBase[]): void {
  if (cards.length === 0) throw new Error('confession deck cannot be empty');
  deckCards = [...cards];
}

// ===== helpers =====
export const pickTimerId = (circleIdx: number, loop: number): string => `confession:pick:${circleIdx}:${loop}`;
export const performTimerId = (circleIdx: number, loop: number): string => `confession:perform:${circleIdx}:${loop}`;
export const juryTimerId = (circleIdx: number, loop: number): string => `confession:jury:${circleIdx}:${loop}`;
export const lockKey = (circleIdx: number, loop: number): string => `confession:${circleIdx}:${loop}`;

const weightOf = (p: Player): number => (p.role === 'imp' ? IMP_WEIGHT : 1);
const hostIdOf = (ctx: GameCtx): PlayerId | null => ctx.players.find((p) => p.role === 'host')?.id ?? null;
/** Everyone but the confessor is jury — imps too, at half weight (4.8). */
const jurorsOf = (ctx: GameCtx, st: ConfessionState): Player[] =>
  [...ctx.players, ...ctx.imps].filter((p) => p.id !== st.confessorId);
const blockingJurorsOf = (ctx: GameCtx, st: ConfessionState): BlockingJuror[] =>
  jurorsOf(ctx, st).map((p) => ({ id: p.id, weight: weightOf(p) }));

function readState(ctx: GameCtx): ConfessionInternalState | null {
  const gs = ctx.state.gameState as ConfessionInternalState | null | undefined;
  return gs &&
    (gs.sub === 'ASSIGN' || gs.sub === 'PICK' || gs.sub === 'DEAL' || gs.sub === 'LOCK' || gs.sub === 'PERFORM' || gs.sub === 'JURY' || gs.sub === 'REVEAL')
    ? gs
    : null;
}

function noop(ctx: GameCtx): GameStep {
  return { gameState: ctx.state.gameState, effects: [] };
}

// ===== payload parsing =====
function parsePick(payload: unknown, handSize: number): number | null {
  if (typeof payload !== 'object' || payload === null) return null;
  const v = (payload as Record<string, unknown>)['pick'];
  return typeof v === 'number' && Number.isInteger(v) && v >= 0 && v < handSize ? v : null;
}
function parseTruth(payload: unknown): boolean | null {
  if (typeof payload !== 'object' || payload === null) return null;
  const v = (payload as Record<string, unknown>)['truth'];
  return typeof v === 'boolean' ? v : null;
}
function parseJuryVote(payload: unknown): JuryVote | null {
  if (typeof payload !== 'object' || payload === null) return null;
  const v = (payload as Record<string, unknown>)['vote'];
  return v === 'believe' || v === 'cap' ? v : null;
}
function parsePit(payload: unknown): 'drag' | 'pit' | null {
  if (typeof payload !== 'object' || payload === null) return null;
  const v = (payload as Record<string, unknown>)['pit'];
  return v === 'drag' || v === 'pit' ? v : null;
}

// ===== confessor + card selection =====
// Consent (4.4): a confession is ABOUT the confessor — the per-subject ceiling
// applies, so no sin runs hotter than the person selling it agreed to. Pick-of-3
// then hands them the final say ("consent and strategy in one move", §4). Rung
// preference (4.3) and night-dedup ride on top. No Fresh Meat gate: sins are
// self-contained — no crew knowledge required (§10 lists the gated games; not us).
function legalFor(ctx: GameCtx, confessor: Player): CardBase[] {
  const ceilings = ctx.players.map((p) => p.heatCeiling);
  const legal = deckCards.filter((c) => cardLegal(c.exposure, { ceilings, subjectCeilings: [confessor.heatCeiling] }));
  if (legal.length === 0) return [];
  const underRung = legal.filter((c) => c.exposure <= ctx.circle.rung);
  return underRung.length > 0 ? underRung : legal;
}

/** Only candidates with subject-legal sins enter the core-owned ceremony. */
function eligibleConfessorIds(ctx: GameCtx, exclude: readonly PlayerId[]): PlayerId[] {
  let candidates = ctx.players.filter((p) => !exclude.includes(p.id));
  if (candidates.length === 0) candidates = [...ctx.players];
  const legal = candidates.filter((p) => legalFor(ctx, p).length > 0);
  return (legal.length > 0 ? legal : candidates).map((p) => p.id);
}

/** Recompute the hand pool for the final assignee, including a burn replacement. */
function poolForConfessor(ctx: GameCtx, confessor: Player): CardBase[] {
  const legal = legalFor(ctx, confessor);
  if (legal.length > 0) return legal;
  const minE = Math.min(...deckCards.map((c) => c.exposure));
  return deckCards.filter((c) => c.exposure === minE);
}

/** Draw up to 3 DISTINCT sins, fresh-first (night-dedup via usedCardIds). A thin deck deals a thin hand. */
function drawHand(ctx: GameCtx, pool: CardBase[]): CardBase[] {
  const hand: CardBase[] = [];
  const unheld = (src: CardBase[]): CardBase[] => src.filter((c) => !hand.some((h) => h.id === c.id));
  const fresh = pool.filter((c) => !ctx.state.usedCardIds.includes(c.id));
  const stale = pool.filter((c) => ctx.state.usedCardIds.includes(c.id));
  while (hand.length < HAND_SIZE) {
    const src = unheld(fresh).length > 0 ? unheld(fresh) : unheld(stale);
    if (src.length === 0) break;
    hand.push(pick(ctx.rand, src));
  }
  return hand;
}

/**
 * Backup for the 4.5 ceremony (reserved so a burn swap costs nothing observable).
 * Prefer a card from OUTSIDE the hand — the unchosen sins stay truly untouched —
 * then an unchosen hand card, then the chosen itself (deck-of-one: a burn has
 * nowhere to go; roast precedent).
 */
function pickBackup(ctx: GameCtx, confessor: Player, chosen: CardBase, hand: readonly CardBase[]): CardBase {
  const legal = legalFor(ctx, confessor);
  const pool = legal.length > 0 ? legal : deckCards;
  const handIds = hand.map((c) => c.id);
  const outside = pool.filter((c) => c.id !== chosen.id && !handIds.includes(c.id));
  const freshOutside = outside.filter((c) => !ctx.state.usedCardIds.includes(c.id));
  if (freshOutside.length > 0) return pick(ctx.rand, freshOutside);
  if (outside.length > 0) return pick(ctx.rand, outside);
  const unchosen = hand.filter((c) => c.id !== chosen.id);
  if (unchosen.length > 0) return pick(ctx.rand, unchosen);
  return chosen;
}

// ===== phase openers =====
interface Carried {
  resolutions: ConfessionResolution[];
  participation: PlayerId[];
  confessorsUsed: PlayerId[];
}

/** Begin core's private confessor assignment before drawing any hand. */
function assignConfessor(ctx: GameCtx, loop: number, loops: number, carried: Carried): GameStep {
  const eligibleIds = eligibleConfessorIds(ctx, carried.confessorsUsed);
  if (eligibleIds.length === 0) return { gameState: ctx.state.gameState, effects: [], done: true };
  const $spotlight: SpotlightRequest = { roles: ['confessor'], eligibleIds };
  const gameState: ConfessionAssignState & { $spotlight: SpotlightRequest } = {
    sub: 'ASSIGN',
    loop,
    loops,
    resolutions: carried.resolutions,
    participation: carried.participation,
    confessorsUsed: carried.confessorsUsed,
    $spotlight,
  };
  return { gameState, effects: [] };
}

/** Assignment locked: open DEAL-3 only for the final confessor. */
function dealHandAssigned(ctx: GameCtx, pending: ConfessionAssignState, confessorId: PlayerId): GameStep {
  const confessor = ctx.players.find((p) => p.id === confessorId);
  if (!confessor) return skipAssignment(ctx, pending);
  const hand = drawHand(ctx, poolForConfessor(ctx, confessor));
  const deadline = ctx.now + PICK_MS;
  const gameState: ConfessionState & { $phase: { k: 'INPUT'; sub: string; deadline: number } } = {
    sub: 'PICK',
    loop: pending.loop,
    loops: pending.loops,
    confessorId,
    hand,
    card: null,
    truthLock: null,
    votes: {},
    pickDeadline: deadline,
    performDeadline: 0,
    juryDeadline: 0,
    blocking: null,
    resolutions: pending.resolutions,
    participation: pending.participation,
    confessorsUsed: [...pending.confessorsUsed, confessorId],
    $phase: { k: 'INPUT', sub: 'PICK', deadline },
  };
  return { gameState, effects: [{ k: 'SCHEDULE', timerId: pickTimerId(ctx.circleIdx, pending.loop), atMs: deadline }] };
}

function skipAssignment(ctx: GameCtx, pending: ConfessionAssignState): GameStep {
  if (pending.loop + 1 < pending.loops) return assignConfessor(ctx, pending.loop + 1, pending.loops, pending);
  return { gameState: pending, effects: [], done: true };
}

/**
 * Sin chosen: hand it to the core's 4.5 ceremony with the confessor as NAMED
 * subject — private pre-view + 10s burn window on the chosen card, public
 * announce at completion, night-dedup writeback for the CHOSEN card only.
 * The unchosen sins return to the pool untraced (5.4 acceptance).
 */
function toCeremony(ctx: GameCtx, st: ConfessionState, chosen: CardBase, pre: Effect[]): GameStep {
  const confessor = ctx.players.find((p) => p.id === st.confessorId);
  const backup = confessor ? pickBackup(ctx, confessor, chosen, st.hand) : chosen;
  const $deal: DealRequest = { primary: chosen, backup, subjectId: st.confessorId };
  const gameState: ConfessionState & { $deal: DealRequest } = {
    ...st,
    sub: 'DEAL',
    hand: [], // the unchosen sins evaporate — never dealt, never marked, never seen
    $deal,
  };
  return { gameState, effects: pre };
}

/** Ceremony done: card + confessor are public. The TRUE/FALSE lock is BLOCKING (4.7). */
function openLock(ctx: GameCtx, st: ConfessionState): GameStep {
  const dealt = ctx.state.deal?.done === true ? ctx.state.deal.card : null;
  if (dealt === null || dealt.deck !== 'confession') return noop(ctx); // stale CORE_DEALT
  const out = blockingBegin(st.confessorId, 'LOCK', lockKey(ctx.circleIdx, st.loop), ctx.now);
  const gameState: ConfessionState & { $phase?: unknown } = {
    ...st,
    sub: 'LOCK',
    card: dealt, // honors a burn swap: this is whatever survived the ceremony
    blocking: out.blocking,
    ...(out.$phase !== undefined ? { $phase: out.$phase } : {}),
  };
  return { gameState, effects: out.effects };
}

/** Truth locked (a REAL human answer — the only path here): 45s to sell it. */
function openPerform(ctx: GameCtx, st: ConfessionState, pre: Effect[]): GameStep {
  const deadline = ctx.now + PERFORM_MS;
  const gameState: ConfessionState & { $phase: { k: 'INPUT'; sub: string; deadline: number } } = {
    ...st,
    sub: 'PERFORM',
    performDeadline: deadline,
    $phase: { k: 'INPUT', sub: 'PERFORM', deadline },
  };
  return { gameState, effects: [...pre, { k: 'SCHEDULE', timerId: performTimerId(ctx.circleIdx, st.loop), atMs: deadline }] };
}

/** The case is closed (timer or [I REST MY CASE]): 12s skippable BELIEVE/CAP. */
function openJury(ctx: GameCtx, st: ConfessionState, pre: Effect[]): GameStep {
  const deadline = ctx.now + JURY_MS;
  const gameState: ConfessionState & { $phase: { k: 'INPUT'; sub: string; deadline: number } } = {
    ...st,
    sub: 'JURY',
    votes: {},
    juryDeadline: deadline,
    $phase: { k: 'INPUT', sub: 'JURY', deadline },
  };
  return { gameState, effects: [...pre, { k: 'SCHEDULE', timerId: juryTimerId(ctx.circleIdx, st.loop), atMs: deadline }] };
}

// ===== resolution =====
/**
 * Majority with the imp clause (4.8): imps vote at half weight, but a tie is
 * never DECIDED by imps — a full-vote tie hangs no matter what imps did, and an
 * imp-manufactured weighted tie collapses back to the full-vote leader. Imp
 * weight still counts whenever no tie is in play, and imps alone may carry a
 * verdict if every player abstained (roast precedent). Nobody at all -> hung:
 * no jury conviction, the liar walks.
 */
function juryMajority(
  weighted: { believe: number; cap: number },
  full: { believe: number; cap: number },
): JuryVote | 'hung' {
  if (full.believe === full.cap && full.believe > 0) return 'hung';
  if (full.believe !== full.cap) {
    const fullLeader: JuryVote = full.believe > full.cap ? 'believe' : 'cap';
    if (weighted.believe === weighted.cap) return fullLeader;
    return weighted.believe > weighted.cap ? 'believe' : 'cap';
  }
  if (weighted.believe === weighted.cap) return 'hung'; // includes the empty jury box
  return weighted.believe > weighted.cap ? 'believe' : 'cap';
}

/** Loop dies scoreless (FIFTH / pit / host VOID / seat-lapse): 0 to all (4.6). */
function resolveVoided(ctx: GameCtx, st: ConfessionState, pre: Effect[], sting: 'void' | 'fled' | null): GameStep {
  const cancels: Effect[] =
    st.sub === 'PICK'
      ? [{ k: 'CANCEL', timerId: pickTimerId(ctx.circleIdx, st.loop) }]
      : st.sub === 'PERFORM'
        ? [{ k: 'CANCEL', timerId: performTimerId(ctx.circleIdx, st.loop) }]
        : st.sub === 'JURY'
          ? [{ k: 'CANCEL', timerId: juryTimerId(ctx.circleIdx, st.loop) }]
          : []; // LOCK terminals cancel their own timers via the blocking helper
  const stingFx: Effect[] = sting === null ? [] : [{ k: 'AUDIO', sting }]; // null = the blocking helper already sang
  const resolution: ConfessionResolution = {
    cardId: st.card?.id ?? null,
    cardText: st.card?.text ?? null,
    confessorId: st.confessorId,
    truth: null, // a voided loop NEVER stamps the lock — the secret dies with it (Part 10 whitelist)
    verdict: null,
    spread: { believe: 0, cap: 0 },
    voided: true,
  };
  const gameState: ConfessionState & { $phase: { k: 'REVEAL' } } = {
    ...st,
    sub: 'REVEAL',
    hand: [], // whatever survives a mid-PICK void evaporates unmarked
    truthLock: null, // scrubbed server-side too: voided truths exist nowhere
    resolutions: [...st.resolutions, resolution],
    $phase: { k: 'REVEAL' },
  };
  return { gameState, effects: [...pre, ...cancels, ...stingFx, { k: 'SNAPSHOT' }], scores: {} };
}

// Scoring (pre-multiplier — core applies finale x3 / Bargain x2):
//   confessor +3 when the jury majority is WRONG — and on a hung jury, because a
//   tie means the confessor wins ("HUNG JURY — THE LIAR WALKS"); each correct
//   voter +1 regardless of what the majority did; participation +1 once per
//   circle for any ballot cast. The confessor earns by fooling, not by showing
//   up: 4.6 pays participation for votes cast, and the confessor casts none —
//   get caught lying, walk with nothing. Imp ballots score FULL points; their
//   0.5 weight only shapes the majority math (overunder precedent).
function resolve(ctx: GameCtx, st: ConfessionState, pre: Effect[]): GameStep {
  if (st.card === null || st.truthLock === null) return noop(ctx); // unreachable: JURY follows a locked truth
  const jurors = jurorsOf(ctx, st);
  const weighted = { believe: 0, cap: 0 };
  const full = { believe: 0, cap: 0 };
  for (const j of jurors) {
    const v = st.votes[j.id];
    if (v === undefined) continue;
    weighted[v] += weightOf(j);
    if (j.role !== 'imp') full[v] += 1;
  }
  const correct: JuryVote = st.truthLock ? 'believe' : 'cap';
  const majority = juryMajority(weighted, full);
  const verdict: Verdict = majority === 'hung' ? 'HUNG' : majority === correct ? 'CAUGHT' : 'FOOLED';

  const scores: Record<string, number> = {};
  const add = (id: PlayerId, pts: number): void => {
    if (pts > 0) scores[id] = (scores[id] ?? 0) + pts;
  };
  const participation = [...st.participation];
  if (verdict !== 'CAUGHT') add(st.confessorId, points('confession.fooled')); // fooled OR hung: the liar walks with the pot
  for (const j of jurors) {
    const v = st.votes[j.id];
    if (v === undefined) continue; // abstainers earn nothing — not even participation
    if (v === correct) add(j.id, points('confession.correctVote'));
    if (!participation.includes(j.id)) {
      participation.push(j.id);
      add(j.id, points('participation'));
    }
  }

  const resolution: ConfessionResolution = {
    cardId: st.card.id,
    cardText: st.card.text,
    confessorId: st.confessorId,
    truth: st.truthLock,
    verdict,
    spread: weighted,
    voided: false,
  };
  // $phase REVEAL: the core stamps holdSince and owns the hold — synced flip beat,
  // DESCEND (host anytime / anyone past 45s), fire-decay past the 20s minimum (4.2).
  const gameState: ConfessionState & { $phase: { k: 'REVEAL' } } = {
    ...st,
    sub: 'REVEAL',
    resolutions: [...st.resolutions, resolution],
    participation,
    $phase: { k: 'REVEAL' },
  };
  return { gameState, effects: [...pre, { k: 'AUDIO', sting: 'boom' }, { k: 'SNAPSHOT' }], scores };
}

// ===== the module =====
export const confessionModule = {
  deck: 'confession',
  minN: 3,

  start(ctx: GameCtx): GameStep {
    const loops = ctx.circle.loops >= 1 ? ctx.circle.loops : CONFESSORS_PER_CIRCLE;
    return assignConfessor(ctx, 0, loops, { resolutions: [], participation: [], confessorsUsed: [] });
  },

  input(ctx: GameCtx, playerId: string, payload: unknown): GameStep {
    const st = readState(ctx);
    if (!st) return noop(ctx);

    if (st.sub === 'PICK') {
      if (playerId !== st.confessorId) return noop(ctx); // the sins are nobody else's business
      const idx = parsePick(payload, st.hand.length);
      if (idx === null) return noop(ctx);
      const chosen = st.hand[idx];
      if (chosen === undefined) return noop(ctx);
      return toCeremony(ctx, st, chosen, [{ k: 'CANCEL', timerId: pickTimerId(ctx.circleIdx, st.loop) }]);
    }

    if (st.sub === 'LOCK' && st.blocking !== null) {
      if (playerId === st.confessorId) {
        // The TRUE/FALSE lock — the ONLY path to a truth value (never fabricated, D-115).
        const truth = parseTruth(payload);
        if (truth === null) return noop(ctx);
        const out = blockingResolveInput(st.blocking);
        if (out.blocking.resolved !== 'input') return noop(ctx); // already terminal
        return openPerform(ctx, { ...st, truthLock: truth, blocking: out.blocking }, out.effects);
      }
      const choice = parsePit(payload);
      if (choice !== null) {
        const out = blockingPitVote(st.blocking, playerId, choice, blockingJurorsOf(ctx, st));
        if (out.blocking === st.blocking) return noop(ctx); // rejected ballot (pit closed / owner / ghost)
        if (out.blocking.resolved === 'voided') {
          return resolveVoided(ctx, { ...st, blocking: out.blocking }, out.effects, null); // helper already sang
        }
        return { gameState: { ...st, blocking: out.blocking }, effects: out.effects };
      }
      return noop(ctx);
    }

    if (st.sub === 'JURY') {
      const v = parseJuryVote(payload);
      if (v === null) return noop(ctx);
      const jurors = jurorsOf(ctx, st);
      if (!jurors.some((j) => j.id === playerId)) return noop(ctx); // the confessor never votes their own fate; ghosts neither
      const votes = { ...st.votes, [playerId]: v }; // re-vote allowed until deadline: last ballot wins
      const next: ConfessionState = { ...st, votes };
      if (Object.keys(votes).length >= jurors.length) {
        // Jury's all in — don't make the accused sweat a countdown for nothing.
        return resolve(ctx, next, [{ k: 'CANCEL', timerId: juryTimerId(ctx.circleIdx, st.loop) }]);
      }
      return { gameState: next, effects: [{ k: 'BROADCAST' }] }; // count ticks up; view() hides who
    }

    return noop(ctx); // DEAL / PERFORM / REVEAL: nothing to type
  },

  timer(ctx: GameCtx, timerId: string): GameStep {
    const st = readState(ctx);
    if (!st) return noop(ctx);
    if (st.sub === 'ASSIGN' && timerId === CORE_SPOTLIGHT_DONE) {
      const confessorId = ctx.spotlight?.assignments.find((a) => a.role === 'confessor')?.playerId ?? null;
      return confessorId === null ? skipAssignment(ctx, st) : dealHandAssigned(ctx, st, confessorId);
    }
    if (st.sub === 'PICK' && timerId === pickTimerId(ctx.circleIdx, st.loop)) {
      const first = st.hand[0];
      if (first === undefined) return resolveVoided(ctx, st, [], 'void'); // unreachable: hands are never dealt empty
      return toCeremony(ctx, st, first, []); // deadline: the deck picks for the ditherer — sin #1
    }
    if (st.sub === 'DEAL' && timerId === CORE_DEALT) {
      return openLock(ctx, st); // ceremony done: card + confessor public, lock pauses the clock
    }
    if (st.sub === 'LOCK' && st.blocking !== null) {
      // 4.7 choreography: grace -> WAITING_ON the confessor; 30s -> pit vote opens.
      const out = blockingTimerFired(st.blocking, timerId, ctx.now);
      if (out.blocking === st.blocking && out.$phase === undefined && out.effects.length === 0) return noop(ctx);
      const gameState: ConfessionState & { $phase?: unknown } = {
        ...st,
        blocking: out.blocking,
        ...(out.$phase !== undefined ? { $phase: out.$phase } : {}),
      };
      return { gameState, effects: out.effects };
    }
    if (st.sub === 'PERFORM' && timerId === performTimerId(ctx.circleIdx, st.loop)) {
      return openJury(ctx, st, []); // time's up whether the story was or not
    }
    if (st.sub === 'JURY' && timerId === juryTimerId(ctx.circleIdx, st.loop)) {
      return resolve(ctx, st, []); // deadline: non-voters auto-abstain — the game never waits
    }
    if (st.sub === 'REVEAL' && timerId === CORE_REVEAL_DONE) {
      if (st.loop + 1 < st.loops) return assignConfessor(ctx, st.loop + 1, st.loops, st); // next sinner
      return { gameState: st, effects: [], done: true }; // core -> LADDER
    }
    return noop(ctx); // stale timer for a dead loop
  },

  control(ctx: GameCtx, playerId: string, kind: 'REST' | 'SKIPEM' | 'FIFTH' | 'VOID'): GameStep {
    const st = readState(ctx);
    if (!st || st.sub === 'ASSIGN') return noop(ctx);
    const hostId = hostIdOf(ctx);

    if (kind === 'REST') {
      // I REST MY CASE — the performer only, mid-performance only. Ends the clock early.
      if (st.sub !== 'PERFORM' || playerId !== st.confessorId) return noop(ctx);
      return openJury(ctx, st, [{ k: 'CANCEL', timerId: performTimerId(ctx.circleIdx, st.loop) }]);
    }

    if (kind === 'FIFTH') {
      // The permanent escape hatch on the confessor's blocking screen — nowhere else.
      if (st.sub !== 'LOCK' || st.blocking === null || playerId !== st.confessorId) return noop(ctx);
      const out = blockingControl(st.blocking, playerId, 'FIFTH', hostId);
      if (out.blocking.resolved !== 'fifth') return noop(ctx);
      return resolveVoided(ctx, { ...st, blocking: out.blocking }, out.effects, null); // fifth sting from the helper
    }

    if (kind === 'VOID') {
      if (hostId !== null && playerId === hostId) {
        // Host kill-switch (4.7): the loop dies scoreless at any live sub-phase.
        if (st.sub === 'LOCK' && st.blocking !== null) {
          const out = blockingControl(st.blocking, playerId, 'VOID', hostId);
          if (out.blocking.resolved !== 'voided') return noop(ctx);
          return resolveVoided(ctx, { ...st, blocking: out.blocking }, out.effects, null);
        }
        if (st.sub === 'PICK' || st.sub === 'PERFORM' || st.sub === 'JURY') return resolveVoided(ctx, st, [], 'void');
        return noop(ctx); // DEAL rides out its ceremony; REVEAL is already resolved
      }
      // Seat-lapse convention (engine 4.7): VOID carrying a NON-host id = that seat lapsed.
      if (playerId === st.confessorId) {
        if (st.sub === 'LOCK' && st.blocking !== null) {
          const out = blockingControl(st.blocking, playerId, 'VOID', hostId);
          if (out.blocking.resolved !== 'voided') return noop(ctx);
          return resolveVoided(ctx, { ...st, blocking: out.blocking }, out.effects, null); // 'fled' sting from the helper
        }
        if (st.sub === 'JURY' || st.sub === 'REVEAL') return noop(ctx); // testimony's in; a jury can convict a ghost
        return resolveVoided(ctx, st, [], 'fled'); // PICK/DEAL/PERFORM: no confessor, no confession
      }
      return noop(ctx); // a lapsed juror just doesn't vote
    }

    return noop(ctx); // SKIPEM: confession has no re-deal valve — you chose your sin
  },

  // The ONLY serialization surface. Redaction law (3.4 + 5.4):
  // - during PICK the hand of 3 exists ONLY on the confessor's socket, and the
  //   room does not learn WHO is choosing (no confessorId in anyone else's frame);
  // - during the DEAL ceremony NOTHING serializes (the pre-view travels via the
  //   core's private SEND, never through here);
  // - the truth lock is in NO view — not even the confessor's own (phones are
  //   face-up at a table; a glance must never spoil the stamp) — until REVEAL;
  // - jury ballots are hidden until reveal: counts + your OWN vote only, and the
  //   reveal spread is weighted counts — no voter-vote pair ever leaves the server;
  // - per-loop score deltas are never in the view (a +1 would out a correct vote).
  view(ctx: GameCtx, viewerId: string): ConfessionView | null {
    const st = readState(ctx);
    if (!st || st.sub === 'ASSIGN' || st.sub === 'DEAL') return null;
    const base = { deck: 'confession' as const, loop: st.loop, loops: st.loops };

    if (st.sub === 'PICK') {
      const you = viewerId === st.confessorId;
      return {
        ...base,
        sub: 'PICK',
        deadline: st.pickDeadline,
        youAreConfessor: you,
        ...(you ? { hand: st.hand.map((c) => ({ id: c.id, text: c.text })) } : {}),
      };
    }

    const face: ConfessionCardFace | null = st.card === null ? null : { id: st.card.id, text: st.card.text };

    if (st.sub === 'LOCK') {
      if (face === null) return null; // defensive: never leak a half-built loop
      return {
        ...base,
        sub: 'LOCK',
        confessorId: st.confessorId,
        card: face,
        youAreConfessor: viewerId === st.confessorId,
        pitOpen: st.blocking?.pitOpen ?? false,
        youPitVoted: st.blocking?.pitVotes[viewerId] ?? null,
      };
    }
    if (st.sub === 'PERFORM') {
      if (face === null) return null;
      return {
        ...base,
        sub: 'PERFORM',
        confessorId: st.confessorId,
        card: face,
        deadline: st.performDeadline,
        youAreConfessor: viewerId === st.confessorId,
      };
    }
    if (st.sub === 'JURY') {
      if (face === null) return null;
      return {
        ...base,
        sub: 'JURY',
        confessorId: st.confessorId,
        card: face,
        deadline: st.juryDeadline,
        eligible: jurorsOf(ctx, st).length,
        votedCount: Object.keys(st.votes).length,
        youVoted: st.votes[viewerId] ?? null,
        youAreConfessor: viewerId === st.confessorId,
      };
    }
    const res = st.resolutions[st.resolutions.length - 1];
    if (!res) return null; // unreachable by construction; never leak on a bug
    return {
      ...base,
      sub: 'REVEAL',
      confessorId: res.confessorId,
      card: res.cardId === null ? null : { id: res.cardId, text: res.cardText ?? '' },
      truth: res.truth,
      verdict: res.verdict,
      spread: res.spread,
      voided: res.voided,
      youVoted: st.votes[viewerId] ?? null,
    };
  },
} satisfies GameModule;
