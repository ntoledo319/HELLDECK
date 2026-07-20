// ROAST CONSENSUS — spec 5.1 + HDRealRules2 Part III §1. Task D-112.
// Three prompts per circle. The room secretly convicts one of its own; synced
// reveal puts the victim's name in lights; the reveal holds while the table
// does the actual roasting. The app counts ballots — humans supply the violence.
//
// Module law (games/module.ts): state lives opaquely in RoomState.gameState;
// view() is the ONLY serialization surface, so every redaction rule lives there.
// Pure module: time arrives as ctx.now, randomness as ctx.rand. Never a clock.
//
// Core integration (engine.ts "module step protocol"): each prompt runs
//   $deal (4.5 ceremony, 5.5s, night-dedup writeback) -> CORE_DEALT
//   -> $phase INPUT "VOTE" (20s skippable, module-owned vote timer)
//   -> $phase REVEAL (core owns the hold: 3-2-1 flip beat, DESCEND softcap,
//      fire-decay) -> CORE_REVEAL_DONE -> next prompt or done.
import type { CardBase, DealRequest, Effect, Player, PlayerId, Tier } from '../types.js';
import { cardLegal } from '../consent.js';
import { pick } from '../rng.js';
import { CORE_DEALT, CORE_REVEAL_DONE, type GameCtx, type GameModule, type GameStep } from './module.js';

// ===== tuning (spec 5.1 / 4.6 / 4.8) =====
export const PROMPTS_PER_CIRCLE = 3;
export const VOTE_MS = 20_000; // VOTE is skippable: deadline -> auto-abstain
export const ATTRIBUTED_MAX_N = 4; // N<=4 -> FACE YOUR ACCUSERS
export const ATTRIBUTED_EXPOSURE_CAP: Tier = 3; // prompts capped E<=3 in attributed mode
export const SUPPRESSED_EDGE = '▮▮▮'; // per-circle repeat-edge display suppression
export const IMP_WEIGHT = 0.5;
const PLURALITY_PTS = 2;
const HEAT_PTS = 1;
const PARTICIPATION_PTS = 1; // once per circle, any ballot cast

// ===== module state (opaque to core; view() redacts) =====
export interface RoastResolution {
  cardId: string;
  cardText: string;
  victims: PlayerId[]; // 0 = voided/no votes, 1 = clean kill, 2+ = DOUBLE ROAST
  doubleRoast: boolean;
  roomHeat: boolean;
  voided: boolean;
  spread: { target: PlayerId; weight: number }[]; // anonymous bars, desc
  displayEdges: { from: PlayerId; to: PlayerId | typeof SUPPRESSED_EDGE }[]; // attributed mode only
}

export interface RoastState {
  sub: 'DEAL' | 'VOTE' | 'REVEAL'; // DEAL = core ceremony running, card still secret
  loop: number; // 0-based prompt index
  loops: number; // 3 per spec
  card: CardBase | null; // null until CORE_DEALT (the ceremony owns the secret)
  votes: Record<PlayerId, PlayerId>; // voter -> target. SECRET — never leaves view() attributed at N>=5
  voteDeadline: number; // server ms mirror for view; authoritative deadline is the SCHEDULE effect
  resolutions: RoastResolution[];
  shownEdges: string[]; // "voter>target" edges already displayed this circle
  participation: PlayerId[]; // voters already paid the once-per-circle +1
}

export type RoastVoteView = {
  deck: 'roast';
  sub: 'VOTE';
  loop: number;
  loops: number;
  attributed: boolean;
  prompt: { id: string; text: string };
  deadline: number;
  eligible: number;
  votedCount: number; // counts only — never who
  youVoted: PlayerId | null; // your own ballot, nobody else's
};
export type RoastRevealView = {
  deck: 'roast';
  sub: 'REVEAL';
  loop: number;
  loops: number;
  attributed: boolean;
  prompt: { id: string; text: string };
  victims: PlayerId[];
  doubleRoast: boolean;
  roomHeat: boolean;
  voided: boolean;
  youVoted: PlayerId | null;
  edges?: { from: PlayerId; to: string }[]; // N<=4 FACE YOUR ACCUSERS
  spread?: { target: PlayerId; weight: number }[]; // N>=5 anonymous
};
export type RoastView = RoastVoteView | RoastRevealView;

