// FIRE coalescing rule (spec 3.2): max 1 msg / 500ms, n = taps since last send.
import { describe, expect, it } from 'vitest';
import { FireCoalescer } from './coalesce';

function harness() {
  const sent: number[] = [];
  const c = new FireCoalescer((n) => sent.push(n));
  return { sent, c };
}

describe('FireCoalescer', () => {
  it('sends the first tap immediately (instant feedback on the wire too)', () => {
    const { sent, c } = harness();
    c.tap(1000);
    expect(sent).toEqual([1]);
  });

  it('coalesces a burst inside the window into one message with the tap count', () => {
    const { sent, c } = harness();
    c.tap(1000);
    for (let t = 1050; t < 1500; t += 50) c.tap(t); // 9 taps inside the window
    expect(sent).toEqual([1]); // nothing extra yet
    c.tick(1500);
    expect(sent).toEqual([1, 9]);
  });

  it('never exceeds one message per 500ms across sustained spam', () => {
    const { sent, c } = harness();
    for (let t = 0; t <= 3000; t += 100) c.tap(t);
    // messages at 0, 500, 1000 ... => at most ceil(duration/500)+1
    expect(sent.length).toBeLessThanOrEqual(7);
    // and every tap is accounted for exactly once
    expect(sent.reduce((a, b) => a + b, 0) + c.queued).toBe(31);
  });

  it('tick with nothing pending sends nothing', () => {
    const { sent, c } = harness();
    c.tick(99_999);
    expect(sent).toEqual([]);
  });

  it('tick before the window closes holds fire', () => {
    const { sent, c } = harness();
    c.tap(1000);
    c.tap(1100);
    c.tick(1400); // only 400ms since last send
    expect(sent).toEqual([1]);
    expect(c.queued).toBe(1);
  });
});
