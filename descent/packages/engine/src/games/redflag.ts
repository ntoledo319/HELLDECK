// RED FLAG RALLY — spec 5.7 + HDRealRules2 Part III §7. Task D-133.
// Two defenses per circle. The deck deals one assigned defender a PERK worth wanting
// and a RED FLAG worth running from; they get 45s to sell going on the date anyway.
// Then the room (not the defender) secretly votes SMASH or PASS, the verdict flips
// on every phone at once, and the reveal holds while the table roasts the choice.
// SMASH means the defense survived. The app counts ballots — humans supply the "ew."
//
// Module law (games/module.ts): state lives opaquely in RoomState.gameState;
// view() is the ONLY serialization surface, so every redaction rule lives there.
// Pure module: time arrives as ctx.now, randomness as ctx.rand. Never a clock.
//
// Core integration (engine.ts "module step protocol"), per loop — built like the
// other spotlight games (overunder/confession) but with NO blocking input:
//   ASSIGN (spotlight-weighted defender, this circle's earlier defenders excluded)
//   -> $deal {subjectId: defender} (4.5: 10s ceremony, PRIVATE pre-view + Brimstone
//      burn window on the defender's fight card, night-dedup writeback) -> CORE_DEALT
//      (perk + flag go public together)
//   -> $phase INPUT "DEFENSE" (45s verbal; [REST] ends it early; [SKIP-'EM] burns the
//      card — not the human — for a fresh one, once per circle)
//   -> $phase INPUT "VOTE" (12s skippable; SMASH/PASS; the defender never votes)
//   -> $phase REVEAL (core-held hold: flip beat, DESCEND, fire-decay) — the verdict
//      stamp + weighted spread
//   -> CORE_REVEAL_DONE -> next defender or done.
import type { DealRequest, Effect, Player, PlayerId, RedFlagCard, SpotlightRequest } from '../types.js';
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

// ===== tuning (spec 5.7 / 4.6 / 4.8) =====
export const DEFENSES_PER_CIRCLE = 2;
export const DEFENSE_MS = 45_000; // verbal sell; [REST] ends it early
export const VOTE_MS = 12_000; // skippable: deadline -> non-voters auto-abstain
export const IMP_WEIGHT = 0.5; // 4.8: imps vote at half weight; ties are never decided by imps

export type RedflagVote = 'smash' | 'pass';
export type Verdict = 'SMASH' | 'PASS';
type Tally = { smash: number; pass: number };

// ===== module state (opaque to core; view() redacts) =====
export interface RedflagResolution {
  cardId: string;
  perk: string;
  flag: string;
  defenderId: PlayerId;
  verdict: Verdict;
  spread: Tally; // weighted counts (imps 0.5) — aggregate bars, never a ballot
  voided: boolean;
}

export interface RedflagState {
  sub: 'DEAL' | 'DEFENSE' | 'VOTE' | 'REVEAL'; // DEAL = core ceremony running, card still secret
  loop: number; // 0-based defender index
  loops: number; // 2 per spec
  defenderId: PlayerId;
  card: RedFlagCard | null; // public from CORE_DEALT (the ceremony owns the announce)
  votes: Record<PlayerId, RedflagVote>; // SECRET until reveal — view() shows counts + your own only
  defenseDeadline: number; // view mirrors; authoritative deadlines are the SCHEDULE effects
  voteDeadline: number;
  skipUsed: boolean; // SKIP-'EM is once PER CIRCLE (rule §7.6) — carried across both loops
  resolutions: RedflagResolution[];
  participation: PlayerId[]; // once-per-circle +1 already paid (a ballot cast)
  defendersUsed: PlayerId[]; // this circle's defenders incl. current — loop 2 excludes loop 1
}

interface RedflagAssignState {
  sub: 'ASSIGN';
  loop: number;
  loops: number;
  skipUsed: boolean;
  resolutions: RedflagResolution[];
  participation: PlayerId[];
  defendersUsed: PlayerId[];
}

type RedflagInternalState = RedflagAssignState | RedflagState;

