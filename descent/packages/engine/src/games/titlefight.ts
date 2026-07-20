// TITLE FIGHT — spec 5.9 + HDRealRules2 "SPIKES & CUTS". Task D-133 (spike).
// The arc's second physical spike: a living-room-executable duel with a
// crowd-judged winner. Two fighters get ASSIGNED, the deck names the duel, the
// room eggs them on for 30s while they perform it IRL, then everyone who ISN'T
// fighting points at the winner. Plurality takes the belt; a dead-even room is a
// SPLIT DECISION and BOTH fighters walk with the win (roast's DOUBLE ROAST, but
// nobody loses). One duel per spike — the palate cleanser, not an exposure bomb.
//
// Module law (games/module.ts): state lives opaquely in RoomState.gameState;
// view() is the ONLY serialization surface, so every redaction rule lives there.
// Pure module: time arrives as ctx.now, randomness as ctx.rand. Never a clock.
//
// Core integration (engine.ts "module step protocol"), single loop — built like roast:
//   $spotlight {roles: fighter-a/fighter-b} (fixed private assignment + dodge)
//   -> $deal (subjectless ceremony, night-dedup writeback) -> CORE_DEALT
//   -> $phase INPUT "BOUT" (30s; all phones = the crowd meter, fire owned by the
//      core; [REST] from either fighter ends the bout early)
//   -> $phase INPUT "VOTE" (15s skippable; everyone but the two fighters points at
//      the winner; module-owned vote timer, deadline -> resolve with what's in)
//   -> $phase REVEAL (core owns the hold: 3-2-1 flip beat, DESCEND softcap,
//      fire-decay) -> CORE_REVEAL_DONE -> done (one belt per spike).
import type { CardBase, DealRequest, Effect, Player, PlayerId, SpotlightRequest, Tier } from '../types.js';
import { cardLegal } from '../consent.js';
import { pick } from '../rng.js';
import {
  CORE_DEALT,
  CORE_REVEAL_DONE,
  CORE_SPOTLIGHT_DONE,
  type GameCtx,
  type GameModule,
  type GameStep,
} from './module.js';

// ===== tuning (spec 5.9 / 4.6 / 4.8) =====
export const DUELS_PER_SPIKE = 1; // one belt on the line per spike (ctx.circle.loops === 1)
export const BOUT_MS = 30_000; // the fighters perform IRL; the room's a crowd meter
export const VOTE_MS = 15_000; // VOTE is skippable: deadline -> resolve with the ballots in
export const IMP_WEIGHT = 0.5; // 4.8: imps judge at half weight; ties are never decided by imps
// Title Fight has no row in the 4.6 table (it ships at launch as a spike), so the
// points live here as constants — roast precedent (PLURALITY_PTS et al).
const WIN_PTS = 3; // the belt: winning fighter (BOTH on a split decision)
const PARTICIPATION_PTS = 1; // each voter who pointed, once — fighters earn by winning, not judging

// ===== module state (opaque to core; view() redacts) =====
export interface TitleFightResolution {
  cardId: string;
  cardText: string;
  winners: PlayerId[]; // 0 = no contest (void / no ballots), 1 = clean win, 2 = SPLIT DECISION
  splitDecision: boolean;
  spread: { fighterId: PlayerId; weight: number }[]; // BOTH fighters' bars, desc — anonymous counts
  voided: boolean;
}

export interface TitleFightState {
  sub: 'DEAL' | 'BOUT' | 'VOTE' | 'REVEAL'; // DEAL = core ceremony running, duel still secret
  loop: number; // 0-based duel index (spike ships loops === 1)
  loops: number;
  fighterA: PlayerId; // assigned at the spike's start — public from CORE_DEALT
  fighterB: PlayerId;
  card: CardBase | null; // the duel; null until CORE_DEALT (the ceremony owns the reveal)
  votes: Record<PlayerId, PlayerId>; // judge -> fighter voted for. SECRET — never leaves view() whole
  boutDeadline: number; // view mirrors; the authoritative deadline is the SCHEDULE effect
  voteDeadline: number;
  resolutions: TitleFightResolution[];
  participation: PlayerId[]; // voters already paid the once-per-spike +1
  fightersUsed: PlayerId[];
}

