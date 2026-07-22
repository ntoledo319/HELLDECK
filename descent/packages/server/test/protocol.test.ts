// Protocol tests — spec Part 3 end-to-end at unit scale. The DO is driven directly with
// fake sockets/storage (a real Miniflare harness needs a dependency this package doesn't
// carry yet — noted gap; the wire law itself is fully covered here):
//   3.1 handshake (WELCOME -> STATE, token issue, silent reseat)
//   3.2 the COMPLETE client message table via parseClientMessage
//   3.3 clock math (median offset from ping batches) + AT deadline broadcasting
//   3.4 redaction (no secret fields in any frame)
//   FIRE 10/s budget + HEAT <= 4Hz, SCHEDULE -> Alarm -> TIMER event, seat-hold delivery.
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { reduce, type Effect, type GameEvent } from '@helldeck/engine';
import {
  clampFire,
  emptyClock,
  FIRE_CAP_PER_SEC,
  HEAT_FLUSH_MS,
  median,
  PENDING_PING_TTL_MS,
  PING_BATCH_SIZE,
  PING_REFRESH_MS,
  PING_SPACING_MS,
  recordPing,
  recordPong,
  sampleOffset,
} from '../src/clock.js';
import { signUnlock } from '../src/entitle.js';
import { LedgerDO } from '../src/ledger-do.js';
import { crewId, parseClientMessage, type ParseCtx } from '../src/protocol.js';
import { assertNoSecrets, redactFor } from '../src/redact.js';
import { RoomDO } from '../src/room-do.js';
import { botMoves, deckOf, subOf } from './botlogic.js';

// Spy on reduce so every wire message can be asserted as the exact engine event it maps
// to — independent of how much of the engine is implemented yet.
vi.mock('@helldeck/engine', async (importOriginal) => {
  const actual = await importOriginal<typeof import('@helldeck/engine')>();
  return { ...actual, reduce: vi.fn(actual.reduce) };
});
const reduceSpy = vi.mocked(reduce);
const engineEvents = (): GameEvent[] => reduceSpy.mock.calls.map((c) => c[1]);

// ===== fakes =====
class FakeStorage {
  data = new Map<string, unknown>();
  alarm: number | null = null;
  async get(k: string): Promise<unknown> {
    return this.data.get(k);
  }
  async put(k: string, v: unknown): Promise<void> {
    this.data.set(k, structuredClone(v));
  }
  async setAlarm(at: number): Promise<void> {
    this.alarm = at;
  }
  async deleteAlarm(): Promise<void> {
    this.alarm = null;
  }
}

class FakeWS {
  private attachment: unknown = null;
  frames: Array<Record<string, unknown>> = [];
  raw: string[] = [];
  serializeAttachment(v: unknown): void {
    this.attachment = structuredClone(v);
  }
  deserializeAttachment(): unknown {
    return this.attachment;
  }
  send(s: string): void {
    this.raw.push(s);
    this.frames.push(JSON.parse(s) as Record<string, unknown>);
  }
  clear(): void {
    this.frames = [];
    this.raw = [];
  }
  last(t: string): Record<string, unknown> | undefined {
    return [...this.frames].reverse().find((f) => f['t'] === t);
  }
}

class FakeCtx {
  storage = new FakeStorage();
  sockets: FakeWS[] = [];
  acceptWebSocket(ws: FakeWS): void {
    this.sockets.push(ws);
  }
  getWebSockets(): FakeWS[] {
    return this.sockets;
  }
}

interface DOInternals {
  handleOpen(ws: unknown, token: string, now: number): Promise<void>;
  runEffects(effects: Effect[]): Promise<void>;
}
const internals = (d: RoomDO): DOInternals => d as unknown as DOInternals;

async function makeRoom(): Promise<{ room: RoomDO; ctx: FakeCtx }> {
  const ctx = new FakeCtx();
  const room = new RoomDO(ctx as unknown as DurableObjectState, {});
  await room.fetch(new Request('https://do/init?code=HELL', { method: 'POST' }));
  return { room, ctx };
}

async function connect(room: RoomDO, ctx: FakeCtx, token: string): Promise<FakeWS> {
  const ws = new FakeWS();
  ctx.acceptWebSocket(ws);
  await internals(room).handleOpen(ws, token, Date.now());
  return ws;
}

const send = (room: RoomDO, ws: FakeWS, msg: Record<string, unknown>): Promise<void> =>
  room.webSocketMessage(ws as unknown as WebSocket, JSON.stringify(msg));

const T0 = 1_720_900_000_000;

beforeEach(() => {
  vi.useFakeTimers({ now: T0 });
  reduceSpy.mockClear();
});
afterEach(() => {
  vi.useRealTimers();
});

// ===== 3.3 clock math =====
describe('clock math', () => {
  it('median: empty=0, odd=middle, even=mean of middles', () => {
    expect(median([])).toBe(0);
    expect(median([5, 1, 9])).toBe(5);
    expect(median([1, 3, 5, 100])).toBe(4);
  });

  it('sampleOffset assumes the network midpoint', () => {
    // server sent at 1000, received pong at 1100; client clock read 500 at the midpoint 1050
    expect(sampleOffset(1000, 500, 1100)).toBe(550);
  });

  it('recordPing/recordPong: consumes pending, caps samples at 5, returns median', () => {
    let c = emptyClock();
    for (let i = 0; i < 7; i++) {
      c = recordPing(c, `p${i}`, T0 + i * 200);
      const r = recordPong(c, `p${i}`, T0 + i * 200 - 700, T0 + i * 200); // client 700ms behind, zero RTT
      c = r.clock;
      expect(r.offset).toBe(700);
    }
    expect(c.samples.length).toBe(5); // capped
    expect(Object.keys(c.pending).length).toBe(0); // all consumed
    expect(recordPong(c, 'ghost', 0, T0).offset).toBeNull(); // unknown pong ignored
  });

  it('recordPing prunes pings unanswered past the TTL', () => {
    let c = recordPing(emptyClock(), 'old', T0);
    c = recordPing(c, 'new', T0 + PENDING_PING_TTL_MS + 1);
    expect(c.pending['old']).toBeUndefined();
    expect(c.pending['new']).toBeDefined();
  });

  it('clampFire: 10/s rolling budget, window resets after 1s', () => {
    let r = clampFire({ start: 0, used: 0 }, 8, T0);
    expect(r.allowed).toBe(8);
    r = clampFire(r.window, 8, T0 + 10);
    expect(r.allowed).toBe(FIRE_CAP_PER_SEC - 8); // clipped to remaining budget
    r = clampFire(r.window, 5, T0 + 20);
    expect(r.allowed).toBe(0); // budget spent
    r = clampFire(r.window, 5, T0 + 1000);
    expect(r.allowed).toBe(5); // fresh window
  });
});

