// DEAL ceremony (spec 4.5): fixed-length ritual, identical for burned and clean deals.
// The roulette is pure theater — the pick already happened server-side.
import { useEffect, useRef, useState } from 'preact/hooks';
import type { Net } from '../net/ws';
import type { PlayerView } from '../view';
import { ModalOverlay, Ring } from './bits';
import type { PreviewAssigned } from './preview.logic';

export function DealScreen({ players }: { players: PlayerView[] }) {
  const names = players.filter((p) => p.role !== 'imp').map((p) => p.name);
  const [i, setI] = useState(0);
  useEffect(() => {
    if (window.matchMedia('(prefers-reduced-motion: reduce)').matches) return undefined;
    const t = setInterval(() => setI((x) => x + 1), 110); // cosmetic shuffle, not a countdown
    return () => clearInterval(t);
  }, []);
  return (
    <main class="screen deal">
      <div class="deal-label breathe">THE DECK IS CHOOSING ITS VICTIM…</div>
      <div class="deal-roulette">{names.length > 0 ? names[i % names.length] : '…'}</div>
    </main>
  );
}

/**
 * Subject pre-view (spec 4.5): the card lands here >=10s before the room sees it.
 * BURN swaps to the backup with zero trace; LET IT RIDE just closes the curtain.
 */
export function PreviewOverlay({
  assignment,
  burnPending,
  net,
  onBurn,
  onDismiss,
}: {
  assignment: PreviewAssigned;
  burnPending: boolean;
  net: Net;
  onBurn: (now: number) => boolean;
  onDismiss: () => void;
}) {
  const burnSentRef = useRef(false);
  useEffect(() => {
    if (!burnPending) burnSentRef.current = false;
  }, [burnPending]);
  const requestBurn = (): void => {
    const now = net.serverNow();
    if (
      burnSentRef.current ||
      burnPending ||
      !assignment.canBurn ||
      now >= assignment.burnDeadline
    ) {
      return;
    }
    if (onBurn(now)) burnSentRef.current = true;
  };
  return (
    <ModalOverlay label="Private card preview" className="preview">
      <div class="preview-tag">YOURS BEFORE THEIRS</div>
      <p class="preview-card">{assignment.card.text}</p>
      <Ring deadline={assignment.burnDeadline} now={() => net.serverNow()} />
      <div class="preview-actions">
        <button
          type="button"
          class="btn-ghost"
          disabled={!assignment.canBurn || burnPending}
          aria-busy={burnPending}
          onClick={requestBurn}
        >
          {burnPending ? 'BURN PENDING…' : assignment.canBurn ? 'BURN IT' : 'BURN UNAVAILABLE'}
        </button>
        <button type="button" class="btn-blood" disabled={burnPending} onClick={onDismiss}>
          LET IT RIDE
        </button>
      </div>
      <p class="preview-note" role="status" aria-live="polite">
        {burnPending
          ? 'WAITING FOR THE PIT TO CONFIRM THE BURN…'
          : assignment.canBurn
            ? 'Burning spends a brimstone. Nobody will ever know it happened.'
            : 'No burn is available. The room stays blind until the ceremony ends.'}
      </p>
    </ModalOverlay>
  );
}
