// POISON PITCH — spec 5.6 + HDRealRules2 Part III §6. Task D-132.
// Two duels per circle. The deck hands two players opposite sides of a cursed
// dilemma in private, then makes them SELL it to the table. The room votes which
// PITCH won — not which option is right; whoever argued harder eats +3. The app
// assigns the sides and counts the ballots; the humans supply the venom.
//
// Module law (games/module.ts): state lives opaquely in RoomState.gameState;
// view() is the ONLY serialization surface, so every redaction rule lives there.
// Pure module: time arrives as ctx.now, randomness as ctx.rand. Never a clock.
//
// Core integration (engine.ts "module step protocol"), per duel — built like roast:
//   ASSIGN (two pitchers via pickSpotlight, opposite sides — the assignment lives
//     ONLY in gameState; view() serializes NOTHING while the ceremony runs, so the
//     room sees the core's generic DEAL, never who drew which side)
//   -> $deal {subjectId:null} (5.5 ceremony, night-dedup writeback) -> CORE_DEALT
//     (both options + pitcher names go public together as an "A vs B" fight card)
//   -> $phase INPUT "PITCH" (60s verbal window; [SKIP-'EM] re-deals a fresh dilemma
//     to the SAME pitchers once/circle; [REST] ends the window early)
//   -> $phase INPUT "VOTE" (12s skippable; non-pitchers pick which PITCH won)
//   -> $phase REVEAL (core owns the hold: 3-2-1 flip beat, DESCEND, fire-decay)
//   -> CORE_REVEAL_DONE -> next duel or done.
import type { DealRequest, Effect, PoisonCard, Player, PlayerId, SpotlightRequest } from '../types.js';
import { cardLegal } from '../consent.js';
import { pick } from '../rng.js';
import { points } from '../scoring.js';
import {
  CORE_DEALT,
  CORE_REVEAL_DONE,
  CORE_SPOTLIGHT_DONE,
  type GameCtx,
  type GameModule,
  type GameStep,
} from './module.js';

// ===== tuning (spec 5.6 / 4.6 / 4.8) =====
export const DUELS_PER_CIRCLE = 2;
export const PITCH_MS = 60_000; // 30s + 30s collapsed to one window (5.6 simplification); [REST]/[SKIP-'EM] live here
export const VOTE_MS = 12_000; // VOTE is skippable: deadline -> auto-abstain
export const IMP_WEIGHT = 0.5; // 4.8: imps vote at half weight; ties are never decided by imps
export const RATING_MIN = 1; // N=3 damage meter: judge rates each pitch 1..5
export const RATING_MAX = 5;
const SPLIT_PTS = 2; // tie -> split pot, +2 to each pitcher (sudden-death is a later valve; split now)

export type Side = 'A' | 'B';
export type DuelWinner = Side | 'tie';

// ===== module state (opaque to core; view() redacts) =====
export interface PoisonResolution {
  cardId: string;
  winner: DuelWinner; // 'tie' also renders a voided (no-contest) duel — spread is then 0-0
  spread: { A: number; B: number }; // weighted vote counts, or summed ratings (N=3) — counts only, never who
  voided: boolean;
}

export interface PoisonState {
  sub: 'DEAL' | 'PITCH' | 'VOTE' | 'REVEAL'; // DEAL = core ceremony running, sides still secret
  loop: number; // 0-based duel index
  loops: number; // 2 per spec
  pitcherA: PlayerId; // argues optionA — PRIVATE until PITCH (view() withholds it during DEAL)
  pitcherB: PlayerId; // argues optionB
  card: PoisonCard | null; // null until CORE_DEALT (the ceremony owns the reveal)
  votes: Record<PlayerId, Side>; // SECRET — view() shows counts + your own only
  ratings: Record<PlayerId, { A: number; B: number }>; // N=3 damage-meter ballots — same secrecy
  pitchDeadline: number; // view mirror; authoritative deadline is the SCHEDULE effect
  voteDeadline: number;
  skipUsed: boolean; // [SKIP-'EM] fires once per circle, carried across both duels
  resolutions: PoisonResolution[];
  participation: PlayerId[]; // once-per-circle +1 already paid (a ballot cast)
  pitchersUsed: PlayerId[]; // fairness memory — duel 2 prefers pitchers duel 1 didn't burn
}

