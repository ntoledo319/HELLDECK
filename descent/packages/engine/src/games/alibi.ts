// ALIBI DROP — spec 5.8 + HDRealRules2 Part III §8. Task D-133.
// Two accused per circle. Each is handed an accusation and three MANDATORY words —
// contraband they must smuggle into a 30s improvised alibi, out loud, on the spot.
// The contraband lives ONLY on the accused's phone. The jury then privately picks
// three suspected plants out of an eight-word lineup (the three plants shuffled in
// with five decoys), and the reveal flips the lineup PLANTED/DECOY one word at a
// time — a server-timed drumroll, not a text dump. The app keeps the plants secret;
// a human mouth has to lie convincingly enough to bury them.
//
// Module law (games/module.ts): state lives opaquely in RoomState.gameState;
// view() is the ONLY serialization surface, so every redaction rule lives there.
// Pure module: time arrives as ctx.now, randomness as ctx.rand. Never a clock.
//
// Core integration (engine.ts "module step protocol"), per loop — built like roast:
//   $spotlight {role: accused} (fixed private assignment + dodge windows)
//   -> $deal {subjectId: accused} (4.5 ceremony, 10s named-subject preview + burn
//     window: the accusation + words + decoys travel to the accused's socket via the
//     core's PRIVATE preview SEND, night-dedup writeback) -> CORE_DEALT (accusation
//     goes public; the three plants stay accused-only)
//   -> $phase INPUT "ALIBI" (30s verbal; words pinned to the accused's phone; [REST]
//      ends the improvisation early)
//   -> $phase INPUT "HUNT" (20s skippable; the jury sees an 8-word lineup, order
//      randomized PER VIEWER, and picks EXACTLY 3 suspected plants)
//   -> $phase REVEAL (core-held hold: DESCEND, fire-decay) WITH the module's own AT
//      beat timers layered on top so every phone flips the lineup word-by-word,
//      1.2s apart, PLANTED/DECOY, one sting per plant)
//   -> CORE_REVEAL_DONE -> next accused or done. No blocking inputs anywhere: a silent
//      juror abstains, a deadline resolves with whatever picks are in — never a wait.
import type { AlibiCard, DealRequest, Effect, Player, PlayerId, SpotlightRequest } from '../types.js';
import { cardLegal } from '../consent.js';
import { pick, rng } from '../rng.js';
import { points } from '../scoring.js';
import {
  CORE_DEALT,
  CORE_REVEAL_DONE,
  CORE_SPOTLIGHT_DONE,
  type GameCtx,
  type GameModule,
  type GameStep,
} from './module.js';

// ===== tuning (spec 5.8 / 4.6 / 4.8) =====
export const ACCUSED_PER_CIRCLE = 2;
export const ALIBI_MS = 30_000; // verbal improvisation; [REST] ends it early
export const HUNT_MS = 20_000; // HUNT is skippable: deadline -> non-hunters auto-abstain
export const BEAT_MS = 1_200; // stage-sequenced reveal: one word flip every 1.2s
export const LINEUP_SIZE = 8; // 3 plants + 5 decoys
export const PICK_COUNT = 3; // the jury names exactly three
export const IMP_WEIGHT = 0.5; // 4.8: imps hunt at half weight; ties are never decided by imps

// ===== module state (opaque to core; view() redacts) =====
export interface AlibiResolution {
  cardId: string;
  accusation: string;
  accusedId: PlayerId;
  results: { word: string; planted: boolean }[]; // STAGE ORDER (8) — the shared reveal sequence
  voided: boolean;
}

export interface AlibiState {
  sub: 'DEAL' | 'ALIBI' | 'HUNT' | 'REVEAL'; // DEAL = core ceremony running, card still secret
  loop: number; // 0-based accused index
  loops: number; // 2 per spec
  accusedId: PlayerId;
  card: AlibiCard | null; // null until CORE_DEALT (the ceremony owns the announce)
  picks: Record<PlayerId, string[]>; // juror -> 3 chosen words. SECRET — counts + own only until reveal
  alibiDeadline: number; // view mirrors; authoritative deadlines are the SCHEDULE effects
  huntDeadline: number;
  beat: number; // REVEAL: how many lineup words have flipped so far (AT-driven)
  resolutions: AlibiResolution[];
  participation: PlayerId[]; // once-per-circle +1 already paid (hunt ballot cast)
  accusedUsed: PlayerId[]; // this circle's accused incl. current — loop 2 excludes loop 1
}

