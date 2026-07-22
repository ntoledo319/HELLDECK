// One Durable Object per host DEVICE (keyed idFromName(deviceToken)). It holds the single
// irreducibly-stateful fact in the monetization model: has this phone already spent its ONE
// free night? Everything else about the paid path is stateless HMAC (entitle.ts). spec Part
// 11 / D-412. Deliberately tiny — a device ledger, not a room; no engine, no timers, no wire.
//
// The runtime serializes all fetch()es to a single DO instance, so the read-then-write in
// /consume-free is atomic against a concurrent BEGIN: two nights can never both be granted
// "the free one."
export class LedgerDO implements DurableObject {
  constructor(
    private ctx: DurableObjectState,
    private env: unknown,
  ) {}

  async fetch(req: Request): Promise<Response> {
    const url = new URL(req.url);
    const used = ((await this.ctx.storage.get('freeNightUsed')) as boolean | undefined) ?? false;

    if (req.method === 'GET' && url.pathname === '/status') {
      return Response.json({ freeNightUsed: used });
    }

    if (req.method === 'POST' && url.pathname === '/consume-free') {
      // Idempotent: a second call is a no-op that reports the night already spent, so a
      // retry (or a double BEGIN) can never mint two free nights.
      if (used) return Response.json({ granted: false, freeNightUsed: true });
      await this.ctx.storage.put('freeNightUsed', true);
      return Response.json({ granted: true, freeNightUsed: true });
    }

    return new Response('bad request', { status: 400 });
  }
}