interface PoisonAssignState {
  sub: 'ASSIGN';
  loop: number;
  loops: number;
  skipUsed: boolean;
  resolutions: PoisonResolution[];
  participation: PlayerId[];
  pitchersUsed: PlayerId[];
}

type PoisonInternalState = PoisonAssignState | PoisonState;

// ===== views =====
interface PoisonViewBase {
  deck: 'poison';
  loop: number;
  loops: number;
}
export type PoisonPitchView = PoisonViewBase & {
  sub: 'PITCH';
  optionA: string;
  optionB: string;
  pitcherA: PlayerId; // public from CORE_DEALT on
  pitcherB: PlayerId;
  youArePitcher: Side | null; // your phone renders "sell it + angle hints" vs "watch + fire"
  deadline: number;
};
export type PoisonVoteView = PoisonViewBase & {
  sub: 'VOTE';
  optionA: string;
  optionB: string;
  pitcherA: PlayerId;
  pitcherB: PlayerId;
  ratingMode: boolean; // N=3: render the 1..5 damage meter instead of A/B (the judge's phone needs to know)
  eligible: number;
  votedCount: number; // counts only — never who
  youVoted: Side | null; // your own call (in ratingMode: the side you rated higher)
  youRated?: { A: number; B: number }; // ONLY on the judge's own view — echoes their own ballot
  deadline: number;
};
export type PoisonRevealView = PoisonViewBase & {
  sub: 'REVEAL';
  winner: DuelWinner;
  pitcherA: PlayerId;
  pitcherB: PlayerId;
  optionA: string;
  optionB: string;
  spread: { A: number; B: number };
};
export type PoisonView = PoisonPitchView | PoisonVoteView | PoisonRevealView;

// ===== deck registry =====
// Card plumbing arrives with content integration (D-127/8.3); until then the core
// (or a test) injects a deck here. Stubs keep dev nights sharp: every dilemma forces
// a real fight — both options are cursed, neither is the "correct" answer, so the
// win is earned at the mic, not on the card.
export const DEFAULT_POISON_DECK: PoisonCard[] = [
  stub('001', 'Which hell do you sentence the group chat to for a year?',
    'Every group photo, you blink.',
    "Every group text, you're left on read for exactly one hour.",
    2, 3, 'petty-domestic', 'group-curse'),
  stub('002', 'Choose the worse public unmasking.',
    'Your search history projects on the bar TV for 30 seconds.',
    'Your voice memos to yourself autoplay at max volume.',
    4, 4, 'table-aware', 'public-unmask'),
  stub('003', 'Pick your eternal social punishment.',
    'You must react to every text with the crying-laughing emoji, forever.',
    "Autocorrect changes every 'lol' you send to 'i'm lonely.'",
    2, 3, 'absurdist', 'social-sentence'),
  stub('004', 'Which betrayal is more forgivable?',
    'Reading a friend’s diary and telling nobody.',
    'Telling one person and swearing them to secrecy.',
    3, 4, 'menace', 'betrayal-tier'),
  stub('005', 'Pick the worse superpower.',
    'You always know when people are lying — and they know you know.',
    "You read minds, but only the one thought they'd least want you to hear.",
    3, 4, 'absurdist', 'cursed-power'),
  stub('006', 'Which roommate crime gets the lighter sentence?',
    'Finishing the leftovers they were obviously saving.',
    "Using their toothbrush 'just once' in an emergency.",
    3, 4, 'gross', 'roommate-crime'),
  stub('007', 'Choose the more embarrassing way to go viral.',
    'A video of you crying in a Chili’s parking lot.',
    'A screenshot of the text you sent to exactly the wrong person.',
    3, 4, 'table-aware', 'viral-shame'),
  stub('008', 'Pick the worse thing to be caught doing.',
    'Rehearsing an argument in the mirror, out loud, doing both voices.',
    "Practicing a fake laugh for a party you don't even want to go to.",
    2, 3, 'deadpan', 'caught-doing'),
  stub('009', 'Which is the greater sin against the crew?',
    'Screenshotting the chat and sending it to an outsider.',
    'Muting the chat for a month and lying that you missed it.',
    3, 4, 'petty-domestic', 'crew-sin'),
  stub('010', 'Choose the funeral you’d rather sit through.',
    'The one where your ex delivers the eulogy.',
    'The one where this group chat is read aloud, unedited.',
    4, 5, 'menace', 'funeral-pick'),
];
function stub(
  n: string,
  text: string,
  optionA: string,
  optionB: string,
  exposure: PoisonCard['exposure'],
  chaos: PoisonCard['chaos'],
  register: PoisonCard['register'],
  skeleton: string,
): PoisonCard {
  return { id: `poison_stub_${n}`, deck: 'poison', text, optionA, optionB, exposure, chaos, register, skeleton };
}

