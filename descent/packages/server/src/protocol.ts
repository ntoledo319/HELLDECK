// Wire law: the COMPLETE client→server message table from spec Part 3.2. Anything not
// here does not exist. Pure — the DO supplies identity/time/authority in ParseCtx, this
// file turns a frame into exactly one of: engine event, clock pong, resync, fire, error.
// Tasks D-104/D-105.
import type { GameEvent } from '@helldeck/engine';

export type ErrCode = 'ROOM_FULL' | 'BAD_INPUT' | 'NOT_HOST' | 'NO_ENTITLEMENT' | 'ROOM_EXPIRED';

export interface ParseCtx {
  id: string; // playerId from the socket attachment (token = identity)
  at: number; // server ms — the DO reads the clock, never the engine
  isHost: boolean;
  entitled: boolean;
  nPlayers: number; // active (host + player) count, for stageMode default
  names: string[]; // current roster, for crewId
}

export type Parsed =
  | { kind: 'event'; event: GameEvent }
  | { kind: 'pong'; pingId: number; clientClock: number }
  | { kind: 'resync' }
  | { kind: 'fire'; n: number }
  | { kind: 'err'; code: ErrCode; msg: string };

/** crewId = stable hash of the sorted lowercased roster (spec Part 2 NightConfig). */
export function crewId(names: readonly string[]): string {
  const key = [...names].map((n) => n.trim().toLowerCase()).sort().join('|');
  let h = 5381;
  for (let i = 0; i < key.length; i++) h = (Math.imul(h, 33) ^ key.charCodeAt(i)) >>> 0;
  return h.toString(36);
}

function ev(event: GameEvent): Parsed {
  return { kind: 'event', event };
}
function err(code: ErrCode, msg: string): Parsed {
  return { kind: 'err', code, msg };
}
function clampInt(v: unknown, min: number, max: number, fallback: number): number {
  const n = Math.floor(Number(v));
  return Number.isFinite(n) ? Math.max(min, Math.min(max, n)) : fallback;
}

export function parseClientMessage(msg: { t: string; [k: string]: unknown }, ctx: ParseCtx): Parsed {
  switch (msg.t) {
    case 'JOIN': {
      const name = typeof msg['name'] === 'string' ? msg['name'] : '';
      return ev({ t: 'JOIN', id: ctx.id, name, avatar: clampInt(msg['avatar'], 0, 15, 0), at: ctx.at });
    }
    case 'ATTEST18':
      return ev({ t: 'ATTEST18', id: ctx.id });
    case 'CEILING': {
      const v = msg['v'];
      if (v !== 1 && v !== 2 && v !== 3 && v !== 4 && v !== 5) return err('BAD_INPUT', 'ceiling is 1..5');
      return ev({ t: 'CEILING', id: ctx.id, v });
    }
    case 'CONFIG': {
      if (!ctx.isHost) return err('NOT_HOST', 'only the host configures the night');
      const depth = msg['depth'];
      const vibe = msg['vibe'];
      if (depth !== 5 && depth !== 7 && depth !== 9) return err('BAD_INPUT', 'depth is 5|7|9');
      if (vibe !== 'sober' && vibe !== 'warm' && vibe !== 'feral') return err('BAD_INPUT', 'vibe is sober|warm|feral');
      const stageMode = typeof msg['stage'] === 'boolean' ? msg['stage'] : ctx.nPlayers >= 5; // spec Part 2 default
      // irlFamiliar is not on the wire (3.2 is exhaustive) — defaults true until crew memory lands (D-127).
      return ev({
        t: 'CONFIG',
        id: ctx.id,
        cfg: { depth, vibe, stageMode, crewId: crewId(ctx.names), irlFamiliar: true },
      });
    }
    case 'BEGIN':
      if (!ctx.isHost) return err('NOT_HOST', 'only the host begins the descent');
      if (!ctx.entitled) return err('NO_ENTITLEMENT', 'host unlock required for this night');
      return ev({ t: 'BEGIN', id: ctx.id, at: ctx.at });
    case 'INPUT':
      return ev({ t: 'INPUT', id: ctx.id, payload: msg['p'], at: ctx.at });
    case 'BURN': {
      const kind = msg['kind'];
      if (kind !== 'card' && kind !== 'spotlight') return err('BAD_INPUT', 'burn kind is card|spotlight');
      return ev({ t: 'BURN', id: ctx.id, kind, at: ctx.at });
    }
    case 'DESCEND':
      return ev({ t: 'DESCEND', id: ctx.id, at: ctx.at });
    case 'VOID':
      if (!ctx.isHost) return err('NOT_HOST', 'only the host voids a round');
      return ev({ t: 'VOID_ROUND', id: ctx.id, at: ctx.at });
    case 'FIFTH':
      return ev({ t: 'PLEAD_FIFTH', id: ctx.id, at: ctx.at });
    case 'SKIPEM':
      return ev({ t: 'SKIP_EM', id: ctx.id, at: ctx.at });
    case 'REST':
      return ev({ t: 'REST_CASE', id: ctx.id, at: ctx.at });
    case 'CLAIM':
      return ev({ t: 'CLAIM', id: ctx.id, at: ctx.at });
    case 'FIRE':
      // Raw tap count; the DO applies the 10/s budget (clock.ts clampFire) before dispatch.
      return { kind: 'fire', n: clampInt(msg['n'], 1, 100, 1) };
    case 'PONG': {
      const pingId = Number(msg['id']);
      const clientClock = Number(msg['cl']);
      if (!Number.isFinite(pingId) || !Number.isFinite(clientClock)) return err('BAD_INPUT', 'malformed pong');
      return { kind: 'pong', pingId, clientClock };
    }
    case 'RESYNC':
      return { kind: 'resync' };
    default:
      return err('BAD_INPUT', `unknown message: ${msg.t}`);
  }
}
