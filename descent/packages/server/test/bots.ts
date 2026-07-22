// bots.ts — N headless WS bots play a FULL mixed night against a live server.
// Spec Part 11 ("bot nights"). Each bot is a thin renderer: it reads its own
// per-socket redacted gameView and taps what a real phone would, via the shared
// botlogic.botMoves decision function — the SAME code the protocol.test.ts unit
// night drives. So this script is the live twin of that test: it exercises every
// registered game end-to-end (all nine shipping decks at depth 9, including both
// BLOCKING truth inputs and the core-owned private spotlight ceremonies).
//
// RUN (tsx is a pinned, direct dev dependency of this package):
//   terminal 1:  pnpm dev                         # wrangler dev on http://127.0.0.1:8787
//   terminal 2:  pnpm smoke:live
//                env: HELLDECK_URL=... HELLDECK_N=6 HELLDECK_DEPTH=9
//                     HELLDECK_TIMEOUT_MS=900000
//
// PASS requires every bot to see JUDGMENT, every configured circle to have been
// observed, and (when present in the arc) all three Scatter BOMB/BOOM loops.
import { botMoves, deckOf, subOf, type RosterMember, type WireMsg } from './botlogic.js';

declare const process: {
  env: Record<string, string | undefined>;
  exit(code: number): never;
};

const BASE = process.env['HELLDECK_URL'] ?? 'http://127.0.0.1:8787';
const WS_BASE = BASE.replace(/^http/, 'ws');
const N = Math.max(3, Math.min(12, Number(process.env['HELLDECK_N']) || 6));
const DEPTH = ([5, 7, 9].includes(Number(process.env['HELLDECK_DEPTH'])) ? Number(process.env['HELLDECK_DEPTH']) : 9) as 5 | 7 | 9;
const DEFAULT_TIMEOUT_MS = 900_000;
const ROOM_FETCH_TIMEOUT_MS = 10_000;
const WS_CONNECT_TIMEOUT_MS = 10_000;
const WS_CLOSE_GRACE_MS = 2_000;
const HEARTBEAT_MS = 15_000;
const TICK_MS = 400;
const BOMB_WATCHDOG_MS = [60_000, 65_000, 70_000] as const;
const POOL = ['HOST', 'ASH', 'BLAZE', 'CINDER', 'EMBER', 'SOOT', 'GRIME', 'RASH', 'MOLD', 'BILE', 'SCAB', 'CRUD'];
const NAMES = POOL.slice(0, N);
const RUN_DEVICE_NONCE = crypto.randomUUID().replaceAll('-', '');

function durationFromEnv(name: string, fallback: number): number {
  const raw = process.env[name];
  if (raw === undefined) return fallback;
  const value = Number(raw);
  if (!Number.isFinite(value) || !Number.isInteger(value) || value <= 0) {
    throw new Error(`${name} must be a positive integer number of milliseconds (received ${JSON.stringify(raw)})`);
  }
  return value;
}

const TIMEOUT_MS = durationFromEnv('HELLDECK_TIMEOUT_MS', DEFAULT_TIMEOUT_MS);

type Frame = { t: string; [k: string]: unknown };
type StateView = {
  you: string;
  config: unknown;
  players: RosterMember[];
  gameView: unknown;
  phase: { k: string; sub?: string; circle?: number; deadline?: number | null };
  circleIdx: number;
  arcLength: number;
};

const PHASES = new Set(['LOBBY', 'CONSENT', 'CIRCLE_INTRO', 'DEAL', 'INPUT', 'WAITING_ON', 'REVEAL', 'LADDER', 'JUDGMENT']);
const CIRCLE_PHASES = new Set(['CIRCLE_INTRO', 'DEAL', 'INPUT', 'WAITING_ON', 'REVEAL', 'LADDER']);

const recordOf = (value: unknown): Record<string, unknown> | null =>
  typeof value === 'object' && value !== null && !Array.isArray(value) ? (value as Record<string, unknown>) : null;
const isNonNegativeInteger = (value: unknown): value is number => Number.isInteger(value) && Number(value) >= 0;
const isFiniteNumber = (value: unknown): value is number => typeof value === 'number' && Number.isFinite(value);
const errorMessage = (error: unknown): string => (error instanceof Error ? error.message : String(error));

