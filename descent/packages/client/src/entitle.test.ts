// Client entitlement — the pure routing/validation that gates the $9.99 toll (spec Part 11).
import { describe, expect, it } from 'vitest';
import { parseUnlockReturn, validDeviceToken } from './entitle';

const DEV = 'a1b2c3d4e5f6a7b8c9d0e1f2';

describe('validDeviceToken', () => {
  it('matches the server rule: 16–64 lowercase hex-ish', () => {
    expect(validDeviceToken(DEV)).toBe(true);
    expect(validDeviceToken('deadbeefdeadbeef')).toBe(true);
    expect(validDeviceToken('short')).toBe(false);
    expect(validDeviceToken('UPPERCASEUPPERCASE')).toBe(false);
    expect(validDeviceToken(null)).toBe(false);
    expect(validDeviceToken(undefined)).toBe(false);
  });
});

describe('parseUnlockReturn', () => {
  it('reads a paid return as a verify job with the session + device', () => {
    expect(parseUnlockReturn(`?session_id=cs_test_123&dev=${DEV}`)).toEqual({
      kind: 'verify',
      sessionId: 'cs_test_123',
      dev: DEV,
    });
  });

  it('reads a canceled checkout', () => {
    expect(parseUnlockReturn('?unlock=cancel')).toEqual({ kind: 'cancel' });
  });

  it('a plain load is none', () => {
    expect(parseUnlockReturn('')).toEqual({ kind: 'none' });
    expect(parseUnlockReturn('?foo=bar')).toEqual({ kind: 'none' });
  });

  it('refuses a session tied to a malformed device (no verify attempt)', () => {
    expect(parseUnlockReturn('?session_id=cs_test_123&dev=not-a-device')).toEqual({ kind: 'none' });
    expect(parseUnlockReturn('?session_id=cs_test_123')).toEqual({ kind: 'none' });
  });

  it('cancel wins even if stale session params linger', () => {
    expect(parseUnlockReturn(`?unlock=cancel&session_id=cs_x&dev=${DEV}`)).toEqual({ kind: 'cancel' });
  });
});