let deckCards: PoisonCard[] = DEFAULT_POISON_DECK;
export function setPoisonDeck(cards: readonly PoisonCard[]): void {
  if (cards.length === 0) throw new Error('poison deck cannot be empty');
  deckCards = [...cards];
}

// ===== helpers =====
const pitchTimer = (circleIdx: number, loop: number): string => `poison:pitch:${circleIdx}:${loop}`;
const voteTimer = (circleIdx: number, loop: number): string => `poison:vote:${circleIdx}:${loop}`;
const weightOf = (p: Player): number => (p.role === 'imp' ? IMP_WEIGHT : 1);
// N=3 auto-variant (5.6): the lone non-pitcher becomes a single damage-meter judge.
const ratingMode = (ctx: GameCtx): boolean => ctx.players.length === 3;
/** Everyone but the two pitchers votes — imps too at half weight, EXCEPT the N=3 judge is a player. */
function votersOf(ctx: GameCtx, st: PoisonState): Player[] {
  const notPitcher = (p: Player): boolean => p.id !== st.pitcherA && p.id !== st.pitcherB;
  if (ratingMode(ctx)) return ctx.players.filter(notPitcher); // the single judge, imps excluded from the meter
  return [...ctx.players, ...ctx.imps].filter(notPitcher);
}

function readState(ctx: GameCtx): PoisonInternalState | null {
  const gs = ctx.state.gameState as PoisonInternalState | null | undefined;
  return gs && (gs.sub === 'ASSIGN' || gs.sub === 'DEAL' || gs.sub === 'PITCH' || gs.sub === 'VOTE' || gs.sub === 'REVEAL') ? gs : null;
}

function noop(ctx: GameCtx): GameStep {
  return { gameState: ctx.state.gameState, effects: [] };
}

// ===== payload parsing =====
function parseVote(payload: unknown): Side | null {
  if (typeof payload !== 'object' || payload === null) return null;
  const v = (payload as Record<string, unknown>)['vote'];
  return v === 'A' || v === 'B' ? v : null;
}
const isRating = (v: unknown): v is number =>
  typeof v === 'number' && Number.isInteger(v) && v >= RATING_MIN && v <= RATING_MAX;
function parseRate(payload: unknown): { A: number; B: number } | null {
  if (typeof payload !== 'object' || payload === null) return null;
  const r = (payload as Record<string, unknown>)['rate'];
  if (typeof r !== 'object' || r === null) return null;
  const a = (r as Record<string, unknown>)['A'];
  const b = (r as Record<string, unknown>)['B'];
  return isRating(a) && isRating(b) ? { A: a, B: b } : null;
}