// ===== views =====
interface RedflagViewBase {
  deck: 'redflag';
  loop: number;
  loops: number;
}
export type RedflagDefenseView = RedflagViewBase & {
  sub: 'DEFENSE';
  perk: string;
  flag: string;
  defenderId: PlayerId;
  youAreDefender: boolean; // "SELL IT" + [I REST MY CASE] vs "watch them squirm" + fire
  deadline: number;
};
export type RedflagVoteView = RedflagViewBase & {
  sub: 'VOTE';
  perk: string;
  flag: string;
  defenderId: PlayerId;
  eligible: number;
  votedCount: number; // counts only — never who
  youVoted: RedflagVote | null; // your own ballot, nobody else's
  deadline: number;
  youAreDefender: boolean; // "they're deciding if you're dateable"
};
export type RedflagRevealView = RedflagViewBase & {
  sub: 'REVEAL';
  verdict: Verdict;
  defenderId: PlayerId;
  perk: string;
  flag: string;
  spread: Tally; // weighted counts — no voter-vote pair ever leaves the server
  youVoted: RedflagVote | null;
  voided: boolean; // host kill-switch / seat-lapse: the client shows the void banner, ignores the stamp
};
export type RedflagView = RedflagDefenseView | RedflagVoteView | RedflagRevealView;

// ===== deck registry =====
// Card plumbing arrives with content integration (D-127/8.3); until then the core
// (or a test) injects a deck here. Stubs keep dev nights honest: the perk must
// genuinely tempt and the flag must genuinely disqualify — the GAP is the whole
// joke ("one card = one joke", 5.7), and every one is a hard swipe either way.
export const DEFAULT_REDFLAG_DECK: RedFlagCard[] = [
  stub('001', 'Pays for everything, everywhere, forever — no receipts, no questions', "Calls their mother 'my first wife,' and means it", 3, 4, 'menace', 'first-wife'),
  stub('002', "The most objectively gorgeous human you'd ever be seen next to", "Keeps a ranked spreadsheet of everyone they've kissed — you're row 47", 3, 4, 'deadpan', 'kiss-spreadsheet'),
  stub('003', 'Lake house, a boat, and zero opinions about how you live your life', "Ends every fight by whispering 'noted' and typing into their phone", 2, 3, 'menace', 'noted-typing'),
  stub('004', 'Would take a bullet for you without blinking', 'Has taken one before and re-enacts it, fully, at every dinner party', 2, 3, 'absurdist', 'bullet-reenact'),
  stub('005', 'Fluent in six languages, cooks like a haunted Michelin kitchen', "Only says 'I love you' in the one language you will never learn", 3, 4, 'euphemism', 'love-untranslated'),
  stub('006', 'Texts back instantly, communicates like a licensed therapist', 'Is texting four other people back exactly that fast, right now', 2, 3, 'observational', 'fast-texter'),
  stub('007', 'Genuinely funny, genuinely kind, genuinely obsessed with you', "Still lives with an ex they describe as 'basically a houseplant'", 3, 3, 'petty-domestic', 'ex-houseplant'),
  stub('008', "Would fly across the country to bring you soup when you're sick", "Reads your group chat aloud to the soup first, for 'context'", 3, 4, 'absurdist', 'soup-context'),
  stub('009', 'Gym, career, skincare — the entire self-improvement arc, completed', "Has a countdown to your 'expiration date' set as their lock screen", 4, 4, 'menace', 'expiration-countdown'),
  stub('010', 'Rich enough to make your student loans a rounding error', "The money is from reselling other people's streaming passwords. Federally", 3, 4, 'deadpan', 'streaming-cartel'),
];
function stub(
  n: string,
  perk: string,
  flag: string,
  exposure: RedFlagCard['exposure'],
  chaos: RedFlagCard['chaos'],
  register: RedFlagCard['register'],
  skeleton: string,
): RedFlagCard {
  return { id: `redflag_stub_${n}`, deck: 'redflag', text: `${perk} — ${flag}`, exposure, chaos, register, skeleton, perk, flag };
}

let deckCards: RedFlagCard[] = DEFAULT_REDFLAG_DECK;
export function setRedflagDeck(cards: readonly RedFlagCard[]): void {
  if (cards.length === 0) throw new Error('redflag deck cannot be empty');
  deckCards = [...cards];
}

// ===== helpers =====
export const defenseTimerId = (circleIdx: number, loop: number): string => `redflag:defense:${circleIdx}:${loop}`;
export const voteTimerId = (circleIdx: number, loop: number): string => `redflag:vote:${circleIdx}:${loop}`;