function requiredString(record: Record<string, unknown>, key: string): string {
  const value = record[key];
  if (typeof value !== 'string' || value.length === 0) throw new Error(`${key} must be a non-empty string`);
  return value;
}

function requiredFiniteNumber(record: Record<string, unknown>, key: string): number {
  const value = record[key];
  if (!isFiniteNumber(value)) throw new Error(`${key} must be a finite number`);
  return value;
}

function parseStateView(value: unknown): StateView {
  const state = recordOf(value);
  if (!state) throw new Error('STATE.s must be an object');

  const phaseRecord = recordOf(state['phase']);
  if (!phaseRecord) throw new Error('STATE.s.phase must be an object');
  const phaseKey = requiredString(phaseRecord, 'k');
  if (!PHASES.has(phaseKey)) throw new Error(`STATE.s.phase.k is unknown: ${phaseKey}`);

  const phase: StateView['phase'] = { k: phaseKey };
  if (phaseRecord['sub'] !== undefined) {
    if (typeof phaseRecord['sub'] !== 'string') throw new Error('STATE.s.phase.sub must be a string');
    phase.sub = phaseRecord['sub'];
  }
  if (phaseRecord['circle'] !== undefined) {
    if (!isNonNegativeInteger(phaseRecord['circle'])) throw new Error('STATE.s.phase.circle must be a non-negative integer');
    phase.circle = phaseRecord['circle'];
  }
  if (CIRCLE_PHASES.has(phaseKey) && phase.circle === undefined) {
    throw new Error(`STATE.s.phase.circle is required during ${phaseKey}`);
  }
  if (phaseRecord['deadline'] !== undefined) {
    const deadline = phaseRecord['deadline'];
    if (deadline !== null && !isFiniteNumber(deadline)) throw new Error('STATE.s.phase.deadline must be finite or null');
    phase.deadline = deadline;
  }

  if (!Array.isArray(state['players'])) throw new Error('STATE.s.players must be an array');
  const players = state['players'].map((value, index): RosterMember => {
    const player = recordOf(value);
    if (!player) throw new Error(`STATE.s.players[${index}] must be an object`);
    const id = requiredString(player, 'id');
    if (player['role'] !== undefined && typeof player['role'] !== 'string') {
      throw new Error(`STATE.s.players[${index}].role must be a string`);
    }
    return typeof player['role'] === 'string' ? { id, role: player['role'] } : { id };
  });

  if (!isNonNegativeInteger(state['circleIdx'])) throw new Error('STATE.s.circleIdx must be a non-negative integer');
  if (!isNonNegativeInteger(state['arcLength'])) throw new Error('STATE.s.arcLength must be a non-negative integer');
  if (!Object.hasOwn(state, 'config')) throw new Error('STATE.s.config is missing');
  if (!Object.hasOwn(state, 'gameView')) throw new Error('STATE.s.gameView is missing');

  return {
    you: requiredString(state, 'you'),
    config: state['config'],
    players,
    gameView: state['gameView'],
    phase,
    circleIdx: state['circleIdx'],
    arcLength: state['arcLength'],
  };
}

/** A one-shot async failure signal for socket event handlers and the global timer. */
class FailureLatch {
  private failure: Error | null = null;
  private readonly listeners = new Set<(error: Error) => void>();

  fail(error: Error): void {
    if (this.failure) return;
    this.failure = error;
    for (const listener of this.listeners) listener(error);
    this.listeners.clear();
  }

  throwIfFailed(): void {
    if (this.failure) throw this.failure;
  }

  race<T>(work: Promise<T>): Promise<T> {
    if (this.failure) return Promise.reject(this.failure);
    return new Promise<T>((resolve, reject) => {
      const onFailure = (error: Error): void => {
        this.listeners.delete(onFailure);
        reject(error);
      };
      this.listeners.add(onFailure);
      work.then(
        (value) => {
          this.listeners.delete(onFailure);
          resolve(value);
        },
        (error: unknown) => {
          this.listeners.delete(onFailure);
          reject(error);
        },
      );
    });
  }
}