// ===== 3.2 the complete client message table =====
describe('parseClientMessage (Part 3.2 — the whole table)', () => {
  const base: ParseCtx = { id: 'p1', at: T0, isHost: false, entitled: true, nPlayers: 4, names: ['SAM', 'MAX'] };
  const host: ParseCtx = { ...base, isHost: true };
  const evt = (msg: Record<string, unknown>, ctx: ParseCtx = base): GameEvent => {
    const p = parseClientMessage(msg as { t: string }, ctx);
    if (p.kind !== 'event') throw new Error(`expected event, got ${p.kind}`);
    return p.event;
  };
  const errCode = (msg: Record<string, unknown>, ctx: ParseCtx = base): string => {
    const p = parseClientMessage(msg as { t: string }, ctx);
    if (p.kind !== 'err') throw new Error(`expected err, got ${p.kind}`);
    return p.code;
  };

  it('JOIN maps name/avatar and clamps avatar to 0..15', () => {
    expect(evt({ t: 'JOIN', name: 'SAM', avatar: 3 })).toEqual({ t: 'JOIN', id: 'p1', name: 'SAM', avatar: 3, at: T0 });
    expect(evt({ t: 'JOIN', name: 'SAM', avatar: 99 })).toMatchObject({ avatar: 15 });
  });

  it('ATTEST18 / CEILING (validated 1..5)', () => {
    expect(evt({ t: 'ATTEST18' })).toEqual({ t: 'ATTEST18', id: 'p1' });
    expect(evt({ t: 'CEILING', v: 4 })).toEqual({ t: 'CEILING', id: 'p1', v: 4 });
    expect(errCode({ t: 'CEILING', v: 9 })).toBe('BAD_INPUT');
  });

  it('CONFIG: host-only, validated, stageMode defaults to nPlayers>=5, crewId derived', () => {
    expect(errCode({ t: 'CONFIG', depth: 7, vibe: 'feral' })).toBe('NOT_HOST');
    expect(errCode({ t: 'CONFIG', depth: 6, vibe: 'feral' }, host)).toBe('BAD_INPUT');
    expect(errCode({ t: 'CONFIG', depth: 7, vibe: 'zen' }, host)).toBe('BAD_INPUT');
    expect(evt({ t: 'CONFIG', depth: 7, vibe: 'feral', stage: true }, host)).toEqual({
      t: 'CONFIG',
      id: 'p1',
      cfg: { depth: 7, vibe: 'feral', stageMode: true, crewId: crewId(['SAM', 'MAX']), irlFamiliar: true },
    });
    // stage omitted at 4 players -> false; at 5 -> true
    expect(evt({ t: 'CONFIG', depth: 5, vibe: 'warm' }, host)).toMatchObject({ cfg: { stageMode: false } });
    expect(evt({ t: 'CONFIG', depth: 5, vibe: 'warm' }, { ...host, nPlayers: 5 })).toMatchObject({
      cfg: { stageMode: true },
    });
  });

  it('BEGIN: host-only, entitlement-gated', () => {
    expect(errCode({ t: 'BEGIN' })).toBe('NOT_HOST');
    expect(errCode({ t: 'BEGIN' }, { ...host, entitled: false })).toBe('NO_ENTITLEMENT');
    expect(evt({ t: 'BEGIN' }, host)).toEqual({ t: 'BEGIN', id: 'p1', at: T0 });
  });

  it('INPUT / BURN / DESCEND / VOID / FIFTH / SKIPEM / REST', () => {
    expect(evt({ t: 'INPUT', p: { vote: 'p2' } })).toEqual({ t: 'INPUT', id: 'p1', payload: { vote: 'p2' }, at: T0 });
    expect(evt({ t: 'BURN', kind: 'card' })).toEqual({ t: 'BURN', id: 'p1', kind: 'card', at: T0 });
    expect(evt({ t: 'BURN', kind: 'spotlight' })).toMatchObject({ kind: 'spotlight' });
    expect(errCode({ t: 'BURN', kind: 'everything' })).toBe('BAD_INPUT');
    expect(evt({ t: 'DESCEND' })).toEqual({ t: 'DESCEND', id: 'p1', at: T0 });
    expect(errCode({ t: 'VOID' })).toBe('NOT_HOST');
    expect(evt({ t: 'VOID' }, host)).toEqual({ t: 'VOID_ROUND', id: 'p1', at: T0 });
    expect(evt({ t: 'FIFTH' })).toEqual({ t: 'PLEAD_FIFTH', id: 'p1', at: T0 });
    expect(evt({ t: 'SKIPEM' })).toEqual({ t: 'SKIP_EM', id: 'p1', at: T0 });
    expect(evt({ t: 'REST' })).toEqual({ t: 'REST_CASE', id: 'p1', at: T0 });
  });

  it('CLAIM: any player may volunteer; identity/time always come from the server context', () => {
    expect(evt({ t: 'CLAIM' })).toEqual({ t: 'CLAIM', id: 'p1', at: T0 });
    expect(evt({ t: 'CLAIM', id: 'spoofed-player', at: 0 }, host)).toEqual({ t: 'CLAIM', id: 'p1', at: T0 });
  });

  it('FIRE / PONG / RESYNC route to DO-side handlers, not the engine', () => {
    expect(parseClientMessage({ t: 'FIRE', n: 4 }, base)).toEqual({ kind: 'fire', n: 4 });
    expect(parseClientMessage({ t: 'PONG', id: 17, cl: 123 }, base)).toEqual({
      kind: 'pong',
      pingId: 17,
      clientClock: 123,
    });
    expect(parseClientMessage({ t: 'RESYNC' }, base)).toEqual({ kind: 'resync' });
  });

  it('anything not in the table does not exist -> ERR BAD_INPUT', () => {
    expect(errCode({ t: 'HAX' })).toBe('BAD_INPUT');
  });

  it('crewId is order- and case-insensitive', () => {
    expect(crewId(['SAM', ' max'])).toBe(crewId(['Max', 'sam ']));
    expect(crewId(['SAM'])).not.toBe(crewId(['MAX']));
  });
});

