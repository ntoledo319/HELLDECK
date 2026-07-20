// Private spotlight assignment ceremony — DESCENT_BUILD_SPEC.md 4.5 / D-134.
//
// Timing law: every run schedules BOTH fixed windows up front. Primary assignees
// get T+0..T+10 to dodge. Burned slots are refilled only at the always-present
// T+10 handoff, giving replacements a full T+10..T+20 private window. At T+20
// assignments lock. A replacement who burns leaves a null slot; consent beats
// inventing a third, timing-revealing window.
import { pickSpotlight } from './deal.js';
import type {
  Effect,
  Player,
  PlayerId,
  SpotlightAssignedPayload,
  SpotlightRequest,
  SpotlightResolution,
  SpotlightSlotState,
  SpotlightState,
} from './types.js';

export const SPOTLIGHT_WINDOW_MS = 10_000;
export const SPOTLIGHT_TOTAL_MS = 2 * SPOTLIGHT_WINDOW_MS;

export const spotlightHandoffTimerId = (key: string): string => `spotlight:handoff:${key}`;
export const spotlightCompletionTimerId = (key: string): string => `spotlight:complete:${key}`;

/** Volunteer-first, fairness-weighted deterministic permutation of eligible players. */
export function orderSpotlightCandidates(
  eligible: readonly Player[],
  volunteers: readonly Player[],
  rand: () => number,
): Player[] {
  const volunteerIds = new Set(volunteers.map((p) => p.id));
  const remaining = [...eligible];
  const ordered: Player[] = [];
  while (remaining.length > 0) {
    const claimed = remaining.filter((p) => volunteerIds.has(p.id));
    const chosen = pickSpotlight(claimed.length > 0 ? claimed : remaining, rand);
    ordered.push(chosen);
    const i = remaining.findIndex((p) => p.id === chosen.id);
    if (i >= 0) remaining.splice(i, 1);
  }
  return ordered;
}

function assignmentEffect(
  state: SpotlightState,
  slot: SpotlightSlotState,
  burnDeadline: number,
): Effect | null {
  if (slot.playerId === null) return null;
  const payload: SpotlightAssignedPayload = {
    status: 'assigned',
    ceremonyId: state.ceremonyId,
    role: slot.role,
    burnDeadline,
    announceAt: state.completesAt,
    canBurn: slot.burnable,
  };
  return {
    k: 'SEND',
    to: slot.playerId,
    kind: 'spotlight',
    payload,
  };
}

/** Begin the fixed two-window ceremony and privately notify primary assignees. */
export function beginSpotlight(
  req: SpotlightRequest,
  activePlayers: readonly Player[],
  volunteers: readonly Player[],
  rand: () => number,
  key: string,
  now: number,
): { spotlight: SpotlightState; effects: Effect[] } {
  const roles = req.roles.filter((role, i, all) => all.indexOf(role) === i).slice(0, 2);
  const requested = new Set(req.eligibleIds);
  const uniqueEligible = activePlayers.filter(
    (p, i, all) =>
      p.role !== 'imp' && requested.has(p.id) && all.findIndex((q) => q.id === p.id) === i,
  );
  // Never initially assign an offline phone: it cannot receive the private notice.
  // If too few connected bodies remain, null slots are safer than conscription.
  const pool = uniqueEligible.filter((p) => p.connected);
  const ordered = orderSpotlightCandidates(pool, volunteers, rand);
  const assigned = ordered.slice(0, roles.length);
  const ceremonyId = `spotlight:${key}`;
  const handoffAt = now + SPOTLIGHT_WINDOW_MS;
  const completesAt = now + SPOTLIGHT_TOTAL_MS;
  const playerById = new Map(activePlayers.map((p) => [p.id, p]));
  const slots: SpotlightSlotState[] = roles.map((role, i) => {
    const playerId = assigned[i]?.id ?? null;
    return {
      role,
      playerId,
      burnable: playerId !== null && (playerById.get(playerId)?.brimstones ?? 0) > 0,
    };
  });
  const spotlight: SpotlightState = {
    ceremonyId,
    slots,
    reserveIds: ordered.slice(roles.length).map((p) => p.id),
    declinedIds: [],
    window: 'primary',
    startedAt: now,
    handoffAt,
    completesAt,
    handoffTimerId: spotlightHandoffTimerId(key),
    completionTimerId: spotlightCompletionTimerId(key),
  };
  const effects: Effect[] = [
    { k: 'SCHEDULE', timerId: spotlight.handoffTimerId, atMs: handoffAt },
    { k: 'SCHEDULE', timerId: spotlight.completionTimerId, atMs: completesAt },
  ];
  for (const slot of slots) {
    const ef = assignmentEffect(spotlight, slot, handoffAt);
    if (ef) effects.push(ef);
  }
  return { spotlight, effects };
}