type ActiveBomb = { key: string; circle: number; loop: 0 | 1 | 2; startedAt: number };

class ProgressTracker {
  readonly circles = new Set<number>();
  readonly bombLoops = new Set<number>();
  readonly boomLoops = new Set<number>();
  sawScatter = false;
  private activeBomb: ActiveBomb | null = null;
  private lastLogKey = '';
  private readonly startedAt: number;

  constructor(startedAt: number) {
    this.startedAt = startedAt;
  }

  observe(view: StateView, epoch: number): void {
    if (CIRCLE_PHASES.has(view.phase.k) && view.phase.circle !== undefined) this.circles.add(view.phase.circle);

    const game = recordOf(view.gameView);
    const deck = deckOf(view.gameView);
    const sub = subOf(view.gameView) ?? view.phase.sub ?? null;
    let scatterLoop: number | null = null;
    let scatterLoops: number | null = null;

    if (deck === 'scatter') {
      this.sawScatter = true; // INTRO names the deck, so this detects Scatter's presence in the arc.
      const loopValue = game?.['loop'];
      const loopsValue = game?.['loops'];
      if (loopValue !== undefined) {
        if (!isNonNegativeInteger(loopValue)) throw new Error('Scatter gameView.loop must be a non-negative integer');
        scatterLoop = loopValue;
      }
      if (loopsValue !== undefined) {
        if (!isNonNegativeInteger(loopsValue)) throw new Error('Scatter gameView.loops must be a non-negative integer');
        scatterLoops = loopsValue;
      }
      if (sub === 'BOMB' || sub === 'BOOM') {
        if (scatterLoop !== 0 && scatterLoop !== 1 && scatterLoop !== 2) {
          throw new Error(`Scatter ${sub} carried invalid loop ${String(scatterLoop)}`);
        }
        if (scatterLoops !== 3) throw new Error(`Scatter ${sub} carried invalid loops ${String(scatterLoops)}`);
        (sub === 'BOMB' ? this.bombLoops : this.boomLoops).add(scatterLoop);
      }
    }

    const now = performance.now();
    if (deck === 'scatter' && sub === 'BOMB' && (scatterLoop === 0 || scatterLoop === 1 || scatterLoop === 2)) {
      const key = `${view.circleIdx}:${scatterLoop}`;
      if (this.activeBomb?.key !== key) this.activeBomb = { key, circle: view.circleIdx, loop: scatterLoop, startedAt: now };
    } else {
      this.activeBomb = null;
    }

    const logKey = [epoch, view.phase.k, view.circleIdx, view.arcLength, deck, sub, scatterLoop, scatterLoops].join('|');
    if (logKey !== this.lastLogKey) {
      const elapsed = ((now - this.startedAt) / 1000).toFixed(1).padStart(6, ' ');
      const loopLabel = scatterLoop === null ? '-' : `${scatterLoop}/${scatterLoops ?? '?'}`;
      console.log(
        `[+${elapsed}s] epoch=${epoch} phase=${view.phase.k} circle=${view.circleIdx}/${view.arcLength || '-'} ` +
          `deck=${deck ?? '-'} sub=${sub ?? '-'} scatterLoop=${loopLabel}`,
      );
      this.lastLogKey = logKey;
    }
  }

  assertBombWatchdog(): void {
    if (!this.activeBomb) return;
    const elapsed = performance.now() - this.activeBomb.startedAt;
    const limit = BOMB_WATCHDOG_MS[this.activeBomb.loop];
    if (elapsed >= limit) {
      throw new Error(
        `Scatter BOMB watchdog expired: circle=${this.activeBomb.circle} loop=${this.activeBomb.loop} ` +
          `remained open for ${(elapsed / 1000).toFixed(1)}s (limit ${limit / 1000}s)`,
      );
    }
  }