// ===== 3.1 handshake + 3.4 redaction, through the DO =====
describe('RoomDO handshake & redaction', () => {
  it('connect: WELCOME (with token) then full STATE (3.1 order)', async () => {
    const { room, ctx } = await makeRoom();
    const ws = await connect(room, ctx, 'tok1');
    expect(ws.frames[0]).toMatchObject({
      t: 'WELCOME',
      you: 'tok1',
      token: 'tok1',
      code: 'HELL',
      sv: expect.any(Number),
    });
    expect(ws.frames[1]?.['t']).toBe('STATE');
  });

  it('JOIN creates the player and broadcasts per-viewer redacted STATE', async () => {
    const { room, ctx } = await makeRoom();
    const ws1 = await connect(room, ctx, 'tok1');
    await send(room, ws1, { t: 'JOIN', name: 'sam', avatar: 3 });
    const ws2 = await connect(room, ctx, 'tok2');
    await send(room, ws2, { t: 'JOIN', name: 'max', avatar: 5 });

    const s1 = ws1.last('STATE')!['s'] as { players: Array<Record<string, unknown>>; you: string };
    expect(s1.you).toBe('tok1');
    expect(s1.players.map((p) => p['name'])).toEqual(['SAM', 'MAX']);
    // own brimstones visible, other players' masked (-1)
    expect(s1.players.find((p) => p['id'] === 'tok1')!['brimstones']).toBe(2);
    expect(s1.players.find((p) => p['id'] === 'tok2')!['brimstones']).toBe(-1);
    // engine saw the JOINs
    expect(engineEvents().filter((e) => e.t === 'JOIN').length).toBe(2);
  });

  it('D-113 guard: no broadcast frame ever carries a secret field', async () => {
    const { room, ctx } = await makeRoom();
    const ws1 = await connect(room, ctx, 'tok1');
    await send(room, ws1, { t: 'JOIN', name: 'sam', avatar: 3 });
    const ws2 = await connect(room, ctx, 'tok2');
    await send(room, ws2, { t: 'JOIN', name: 'max', avatar: 5 });
    await send(room, ws2, { t: 'RESYNC' });
    for (const ws of [ws1, ws2]) for (const frame of ws.raw) assertNoSecrets(frame);
    // and directly against the serializer
    const state = reduceSpy.mock.results.at(-1)!.value.state;
    const json = JSON.stringify(redactFor(state, 'tok1'));
    expect(json).not.toContain('heatCeiling');
    assertNoSecrets(json);
  });

  it('reseat: known token silently reconnects — WELCOME + STATE, no duplicate player', async () => {
    const { room, ctx } = await makeRoom();
    const ws1 = await connect(room, ctx, 'tok1');
    await send(room, ws1, { t: 'JOIN', name: 'sam', avatar: 3 });
    ctx.sockets = []; // phone died
    const ws1b = await connect(room, ctx, 'tok1');
    expect(ws1b.frames[0]).toMatchObject({ t: 'WELCOME', you: 'tok1' });
    const s = ws1b.last('STATE')!['s'] as { players: unknown[] };
    expect(s.players.length).toBe(1); // reseat, not re-JOIN
    expect(engineEvents().some((e) => e.t === 'RECONNECT' && 'id' in e && e.id === 'tok1')).toBe(true);
  });

  it('RESYNC returns full STATE to the asking socket only', async () => {
    const { room, ctx } = await makeRoom();
    const ws1 = await connect(room, ctx, 'tok1');
    const ws2 = await connect(room, ctx, 'tok2');
    ws1.clear();
    ws2.clear();
    await send(room, ws1, { t: 'RESYNC' });
    expect(ws1.frames.some((f) => f['t'] === 'STATE')).toBe(true);
    // the message-path pump may PING everyone (clock batch), but STATE goes only to the asker
    expect(ws2.frames.some((f) => f['t'] === 'STATE')).toBe(false);
  });

  it('host-only messages from a non-host answer ERR NOT_HOST on that socket', async () => {
    const { room, ctx } = await makeRoom();
    const wsHost = await connect(room, ctx, 'tok1');
    await send(room, wsHost, { t: 'JOIN', name: 'sam', avatar: 0 });
    const ws2 = await connect(room, ctx, 'tok2');
    await send(room, ws2, { t: 'JOIN', name: 'max', avatar: 1 });
    ws2.clear();
    await send(room, ws2, { t: 'CONFIG', depth: 7, vibe: 'feral' });
    expect(ws2.last('ERR')).toMatchObject({ code: 'NOT_HOST' });
  });

  it('CLAIM round-trip: a non-host volunteer is counted without exposing their identity', async () => {
    const { room, ctx } = await makeRoom(); // HELL / N=5 / depth-5 warm opens on overunder (spotlight-claimable)
    const socks: FakeWS[] = [];
    for (let i = 0; i < 5; i++) {
      const ws = await connect(room, ctx, `tok${i + 1}`);
      await send(room, ws, { t: 'JOIN', name: `B${i}`, avatar: i });
      await send(room, ws, { t: 'CEILING', v: 3 });
      await send(room, ws, { t: 'ATTEST18' });
      socks.push(ws);
    }
    await send(room, socks[0]!, { t: 'CONFIG', depth: 5, vibe: 'warm', stage: false });
    await send(room, socks[0]!, { t: 'BEGIN' });

    const gameView = (ws: FakeWS): Record<string, unknown> => {
      const state = ws.last('STATE')!['s'] as { gameView: Record<string, unknown> };
      return state.gameView;
    };
    expect(gameView(socks[0]!)).toEqual({
      deck: 'overunder',
      sub: 'INTRO',
      firstTime: true,
      claimable: true,
      claimed: 0,
      youVolunteered: false,
    });

    for (const ws of socks) ws.clear();
    reduceSpy.mockClear();
    await send(room, socks[1]!, { t: 'CLAIM', id: 'tok5', at: 0 }); // non-host; attempted identity/time spoof

    expect(engineEvents()).toContainEqual({ t: 'CLAIM', id: 'tok2', at: T0 });
    for (const [i, ws] of socks.entries()) {
      expect(ws.last('ERR')).toBeUndefined();
      expect(gameView(ws)).toEqual({
        deck: 'overunder',
        sub: 'INTRO',
        firstTime: true,
        claimable: true,
        claimed: 1,
        youVolunteered: i === 1,
      });
    }
  });

  it('spotlight safety is socket-private and survives RESYNC through a burn handoff', async () => {
    const { room, ctx } = await makeRoom(); // seeded first circle is single-role over/under
    const socks: FakeWS[] = [];
    for (let i = 0; i < 5; i++) {
      const ws = await connect(room, ctx, `tok${i + 1}`);
      await send(room, ws, { t: 'JOIN', name: `B${i}`, avatar: i });
      await send(room, ws, { t: 'CEILING', v: 3 });
      await send(room, ws, { t: 'ATTEST18' });
      socks.push(ws);
    }
    await send(room, socks[0]!, { t: 'CONFIG', depth: 5, vibe: 'warm', stage: false });
    await send(room, socks[0]!, { t: 'BEGIN' });

    const timers = ctx.storage.data.get('timers') as Record<string, number>;
    const intro = Object.entries(timers).find(([id]) => id.startsWith('intro:'));
    expect(intro).toBeDefined();
    for (const ws of socks) ws.clear();
    vi.setSystemTime(intro![1]);
    await room.alarm();

    const spotlightFrames = (ws: FakeWS): Array<Record<string, unknown>> =>
      ws.frames.filter((frame) => frame['t'] === 'PRIVATE' && frame['k'] === 'spotlight');
    const assigned = socks.flatMap((ws) => spotlightFrames(ws).map((frame) => ({ ws, frame })));
    expect(assigned).toHaveLength(1);
    const primary = assigned[0]!.ws;
    const primaryPayload = assigned[0]!.frame['p'] as Record<string, unknown>;
    expect(primaryPayload).toMatchObject({
      status: 'assigned',
      role: 'subject',
      canBurn: true,
    });
    expect(typeof primaryPayload['ceremonyId']).toBe('string');
    expect(typeof primaryPayload['burnDeadline']).toBe('number');
    expect(typeof primaryPayload['announceAt']).toBe('number');
    for (const ws of socks.filter((candidate) => candidate !== primary)) {
      expect(spotlightFrames(ws)).toHaveLength(0);
    }

    // An accepted dodge acknowledges only the burner and emits no public STATE/audio.
    for (const ws of socks) ws.clear();
    await send(room, primary, { t: 'BURN', kind: 'spotlight' });
    expect(primary.last('PRIVATE')).toMatchObject({
      k: 'spotlight',
      p: { status: 'released', ceremonyId: primaryPayload['ceremonyId'] },
    });
    for (const ws of socks.filter((candidate) => candidate !== primary)) {
      expect(ws.frames).toEqual([]);
    }

    // The released primary is not resurrected by a full per-socket RESYNC.
    primary.clear();
    await send(room, primary, { t: 'RESYNC' });
    expect(primary.last('STATE')).toBeDefined();
    expect(spotlightFrames(primary)).toHaveLength(0);

    // The fixed T+10 handoff privately names one replacement with the original
    // fixed T+20 announcement deadline; every other socket remains ignorant.
    for (const ws of socks) ws.clear();
    vi.setSystemTime(Number(primaryPayload['burnDeadline']));
    await room.alarm();
    const replacements = socks.flatMap((ws) => spotlightFrames(ws).map((frame) => ({ ws, frame })));
    expect(replacements).toHaveLength(1);
    const replacement = replacements[0]!.ws;
    const replacementPayload = replacements[0]!.frame['p'] as Record<string, unknown>;
    expect(replacement).not.toBe(primary);
    expect(replacementPayload).toMatchObject({
      status: 'assigned',
      ceremonyId: primaryPayload['ceremonyId'],
      role: 'subject',
      burnDeadline: primaryPayload['announceAt'],
    });

    // RESYNC replays the live replacement with the same deadline to that socket only.
    for (const ws of socks) ws.clear();
    await send(room, replacement, { t: 'RESYNC' });
    expect(replacement.last('PRIVATE')).toMatchObject({ k: 'spotlight', p: replacementPayload });
    for (const ws of socks.filter((candidate) => candidate !== replacement)) {
      expect(ws.frames).toEqual([]);
    }

    // No public snapshot contains the private candidate order or burn history.
    for (const raw of replacement.raw.filter((frame) => frame.includes('"STATE"'))) {
      expect(raw).not.toContain('reserveIds');
      expect(raw).not.toContain('declinedIds');
      assertNoSecrets(raw);
    }
  });

  it('card preview replay and burn acknowledgement stay correlated and socket-private', async () => {
    const { room, ctx } = await makeRoom(); // HELL / N=5 / depth-5 warm opens on over/under
    const socks: FakeWS[] = [];
    for (let i = 0; i < 5; i++) {
      const ws = await connect(room, ctx, `tok${i + 1}`);
      await send(room, ws, { t: 'JOIN', name: `B${i}`, avatar: i });
      await send(room, ws, { t: 'CEILING', v: 3 });
      await send(room, ws, { t: 'ATTEST18' });
      socks.push(ws);
    }
    await send(room, socks[0]!, { t: 'CONFIG', depth: 5, vibe: 'warm', stage: false });
    await send(room, socks[0]!, { t: 'BEGIN' });

    const timers = ctx.storage.data.get('timers') as Record<string, number>;
    const intro = Object.entries(timers).find(([id]) => id.startsWith('intro:'));
    expect(intro).toBeDefined();
    for (const ws of socks) ws.clear();
    vi.setSystemTime(intro![1]);
    await room.alarm();

    // Let the fixed spotlight ceremony settle cleanly. Its final callback starts
    // over/under's named-card ceremony and privately assigns exactly one preview.
    const spotlight = socks
      .flatMap((ws) => ws.frames)
      .find((frame) => frame['t'] === 'PRIVATE' && frame['k'] === 'spotlight');
    const spotlightPayload = spotlight?.['p'] as Record<string, unknown> | undefined;
    expect(spotlightPayload).toBeDefined();
    for (const ws of socks) ws.clear();
    vi.setSystemTime(Number(spotlightPayload!['burnDeadline']));
    await room.alarm();
    for (const ws of socks) ws.clear();
    vi.setSystemTime(Number(spotlightPayload!['announceAt']));
    await room.alarm();

    const previewFrames = (ws: FakeWS): Array<Record<string, unknown>> =>
      ws.frames.filter((frame) => frame['t'] === 'PRIVATE' && frame['k'] === 'preview');
    const assigned = socks.flatMap((ws) => previewFrames(ws).map((frame) => ({ ws, frame })));
    expect(assigned).toHaveLength(1);
    let subject = assigned[0]!.ws;
    const payload = assigned[0]!.frame['p'] as Record<string, unknown>;
    expect(Object.keys(payload).sort()).toEqual([
      'burnDeadline',
      'canBurn',
      'card',
      'previewId',
      'revealAt',
      'status',
    ]);
    expect(payload).toMatchObject({
      status: 'assigned',
      previewId: expect.any(String),
      card: expect.objectContaining({ id: expect.any(String), text: expect.any(String) }),
      burnDeadline: expect.any(Number),
      revealAt: expect.any(Number),
      canBurn: true,
    });
    expect(payload['burnDeadline']).toBe(payload['revealAt']);
    for (const ws of socks.filter((candidate) => candidate !== subject)) {
      expect(previewFrames(ws)).toHaveLength(0);
    }

    // A dropped phone reseats with the same identity and gets the same live preview.
    const subjectIndex = socks.indexOf(subject);
    const subjectId = (subject.deserializeAttachment() as { playerId: string }).playerId;
    ctx.sockets = ctx.sockets.filter((socket) => socket !== subject);
    for (const ws of socks) ws.clear();
    const reconnected = await connect(room, ctx, subjectId);
    expect(reconnected.last('PRIVATE')).toEqual({ t: 'PRIVATE', k: 'preview', p: payload });
    for (const ws of socks.filter((candidate) => candidate !== subject)) {
      expect(previewFrames(ws)).toHaveLength(0);
    }
    socks[subjectIndex] = reconnected;
    subject = reconnected;
    // Consume the reconnect's immediate clock-sync tick before isolating RESYNC frames.
    await room.alarm();

    // RESYNC reconstructs the exact still-live assignment for the subject only.
    for (const ws of socks) ws.clear();
    await send(room, subject, { t: 'RESYNC' });
    expect(subject.last('STATE')).toBeDefined();
    expect(subject.last('PRIVATE')).toEqual({ t: 'PRIVATE', k: 'preview', p: payload });
    for (const ws of socks.filter((candidate) => candidate !== subject)) {
      expect(ws.frames).toEqual([]);
    }

    // A legal burn gets one correlated PRIVATE release. It does not move the fixed
    // deadline or emit STATE, AT, AUDIO, HEAT, or any frame to another identity.
    for (const ws of socks) ws.clear();
    await send(room, subject, { t: 'BURN', kind: 'card' });
    expect(subject.frames).toEqual([
      {
        t: 'PRIVATE',
        k: 'preview',
        p: { status: 'released', previewId: payload['previewId'] },
      },
    ]);
    for (const ws of socks.filter((candidate) => candidate !== subject)) {
      expect(ws.frames).toEqual([]);
    }
    const afterBurnTimers = ctx.storage.data.get('timers') as Record<string, number>;
    expect(afterBurnTimers[String(payload['previewId'])]).toBe(payload['revealAt']);

    // Once released, neither the burned card nor a historical acknowledgement is replayed.
    subject.clear();
    await send(room, subject, { t: 'RESYNC' });
    expect(subject.last('STATE')).toBeDefined();
    expect(previewFrames(subject)).toHaveLength(0);
  });
});

