// The thinnest possible Stripe adapter — just the two calls the $9.99 unlock needs, over
// fetch to the REST API. Test mode and live mode are the same API with a different key, so
// nothing here changes when the owner swaps sk_test_ for sk_live_ (D-412; live key is
// owner-gated). No SDK dependency: this is a phones-only game on a Worker, not a store.

const API = 'https://api.stripe.com/v1';

function form(fields: Record<string, string>): string {
  return Object.entries(fields)
    .map(([k, v]) => `${encodeURIComponent(k)}=${encodeURIComponent(v)}`)
    .join('&');
}

async function call(secret: string, method: 'GET' | 'POST', path: string, body?: string): Promise<unknown> {
  const res = await fetch(`${API}${path}`, {
    method,
    headers: {
      Authorization: `Bearer ${secret}`,
      ...(body ? { 'Content-Type': 'application/x-www-form-urlencoded' } : {}),
    },
    body,
  });
  if (!res.ok) throw new Error(`stripe ${path} -> ${res.status}`);
  return res.json();
}

export interface CheckoutSession {
  id: string;
  url: string;
}

/**
 * One-time $9.99 Checkout Session. The payer returns to `returnPath` (their room) with
 * ?session_id=…&dev=… so the client can finish the unlock right where they left off; `device`
 * rides client_reference_id so /verify can prove the paid session belongs to the phone that's
 * asking. Amount/currency are inline (one SKU, no Price object to provision) so this works the
 * instant a test key is set.
 */
export async function createCheckout(
  secret: string,
  device: string,
  origin: string,
  returnPath: string,
): Promise<CheckoutSession> {
  const success = `${origin}${returnPath}?session_id={CHECKOUT_SESSION_ID}&dev=${encodeURIComponent(device)}`;
  const body = form({
    mode: 'payment',
    client_reference_id: device,
    success_url: success,
    cancel_url: `${origin}${returnPath}?unlock=cancel`,
    'line_items[0][quantity]': '1',
    'line_items[0][price_data][currency]': 'usd',
    'line_items[0][price_data][unit_amount]': '999',
    'line_items[0][price_data][product_data][name]': 'HELLDECK — every night, this phone, forever',
  });
  const s = (await call(secret, 'POST', '/checkout/sessions', body)) as { id?: string; url?: string };
  if (typeof s.id !== 'string' || typeof s.url !== 'string') throw new Error('stripe: malformed session');
  return { id: s.id, url: s.url };
}

/**
 * True only if this session is PAID and its client_reference_id matches the device asking —
 * so a session id can't be replayed to unlock a different phone. Any Stripe error resolves
 * to false; the caller issues no token on false.
 */
export async function sessionUnlocksDevice(secret: string, sessionId: string, device: string): Promise<boolean> {
  try {
    const s = (await call(secret, 'GET', `/checkout/sessions/${encodeURIComponent(sessionId)}`)) as {
      payment_status?: string;
      client_reference_id?: string;
    };
    return s.payment_status === 'paid' && s.client_reference_id === device;
  } catch {
    return false;
  }
}
