// Wire-router rules (spec 3.2/3.3/3.5): PRIVATE dispatch, epoch discipline, offset median.
// Node env — Net's browser touchpoints are shimmed to exactly what the constructor needs.
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { Net, type NetHandlers } from './ws';

// ===== minimal browser shims (constructor + send path only; connect() is never called) =====
const g = globalThis as Record<string, unknown>;
g['document'] = {
  addEventListener() {},
  removeEventListener() {},
  visibilityState: 'visible',
};
if (!('WebSocket' in globalThis)) g['WebSocket'] = class { static OPEN = 1 };

function makeNet(): { net: Net; h: { [K in keyof NetHandlers]: ReturnType<typeof vi.fn> }; sent: string[] } {
  const h = {
    onState: vi.fn(),
    onPrivate: vi.fn(),
    onAudio: vi.fn(),
    onHeat: vi.fn(),
    onDeadline: vi.fn(),
    onStatus: vi.fn(),
    onWelcome: vi.fn(),
    onError: vi.fn(),
  };
  const net = new Net('HRLM', h);
  const sent: string[] = [];
  // Fake open socket so send() has somewhere to go (PONG path).
  (net as unknown as { ws: unknown }).ws = {
    readyState: (globalThis.WebSocket as unknown as { OPEN: number }).OPEN,
    send: (s: string) => sent.push(s),
  };
  return { net, h, sent };
}

beforeEach(() => {
  vi.useFakeTimers();
  vi.setSystemTime(1_720_900_000_000);
});
afterEach(() => {
  vi.useRealTimers();
});

describe('PRIVATE dispatch (spec 3.4: secrets ride per-socket only)', () => {
  it('routes k + payload untouched for card previews and spotlight acknowledgements', () => {
    const { net, h } = makeNet();
    const preview = { card: { text: 'Who cried at a car commercial?' }, burnDeadline: 123 };
    const released = { status: 'released', ceremonyId: 'spotlight:9:123' };
    net.handleMessage({ t: 'PRIVATE', k: 'preview', p: preview });
    net.handleMessage({ t: 'PRIVATE', k: 'spotlight', p: released });
    expect(h.onPrivate.mock.calls).toEqual([
      ['preview', preview],
      ['spotlight', released],
    ]);
  });
});

describe('STATE epoch discipline (spec 3.5)', () => {
  it('accepts rising epochs, accepts equal (full resync), drops stale', () => {
    const { net, h } = makeNet();
    net.handleMessage({ t: 'STATE', s: { a: 1 }, epoch: 5 });
    net.handleMessage({ t: 'STATE', s: { a: 2 }, epoch: 4 }); // stale — dropped
    net.handleMessage({ t: 'STATE', s: { a: 3 }, epoch: 5 }); // resync — accepted
    expect(h.onState.mock.calls).toEqual([
      [{ a: 1 }, 5],
      [{ a: 3 }, 5],
    ]);
  });
});

describe('PING → PONG + clock offset (spec 3.3)', () => {
  it('answers immediately and sets offset to the median sample', () => {
    const { net, sent } = makeNet();
    const now = Date.now();
    for (const skew of [100, 120, 90]) net.handleMessage({ t: 'PING', id: 7, sv: now + skew });
    expect(sent).toHaveLength(3);
    expect(JSON.parse(sent[0]!)).toEqual({ t: 'PONG', id: 7, cl: now });
    expect(net.serverNow()).toBe(now + 100); // median of {100,120,90}
  });
});

describe('AT / HEAT / ERR dispatch', () => {
  it('deadlines land in server time; heat and errors pass through', () => {
    const { net, h } = makeNet();
    net.handleMessage({ t: 'AT', timerId: 'input:c3', at: 1_720_900_012_000 });
    net.handleMessage({ t: 'HEAT', n: 37 });
    net.handleMessage({ t: 'ERR', code: 'ROOM_FULL', msg: 'hell is full' });
    expect(h.onDeadline.mock.calls).toEqual([['input:c3', 1_720_900_012_000]]);
    expect(h.onHeat.mock.calls).toEqual([[37]]);
    expect(h.onError.mock.calls).toEqual([['ROOM_FULL', 'hell is full']]);
  });
});
