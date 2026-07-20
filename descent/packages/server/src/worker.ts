// Router: POST /api/room (create), GET /ws/:code (upgrade -> RoomDO), static assets.
// DESCENT_BUILD_SPEC.md Part 1.1, 4.1. Task D-102.
import { CODE_ALPHABET } from '@helldeck/engine';
import { loadContent } from './content.js';

// D-127: inject the real corpus into every game module once, at isolate load — before any
// DO (same isolate, same engine module instances) ever deals a card. Idempotent.
loadContent();

export interface Env {
  ROOM: DurableObjectNamespace;
  ASSETS: Fetcher;
  ENV: string;
  UNLOCK_SECRET?: string;
}

export { RoomDO } from './room-do.js';

function newCode(): string {
  let code = '';
  const bytes = crypto.getRandomValues(new Uint8Array(4));
  for (const b of bytes) code += CODE_ALPHABET[b % CODE_ALPHABET.length];
  return code;
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

    // D-412: GET /api/entitle/verify?session= (Stripe), POST /api/entitle/play (Play receipt)

    return env.ASSETS.fetch(req);
  },
} satisfies ExportedHandler<Env>;
