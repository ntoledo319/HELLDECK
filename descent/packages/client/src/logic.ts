// Pure client rules — no DOM, no clocks. Every rule here has a test in logic.test.ts.
import { CODE_ALPHABET } from '@helldeck/engine';

export const NAME_MAX = 14; // spec 6.1: name field 14 max
export const DESCEND_SOFTCAP_MS = 45_000; // spec 4.2: anyone may DESCEND >= 45s into a reveal hold

export function cleanName(raw: string): string {
  return raw.replace(/\s+/g, ' ').trim().toUpperCase().slice(0, NAME_MAX);
}

const CODE_RE = new RegExp(`^[${CODE_ALPHABET}]{4}$`);
const CODE_SET = new Set(CODE_ALPHABET);

/** Normalize typed/pasted room codes before they reach navigation or validation. */
export function cleanCodeInput(raw: string): string {
  return [...raw.toUpperCase()]
    .filter((char) => CODE_SET.has(char))
    .slice(0, 4)
    .join('');
}

export function validCode(s: string): boolean {
  return CODE_RE.test(s);
}

/** Turn protocol failures into useful player-facing copy when the server has no message. */
export function serverErrorMessage(code: string, message: string): string {
  if (code === 'ROOM_FULL') return 'HELL IS FULL — you enter as an Imp.';

  const detail = message.trim();
  if (detail) return detail;

  switch (code) {
    case 'BAD_INPUT':
      return 'THE PIT REJECTED THAT — check your choice and try again.';
    case 'NO_ENTITLEMENT':
      return 'THIS NIGHT IS LOCKED — the host needs to unlock it.';
    default:
      return 'THE PIT REJECTED THAT — try again.';
  }
}

// DESCEND button visibility (server is the enforcer; this is the client's mirror).
export function canDescend(isHost: boolean, holdSinceServerMs: number, nowServerMs: number): boolean {
  return isHost || nowServerMs - holdSinceServerMs >= DESCEND_SOFTCAP_MS;
}

export function remainingMs(deadlineServerMs: number, nowServerMs: number): number {
  return Math.max(0, deadlineServerMs - nowServerMs);
}

// Depth gate by pool arithmetic (HDRealRules2 §2: 3 sinners cap at Standard).
export function depthGate(nSinners: number): { max: 5 | 7 | 9; warning: string | null } {
  if (nSinners <= 3) {
    return {
      max: 7,
      warning: '9 circles for 3 sinners means the wheel comes back around — bring more bodies for FULL DAMNATION.',
    };
  }
  return { max: 9, warning: null };
}

// Ladder order: sinners only, score desc, join order breaks ties.
export function rankPlayers<T extends { score: number; seat: number; role: string }>(players: readonly T[]): T[] {
  return players
    .filter((p) => p.role !== 'imp')
    .slice()
    .sort((a, b) => b.score - a.score || a.seat - b.seat);
}

export function biggestMover(deltas: Record<string, number> | undefined): string | null {
  if (!deltas) return null;
  let best: string | null = null;
  let bestV = 0;
  for (const [id, d] of Object.entries(deltas)) {
    if (d > bestV) {
      bestV = d;
      best = id;
    }
  }
  return best;
}

// Reveal flip moment: the future holdSince or the nearest future AT timer tagged reveal/flip.
// Null => flip NOW (a reconnecting phone renders the current reveal, spec 3.5).
export function flipAt(
  holdSinceServerMs: number,
  deadlines: ReadonlyMap<string, number>,
  nowServerMs: number,
): number | null {
  let at: number | null = holdSinceServerMs > nowServerMs ? holdSinceServerMs : null;
  for (const [id, t] of deadlines) {
    if (/reveal|flip/i.test(id) && t > nowServerMs && (at === null || t < at)) at = t;
  }
  return at;
}

export function median(xs: readonly number[]): number {
  if (xs.length === 0) return 0;
  const s = xs.slice().sort((a, b) => a - b);
  const mid = s.length >> 1;
  return s.length % 2 === 1 ? s[mid]! : (s[mid - 1]! + s[mid]!) / 2;
}