const weightOf = (p: Player): number => (p.role === 'imp' ? IMP_WEIGHT : 1);
const hostIdOf = (ctx: GameCtx): PlayerId | null => ctx.players.find((p) => p.role === 'host')?.id ?? null;
/** Everyone but the defender votes — imps too, at half weight (4.8). */
const votersOf = (ctx: GameCtx, st: RedflagState): Player[] =>
  [...ctx.players, ...ctx.imps].filter((p) => p.id !== st.defenderId);
const isActive = (ctx: GameCtx, id: PlayerId): boolean =>
  ctx.players.some((p) => p.id === id) || ctx.imps.some((p) => p.id === id);

function readState(ctx: GameCtx): RedflagInternalState | null {
  const gs = ctx.state.gameState as RedflagInternalState | null | undefined;
  return gs && (gs.sub === 'ASSIGN' || gs.sub === 'DEAL' || gs.sub === 'DEFENSE' || gs.sub === 'VOTE' || gs.sub === 'REVEAL') ? gs : null;
}

function noop(ctx: GameCtx): GameStep {
  return { gameState: ctx.state.gameState, effects: [] };
}

function parseVote(payload: unknown): RedflagVote | null {
  if (typeof payload !== 'object' || payload === null) return null;
  const v = (payload as Record<string, unknown>)['vote'];
  return v === 'smash' || v === 'pass' ? v : null;
}

/** Narrow a dealt Card to a RedFlagCard (deck tag + both required extras present). */
function isRedFlagCard(c: unknown): c is RedFlagCard {
  return (
    typeof c === 'object' &&
    c !== null &&
    (c as { deck?: unknown }).deck === 'redflag' &&
    'perk' in c &&
    'flag' in c
  );
}

// ===== defender + card selection =====
// Consent (4.4): Red Flag is aim-OUTWARD — the fight card is a fictional date, not a
// fact about the defender, so the generic room ceiling caps it (no subjectCeiling:
// nothing here exposes the person selling it). No Fresh Meat gate: a hypothetical
// needs zero crew knowledge (§10 lists the gated games; not us). Rung preference
// (4.3) and night-dedup ride on top. The defender is still NAMED in the $deal — that
// only buys them the private pre-view + Brimstone burn (the "burnable" in 5.7's ASSIGN).
function legalPool(ctx: GameCtx): RedFlagCard[] {
  const ceilings = ctx.players.map((p) => p.heatCeiling);
  let legal = deckCards.filter((c) => cardLegal(c.exposure, { ceilings }));
  if (legal.length === 0) {
    // Content bug (nothing legal): degrade to the mildest cards rather than deadlock
    // the night. The arc builder gates depth on legal content; this is the backstop.
    const minE = Math.min(...deckCards.map((c) => c.exposure));
    legal = deckCards.filter((c) => c.exposure === minE);
  }
  const underRung = legal.filter((c) => c.exposure <= ctx.circle.rung);
  return underRung.length > 0 ? underRung : legal;
}

function eligibleDefenderIds(ctx: GameCtx, exclude: readonly PlayerId[]): PlayerId[] {
  let candidates = ctx.players.filter((p) => !exclude.includes(p.id));
  if (candidates.length === 0) candidates = [...ctx.players];
  return candidates.map((p) => p.id);
}

/** Primary + reserved backup (4.5), night-deduped via usedCardIds (core writes back). */
function pickPair(ctx: GameCtx, legal: RedFlagCard[]): { primary: RedFlagCard; backup: RedFlagCard } {
  const fresh = legal.filter((c) => !ctx.state.usedCardIds.includes(c.id));
  const pool = fresh.length > 0 ? fresh : legal; // deck exhausted: a repeat beats a dead night
  const primary = pick(ctx.rand, pool);
  const rest = pool.filter((c) => c.id !== primary.id);
  const fallback = legal.filter((c) => c.id !== primary.id);
  const backup = rest.length > 0 ? pick(ctx.rand, rest) : fallback.length > 0 ? pick(ctx.rand, fallback) : primary;
  return { primary, backup };
}