// ===== card + pitcher selection =====
// A poison dilemma names no subject and isn't vote-emergent: it's generic content,
// so the generic room ceiling caps it (4.4). Rung preference (4.3) then night-dedup
// ride on top, roast-style.
function legalPool(ctx: GameCtx): PoisonCard[] {
  const ceilings = ctx.players.map((p) => p.heatCeiling);
  let legal = deckCards.filter((c) => cardLegal(c.exposure, { ceilings }));
  if (legal.length === 0) {
    // Content bug (nothing legal): degrade to the mildest dilemmas rather than
    // deadlock the night. The arc builder gates depth on legal content; this is the backstop.
    const minE = Math.min(...deckCards.map((c) => c.exposure));
    legal = deckCards.filter((c) => c.exposure === minE);
  }
  const underRung = legal.filter((c) => c.exposure <= ctx.circle.rung);
  return underRung.length > 0 ? underRung : legal;
}

/** Pick primary + reserved backup (4.5), night-deduped via usedCardIds (core writes back on ceremony completion). */
function pickCardPair(ctx: GameCtx): { primary: PoisonCard; backup: PoisonCard } {
  const legal = legalPool(ctx);
  const fresh = legal.filter((c) => !ctx.state.usedCardIds.includes(c.id));
  const pool = fresh.length > 0 ? fresh : legal; // deck exhausted: a repeat beats a dead duel
  const primary = pick(ctx.rand, pool);
  const rest = pool.filter((c) => c.id !== primary.id);
  const fallback = legal.filter((c) => c.id !== primary.id);
  const backup = rest.length > 0 ? pick(ctx.rand, rest) : fallback.length > 0 ? pick(ctx.rand, fallback) : primary;
  return { primary, backup };
}

function eligiblePitcherIds(ctx: GameCtx, exclude: readonly PlayerId[]): PlayerId[] {
  let candidates = ctx.players.filter((p) => !exclude.includes(p.id));
  if (candidates.length < 2) candidates = [...ctx.players];
  return candidates.map((p) => p.id);
}

// ===== phase openers =====
interface Carried {
  resolutions: PoisonResolution[];
  participation: PlayerId[];
  pitchersUsed: PlayerId[];
  skipUsed: boolean;
}

/**
 * Assign the duel and hand its dilemma to the 4.5 ceremony. subjectId is null: both
 * options go public at once (no per-person pre-view, no card-burn window — the ASSIGN
 * spotlight-burn is D-134, and SKIP-'EM covers the "hate this dilemma" escape). The
 * side assignment lives in gameState alone; view() serializes nothing until PITCH.
 */
function assignDuel(ctx: GameCtx, loop: number, loops: number, carried: Carried): GameStep {
  const eligibleIds = eligiblePitcherIds(ctx, carried.pitchersUsed);
  if (eligibleIds.length < 2) return { gameState: ctx.state.gameState, effects: [], done: true };
  const $spotlight: SpotlightRequest = { roles: ['pitcher-a', 'pitcher-b'], eligibleIds };
  const gameState: PoisonAssignState & { $spotlight: SpotlightRequest } = {
    sub: 'ASSIGN',
    loop,
    loops,
    skipUsed: carried.skipUsed,
    resolutions: carried.resolutions,
    participation: carried.participation,
    pitchersUsed: carried.pitchersUsed,
    $spotlight,
  };
  return { gameState, effects: [] };
}

function dealAssigned(
  ctx: GameCtx,
  pending: PoisonAssignState,
  pitcherA: PlayerId | null,
  pitcherB: PlayerId | null,
): GameStep {
  const activeIds = new Set(ctx.players.map((p) => p.id));
  const finalIds = [...new Set([pitcherA, pitcherB].filter((id): id is PlayerId => id !== null && activeIds.has(id)))];
  const settled: PoisonAssignState = {
    ...pending,
    pitchersUsed: [...pending.pitchersUsed, ...finalIds.filter((id) => !pending.pitchersUsed.includes(id))],
  };
  if (pitcherA === null || pitcherB === null || pitcherA === pitcherB || !activeIds.has(pitcherA) || !activeIds.has(pitcherB)) {
    return skipAssignment(ctx, settled);
  }
  const { primary, backup } = pickCardPair(ctx);
  const $deal: DealRequest = { primary, backup, subjectId: null };
  const gameState: PoisonState & { $deal: DealRequest } = {
    sub: 'DEAL',
    loop: pending.loop,
    loops: pending.loops,
    pitcherA,
    pitcherB,
    card: null, // secret until the ceremony completes (CORE_DEALT)
    votes: {},
    ratings: {},
    pitchDeadline: 0,
    voteDeadline: 0,
    skipUsed: pending.skipUsed,
    resolutions: pending.resolutions,
    participation: pending.participation,
    pitchersUsed: settled.pitchersUsed,
    $deal,
  };
  return { gameState, effects: [] };
}

