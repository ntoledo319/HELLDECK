// Scoring — DESCENT_BUILD_SPEC.md 4.6, the complete table as data.
// LAW (from playtests): no points for aligning with a majority except Roast.
// Performers, writers, liars, and subjects earn; spectators tread water.
import type { RoomState, Tier } from './types.js';

/**
 * Flat rows of the 4.6 table: event id -> base points (pre-multiplier).
 * Per-unit rows (alibi words) multiply by a count via points(); the two formula
 * rows (reader fires, damage meter) have dedicated functions below.
 */
export const SCORE_TABLE = {
  'fillin.win': 3, // your punchline wins the vote
  'fillin.panicWin': 1, // panic-button answer wins (half of +3, rounded down)
  'roast.pluralityVote': 2, // you voted the plurality victim (the ONE majority-alignment game)
  'roast.roomHeat': 1, // extra to plurality voters when the room hits the heat threshold
  'overunder.correctBet': 2,
  'overunder.push': 1, // exact-line push: everyone +1
  'confession.fooled': 3, // jury majority wrong -> confessor
  'confession.correctVote': 1, // your believe/cap call was right
  'poison.pitchWin': 3,
  'poison.predictExact': 2, // N=3 damage meter: predict-own-score exact
  'redflag.smash': 3, // SMASH verdict — the defense survived
  'alibi.plantFound': 1, // jury member, per planted word found
  'alibi.wordMissed': 1, // accused, per word MISSED by >50% of jury
  'scatter.survive': 1, // not holding at boom
  'scatter.holder': 0, // the corpse
  'texttrap.survived': 3,
  'reality.egoGap': 2, // subject, ego-gap <= 1
  'reality.calledIt': 1, // every debater, gap >= 4
  participation: 1, // any vote cast at all — max once per circle (module-enforced)
  fifth: 0, // PLEAD THE FIFTH / voided round: 0 to all for that loop
} as const satisfies Record<string, number>;

export type ScoreEventId = keyof typeof SCORE_TABLE;

/** Base points for a table row; `count` multiplies per-unit rows (alibi words). */
export function points(id: ScoreEventId, count = 1): number {
  return SCORE_TABLE[id] * count;
}

/** Finale Fill-In Reader: +1 per 10 fires during the read, cap +3. */
export function readerFirePoints(fires: number): number {
  return Math.min(3, Math.floor(Math.max(0, fires) / 10));
}

/** Poison Pitch N=3 damage meter: score = the judge's rating. */
export function damageMeterPoints(rating: Tier): number {
  return rating;
}

/**
 * Roast Room Heat threshold (4.6): unanimity-minus-victim at N<=5; >=60% at N>=8;
 * >=80% else (N 6-7). `votesForVictim` / `votesCast` are weighted counts (imps 0.5).
 */
export function roomHeatHit(votesForVictim: number, votesCast: number, nPlayers: number): boolean {
  if (votesCast <= 0) return false;
  if (nPlayers <= 5) return votesForVictim >= votesCast; // unanimity minus the victim
  const needed = nPlayers >= 8 ? 0.6 : 0.8;
  return votesForVictim / votesCast >= needed;
}

/**
 * Multiplier law: Devil's Bargain x2 stacks before finale x3 — same circle is
 * impossible by construction (the bargain never lands on the finale), but the
 * order is spec'd so we keep it.
 */
export function applyMultipliers(base: number, opts: { finale: boolean; bargain: boolean }): number {
  return base * (opts.bargain ? 2 : 1) * (opts.finale ? 3 : 1);
}

/**
 * Apply a module's pre-multiplier score sheet to the room. The core calls this on
 * every GameStep; modules never touch Player.score directly.
 */
export function awardScores(
  s: RoomState,
  scores: Record<string, number>,
  circleIdx: number,
): RoomState {
  const spec = s.arc[circleIdx];
  const finale = spec?.finale ?? false;
  const players = s.players.map((p) => {
    const base = scores[p.id];
    if (base === undefined || base === 0) return p;
    const bargain =
      s.devilsBargain !== null &&
      s.devilsBargain.holder === p.id &&
      s.devilsBargain.circle === circleIdx;
    return { ...p, score: p.score + applyMultipliers(base, { finale, bargain }) };
  });
  return { ...s, players };
}