// ===== phase openers =====
interface Carried {
  resolutions: RedflagResolution[];
  participation: PlayerId[];
  defendersUsed: PlayerId[];
  skipUsed: boolean;
}

/**
 * Hand a fight card to the core's 4.5 ceremony with the defender NAMED (private pre-
 * view + burn window). No $phase yet — the ceremony broadcasts the card at completion
 * (CORE_DEALT), where perk + flag go public together. Used both to open a loop and to
 * re-arm the SAME defender after a SKIP-'EM.
 */
function dealTo(ctx: GameCtx, defenderId: PlayerId, loop: number, loops: number, carried: Carried, pre: Effect[]): GameStep {
  const { primary, backup } = pickPair(ctx, legalPool(ctx));
  const $deal: DealRequest = { primary, backup, subjectId: defenderId };
  const gameState: RedflagState & { $deal: DealRequest } = {
    sub: 'DEAL',
    loop,
    loops,
    defenderId,
    card: null, // secret until the ceremony completes (CORE_DEALT)
    votes: {},
    defenseDeadline: 0,
    voteDeadline: 0,
    skipUsed: carried.skipUsed,
    resolutions: carried.resolutions,
    participation: carried.participation,
    defendersUsed: carried.defendersUsed,
    $deal,
  };
  return { gameState, effects: pre };
}

/** Ask core to assign this loop's defender; no identity serializes from this step. */
function assign(ctx: GameCtx, loop: number, loops: number, carried: Carried): GameStep {
  const eligibleIds = eligibleDefenderIds(ctx, carried.defendersUsed);
  if (eligibleIds.length === 0) return { gameState: ctx.state.gameState, effects: [], done: true };
  const $spotlight: SpotlightRequest = { roles: ['defender'], eligibleIds };
  const gameState: RedflagAssignState & { $spotlight: SpotlightRequest } = {
    sub: 'ASSIGN',
    loop,
    loops,
    skipUsed: carried.skipUsed,
    resolutions: carried.resolutions,
    participation: carried.participation,
    defendersUsed: carried.defendersUsed,
    $spotlight,
  };
  return { gameState, effects: [] };
}

function dealAssigned(ctx: GameCtx, pending: RedflagAssignState, defenderId: PlayerId): GameStep {
  if (!ctx.players.some((p) => p.id === defenderId)) return skipAssignment(ctx, pending);
  return dealTo(
    ctx,
    defenderId,
    pending.loop,
    pending.loops,
    { ...pending, defendersUsed: [...pending.defendersUsed, defenderId] },
    [],
  );
}

function skipAssignment(ctx: GameCtx, pending: RedflagAssignState): GameStep {
  if (pending.loop + 1 < pending.loops) return assign(ctx, pending.loop + 1, pending.loops, pending);
  return { gameState: pending, effects: [], done: true };
}

/** Ceremony done: perk + flag are public. Open the 45s DEFENSE. */
function openDefense(ctx: GameCtx, st: RedflagState): GameStep {
  const dealt = ctx.state.deal?.done === true ? ctx.state.deal.card : null;
  if (!isRedFlagCard(dealt)) return noop(ctx); // stale CORE_DEALT
  const deadline = ctx.now + DEFENSE_MS;
  const gameState: RedflagState & { $phase: { k: 'INPUT'; sub: string; deadline: number } } = {
    ...st,
    sub: 'DEFENSE',
    card: dealt, // honors a burn swap: this is whatever survived the ceremony
    defenseDeadline: deadline,
    $phase: { k: 'INPUT', sub: 'DEFENSE', deadline },
  };
  return { gameState, effects: [{ k: 'SCHEDULE', timerId: defenseTimerId(ctx.circleIdx, st.loop), atMs: deadline }] };
}

/** Case rested (timer or [I REST MY CASE]): open the 12s skippable SMASH/PASS. */
function openVote(ctx: GameCtx, st: RedflagState, pre: Effect[]): GameStep {
  const deadline = ctx.now + VOTE_MS;
  const gameState: RedflagState & { $phase: { k: 'INPUT'; sub: string; deadline: number } } = {
    ...st,
    sub: 'VOTE',
    votes: {},
    voteDeadline: deadline,
    $phase: { k: 'INPUT', sub: 'VOTE', deadline },
  };
  return { gameState, effects: [...pre, { k: 'SCHEDULE', timerId: voteTimerId(ctx.circleIdx, st.loop), atMs: deadline }] };
}

