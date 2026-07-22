// Wire-router rules (spec 3.2/3.3/3.5): PRIVATE dispatch, epoch discipline, offset median.
// Node env — Net's browser touchpoints are shimmed to exactly what the constructor needs.
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { receivePreviewMessage, type PreviewAssigned } from '../screens/preview.logic';
import { HEARTBEAT_MS, Net, type NetHandlers } from './ws';

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

describe('WELCOME clock seed', () => {
  it('aligns a skewed fresh client before a live private deadline is evaluated', () => {
    const { net, h } = makeNet();
    const serverNow = 1_720_900_000_000;
    vi.setSystemTime(serverNow + 60 * 60 * 1000); // phone clock is one hour fast
    net.handleMessage({ t: 'WELCOME', you: 'P1', sv: serverNow });
    expect(h.onWelcome).toHaveBeenCalledWith('P1');
    expect(net.serverNow()).toBe(serverNow);

    const assignment: PreviewAssigned = {
      status: 'assigned',
      previewId: 'deal:7',
      card: { id: 'redflag_v3_1', text: 'Still live' },
      burnDeadline: serverNow + 10_000,
      revealAt: serverNow + 10_000,
      canBurn: true,
    };
    expect(receivePreviewMessage(null, assignment, 1, net.serverNow())).not.toBeNull();
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

describe('transport heartbeat', () => {
  it('sends phase-agnostic liveness frames without multiplying intervals', () => {
    const { net, sent } = makeNet();
    const controls = net as unknown as { startHeartbeat(): void; stopHeartbeat(): void };

    controls.startHeartbeat();
    vi.advanceTimersByTime(HEARTBEAT_MS);
    expect(sent.map((frame) => JSON.parse(frame))).toEqual([{ t: 'HEARTBEAT' }]);

    controls.startHeartbeat(); // reconnect/open races replace the prior interval
    vi.advanceTimersByTime(HEARTBEAT_MS);
    expect(sent.map((frame) => JSON.parse(frame))).toEqual([{ t: 'HEARTBEAT' }, { t: 'HEARTBEAT' }]);

    controls.stopHeartbeat();
    vi.advanceTimersByTime(HEARTBEAT_MS * 2);
    expect(sent).toHaveLength(2);
  });

  it('starts on socket open and stops across close/reconnect lifecycle', () => {
    const originalWebSocket = g['WebSocket'];
    const originalLocation = g['location'];
    const originalStorage = g['localStorage'];
    const sockets: LifecycleSocket[] = [];

    class LifecycleSocket {
      static readonly OPEN = 1;
      static readonly CLOSED = 3;
      readyState = 0;
      sent: string[] = [];
      onopen: (() => void) | null = null;
      onclose: (() => void) | null = null;
      onmessage: ((event: { data: string }) => void) | null = null;

      constructor(readonly url: string) {
        sockets.push(this);
      }

      send(frame: string): void {
        this.sent.push(frame);
      }

      open(): void {
        this.readyState = LifecycleSocket.OPEN;
        this.onopen?.();
      }

      close(): void {
        this.readyState = LifecycleSocket.CLOSED;
        this.onclose?.();
      }
    }

    const values = new Map<string, string>();
    g['WebSocket'] = LifecycleSocket;
    g['location'] = { protocol: 'http:', host: '127.0.0.1:8787' };
    g['localStorage'] = {
      getItem: (key: string): string | null => values.get(key) ?? null,
      setItem: (key: string, value: string): void => {
        values.set(key, value);
      },
    };

    let net: Net | null = null;
    try {
      net = makeNet().net;
      net.connect();
      const first = sockets[0]!;
      expect(first.url).toMatch(/^ws:\/\/127\.0\.0\.1:8787\/ws\/HRLM\?token=.*&v=1&dev=[a-z0-9]{16,64}$/);
      first.open();
      vi.advanceTimersByTime(HEARTBEAT_MS);
      expect(first.sent.map((frame) => JSON.parse(frame))).toEqual([{ t: 'HEARTBEAT' }]);

      first.close();
      vi.advanceTimersByTime(HEARTBEAT_MS);
      expect(first.sent).toHaveLength(1);
      expect(sockets).toHaveLength(2); // reconnect was created, but cannot heartbeat before open
    } finally {
      net?.close();
      g['WebSocket'] = originalWebSocket;
      g['location'] = originalLocation;
      g['localStorage'] = originalStorage;
    }
  });
});

describe('send delivery signal', () => {
  it('returns true for an open socket and false when the socket cannot accept the action', () => {
    const { net, sent } = makeNet();
    expect(net.send({ t: 'RESYNC' })).toBe(true);
    expect(sent).toEqual([JSON.stringify({ t: 'RESYNC' })]);
    (net as unknown as { ws: unknown }).ws = null;
    expect(net.send({ t: 'RESYNC' })).toBe(false);
  });

  it('returns false instead of throwing if an open socket rejects the write', () => {
    const { net } = makeNet();
    (net as unknown as { ws: { readyState: number; send: () => never } }).ws = {
      readyState: WebSocket.OPEN,
      send: () => {
        throw new Error('socket closed during send');
      },
    };
    expect(net.send({ t: 'RESYNC' })).toBe(false);
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
