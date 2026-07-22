// One Durable Object per host DEVICE (keyed idFromName(deviceToken)). It holds the single
// irreducibly-stateful fact in the monetization model: has this phone already spent its ONE
// free night? Everything else about the paid path is stateless HMAC (entitle.ts). spec Part
// 11 / D-412. Deliberately tiny — a device ledger, not a room; no engine, no timers, no wire.
//
// The runtime serializes all fetch()es to a single DO instance, so the read-then-write in
// /consume-free is atomic against a concurrent BEGIN. Claims are keyed by the room's durable
// lobby attempt (`CODE:epoch`): retrying the same attempt after a RoomDO crash is safe, while a
// later night in the same room and every other room remain locked.
const CLAIM_RE = /^[A-Z0-9]{4}:[0-9]+$/;

export class LedgerDO implements DurableObject {
  constructor(
    private ctx: DurableObjectState,
    private env: unknown,
  ) {}

  async fetch(req: Request): Promise<Response> {
    const url = new URL(req.url);
    const legacyUsed = ((await this.ctx.storage.get('freeNightUsed')) as boolean | undefined) ?? false;
    const storedClaim = (await this.ctx.storage.get('freeNightClaimId')) as string | undefined;
    const used = legacyUsed || storedClaim !== undefined;
    const requestedClaim = url.searchParams.get('claim') ?? '';
    const claimMatches = CLAIM_RE.test(requestedClaim) && storedClaim === requestedClaim;

    if (req.method === 'GET' && url.pathname === '/status') {
      return Response.json({ freeNightUsed: used, claimMatches });
    }

    if (req.method === 'POST' && url.pathname === '/consume-free') {
      if (!CLAIM_RE.test(requestedClaim)) return Response.json({ error: 'BAD_CLAIM' }, { status: 400 });
      // Crash-safe idempotency: a retry of the exact lobby attempt receives the same
      // grant. A different room/epoch can never reuse it.
      if (claimMatches) return Response.json({ granted: true, freeNightUsed: true, replayed: true });
      if (used) return Response.json({ granted: false, freeNightUsed: true });
      await this.ctx.storage.put('freeNightClaimId', requestedClaim);
      return Response.json({ granted: true, freeNightUsed: true, replayed: false });
    }

    return new Response('bad request', { status: 400 });
  }
}
