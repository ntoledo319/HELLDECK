// Consent filter (spec 4.4) — task D-114 engine side. Ceilings cap exposure,
// invisibly; genericCeiling flips from min to second-lowest at N=5.
import { describe, expect, it } from 'vitest';
import type { Tier } from '../src/types.js';
import { cardLegal, genericCeiling, maxLegalE, secondLowest } from '../src/consent.js';
import { rng } from '../src/rng.js';

const t = (n: number): Tier => n as Tier;

describe('genericCeiling', () => {
  it('N<=4 uses the MINIMUM ceiling (small rooms have nowhere to hide)', () => {
    expect(genericCeiling([t(2), t(4), t(5)])).toBe(2);
    expect(genericCeiling([t(1), t(5), t(5), t(5)])).toBe(1);
  });

  it('N>=5 uses the SECOND-LOWEST (one shy player caps cards about them, not the night)', () => {
    expect(genericCeiling([t(1), t(3), t(3), t(4), t(5)])).toBe(3);
    expect(genericCeiling([t(2), t(2), t(5), t(5), t(5), t(5)])).toBe(2);
  });

  it('the N=4 -> N=5 boundary is exactly where the rule flips', () => {
    const four: Tier[] = [t(1), t(5), t(5), t(5)];
    expect(genericCeiling(four)).toBe(1);
    expect(genericCeiling([...four, t(5)])).toBe(5); // second-lowest of [1,5,5,5,5]
  });

  it('degenerate inputs stay sane', () => {
    expect(genericCeiling([])).toBe(1);
    expect(genericCeiling([t(4)])).toBe(4);
    expect(secondLowest([7])).toBe(7);
    expect(secondLowest([])).toBe(1);
  });
});

describe('cardLegal (4.4 pseudocode, branch by branch)', () => {
  const ceilings: Tier[] = [t(2), t(4), t(5), t(5), t(5)]; // N=5, generic=4, min=2

  it('a card naming subjects obeys the MIN of the named subjects only', () => {
    expect(cardLegal(t(4), { ceilings, subjectCeilings: [t(4), t(5)] })).toBe(true);
    expect(cardLegal(t(4), { ceilings, subjectCeilings: [t(2), t(5)] })).toBe(false);
    expect(cardLegal(t(2), { ceilings, subjectCeilings: [t(2)] })).toBe(true);
  });

  it('vote-emergent (roast) above E3 obeys the MIN of ALL ceilings', () => {
    expect(cardLegal(t(4), { ceilings, voteEmergent: true })).toBe(false); // min=2
    const feral: Tier[] = [t(4), t(4), t(5), t(5), t(5)];
    expect(cardLegal(t(4), { ceilings: feral, voteEmergent: true })).toBe(true);
    expect(cardLegal(t(5), { ceilings: feral, voteEmergent: true })).toBe(false);
  });

  it('vote-emergent at E<=3 falls through to the generic ceiling', () => {
    expect(cardLegal(t(3), { ceilings, voteEmergent: true })).toBe(true); // generic=4
    const cold: Tier[] = [t(1), t(2), t(2)]; // N=3 -> generic=min=1
    expect(cardLegal(t(2), { ceilings: cold, voteEmergent: true })).toBe(false);
  });

  it('generic cards obey genericCeiling', () => {
    expect(cardLegal(t(4), { ceilings })).toBe(true);
    expect(cardLegal(t(5), { ceilings })).toBe(false);
  });

  it('property: a subject-named card NEVER exceeds any named subject ceiling (500 rolls)', () => {
    const rand = rng('consent-prop');
    for (let i = 0; i < 500; i++) {
      const n = 3 + Math.floor(rand() * 10);
      const all = Array.from({ length: n }, () => t(1 + Math.floor(rand() * 5)));
      const nSubj = 1 + Math.floor(rand() * 2);
      const subj = all.slice(0, nSubj);
      const e = t(1 + Math.floor(rand() * 5));
      if (cardLegal(e, { ceilings: all, subjectCeilings: subj })) {
        for (const c of subj) expect(e).toBeLessThanOrEqual(c);
      }
    }
  });
});

describe('maxLegalE', () => {
  it('vote-emergent tops out at generic when min blocks 4-5', () => {
    const ceilings: Tier[] = [t(2), t(5), t(5), t(5), t(5)]; // min=2, generic=5
    expect(maxLegalE({ ceilings, voteEmergent: true })).toBe(3); // E4/E5 need min>=4
    expect(maxLegalE({ ceilings })).toBe(5);
  });

  it('never returns below 1 and never above the applicable cap', () => {
    expect(maxLegalE({ ceilings: [t(1), t(1), t(1)] })).toBe(1);
    expect(maxLegalE({ ceilings: [t(5), t(5), t(5), t(5), t(5)], voteEmergent: true })).toBe(5);
  });
});
