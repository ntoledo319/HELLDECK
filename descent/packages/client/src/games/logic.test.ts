import { describe, expect, it } from 'vitest';
import {
  ANSWER_MAX,
  answerReady,
  betResult,
  charsLeft,
  clampAnswer,
  confessionStamp,
  COUNT_MAX,
  DIAL_STEPS,
  juryResult,
  ouBanner,
  padPress,
  padValue,
  performProgress,
  stepLine,
} from './logic';

describe('clampAnswer (spec 5.2: 140-char field, one line)', () => {
  it('collapses whitespace runs and newlines to single spaces', () => {
    expect(clampAnswer('two  spaces\nand a newline')).toBe('two spaces and a newline');
  });
  it('hard-caps at 140 chars', () => {
    expect(clampAnswer('x'.repeat(200))).toHaveLength(ANSWER_MAX);
  });
  it('keeps a single trailing space while typing (mid-word rhythm survives)', () => {
    expect(clampAnswer('half a ')).toBe('half a ');
  });
  it('strips leading whitespace', () => {
    expect(clampAnswer('   lead')).toBe('lead');
  });
});

describe('answerReady / charsLeft', () => {
  it('whitespace-only is not submittable (engine auto-panics empties)', () => {
    expect(answerReady('   ')).toBe(false);
    expect(answerReady('a')).toBe(true);
  });
  it('charsLeft counts down from 140 and floors at 0', () => {
    expect(charsLeft('')).toBe(140);
    expect(charsLeft('abcde')).toBe(135);
    expect(charsLeft('x'.repeat(500))).toBe(0);
  });
});

describe('performProgress (spec 5.2: audience sees "answer 3 of 7", never the text)', () => {
  it('renders 1-based progress', () => {
    expect(performProgress(2, 7)).toBe('ANSWER 3 OF 7');
  });
  it('never overshoots the count', () => {
    expect(performProgress(9, 7)).toBe('ANSWER 7 OF 7');
  });
});

describe('stepLine (spec 5.3: big integer stepper, numbers are receipts)', () => {
  it('walks by the dial steps', () => {
    expect(DIAL_STEPS).toEqual([-10, -1, 1, 10]);
    expect(stepLine(5, 10)).toBe(15);
    expect(stepLine(5, -1)).toBe(4);
  });
  it('never goes negative — a count of -3 unread emails is not a thing', () => {
    expect(stepLine(5, -10)).toBe(0);
    expect(stepLine(0, -1)).toBe(0);
  });
  it('clamps at the engine sanity bound', () => {
    expect(stepLine(COUNT_MAX, 10)).toBe(COUNT_MAX);
  });
  it('garbage in -> sane integer out', () => {
    expect(stepLine(Number.NaN, 1)).toBe(1);
    expect(stepLine(3.7, 0)).toBe(4);
  });
});

describe('number pad (spec 5.3 phase 4: subject number pad)', () => {
  it('appends digits, backspaces, clears', () => {
    let s = '';
    for (const k of ['4', '2', 'back', '7']) s = padPress(s, k);
    expect(s).toBe('47');
    expect(padPress(s, 'clear')).toBe('');
  });
  it('no leading zeros — typing 0 then 5 reads 5', () => {
    expect(padPress(padPress('', '0'), '5')).toBe('5');
  });
  it('zero alone is a real answer, empty is not (TESTIFY stays dead)', () => {
    expect(padValue('0')).toBe(0);
    expect(padValue('')).toBeNull();
  });
  it('refuses digits past the engine bound (mirrors MAX_COUNT)', () => {
    const nineNines = '9'.repeat(9);
    expect(padPress(nineNines, '9')).toBe(nineNines); // 9,999,999,999 > 1e9
    expect(padValue('1000000000')).toBe(COUNT_MAX);
  });
  it('ignores non-digit keys', () => {
    expect(padPress('12', 'x')).toBe('12');
  });
});