interface AlibiAssignState {
  sub: 'ASSIGN';
  loop: number;
  loops: number;
  resolutions: AlibiResolution[];
  participation: PlayerId[];
  accusedUsed: PlayerId[];
}

type AlibiInternalState = AlibiAssignState | AlibiState;

// ===== views =====
interface AlibiViewBase {
  deck: 'alibi';
  loop: number;
  loops: number;
}
export type AlibiAlibiView = AlibiViewBase & {
  sub: 'ALIBI';
  accusation: string;
  accusedId: PlayerId;
  youAreAccused: boolean;
  words?: [string, string, string]; // the contraband — present ONLY on the accused's own view
  deadline: number;
};
export type AlibiHuntView = AlibiViewBase & {
  sub: 'HUNT';
  accusation: string;
  accusedId: PlayerId;
  lineup: string[]; // 8 words, plants MIXED with decoys, order randomized PER VIEWER
  youPicked: string[] | null; // your own three, nobody else's
  eligible: number;
  pickedCount: number; // counts only — never who, never what
  deadline: number;
};
export type AlibiRevealView = AlibiViewBase & {
  sub: 'REVEAL';
  accusation: string;
  accusedId: PlayerId;
  results: { word: string; planted: boolean }[]; // stage order; PLANTED exists on the wire HERE and nowhere earlier
  youPicked: string[] | null;
  beat: number; // client flips word N once beat reaches N
  voided: boolean;
};
export type AlibiView = AlibiAlibiView | AlibiHuntView | AlibiRevealView;

// ===== deck registry =====
// Card plumbing arrives with content integration (D-127/8.3); until then the core
// (or a test) injects a deck here. Stubs keep dev nights honest: each accusation is
// a specific, feral "account for yourself" — the three plants and five decoys share a
// category so you can't sniff out a plant by vibe, only by catching it in the alibi.
export const DEFAULT_ALIBI_DECK: AlibiCard[] = [
  stub('001', 'Every photo from the trip is just you mid-blink. Explain the sabotage.', 2, 3, 'observational', 'blink-sabotage',
    ['tripod', 'flashcube', 'lanyard'], ['viewfinder', 'shutter', 'monopod', 'filmstrip', 'bokeh']),
  stub('002', 'You vanished for forty minutes at the wedding. So did the gravy boat.', 3, 4, 'menace', 'wedding-vanish',
    ['bouquet', 'cummerbund', 'gravy'], ['boutonniere', 'taffeta', 'canape', 'trellis', 'confetti']),
  stub('003', "Building security has forty minutes of you 'testing' every showroom mattress.", 2, 4, 'absurdist', 'mattress-testing',
    ['duvet', 'lumbar', 'sham'], ['bolster', 'coil', 'ottoman', 'valance', 'gusset']),
  stub('004', 'Your ex swears you still have a key and a grudge. Account for last night.', 4, 4, 'table-aware', 'ex-key-grudge',
    ['deadbolt', 'fern', 'decanter'], ['transom', 'sconce', 'runner', 'trivet', 'doorjamb']),
  stub('005', "There's a 3 AM receipt in your name from a store that only sells regret.", 3, 4, 'euphemism', 'novelty-receipt',
    ['inflatable', 'whoopee', 'disco'], ['streamer', 'kazoo', 'glowstick', 'pinata', 'sparkler']),
  stub('006', "The noise complaint just says 'the chanting.' It was your address.", 3, 5, 'menace', 'chanting-complaint',
    ['candle', 'robe', 'chalice'], ['incense', 'pendant', 'sigil', 'brazier', 'tapestry']),
  stub('007', 'You left the party cradling a houseplant that was not yours.', 2, 3, 'petty-domestic', 'stolen-houseplant',
    ['terracotta', 'succulent', 'trellis'], ['philodendron', 'saucer', 'mulch', 'pothos', 'fronds']),
  stub('008', 'Your search history for Tuesday is one long apology. Walk us through Tuesday.', 4, 4, 'deadpan', 'apology-history',
    ['ladder', 'tarp', 'floodlight'], ['grommet', 'ratchet', 'carabiner', 'bungee', 'dolly']),
  stub('009', 'Ninety minutes at the gym and you touched exactly zero weights. Explain.', 2, 3, 'observational', 'gym-no-weights',
    ['kettlebell', 'chalk', 'sauna'], ['dumbbell', 'treadmill', 'elliptical', 'locker', 'protein']),
];
function stub(
  n: string,
  accusation: string,
  exposure: AlibiCard['exposure'],
  chaos: AlibiCard['chaos'],
  register: AlibiCard['register'],
  skeleton: string,
  words: [string, string, string],
  decoys: [string, string, string, string, string],
): AlibiCard {
  return { id: `alibi_stub_${n}`, deck: 'alibi', text: accusation, exposure, chaos, register, skeleton, accusation, words, decoys };
}