// ===== 3.3 through the DO: ping batches, PONG -> median offset =====
describe('RoomDO clock sync', () => {
  it('connect schedules a 5x200ms ping batch + 60s refresh; PONGs converge to the median offset', async () => {
    const { room, ctx } = await makeRoom();
    const ws = await connect(room, ctx, 'tok1');
    const storage = ctx.storage;
    expect(storage.alarm).toBe(T0 + 1); // first ping due now; re-arm clamps strictly future

    const CLIENT_BEHIND = 700; // client clock lags the server by 700ms
    for (let k = 0; k < PING_BATCH_SIZE; k++) {
      vi.setSystemTime(T0 + k * PING_SPACING_MS);
      await room.alarm();
      const ping = ws.last('PING')!;
      expect(ping['sv']).toBe(T0 + k * PING_SPACING_MS);
      await send(room, ws, { t: 'PONG', id: ping['id'], cl: Date.now() - CLIENT_BEHIND });
    }
    const att = ws.deserializeAttachment() as { clockOffset: number; clock: { samples: number[] } };
    expect(att.clock.samples.length).toBe(PING_BATCH_SIZE);
    expect(att.clockOffset).toBe(CLIENT_BEHIND);
    // refresh batch armed for +60s
    const timers = storage.data.get('timers') as Record<string, number>;
    expect(timers['do:clock:refresh']).toBe(T0 + PING_REFRESH_MS);
  });
});