// ===== resolution =====
/**
 * Verdict with the imp clause (4.8), SMASH-wins-ties (performers win ties, 5.7):
 * a human-only tie WITH ballots stamps SMASH no matter what imps did; an imp-
 * manufactured weighted tie collapses back to the human leader; imp weight still
 * counts whenever no tie is in play (and imps alone may carry a verdict if every
 * player abstained). Nobody at all -> SMASH (an unheard defense survives by default).
 */
function decideVerdict(weighted: Tally, full: Tally): Verdict {
  if (full.smash === full.pass && full.smash > 0) return 'SMASH'; // human tie WITH ballots -> the defender walks; imps can't break it
  if (full.smash !== full.pass) {
    const humanLeader: Verdict = full.smash > full.pass ? 'SMASH' : 'PASS';
    if (weighted.smash === weighted.pass) return humanLeader; // imp-manufactured weighted tie -> the human leader
    return weighted.smash > weighted.pass ? 'SMASH' : 'PASS'; // no tie in play: imp weight counts
  }
  return weighted.pass > weighted.smash ? 'PASS' : 'SMASH'; // no human ballots: imps alone, else nobody -> SMASH wins ties
}

function tally(votes: Record<PlayerId, RedflagVote>, voters: Player[]): { weighted: Tally; full: Tally } {
  const weighted: Tally = { smash: 0, pass: 0 };
  const full: Tally = { smash: 0, pass: 0 };
  for (const voter of voters) {
    const v = votes[voter.id];
    if (v === undefined) continue;
    weighted[v] += weightOf(voter);
    if (voter.role !== 'imp') full[v] += 1;
  }
  return { weighted, full };
}

// Scoring (pre-multiplier — core applies finale x3 / Bargain x2):
//   SMASH verdict -> defender +3 (the date survived; they sold the unsellable);
//   each voter +1 participation, once per circle, for any ballot cast. Voters earn
//   nothing for guessing "right" — Red Flag has no correct answer, only a verdict
//   (4.6: no majority-alignment metagame outside Roast). The defender casts no
//   ballot and earns only by surviving. Imp ballots score FULL points; their 0.5
//   weight only shapes the verdict math (roast/confession precedent).
function resolve(ctx: GameCtx, st: RedflagState, pre: Effect[]): GameStep {
  if (st.card === null) return noop(ctx); // unreachable: VOTE always follows a dealt card
  const voters = votersOf(ctx, st);
  const { weighted, full } = tally(st.votes, voters);
  const verdict = decideVerdict(weighted, full);

  const scores: Record<string, number> = {};
  const add = (id: PlayerId, pts: number): void => {
    if (pts > 0) scores[id] = (scores[id] ?? 0) + pts;
  };
  const participation = [...st.participation];
  if (verdict === 'SMASH') add(st.defenderId, points('redflag.smash'));
  for (const voter of voters) {
    if (st.votes[voter.id] === undefined) continue; // abstainers earn nothing — not even participation
    if (!participation.includes(voter.id)) {
      participation.push(voter.id);
      add(voter.id, points('participation'));
    }
  }

  const resolution: RedflagResolution = {
    cardId: st.card.id,
    perk: st.card.perk,
    flag: st.card.flag,
    defenderId: st.defenderId,
    verdict,
    spread: weighted,
    voided: false,
  };
  // $phase REVEAL: the core stamps holdSince and owns the hold — synced flip beat,
  // DESCEND (host anytime / anyone past 45s), fire-decay past the 20s minimum (4.2).
  const gameState: RedflagState & { $phase: { k: 'REVEAL' } } = {
    ...st,
    sub: 'REVEAL',
    resolutions: [...st.resolutions, resolution],
    participation,
    $phase: { k: 'REVEAL' },
  };
  return {
    gameState,
    effects: [...pre, { k: 'AUDIO', sting: verdict === 'SMASH' ? 'boom' : 'burn' }, { k: 'SNAPSHOT' }],
    scores,
  };
}