let deckCards: AlibiCard[] = DEFAULT_ALIBI_DECK;
export function setAlibiDeck(cards: readonly AlibiCard[]): void {
  if (cards.length === 0) throw new Error('alibi deck cannot be empty');
  deckCards = [...cards];
}

// ===== helpers =====
export const alibiTimerId = (circleIdx: number, loop: number): string => `alibi:alibi:${circleIdx}:${loop}`;
export const huntTimerId = (circleIdx: number, loop: number): string => `alibi:hunt:${circleIdx}:${loop}`;
export const beatTimerId = (circleIdx: number, loop: number, n: number): string => `alibi:beat:${circleIdx}:${loop}:${n}`;

const weightOf = (p: Player): number => (p.role === 'imp' ? IMP_WEIGHT : 1);
const hostIdOf = (ctx: GameCtx): PlayerId | null => ctx.players.find((p) => p.role === 'host')?.id ?? null;
/** Everyone but the accused hunts — imps too, at half weight (4.8). */
const jurorsOf = (ctx: GameCtx, st: AlibiState): Player[] =>
  [...ctx.players, ...ctx.imps].filter((p) => p.id !== st.accusedId);
/** The eight-word lineup: three plants smuggled in among five decoys, both off the card. */
const lineupOf = (card: AlibiCard): string[] => [...card.words, ...card.decoys];

function readState(ctx: GameCtx): AlibiInternalState | null {
  const gs = ctx.state.gameState as AlibiInternalState | null | undefined;
  return gs && (gs.sub === 'ASSIGN' || gs.sub === 'DEAL' || gs.sub === 'ALIBI' || gs.sub === 'HUNT' || gs.sub === 'REVEAL') ? gs : null;
}

function noop(ctx: GameCtx): GameStep {
  return { gameState: ctx.state.gameState, effects: [] };
}

function shuffleWith<T>(rand: () => number, arr: readonly T[]): T[] {
  const out = [...arr];
  for (let i = out.length - 1; i > 0; i--) {
    const j = Math.floor(rand() * (i + 1));
    const a = out[i] as T;
    out[i] = out[j] as T;
    out[j] = a;
  }
  return out;
}

/** Parse a HUNT ballot: exactly three DISTINCT words. Membership in the lineup is checked by the caller. */
function parsePicks(payload: unknown): string[] | null {
  if (typeof payload !== 'object' || payload === null) return null;
  const v = (payload as Record<string, unknown>)['picks'];
  if (!Array.isArray(v) || v.length !== PICK_COUNT) return null;
  const words = v.filter((x): x is string => typeof x === 'string' && x.length > 0);
  if (words.length !== PICK_COUNT) return null;
  if (new Set(words).size !== PICK_COUNT) return null; // three DISTINCT accusations
  return words;
}