  assertPass(): void {
    const expectedCircles = Array.from({ length: DEPTH }, (_, index) => index);
    const observedCircles = [...this.circles].sort((a, b) => a - b);
    if (observedCircles.length !== DEPTH || observedCircles.some((circle, index) => circle !== expectedCircles[index])) {
      throw new Error(`PASS invariant failed: expected circles ${expectedCircles.join(',')}; observed ${observedCircles.join(',') || 'none'}`);
    }
    if (!this.sawScatter) return; // Arc construction may legitimately omit Scatter.

    const expectedLoops = '0,1,2';
    const bombs = [...this.bombLoops].sort((a, b) => a - b).join(',');
    const booms = [...this.boomLoops].sort((a, b) => a - b).join(',');
    if (bombs !== expectedLoops || booms !== expectedLoops) {
      throw new Error(`PASS invariant failed: Scatter expected BOMB/BOOM loops ${expectedLoops}; observed BOMB=${bombs || 'none'} BOOM=${booms || 'none'}`);
    }
  }
}

class Bot {
  private ws: WebSocket | null = null;
  private opened = false;
  private closing = false;
  private heartbeatTimer: ReturnType<typeof setInterval> | null = null;
  view: StateView | null = null;
  epoch = -1;
  joined = false;
  consented = false;
  lastBeginAt = 0;
  readonly name: string;
  readonly isHost: boolean;
  private readonly deviceToken: string;
  private readonly fail: (error: Error) => void;
  private readonly onState?: (view: StateView, epoch: number) => void;

  // no TS parameter properties: keep the file friendly to strip-only TS runners
  constructor(
    name: string,
    isHost: boolean,
    deviceToken: string,
    fail: (error: Error) => void,
    onState?: (view: StateView, epoch: number) => void,
  ) {
    this.name = name;
    this.isHost = isHost;
    this.deviceToken = deviceToken;
    this.fail = fail;
    this.onState = onState;
  }

  connect(code: string): Promise<void> {
    return new Promise((resolve, reject) => {
      let settled = false;
      let connectTimer: ReturnType<typeof setTimeout> | null = null;
      const clearConnectTimer = (): void => {
        if (connectTimer !== null) clearTimeout(connectTimer);
        connectTimer = null;
      };
      const rejectBeforeOpen = (error: Error): void => {
        if (settled) return;
        settled = true;
        clearConnectTimer();
        reject(error);
      };

      this.ws = new WebSocket(`${WS_BASE}/ws/${code}?v=1&dev=${this.deviceToken}`);
      this.ws.addEventListener('open', () => {
        if (settled || this.closing) {
          try {
            this.ws?.close(1000, 'harness stopping');
          } catch {
            // The connection is already unusable; the original failure is authoritative.
          }
          rejectBeforeOpen(new Error(`${this.name}: connection cancelled before open`));
          return;
        }
        settled = true;
        clearConnectTimer();
        this.opened = true;
        this.heartbeatTimer = setInterval(() => this.send({ t: 'HEARTBEAT' }), HEARTBEAT_MS);
        resolve();
      });
      this.ws.addEventListener('error', () => {
        const error = new Error(`${this.name}: WebSocket error`);
        if (!this.opened) rejectBeforeOpen(error);
        else if (!this.closing) this.fail(error);
      });
      this.ws.addEventListener('close', (event) => {
        const close = event as CloseEvent;
        if (!this.opened) {
          rejectBeforeOpen(new Error(`${this.name}: WebSocket closed before open (code=${close.code})`));
        } else if (!this.closing) {
          this.fail(new Error(`${this.name}: WebSocket closed after open (code=${close.code}, reason=${close.reason || '-'})`));
        }
        this.opened = false;
        this.stopHeartbeat();
      });
      this.ws.addEventListener('message', (event) => {
        try {
          if (typeof event.data !== 'string') throw new Error('frame payload must be text');
          const parsed: unknown = JSON.parse(event.data);
          const frame = recordOf(parsed);
          if (!frame || typeof frame['t'] !== 'string' || frame['t'].length === 0) {
            throw new Error('frame must be an object with a non-empty t');
          }
          this.onFrame(frame as Frame);
        } catch (error: unknown) {
          this.fail(new Error(`${this.name}: malformed server frame: ${errorMessage(error)}`));
        }
      });

      connectTimer = setTimeout(() => {
        rejectBeforeOpen(new Error(`${this.name}: WebSocket connection timed out after ${WS_CONNECT_TIMEOUT_MS}ms`));
        try {
          this.ws?.close(1000, 'connect timeout');
        } catch {
          // Some WebSocket implementations reject close() while still CONNECTING.
        }
      }, WS_CONNECT_TIMEOUT_MS);
    });
  }

