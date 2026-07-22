// Router: POST /api/room (create), GET /ws/:code (upgrade -> RoomDO), /api/entitle/* (the
// $9.99 unlock), static assets. DESCENT_BUILD_SPEC.md Part 1.1, 4.1, Part 11. Tasks D-102, D-412.
import { CODE_ALPHABET } from '@helldeck/engine';
import { loadContent } from './content.js';
import { devUnlockAvailable, resolveEntitlement, signUnlock, validDevice, verifyUnlock } from './entitle.js';
import { createCheckout, sessionUnlocksDevice } from './stripe.js';

// D-127: inject the real corpus into every game module once, at isolate load — before any
// DO (same isolate, same engine module instances) ever deals a card. Idempotent.
loadContent();

export interface Env {
  ROOM: DurableObjectNamespace;
  LEDGER: DurableObjectNamespace; // one per host device: the free-night ledger (D-412)
  ASSETS: Fetcher;
  ENV: string;
  UNLOCK_SECRET?: string; // HMAC key for the device-bound unlock token
  STRIPE_SECRET?: string; // sk_test_ / sk_live_ (owner-gated); absent => dev-unlock only
}

export { RoomDO } from './room-do.js';
export { LedgerDO } from './ledger-do.js';

async function freeNightUsed(env: Env, device: string): Promise<boolean> {
  try {
    const stub = env.LEDGER.get(env.LEDGER.idFromName(device));
    const res = await stub.fetch(new Request('https://ledger/status'));
    return ((await res.json()) as { freeNightUsed?: boolean }).freeNightUsed === true;
  } catch {
    return false; // ledger unreachable: bias to letting friends play (fail-open, spec Part 11)
  }
}

function newCode(): string {
  let code = '';
  const bytes = crypto.getRandomValues(new Uint8Array(4));
  for (const b of bytes) code += CODE_ALPHABET[b % CODE_ALPHABET.length];
  return code;
}

const badDevice = (): Response => Response.json({ error: 'BAD_DEVICE' }, { status: 400 });

// The $9.99 unlock endpoints (spec Part 11 / D-412). All device-scoped; the device token is
// a client-minted opaque id, validated before it names a DO or feeds the HMAC.
async function entitle(req: Request, env: Env, url: URL): Promise<Response> {
  // GET /api/entitle/status?dev=&unlock= — the paywall's read model: is this phone entitled,
  // and if not, has it burned its free night (so the copy can say "your first was free")?
  if (req.method === 'GET' && url.pathname === '/api/entitle/status') {
    const device = url.searchParams.get('dev') ?? '';
    if (!validDevice(device)) return badDevice();
    const unlocked = await verifyUnlock(env.UNLOCK_SECRET, device, url.searchParams.get('unlock'));
    const used = unlocked ? true : await freeNightUsed(env, device);
    const ent = resolveEntitlement({ unlocked, freeNightUsed: used });
    return Response.json({ entitled: ent.entitled, reason: ent.reason, unlocked, freeNightUsed: used });
  }

  // POST /api/entitle/checkout {device} — open a Stripe Checkout Session, return its URL.
  // No key configured => 501 with a flag the client reads to offer dev-unlock (non-prod only).
  if (req.method === 'POST' && url.pathname === '/api/entitle/checkout') {
    const body = (await req.json().catch(() => ({}))) as { device?: string; returnPath?: string };
    const device = body.device ?? '';
    if (!validDevice(device)) return badDevice();
    if (!env.STRIPE_SECRET) {
      return Response.json(
        { error: 'NO_STRIPE', devUnlock: devUnlockAvailable(env.ENV, env.UNLOCK_SECRET) },
        { status: 501 },
      );
    }
    // Only bounce back to a same-origin room path (/ABCD); never an attacker-supplied absolute URL.
    const returnPath = /^\/[A-Z]{4}$/.test(body.returnPath ?? '') ? body.returnPath! : '/';
    try {
      const session = await createCheckout(env.STRIPE_SECRET, device, url.origin, returnPath);
      return Response.json({ url: session.url });
    } catch {
      return Response.json({ error: 'CHECKOUT_FAILED' }, { status: 502 });
    }
  }

  // GET /api/entitle/verify?session_id=&dev= — after payment, prove the session is paid and
  // bound to this device, then mint the HMAC unlock token the phone stores forever.
  if (req.method === 'GET' && url.pathname === '/api/entitle/verify') {
    const device = url.searchParams.get('dev') ?? '';
    const sessionId = url.searchParams.get('session_id') ?? '';
    if (!validDevice(device)) return badDevice();
    if (!env.STRIPE_SECRET || !env.UNLOCK_SECRET) return Response.json({ error: 'NO_STRIPE' }, { status: 501 });
    const ok = sessionId ? await sessionUnlocksDevice(env.STRIPE_SECRET, sessionId, device) : false;
    if (!ok) return Response.json({ error: 'NOT_PAID' }, { status: 402 });
    return Response.json({ unlock: await signUnlock(env.UNLOCK_SECRET, device) });
  }

  // POST /api/entitle/dev-unlock?dev= — NON-PROD ESCAPE HATCH so the whole vertical is
  // playable and testable without any Stripe key. Refuses in production.
  if (req.method === 'POST' && url.pathname === '/api/entitle/dev-unlock') {
    if (!devUnlockAvailable(env.ENV, env.UNLOCK_SECRET)) return new Response('not found', { status: 404 });
    const device = url.searchParams.get('dev') ?? '';
    if (!validDevice(device)) return badDevice();
    return Response.json({ unlock: await signUnlock(env.UNLOCK_SECRET!, device) });
  }

  return new Response('not found', { status: 404 });
}

export default {
  async fetch(req: Request, env: Env): Promise<Response> {
    const url = new URL(req.url);

    if (req.method === 'POST' && url.pathname === '/api/room') {
      // Retry on (astronomically unlikely) live-code collision is deferred; codes expire with rooms.
      const code = newCode();
      const stub = env.ROOM.get(env.ROOM.idFromName(code));
      await stub.fetch(new Request(`https://do/init?code=${code}`, { method: 'POST' }));
      return Response.json({ code });
    }

    const wsMatch = url.pathname.match(/^\/ws\/([A-Z]{4})$/);
    if (wsMatch) {
      const code = wsMatch[1]!;
      const stub = env.ROOM.get(env.ROOM.idFromName(code));
      return stub.fetch(req); // DO handles the upgrade
    }

    if (url.pathname.startsWith('/api/entitle/')) return entitle(req, env, url);

    return env.ASSETS.fetch(req);
  },
} satisfies ExportedHandler<Env>;