function skipAssignment(ctx: GameCtx, pending: PoisonAssignState): GameStep {
  if (pending.loop + 1 < pending.loops) return assignDuel(ctx, pending.loop + 1, pending.loops, pending);
  return { gameState: pending, effects: [], done: true };
}

/** [SKIP-'EM] (once/circle): re-deal a fresh dilemma to the SAME pitchers. Sides hold. */
function redeal(ctx: GameCtx, st: PoisonState, pre: Effect[]): GameStep {
  const { primary, backup } = pickCardPair(ctx);
  const $deal: DealRequest = { primary, backup, subjectId: null };
  const gameState: PoisonState & { $deal: DealRequest } = {
    ...st,
    sub: 'DEAL',
    card: null,
    votes: {},
    ratings: {},
    pitchDeadline: 0,
    skipUsed: true, // one skip per circle, no matter which duel spent it
    $deal,
  };
  return { gameState, effects: pre };
}

function readDealtCard(ctx: GameCtx): PoisonCard | null {
  const d = ctx.state.deal;
  if (!d || d.done !== true) return null; // no completed deal — stale CORE_DEALT
  return d.card.deck === 'poison' ? (d.card as PoisonCard) : null;
}

/** Ceremony done: options + pitcher names are public. Open the 60s verbal PITCH window. */
function openPitch(ctx: GameCtx, st: PoisonState): GameStep {
  const card = readDealtCard(ctx);
  if (card === null) return noop(ctx);
  const deadline = ctx.now + PITCH_MS;
  const gameState: PoisonState & { $phase: { k: 'INPUT'; sub: string; deadline: number } } = {
    ...st,
    sub: 'PITCH',
    card,
    pitchDeadline: deadline,
    $phase: { k: 'INPUT', sub: 'PITCH', deadline },
  };
  return { gameState, effects: [{ k: 'SCHEDULE', timerId: pitchTimer(ctx.circleIdx, st.loop), atMs: deadline }] };
}

/** Bell rings (timer or [REST]): 12s skippable vote on which PITCH won. */
function openVote(ctx: GameCtx, st: PoisonState, pre: Effect[] = []): GameStep {
  const deadline = ctx.now + VOTE_MS;
  const gameState: PoisonState & { $phase: { k: 'INPUT'; sub: string; deadline: number } } = {
    ...st,
    sub: 'VOTE',
    votes: {},
    ratings: {},
    voteDeadline: deadline,
    $phase: { k: 'INPUT', sub: 'VOTE', deadline },
  };
  return {
    gameState,
    effects: [...pre, { k: 'SCHEDULE', timerId: voteTimer(ctx.circleIdx, st.loop), atMs: deadline }],
  };
}

// ===== vote math =====
// Plurality with the imp clause (4.8): imps vote at 0.5 weight, but a tie is never
// DECIDED by imps — a full-vote tie stands (split pot) no matter what imps did, and
// an imp-manufactured weighted tie collapses back to the full-vote leader. Imp weight
// still counts whenever no tie is in play; imps alone may crown a duel if every player
// abstained (roast precedent).
function decideWinner(weighted: { A: number; B: number }, full: { A: number; B: number }): DuelWinner {
  if (full.A === full.B && full.A > 0) return 'tie';
  if (full.A !== full.B) {
    const lead: Side = full.A > full.B ? 'A' : 'B';
    if (weighted.A === weighted.B) return lead;
    return weighted.A > weighted.B ? 'A' : 'B';
  }
  if (weighted.A === weighted.B) return 'tie'; // all-imp ballots, or the empty box
  return weighted.A > weighted.B ? 'A' : 'B';
}

