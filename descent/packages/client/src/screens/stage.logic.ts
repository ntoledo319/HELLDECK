export type StageGate = 'passthrough' | 'flat' | 'lifted';

function recordOf(value: unknown): Record<string, unknown> | null {
  return value !== null && typeof value === 'object' && !Array.isArray(value)
    ? (value as Record<string, unknown>)
    : null;
}

/** Viewer-private acknowledgements that mean a lifted phone has finished its decision. */
export function stageDecisionSignature(gameView: unknown): string {
  const view = recordOf(gameView);
  if (!view) return '';
  const decision: Record<string, unknown> = {};
  for (const key of ['youVoted', 'youBet', 'youTapped', 'youPicked', 'youRated', 'youPitVoted']) {
    if (Object.prototype.hasOwnProperty.call(view, key)) decision[key] = view[key];
  }
  const you = recordOf(view['you']);
  if (you) {
    for (const key of ['yourAnswer', 'yourAnswerPanic', 'yourTone']) {
      if (Object.prototype.hasOwnProperty.call(you, key)) decision[key] = you[key];
    }
  }
  return JSON.stringify(decision);
}

/** A lift is valid for one exact privacy context. New phases, subs, circles, or
 * private payloads get a different key and are flat before they can render. */
export function stagePrivacyKey(
  phase: string,
  circleIdx: number,
  sub: string | null,
  privateOverlayId: number | null,
  enabled: boolean,
  decisionSignature = '',
  connectionGeneration = 0,
): string {
  return JSON.stringify([
    enabled,
    circleIdx,
    phase,
    sub,
    privateOverlayId,
    decisionSignature,
    connectionGeneration,
  ]);
}

/** Never resurrect an old lift when the room later returns to the same-looking context. */
export function stageLiftForContext(
  liftedFor: string | null,
  previousPrivacyKey: string,
  currentPrivacyKey: string,
): string | null {
  return previousPrivacyKey === currentPrivacyKey ? liftedFor : null;
}

export function stageGate(
  enabled: boolean,
  phase: string,
  privateOverlayId: number | null,
  liftedFor: string | null,
  privacyKey: string,
): StageGate {
  if (!enabled) return 'passthrough';
  const protectsPrivateInput = phase === 'INPUT' || phase === 'WAITING_ON';
  if (!protectsPrivateInput && privateOverlayId === null) return 'passthrough';
  return liftedFor === privacyKey ? 'lifted' : 'flat';
}
