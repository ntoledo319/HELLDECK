// Consent filter — DESCENT_BUILD_SPEC.md 4.4. Ceilings cap EXPOSURE, never CHAOS.
// LAW: no ceiling value, and no fact-of-filtering, is ever surfaced anywhere.
// This file only COMPUTES legality; redaction (server) keeps the values off the wire.
import type { Tier } from './types.js';

/** Second-lowest value; with a single value, that value. */
export function secondLowest(values: readonly number[]): number {
  if (values.length === 0) return 1;
  const sorted = [...values].sort((a, b) => a - b);
  return (sorted[1] ?? sorted[0]) as number;
}

/**
 * Room ceiling for generic (no-subject) cards.
 * N ≤ 4: min of all ceilings (small rooms have nowhere to hide).
 * N ≥ 5: second-lowest (one shy player caps cards ABOUT them, not the whole night).
 */
export function genericCeiling(ceilings: readonly Tier[]): Tier {
  if (ceilings.length === 0) return 1;
  if (ceilings.length <= 4) return Math.min(...ceilings) as Tier;
  return secondLowest(ceilings) as Tier;
}

export interface LegalityCtx {
  /** All active players' ceilings (imps excluded until converted). */
  ceilings: readonly Tier[];
  /** Ceilings of the players the card NAMES, if any. */
  subjectCeilings?: readonly Tier[];
  /** Vote-emergent-victim game (Roast Consensus class): anyone can end up the subject. */
  voteEmergent?: boolean;
}

/**
 * The 4.4 pseudocode, verbatim:
 *   card names subjects        → E ≤ min(ceiling of each named subject)
 *   vote-emergent and E > 3    → E ≤ min(all ceilings)
 *   otherwise                  → E ≤ genericCeiling
 */
export function cardLegal(exposure: Tier, ctx: LegalityCtx): boolean {
  const subjects = ctx.subjectCeilings ?? [];
  if (subjects.length > 0) return exposure <= Math.min(...subjects);
  if (ctx.voteEmergent === true && exposure > 3) return exposure <= Math.min(...ctx.ceilings);
  return exposure <= genericCeiling(ctx.ceilings);
}

/** Highest legal exposure tier for subjectless content in this room/game class. */
export function maxLegalE(ctx: Omit<LegalityCtx, 'subjectCeilings'>): Tier {
  for (let e = 5; e >= 2; e--) {
    if (cardLegal(e as Tier, ctx)) return e as Tier;
  }
  return 1;
}