// ===== accused + card selection =====
// Consent (4.4): an alibi is ABOUT the accused — the per-subject ceiling applies, so
// no accusation runs hotter than the person who has to explain it agreed to. Rung
// preference (4.3) and night-dedup ride on top. No Fresh Meat gate: the puzzle is
// self-contained (§10 lists the gated games; not us).
function legalFor(ctx: GameCtx, accused: Player): AlibiCard[] {
  const ceilings = ctx.players.map((p) => p.heatCeiling);
  const legal = deckCards.filter((c) => cardLegal(c.exposure, { ceilings, subjectCeilings: [accused.heatCeiling] }));
  if (legal.length === 0) return [];
  const underRung = legal.filter((c) => c.exposure <= ctx.circle.rung);
  return underRung.length > 0 ? underRung : legal;
}

function eligibleAccusedIds(ctx: GameCtx, exclude: readonly PlayerId[]): PlayerId[] {
  let candidates = ctx.players.filter((p) => !exclude.includes(p.id));
  if (candidates.length === 0) candidates = [...ctx.players];
  const legal = candidates.filter((p) => legalFor(ctx, p).length > 0);
  return (legal.length > 0 ? legal : candidates).map((p) => p.id);
}

/** Resolve content against the final assignee, never the player who burned out. */
function poolForAccused(ctx: GameCtx, accused: Player): AlibiCard[] {
  const legal = legalFor(ctx, accused);
  if (legal.length > 0) return legal;
  const minE = Math.min(...deckCards.map((c) => c.exposure));
  return deckCards.filter((c) => c.exposure === minE);
}

/** Primary + reserved backup (4.5) from the accused's legal pool, night-deduped via usedCardIds. */
function pickCardPair(ctx: GameCtx, pool: AlibiCard[]): { primary: AlibiCard; backup: AlibiCard } {
  const fresh = pool.filter((c) => !ctx.state.usedCardIds.includes(c.id));
  const src = fresh.length > 0 ? fresh : pool; // deck exhausted: a repeat beats a dead night
  const primary = pick(ctx.rand, src);
  const rest = src.filter((c) => c.id !== primary.id);
  const fallback = pool.filter((c) => c.id !== primary.id);
  const backup = rest.length > 0 ? pick(ctx.rand, rest) : fallback.length > 0 ? pick(ctx.rand, fallback) : primary;
  return { primary, backup };
}

// ===== phase openers =====
interface Carried {
  resolutions: AlibiResolution[];
  participation: PlayerId[];
  accusedUsed: PlayerId[];
}

/**
 * Hand the accusation to the core's 4.5 ceremony with the accused as NAMED subject:
 * 10s private preview (accusation + words + decoys travel via the core's PRIVATE SEND,
 * never through here) + burn window, public announce of the ACCUSATION at completion,
 * night-dedup writeback. The three plants never touch a non-accused frame.
 */
function assignAccused(ctx: GameCtx, loop: number, loops: number, carried: Carried): GameStep {
  const eligibleIds = eligibleAccusedIds(ctx, carried.accusedUsed);
  if (eligibleIds.length === 0) return { gameState: ctx.state.gameState, effects: [], done: true };
  const $spotlight: SpotlightRequest = { roles: ['accused'], eligibleIds };
  const gameState: AlibiAssignState & { $spotlight: SpotlightRequest } = {
    sub: 'ASSIGN',
    loop,
    loops,
    resolutions: carried.resolutions,
    participation: carried.participation,
    accusedUsed: carried.accusedUsed,
    $spotlight,
  };
  return { gameState, effects: [] };
}

function dealAssigned(ctx: GameCtx, pending: AlibiAssignState, accusedId: PlayerId): GameStep {
  const accused = ctx.players.find((p) => p.id === accusedId);
  if (!accused) return skipAssignment(ctx, pending);
  const { primary, backup } = pickCardPair(ctx, poolForAccused(ctx, accused));
  const $deal: DealRequest = { primary, backup, subjectId: accusedId };
  const gameState: AlibiState & { $deal: DealRequest } = {
    sub: 'DEAL',
    loop: pending.loop,
    loops: pending.loops,
    accusedId,
    card: null, // secret until the ceremony completes (CORE_DEALT)
    picks: {},
    alibiDeadline: 0,
    huntDeadline: 0,
    beat: 0,
    resolutions: pending.resolutions,
    participation: pending.participation,
    accusedUsed: [...pending.accusedUsed, accusedId],
    $deal,
  };
  return { gameState, effects: [] };
}

