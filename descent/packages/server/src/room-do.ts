// RoomDO: the authoritative room. WS hub (hibernation API), Alarm timers, SQLite snapshots.
// DESCENT_BUILD_SPEC.md Part 3 (protocol), Part 4 (engine wiring). Tasks D-102..D-105, D-113.
//
// Alarm multiplexing: this.timers holds BOTH engine deadlines (dispatched back into
// reduce() as TIMER events — timers are events, not effects) and DO-internal timers
// prefixed "do:" (clock ping batches, HEAT flush) that never touch the engine.
import {
  cardPreviewPrivateFor,
  initialRoom,
  reduce,
  spotlightPrivateFor,
  type Effect,
  type GameEvent,
  type ReduceResult,
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
import { resolveEntitlement, validDevice, verifyUnlock, type EntReason } from './entitle.js';
import { parseClientMessage } from './protocol.js';
import { redactFor } from './redact.js';

// Only the fields the RoomDO reads off the environment for entitlement (spec Part 11 / D-412).
interface ServerEnv {
  LEDGER?: DurableObjectNamespace;
  UNLOCK_SECRET?: string;
}

interface Attachment {
  playerId: string; // token = stable identity across reconnects (spec 3.1)
  clockOffset: number; // serverTime - clientTime, median of last 5 samples (spec 3.3)
  clock: ClockState;
  fire: FireWindow; // rolling 10/s FIRE budget
  dev?: string; // host device token (this phone) — entitlement identity; never leaves this socket
  unlock?: string; // device-bound HMAC unlock token, if this phone has paid
}

const CLOCK_PREFIX = 'do:clock:';
const CLOCK_REFRESH = 'do:clock:refresh';
const HEAT_TIMER = 'do:heat';
const LEDGER_TIMEOUT_MS = 3_000;
// Workerd alarms may be admitted a few milliseconds early. Keep the physical wake after
// the logical deadline so the strict pump never returns success after re-arming the exact
// timestamp that the runtime believes it just delivered (the original lost-deal deadlock).
export const ALARM_WAKE_GUARD_MS = 50;
// Inbound traffic is a fallback alarm path, not a competitor to a healthy runtime alarm.
// Give the scheduled callback a short admission window before a PONG/vote/HEARTBEAT steals
// its timer and moves SQLite to the next deadline. A genuinely lost callback is still
// recovered by the next inbound frame.
export const ALARM_RECOVERY_GRACE_MS = 1_000;

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
  // Durable Object handlers may interleave whenever one awaits external I/O. Every room
  // mutation (including alarm delivery) shares this queue so stale reducer previews cannot
  // overwrite newer socket events and SQLite never receives competing writes from one actor.
  private mutationTail: Promise<void> = Promise.resolve();
  // Timer effects dispatched by pump() are committed atomically, but their alarm update is
  // deferred until the pump knows the final nearest deadline. This avoids transient/stale
  // AlarmManager schedules between chained engine transitions.
  private pumping = false;
  // Heat is garnish: pending taps live in memory only — losing them to eviction is fine.
  private heatPending = 0;
  private lastHeatFlush = 0;

  constructor(
    private ctx: DurableObjectState,
    private env: unknown,
  ) {}

  private serializeMutation<T>(operation: () => Promise<T>): Promise<T> {
    const result = this.mutationTail.then(operation);
    this.mutationTail = result.then(
      () => undefined,
      () => undefined,
    );
    return result;
  }

  private async load(): Promise<void> {
    if (this.state) return;
    this.state = ((await this.ctx.storage.get('room')) as RoomState | undefined) ?? null;
    this.timers = new Map(Object.entries(((await this.ctx.storage.get('timers')) as Record<string, number>) ?? {}));
    if (!this.state) return;

    // Current commits batch state + timer index atomically. Keep this reconciliation for
    // rooms written by older deployments (and defensive recovery from damaged local data):
    // moduleTimers is the engine's durable source of truth for module-owned deadlines.
    let restored = false;
    for (const [timerId, timer] of Object.entries(this.state.moduleTimers)) {
      if (this.timers.has(timerId)) continue;
      this.timers.set(timerId, timer.atMs);
      restored = true;
    }
    if (restored) {
      await this.persistTimers();
      await this.armAlarm();
    }
  }

  fetch(req: Request): Promise<Response> {
    return this.serializeMutation(() => this.fetchLocked(req));
  }

  private async fetchLocked(req: Request): Promise<Response> {
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
      // The host phone rides its device + unlock token on its OWN socket so entitlement is
      // resolved at BEGIN without ever putting either on the wire to other players (Part 11).
      const dev = url.searchParams.get('dev') ?? undefined;
      const unlock = url.searchParams.get('unlock') ?? undefined;
      const pair = new WebSocketPair();
      const [client, server] = Object.values(pair) as [WebSocket, WebSocket];
      // Hibernation API: survives DO eviction; attachment carries identity + clock state.
      this.ctx.acceptWebSocket(server);
      await this.handleOpen(server, token, Date.now(), dev, unlock);
      return new Response(null, { status: 101, webSocket: client });
    }

    return new Response('bad request', { status: 400 });
  }

  /** Spec 3.1: WELCOME (with token) -> STATE; known token = silent reseat, else JOIN msg follows. */
  private async handleOpen(ws: WebSocket, token: string, now: number, dev?: string, unlock?: string): Promise<void> {
    if (!this.state) return;
    const att: Attachment = {
      playerId: token,
      clockOffset: 0,
      clock: emptyClock(),
      fire: { start: 0, used: 0 },
      dev,
      unlock,
    };
    ws.serializeAttachment(att);
    // Seed client/server clock alignment before STATE or any absolute-deadline
    // PRIVATE payload. The follow-up ping batch refines this one-way sample.
    this.sendTo(ws, { t: 'WELCOME', you: token, token, code: this.state.code, sv: now });
    if (this.state.players.some((p) => p.id === token)) {
      // Silent reconnect: the engine marks the seat live again (D-111 completes the rule).
      await this.dispatch({ t: 'RECONNECT', id: token, at: now });
    }
    this.sendState(ws, true, now);
    await this.syncClockOnOpen(ws, now); // 5 pings x 200ms on connect (3.3)
  }

  webSocketMessage(ws: WebSocket, raw: string | ArrayBuffer): Promise<void> {
    return this.serializeMutation(() => this.webSocketMessageLocked(ws, raw));
  }

  private async webSocketMessageLocked(ws: WebSocket, raw: string | ArrayBuffer): Promise<void> {
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

    // Entitlement is re-resolved against the host's DEVICE ledger at every LOBBY BEGIN —
    // never cached at room init — so the paywall lands on the device's SECOND night whether
    // that is a reset of this room or a brand-new room (spec Part 11 / D-412).
    let beginReason: EntReason | 'unavailable' | null = null;
    if (msg.t === 'BEGIN' && this.state.phase.k === 'LOBBY' && me?.role === 'host' && att) {
      const ent = await this.resolveEntitlement(att);
      beginReason = ent.reason;
      if (this.state.entitled !== ent.entitled) {
        this.state = { ...this.state, entitled: ent.entitled };
        await this.persist();
      }
    }

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
        if (parsed.code === 'NO_ENTITLEMENT' && beginReason === 'unavailable') {
          this.sendTo(ws, {
            t: 'ERR',
            code: 'ENTITLEMENT_UNAVAILABLE',
            msg: 'the tollkeeper blinked; try BEGIN again',
          });
        } else {
          this.sendTo(ws, { t: 'ERR', code: parsed.code, msg: parsed.msg });
        }
        break;
      case 'heartbeat':
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
      case 'event': {
        const wasLobby = this.state.phase.k === 'LOBBY';
        if (parsed.event.t === 'BEGIN' && beginReason === 'free-night' && wasLobby && att) {
          // Preview the pure reducer result first. A rejected BEGIN (too few sinners,
          // unset ceilings) never claims the device's free night. A valid start claims
          // the shared device ledger BEFORE its effects become visible, closing the race
          // where two rooms both observed an unused free night.
          const result = reduce(this.state, parsed.event, this.state.code);
          if (result.state.phase.k === 'LOBBY') {
            await this.commitResult(this.state, result);
            break;
          }
          const granted = await this.claimFreeNight(att);
          if (granted !== true) {
            this.state = { ...this.state, entitled: false };
            await this.persist();
            this.sendTo(
              ws,
              granted === null
                ? { t: 'ERR', code: 'ENTITLEMENT_UNAVAILABLE', msg: 'the tollkeeper blinked; try BEGIN again' }
                : { t: 'ERR', code: 'NO_ENTITLEMENT', msg: 'host unlock required for this night' },
            );
            break;
          }
          await this.commitResult(this.state, result);
        } else {
          await this.dispatch(parsed.event);
        }
        break;
      }
    }

    // Self-healing: if the runtime lost an alarm (workerd AlarmManager races), any
    // inbound frame — including the independent 15s heartbeat — processes the overdue
    // timer. Read the clock again because entitlement/engine work above may have awaited.
    const pumpNow = Date.now();
    const next = Math.min(...this.timers.values());
    if (Number.isFinite(next) && next + ALARM_RECOVERY_GRACE_MS <= pumpNow) await this.pump(pumpNow);
  }

  webSocketClose(ws: WebSocket): Promise<void> {
    return this.serializeMutation(() => this.webSocketCloseLocked(ws));
  }

  private async webSocketCloseLocked(ws: WebSocket): Promise<void> {
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

  alarm(): Promise<void> {
    return this.serializeMutation(() => this.alarmLocked());
  }

  private async alarmLocked(): Promise<void> {
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
    this.pumping = true;
    try {
      for (let pass = 0; pass < 8; pass++) {
        const due = [...this.timers].filter(([, atMs]) => atMs <= now).sort((a, b) => a[1] - b[1]);
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
    } finally {
      this.pumping = false;
    }
    await this.persistTimers();
    // A pump consumed (or inspected) the physical wake that brought it here. Replace any
    // stale/overdue storage alarm with the one true successor even when HEARTBEAT recovered it.
    await this.armAlarm(now, true);
  }

  // ===== entitlement (spec Part 11 / D-412) =====
  /** Peek — never charge — the host device's entitlement: a valid unlock always plays; else
   *  the device's free night if unspent; else locked. The ledger is consulted only when there
   *  is no unlock to check. */
  private async resolveEntitlement(
    att: Attachment,
  ): Promise<{ entitled: boolean; reason: EntReason | 'unavailable' }> {
    if (!validDevice(att.dev)) return { entitled: false, reason: 'locked' };
    const env = this.env as ServerEnv;
    const unlocked = await verifyUnlock(env.UNLOCK_SECRET, att.dev, att.unlock);
    let freeNightUsed = false;
    if (!unlocked) {
      const status = (await this.ledgerFetch(
        att.dev,
        'GET',
        `/status?claim=${encodeURIComponent(this.freeNightClaimId())}`,
      )) as { freeNightUsed?: boolean; claimMatches?: boolean } | null;
      if (status === null) return { entitled: false, reason: 'unavailable' };
      freeNightUsed = status.freeNightUsed === true && status.claimMatches !== true;
    }
    return resolveEntitlement({ unlocked, freeNightUsed });
  }

  private async claimFreeNight(att: Attachment): Promise<boolean | null> {
    const claim = (await this.ledgerFetch(
      att.dev,
      'POST',
      `/consume-free?claim=${encodeURIComponent(this.freeNightClaimId())}`,
    )) as { granted?: boolean } | null;
    return claim === null ? null : claim.granted === true;
  }

  private freeNightClaimId(): string {
    if (!this.state) return 'NONE:0';
    return `${this.state.code}:${this.state.epoch}`;
  }

  /** Call the per-device LedgerDO with a hard deadline. Paid devices verify locally and never
   *  enter this path; an uncertain unpaid claim is retryable but must not mint an unrecorded night. */
  private async ledgerFetch(dev: string | undefined, method: string, path: string): Promise<unknown> {
    const env = this.env as ServerEnv;
    if (!dev || !env.LEDGER) return null;
    let timeout: ReturnType<typeof setTimeout> | null = null;
    try {
      const stub = env.LEDGER.get(env.LEDGER.idFromName(dev));
      const timedOut = new Promise<never>((_, reject) => {
        timeout = setTimeout(() => reject(new Error('ledger timeout')), LEDGER_TIMEOUT_MS);
      });
      const res = await Promise.race([stub.fetch(new Request(`https://ledger${path}`, { method })), timedOut]);
      if (!res.ok) return null;
      return await res.json();
    } catch {
      return null;
    } finally {
      if (timeout !== null) clearTimeout(timeout);
    }
  }

  // ===== engine wiring =====
  private async dispatch(event: GameEvent): Promise<void> {
    if (!this.state) return;
    const prev = this.state;
    await this.commitResult(prev, reduce(this.state, event, this.state.code));
  }

  private async commitResult(prev: RoomState, { state, effects }: ReduceResult): Promise<void> {
    this.state = state;
    const timersChanged = this.applyTimerEffects(effects);
    // Persist EVERY state mutation, not just engine SNAPSHOT hints: with WS hibernation
    // the instance can be evicted between any two events. State and its resulting timer
    // index land in one atomic storage batch, so no durable phase can exist without the
    // core/module deadline that advances it.
    if (state !== prev || timersChanged) await this.persist();
    if (timersChanged) await this.armAlarm();
    await this.emitEffects(effects);
  }

  private async runEffects(effects: Effect[]): Promise<void> {
    const timersChanged = this.applyTimerEffects(effects);
    if (timersChanged) {
      await this.persist();
      await this.armAlarm();
    }
    await this.emitEffects(effects);
  }

  private applyTimerEffects(effects: readonly Effect[]): boolean {
    let changed = false;
    for (const ef of effects) {
      if (ef.k === 'SCHEDULE') {
        if (this.timers.get(ef.timerId) !== ef.atMs) changed = true;
        this.timers.set(ef.timerId, ef.atMs);
      } else if (ef.k === 'CANCEL' && this.timers.delete(ef.timerId)) {
        changed = true;
      }
    }
    return changed;
  }

  private async emitEffects(effects: readonly Effect[]): Promise<void> {
    for (const ef of effects) {
      switch (ef.k) {
        case 'SCHEDULE':
          // Public deadlines are broadcast once in server time. Hidden mechanics such
          // as Scatter's variable fuse still use the same durable scheduler, but never
          // reveal their exact deadline or timer identity to clients.
          if (ef.announce !== false) this.broadcastRaw({ t: 'AT', timerId: ef.timerId, at: ef.atMs });
          break;
        case 'CANCEL':
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
  /** Rapid joins share the live batch instead of repeatedly canceling/rearming its immediate
   * alarm (which local Workerd reports as requestScheduledAlarm noise). A late join gets a
   * fresh batch; a socket entering mid-batch also receives an immediate personal sample. */
  private async syncClockOnOpen(ws: WebSocket, now: number): Promise<void> {
    const batchPending = [...this.timers.keys()].some(
      (timerId) => timerId !== CLOCK_REFRESH && timerId.startsWith(CLOCK_PREFIX),
    );
    if (batchPending) {
      this.pingSocket(ws, now, now + 0.5); // half-ms id cannot collide with integer alarm-batch ids
      return;
    }
    await this.startClockBatch(now);
  }

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
    for (const ws of this.ctx.getWebSockets()) this.pingSocket(ws, now, pingId);
  }

  private pingSocket(ws: WebSocket, now: number, pingId: number): void {
    const att = this.att(ws);
    if (!att) return;
    att.clock = recordPing(att.clock, String(pingId), now);
    ws.serializeAttachment(att);
    this.sendTo(ws, { t: 'PING', id: pingId, sv: now });
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
    await this.dispatch({ t: 'FIRE', id: att.playerId, n: allowed, at: now });

    // The first flush in a pacing window is already due, so emit it directly instead of
    // scheduling an immediate alarm and then consuming that alarm from the same request.
    // Subsequent taps share one future alarm, keeping HEAT at <= 4Hz.
    if (now >= this.lastHeatFlush + HEAT_FLUSH_MS) {
      this.flushHeat(now);
    } else if (!this.timers.has(HEAT_TIMER)) {
      this.timers.set(HEAT_TIMER, this.lastHeatFlush + HEAT_FLUSH_MS);
      await this.persistTimers();
      await this.armAlarm();
    }
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
    // assignment/preview on an explicit reconnect/RESYNC so a dropped phone retains
    // its safety window without broadcasting candidates, burns, or private content.
    if (replayPrivate) {
      const assignment = spotlightPrivateFor(this.state.spotlight ?? null, playerId, now);
      if (assignment) this.sendTo(ws, { t: 'PRIVATE', k: 'spotlight', p: assignment });
      const canBurn = (this.state.players.find((player) => player.id === playerId)?.brimstones ?? 0) > 0;
      const preview = cardPreviewPrivateFor(this.state.deal, playerId, now, canBurn);
      if (preview) this.sendTo(ws, { t: 'PRIVATE', k: 'preview', p: preview });
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
  private async armAlarm(now = Date.now(), force = false): Promise<void> {
    if (this.pumping) return;
    const next = Math.min(...this.timers.values());
    const current = await this.ctx.storage.getAlarm();
    if (Number.isFinite(next)) {
      const target = Math.max(next + ALARM_WAKE_GUARD_MS, now + 1);
      if (current === target) return;

      // Outside pump(), an existing earlier wake is sufficient. Preserve it instead of
      // moving SQLite later while its callback may already be queued; that harmless wake
      // will inspect the current logical timer map and force-arm the true next deadline.
      // A materially overdue alarm is no longer useful and may be replaced for recovery.
      if (!force && current !== null && current <= target && current > now - ALARM_RECOVERY_GRACE_MS) return;
      await this.ctx.storage.setAlarm(target);
    } else if (current !== null && (force || current <= now - ALARM_RECOVERY_GRACE_MS)) {
      // Preserve an imminent earlier wake after external cancellation for the same reason;
      // pump() will delete it. An admitted or clearly lost alarm can be deleted immediately.
      await this.ctx.storage.deleteAlarm();
    }
  }

  private async persistTimers(): Promise<void> {
    await this.ctx.storage.put('timers', Object.fromEntries(this.timers));
  }

  private async persist(): Promise<void> {
    if (!this.state) return;
    await this.ctx.storage.put({ room: this.state, timers: Object.fromEntries(this.timers) });
  }
}
