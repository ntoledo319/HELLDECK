// RoomDO: the authoritative room. WS hub (hibernation API), Alarm timers, SQLite snapshots.
// DESCENT_BUILD_SPEC.md Part 3 (protocol), Part 4 (engine wiring). Tasks D-102..D-105, D-113.
//
// Alarm multiplexing: this.timers holds BOTH engine deadlines (dispatched back into
// reduce() as TIMER events — timers are events, not effects) and DO-internal timers
// prefixed "do:" (clock ping batches, HEAT flush) that never touch the engine.
import {
  initialRoom,
  reduce,
  spotlightPrivateFor,
  type Effect,
  type GameEvent,
  type RoomState,
} from '@helldeck/engine';
import {
  clampFire,
  emptyClock,
  HEAT_FLUSH_MS,
  PING_BATCH_SIZE,
  PING_REFRESH_MS,
  PING_SPACING_MS,
  recordPing,
  recordPong,
  type ClockState,
  type FireWindow,
} from './clock.js';
import { parseClientMessage } from './protocol.js';
import { redactFor } from './redact.js';

interface Attachment {
  playerId: string; // token = stable identity across reconnects (spec 3.1)
  clockOffset: number; // serverTime - clientTime, median of last 5 samples (spec 3.3)
  clock: ClockState;
  fire: FireWindow; // rolling 10/s FIRE budget
}

const CLOCK_PREFIX = 'do:clock:';
const CLOCK_REFRESH = 'do:clock:refresh';
const HEAT_TIMER = 'do:heat';
// Alarms can wake a few ms before their target (observed in workerd: "AlarmManager
// mismatch" when a message-handler re-arm races the in-flight invocation). Timers due
// within this window are processed rather than skipped — skipping and re-arming the SAME
// timestamp can be treated as already-fired, silently killing the timer (the lost-deal
// deadlock). Imperceptibly early is harmless; a dead alarm ends the night.
const ALARM_EPSILON_MS = 50;

function newToken(): string {
  const alphabet = 'abcdefghijklmnopqrstuvwxyz0123456789';
  const bytes = crypto.getRandomValues(new Uint8Array(12));
  let t = '';
  for (const b of bytes) t += alphabet[b % alphabet.length];
  return t;
}

export class RoomDO implements DurableObject {
  private state: RoomState | null = null;
  private timers = new Map<string, number>(); // timerId -> atMs (persisted; nearest drives the Alarm)
  // Heat is garnish: pending taps live in memory only — losing them to eviction is fine.
  private heatPending = 0;
  private lastHeatFlush = 0;

  constructor(
    private ctx: DurableObjectState,
    private env: unknown,
  ) {}

  private async load(): Promise<void> {
    if (this.state) return;
    this.state = ((await this.ctx.storage.get('room')) as RoomState | undefined) ?? null;
    this.timers = new Map(Object.entries(((await this.ctx.storage.get('timers')) as Record<string, number>) ?? {}));
  }

  async fetch(req: Request): Promise<Response> {
    await this.load();
    const url = new URL(req.url);

    if (req.method === 'POST' && url.pathname === '/init') {
      const code = url.searchParams.get('code') ?? 'XXXX';
      if (!this.state) {
        this.state = initialRoom(code, Date.now(), true /* entitlement checked at BEGIN, D-412 */);
        await this.persist();
      }
      return new Response('ok');
    }

    if (req.headers.get('Upgrade') === 'websocket') {
      if (!this.state) return new Response('NO SUCH PIT', { status: 404 });
      const token = url.searchParams.get('token') || newToken(); // no token -> server issues one (3.1)
      const pair = new WebSocketPair();
      const [client, server] = Object.values(pair) as [WebSocket, WebSocket];
      // Hibernation API: survives DO eviction; attachment carries identity + clock state.
      this.ctx.acceptWebSocket(server);
      await this.handleOpen(server, token, Date.now());
      return new Response(null, { status: 101, webSocket: client });
    }

    return new Response('bad request', { status: 400 });
  }

  /** Spec 3.1: WELCOME (with token) -> STATE; known token = silent reseat, else JOIN msg follows. */
  private async handleOpen(ws: WebSocket, token: string, now: number): Promise<void> {
    if (!this.state) return;
    const att: Attachment = { playerId: token, clockOffset: 0, clock: emptyClock(), fire: { start: 0, used: 0 } };
    ws.serializeAttachment(att);
    this.sendTo(ws, { t: 'WELCOME', you: token, token, code: this.state.code });
    if (this.state.players.some((p) => p.id === token)) {
      // Silent reconnect: the engine marks the seat live again (D-111 completes the rule).
      await this.dispatch({ t: 'RECONNECT', id: token, at: now });
    }
    this.sendState(ws, true, now);
    await this.startClockBatch(now); // 5 pings x 200ms on connect (3.3)
  }