interface TitleFightAssignState {
  sub: 'ASSIGN';
  loop: number;
  loops: number;
  resolutions: TitleFightResolution[];
  participation: PlayerId[];
  fightersUsed: PlayerId[];
}

type TitleFightInternalState = TitleFightAssignState | TitleFightState;

// ===== views =====
// Field names are load-bearing — the client and the bots drive off these exact
// discriminants. loop/loops ride the BOUT frame only (spec 5.9 shape).
export type TitleFightBoutView = {
  deck: 'titlefight';
  sub: 'BOUT';
  duel: { id: string; text: string };
  fighterA: PlayerId;
  fighterB: PlayerId;
  youAreFighter: boolean;
  deadline: number;
  loop: number;
  loops: number;
};
export type TitleFightVoteView = {
  deck: 'titlefight';
  sub: 'VOTE';
  duel: { id: string; text: string };
  fighterA: PlayerId;
  fighterB: PlayerId;
  eligible: number;
  votedCount: number; // counts only — never who
  youVoted: PlayerId | null; // your own ballot, nobody else's
  youAreFighter: boolean; // fighters get "they're judging you", no ballot
  deadline: number;
};
export type TitleFightRevealView = {
  deck: 'titlefight';
  sub: 'REVEAL';
  duel: { id: string; text: string } | null;
  winners: PlayerId[];
  fighterA: PlayerId;
  fighterB: PlayerId;
  splitDecision: boolean;
  spread: { fighterId: PlayerId; weight: number }[];
  youVoted: PlayerId | null;
};
export type TitleFightView = TitleFightBoutView | TitleFightVoteView | TitleFightRevealView;

// ===== deck registry =====
// Card plumbing arrives with content integration (D-127/8.3); until then the core
// (or a test) injects a deck here. Stubs keep dev nights feral: every duel is a
// living-room-executable bit the crowd can actually judge — no props, no setup,
// just two people committing harder than the situation deserves.
export const DEFAULT_TITLEFIGHT_DECK: CardBase[] = [
  stub('001', 'Dead-eye staring contest. First to blink, laugh, or beg is the coward this room always suspected.', 1, 3, 'physical', 'staring-contest'),
  stub('002', 'Thumb war to the death. Loser stays silent the entire next round. Winner gets to gloat about it until sunrise.', 1, 3, 'physical', 'thumb-war'),
  stub('003', 'Hype-man showdown: ten seconds each to introduce your opponent like it is their walkout. Loudest, most unhinged intro takes the belt.', 1, 4, 'parody', 'hype-man-off'),
  stub('004', 'Slow-motion reenactment duel: act out your most embarrassing fall of the year, frame by frame. The room judges pure commitment.', 2, 4, 'physical', 'slow-mo-fall'),
  stub('005', 'Fake-cry contest. Summon real-looking tears on command. First believable sob wins the belt; first fake laugh forfeits their dignity.', 1, 3, 'deadpan', 'fake-cry-off'),
  stub('006', 'Villain monologue: explain, out loud, exactly why this friend group had it coming. Best menace wins; anyone who giggles loses.', 2, 4, 'menace', 'villain-monologue'),
  stub('007', 'Air-instrument solo. Pick your weapon, commit like your soul is the ante. The least committed loses it.', 1, 3, 'absurdist', 'air-solo'),
  stub('008', 'Ten seconds of pure trash talk about your opponent’s technique. All bark, zero facts. The room crowns the better mouth.', 2, 4, 'table-aware', 'trash-talk-freestyle'),
  stub('009', 'Seduce this table. No music, no touching, no mercy. Whoever makes the room more uncomfortable takes the belt.', 2, 5, 'physical', 'silent-seduction'),
  stub('010', 'Worst-impression duel: do your meanest impression of each other, right now, to each other’s face. The one that still lands wins.', 2, 4, 'table-aware', 'worst-impression'),
];
function stub(n: string, text: string, exposure: Tier, chaos: Tier, register: CardBase['register'], skeleton: string): CardBase {
  return { id: `titlefight_stub_${n}`, deck: 'titlefight', text, exposure, chaos, register, skeleton };
}