// ===== FIRE budget + HEAT <= 4Hz =====
describe('RoomDO fire/heat', () => {
  it('caps FIRE at 10/s per client and coalesces HEAT broadcasts', async () => {
    const { room, ctx } = await makeRoom();
    const ws = await connect(room, ctx, 'tok1');
    await send(room, ws, { t: 'JOIN', name: 'sam', avatar: 0 });
    reduceSpy.mockClear();

    // First flush is immediate (message-path pump): the opening FIRE lands as HEAT now.
    await send(room, ws, { t: 'FIRE', n: 8 });
    expect(ws.last('HEAT')).toMatchObject({ n: 8 });
    ws.clear();
    await send(room, ws, { t: 'FIRE', n: 8 }); // same second: only 2 left in budget
    await send(room, ws, { t: 'FIRE', n: 5 }); // budget spent: dropped entirely
    const fires = engineEvents().filter((e): e is Extract<GameEvent, { t: 'FIRE' }> => e.t === 'FIRE');
    expect(fires.map((f) => f.n)).toEqual([8, 2]);

    // The surviving 2 taps are HELD: next flush is paced >= lastFlush + 250ms (<= 4Hz).
    expect(ws.last('HEAT')).toBeUndefined();
    const timers = ctx.storage.data.get('timers') as Record<string, number>;
    expect(timers['do:heat']).toBe(T0 + HEAT_FLUSH_MS);
    vi.setSystemTime(T0 + HEAT_FLUSH_MS);
    await room.alarm();
    expect(ws.last('HEAT')).toMatchObject({ n: 2 });

    // Fresh 1s window: budget resets; flush allowed immediately (>=250ms since last).
    ws.clear();
    vi.setSystemTime(T0 + 1100);
    await send(room, ws, { t: 'FIRE', n: 3 });
    expect(ws.last('HEAT')).toMatchObject({ n: 3 });
  });
});

