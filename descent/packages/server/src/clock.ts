// Clock sync math (spec 3.3) + FIRE/HEAT pacing (spec 3.2). Pure functions only —
// RoomDO owns sockets, alarms, and Date.now(); everything here takes times as arguments
// so the whole reveal-simultaneity mechanism is testable as arithmetic. Task D-104.

export const PING_BATCH_SIZE = 5; // pings per batch
export const PING_SPACING_MS = 200; // gap between pings in a batch
export const PING_REFRESH_MS = 60_000; // new batch every 60s while sockets live
export const OFFSET_SAMPLE_CAP = 5; // median over the last 5 samples
export const PENDING_PING_TTL_MS = 10_000; // unanswered pings are forgotten

export const HEAT_FLUSH_MS = 250; // HEAT broadcast <= 4Hz
export const FIRE_CAP_PER_SEC = 10; // per-client FIRE budget (rolling 1s window)

export function median(xs: readonly number[]): number {
  if (xs.length === 0) return 0;
  const sorted = [...xs].sort((a, b) => a - b);
  const mid = Math.floor(sorted.length / 2);
  return sorted.length % 2 === 1 ? sorted[mid]! : Math.round((sorted[mid - 1]! + sorted[mid]!) / 2);
}

/**
 * One ping-pong round: server sent PING at `sentSv`, client stamped its clock `clientClock`
 * into the PONG, server received it at `receivedSv`. Assuming the client answered at the
 * network midpoint, offset (serverTime − clientTime) = midpoint − clientClock.
 */
export function sampleOffset(sentSv: number, clientClock: number, receivedSv: number): number {
  return Math.round((sentSv + receivedSv) / 2 - clientClock);
}

/** Per-socket clock-sync bookkeeping; lives in the WS attachment so it survives hibernation. */
export interface ClockState {
  samples: number[]; // last <= OFFSET_SAMPLE_CAP offset samples
  pending: Record<string, number>; // pingId -> server time when sent
}

export function emptyClock(): ClockState {
  return { samples: [], pending: {} };
}

export function recordPing(c: ClockState, pingId: string, sv: number): ClockState {
  const pending: Record<string, number> = {};
  for (const [id, sent] of Object.entries(c.pending)) {
    if (sv - sent <= PENDING_PING_TTL_MS) pending[id] = sent; // prune the unanswered
  }
  pending[pingId] = sv;
  return { samples: c.samples, pending };
}

export function recordPong(
  c: ClockState,
  pingId: string,
  clientClock: number,
  receivedSv: number,
): { clock: ClockState; offset: number | null } {
  const sent = c.pending[pingId];
  if (sent === undefined) return { clock: c, offset: null }; // stale/unknown pong
  const pending = { ...c.pending };
  delete pending[pingId];
  const samples = [...c.samples, sampleOffset(sent, clientClock, receivedSv)].slice(-OFFSET_SAMPLE_CAP);
  return { clock: { samples, pending }, offset: median(samples) };
}

/** Per-socket FIRE budget: rolling 1s window, hard cap FIRE_CAP_PER_SEC taps. */
export interface FireWindow {
  start: number; // window open time (server ms)
  used: number; // taps spent in this window
}

export function clampFire(w: FireWindow, n: number, now: number): { window: FireWindow; allowed: number } {
  const fresh = now - w.start >= 1000;
  const start = fresh ? now : w.start;
  const used = fresh ? 0 : w.used;
  const allowed = Math.max(0, Math.min(n, FIRE_CAP_PER_SEC - used));
  return { window: { start, used: used + allowed }, allowed };
}