let deckCards: CardBase[] = DEFAULT_TITLEFIGHT_DECK;
export function setTitleFightDeck(cards: readonly CardBase[]): void {
  if (cards.length === 0) throw new Error('titlefight deck cannot be empty');
  deckCards = [...cards];
}

// ===== helpers =====
const boutTimer = (circleIdx: number, loop: number): string => `titlefight:bout:${circleIdx}:${loop}`;
const voteTimer = (circleIdx: number, loop: number): string => `titlefight:vote:${circleIdx}:${loop}`;
const weightOf = (p: Player): number => (p.role === 'imp' ? IMP_WEIGHT : 1);
const hostIdOf = (ctx: GameCtx): PlayerId | null => ctx.players.find((p) => p.role === 'host')?.id ?? null;
/** Everyone but the two fighters judges — imps too, at half weight (4.8). */
const judgesOf = (ctx: GameCtx, st: TitleFightState): Player[] =>
  [...ctx.players, ...ctx.imps].filter((p) => p.id !== st.fighterA && p.id !== st.fighterB);

function readState(ctx: GameCtx): TitleFightInternalState | null {
  const gs = ctx.state.gameState as TitleFightInternalState | null | undefined;
  return gs && (gs.sub === 'ASSIGN' || gs.sub === 'DEAL' || gs.sub === 'BOUT' || gs.sub === 'VOTE' || gs.sub === 'REVEAL') ? gs : null;
}

function noop(ctx: GameCtx): GameStep {
  return { gameState: ctx.state.gameState, effects: [] };
}

function parseBallot(payload: unknown): PlayerId | null {
  if (typeof payload !== 'object' || payload === null) return null;
  const v = (payload as Record<string, unknown>)['vote'];
  return typeof v === 'string' && v.length > 0 ? v : null;
}

// ===== fighter + card selection =====
// Title Fight is subjectless: the duel is a physical bit, not a card ABOUT anyone,
// so the GENERIC ceiling applies (consent.ts) — the belt never runs hotter than
// the room agreed to. Rung preference (4.3) and night-dedup ride on top. These
// spikes are palate cleansers (exposure 1-2), so the pool is almost never gated.
function legalPool(ctx: GameCtx): CardBase[] {
  const ceilings = ctx.players.map((p) => p.heatCeiling);
  let legal = deckCards.filter((c) => cardLegal(c.exposure, { ceilings }));
  if (legal.length === 0) {
    // Content bug (nothing legal): degrade to the mildest duels rather than
    // deadlock the night. The arc builder gates depth; this is the backstop.
    const minE = Math.min(...deckCards.map((c) => c.exposure));
    legal = deckCards.filter((c) => c.exposure === minE);
  }
  const underRung = legal.filter((c) => c.exposure <= ctx.circle.rung);
  return underRung.length > 0 ? underRung : legal;
}

/** Pick primary + reserved backup (4.5), night-deduped via usedCardIds (core writes back on ceremony completion). */
function pickDuelPair(ctx: GameCtx): { primary: CardBase; backup: CardBase } {
  const legal = legalPool(ctx);
  const fresh = legal.filter((c) => !ctx.state.usedCardIds.includes(c.id));
  const pool = fresh.length > 0 ? fresh : legal; // deck exhausted: a repeat beats a dead night
  const primary = pick(ctx.rand, pool);
  const rest = pool.filter((c) => c.id !== primary.id);
  const fallback = legal.filter((c) => c.id !== primary.id);
  const backup = rest.length > 0 ? pick(ctx.rand, rest) : fallback.length > 0 ? pick(ctx.rand, fallback) : primary;
  return { primary, backup };
}