function skipAssignment(ctx: GameCtx, pending: AlibiAssignState): GameStep {
  if (pending.loop + 1 < pending.loops) return assignAccused(ctx, pending.loop + 1, pending.loops, pending);
  return { gameState: pending, effects: [], done: true };
}

/** Ceremony done: the accusation is public, the words pinned to the accused's phone. Open the 30s alibi. */
function openAlibi(ctx: GameCtx, st: AlibiState): GameStep {
  const dealt = ctx.state.deal?.done === true ? ctx.state.deal.card : null;
  if (dealt === null || dealt.deck !== 'alibi') return noop(ctx); // stale CORE_DEALT
  const card = dealt as AlibiCard; // honors a burn swap: whatever survived the ceremony
  const deadline = ctx.now + ALIBI_MS;
  const gameState: AlibiState & { $phase: { k: 'INPUT'; sub: string; deadline: number } } = {
    ...st,
    sub: 'ALIBI',
    card,
    alibiDeadline: deadline,
    $phase: { k: 'INPUT', sub: 'ALIBI', deadline },
  };
  return { gameState, effects: [{ k: 'SCHEDULE', timerId: alibiTimerId(ctx.circleIdx, st.loop), atMs: deadline }] };
}

/** The alibi is delivered (timer or [REST]): the jury gets the 20s skippable WORD HUNT. */
function openHunt(ctx: GameCtx, st: AlibiState, pre: Effect[]): GameStep {
  if (st.card === null) return noop(ctx); // unreachable: ALIBI always has a card
  const deadline = ctx.now + HUNT_MS;
  const gameState: AlibiState & { $phase: { k: 'INPUT'; sub: string; deadline: number } } = {
    ...st,
    sub: 'HUNT',
    picks: {},
    huntDeadline: deadline,
    $phase: { k: 'INPUT', sub: 'HUNT', deadline },
  };
  return { gameState, effects: [...pre, { k: 'SCHEDULE', timerId: huntTimerId(ctx.circleIdx, st.loop), atMs: deadline }] };
}

// ===== hunt math =====
/**
 * Missed-majority with the imp clause (4.8), reusing confession's juryMajority shape.
 * Every juror casts one of two ballots per plant — FOUND (picked it) or MISSED (didn't,
 * abstaining counts as missed) — so "missed > found" IS ">50% of the jury missed it".
 * Imps count at half weight, but a tie is never DECIDED by imps: a full-vote (players
 * only) tie means the plant was NOT missed by a majority (the jury gets the benefit),
 * and imps alone may carry the call only when every juror is an imp.
 */
function missedMajority(weighted: { found: number; missed: number }, full: { found: number; missed: number }): 'found' | 'missed' | 'tie' {
  if (full.found === full.missed && full.found + full.missed > 0) return 'tie'; // players split — imps don't decide
  if (full.found !== full.missed) {
    const leader = full.missed > full.found ? 'missed' : 'found';
    if (weighted.found === weighted.missed) return leader;
    return weighted.missed > weighted.found ? 'missed' : 'found';
  }
  if (weighted.found === weighted.missed) return 'tie'; // no player jurors AND imps split
  return weighted.missed > weighted.found ? 'missed' : 'found';
}

/** Did >50% of the jury FAIL to find this plant? (abstain = missed; imps at half weight, never decisive) */
function plantMissed(plant: string, jurors: Player[], picks: Record<PlayerId, string[]>): boolean {
  const weighted = { found: 0, missed: 0 };
  const full = { found: 0, missed: 0 };
  for (const j of jurors) {
    const key = picks[j.id]?.includes(plant) ? 'found' : 'missed';
    weighted[key] += weightOf(j);
    if (j.role !== 'imp') full[key] += 1;
  }
  return missedMajority(weighted, full) === 'missed';
}