function tallyDuel(ctx: GameCtx, st: PoisonState, voters: Player[]): { winner: DuelWinner; spread: { A: number; B: number } } {
  if (ratingMode(ctx)) {
    const sum = { A: 0, B: 0 };
    for (const j of voters) {
      const r = st.ratings[j.id];
      if (r === undefined) continue;
      sum.A += r.A;
      sum.B += r.B;
    }
    return { winner: sum.A === sum.B ? 'tie' : sum.A > sum.B ? 'A' : 'B', spread: sum };
  }
  const weighted = { A: 0, B: 0 };
  const full = { A: 0, B: 0 };
  for (const j of voters) {
    const v = st.votes[j.id];
    if (v === undefined) continue;
    weighted[v] += weightOf(j);
    if (j.role !== 'imp') full[v] += 1;
  }
  return { winner: decideWinner(weighted, full), spread: weighted };
}

// Scoring (pre-multiplier — core applies finale x3 / Bargain x2):
//   winning pitch's pitcher +3; a tie splits the pot +2 each; every voter who cast a
//   ballot earns participation +1 once per circle. Pitchers earn by WINNING the room,
//   not by showing up — a loser walks with nothing. A voided duel (host VOID / pitcher
//   seat-lapse) pays no one and renders as a 0-0 no-contest. Imp ballots shape the
//   weighted math at 0.5 but still bank full participation (roast/confession precedent).
function resolve(ctx: GameCtx, st: PoisonState, pre: Effect[], voided = false): GameStep {
  if (st.card === null) return noop(ctx); // unreachable: VOTE/PITCH always carry a card
  const voters = votersOf(ctx, st);
  const { winner, spread } = voided ? { winner: 'tie' as DuelWinner, spread: { A: 0, B: 0 } } : tallyDuel(ctx, st, voters);

  const scores: Record<string, number> = {};
  const add = (id: PlayerId, pts: number): void => {
    if (pts > 0) scores[id] = (scores[id] ?? 0) + pts;
  };
  const participation = [...st.participation];
  if (!voided) {
    if (winner === 'A') add(st.pitcherA, points('poison.pitchWin'));
    else if (winner === 'B') add(st.pitcherB, points('poison.pitchWin'));
    else {
      add(st.pitcherA, SPLIT_PTS);
      add(st.pitcherB, SPLIT_PTS);
    }
    const cast = (id: PlayerId): boolean => (ratingMode(ctx) ? st.ratings[id] !== undefined : st.votes[id] !== undefined);
    for (const j of voters) {
      if (!cast(j.id)) continue; // abstainers earn nothing — not even participation
      if (!participation.includes(j.id)) {
        participation.push(j.id);
        add(j.id, points('participation'));
      }
    }
  }

  const resolution: PoisonResolution = { cardId: st.card.id, winner, spread, voided };
  // $phase REVEAL: the core stamps holdSince and owns the hold — synced flip beat,
  // DESCEND (host anytime / anyone past 45s), fire-decay past the 20s minimum (4.2).
  const gameState: PoisonState & { $phase: { k: 'REVEAL' } } = {
    ...st,
    sub: 'REVEAL',
    resolutions: [...st.resolutions, resolution],
    participation,
    $phase: { k: 'REVEAL' },
  };
  return {
    gameState,
    effects: [...pre, { k: 'AUDIO', sting: voided ? 'burn' : 'boom' }, { k: 'SNAPSHOT' }],
    scores,
  };
}

