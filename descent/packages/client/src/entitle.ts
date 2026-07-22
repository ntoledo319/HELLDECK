// Client side of the $9.99 host unlock (spec Part 11 / D-412). The device token and the
// paid unlock token both live in localStorage so a purchase survives a browser restart with
// no account. The device token is never shown and only rides the host's own WS/API calls.
//
// Pure helpers (parseUnlockReturn, validDeviceToken, paywall copy) are exported for the unit
// tests in entitle.test.ts; the fetch/localStorage wrappers below are the thin IO edge.

const DEVICE_KEY = 'hd:device';
const UNLOCK_KEY = 'hd:unlock';
const DEVICE_RE = /^[a-z0-9]{16,64}$/;

export function validDeviceToken(s: string | null | undefined): s is string {
  return typeof s === 'string' && DEVICE_RE.test(s);
}

/** This phone's stable id — minted once, reused forever. Identifies the paying device. */
export function deviceToken(): string {
  let t = localStorage.getItem(DEVICE_KEY);
  if (!validDeviceToken(t)) {
    t = crypto.randomUUID().replaceAll('-', '');
    localStorage.setItem(DEVICE_KEY, t);
  }
  return t;
}

export function unlockToken(): string | null {
  const t = localStorage.getItem(UNLOCK_KEY);
  return t && t.length > 0 ? t : null;
}

export function storeUnlock(token: string): void {
  localStorage.setItem(UNLOCK_KEY, token);
}

// ===== the Stripe round-trip (pure parse) =====

export type UnlockReturn =
  | { kind: 'verify'; sessionId: string; dev: string }
  | { kind: 'cancel' }
  | { kind: 'none' };

/**
 * Read the query Stripe appended to the room URL on the way back. `?session_id=…&dev=…` means
 * the payer returned and we must verify + mint the token; `?unlock=cancel` means they backed
 * out. Everything else is a normal load. Pure so the routing is unit-tested.
 */
export function parseUnlockReturn(search: string): UnlockReturn {
  const q = new URLSearchParams(search);
  if (q.get('unlock') === 'cancel') return { kind: 'cancel' };
  const sessionId = q.get('session_id');
  const dev = q.get('dev');
  if (sessionId && validDeviceToken(dev)) return { kind: 'verify', sessionId, dev };
  return { kind: 'none' };
}

// ===== IO edge (checkout / verify / dev-unlock) =====

export type CheckoutResult =
  | { kind: 'redirect'; url: string }
  | { kind: 'dev-unlock' } // no Stripe key, but non-prod: offer the escape hatch
  | { kind: 'error' };

/** Open a Checkout Session for this device, returning the host to `returnPath` when paid. */
export async function beginCheckout(device: string, returnPath: string): Promise<CheckoutResult> {
  try {
    const res = await fetch('/api/entitle/checkout', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ device, returnPath }),
    });
    if (res.ok) {
      const body = (await res.json()) as { url?: string };
      return typeof body.url === 'string' ? { kind: 'redirect', url: body.url } : { kind: 'error' };
    }
    const body = (await res.json().catch(() => ({}))) as { devUnlock?: boolean };
    return body.devUnlock ? { kind: 'dev-unlock' } : { kind: 'error' };
  } catch {
    return { kind: 'error' };
  }
}

/** After payment: prove the session paid for THIS device and store the returned unlock token. */
export async function verifyAndStore(sessionId: string, device: string): Promise<boolean> {
  try {
    const res = await fetch(`/api/entitle/verify?session_id=${encodeURIComponent(sessionId)}&dev=${encodeURIComponent(device)}`);
    if (!res.ok) return false;
    const body = (await res.json()) as { unlock?: string };
    if (typeof body.unlock !== 'string') return false;
    storeUnlock(body.unlock);
    return true;
  } catch {
    return false;
  }
}

/** Non-prod only: mint an unlock token with no payment so the flow is testable without keys. */
export async function devUnlock(device: string): Promise<boolean> {
  try {
    const res = await fetch(`/api/entitle/dev-unlock?dev=${encodeURIComponent(device)}`, { method: 'POST' });
    if (!res.ok) return false;
    const body = (await res.json()) as { unlock?: string };
    if (typeof body.unlock !== 'string') return false;
    storeUnlock(body.unlock);
    return true;
  } catch {
    return false;
  }
}