/** The stage-sequenced word-flip schedule layered on top of the core reveal hold. */
function buildBeats(circleIdx: number, loop: number, now: number): Effect[] {
  const out: Effect[] = [];
  for (let n = 1; n <= LINEUP_SIZE; n++) out.push({ k: 'SCHEDULE', timerId: beatTimerId(circleIdx, loop, n), atMs: now + n * BEAT_MS });
  return out;
}
function cancelBeats(circleIdx: number, loop: number): Effect[] {
  const out: Effect[] = [];
  for (let n = 1; n <= LINEUP_SIZE; n++) out.push({ k: 'CANCEL', timerId: beatTimerId(circleIdx, loop, n) });
  return out;
}
function beatIndexOf(timerId: string, circleIdx: number, loop: number): number | null {
  for (let n = 1; n <= LINEUP_SIZE; n++) if (timerId === beatTimerId(circleIdx, loop, n)) return n;
  return null;
}

// ===== resolution =====
/**
 * Scoring (pre-multiplier — core applies finale x3 / Bargain x2):
 *   each juror +1 per PLANTED word they correctly found; the accused +1 per planted
 *   word MISSED by >50% of the jury; participation +1 once per circle for any hunt
 *   ballot cast. The accused earns by burying plants in a convincing lie — get every
 *   plant caught, walk with nothing. Imp ballots score FULL points; their 0.5 weight
 *   only shapes the missed-majority math (confession precedent).
 * A voided loop (host VOID / accused fled): 0 to all, no word choreography — just the
 * VOID banner (4.6).
 */
function resolve(ctx: GameCtx, st: AlibiState, pre: Effect[], voided = false, voidSting: 'void' | 'fled' = 'void'): GameStep {
  if (st.card === null) return noop(ctx); // unreachable: HUNT/ALIBI always have a card
  const jurors = jurorsOf(ctx, st);
  const plants = st.card.words;
  const picks = voided ? {} : st.picks;

  // Stage order: a seeded shuffle of the 8 words — the SAME sequence on every phone,
  // server-timed (AT per word). Not per-viewer: the reveal is a shared drumroll.
  const results = shuffleWith(ctx.rand, lineupOf(st.card)).map((word) => ({ word, planted: plants.includes(word) }));

  const scores: Record<string, number> = {};
  const participation = [...st.participation];
  if (!voided) {
    for (const j of jurors) {
      const p = picks[j.id];
      if (p === undefined) continue; // abstainers earn nothing — not even participation
      let pts = 0;
      const found = plants.filter((w) => p.includes(w)).length;
      if (found > 0) pts += points('alibi.plantFound', found);
      if (!participation.includes(j.id)) {
        participation.push(j.id);
        pts += points('participation');
      }
      if (pts > 0) scores[j.id] = (scores[j.id] ?? 0) + pts;
    }
    const missed = plants.filter((w) => plantMissed(w, jurors, picks)).length;
    if (missed > 0) scores[st.accusedId] = (scores[st.accusedId] ?? 0) + points('alibi.wordMissed', missed);
  }

  const resolution: AlibiResolution = {
    cardId: st.card.id,
    accusation: st.card.accusation,
    accusedId: st.accusedId,
    results,
    voided,
  };
  // $phase REVEAL: the core stamps holdSince and owns the hold — DESCEND (host anytime /
  // anyone past 45s), fire-decay past the 20s minimum (4.2). The word-by-word flip rides
  // on the module's own AT beats (1.2s apart), scheduled here alongside the hold.
  const gameState: AlibiState & { $phase: { k: 'REVEAL' } } = {
    ...st,
    sub: 'REVEAL',
    beat: 0,
    resolutions: [...st.resolutions, resolution],
    participation,
    $phase: { k: 'REVEAL' },
  };
  const sting = voided ? voidSting : 'boom';
  const beats = voided ? [] : buildBeats(ctx.circleIdx, st.loop, ctx.now);
  return { gameState, effects: [...pre, { k: 'AUDIO', sting }, { k: 'SNAPSHOT' }, ...beats], scores };
}