  async webSocketMessage(ws: WebSocket, raw: string | ArrayBuffer): Promise<void> {
    await this.load();
    if (!this.state || typeof raw !== 'string' || raw.length > 4096) return;
    let parsedRaw: unknown;
    try {
      parsedRaw = JSON.parse(raw);
    } catch {
      return;
    }
    if (typeof parsedRaw !== 'object' || parsedRaw === null) return;
    const msg = parsedRaw as { t?: unknown; [k: string]: unknown };
    if (typeof msg.t !== 'string') return;

    const att = this.att(ws);
    const id = att?.playerId ?? '';
    const now = Date.now();
    const me = this.state.players.find((p) => p.id === id);
    const parsed = parseClientMessage(msg as { t: string; [k: string]: unknown }, {
      id,
      at: now,
      isHost: me?.role === 'host',
      entitled: this.state.entitled,
      nPlayers: this.state.players.filter((p) => p.role !== 'imp').length,
      names: this.state.players.map((p) => p.name),
    });

    switch (parsed.kind) {
      case 'err':
        this.sendTo(ws, { t: 'ERR', code: parsed.code, msg: parsed.msg });
        break;
      case 'pong':
        this.handlePong(ws, parsed.pingId, parsed.clientClock, now);
        break;
      case 'resync':
        this.sendState(ws, true, now); // full STATE + any live private assignment to this socket only
        break;
      case 'fire':
        await this.handleFire(ws, parsed.n, now);
        break;
      case 'event':
        await this.dispatch(parsed.event);
        break;
    }

    // Self-healing: if the runtime lost an alarm (workerd AlarmManager races), any
    // inbound frame — bots and phones PONG every <=60s — processes the overdue timer.
    const next = Math.min(...this.timers.values());
    if (Number.isFinite(next) && next <= now) await this.pump(now);
  }

  async webSocketClose(ws: WebSocket): Promise<void> {
    await this.load();
    const att = this.att(ws);
    if (!att?.playerId || !this.state) return;
    // Reconnects race closes: only LEAVE when no other socket holds this seat. The engine
    // owns the 90s seat-hold rule — its LEAVE handler schedules the timer effect (4.7).
    const stillSeated = this.ctx
      .getWebSockets()
      .some((other) => other !== ws && this.att(other)?.playerId === att.playerId);
    if (!stillSeated) await this.dispatch({ t: 'LEAVE', id: att.playerId, at: Date.now() });
  }

  async alarm(): Promise<void> {
    await this.load();
    await this.pump(Date.now());
  }

  /**
   * Process every due timer, then re-arm. Multi-pass because a consumed timer may
   * SCHEDULE a new already-due one (module chains). Called from alarm() AND after every
   * inbound message with an overdue timer — so a single dropped runtime alarm can never
   * deadlock a night: the next PONG/vote un-sticks it.
   */
  private async pump(now: number): Promise<void> {
    for (let pass = 0; pass < 8; pass++) {
      const due = [...this.timers]
        .filter(([, atMs]) => atMs <= now + ALARM_EPSILON_MS)
        .sort((a, b) => a[1] - b[1]);
      if (due.length === 0) break;
      for (const [timerId] of due) {
        if (!this.timers.delete(timerId)) continue; // canceled by an earlier dispatch this pass
        if (timerId === HEAT_TIMER) {
          this.flushHeat(now);
        } else if (timerId.startsWith(CLOCK_PREFIX)) {
          await this.clockStep(timerId, now);
        } else {
          await this.dispatch({ t: 'TIMER', timerId, at: now }); // timers are events, not effects
        }
      }
    }
    await this.persistTimers();
    await this.armAlarm(now);
  }

  // ===== engine wiring =====
  private async dispatch(event: GameEvent): Promise<void> {
    if (!this.state) return;
    const prev = this.state;
    const { state, effects } = reduce(this.state, event, this.state.code);
    this.state = state;
    // Persist EVERY state mutation, not just engine SNAPSHOT hints: with WS hibernation
    // the instance can be evicted between any two events, and a consumed timer whose
    // outcome only lived in memory deadlocks the night (state rolls back, timer is gone).
    if (state !== prev) await this.persist();
    await this.runEffects(effects);
  }

  private async runEffects(effects: Effect[]): Promise<void> {
    for (const ef of effects) {
      switch (ef.k) {
        case 'SCHEDULE':
          this.timers.set(ef.timerId, ef.atMs);
          await this.persistTimers();
          await this.armAlarm();
          // Spec 3.3: every deadline broadcast once, in SERVER time; clients derive countdowns.
          this.broadcastRaw({ t: 'AT', timerId: ef.timerId, at: ef.atMs });
          break;
        case 'CANCEL':
          this.timers.delete(ef.timerId);
          await this.persistTimers();
          await this.armAlarm();
          break;
        case 'BROADCAST':
          this.broadcast();
          break;
        case 'SEND':
          this.sendPrivate(ef.to, ef.kind, ef.payload);
          break;
        case 'SNAPSHOT':
          await this.persist();
          break;
        case 'AUDIO':
          this.broadcastRaw({ t: 'AUDIO', sting: ef.sting });
          break;
      }
    }
  }