describe('ouBanner (spec 5.3 phase 5: number vs line slam)', () => {
  const base = { voided: false, push: false, truth: 63, line: 47 };
  it('over -> subject exposed', () => {
    const v = ouBanner(base, 'SAM');
    expect(v.headline).toBe('OVER');
    expect(v.subjectLine).toContain('EXPOSED');
  });
  it('under -> subject vindicated', () => {
    const v = ouBanner({ ...base, truth: 12 }, 'SAM');
    expect(v.headline).toBe('UNDER');
    expect(v.subjectLine).toContain('VINDICATED');
  });
  it('push -> the median roast, everyone +1 (spec 4.6)', () => {
    const v = ouBanner({ ...base, truth: 47, push: true }, 'SAM');
    expect(v.headline).toBe('DEAD ON THE LINE');
    expect(v.subjectLine).toContain('median');
  });
  it('voided -> no number, EVER (test D-115 discipline: null truth renders as absence)', () => {
    const v = ouBanner({ voided: true, push: false, truth: null, line: 47 }, 'SAM');
    expect(v.headline).toContain('NO NUMBER');
    expect(v.subjectLine).not.toContain('null');
  });
});

describe('betResult', () => {
  const r = { voided: false, push: false, truth: 63, line: 47 };
  it('judges your side against the slam', () => {
    expect(betResult('over', r)).toBe('won');
    expect(betResult('under', r)).toBe('lost');
  });
  it('push pays both sides', () => {
    expect(betResult('under', { ...r, truth: 47, push: true })).toBe('push');
  });
  it('abstainers and voided loops judge nothing', () => {
    expect(betResult(null, r)).toBeNull();
    expect(betResult('over', { ...r, voided: true, truth: null })).toBeNull();
  });
});

describe('confessionStamp (spec 5.4 phase 5: TRUE/FALSE slam + HUNG JURY; verdict enum mirrors engine)', () => {
  const base = { truth: false, verdict: 'FOOLED' as const, voided: false };
  it('a sold lie stamps FALSE and pays the confessor', () => {
    const v = confessionStamp(base, 'ZOE');
    expect(v.stamp).toBe('FALSE');
    expect(v.banner).toContain('+3');
  });
  it('a disbelieved truth stamps TRUE and still pays (jury guessed wrong)', () => {
    const v = confessionStamp({ ...base, truth: true }, 'ZOE');
    expect(v.stamp).toBe('TRUE');
    expect(v.banner).toContain('+3');
  });
  it('caught cap stamps FALSE with no payout line', () => {
    const v = confessionStamp({ ...base, verdict: 'CAUGHT' }, 'ZOE');
    expect(v.stamp).toBe('FALSE');
    expect(v.banner).not.toContain('+3');
  });
  it('hung jury: the liar walks (tie goes to the confessor, spec 5.4)', () => {
    const v = confessionStamp({ ...base, truth: true, verdict: 'HUNG' }, 'ZOE');
    expect(v.stamp).toBe('TRUE');
    expect(v.banner).toContain('HUNG JURY');
    expect(v.banner).toContain('+3');
  });
  it('fifth/voided: no stamp, no truth value rendered — nothing was fabricated', () => {
    const v = confessionStamp({ truth: null, verdict: null, voided: true }, 'ZOE');
    expect(v.stamp).toBeNull();
    expect(v.banner).not.toMatch(/TRUE|FALSE/);
  });
});

describe('juryResult (+1 for a correct believe/cap, spec 4.6)', () => {
  it('believe a truth = right; believe a cap = wrong', () => {
    expect(juryResult('believe', true)).toBe('right');
    expect(juryResult('believe', false)).toBe('wrong');
    expect(juryResult('cap', false)).toBe('right');
  });
  it('no vote or no truth -> nothing to judge', () => {
    expect(juryResult(null, true)).toBeNull();
    expect(juryResult('cap', null)).toBeNull();
  });
});