function eligibleFighterIds(ctx: GameCtx, exclude: readonly PlayerId[]): PlayerId[] {
  let candidates = ctx.players.filter((p) => !exclude.includes(p.id));
  if (candidates.length < 2) candidates = [...ctx.players];
  return candidates.map((p) => p.id);
}

// ===== phase openers =====
interface Carried {
  resolutions: TitleFightResolution[];
  participation: PlayerId[];
  fightersUsed: PlayerId[];
}

/** Ask core for two private, distinct fighter assignments before dealing the duel. */
function assignDuel(ctx: GameCtx, loop: number, loops: number, carried: Carried): GameStep {
  const eligibleIds = eligibleFighterIds(ctx, carried.fightersUsed);
  if (eligibleIds.length < 2) return { gameState: ctx.state.gameState, effects: [], done: true };
  const $spotlight: SpotlightRequest = { roles: ['fighter-a', 'fighter-b'], eligibleIds };
  const gameState: TitleFightAssignState & { $spotlight: SpotlightRequest } = {
    sub: 'ASSIGN',
    loop,
    loops,
    resolutions: carried.resolutions,
    participation: carried.participation,
    fightersUsed: carried.fightersUsed,
    $spotlight,
  };
  return { gameState, effects: [] };
}

function dealAssigned(
  ctx: GameCtx,
  pending: TitleFightAssignState,
  fighterA: PlayerId | null,
  fighterB: PlayerId | null,
): GameStep {
  const activeIds = new Set(ctx.players.map((p) => p.id));
  const finalIds = [...new Set([fighterA, fighterB].filter((id): id is PlayerId => id !== null && activeIds.has(id)))];
  const settled: TitleFightAssignState = {
    ...pending,
    fightersUsed: [...pending.fightersUsed, ...finalIds.filter((id) => !pending.fightersUsed.includes(id))],
  };
  if (fighterA === null || fighterB === null || fighterA === fighterB || !activeIds.has(fighterA) || !activeIds.has(fighterB)) {
    return skipAssignment(ctx, settled);
  }
  const { primary, backup } = pickDuelPair(ctx);
  const $deal: DealRequest = { primary, backup, subjectId: null };
  const gameState: TitleFightState & { $deal: DealRequest } = {
    sub: 'DEAL',
    loop: pending.loop,
    loops: pending.loops,
    fighterA,
    fighterB,
    card: null, // secret until the ceremony completes (CORE_DEALT)
    votes: {},
    boutDeadline: 0,
    voteDeadline: 0,
    resolutions: pending.resolutions,
    participation: pending.participation,
    fightersUsed: settled.fightersUsed,
    $deal,
  };
  return { gameState, effects: [] };
}

function skipAssignment(ctx: GameCtx, pending: TitleFightAssignState): GameStep {
  if (pending.loop + 1 < pending.loops) return assignDuel(ctx, pending.loop + 1, pending.loops, pending);
  return { gameState: pending, effects: [], done: true };
}

/** Ceremony done: the duel is public. Ring the bell — 30s of crowd-meter BOUT. */
function openBout(ctx: GameCtx, st: TitleFightState): GameStep {
  const card = ctx.state.deal?.done === true ? (ctx.state.deal.card as CardBase) : null;
  if (card === null || card.deck !== 'titlefight') return noop(ctx); // stale CORE_DEALT
  const deadline = ctx.now + BOUT_MS;
  const gameState: TitleFightState & { $phase: { k: 'INPUT'; sub: string; deadline: number } } = {
    ...st,
    sub: 'BOUT',
    card,
    boutDeadline: deadline,
    $phase: { k: 'INPUT', sub: 'BOUT', deadline },
  };
  return { gameState, effects: [{ k: 'SCHEDULE', timerId: boutTimer(ctx.circleIdx, st.loop), atMs: deadline }] };
}