// ===== deck registry =====
// Card plumbing arrives with content integration (D-127/8.3); until then the core
// (or a test) injects a deck here. Stubs below keep dev nights from tasting like lorem ipsum.
export const DEFAULT_ROAST_DECK: CardBase[] = [
  stub('001', 'Who here would narc on the group chat just to seem interesting at brunch?', 2, 3, 'petty-domestic', 'narc-for-clout'),
  stub('002', 'Whose browser history is a federal exhibit waiting for a case number?', 3, 4, 'menace', 'browser-history'),
  stub('003', 'Who would survive prison purely by being too annoying to keep?', 2, 4, 'absurdist', 'prison-annoying'),
  stub('004', 'Who is one bad month away from starting a podcast about themselves?', 2, 3, 'deadpan', 'podcast-threat'),
  stub('005', "Who would read this room's group chat aloud at your funeral?", 2, 4, 'menace', 'funeral-groupchat'),
  stub('006', 'Who peaked in a year they refuse to say out loud?', 3, 3, 'observational', 'peaked-year'),
  stub('007', 'Who would text their ex from a borrowed phone in this room tonight?', 4, 4, 'table-aware', 'ex-relapse'),
  stub('008', "Who has cried in a bar bathroom and filed it under self-care?", 4, 3, 'euphemism', 'bathroom-selfcare'),
];
function stub(n: string, text: string, exposure: Tier, chaos: Tier, register: CardBase['register'], skeleton: string): CardBase {
  return { id: `roast_stub_${n}`, deck: 'roast', text, exposure, chaos, register, skeleton };
}

let deckCards: CardBase[] = DEFAULT_ROAST_DECK;
export function setRoastDeck(cards: readonly CardBase[]): void {
  if (cards.length === 0) throw new Error('roast deck cannot be empty');
  deckCards = [...cards];
}

/** Filter hook (5.1): attributed rooms (N<=4) never see prompts above E3. Ballots are NEVER constrained — only prompts. */
export function roastPromptFilter(cards: readonly CardBase[], nPlayers: number): CardBase[] {
  return nPlayers <= ATTRIBUTED_MAX_N ? cards.filter((c) => c.exposure <= ATTRIBUTED_EXPOSURE_CAP) : [...cards];
}

// ===== helpers =====
const voteTimer = (circleIdx: number, loop: number): string => `roast:vote:${circleIdx}:${loop}`;
const votersOf = (ctx: GameCtx): Player[] => [...ctx.players, ...ctx.imps]; // imps are citizens: they vote (at half) and are roastable
const weightOf = (p: Player): number => (p.role === 'imp' ? IMP_WEIGHT : 1);
const isAttributed = (ctx: GameCtx): boolean => ctx.players.length <= ATTRIBUTED_MAX_N;

function readState(ctx: GameCtx): RoastState | null {
  const gs = ctx.state.gameState as RoastState | null | undefined;
  return gs && (gs.sub === 'DEAL' || gs.sub === 'VOTE' || gs.sub === 'REVEAL') ? gs : null;
}

function noop(ctx: GameCtx): GameStep {
  return { gameState: ctx.state.gameState, effects: [] };
}

function parseBallot(payload: unknown): PlayerId | null {
  if (typeof payload !== 'object' || payload === null) return null;
  const v = (payload as Record<string, unknown>)['vote'];
  return typeof v === 'string' && v.length > 0 ? v : null;
}

