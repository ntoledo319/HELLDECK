import { useEffect, useRef } from 'preact/hooks';
import type { Net } from '../net/ws';
import { ModalOverlay, Ring } from './bits';
import { spotlightBurnCopy, spotlightRoleTitle, type SpotlightAssigned } from './spotlight.logic';

export function SpotlightOverlay({
  assignment,
  burnPending,
  net,
  onBurn,
  onDismiss,
}: {
  assignment: SpotlightAssigned;
  burnPending: boolean;
  net: Net;
  onBurn: (at: number) => boolean;
  onDismiss: () => void;
}) {
  const burnSentRef = useRef(false);
  useEffect(() => {
    if (!burnPending) burnSentRef.current = false;
  }, [burnPending]);
  const copy = spotlightBurnCopy(assignment.canBurn, burnPending);
  const requestBurn = (): void => {
    const now = net.serverNow();
    if (burnSentRef.current || burnPending || !assignment.canBurn || now >= assignment.burnDeadline) {
      return;
    }
    if (onBurn(now)) burnSentRef.current = true;
  };

  return (
    <ModalOverlay label="Private spotlight assignment" className="preview">
      <div class="preview-tag">THE PIT PICKED YOU</div>
      <p id="spotlight-role-title" class="preview-card">
        {spotlightRoleTitle(assignment.role)}
      </p>
      <Ring deadline={assignment.burnDeadline} now={() => net.serverNow()} />
      <div class="preview-actions">
        <button class="btn-ghost" disabled={!assignment.canBurn || burnPending} onClick={requestBurn}>
          {copy.button}
        </button>
        <button class="btn-blood" disabled={burnPending} onClick={onDismiss}>
          LET IT RIDE
        </button>
      </div>
      <p class="preview-note" role="status" aria-live="polite">
        {copy.note}
      </p>
    </ModalOverlay>
  );
}
