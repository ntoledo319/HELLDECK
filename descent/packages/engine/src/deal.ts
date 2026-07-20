// Deal ceremony + burn absorption — DESCENT_BUILD_SPEC.md 4.5.
// LAW: burned and clean deals are timing-identical. A burn changes ZERO wire effects,
// ZERO schedules, ZERO animations. The backup is reserved BEFORE the ceremony starts
// so the swap costs nothing observable. (Test asserts effect timelines byte-equal.)
import type { DealRequest, DealState, Effect, Player, PlayerId } from './types.js';

/** Fixed ritual length, no named subject ("THE DECK IS CHOOSING ITS VICTIM…"). */
export const CEREMONY_MS = 5500;
/** Subject pre-view + burn window. Never make a safety valve faster than a sip of beer. */
export const PREVIEW_MS = 10_000;

/**
 * Start a deal. Both cards are already picked and reserved by the caller (module).
 * If the card names a subject, the ceremony stretches to 10s and the subject gets a
 * PRIVATE preview (SEND effect) with the burn deadline. The room sees only the ritual.
 */
export function beginDeal(
  req: DealRequest,
  timerId: string,
  now: number,
): { deal: DealState; effects: Effect[] } {
  const hasSubject = req.subjectId !== null;
  const completesAt = now + (hasSubject ? PREVIEW_MS : CEREMONY_MS);
  const deal: DealState = {
    card: req.primary,
    backup: req.backup,
    burnedId: null,
    subjectId: req.subjectId,
    startedAt: now,
    burnWindowEndsAt: hasSubject ? now + PREVIEW_MS : now,
    completesAt,
    timerId,
    done: false,
  };
  const effects: Effect[] = [{ k: 'SCHEDULE', timerId, atMs: completesAt }];
  if (hasSubject && req.subjectId) {
    effects.push({
      k: 'SEND',
      to: req.subjectId,
      kind: 'preview',
      payload: { card: req.primary, burnDeadline: deal.burnWindowEndsAt },
    });
  }
  return { deal, effects };
}

/**
 * Subject burns the card: silent swap to the reserved backup. Completion time is
 * untouched ("same ceremony length" — 4.5); the burned id is quarantined for the night.
 * Returns burned=false for every illegal attempt (wrong player, window closed, no backup)
 * — the caller emits nothing either way, so failed attempts leave no trace too.
 */
export function burnDeal(
  deal: DealState,
  playerId: PlayerId,
  now: number,
): { deal: DealState; burned: boolean } {
  if (deal.done || deal.backup === null) return { deal, burned: false };
  if (deal.subjectId !== playerId) return { deal, burned: false };
  if (now > deal.burnWindowEndsAt) return { deal, burned: false };
  return {
    deal: { ...deal, card: deal.backup, backup: null, burnedId: deal.card.id },
    burned: true,
  };
}

/**
 * Ceremony timer fired: the current card goes public. Reports which ids are now
 * consumed for night-scope dedup (the dealt card, plus the vetoed one if any —
 * a burned card must never resurface tonight; an unused backup returns to the pool).
 */
export function completeDeal(deal: DealState): { deal: DealState; usedCardIds: string[] } {
  const used = deal.burnedId ? [deal.card.id, deal.burnedId] : [deal.card.id];
  return { deal: { ...deal, done: true }, usedCardIds: used };
}

/**
 * Spotlight fairness pick (4.5): random assignment weighted toward the lowest
 * spotlightCount, so the same loudmouth doesn't eat every spotlight.
 */
export function pickSpotlight(players: readonly Player[], rand: () => number): Player {
  if (players.length === 0) throw new Error('pickSpotlight from empty');
  const max = Math.max(...players.map((p) => p.spotlightCount));
  const weights = players.map((p) => max - p.spotlightCount + 1);
  const total = weights.reduce((a, b) => a + b, 0);
  let roll = rand() * total;
  for (let i = 0; i < players.length; i++) {
    roll -= weights[i] as number;
    if (roll < 0) return players[i] as Player;
  }
  return players[players.length - 1] as Player;
}

/**
 * Volunteer-first spotlight pick (4.5 "WHO WANTS BLOOD?"): if any eligible player claimed
 * the spotlight this circle, choose among the claimants (still lowest-spotlightCount-weighted
 * so a serial volunteer can't hog every turn); otherwise fall back to the weighted-random pick.
 * A claim from someone not eligible for THIS assignment (an imp, or an already-used seat) is
 * simply ignored — it never forces or skips anyone.
 */
export function pickSpotlightPreferring(
  eligible: readonly Player[],
  volunteers: readonly Player[],
  rand: () => number,
): Player {
  const ids = new Set(volunteers.map((v) => v.id));
  const claimed = eligible.filter((p) => ids.has(p.id));
  return pickSpotlight(claimed.length > 0 ? claimed : eligible, rand);
}