  send(msg: Record<string, unknown>): void {
    if (!this.ws || this.ws.readyState !== WebSocket.OPEN) return;
    try {
      this.ws.send(JSON.stringify(msg));
    } catch (error: unknown) {
      this.fail(new Error(`${this.name}: WebSocket send failed: ${errorMessage(error)}`));
    }
  }

  async close(): Promise<void> {
    this.closing = true;
    this.stopHeartbeat();
    const ws = this.ws;
    if (!ws || ws.readyState === WebSocket.CLOSED) return;

    await new Promise<void>((resolve) => {
      let done = false;
      const finish = (): void => {
        if (done) return;
        done = true;
        clearTimeout(graceTimer);
        resolve();
      };
      const graceTimer = setTimeout(finish, WS_CLOSE_GRACE_MS);
      ws.addEventListener('close', finish, { once: true });
      try {
        ws.close(1000, 'live harness complete');
      } catch {
        finish();
      }
    });
  }

  private stopHeartbeat(): void {
    if (this.heartbeatTimer !== null) clearInterval(this.heartbeatTimer);
    this.heartbeatTimer = null;
  }

  private onFrame(frame: Frame): void {
    switch (frame.t) {
      case 'PING':
        requiredFiniteNumber(frame, 'id');
        requiredFiniteNumber(frame, 'sv');
        this.send({ t: 'PONG', id: frame['id'], cl: Date.now() }); // answer every PING immediately (3.1)
        break;
      case 'STATE': {
        const epoch = requiredFiniteNumber(frame, 'epoch');
        if (!Number.isInteger(epoch) || epoch < 0) throw new Error('STATE.epoch must be a non-negative integer');
        const view = parseStateView(frame['s']);
        if (epoch < this.epoch) return; // stale
        this.epoch = epoch;
        this.view = view;
        this.onState?.(view, epoch);
        break;
      }
      case 'ERR':
        this.fail(new Error(`${this.name}: protocol ERR ${String(frame['code'])}: ${String(frame['msg'])}`));
        break;
      case 'WELCOME':
        requiredString(frame, 'you');
        requiredString(frame, 'token');
        requiredString(frame, 'code');
        requiredFiniteNumber(frame, 'sv');
        break;
      case 'AT':
        requiredString(frame, 'timerId');
        requiredFiniteNumber(frame, 'at');
        break;
      case 'AUDIO':
        requiredString(frame, 'sting');
        break;
      case 'HEAT':
        requiredFiniteNumber(frame, 'n');
        break;
      case 'PRIVATE':
        requiredString(frame, 'k');
        if (!Object.hasOwn(frame, 'p')) throw new Error('PRIVATE.p is missing');
        break;
      default:
        throw new Error(`unknown frame type ${frame.t}`);
    }
  }

  /** One decision tick. Returns true once this bot has seen JUDGMENT. */
  tick(): boolean {
    const view = this.view;
    if (!view) return false;
    const phase = view.phase.k;
    const meIn = view.players.some((player) => player.id === view.you);

    if (!this.joined) {
      this.send({ t: 'JOIN', name: this.name, avatar: Math.floor(Math.random() * 16) });
      this.joined = true;
      return false;
    }
    if (!this.consented && meIn) {
      this.send({ t: 'CEILING', v: 5 }); // ceiling high so the deck has room to climb (invisible either way)
      this.send({ t: 'ATTEST18' });
      this.consented = true;
    }

    if (phase === 'LOBBY' || phase === 'CONSENT') {
      if (this.isHost) {
        const everyoneIn = view.players.length >= NAMES.length;
        if (everyoneIn && !view.config) this.send({ t: 'CONFIG', depth: DEPTH, vibe: 'feral', stage: N >= 5 });
        if (everyoneIn && view.config && Date.now() - this.lastBeginAt > 2000) {
          this.send({ t: 'BEGIN' }); // retried until the phase moves
          this.lastBeginAt = Date.now();
        }
      }
      return false;
    }

    // In a circle: every action is a decision off THIS bot's own redacted gameView —
    // exactly what a real phone sees. botMoves covers every registered game and both
    // blocking inputs; PRIVATE notices need no bot action unless a burn is being tested.
    for (const move of botMoves(view.gameView, view.you, view.players, this.isHost) as WireMsg[]) this.send(move);

    return phase === 'JUDGMENT';
  }
}

