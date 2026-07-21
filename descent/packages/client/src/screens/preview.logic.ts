export interface PreviewCard {
  id: string;
  text: string;
}

export interface PreviewAssigned {
  status: 'assigned';
  previewId: string;
  card: PreviewCard;
  burnDeadline: number;
  revealAt: number;
  canBurn: boolean;
}

export interface PreviewReleased {
  status: 'released';
  previewId: string;
}

export type PreviewPrivateMessage = PreviewAssigned | PreviewReleased;

export interface PreviewClientState {
  id: number;
  assignment: PreviewAssigned;
  burnPending: boolean;
  dismissed: boolean;
}

function recordOf(value: unknown): Record<string, unknown> | null {
  return value !== null && typeof value === 'object' && !Array.isArray(value)
    ? (value as Record<string, unknown>)
    : null;
}

/** PRIVATE is an untrusted boundary. Only render a complete, correlated preview. */
export function parsePreviewMessage(value: unknown): PreviewPrivateMessage | null {
  const p = recordOf(value);
  if (!p || typeof p['previewId'] !== 'string' || p['previewId'].length === 0) return null;

  if (p['status'] === 'released') {
    return { status: 'released', previewId: p['previewId'] };
  }

  const card = recordOf(p['card']);
  if (
    p['status'] !== 'assigned' ||
    !card ||
    typeof card['id'] !== 'string' ||
    card['id'].length === 0 ||
    typeof card['text'] !== 'string' ||
    card['text'].length === 0 ||
    typeof p['burnDeadline'] !== 'number' ||
    !Number.isFinite(p['burnDeadline']) ||
    typeof p['revealAt'] !== 'number' ||
    !Number.isFinite(p['revealAt']) ||
    p['revealAt'] < p['burnDeadline'] ||
    typeof p['canBurn'] !== 'boolean'
  ) {
    return null;
  }

  return {
    status: 'assigned',
    previewId: p['previewId'],
    card: { id: card['id'], text: card['text'] },
    burnDeadline: p['burnDeadline'],
    revealAt: p['revealAt'],
    canBurn: p['canBurn'],
  };
}

/** Apply assignment/release messages without reopening a locally dismissed preview. */
export function receivePreviewMessage(
  state: PreviewClientState | null,
  message: PreviewPrivateMessage,
  eventId: number,
  now: number,
): PreviewClientState | null {
  if (message.status === 'released') {
    return state?.assignment.previewId === message.previewId ? null : state;
  }
  if (message.burnDeadline <= now) {
    return state?.assignment.previewId === message.previewId ? null : state;
  }
  if (state?.assignment.previewId === message.previewId) {
    return { ...state, id: eventId, assignment: message };
  }
  return { id: eventId, assignment: message, burnPending: false, dismissed: false };
}

/** A burn stays pending until the server releases this exact preview. */
export function requestPreviewBurn(
  state: PreviewClientState | null,
  previewId: string,
  now: number,
): PreviewClientState | null {
  if (
    !state ||
    state.assignment.previewId !== previewId ||
    state.dismissed ||
    state.burnPending ||
    !state.assignment.canBurn ||
    state.assignment.burnDeadline <= now
  ) {
    return state;
  }
  return { ...state, burnPending: true };
}

export function rejectPreviewBurn(state: PreviewClientState | null): PreviewClientState | null {
  return state?.burnPending ? { ...state, burnPending: false } : state;
}

/**
 * A fresh socket has no pending writes. Drop visible/pending assignments and let
 * reconnect replay rebuild only a still-live server assignment. A locally dismissed
 * curtain remains hidden if that same live assignment is replayed.
 */
export function preparePreviewReconnect(state: PreviewClientState | null): PreviewClientState | null {
  return state?.dismissed ? state : null;
}

export function dismissPreview(
  state: PreviewClientState | null,
  previewId: string,
): PreviewClientState | null {
  if (!state || state.assignment.previewId !== previewId || state.burnPending) return state;
  return { ...state, dismissed: true };
}

export function expirePreview(
  state: PreviewClientState | null,
  previewId: string,
): PreviewClientState | null {
  return state?.assignment.previewId === previewId ? null : state;
}