// ===== the module =====
export const alibiModule = {
  deck: 'alibi',
  minN: 3,

  start(ctx: GameCtx): GameStep {
    const loops = ctx.circle.loops >= 1 ? ctx.circle.loops : ACCUSED_PER_CIRCLE;
    return assignAccused(ctx, 0, loops, { resolutions: [], participation: [], accusedUsed: [] });
  },

  input(ctx: GameCtx, playerId: string, payload: unknown): GameStep {
    const st = readState(ctx);
    if (!st || st.sub !== 'HUNT' || st.card === null) return noop(ctx); // stale ballots die silently (3.5)
    const jurors = jurorsOf(ctx, st);
    if (!jurors.some((j) => j.id === playerId)) return noop(ctx); // the accused never hunts; ghosts neither
    const picks = parsePicks(payload);
    if (picks === null) return noop(ctx);
    const lineup = lineupOf(st.card);
    if (!picks.every((w) => lineup.includes(w))) return noop(ctx); // only words on the lineup are votable
    const nextPicks = { ...st.picks, [playerId]: picks }; // re-pick allowed until the deadline: last ballot wins
    const next: AlibiState = { ...st, picks: nextPicks };
    if (Object.keys(nextPicks).length >= jurors.length) {
      // Jury's all in — don't make the accused sweat a countdown.
      return resolve(ctx, next, [{ k: 'CANCEL', timerId: huntTimerId(ctx.circleIdx, st.loop) }]);
    }
    return { gameState: next, effects: [{ k: 'BROADCAST' }] }; // count ticks up; view() hides who and what
  },

  timer(ctx: GameCtx, timerId: string): GameStep {
    const st = readState(ctx);
    if (!st) return noop(ctx);
    if (st.sub === 'ASSIGN' && timerId === CORE_SPOTLIGHT_DONE) {
      const accusedId = ctx.spotlight?.assignments.find((a) => a.role === 'accused')?.playerId ?? null;
      return accusedId === null ? skipAssignment(ctx, st) : dealAssigned(ctx, st, accusedId);
    }
    if (st.sub === 'DEAL' && timerId === CORE_DEALT) {
      return openAlibi(ctx, st); // ceremony done: accusation public, words pinned to the accused
    }
    if (st.sub === 'ALIBI' && timerId === alibiTimerId(ctx.circleIdx, st.loop)) {
      return openHunt(ctx, st, []); // 30s up whether the lie was finished or not
    }
    if (st.sub === 'HUNT' && timerId === huntTimerId(ctx.circleIdx, st.loop)) {
      return resolve(ctx, st, []); // deadline: non-hunters auto-abstain — the game never waits
    }
    if (st.sub === 'REVEAL') {
      if (timerId === CORE_REVEAL_DONE) {
        const cancels = cancelBeats(ctx.circleIdx, st.loop); // any un-fired flips (early DESCEND) die here
        if (st.loop + 1 < st.loops) {
          const next = assignAccused(ctx, st.loop + 1, st.loops, st);
          return { ...next, effects: [...cancels, ...next.effects] };
        }
        return { gameState: st, effects: cancels, done: true }; // core -> LADDER
      }
      const n = beatIndexOf(timerId, ctx.circleIdx, st.loop);
      if (n !== null) {
        if (n <= st.beat) return noop(ctx); // stale/duplicate flip — never walk the counter backward
        return { gameState: { ...st, beat: n }, effects: [{ k: 'BROADCAST' }] }; // one more word flips PLANTED/DECOY
      }
    }
    return noop(ctx); // stale timer for a dead loop
  },

  control(ctx: GameCtx, playerId: string, kind: 'REST' | 'SKIPEM' | 'FIFTH' | 'VOID'): GameStep {
    const st = readState(ctx);
    if (!st || st.sub === 'ASSIGN') return noop(ctx);
    const hostId = hostIdOf(ctx);

    if (kind === 'REST') {
      // [REST] ends the improvisation early — the accused only, mid-ALIBI only.
      if (st.sub !== 'ALIBI' || playerId !== st.accusedId) return noop(ctx);
      return openHunt(ctx, st, [{ k: 'CANCEL', timerId: alibiTimerId(ctx.circleIdx, st.loop) }]);
    }

    if (kind === 'VOID') {
      if (hostId !== null && playerId === hostId) {
        // Host kill-switch (4.7): the loop dies scoreless at any live sub-phase.
        if (st.sub === 'ALIBI' || st.sub === 'HUNT') return resolve(ctx, st, cancelLive(ctx, st), true, 'void');
        return noop(ctx); // DEAL rides out its ceremony; REVEAL is already resolved
      }
      // Seat-lapse convention (engine 4.7): VOID carrying a NON-host id = that seat lapsed.
      if (playerId === st.accusedId) {
        // No accused, no alibi — but only while they're still the one on the spot. Once the
        // alibi is delivered, the jury hunts a ghost (confession's "testimony's in" precedent).
        if (st.sub === 'ALIBI') return resolve(ctx, st, cancelLive(ctx, st), true, 'fled');
        return noop(ctx); // DEAL rides out; HUNT/REVEAL proceed without them
      }
      return noop(ctx); // a lapsed juror just doesn't hunt
    }

    return noop(ctx); // SKIPEM / FIFTH: alibi has no re-deal valve and no blocking input
  },

  // The ONLY serialization surface. Redaction law (3.4 + 5.8):
  // - during the DEAL ceremony NOTHING serializes (the accused's private preview —
  //   accusation + words + decoys — travels via the core's PRIVATE SEND, never here);
  // - the three PLANTED words appear in NO non-accused frame before REVEAL: on the
  //   accused's own ALIBI view as pinned contraband, and mixed indistinguishably into
  //   the HUNT lineup among the decoys (that mix IS the puzzle) — which are plants stays
  //   server-only until the flip;
  // - HUNT ballots are hidden: counts + your OWN three only, and the lineup order is a
  //   viewer-seeded shuffle (stable per socket, different across phones — no shoulder-surf);
  // - REVEAL carries the PLANTED/DECOY truth (the secret's out) plus a beat counter the
  //   client animates against; per-loop score deltas never serialize (a +N would out a ballot).
  view(ctx: GameCtx, viewerId: string): AlibiView | null {
    const st = readState(ctx);
    if (!st || st.sub === 'ASSIGN' || st.sub === 'DEAL' || st.card === null) return null;
    const base = { deck: 'alibi' as const, loop: st.loop, loops: st.loops };
    const accusation = st.card.accusation;

    if (st.sub === 'ALIBI') {
      const you = viewerId === st.accusedId;
      return {
        ...base,
        sub: 'ALIBI',
        accusation,
        accusedId: st.accusedId,
        youAreAccused: you,
        ...(you ? { words: st.card.words } : {}), // the contraband, the accused's eyes only
        deadline: st.alibiDeadline,
      };
    }

    if (st.sub === 'HUNT') {
      // Randomized PER VIEWER: a viewer-seeded shuffle — deterministic for the same
      // socket (re-renders stable), different across phones (no shoulder-surf sync).
      const lineup = shuffleWith(rng(`alibi:${ctx.circleIdx}:${st.loop}:${viewerId}`), lineupOf(st.card));
      return {
        ...base,
        sub: 'HUNT',
        accusation,
        accusedId: st.accusedId,
        lineup,
        youPicked: st.picks[viewerId] ?? null,
        eligible: jurorsOf(ctx, st).length,
        pickedCount: Object.keys(st.picks).length,
        deadline: st.huntDeadline,
      };
    }

    const res = st.resolutions[st.resolutions.length - 1];
    if (!res) return null; // unreachable by construction; never leak on a bug
    return {
      ...base,
      sub: 'REVEAL',
      accusation: res.accusation,
      accusedId: res.accusedId,
      results: res.results,
      youPicked: st.picks[viewerId] ?? null,
      beat: st.beat,
      voided: res.voided,
    };
  },
} satisfies GameModule;

/** Cancel the live phase timer on a void path. */
function cancelLive(ctx: GameCtx, st: AlibiState): Effect[] {
  if (st.sub === 'ALIBI') return [{ k: 'CANCEL', timerId: alibiTimerId(ctx.circleIdx, st.loop) }];
  if (st.sub === 'HUNT') return [{ k: 'CANCEL', timerId: huntTimerId(ctx.circleIdx, st.loop) }];
  return [];
}