/** Loop dies scoreless (host VOID / seat-lapse): 0 to all (4.6), the stamp is a void banner. */
function resolveVoided(ctx: GameCtx, st: RedflagState, pre: Effect[], sting: 'void' | 'fled'): GameStep {
  const cancels: Effect[] =
    st.sub === 'DEFENSE'
      ? [{ k: 'CANCEL', timerId: defenseTimerId(ctx.circleIdx, st.loop) }]
      : st.sub === 'VOTE'
        ? [{ k: 'CANCEL', timerId: voteTimerId(ctx.circleIdx, st.loop) }]
        : [];
  const resolution: RedflagResolution = {
    cardId: st.card?.id ?? '',
    perk: st.card?.perk ?? '',
    flag: st.card?.flag ?? '',
    defenderId: st.defenderId,
    verdict: 'PASS', // a nullified defense didn't survive; the client keys off `voided`, not the stamp
    spread: { smash: 0, pass: 0 },
    voided: true,
  };
  const gameState: RedflagState & { $phase: { k: 'REVEAL' } } = {
    ...st,
    sub: 'REVEAL',
    votes: {}, // scrub the ballots the void ate
    resolutions: [...st.resolutions, resolution],
    $phase: { k: 'REVEAL' },
  };
  return { gameState, effects: [...pre, ...cancels, { k: 'AUDIO', sting }, { k: 'SNAPSHOT' }], scores: {} };
}