// ===== SCHEDULE -> AT broadcast -> Alarm -> TIMER event (the seat-hold delivery path) =====
describe('RoomDO timer delivery (engine owns rules; DO delivers)', () => {
  it('SCHEDULE effect broadcasts AT in server time, arms the alarm, and comes back as a TIMER event', async () => {
    const { room, ctx } = await makeRoom();
    const ws = await connect(room, ctx, 'tok1');
    await send(room, ws, { t: 'JOIN', name: 'sam', avatar: 0 });

    // The engine's LEAVE handler (D-111) emits this seat-hold effect; deliver it as the DO would.
    const holdAt = Date.now() + 90_000;
    await internals(room).runEffects([{ k: 'SCHEDULE', timerId: 'seat:tok1', atMs: holdAt }]);
    expect(ws.last('AT')).toMatchObject({ timerId: 'seat:tok1', at: holdAt });

    reduceSpy.mockClear();
    vi.setSystemTime(holdAt + 1);
    await room.alarm();
    const timerEvents = engineEvents().filter((e): e is Extract<GameEvent, { t: 'TIMER' }> => e.t === 'TIMER');
    expect(timerEvents.map((e) => e.timerId)).toContain('seat:tok1');
    const timers = ctx.storage.data.get('timers') as Record<string, number>;
    expect(timers['seat:tok1']).toBeUndefined(); // consumed
  });

  it('CANCEL removes a scheduled timer before it fires', async () => {
    const { room, ctx } = await makeRoom();
    await connect(room, ctx, 'tok1');
    await internals(room).runEffects([{ k: 'SCHEDULE', timerId: 'seat:tok1', atMs: Date.now() + 90_000 }]);
    await internals(room).runEffects([{ k: 'CANCEL', timerId: 'seat:tok1' }]);
    reduceSpy.mockClear();
    vi.setSystemTime(Date.now() + 90_001);
    await room.alarm();
    expect(engineEvents().some((e) => e.t === 'TIMER')).toBe(false);
  });

  it('seat-hold end-to-end (4.7): LEAVE -> engine schedules 90s hold; reseat cancels; lapse delivers TIMER', async () => {
    const { room, ctx } = await makeRoom();
    const wsHost = await connect(room, ctx, 'tok1');
    await send(room, wsHost, { t: 'JOIN', name: 'sam', avatar: 0 });
    const ws2 = await connect(room, ctx, 'tok2');
    await send(room, ws2, { t: 'JOIN', name: 'max', avatar: 1 });

    // phone 2 drops: real engine LEAVE rule schedules the seat-hold; DO arms + announces it
    ctx.sockets = ctx.sockets.filter((s) => s !== ws2);
    wsHost.clear();
    await room.webSocketClose(ws2 as unknown as WebSocket);
    const timers = ctx.storage.data.get('timers') as Record<string, number>;
    expect(timers['seathold:tok2']).toBe(Date.now() + 90_000);
    expect(wsHost.last('AT')).toMatchObject({ timerId: 'seathold:tok2' });
    const s = wsHost.last('STATE')!['s'] as { players: Array<Record<string, unknown>> };
    expect(s.players.find((p) => p['id'] === 'tok2')!['connected']).toBe(false);

    // silent reseat cancels the hold and marks the seat live again
    const ws2b = await connect(room, ctx, 'tok2');
    expect((ctx.storage.data.get('timers') as Record<string, number>)['seathold:tok2']).toBeUndefined();
    const s2 = ws2b.last('STATE')!['s'] as { players: Array<Record<string, unknown>> };
    expect(s2.players.find((p) => p['id'] === 'tok2')!['connected']).toBe(true);

    // nobody comes back: the DO delivers TIMER into reduce at the deadline (engine owns the verdict)
    ctx.sockets = ctx.sockets.filter((s) => s !== ws2b);
    await room.webSocketClose(ws2b as unknown as WebSocket);
    reduceSpy.mockClear();
    vi.setSystemTime(Date.now() + 90_001);
    await room.alarm();
    const timerEvents = engineEvents().filter((e): e is Extract<GameEvent, { t: 'TIMER' }> => e.t === 'TIMER');
    expect(timerEvents.map((e) => e.timerId)).toContain('seathold:tok2');
  });

  it('close dispatches LEAVE only when the last socket for that seat is gone', async () => {
    const { room, ctx } = await makeRoom();
    const wsA = await connect(room, ctx, 'tok1');
    const wsB = await connect(room, ctx, 'tok1'); // same phone reconnected; old socket lingers
    reduceSpy.mockClear();
    await room.webSocketClose(wsA as unknown as WebSocket);
    expect(engineEvents().some((e) => e.t === 'LEAVE')).toBe(false); // seat still held by wsB
    ctx.sockets = ctx.sockets.filter((s) => s !== wsA && s !== wsB);
    await room.webSocketClose(wsB as unknown as WebSocket);
    const leaves = engineEvents().filter((e): e is Extract<GameEvent, { t: 'LEAVE' }> => e.t === 'LEAVE');
    expect(leaves.map((e) => e.id)).toEqual(['tok1']);
  });
});

