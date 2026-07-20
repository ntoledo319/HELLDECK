// DEAL ceremony (spec 4.5): fixed-length ritual, identical for burned and clean deals.
// The roulette is pure theater — the pick already happened server-side.
import { useEffect, useState } from 'preact/hooks';
import type { Net } from '../net/ws';
import type { PlayerView } from '../view';
import { Ring } from './bits';

export function DealScreen({ players }: { players: PlayerView[] }) {
  const names = players.filter((p) => p.role !== 'imp').map((p) => p.name);
  const [i, setI] = useState(0);
  useEffect(() => {
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
  payload,
  net,
  onClose,
}: {
  payload: unknown;
  net: Net;
  onClose: () => void;
}) {
  const p = payload as { card?: { text?: string }; burnDeadline?: number };
  return (
    <div class="overlay preview">
      <div class="preview-tag">YOURS BEFORE THEIRS</div>
      <p class="preview-card">{p.card?.text ?? ''}</p>
      {typeof p.burnDeadline === 'number' && <Ring deadline={p.burnDeadline} now={() => net.serverNow()} />}
      <div class="preview-actions">
        <button
          class="btn-ghost"
          onClick={() => {
            net.send({ t: 'BURN', kind: 'card' });
            onClose();
          }}
        >
          BURN IT
        </button>
        <button class="btn-blood" onClick={onClose}>
          LET IT RIDE
        </button>
      </div>
      <p class="preview-note">Burning spends a brimstone. Nobody will ever know it happened.</p>
    </div>
  );
}
