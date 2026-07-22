// The toll. Shown to the HOST when a second night is locked (server said NO_ENTITLEMENT).
// It is not an upgrade screen — it's the price of going back down. One payment, this phone,
// forever; framed as buying more damnation, never "premium features" (repo taste law).
import { useState } from 'preact/hooks';
import { beginCheckout, devUnlock } from '../entitle';
import { ModalOverlay, Sigil } from './bits';

type Status = 'idle' | 'opening' | 'dev' | 'sealing' | 'error';

export function Paywall({
  device,
  returnPath,
  onDismiss,
}: {
  device: string;
  returnPath: string;
  onDismiss: () => void;
}) {
  const [status, setStatus] = useState<Status>('idle');

  const pay = async (): Promise<void> => {
    setStatus('opening');
    const result = await beginCheckout(device, returnPath);
    if (result.kind === 'redirect') {
      location.href = result.url; // leave for Stripe; we come back to returnPath with ?session_id
      return;
    }
    // No live payment configured. In dev we still let the flow complete via the escape hatch.
    setStatus(result.kind === 'dev-unlock' ? 'dev' : 'error');
  };

  const takeDevUnlock = async (): Promise<void> => {
    setStatus('sealing');
    const ok = await devUnlock(device);
    // Reload clean so the socket reconnects carrying the freshly-minted unlock (host is entitled now).
    if (ok) location.replace(returnPath);
    else setStatus('error');
  };

  const busy = status === 'opening' || status === 'sealing';

  return (
    <ModalOverlay label="Pay the toll to descend again" className="paywall">
      <div class="paywall-toll">
        <Sigil />
      </div>
      <div class="overlay-title breathe">THE FIRST DESCENT WAS ON THE HOUSE</div>
      <p class="paywall-pitch">
        The pit doesn&rsquo;t run a tab. Pay the toll once and the trapdoor never locks again —
        <strong> every night, this phone, until you run out of friends.</strong>
      </p>
      <p class="paywall-terms">$9.99 · ONE TIME · NO ACCOUNT · NO MONTHLY TITHE · NO SECOND COLLECTION PLATE</p>

      {status === 'error' && (
        <p class="paywall-error" role="alert">
          THE GATE JAMMED. No coin left your pocket. Try the toll again.
        </p>
      )}

      {status === 'dev' ? (
        <button type="button" class="btn-blood big" onClick={() => void takeDevUnlock()}>
          UNLOCK (DEV — NO PAYMENT)
        </button>
      ) : (
        <button type="button" class="btn-blood big" disabled={busy} aria-busy={busy} onClick={() => void pay()}>
          {status === 'opening' ? 'OPENING THE GATE…' : status === 'sealing' ? 'SEALING THE PACT…' : 'PAY THE TOLL — $9.99'}
        </button>
      )}

      <button type="button" class="btn-ghost paywall-dismiss" disabled={busy} onClick={onDismiss}>
        NOT TONIGHT
      </button>
      <p class="paywall-fineprint">The night you already played is yours. This only buys the next descent.</p>
    </ModalOverlay>
  );
}