// Consent (4.4, roast is vote-emergent) beats the attributed cap beats the E-curve
// rung beats freshness: repeating a prompt is a lesser sin than dealing an
// over-exposed one into a room that never consented to it.
function legalPool(ctx: GameCtx): CardBase[] {
  const ceilings = ctx.players.map((p) => p.heatCeiling);
  let legal = deckCards.filter((c) => cardLegal(c.exposure, { ceilings, voteEmergent: true }));
  legal = roastPromptFilter(legal, ctx.players.length);
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
function pickPromptPair(ctx: GameCtx): { primary: CardBase; backup: CardBase } {
  const legal = legalPool(ctx);
  const fresh = legal.filter((c) => !ctx.state.usedCardIds.includes(c.id));
  const pool = fresh.length > 0 ? fresh : legal; // deck exhausted: a repeat beats a dead night
  const primary = pick(ctx.rand, pool);
  const rest = pool.filter((c) => c.id !== primary.id);
  const fallback = legal.filter((c) => c.id !== primary.id);
  const backup = rest.length > 0 ? pick(ctx.rand, rest) : fallback.length > 0 ? pick(ctx.rand, fallback) : primary;
  return { primary, backup };
}

/** Hand the prompt to the core's 4.5 ceremony. No subject: roast victims are vote-emergent. */
function deal(
  ctx: GameCtx,
  loop: number,
  loops: number,
  carried: Pick<RoastState, 'shownEdges' | 'participation' | 'resolutions'>,
): GameStep {
  const { primary, backup } = pickPromptPair(ctx);
  const $deal: DealRequest = { primary, backup, subjectId: null };
  const gameState: RoastState & { $deal: DealRequest } = {
    sub: 'DEAL',
    loop,
    loops,
    card: null, // secret until the ceremony completes (CORE_DEALT)
    votes: {},
    voteDeadline: 0,
    resolutions: carried.resolutions,
    shownEdges: carried.shownEdges,
    participation: carried.participation,
    $deal,
  };
  return { gameState, effects: [] };
}

/** Ceremony done: the card is public. Open the 20s skippable VOTE. */
function openVote(ctx: GameCtx, st: RoastState): GameStep {
  const card = ctx.state.deal?.done === true ? (ctx.state.deal.card as CardBase) : null;
  if (card === null) return noop(ctx); // no completed deal — stale CORE_DEALT
  const gameState: RoastState & { $phase: { k: 'INPUT'; sub: string; deadline: number } } = {
    ...st,
    sub: 'VOTE',
    card,
    votes: {},
    voteDeadline: ctx.now + VOTE_MS,
    $phase: { k: 'INPUT', sub: 'VOTE', deadline: ctx.now + VOTE_MS },
  };
  return {
    gameState,
    effects: [{ k: 'SCHEDULE', timerId: voteTimer(ctx.circleIdx, st.loop), atMs: ctx.now + VOTE_MS }],
  };
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

// Plurality with the imp clause (4.8): imps vote at 0.5 weight, but a tie is
// never DECIDED by imps — a full-vote tie stands (DOUBLE ROAST) no matter what
// imps did, and an imp-manufactured weighted tie collapses back to the
// full-vote leader. Imp weight still counts whenever no tie is in play.
function decideVictims(weighted: Map<PlayerId, number>, full: Map<PlayerId, number>): PlayerId[] {
  const fullLeaders = leaders(full);
  if (fullLeaders.length >= 2) return fullLeaders;
  const weightedLeaders = leaders(weighted);
  if (fullLeaders.length === 1 && weightedLeaders.length >= 2) return fullLeaders;
  return weightedLeaders; // single weighted leader, or the all-imp-ballots edge case
}

function tally(votes: Record<PlayerId, PlayerId>, voters: Player[]): { victims: PlayerId[]; spread: RoastResolution['spread'] } {
  const weighted = new Map<PlayerId, number>();
  const full = new Map<PlayerId, number>();
  for (const voter of voters) {
    const to = votes[voter.id];
    if (to === undefined) continue;
    weighted.set(to, (weighted.get(to) ?? 0) + weightOf(voter));
    if (voter.role !== 'imp') full.set(to, (full.get(to) ?? 0) + 1);
  }
  const spread = [...weighted.entries()]
    .map(([target, weight]) => ({ target, weight }))
    .sort((a, b) => b.weight - a.weight || a.target.localeCompare(b.target));
  return { victims: decideVictims(weighted, full), spread };
}

// Room Heat (4.6): unanimity-minus-victim at N<=5; >=60% at N>=8; >=80% at N 6-7.
// Denominator is every eligible voter (abstainers break unanimity); imps fuel it at half weight.
function hasRoomHeat(victims: PlayerId[], votes: Record<PlayerId, PlayerId>, voters: Player[], nPlayers: number): boolean {
  if (victims.length === 0) return false;
  let num = 0;
  let denom = 0;
  for (const p of voters) {
    if (victims.includes(p.id)) continue;
    denom += weightOf(p);
    const to = votes[p.id];
    if (to !== undefined && victims.includes(to)) num += weightOf(p);
  }
  if (denom <= 0) return false;
  if (nPlayers <= 5) return num === denom;
  const threshold = nPlayers >= 8 ? 0.6 : 0.8;
  return num / denom >= threshold - 1e-9;
}

function resolve(ctx: GameCtx, st: RoastState, preEffects: Effect[], voided = false): GameStep {
  if (st.card === null) return noop(ctx); // unreachable: VOTE always has a card
  const voters = votersOf(ctx);
  const votes = voided ? {} : st.votes; // voided loop: 0 to all (4.6)
  const { victims, spread } = tally(votes, voters);
  const doubleRoast = victims.length >= 2;
  const roomHeat = !voided && hasRoomHeat(victims, votes, voters, ctx.players.length);

  // FACE YOUR ACCUSERS display edges, with per-circle repeat suppression.
  // Display-only: ballots were never constrained (at N=3 only 6 edges exist).
  const shownEdges = [...st.shownEdges];
  const displayEdges: RoastResolution['displayEdges'] = [];
  if (isAttributed(ctx) && !voided) {
    for (const voter of voters) {
      const to = votes[voter.id];
      if (to === undefined) continue;
      const key = `${voter.id}>${to}`;
      if (shownEdges.includes(key)) {
        displayEdges.push({ from: voter.id, to: SUPPRESSED_EDGE });
      } else {
        shownEdges.push(key);
        displayEdges.push({ from: voter.id, to });
      }
    }
  }

  // Scoring (pre-multiplier — core applies finale x3 / Bargain x2):
  // +2 voted-the-plurality (either victim on DOUBLE ROAST), +1 Room Heat rider,
  // +1 participation once per circle. Victims earn nothing for being victims.
  const scores: Record<string, number> = {};
  const participation = [...st.participation];
  if (!voided) {
    for (const voter of voters) {
      const to = votes[voter.id];
      if (to === undefined) continue;
      let pts = 0;
      if (victims.includes(to)) pts += PLURALITY_PTS + (roomHeat ? HEAT_PTS : 0);
      if (!participation.includes(voter.id)) {
        participation.push(voter.id);
        pts += PARTICIPATION_PTS;
      }
      if (pts > 0) scores[voter.id] = pts;
    }
  }

  const resolution: RoastResolution = {
    cardId: st.card.id,
    cardText: st.card.text,
    victims,
    doubleRoast,
    roomHeat,
    voided,
    spread,
    displayEdges,
  };
  // $phase REVEAL: the core stamps holdSince and owns the hold — 3-2-1 flip beat,
  // DESCEND (host anytime / anyone past 45s), fire-decay past the 20s minimum (4.2).
  const gameState: RoastState & { $phase: { k: 'REVEAL' } } = {
    ...st,
    sub: 'REVEAL',
    resolutions: [...st.resolutions, resolution],
    shownEdges,
    participation,
    $phase: { k: 'REVEAL' },
  };
  return {
    gameState,
    effects: [...preEffects, { k: 'AUDIO', sting: voided ? 'burn' : 'boom' }, { k: 'SNAPSHOT' }],
    scores,
    // TYPECAST GOVERNOR (card-council finding): a roast victim is ELECTED by ballot, so the
    // 4.5 spotlight-fairness counter never saw them — yet being named in lights IS the
    // spotlight. Roast is repeatable up to 2x/night and eligible as opener, bargain slot AND
    // finale, so without this one quiet player can be voted the answer all night while
    // pickSpotlight happily hands them the subject/confessor/defender seat too. The exposure
    // ceilings can't catch it (it's a FREQUENCY problem, not an exposure-tier one). Feeding
    // victims through the same channel makes every other game lean AWAY from them.
    ...(voided || victims.length === 0 ? {} : { spotlight: victims }),
  };
}

// ===== the module =====
export const roastModule = {
  deck: 'roast',
  minN: 3,

  start(ctx: GameCtx): GameStep {
    const loops = ctx.circle.loops >= 1 ? ctx.circle.loops : PROMPTS_PER_CIRCLE;
    return deal(ctx, 0, loops, { shownEdges: [], participation: [], resolutions: [] });
  },

  input(ctx: GameCtx, playerId: string, payload: unknown): GameStep {
    const st = readState(ctx);
    if (!st || st.sub !== 'VOTE') return noop(ctx); // stale ballots die silently (3.5)
    const target = parseBallot(payload);
    if (target === null) return noop(ctx);
    const voters = votersOf(ctx);
    const voter = voters.find((p) => p.id === playerId);
    const victim = voters.find((p) => p.id === target);
    if (!voter || !victim || voter.id === victim.id) return noop(ctx); // self-vote blocked; nothing else ever is
    const votes = { ...st.votes, [playerId]: victim.id }; // re-vote allowed until deadline: last ballot wins
    const next: RoastState = { ...st, votes };
    if (Object.keys(votes).length >= voters.length) {
      // Room's all in — don't make them stare at a countdown.
      return resolve(ctx, next, [{ k: 'CANCEL', timerId: voteTimer(ctx.circleIdx, st.loop) }]);
    }
    return { gameState: next, effects: [{ k: 'BROADCAST' }] }; // count ticks up; view() hides who
  },

  timer(ctx: GameCtx, timerId: string): GameStep {
    const st = readState(ctx);
    if (!st) return noop(ctx);
    if (st.sub === 'DEAL' && timerId === CORE_DEALT) {
      return openVote(ctx, st); // ceremony done: card public, VOTE opens
    }
    if (st.sub === 'VOTE' && timerId === voteTimer(ctx.circleIdx, st.loop)) {
      return resolve(ctx, st, []); // deadline: non-voters auto-abstain — the game never waits
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
      // Host kill-switch (4.7): the loop dies scoreless, sting blames nobody.
      const caller = ctx.players.find((p) => p.id === playerId);
      if (!caller || caller.role !== 'host' || st.sub !== 'VOTE') return noop(ctx);
      return resolve(ctx, st, [{ k: 'CANCEL', timerId: voteTimer(ctx.circleIdx, st.loop) }], true);
    }
    return noop(ctx); // REST/SKIPEM/FIFTH: roast has no performer and no blocking input
  },

  // The ONLY serialization surface. Redaction law (3.4 + 5.1):
  // - during the DEAL ceremony NOTHING serializes (the card is still secret);
  // - during VOTE a viewer sees counts + their OWN ballot, nothing else;
  // - after resolution: N>=5 anonymous spread (no voter-victim pair leaves the
  //   server), N<=4 attributed edges with per-circle repeat suppression;
  // - per-prompt score deltas are never in the view (a +2 would out a ballot).
  view(ctx: GameCtx, viewerId: string): RoastView | null {
    const st = readState(ctx);
    if (!st || st.sub === 'DEAL' || st.card === null) return null;
    const attributed = isAttributed(ctx);
    const base = {
      deck: 'roast' as const,
      loop: st.loop,
      loops: st.loops,
      attributed,
      prompt: { id: st.card.id, text: st.card.text },
    };
    if (st.sub === 'VOTE') {
      return {
        ...base,
        sub: 'VOTE',
        deadline: st.voteDeadline,
        eligible: votersOf(ctx).length,
        votedCount: Object.keys(st.votes).length,
        youVoted: st.votes[viewerId] ?? null,
      };
    }
    const res = st.resolutions[st.resolutions.length - 1];
    if (!res) return null; // unreachable by construction; never leak on a bug
    const reveal: RoastRevealView = {
      ...base,
      sub: 'REVEAL',
      victims: res.victims,
      doubleRoast: res.doubleRoast,
      roomHeat: res.roomHeat,
      voided: res.voided,
      youVoted: st.votes[viewerId] ?? null,
    };
    if (attributed) reveal.edges = res.displayEdges;
    else reveal.spread = res.spread;
    return reveal;
  },
} satisfies GameModule;
