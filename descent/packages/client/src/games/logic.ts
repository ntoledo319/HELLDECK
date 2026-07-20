// Pure screen rules for the M2 games (Fill-In 5.2, Over/Under 5.3, Confession 5.4).
// No DOM, no clocks — every rule here has a test in logic.test.ts. Screens render;
// this file decides. Mirrors of engine constants are marked as such.
import type { OverUnderRevealView } from './wire';

// ============================================================================
// FILL-IN — the 140-char writer field (spec 5.2 phase 2)
// ============================================================================
export const ANSWER_MAX = 140;

/** Live input filter: one line, single spaces, hard 140 cap. Never trims the tail
 * mid-typing beyond collapsing runs — writers keep their rhythm. */
export function clampAnswer(raw: string): string {
  return raw.replace(/\s+/g, ' ').trimStart().slice(0, ANSWER_MAX);
}

/** Submittable = something survives a full trim. Empty writes fall to the panic shelf (engine). */
export function answerReady(raw: string): boolean {
  return clampAnswer(raw).trim().length > 0;
}

export function charsLeft(raw: string): number {
  return ANSWER_MAX - clampAnswer(raw).length;
}

/** Audience beat counter: 0-based index in, human string out. */
export function performProgress(answerIdx: number, answerCount: number): string {
  return `ANSWER ${Math.min(answerIdx + 1, answerCount)} OF ${answerCount}`;
}

// ============================================================================
// OVER/UNDER — dial + number pad (numbers are receipts: non-negative integers)
// ============================================================================
export const COUNT_MAX = 1_000_000_000; // mirror of engine games/overunder.ts MAX_COUNT
export const DIAL_STEPS: readonly number[] = [-10, -1, 1, 10];

/** Scribe dial: integer walk, clamped to [0, COUNT_MAX]. Garbage in -> 0. */
export function stepLine(current: number, delta: number): number {
  const base = Number.isFinite(current) ? Math.round(current) : 0;
  const d = Number.isFinite(delta) ? Math.round(delta) : 0;
  return Math.max(0, Math.min(COUNT_MAX, base + d));
}

// --- number pad: string model so "0" and "" stay distinct on screen ---
const PAD_MAX_DIGITS = 10; // COUNT_MAX is 10 digits

export function padPress(current: string, key: string): string {
  if (key === 'back') return current.slice(0, -1);
  if (key === 'clear') return '';
  if (!/^[0-9]$/.test(key)) return current;
  const base = current === '0' ? '' : current; // no leading zeros
  const next = base + key;
  if (next.length > PAD_MAX_DIGITS || Number(next) > COUNT_MAX) return current;
  return next;
}

/** null = nothing entered yet (TESTIFY stays dead). "0" is a real answer. */
export function padValue(current: string): number | null {
  if (current.length === 0) return null;
  const n = Number(current);
  return Number.isInteger(n) && n >= 0 && n <= COUNT_MAX ? n : null;
}

// --- reveal banners (5.3 phase 5: number vs line slam, vindicated/exposed) ---
export interface OuVerdict {
  headline: string; // the slam word
  subjectLine: string; // vindicated / exposed / median banner
}

export function ouBanner(
  r: Pick<OverUnderRevealView, 'voided' | 'push' | 'truth' | 'line'>,
  subjectName: string,
): OuVerdict {
  if (r.voided || r.truth === null || r.line === null) {
    return {
      headline: 'NO NUMBER. NO TRUTH.',
      subjectLine: 'The loop dies scoreless. Remember who let it.',
    };
  }
  if (r.push) {
    return {
      headline: 'DEAD ON THE LINE',
      subjectLine: `${subjectName} IS the median. Everyone +1. Somehow that's worse.`,
    };
  }
  return r.truth > r.line
    ? { headline: 'OVER', subjectLine: `${subjectName} — EXPOSED` }
    : { headline: 'UNDER', subjectLine: `${subjectName} WALKS — VINDICATED` };
}

/** Your bet, judged. null = you didn't bet / nothing resolved. */
export function betResult(
  youBet: 'over' | 'under' | null,
  r: Pick<OverUnderRevealView, 'voided' | 'push' | 'truth' | 'line'>,
): 'won' | 'lost' | 'push' | null {
  if (youBet === null || r.voided || r.truth === null || r.line === null) return null;
  if (r.push) return 'push';
  return (r.truth > r.line ? 'over' : 'under') === youBet ? 'won' : 'lost';
}

// ============================================================================
// CONFESSION — the stamp + banners (5.4 phase 5)
// ============================================================================
export interface ConfessionStamp {
  stamp: 'TRUE' | 'FALSE' | null; // null = nothing to stamp (fifth/voided)
  banner: string;
}

/** Verdict enum mirrors engine games/confession.ts: FOOLED (majority wrong,
 * confessor +3) / CAUGHT / HUNG (tie — the liar walks, confessor wins). */
export function confessionStamp(
  r: { truth: boolean | null; verdict: 'FOOLED' | 'CAUGHT' | 'HUNG' | null; voided: boolean },
  confessorName: string,
): ConfessionStamp {
  if (r.voided || r.truth === null || r.verdict === null) {
    return { stamp: null, banner: 'THE CONFESSION DIED IN CHAMBERS. NOBODY SCORES.' };
  }
  const stamp = r.truth ? 'TRUE' : 'FALSE';
  if (r.verdict === 'HUNG') return { stamp, banner: `HUNG JURY — THE LIAR WALKS. ${confessorName} +3.` };
  if (r.verdict === 'FOOLED') {
    return {
      stamp,
      banner: r.truth
        ? `IT HAPPENED — AND YOU CALLED CAP ON A REAL SIN. ${confessorName} +3.`
        : `A LIE, SOLD CLEAN. ${confessorName} +3 FOR LYING BEAUTIFULLY.`,
    };
  }
  return {
    stamp,
    banner: r.truth ? 'TRUE — AND THE JURY SMELLED IT.' : 'CAP — CAUGHT RED-HANDED.',
  };
}

/** Your believe/cap vote, judged against the lock (+1 when right, spec 4.6). */
export function juryResult(
  youVoted: 'believe' | 'cap' | null,
  truth: boolean | null,
): 'right' | 'wrong' | null {
  if (youVoted === null || truth === null) return null;
  return (youVoted === 'believe') === truth ? 'right' : 'wrong';
}

// ============================================================================
// shared — hold-to-commit weight for the heavy locks (TRUE/FALSE)
// ============================================================================
export const LOCK_HOLD_MS = 650; // deliberate, not accidental; under a second, not a chore
