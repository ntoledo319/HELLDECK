// FIRE coalescing — spec 3.2: max 1 msg / 500ms, n = taps since last send.
// Pure: time is injected, so the rule is unit-testable (coalesce.test.ts).
export class FireCoalescer {
  private pending = 0;
  private lastSentAt = -Infinity; // first tap always sends immediately

  constructor(
    private readonly send: (n: number) => void,
    readonly intervalMs = 500,
  ) {}

  /** Register n taps at nowMs; flushes immediately if the window has elapsed. */
  tap(nowMs: number, n = 1): void {
    this.pending += n;
    this.maybeFlush(nowMs);
  }

  /** Periodic nudge (UI timer / animation loop) to flush a partially-filled window. */
  tick(nowMs: number): void {
    this.maybeFlush(nowMs);
  }

  get queued(): number {
    return this.pending;
  }

  private maybeFlush(nowMs: number): void {
    if (this.pending > 0 && nowMs - this.lastSentAt >= this.intervalMs) {
      this.send(this.pending);
      this.pending = 0;
      this.lastSentAt = nowMs;
    }
  }
}
