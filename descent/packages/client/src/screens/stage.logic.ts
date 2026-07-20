export type StageGate = 'passthrough' | 'flat' | 'lifted';

/** A lift is valid for one exact privacy context. New phases, subs, circles, or
 * private payloads get a different key and are flat before they can render. */
export function stagePrivacyKey(
  phase: string,
  circleIdx: number,
  sub: string | null,
  privateOverlayId: number | null,
  enabled: boolean,
): string {
  return JSON.stringify([enabled, circleIdx, phase, sub, privateOverlayId]);
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