const sleep = (ms: number): Promise<void> => new Promise((resolve) => setTimeout(resolve, ms));

async function createRoom(): Promise<string> {
  const controller = new AbortController();
  let timedOut = false;
  const timeout = setTimeout(() => {
    timedOut = true;
    controller.abort();
  }, ROOM_FETCH_TIMEOUT_MS);

  try {
    const response = await fetch(`${BASE}/api/room`, { method: 'POST', signal: controller.signal });
    if (!response.ok) throw new Error(`POST /api/room -> ${response.status}`);
    const body: unknown = await response.json();
    const record = recordOf(body);
    if (!record) throw new Error('POST /api/room returned a non-object body');
    return requiredString(record, 'code');
  } catch (error: unknown) {
    if (timedOut) throw new Error(`POST /api/room timed out after ${ROOM_FETCH_TIMEOUT_MS}ms`);
    throw error;
  } finally {
    clearTimeout(timeout);
  }
}

function dumpViews(bots: readonly Bot[]): void {
  if (bots.length === 0) return;
  console.error('Last known bot views:');
  for (const bot of bots) {
    const game = recordOf(bot.view?.gameView);
    console.error(
      `  ${bot.name}: phase=${bot.view?.phase.k ?? 'none'} circle=${bot.view?.circleIdx ?? '-'} ` +
        `deck=${deckOf(bot.view?.gameView) ?? '-'} sub=${subOf(bot.view?.gameView) ?? '-'} ` +
        `loop=${String(game?.['loop'] ?? '-')} epoch=${bot.epoch}`,
    );
  }
}

async function main(): Promise<void> {
  const startedAt = performance.now();
  const failures = new FailureLatch();
  const progress = new ProgressTracker(startedAt);
  const bots: Bot[] = [];
  const globalTimer = setTimeout(
    () => failures.fail(new Error(`global timeout expired after ${TIMEOUT_MS}ms`)),
    TIMEOUT_MS,
  );

  try {
    console.log(`creating room at ${BASE} — N=${N} depth=${DEPTH} timeout=${TIMEOUT_MS}ms`);
    const code = await failures.race(createRoom());
    console.log(`room ${code} — connecting ${NAMES.length} bots`);

    for (const [index, name] of NAMES.entries()) {
      // Entitlement is device-scoped. A per-run token gives every smoke a fresh free
      // night while remaining stable for this bot's full socket lifetime/reconnects.
      const deviceToken = `bot${RUN_DEVICE_NONCE}${index.toString(36)}`;
      const bot = new Bot(
        name,
        index === 0,
        deviceToken,
        (error) => failures.fail(error),
        index === 0 ? (view, epoch) => progress.observe(view, epoch) : undefined,
      );
      bots.push(bot);
      await failures.race(bot.connect(code)); // sequential: host joins first -> seat 0 (host role)
    }

    while (true) {
      const done = bots.map((bot) => bot.tick());
      failures.throwIfFailed();
      progress.assertBombWatchdog();

      if (done.every(Boolean)) {
        const allJudgment = bots.every((bot) => bot.view?.phase.k === 'JUDGMENT');
        if (!allJudgment) throw new Error('PASS invariant failed: not every bot observed JUDGMENT');
        progress.assertPass();
        console.log(`JUDGMENT reached in ${((performance.now() - startedAt) / 1000).toFixed(1)}s — PASS`);
        return;
      }

      await failures.race(sleep(TICK_MS));
    }
  } catch (error: unknown) {
    console.error(`FAIL — ${errorMessage(error)}`);
    dumpViews(bots);
    throw error;
  } finally {
    clearTimeout(globalTimer);
    await Promise.all(bots.map((bot) => bot.close()));
  }
}

main().catch(() => process.exit(1));