/** Bell rang (timer or a fighter's [REST]): open the 15s skippable point-at-the-winner VOTE. */
function openVote(ctx: GameCtx, st: TitleFightState, pre: Effect[]): GameStep {
  const deadline = ctx.now + VOTE_MS;
  const gameState: TitleFightState & { $phase: { k: 'INPUT'; sub: string; deadline: number } } = {
    ...st,
    sub: 'VOTE',
    votes: {},
    voteDeadline: deadline,
    $phase: { k: 'INPUT', sub: 'VOTE', deadline },
  };
  return { gameState, effects: [...pre, { k: 'SCHEDULE', timerId: voteTimer(ctx.circleIdx, st.loop), atMs: deadline }] };
}

// ===== vote math =====
function leaders(t: Map<PlayerId, number>): PlayerId[] {
  let max = 0;
  for (const v of t.values()) if (v > max) max = v;
  if (max <= 0) return [];
  return [...t.entries()]
    .filter(([, v]) => v === max)
    .map(([k]) => k)
    .sort();
}

// Plurality with the imp clause (4.8): imps judge at 0.5 weight, but a tie is
// never DECIDED by imps — a full-vote tie is a SPLIT DECISION no matter what imps
// did, and an imp-manufactured weighted tie collapses back to the full-vote
// leader. Imp weight still counts whenever no tie is in play (roast precedent).
function decideWinners(weighted: Map<PlayerId, number>, full: Map<PlayerId, number>): PlayerId[] {
  const fullLeaders = leaders(full);
  if (fullLeaders.length >= 2) return fullLeaders; // full-vote tie -> split, imps can't break it
  const weightedLeaders = leaders(weighted);
  if (fullLeaders.length === 1 && weightedLeaders.length >= 2) return fullLeaders;
  return weightedLeaders; // single weighted leader, or the all-imp-ballots edge case
}

function tally(
  votes: Record<PlayerId, PlayerId>,
  judges: Player[],
  fighterA: PlayerId,
  fighterB: PlayerId,
): { winners: PlayerId[]; spread: TitleFightResolution['spread'] } {
  const weighted = new Map<PlayerId, number>();
  const full = new Map<PlayerId, number>();
  for (const judge of judges) {
    const to = votes[judge.id];
    if (to === undefined) continue;
    weighted.set(to, (weighted.get(to) ?? 0) + weightOf(judge));
    if (judge.role !== 'imp') full.set(to, (full.get(to) ?? 0) + 1);
  }
  // Both fighters get a bar (even a zero) — the reveal shows the split, not a leaderboard.
  const spread = [fighterA, fighterB]
    .map((fighterId) => ({ fighterId, weight: weighted.get(fighterId) ?? 0 }))
    .sort((a, b) => b.weight - a.weight || a.fighterId.localeCompare(b.fighterId));
  return { winners: decideWinners(weighted, full), spread };
}