// ===== the module =====
export const redflagModule = {
  deck: 'redflag',
  minN: 3,

  start(ctx: GameCtx): GameStep {
    const loops = ctx.circle.loops >= 1 ? ctx.circle.loops : DEFENSES_PER_CIRCLE;
    return assign(ctx, 0, loops, { resolutions: [], participation: [], defendersUsed: [], skipUsed: false });
  },

  input(ctx: GameCtx, playerId: string, payload: unknown): GameStep {
    const st = readState(ctx);
    if (!st || st.sub !== 'VOTE') return noop(ctx); // stale ballots die silently (3.5)
    const v = parseVote(payload);
    if (v === null) return noop(ctx);
    const voters = votersOf(ctx, st);
    if (!voters.some((p) => p.id === playerId)) return noop(ctx); // the defender never votes their own fate; ghosts neither
    const votes = { ...st.votes, [playerId]: v }; // re-vote allowed until deadline: last ballot wins
    const next: RedflagState = { ...st, votes };
    if (Object.keys(votes).length >= voters.length) {
      // Room's all in — don't make the defender sweat a countdown for nothing.
      return resolve(ctx, next, [{ k: 'CANCEL', timerId: voteTimerId(ctx.circleIdx, st.loop) }]);
    }
    return { gameState: next, effects: [{ k: 'BROADCAST' }] }; // count ticks up; view() hides who
  },

  timer(ctx: GameCtx, timerId: string): GameStep {
    const st = readState(ctx);
    if (!st) return noop(ctx);
    if (st.sub === 'ASSIGN' && timerId === CORE_SPOTLIGHT_DONE) {
      const defenderId = ctx.spotlight?.assignments.find((a) => a.role === 'defender')?.playerId ?? null;
      return defenderId === null ? skipAssignment(ctx, st) : dealAssigned(ctx, st, defenderId);
    }
    if (st.sub === 'DEAL' && timerId === CORE_DEALT) {
      return openDefense(ctx, st); // ceremony done: perk + flag public, DEFENSE opens
    }
    if (st.sub === 'DEFENSE' && timerId === defenseTimerId(ctx.circleIdx, st.loop)) {
      return openVote(ctx, st, []); // 45s up whether they closed strong or ran out of alibi
    }
    if (st.sub === 'VOTE' && timerId === voteTimerId(ctx.circleIdx, st.loop)) {
      return resolve(ctx, st, []); // deadline: non-voters auto-abstain — the game never waits
    }
    if (st.sub === 'REVEAL' && timerId === CORE_REVEAL_DONE) {
      if (st.loop + 1 < st.loops) return assign(ctx, st.loop + 1, st.loops, st); // next defender
      return { gameState: st, effects: [], done: true }; // core -> LADDER
    }
    return noop(ctx); // stale timer for a dead loop
  },

  control(ctx: GameCtx, playerId: string, kind: 'REST' | 'SKIPEM' | 'FIFTH' | 'VOID'): GameStep {
    const st = readState(ctx);
    if (!st || st.sub === 'ASSIGN') return noop(ctx);
    const hostId = hostIdOf(ctx);

    if (kind === 'REST') {
      // I REST MY CASE — the defender only, mid-defense only. Ends the clock early (§7.5).
      if (st.sub !== 'DEFENSE' || playerId !== st.defenderId) return noop(ctx);
      return openVote(ctx, st, [{ k: 'CANCEL', timerId: defenseTimerId(ctx.circleIdx, st.loop) }]);
    }

    if (kind === 'SKIPEM') {
      // THAT CARD WAS TRASH — NEW WEAPON (§7.6): the room re-deals the CARD, never the
      // human. Same defender, fresh fight card, once PER CIRCLE. DEFENSE only, so it's
      // dead the moment a REST or the deadline moves the loop to VOTE.
      if (st.sub !== 'DEFENSE' || st.skipUsed || !isActive(ctx, playerId)) return noop(ctx);
      return dealTo(ctx, st.defenderId, st.loop, st.loops, { ...st, skipUsed: true }, [
        { k: 'CANCEL', timerId: defenseTimerId(ctx.circleIdx, st.loop) },
        { k: 'AUDIO', sting: 'skip' },
      ]);
    }

    if (kind === 'VOID') {
      if (hostId !== null && playerId === hostId) {
        // Host kill-switch (4.7): the loop dies scoreless at any live sub-phase.
        if (st.sub === 'DEFENSE' || st.sub === 'VOTE') return resolveVoided(ctx, st, [], 'void');
        return noop(ctx); // DEAL rides out its ceremony; REVEAL is already resolved
      }
      // Seat-lapse convention (engine 4.7): VOID carrying a NON-host id = that seat lapsed.
      if (playerId === st.defenderId) {
        if (st.sub === 'DEFENSE') return resolveVoided(ctx, st, [], 'fled'); // no defender, no defense
        return noop(ctx); // VOTE/REVEAL: the case is closed; the room can pass on a ghost
      }
      return noop(ctx); // a lapsed voter just doesn't vote
    }

    return noop(ctx); // FIFTH: Red Flag locks no secret and traps no human — nothing to plead
  },

  // The ONLY serialization surface. Redaction law (3.4 + 5.7):
  // - during the DEAL ceremony NOTHING serializes (perk + flag are still secret;
  //   the defender's pre-view travels via the core's private SEND, never through here);
  // - during VOTE a viewer sees counts + their OWN ballot, nothing else;
  // - the reveal spread is weighted counts only — no voter-vote pair ever leaves the
  //   server;
  // - per-loop score deltas are never in the view (a +3 would out the verdict early,
  //   a +1 would out a ballot).
  view(ctx: GameCtx, viewerId: string): RedflagView | null {
    const st = readState(ctx);
    if (!st || st.sub === 'ASSIGN' || st.sub === 'DEAL') return null; // ceremony owns the secret
    const base = { deck: 'redflag' as const, loop: st.loop, loops: st.loops };
    const youAreDefender = viewerId === st.defenderId;

    if (st.sub === 'DEFENSE') {
      if (st.card === null) return null; // defensive: never leak a half-built loop
      return {
        ...base,
        sub: 'DEFENSE',
        perk: st.card.perk,
        flag: st.card.flag,
        defenderId: st.defenderId,
        youAreDefender,
        deadline: st.defenseDeadline,
      };
    }
    if (st.sub === 'VOTE') {
      if (st.card === null) return null;
      return {
        ...base,
        sub: 'VOTE',
        perk: st.card.perk,
        flag: st.card.flag,
        defenderId: st.defenderId,
        eligible: votersOf(ctx, st).length,
        votedCount: Object.keys(st.votes).length,
        youVoted: st.votes[viewerId] ?? null,
        deadline: st.voteDeadline,
        youAreDefender,
      };
    }
    const res = st.resolutions[st.resolutions.length - 1];
    if (!res) return null; // unreachable by construction; never leak on a bug
    return {
      ...base,
      sub: 'REVEAL',
      verdict: res.verdict,
      defenderId: res.defenderId,
      perk: res.perk,
      flag: res.flag,
      spread: res.spread,
      youVoted: st.votes[viewerId] ?? null,
      voided: res.voided,
    };
  },
} satisfies GameModule;
