// The $9.99 one-time host unlock — spec Part 11 monetization (D-412/413). This file owns
// the STATELESS half: a device-bound HMAC the host's phone carries in localStorage, so a
// purchase survives a browser restart with no account and no login. The one stateful bit —
// "has this device already spent its single free night?" — lives in ledger-do.ts.
//
// Pure + WebCrypto only (available in both workerd and Node): no DO, no Date.now, no IO,
// so the whole decision is unit-testable and can never take a live night down.

const enc = new TextEncoder();

// A device token is a client-minted opaque id (UUID hex). Validate shape before it touches
// the HMAC or a DO name so a hostile/garbage value can't fan out storage or blow the key.
const DEVICE_RE = /^[a-z0-9]{16,64}$/;

export function validDevice(device: string | undefined | null): device is string {
  return typeof device === 'string' && DEVICE_RE.test(device);
}

async function hmacKey(secret: string): Promise<CryptoKey> {
  return crypto.subtle.importKey('raw', enc.encode(secret), { name: 'HMAC', hash: 'SHA-256' }, false, ['sign']);
}

function toHex(buf: ArrayBuffer): string {
  let s = '';
  for (const b of new Uint8Array(buf)) s += b.toString(16).padStart(2, '0');
  return s;
}

/**
 * The unlock token = "v1." + HMAC(secret, "unlock:v1:"+device). Device-bound so a token
 * lifted onto another phone verifies against the WRONG device and fails; version-tagged so
 * the format/secret can rotate without silently trusting an old shape.
 */
export async function signUnlock(secret: string, device: string): Promise<string> {
  const key = await hmacKey(secret);
  const sig = await crypto.subtle.sign('HMAC', key, enc.encode(`unlock:v1:${device}`));
  return `v1.${toHex(sig)}`;
}

/**
 * Constant-time verify. Returns false on ANY malformed input rather than throwing — this
 * runs on the BEGIN hot path and an unlock check must never crash a room.
 */
export async function verifyUnlock(
  secret: string | undefined,
  device: string | undefined,
  token: string | undefined | null,
): Promise<boolean> {
  if (!secret || !validDevice(device) || !token) return false;
  let expected: string;
  try {
    expected = await signUnlock(secret, device);
  } catch {
    return false;
  }
  return timingSafeEqual(expected, token);
}

function timingSafeEqual(a: string, b: string): boolean {
  if (a.length !== b.length) return false;
  let diff = 0;
  for (let i = 0; i < a.length; i++) diff |= a.charCodeAt(i) ^ b.charCodeAt(i);
  return diff === 0;
}

// ===== the entitlement decision =====

export type EntReason = 'unlocked' | 'free-night' | 'locked';

export interface EntitlementInputs {
  unlocked: boolean; // a valid HMAC unlock token was presented for this device
  freeNightUsed: boolean; // ledger: this device has already spent its one free night
}

export interface Entitlement {
  entitled: boolean;
  reason: EntReason;
}

/**
 * The whole business model in five lines: a paid device always plays; an unpaid device
 * plays its FIRST night free; every night after that is locked until it pays. The caller
 * charges the free night (marks the ledger) only when reason==='free-night' AND a night
 * actually starts — a rejected BEGIN must never burn the one free descent.
 */
export function resolveEntitlement({ unlocked, freeNightUsed }: EntitlementInputs): Entitlement {
  if (unlocked) return { entitled: true, reason: 'unlocked' };
  if (!freeNightUsed) return { entitled: true, reason: 'free-night' };
  return { entitled: false, reason: 'locked' };
}

/** Fail-safe policy for the local-only unlock escape hatch. An absent, misspelled, or
 * unfamiliar environment is production-like; development must be explicitly selected
 * and have a signing secret capable of minting the device-bound token. */
export function devUnlockAvailable(environment: string | undefined, unlockSecret: string | undefined): boolean {
  return environment === 'dev' && Boolean(unlockSecret);
}