// Scoring (pre-multiplier — core applies finale x3 / Bargain x2):
//   winning fighter +3 (BOTH fighters on a SPLIT DECISION); each judge who
//   actually pointed +1 participation, once. Fighters can't vote, so the belt and
//   the participation pot never touch the same id. A no-contest (void or an empty
//   room) pays nobody — you don't win a belt nobody bet on.
function resolve(ctx: GameCtx, st: TitleFightState, pre: Effect[], voided = false): GameStep {
  if (st.card === null) return noop(ctx); // unreachable: BOUT/VOTE always carry a card
  const judges = judgesOf(ctx, st);
  const votes = voided ? {} : st.votes; // voided duel: no ballots count (4.6)
  const { winners, spread } = tally(votes, judges, st.fighterA, st.fighterB);
  const splitDecision = winners.length >= 2;

  const scores: Record<string, number> = {};
  const add = (id: PlayerId, pts: number): void => {
    if (pts > 0) scores[id] = (scores[id] ?? 0) + pts;
  };
  const participation = [...st.participation];
  if (!voided) {
    for (const w of winners) add(w, WIN_PTS);
    for (const judge of judges) {
      if (votes[judge.id] === undefined) continue; // abstainers earn nothing
      if (!participation.includes(judge.id)) {
        participation.push(judge.id);
        add(judge.id, PARTICIPATION_PTS);
      }
    }
  }

  const resolution: TitleFightResolution = {
    cardId: st.card.id,
    cardText: st.card.text,
    winners,
    splitDecision,
    spread,
    voided,
  };
  // $phase REVEAL: the core stamps holdSince and owns the hold — synced 3-2-1 flip
  // beat, DESCEND (host anytime / anyone past 45s), fire-decay past the 20s min (4.2).
  const gameState: TitleFightState & { $phase: { k: 'REVEAL' } } = {
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
export const titlefightModule = {
  deck: 'titlefight',
  minN: 3,

  start(ctx: GameCtx): GameStep {
    const loops = ctx.circle.loops >= 1 ? ctx.circle.loops : DUELS_PER_SPIKE;
    return assignDuel(ctx, 0, loops, { resolutions: [], participation: [], fightersUsed: [] });
  },

  input(ctx: GameCtx, playerId: string, payload: unknown): GameStep {
    const st = readState(ctx);
    if (!st || st.sub !== 'VOTE') return noop(ctx); // stale/foreign ballots die silently (3.5)
    const target = parseBallot(payload);
    if (target === null) return noop(ctx);
    if (target !== st.fighterA && target !== st.fighterB) return noop(ctx); // you may only point at a fighter
    const judges = judgesOf(ctx, st);
    const judge = judges.find((p) => p.id === playerId);
    if (!judge) return noop(ctx); // fighters don't judge their own duel; ghosts don't either
    const votes = { ...st.votes, [playerId]: target }; // re-vote allowed until deadline: last ballot wins
    const next: TitleFightState = { ...st, votes };
    if (Object.keys(votes).length >= judges.length) {
      // Room's all in — don't make the fighters sweat a countdown.
      return resolve(ctx, next, [{ k: 'CANCEL', timerId: voteTimer(ctx.circleIdx, st.loop) }]);
    }
    return { gameState: next, effects: [{ k: 'BROADCAST' }] }; // count ticks up; view() hides who
  },

  timer(ctx: GameCtx, timerId: string): GameStep {
    const st = readState(ctx);
    if (!st) return noop(ctx);
    if (st.sub === 'ASSIGN' && timerId === CORE_SPOTLIGHT_DONE) {
      const fighterA = ctx.spotlight?.assignments.find((a) => a.role === 'fighter-a')?.playerId ?? null;
      const fighterB = ctx.spotlight?.assignments.find((a) => a.role === 'fighter-b')?.playerId ?? null;
      return dealAssigned(ctx, st, fighterA, fighterB);
    }
    if (st.sub === 'DEAL' && timerId === CORE_DEALT) {
      return openBout(ctx, st); // ceremony done: duel public, bell rings
    }
    if (st.sub === 'BOUT' && timerId === boutTimer(ctx.circleIdx, st.loop)) {
      return openVote(ctx, st, []); // 30s up: the room votes on what it saw
    }
    if (st.sub === 'VOTE' && timerId === voteTimer(ctx.circleIdx, st.loop)) {
      return resolve(ctx, st, []); // deadline: non-voters auto-abstain — the game never waits
    }
    if (st.sub === 'REVEAL' && timerId === CORE_REVEAL_DONE) {
      if (st.loop + 1 < st.loops) return assignDuel(ctx, st.loop + 1, st.loops, st); // (spike runs one)
      return { gameState: st, effects: [], done: true }; // core -> LADDER
    }
    return noop(ctx); // stale timer for a dead loop
  },

  control(ctx: GameCtx, playerId: string, kind: 'REST' | 'SKIPEM' | 'FIFTH' | 'VOID'): GameStep {
    const st = readState(ctx);
    if (!st || st.sub === 'ASSIGN') return noop(ctx);
    const hostId = hostIdOf(ctx);

    if (kind === 'REST') {
      // [REST] — either fighter throws in the towel, mid-bout only. Ends the 30s early.
      if (st.sub !== 'BOUT' || (playerId !== st.fighterA && playerId !== st.fighterB)) return noop(ctx);
      return openVote(ctx, st, [{ k: 'CANCEL', timerId: boutTimer(ctx.circleIdx, st.loop) }]);
    }

    if (kind === 'VOID') {
      if (hostId !== null && playerId === hostId) {
        // Host kill-switch (4.7): the duel dies scoreless, no belt awarded.
        if (st.sub === 'BOUT') return resolve(ctx, st, [{ k: 'CANCEL', timerId: boutTimer(ctx.circleIdx, st.loop) }], true);
        if (st.sub === 'VOTE') return resolve(ctx, st, [{ k: 'CANCEL', timerId: voteTimer(ctx.circleIdx, st.loop) }], true);
        return noop(ctx); // DEAL rides out its ceremony; REVEAL is already resolved
      }
      // Seat-lapse convention (engine 4.7): a NON-host VOID = that seat lapsed. A
      // fighter vanishing mid-bout kills the duel (no opponent, no contest); a
      // lapsed spectator just stops voting, and the crowd already saw the fight.
      if ((playerId === st.fighterA || playerId === st.fighterB) && st.sub === 'BOUT') {
        return resolve(ctx, st, [{ k: 'CANCEL', timerId: boutTimer(ctx.circleIdx, st.loop) }], true);
      }
      return noop(ctx);
    }

    return noop(ctx); // SKIPEM / FIFTH: a physical duel has no re-deal valve and no private lock
  },

  // The ONLY serialization surface. Redaction law (3.4 + 5.9):
  // - during the DEAL ceremony NOTHING serializes (the duel is still secret; the
  //   core runs its own "dealing" ritual — fighters go public when the bell rings);
  // - during VOTE a viewer sees counts + their OWN ballot, nothing else (the vote
  //   map never leaves the server; a fighter sees youAreFighter, no ballot);
  // - the reveal spread is anonymous weighted counts — no judge-to-fighter pair
  //   ever crosses the wire — plus the winner(s) in lights;
  // - per-loop score deltas are never in the view (a +1 would out a ballot).
  view(ctx: GameCtx, viewerId: string): TitleFightView | null {
    const st = readState(ctx);
    if (!st || st.sub === 'ASSIGN' || st.sub === 'DEAL' || st.card === null) return null;
    const duel = { id: st.card.id, text: st.card.text };
    const youAreFighter = viewerId === st.fighterA || viewerId === st.fighterB;

    if (st.sub === 'BOUT') {
      return {
        deck: 'titlefight',
        sub: 'BOUT',
        duel,
        fighterA: st.fighterA,
        fighterB: st.fighterB,
        youAreFighter,
        deadline: st.boutDeadline,
        loop: st.loop,
        loops: st.loops,
      };
    }
    if (st.sub === 'VOTE') {
      return {
        deck: 'titlefight',
        sub: 'VOTE',
        duel,
        fighterA: st.fighterA,
        fighterB: st.fighterB,
        eligible: judgesOf(ctx, st).length,
        votedCount: Object.keys(st.votes).length,
        youVoted: st.votes[viewerId] ?? null,
        youAreFighter,
        deadline: st.voteDeadline,
      };
    }
    const res = st.resolutions[st.resolutions.length - 1];
    if (!res) return null; // unreachable by construction; never leak on a bug
    return {
      deck: 'titlefight',
      sub: 'REVEAL',
      duel: { id: res.cardId, text: res.cardText }, // titlefight always has a card at reveal; null branch is for the shape only
      winners: res.winners,
      fighterA: st.fighterA,
      fighterB: st.fighterB,
      splitDecision: res.splitDecision,
      spread: res.spread,
      youVoted: st.votes[viewerId] ?? null,
    };
  },
} satisfies GameModule;