// ===== spec item: broadcast carries the engine module's per-viewer view() =====
// M2-INT: drives a REAL *mixed* night through the DO (no engine mocks beyond the
// pass-through spy). At N=5 / depth 5 / warm the seeded arc for room "HELL" is
// [overunder, roast, scatter, fillin, roast] — so this one night exercises the
// over/under BLOCKING truth number, roast's redaction (counts-not-ballots, own
// ballot echoed, N>=5 anonymous spread), a fill-in circle, AND scatter's WHO-DIED
// bomb (M3: the mid-spike module is now registered and plays). Every input is a
// real per-socket gameView decision (botlogic.ts), and the D-113 secret sweep runs
// over every frame that left the DO across the whole mixed night.
describe('RoomDO game view (module view() per viewer)', () => {
  async function advanceToNextTimer(room: RoomDO, ctx: FakeCtx): Promise<boolean> {
    const timers = (ctx.storage.data.get('timers') as Record<string, number>) ?? {};
    const times = Object.values(timers);
    if (times.length === 0) return false;
    const next = Math.min(...times);
    if (next > Date.now()) vi.setSystemTime(next);
    await room.alarm();
    return true;
  }

  it('full mixed night: every registered game plays, blocking truth resolves, no ballot ever leaks', async () => {
    const { room, ctx } = await makeRoom(); // code "HELL" -> arc [overunder, roast, scatter, fillin, roast]
    const tokens = ['tok1', 'tok2', 'tok3', 'tok4', 'tok5']; // N=5 -> roast anonymous-spread mode
    const roster = tokens.map((id, i) => ({ id, role: i === 0 ? 'host' : 'player' }));
    const socks: FakeWS[] = [];
    for (const [i, tok] of tokens.entries()) {
      const ws = await connect(room, ctx, tok);
      await send(room, ws, { t: 'JOIN', name: `B${i}`, avatar: i });
      await send(room, ws, { t: 'CEILING', v: 3 });
      await send(room, ws, { t: 'ATTEST18' });
      socks.push(ws);
    }
    // the lobby BEGIN mirror: every phone sees the FACT of a pick (sealed), never the value
    const lobby = socks[4]!.last('STATE')!['s'] as { players: Array<Record<string, unknown>> };
    expect(lobby.players.every((p) => p['sealed'] === true)).toBe(true);
    await send(room, socks[0]!, { t: 'CONFIG', depth: 5, vibe: 'warm', stage: false });
    await send(room, socks[0]!, { t: 'BEGIN' });

    const gv = (ws: FakeWS): Record<string, unknown> | null => {
      const s = ws.last('STATE')?.['s'] as { gameView?: unknown } | undefined;
      return (s?.gameView ?? null) as Record<string, unknown> | null;
    };
    const phase = (): string => {
      const s = socks[0]!.last('STATE')?.['s'] as { phase: { k: string } } | undefined;
      return s?.phase.k ?? 'LOBBY';
    };

    // CIRCLE_INTRO carries a public intro view so phones can title the game (6.1)
    expect(phase()).toBe('CIRCLE_INTRO');
    expect(gv(socks[0]!)).toMatchObject({ sub: 'INTRO' });

    let sawRoastVote = false;
    let sawRoastReveal = false;
    let sawOverUnderTruth = false; // the blocking truth number resolved a real reveal (4.7/D-115)
    const revealDecks = new Set<string>();
    for (let step = 0; step < 4000 && phase() !== 'JUDGMENT'; step++) {
      // --- inspect current per-socket views BEFORE acting (the host DESCENDs reveals) ---
      const v0 = gv(socks[0]!);
      const freshRoastVote = deckOf(v0) === 'roast' && subOf(v0) === 'VOTE' && v0!['votedCount'] === 0;
      if (freshRoastVote) {
        expect(phase()).toBe('INPUT'); // the client's screen switch rides Phase.k (6.1)
        expect(typeof (v0!['prompt'] as { text?: unknown } | undefined)?.text).toBe('string'); // prompt reaches every phone
        expect(v0!['votes']).toBeUndefined(); // counts only — the voter->target map never serializes
        sawRoastVote = true;
      }
      for (const ws of socks) {
        const v = gv(ws);
        if (subOf(v) !== 'REVEAL') continue;
        const d = deckOf(v);
        if (d) revealDecks.add(d);
        if (d === 'roast') {
          expect(phase()).toBe('REVEAL'); // core-held reveal: DESCEND/fire-decay live here
          expect(v!['spread']).toBeDefined(); // N>=5: anonymous bars
          expect(v!['edges']).toBeUndefined(); // attribution never leaves the server at N>=5
          sawRoastReveal = true;
        }
        if (d === 'overunder' && typeof v!['truth'] === 'number' && v!['voided'] !== true) sawOverUnderTruth = true;
      }

      // --- act: each socket plays its OWN redacted gameView, exactly as a real phone would ---
      let acted = false;
      for (const [i, ws] of socks.entries()) {
        for (const m of botMoves(gv(ws), tokens[i]!, roster, i === 0)) {
          await send(room, ws, m);
          acted = true;
        }
      }
      if (freshRoastVote) {
        // own ballot echoes ONLY to its caster (roast view youVoted); botlogic votes the first other seat
        expect(gv(socks[0]!)?.['youVoted']).toBe('tok2');
        expect(gv(socks[1]!)?.['youVoted']).toBe('tok1');
      }

      // Advance a timer only when the room is genuinely waiting on one (intro, ceremony,
      // ladder, a skippable deadline) — never pre-empt an input window the bots still owe.
      if (!acted && !(await advanceToNextTimer(room, ctx))) break;
    }

    // all four games in this arc played to a reveal — including scatter (M3: the mid-spike
    // module is registered, so it is PLAYED, no longer skipped)
    expect(sawRoastVote).toBe(true);
    expect(sawRoastReveal).toBe(true);
    expect(sawOverUnderTruth).toBe(true);
    expect(revealDecks.has('overunder')).toBe(true);
    expect(revealDecks.has('roast')).toBe(true);
    expect(revealDecks.has('fillin')).toBe(true);
    expect(revealDecks.has('scatter')).toBe(true); // M3: scatter now plays its WHO-DIED bomb
    expect(phase()).toBe('JUDGMENT');
    // Judgment payload feeds the client's crown + superlatives
    const jf = socks[0]!.last('STATE')!['s'] as { judgment: { winners: string[] } | null };
    expect(jf.judgment).not.toBeNull();
    expect(jf.judgment!.winners.length).toBeGreaterThan(0);
    // the synced 3-2-1 flip beat (3.3/5.1) reached every phone as an AT deadline
    for (const ws of socks) {
      expect(ws.frames.some((f) => f['t'] === 'AT' && String(f['timerId']).startsWith('reveal:flip:'))).toBe(true);
    }
    // D-113: every frame that ever left the DO, for every viewer, carries no secret field
    for (const ws of socks) for (const frame of ws.raw) assertNoSecrets(frame);
  });

  it('gameView is null outside circle phases (lobby leaks nothing)', async () => {
    const { room, ctx } = await makeRoom();
    const ws = await connect(room, ctx, 'tok1');
    await send(room, ws, { t: 'JOIN', name: 'sam', avatar: 0 });
    const s = ws.last('STATE')!['s'] as { gameView: unknown };
    expect(s.gameView).toBeNull();
  });
});

// ===== D-103: snapshot restore =====
describe('RoomDO persistence', () => {
  it('a fresh DO instance over the same storage restores the room (kill-mid-night survival)', async () => {
    const { room, ctx } = await makeRoom();
    const ws = await connect(room, ctx, 'tok1');
    await send(room, ws, { t: 'JOIN', name: 'sam', avatar: 3 });

    // DO evicted: new instance, same storage, hibernated socket still attached.
    const room2 = new RoomDO(ctx as unknown as DurableObjectState, {});
    ws.clear();
    await send(room2, ws, { t: 'RESYNC' });
    const s = ws.last('STATE')!['s'] as { players: Array<Record<string, unknown>> };
    expect(s.players.map((p) => p['name'])).toEqual(['SAM']);
  });
});