// ===== the module =====
export const poisonModule = {
  deck: 'poison',
  minN: 3,

  start(ctx: GameCtx): GameStep {
    const loops = ctx.circle.loops >= 1 ? ctx.circle.loops : DUELS_PER_CIRCLE;
    return assignDuel(ctx, 0, loops, { resolutions: [], participation: [], pitchersUsed: [], skipUsed: false });
  },

  input(ctx: GameCtx, playerId: string, payload: unknown): GameStep {
    const st = readState(ctx);
    if (!st || st.sub !== 'VOTE') return noop(ctx); // PITCH is verbal; stale ballots die silently (3.5)
    const voters = votersOf(ctx, st);
    if (!voters.some((p) => p.id === playerId)) return noop(ctx); // pitchers never vote their own duel; ghosts neither

    if (ratingMode(ctx)) {
      const rate = parseRate(payload);
      if (rate === null) return noop(ctx);
      const ratings = { ...st.ratings, [playerId]: rate }; // re-rate allowed until deadline: last ballot wins
      const next: PoisonState = { ...st, ratings };
      if (Object.keys(ratings).length >= voters.length) {
        return resolve(ctx, next, [{ k: 'CANCEL', timerId: voteTimer(ctx.circleIdx, st.loop) }]);
      }
      return { gameState: next, effects: [{ k: 'BROADCAST' }] }; // count ticks up; view() hides the numbers
    }

    const v = parseVote(payload);
    if (v === null) return noop(ctx);
    const votes = { ...st.votes, [playerId]: v }; // re-vote allowed until deadline: last ballot wins
    const next: PoisonState = { ...st, votes };
    if (Object.keys(votes).length >= voters.length) {
      // Room's all in — don't make the pitchers stare at a countdown.
      return resolve(ctx, next, [{ k: 'CANCEL', timerId: voteTimer(ctx.circleIdx, st.loop) }]);
    }
    return { gameState: next, effects: [{ k: 'BROADCAST' }] }; // count ticks up; view() hides who
  },

  timer(ctx: GameCtx, timerId: string): GameStep {
    const st = readState(ctx);
    if (!st) return noop(ctx);
    if (st.sub === 'ASSIGN' && timerId === CORE_SPOTLIGHT_DONE) {
      const pitcherA = ctx.spotlight?.assignments.find((a) => a.role === 'pitcher-a')?.playerId ?? null;
      const pitcherB = ctx.spotlight?.assignments.find((a) => a.role === 'pitcher-b')?.playerId ?? null;
      return dealAssigned(ctx, st, pitcherA, pitcherB);
    }
    if (st.sub === 'DEAL' && timerId === CORE_DEALT) {
      return openPitch(ctx, st); // ceremony done: fight card public, PITCH opens
    }
    if (st.sub === 'PITCH' && timerId === pitchTimer(ctx.circleIdx, st.loop)) {
      return openVote(ctx, st); // bell: the mics go quiet, the room votes
    }
    if (st.sub === 'VOTE' && timerId === voteTimer(ctx.circleIdx, st.loop)) {
      return resolve(ctx, st, []); // deadline: non-voters auto-abstain — the game never waits
    }
    if (st.sub === 'REVEAL' && timerId === CORE_REVEAL_DONE) {
      if (st.loop + 1 < st.loops) return assignDuel(ctx, st.loop + 1, st.loops, st);
      return { gameState: st, effects: [], done: true }; // core -> LADDER
    }
    return noop(ctx); // stale timer for a dead loop
  },

  control(ctx: GameCtx, playerId: string, kind: 'REST' | 'SKIPEM' | 'FIFTH' | 'VOID'): GameStep {
    const st = readState(ctx);
    if (!st || st.sub === 'ASSIGN') return noop(ctx);
    const caller = ctx.players.find((p) => p.id === playerId);
    const isPitcher = playerId === st.pitcherA || playerId === st.pitcherB;
    const isHost = caller?.role === 'host';

    if (kind === 'SKIPEM') {
      // A pitcher (or the host) hates the dilemma: swap it, keep the fighters. Once/circle.
      if (st.sub !== 'PITCH' || st.skipUsed || !(isPitcher || isHost)) return noop(ctx);
      return redeal(ctx, st, [{ k: 'CANCEL', timerId: pitchTimer(ctx.circleIdx, st.loop) }]);
    }

    if (kind === 'REST') {
      // [REST]: both pitches are in, cut the clock and go straight to the vote.
      if (st.sub !== 'PITCH' || !(isPitcher || isHost)) return noop(ctx);
      return openVote(ctx, st, [{ k: 'CANCEL', timerId: pitchTimer(ctx.circleIdx, st.loop) }]);
    }

    if (kind === 'VOID') {
      if (isHost) {
        // Host kill-switch (4.7): the duel dies scoreless at any live sub-phase.
        if (st.sub === 'PITCH') return resolve(ctx, st, [{ k: 'CANCEL', timerId: pitchTimer(ctx.circleIdx, st.loop) }], true);
        if (st.sub === 'VOTE') return resolve(ctx, st, [{ k: 'CANCEL', timerId: voteTimer(ctx.circleIdx, st.loop) }], true);
        return noop(ctx); // DEAL rides out its ceremony; REVEAL is already resolved
      }
      // Seat-lapse convention (engine 4.7): a non-host VOID = that seat lapsed. A lapsed
      // PITCHER kills the duel (no advocate, no contest); a lapsed voter just doesn't vote.
      if (isPitcher && st.sub === 'PITCH') {
        return resolve(ctx, st, [{ k: 'CANCEL', timerId: pitchTimer(ctx.circleIdx, st.loop) }], true);
      }
      return noop(ctx); // DEAL rides the ceremony out; VOTE/REVEAL: the ballots are in, a duel can crown a ghost
    }

    return noop(ctx); // FIFTH: poison has no blocking confessional screen
  },

  // The ONLY serialization surface. Redaction law (3.4 + 5.6):
  // - during the DEAL ceremony NOTHING serializes — the side assignment (who drew A vs
  //   B) never reaches the wire until PITCH, so sides cannot leak before phase 2;
  // - during VOTE a viewer sees counts + their OWN ballot (and, if they're the N=3
  //   judge, their own rating echoed back) — nobody else's;
  // - the reveal spread is aggregate counts/ratings only — no voter-choice pair ever
  //   leaves the server;
  // - per-duel score deltas are never in the view (a +3 would out the winning ballot).
  view(ctx: GameCtx, viewerId: string): PoisonView | null {
    const st = readState(ctx);
    if (!st || st.sub === 'ASSIGN' || st.sub === 'DEAL' || st.card === null) return null;
    const base = { deck: 'poison' as const, loop: st.loop, loops: st.loops };
    const youArePitcher: Side | null = viewerId === st.pitcherA ? 'A' : viewerId === st.pitcherB ? 'B' : null;

    if (st.sub === 'PITCH') {
      return {
        ...base,
        sub: 'PITCH',
        optionA: st.card.optionA,
        optionB: st.card.optionB,
        pitcherA: st.pitcherA,
        pitcherB: st.pitcherB,
        youArePitcher,
        deadline: st.pitchDeadline,
      };
    }

    if (st.sub === 'VOTE') {
      const rm = ratingMode(ctx);
      const own = st.ratings[viewerId];
      const youVoted: Side | null = rm
        ? own === undefined
          ? null
          : own.A > own.B
            ? 'A'
            : own.B > own.A
              ? 'B'
              : null
        : st.votes[viewerId] ?? null;
      return {
        ...base,
        sub: 'VOTE',
        optionA: st.card.optionA,
        optionB: st.card.optionB,
        pitcherA: st.pitcherA,
        pitcherB: st.pitcherB,
        ratingMode: rm,
        eligible: votersOf(ctx, st).length,
        votedCount: rm ? Object.keys(st.ratings).length : Object.keys(st.votes).length,
        youVoted,
        ...(rm && own !== undefined ? { youRated: own } : {}),
        deadline: st.voteDeadline,
      };
    }

    const res = st.resolutions[st.resolutions.length - 1];
    if (!res) return null; // unreachable by construction; never leak on a bug
    return {
      ...base,
      sub: 'REVEAL',
      winner: res.winner,
      pitcherA: st.pitcherA,
      pitcherB: st.pitcherB,
      optionA: st.card.optionA,
      optionB: st.card.optionB,
      spread: res.spread,
    };
  },
} satisfies GameModule;
