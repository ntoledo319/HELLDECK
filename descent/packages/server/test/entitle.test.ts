// The $9.99 unlock, at unit scale (spec Part 11 / D-412): the stateless HMAC token
// (entitle.ts), the entitlement decision, and the per-device free-night ledger (ledger-do.ts).
import { describe, expect, it } from 'vitest';
import { devUnlockAvailable, resolveEntitlement, signUnlock, validDevice, verifyUnlock } from '../src/entitle.js';
import { LedgerDO } from '../src/ledger-do.js';

const SECRET = 'test-unlock-secret';
const DEVICE = 'a1b2c3d4e5f6a7b8c9d0e1f2'; // 24 hex, a plausible UUID-derived id

describe('device token shape', () => {
  it('accepts UUID-hex ids, rejects junk', () => {
    expect(validDevice(DEVICE)).toBe(true);
    expect(validDevice('deadbeefdeadbeef')).toBe(true); // 16, the floor
    expect(validDevice('short')).toBe(false);
    expect(validDevice('has-dashes-and-symbols!!')).toBe(false);
    expect(validDevice('x'.repeat(65))).toBe(false); // over the ceiling
    expect(validDevice('')).toBe(false);
    expect(validDevice(undefined)).toBe(false);
  });
});

describe('unlock token (HMAC)', () => {
  it('a freshly signed token verifies for its own device', async () => {
    const token = await signUnlock(SECRET, DEVICE);
    expect(token.startsWith('v1.')).toBe(true);
    expect(await verifyUnlock(SECRET, DEVICE, token)).toBe(true);
  });

  it('is device-bound: a token lifted to another phone fails', async () => {
    const token = await signUnlock(SECRET, DEVICE);
    expect(await verifyUnlock(SECRET, 'ffffffffffffffffffffffff', token)).toBe(false);
  });

  it('a different secret invalidates the token (rotation)', async () => {
    const token = await signUnlock(SECRET, DEVICE);
    expect(await verifyUnlock('other-secret', DEVICE, token)).toBe(false);
  });

  it('never throws on missing/garbage inputs — the BEGIN hot path stays alive', async () => {
    expect(await verifyUnlock(undefined, DEVICE, 'v1.abc')).toBe(false);
    expect(await verifyUnlock(SECRET, DEVICE, undefined)).toBe(false);
    expect(await verifyUnlock(SECRET, 'bad device', 'v1.abc')).toBe(false);
    expect(await verifyUnlock(SECRET, DEVICE, 'v1.tampered')).toBe(false);
  });
});

describe('entitlement decision', () => {
  it('a paid device always plays', () => {
    expect(resolveEntitlement({ unlocked: true, freeNightUsed: true })).toEqual({
      entitled: true,
      reason: 'unlocked',
    });
  });
  it('an unpaid device plays its first night free', () => {
    expect(resolveEntitlement({ unlocked: false, freeNightUsed: false })).toEqual({
      entitled: true,
      reason: 'free-night',
    });
  });
  it('an unpaid device is locked once the free night is spent', () => {
    expect(resolveEntitlement({ unlocked: false, freeNightUsed: true })).toEqual({
      entitled: false,
      reason: 'locked',
    });
  });
});

describe('development unlock policy', () => {
  it('requires the exact dev environment and a signing secret', () => {
    expect(devUnlockAvailable('dev', SECRET)).toBe(true);
    expect(devUnlockAvailable('production', SECRET)).toBe(false);
    expect(devUnlockAvailable('staging', SECRET)).toBe(false);
    expect(devUnlockAvailable('', SECRET)).toBe(false);
    expect(devUnlockAvailable(undefined, SECRET)).toBe(false);
    expect(devUnlockAvailable('dev', undefined)).toBe(false);
  });
});

// ===== LedgerDO: the one stateful bit =====
class FakeStorage {
  data = new Map<string, unknown>();
  async get(k: string): Promise<unknown> {
    return this.data.get(k);
  }
  async put(k: string, v: unknown): Promise<void> {
    this.data.set(k, v);
  }
}
const makeLedger = (): LedgerDO =>
  new LedgerDO({ storage: new FakeStorage() } as unknown as DurableObjectState, {});
const call = (l: LedgerDO, method: string, path: string): Promise<Response> =>
  l.fetch(new Request(`https://ledger${path}`, { method }));

describe('LedgerDO free-night', () => {
  it('reports unspent, replays one lobby-attempt claim, and rejects every other attempt', async () => {
    const l = makeLedger();
    expect(await (await call(l, 'GET', '/status')).json()).toEqual({ freeNightUsed: false, claimMatches: false });

    const first = (await (await call(l, 'POST', '/consume-free?claim=HELL:0')).json()) as { granted: boolean };
    expect(first.granted).toBe(true);

    const replay = (await (await call(l, 'POST', '/consume-free?claim=HELL:0')).json()) as {
      granted: boolean;
      replayed: boolean;
    };
    expect(replay).toMatchObject({ granted: true, replayed: true });

    // A later lobby epoch (or a different room) never reuses the first device grant.
    const second = (await (await call(l, 'POST', '/consume-free?claim=HELL:1')).json()) as { granted: boolean };
    expect(second.granted).toBe(false);

    expect(await (await call(l, 'GET', '/status?claim=HELL:0')).json()).toEqual({
      freeNightUsed: true,
      claimMatches: true,
    });
    expect(await (await call(l, 'GET', '/status?claim=FIRE:0')).json()).toEqual({
      freeNightUsed: true,
      claimMatches: false,
    });
  });

  it('rejects a consume without a valid internal lobby-attempt claim', async () => {
    expect((await call(makeLedger(), 'POST', '/consume-free')).status).toBe(400);
  });

  it('rejects unknown routes', async () => {
    expect((await call(makeLedger(), 'GET', '/nope')).status).toBe(400);
  });
});