/**
 * Accept a dodge in the assignee's current private window. No public effect is
 * emitted and neither fixed timer moves. The PRIVATE acknowledgement is important:
 * clients must not optimistically claim a late/invalid safety action succeeded.
 */
export function burnSpotlight(
  spotlight: SpotlightState,
  playerId: PlayerId,
  now: number,
): { spotlight: SpotlightState; effects: Effect[]; burned: boolean } {
  if (spotlight.window === 'done') return { spotlight, effects: [], burned: false };
  const deadline = spotlight.window === 'primary' ? spotlight.handoffAt : spotlight.completesAt;
  if (now > deadline) return { spotlight, effects: [], burned: false };
  const index = spotlight.slots.findIndex(
    (slot) => slot.playerId === playerId && slot.burnable,
  );
  if (index < 0) return { spotlight, effects: [], burned: false };
  const slots = spotlight.slots.map((slot, i) =>
    i === index ? { ...slot, playerId: null, burnable: false } : slot,
  );
  const next: SpotlightState = {
    ...spotlight,
    slots,
    declinedIds: spotlight.declinedIds.includes(playerId)
      ? spotlight.declinedIds
      : [...spotlight.declinedIds, playerId],
  };
  return {
    spotlight: next,
    burned: true,
    effects: [
      {
        k: 'SEND',
        to: playerId,
        kind: 'spotlight',
        payload: { status: 'released', ceremonyId: spotlight.ceremonyId },
      },
    ],
  };
}

/** Fixed T+10 transition: lock kept primaries and privately fill burned slots. */
export function handoffSpotlight(
  spotlight: SpotlightState,
  activePlayers: readonly Player[],
): { spotlight: SpotlightState; effects: Effect[] } {
  if (spotlight.window !== 'primary') return { spotlight, effects: [] };
  const occupied = new Set(
    spotlight.slots.flatMap((slot) => (slot.playerId === null ? [] : [slot.playerId])),
  );
  const declined = new Set(spotlight.declinedIds);
  const playerById = new Map(activePlayers.map((p) => [p.id, p]));
  const reserves = spotlight.reserveIds.filter(
    (id) => !occupied.has(id) && !declined.has(id) && playerById.get(id)?.connected === true,
  );
  let cursor = 0;
  const replacementSlots = new Set<number>();
  const slots = spotlight.slots.map((slot, index): SpotlightSlotState => {
    if (slot.playerId !== null) return { ...slot, burnable: false }; // primary locked
    const playerId = reserves[cursor++] ?? null;
    if (playerId !== null) replacementSlots.add(index);
    return {
      ...slot,
      playerId,
      burnable: playerId !== null && (playerById.get(playerId)?.brimstones ?? 0) > 0,
    };
  });
  const used = new Set(reserves.slice(0, cursor));
  const next: SpotlightState = {
    ...spotlight,
    slots,
    reserveIds: spotlight.reserveIds.filter((id) => !used.has(id)),
    window: 'replacement',
  };
  const effects: Effect[] = [];
  for (const [index, slot] of slots.entries()) {
    // Kept primaries were already notified at T+0. Every newly filled slot receives
    // its role notice, including a player with zero Brimstones (`canBurn:false`).
    if (!replacementSlots.has(index)) continue;
    const ef = assignmentEffect(next, slot, next.completesAt);
    if (ef) effects.push(ef);
  }
  return { spotlight: next, effects };
}

/** Fixed T+20 lock. Final ids (including explicit null slots) feed the module callback. */
export function completeSpotlight(spotlight: SpotlightState): SpotlightState {
  if (spotlight.window !== 'replacement') return spotlight;
  return {
    ...spotlight,
    slots: spotlight.slots.map((slot) => ({ ...slot, burnable: false })),
    window: 'done',
  };
}

export function spotlightResolution(spotlight: SpotlightState): SpotlightResolution {
  return {
    assignments: spotlight.slots.map(({ role, playerId }) => ({ role, playerId })),
  };
}

/**
 * Rebuild the currently relevant PRIVATE assignment after reconnect/RESYNC. The
 * server sends this only to `viewerId`, after STATE. Released/expired assignments
 * intentionally return null; no historical primary or burn attribution is replayed.
 */
export function spotlightPrivateFor(
  spotlight: SpotlightState | null,
  viewerId: PlayerId,
  now: number,
): SpotlightAssignedPayload | null {
  if (!spotlight || spotlight.window === 'done' || now > spotlight.completesAt) return null;
  if (spotlight.window === 'primary' && now > spotlight.handoffAt) return null;
  const slot = spotlight.slots.find((candidate) => candidate.playerId === viewerId);
  if (!slot) return null;
  return {
    status: 'assigned',
    ceremonyId: spotlight.ceremonyId,
    role: slot.role,
    burnDeadline: spotlight.window === 'primary' ? spotlight.handoffAt : spotlight.completesAt,
    announceAt: spotlight.completesAt,
    canBurn: slot.burnable,
  };
}
