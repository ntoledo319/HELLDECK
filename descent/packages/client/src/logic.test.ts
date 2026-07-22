import { describe, expect, it } from 'vitest';
import {
  biggestMover,
  canDescend,
  cleanCodeInput,
  cleanName,
  depthGate,
  DESCEND_SOFTCAP_MS,
  flipAt,
  median,
  rankPlayers,
  remainingMs,
  serverErrorMessage,
  validCode,
} from './logic';
import { asView } from './view';

describe('cleanName (spec 6.1: 14 max, trimmed, uppercased)', () => {
  it('trims, collapses whitespace, uppercases', () => {
    expect(cleanName('  sam   toledo ')).toBe('SAM TOLEDO');
  });
  it('caps at 14 chars', () => {
    expect(cleanName('abcdefghijklmnopqrst')).toHaveLength(14);
  });
  it('empty input stays empty (join button stays dead)', () => {
    expect(cleanName('   ')).toBe('');
  });
});

describe('validCode (spec 4.1 alphabet: 20 consonants, no vowels/lookalikes)', () => {
  it('accepts codes from the alphabet', () => {
    expect(validCode('HRLM')).toBe(true);
    expect(validCode('BCDF')).toBe(true);
  });
  it('rejects vowels, lookalikes, wrong length, lowercase', () => {
    expect(validCode('HELL')).toBe(false); // E is a vowel
    expect(validCode('HRL1')).toBe(false);
    expect(validCode('HRLMM')).toBe(false);
    expect(validCode('hrlm')).toBe(false);
    expect(validCode('')).toBe(false);
  });
});

describe('cleanCodeInput', () => {
  it('uppercases, strips separators and forbidden characters, and caps at four', () => {
    expect(cleanCodeInput(' h-r l_m! ')).toBe('HRLM');
    expect(cleanCodeInput('he11bcdfz')).toBe('HBCD');
  });
});

describe('serverErrorMessage', () => {
  it('keeps useful server detail and supplies player-facing fallbacks', () => {
    expect(serverErrorMessage('BAD_INPUT', 'That ballot is closed.')).toBe('That ballot is closed.');
    expect(serverErrorMessage('BAD_INPUT', '')).toMatch(/check your choice/i);
    expect(serverErrorMessage('NO_ENTITLEMENT', '')).toMatch(/host needs to unlock/i);
    expect(serverErrorMessage('ENTITLEMENT_UNAVAILABLE', '')).toMatch(/try BEGIN again/i);
    expect(serverErrorMessage('UNKNOWN', '')).toMatch(/try again/i);
  });

  it('preserves the established full-room Imp explanation', () => {
    expect(serverErrorMessage('ROOM_FULL', 'Room full')).toMatch(/enter as an Imp/i);
  });
});

describe('canDescend (spec 4.2: host anytime, everyone past 45s softcap)', () => {
  const hold = 1_000_000;
  it('host may always descend', () => {
    expect(canDescend(true, hold, hold)).toBe(true);
  });
  it('non-host blocked before the softcap', () => {
    expect(canDescend(false, hold, hold + DESCEND_SOFTCAP_MS - 1)).toBe(false);
  });
  it('non-host allowed at exactly the softcap', () => {
    expect(canDescend(false, hold, hold + DESCEND_SOFTCAP_MS)).toBe(true);
  });
});

describe('remainingMs', () => {
  it('clamps at zero — countdowns never go negative', () => {
    expect(remainingMs(100, 500)).toBe(0);
    expect(remainingMs(500, 100)).toBe(400);
  });
});

describe('depthGate (HDRealRules2 §2: 3 sinners cap at Standard)', () => {
  it('caps 3 players at depth 7 with the pool-arithmetic warning', () => {
    const g = depthGate(3);
    expect(g.max).toBe(7);
    expect(g.warning).toMatch(/bring more bodies/i);
  });
  it('opens FULL DAMNATION at 4+', () => {
    expect(depthGate(4)).toEqual({ max: 9, warning: null });
    expect(depthGate(12).max).toBe(9);
  });
});

describe('rankPlayers', () => {
  const p = (id: string, score: number, seat: number, role = 'player') => ({ id, score, seat, role });
  it('sorts by score desc, seat asc on ties, imps excluded', () => {
    const ranked = rankPlayers([p('a', 3, 2), p('b', 7, 1), p('c', 3, 0), p('imp', 99, 3, 'imp')]);
    expect(ranked.map((x) => x.id)).toEqual(['b', 'c', 'a']);
  });
});

describe('biggestMover', () => {
  it('returns the largest positive delta', () => {
    expect(biggestMover({ a: 2, b: 5, c: 0 })).toBe('b');
  });
  it('null when nothing moved up or deltas missing', () => {
    expect(biggestMover({ a: 0 })).toBeNull();
    expect(biggestMover(undefined)).toBeNull();
  });
});

describe('flipAt (reveal simultaneity, spec 3.3/3.5)', () => {
  const now = 10_000;
  it('uses a future holdSince', () => {
    expect(flipAt(now + 1500, new Map(), now)).toBe(now + 1500);
  });
  it('uses the nearest future reveal-tagged AT deadline', () => {
    const d = new Map([
      ['reveal:c3', now + 900],
      ['input:c3', now + 5000],
    ]);
    expect(flipAt(now - 100, d, now)).toBe(now + 900);
  });
  it('null when everything is in the past — late rejoiners render the reveal immediately', () => {
    expect(flipAt(now - 100, new Map([['reveal:c3', now - 50]]), now)).toBeNull();
  });
});

describe('median (clock offset, spec 3.3)', () => {
  it('odd and even sample counts', () => {
    expect(median([5, 1, 9])).toBe(5);
    expect(median([1, 3, 5, 100])).toBe(4);
    expect(median([])).toBe(0);
  });
});

describe('asView runtime guard', () => {
  it('accepts a matching sub discriminant, rejects everything else', () => {
    expect(asView<{ sub: 'VOTE'; prompt: string }>({ sub: 'VOTE', prompt: 'x' }, 'VOTE')?.prompt).toBe('x');
    expect(asView({ sub: 'REVEAL' }, 'VOTE')).toBeNull();
    expect(asView(null, 'VOTE')).toBeNull();
    expect(asView('VOTE', 'VOTE')).toBeNull();
  });
});