// ===== D-412: monetization — the free night is per DEVICE and charged only on a real start =====
describe('entitlement at BEGIN', () => {
  const SECRET = 'test-unlock-secret';
  const DEVICE = 'a1b2c3d4e5f6a7b8c9d0e1f2';

  // A LEDGER namespace backed by real LedgerDO instances, one per device name — so the
  // free-night ledger is shared across every room the same device hosts (the whole point).
  class FakeLedger {
    private ledgers = new Map<string, LedgerDO>();
    idFromName(name: string): { name: string } {
      return { name };
    }
    get(id: { name: string }): { fetch(req: Request): Promise<Response> } {
      let l = this.ledgers.get(id.name);
      if (!l) {
        l = new LedgerDO({ storage: new FakeStorage() } as unknown as DurableObjectState, {});
        this.ledgers.set(id.name, l);
      }
      const ledger = l;
      return { fetch: (req: Request) => ledger.fetch(req) };
    }
    async used(dev: string): Promise<boolean> {
      const res = await this.get(this.idFromName(dev)).fetch(new Request('https://ledger/status'));
      return ((await res.json()) as { freeNightUsed: boolean }).freeNightUsed;
    }
    async spend(dev: string): Promise<void> {
      await this.get(this.idFromName(dev)).fetch(new Request('https://ledger/consume-free', { method: 'POST' }));
    }
  }

  async function makeRoomWithEnv(env: unknown): Promise<{ room: RoomDO; ctx: FakeCtx }> {
    const ctx = new FakeCtx();
    const room = new RoomDO(ctx as unknown as DurableObjectState, env);
    await room.fetch(new Request('https://do/init?code=HELL', { method: 'POST' }));
    return { room, ctx };
  }

  interface OpenWithDev {
    handleOpen(ws: unknown, token: string, now: number, dev?: string, unlock?: string): Promise<void>;
  }
  async function connectDev(
    room: RoomDO,
    ctx: FakeCtx,
    token: string,
    dev?: string,
    unlock?: string,
  ): Promise<FakeWS> {
    const ws = new FakeWS();
    ctx.acceptWebSocket(ws);
    await (room as unknown as OpenWithDev).handleOpen(ws, token, Date.now(), dev, unlock);
    return ws;
  }

  // Fill a lobby to `n` sinners with the host on `hostDev`/`hostUnlock`, config set, ready to BEGIN.
  async function lobby(
    room: RoomDO,
    ctx: FakeCtx,
    n: number,
    hostDev?: string,
    hostUnlock?: string,
  ): Promise<FakeWS[]> {
    const socks: FakeWS[] = [];
    for (let i = 0; i < n; i++) {
      const ws = i === 0 ? await connectDev(room, ctx, `tok${i + 1}`, hostDev, hostUnlock) : await connect(room, ctx, `tok${i + 1}`);
      await send(room, ws, { t: 'JOIN', name: `B${i}`, avatar: i });
      await send(room, ws, { t: 'CEILING', v: 3 });
      await send(room, ws, { t: 'ATTEST18' });
      socks.push(ws);
    }
    await send(room, socks[0]!, { t: 'CONFIG', depth: 5, vibe: 'warm', stage: false });
    return socks;
  }

  const roomPhase = (ctx: FakeCtx): string => (ctx.storage.data.get('room') as { phase: { k: string } }).phase.k;

  it('a device gets exactly one free night: the first begins, a second room for the same device is locked', async () => {
    const ledger = new FakeLedger();
    const env = { LEDGER: ledger, UNLOCK_SECRET: SECRET };

    const r1 = await makeRoomWithEnv(env);
    const s1 = await lobby(r1.room, r1.ctx, 3, DEVICE);
    await send(r1.room, s1[0]!, { t: 'BEGIN' });
    expect(s1[0]!.last('ERR')).toBeUndefined();
    expect(roomPhase(r1.ctx)).toBe('CIRCLE_INTRO');
    expect(await ledger.used(DEVICE)).toBe(true);

    // Same device, a brand-new room: the free night is already spent -> locked at BEGIN.
    const r2 = await makeRoomWithEnv(env);
    const s2 = await lobby(r2.room, r2.ctx, 3, DEVICE);
    await send(r2.room, s2[0]!, { t: 'BEGIN' });
    expect(s2[0]!.last('ERR')).toMatchObject({ code: 'NO_ENTITLEMENT' });
    expect(roomPhase(r2.ctx)).toBe('LOBBY');
  });

  it('a paid device plays even after its free night is gone, and unlocking never burns a free night', async () => {
    const ledger = new FakeLedger();
    await ledger.spend(DEVICE); // this device already used its free night
    const unlock = await signUnlock(SECRET, DEVICE);

    const { room, ctx } = await makeRoomWithEnv({ LEDGER: ledger, UNLOCK_SECRET: SECRET });
    const socks = await lobby(room, ctx, 3, DEVICE, unlock);
    await send(room, socks[0]!, { t: 'BEGIN' });
    expect(socks[0]!.last('ERR')).toBeUndefined();
    expect(roomPhase(ctx)).toBe('CIRCLE_INTRO');
  });

  it('a stale/forged unlock token does NOT entitle — it falls through to the free-night rule', async () => {
    const ledger = new FakeLedger();
    await ledger.spend(DEVICE); // no free night left
    const { room, ctx } = await makeRoomWithEnv({ LEDGER: ledger, UNLOCK_SECRET: SECRET });
    const socks = await lobby(room, ctx, 3, DEVICE, 'v1.deadbeef'); // forged token
    await send(room, socks[0]!, { t: 'BEGIN' });
    expect(socks[0]!.last('ERR')).toMatchObject({ code: 'NO_ENTITLEMENT' });
    expect(roomPhase(ctx)).toBe('LOBBY');
  });

  it('a BEGIN the engine rejects (too few sinners) never burns the free night', async () => {
    const ledger = new FakeLedger();
    const { room, ctx } = await makeRoomWithEnv({ LEDGER: ledger, UNLOCK_SECRET: SECRET });
    const socks = await lobby(room, ctx, 2, DEVICE); // only 2 sinners: below the 3 minimum
    await send(room, socks[0]!, { t: 'BEGIN' });
    expect(socks[0]!.last('ERR')).toBeUndefined(); // entitled, so parse passes; the engine simply noops
    expect(roomPhase(ctx)).toBe('LOBBY');
    expect(await ledger.used(DEVICE)).toBe(false); // free night intact — nothing started

    // Add the third sinner; now it actually starts and the free night is charged exactly once.
    const third = await connect(room, ctx, 'tok3');
    await send(room, third, { t: 'JOIN', name: 'B2', avatar: 2 });
    await send(room, third, { t: 'CEILING', v: 3 });
    await send(room, third, { t: 'ATTEST18' });
    await send(room, socks[0]!, { t: 'BEGIN' });
    expect(roomPhase(ctx)).toBe('CIRCLE_INTRO');
    expect(await ledger.used(DEVICE)).toBe(true);
  });
});
