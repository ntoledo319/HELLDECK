export const SPOTLIGHT_ROLES = [
  'subject',
  'confessor',
  'defender',
  'accused',
  'pitcher-a',
  'pitcher-b',
  'fighter-a',
  'fighter-b',
] as const;

export type SpotlightRole = (typeof SPOTLIGHT_ROLES)[number];

export interface SpotlightAssigned {
  status: 'assigned';
  ceremonyId: string;
  role: SpotlightRole;
  burnDeadline: number;
  announceAt: number;
  canBurn: boolean;
}

export interface SpotlightReleased {
  status: 'released';
  ceremonyId: string;
}

export type SpotlightPrivateMessage = SpotlightAssigned | SpotlightReleased;

export interface SpotlightClientState {
  id: number;
  assignment: SpotlightAssigned;
  burnPending: boolean;
  dismissed: boolean;
}

const roleSet = new Set<string>(SPOTLIGHT_ROLES);

function recordOf(value: unknown): Record<string, unknown> | null {
  return value !== null && typeof value === 'object' && !Array.isArray(value)
    ? (value as Record<string, unknown>)
    : null;
}

/** PRIVATE is an untrusted wire boundary; malformed role payloads never reach the screen. */
export function parseSpotlightMessage(value: unknown): SpotlightPrivateMessage | null {
  const p = recordOf(value);
  if (!p || typeof p['ceremonyId'] !== 'string' || p['ceremonyId'].length === 0) return null;

  if (p['status'] === 'released') {
    return { status: 'released', ceremonyId: p['ceremonyId'] };
  }
  if (
    p['status'] !== 'assigned' ||
    typeof p['role'] !== 'string' ||
    !roleSet.has(p['role']) ||
    typeof p['burnDeadline'] !== 'number' ||
    !Number.isFinite(p['burnDeadline']) ||
    typeof p['announceAt'] !== 'number' ||
    !Number.isFinite(p['announceAt']) ||
    p['announceAt'] < p['burnDeadline'] ||
    typeof p['canBurn'] !== 'boolean'
  ) {
    return null;
  }
  return {
    status: 'assigned',
    ceremonyId: p['ceremonyId'],
    role: p['role'] as SpotlightRole,
    burnDeadline: p['burnDeadline'],
    announceAt: p['announceAt'],
    canBurn: p['canBurn'],
  };
}

/** Apply server acknowledgement/assignment without reopening dismissed or pending ceremonies. */
export function receiveSpotlightMessage(
  state: SpotlightClientState | null,
  message: SpotlightPrivateMessage,
  eventId: number,
  now: number,
): SpotlightClientState | null {
  if (message.status === 'released') {
    return state?.assignment.ceremonyId === message.ceremonyId ? null : state;
  }
  if (message.burnDeadline <= now) {
    return state?.assignment.ceremonyId === message.ceremonyId ? null : state;
  }
  if (state?.assignment.ceremonyId === message.ceremonyId) {
    // Preserve the ceremony decision, but rotate the private event id so a flat
    // Stage requires a fresh lift before rendering any newly delivered payload.
    return { ...state, id: eventId, assignment: message };
  }
  return { id: eventId, assignment: message, burnPending: false, dismissed: false };
}

/** A burn is a request until PRIVATE released confirms this exact ceremony. */
export function requestSpotlightBurn(
  state: SpotlightClientState | null,
  ceremonyId: string,
  now: number,
): SpotlightClientState | null {
  if (
    !state ||
    state.assignment.ceremonyId !== ceremonyId ||
    state.dismissed ||
    state.burnPending ||
    !state.assignment.canBurn ||
    state.assignment.burnDeadline <= now
  ) {
    return state;
  }
  return { ...state, burnPending: true };
}

export function dismissSpotlight(
  state: SpotlightClientState | null,
  ceremonyId: string,
): SpotlightClientState | null {
  if (!state || state.assignment.ceremonyId !== ceremonyId || state.burnPending) return state;
  return { ...state, dismissed: true };
}

export function expireSpotlight(
  state: SpotlightClientState | null,
  ceremonyId: string,
): SpotlightClientState | null {
  return state?.assignment.ceremonyId === ceremonyId ? null : state;
}

const ROLE_TITLES: Record<SpotlightRole, string> = {
  subject: "YOU'RE THE SUBJECT",
  confessor: "YOU'RE THE CONFESSOR",
  defender: "YOU'RE THE DEFENDER",
  accused: "YOU'RE THE ACCUSED",
  'pitcher-a': "YOU'RE PITCHER A",
  'pitcher-b': "YOU'RE PITCHER B",
  'fighter-a': "YOU'RE FIGHTER A",
  'fighter-b': "YOU'RE FIGHTER B",
};

export function spotlightRoleTitle(role: SpotlightRole): string {
  return ROLE_TITLES[role];
}

export function spotlightBurnCopy(
  canBurn: boolean,
  pending: boolean,
): { button: string; note: string } {
  if (pending) {
    return { button: 'BURN PENDING…', note: 'WAITING FOR THE PIT TO RELEASE YOU…' };
  }
  if (canBurn) {
    return {
      button: 'SPEND BRIMSTONE',
      note: 'Burning asks the pit to move the spotlight. Stay here until it confirms.',
    };
  }
  return {
    button: 'BURN UNAVAILABLE',
    note: 'This assignment is locked. The room stays blind until the ceremony ends.',
  };
}
