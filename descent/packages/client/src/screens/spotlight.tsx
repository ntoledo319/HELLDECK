import { useRef } from 'preact/hooks';
import type { Net } from '../net/ws';
import { Ring } from './bits';
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
  onBurn: (at: number) => void;
  onDismiss: () => void;
}) {
  const burnSentRef = useRef(false);
  const copy = spotlightBurnCopy(assignment.canBurn, burnPending);
  const requestBurn = (): void => {
    const now = net.serverNow();
    if (burnSentRef.current || burnPending || !assignment.canBurn || now >= assignment.burnDeadline) {
      return;
    }
    burnSentRef.current = true;
    onBurn(now);
  };

  return (
    <div class="overlay preview" role="dialog" aria-modal="true" aria-labelledby="spotlight-role-title">
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
    </div>
  );
}