  // ===== clock sync (spec 3.3, task D-104) =====
  /** Schedule a fresh 5x200ms ping batch + the 60s refresh. Alarm-driven = hibernation-safe. */
  private async startClockBatch(now: number): Promise<void> {
    for (let k = 0; k < PING_BATCH_SIZE; k++) this.timers.set(`${CLOCK_PREFIX}${k}`, now + k * PING_SPACING_MS);
    this.timers.set(CLOCK_REFRESH, now + PING_REFRESH_MS);
    await this.persistTimers();
    await this.armAlarm();
  }

  private async clockStep(timerId: string, now: number): Promise<void> {
    if (this.ctx.getWebSockets().length === 0) return; // idle room: batch resumes on next connect
    if (timerId === CLOCK_REFRESH) {
      await this.startClockBatch(now);
      return;
    }
    const k = Number(timerId.slice(CLOCK_PREFIX.length)) || 0;
    this.pingAll(now, k);
  }

  private pingAll(now: number, k: number): void {
    const pingId = now + k; // distinct per batch slot even when alarms coalesce into one tick
    for (const ws of this.ctx.getWebSockets()) {
      const att = this.att(ws);
      if (!att) continue;
      att.clock = recordPing(att.clock, String(pingId), now);
      ws.serializeAttachment(att);
      this.sendTo(ws, { t: 'PING', id: pingId, sv: now });
    }
  }

  private handlePong(ws: WebSocket, pingId: number, clientClock: number, now: number): void {
    const att = this.att(ws);
    if (!att) return;
    const { clock, offset } = recordPong(att.clock, String(pingId), clientClock, now);
    att.clock = clock;
    if (offset !== null) att.clockOffset = offset; // median of samples -> the socket's offset
    ws.serializeAttachment(att);
  }

  // ===== FIRE / HEAT pacing (spec 3.2, task D-116 groundwork) =====
  private async handleFire(ws: WebSocket, n: number, now: number): Promise<void> {
    const att = this.att(ws);
    if (!att?.playerId) return;
    const { window, allowed } = clampFire(att.fire, n, now);
    att.fire = window;
    ws.serializeAttachment(att);
    if (allowed <= 0) return; // over budget: drop silently
    this.heatPending += allowed;
    if (!this.timers.has(HEAT_TIMER)) {
      this.timers.set(HEAT_TIMER, Math.max(now, this.lastHeatFlush + HEAT_FLUSH_MS)); // <= 4Hz
      await this.persistTimers();
      await this.armAlarm();
    }
    await this.dispatch({ t: 'FIRE', id: att.playerId, n: allowed, at: now });
  }

  private flushHeat(now: number): void {
    if (this.heatPending <= 0) return;
    this.broadcastRaw({ t: 'HEAT', n: this.heatPending });
    this.heatPending = 0;
    this.lastHeatFlush = now;
  }

  // ===== socket plumbing =====
  private att(ws: WebSocket): Attachment | null {
    try {
      return ws.deserializeAttachment() as Attachment | null;
    } catch {
      return null;
    }
  }

  private sendState(ws: WebSocket, replayPrivate = false, now = Date.now()): void {
    if (!this.state) return;
    const att = this.att(ws);
    const playerId = att?.playerId ?? '';
    const view = redactFor(this.state, playerId);
    this.sendTo(ws, { t: 'STATE', s: view, epoch: this.state.epoch });
    // SEND effects are intentionally ephemeral. Rebuild only the viewer's CURRENT
    // assignment on an explicit reconnect/RESYNC so a dropped phone retains its
    // safety window without broadcasting candidates, burns, or historical roles.
    if (replayPrivate) {
      const assignment = spotlightPrivateFor(this.state.spotlight ?? null, playerId, now);
      if (assignment) this.sendTo(ws, { t: 'PRIVATE', k: 'spotlight', p: assignment });
    }
  }

  private broadcast(): void {
    for (const ws of this.ctx.getWebSockets()) this.sendState(ws); // redacted PER viewer (3.4)
  }

  private sendPrivate(playerId: string, kind: string, payload: unknown): void {
    for (const ws of this.ctx.getWebSockets()) {
      if (this.att(ws)?.playerId === playerId) this.sendTo(ws, { t: 'PRIVATE', k: kind, p: payload });
    }
  }

  private broadcastRaw(msg: unknown): void {
    for (const ws of this.ctx.getWebSockets()) this.sendTo(ws, msg);
  }

  private sendTo(ws: WebSocket, msg: unknown): void {
    try {
      ws.send(JSON.stringify(msg));
    } catch {
      /* dead socket; close event will handle */
    }
  }

  // ===== persistence =====
  private async armAlarm(now = Date.now()): Promise<void> {
    const next = Math.min(...this.timers.values());
    // Clamp strictly into the future: re-arming the timestamp that just fired can be
    // deduped as already-handled by the AlarmManager (see ALARM_EPSILON_MS note).
    if (Number.isFinite(next)) await this.ctx.storage.setAlarm(Math.max(next, now + 1));
    else await this.ctx.storage.deleteAlarm();
  }

  private async persistTimers(): Promise<void> {
    await this.ctx.storage.put('timers', Object.fromEntries(this.timers));
  }

  private async persist(): Promise<void> {
    if (!this.state) return;
    await this.ctx.storage.put('room', this.state);
    await this.persistTimers();
  }
}
