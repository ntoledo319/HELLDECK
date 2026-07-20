// Arc builder (spec 4.3) — grammar property test: 1000 seeds x N in {3,4,6,8,10,12}
// x depth in {5,7,9} never violates the grammar. Task D-124 (built early for M1).
import { describe, expect, it } from 'vitest';
import type { CircleSpec, Player, Tier } from '../src/types.js';
import { GAMES, bargainSlotFor, buildArc, lastThirdStart, rungFor } from '../src/arc.js';
import { genericCeiling, maxLegalE } from '../src/consent.js';
import { rng } from '../src/rng.js';
import { mkConfig, mkPlayer } from './helpers.js';

const NS = [3, 4, 6, 8, 10, 12] as const;
const DEPTHS = [5, 7, 9] as const;
const VIBES = ['sober', 'warm', 'feral'] as const;

function crew(n: number, seedRand: () => number): Player[] {
  return Array.from({ length: n }, (_, i) =>
    mkPlayer(`P${i}`, i === 0 ? 'host' : 'player', i, {
      heatCeiling: (1 + Math.floor(seedRand() * 5)) as Tier,
    }),
  );
}

function assertGrammar(arc: CircleSpec[], depth: number, n: number, label: string): void {
  const why = (rule: string): string => `${label}: ${rule} — arc=[${arc.map((c) => c.game).join(',')}]`;

  // length + finale placement
  expect(arc, why('length')).toHaveLength(depth);

  // slot 0: simultaneous-input opener
  const opener = arc[0] as CircleSpec;
  expect(GAMES[opener.game].simultaneous, why('slot0 simultaneous')).toBe(true);

  // spikes: scatter at mid; titlefight at mid+2 when depth >= 7
  const mid = Math.floor(depth / 2);
  expect((arc[mid] as CircleSpec).game, why('mid spike')).toBe('scatter');
  if (depth >= 7) expect((arc[mid + 2] as CircleSpec).game, why('second spike')).toBe('titlefight');

  // finale: all-players-score; N<=4 prefers overunder (never blocked at fill time)
  const finale = arc[depth - 1] as CircleSpec;
  expect(finale.finale, why('finale flag')).toBe(true);
  expect(GAMES[finale.game].allPlayersScore, why('finale all-players-score')).toBe(true);
  if (n <= 4) expect(finale.game, why('N<=4 finale prefers overunder')).toBe('overunder');
  expect(arc.filter((c) => c.finale), why('exactly one finale')).toHaveLength(1);

  for (let i = 0; i < depth; i++) {
    const spec = arc[i] as CircleSpec;
    const info = GAMES[spec.game];
    // minN gates + availability + irl gate
    expect(info.minN, why(`minN gate at ${i}`)).toBeLessThanOrEqual(n);
    expect(info.available, why(`availability at ${i}`)).toBe(true);
    // spotlight games only after slot 1
    if (info.spotlight) expect(i, why(`spotlight gate at ${i}`)).toBeGreaterThanOrEqual(2);
    // loop counts per the 4.3 table
    expect(spec.loops, why(`loops at ${i}`)).toBe(GAMES[spec.game].loops(n));
    // rung sanity
    expect(spec.rung, why(`rung>=1 at ${i}`)).toBeGreaterThanOrEqual(1);
    expect(spec.rung, why(`rung<=5 at ${i}`)).toBeLessThanOrEqual(5);
    // no adjacent subject-targeting after slot 4
    if (i >= 4) {
      const prev = arc[i - 1] as CircleSpec;
      expect(
        info.subjectTargeting && GAMES[prev.game].subjectTargeting,
        why(`subject-targeting adjacency at ${i}`),
      ).toBe(false);
    }
    // same game never adjacent (repeat law)
    if (i > 0) expect(spec.game, why(`adjacent repeat at ${i}`)).not.toBe((arc[i - 1] as CircleSpec).game);
  }

  // repeats: only {roast, scatter, overunder}, max 1 repeat each
  const counts = new Map<string, number>();
  for (const c of arc) counts.set(c.game, (counts.get(c.game) ?? 0) + 1);
  for (const [game, cnt] of counts) {
    const cap = GAMES[game as CircleSpec['game']].repeatable ? 2 : 1;
    expect(cnt, why(`repeat budget for ${game}`)).toBeLessThanOrEqual(cap);
  }

  // outward per ceil(depth/3) block: every block after the first contains an outward
  // circle, unless the block is exactly the finale. (First block is structurally
  // inward: slot-0 mandate + spotlight gate — documented in arc.ts.)
  const blockSize = Math.ceil(depth / 3);
  for (let start = blockSize; start < depth; start += blockSize) {
    const block = arc.slice(start, start + blockSize);
    const finaleOnly = block.length === 1 && block[0]?.finale === true;
    if (!finaleOnly) {
      expect(block.some((c) => c.outward), why(`outward in block @${start}`)).toBe(true);
    }
  }

  // Devil's Bargain: exactly one, all-players-score, in the last third, never the finale
  const bargains = arc.map((c, i) => (c.bargain ? i : -1)).filter((i) => i >= 0);
  expect(bargains, why('exactly one bargain')).toHaveLength(1);
  const bIdx = bargains[0] as number;
  expect(bIdx, why('bargain in last third')).toBeGreaterThanOrEqual(lastThirdStart(depth));
  expect(bIdx, why('bargain never on finale')).toBeLessThan(depth - 1);
  expect(GAMES[(arc[bIdx] as CircleSpec).game].allPlayersScore, why('bargain all-players-score')).toBe(true);
}

describe('buildArc grammar property (1000 seeds x N x depth)', () => {
  for (const depth of DEPTHS) {
    for (const n of NS) {
      it(`never violates the grammar at N=${n}, depth=${depth}`, () => {
        for (let seedN = 0; seedN < 1000; seedN++) {
          const seed = `prop-${seedN}`;
          const vibe = VIBES[seedN % 3] as (typeof VIBES)[number];
          const players = crew(n, rng(`crew-${seed}-${n}`));
          const arc = buildArc(mkConfig(depth, { vibe }), players, seed);
          assertGrammar(arc, depth, n, `seed=${seed} N=${n} d=${depth} vibe=${vibe}`);
        }
      });
    }
  }
});

describe('buildArc determinism & inputs', () => {
  it('same (config, players, seed) -> identical arc', () => {
    const players = crew(6, rng('det'));
    const a = buildArc(mkConfig(7), players, 'same-seed');
    const b = buildArc(mkConfig(7), players, 'same-seed');
    expect(a).toEqual(b);
  });

  it('different seeds vary the arc (not a constant table)', () => {
    const players = crew(8, rng('vary'));
    const arcs = new Set(
      Array.from({ length: 30 }, (_, i) => buildArc(mkConfig(9), players, `s${i}`).map((c) => c.game).join(',')),
    );
    expect(arcs.size).toBeGreaterThan(1);
  });

  it('imps are excluded from N (arc math uses actives only)', () => {
    const players = [...crew(4, rng('imp')), mkPlayer('IMP', 'imp', 4)];
    const arc = buildArc(mkConfig(5), players, 'imp-seed');
    expect((arc[4] as CircleSpec).game).toBe('overunder'); // N<=4 finale rule held
  });

  it('bargainSlotFor lands inside [lastThirdStart, depth-2] for every depth', () => {
    for (const d of DEPTHS) {
      expect(bargainSlotFor(d)).toBeGreaterThanOrEqual(lastThirdStart(d));
      expect(bargainSlotFor(d)).toBeLessThanOrEqual(d - 2);
    }
  });
});

describe('E-curve (rungFor)', () => {
  const ceilings: Tier[] = [5, 5, 5, 5, 5];

  it('start rung = min(vibe rung, generic-2), floor 1 — ceiling always wins', () => {
    expect(rungFor(0, 7, 'fillin', 'feral', ceilings)).toBe(3);
    expect(rungFor(0, 7, 'fillin', 'sober', ceilings)).toBe(1);
    const cold: Tier[] = [2, 2, 2]; // generic=2 -> start floor 1 even feral
    expect(rungFor(0, 7, 'fillin', 'feral', cold)).toBe(1);
  });

  it('ramps to maxLegal by the finale', () => {
    expect(rungFor(6, 7, 'fillin', 'feral', ceilings)).toBe(5);
    expect(rungFor(6, 7, 'fillin', 'sober', ceilings)).toBe(5); // same summit, longer climb
  });

  it('is clamped by the per-game tier ceiling (vote-emergent min rule)', () => {
    const mixed: Tier[] = [2, 5, 5, 5, 5]; // generic=5, min=2 -> roast tops at 3
    expect(rungFor(6, 7, 'roast', 'feral', mixed)).toBe(maxLegalE({ ceilings: mixed, voteEmergent: true }));
    expect(rungFor(6, 7, 'roast', 'feral', mixed)).toBe(3);
  });

  it('the ramp never decreases along the night', () => {
    for (const vibe of VIBES) {
      let prev = 0;
      for (let c = 0; c < 9; c++) {
        const r = rungFor(c, 9, 'fillin', vibe, ceilings);
        expect(r).toBeGreaterThanOrEqual(prev);
        prev = r;
      }
    }
  });

  it('generic ceiling honors the N<=4 min rule end-to-end', () => {
    const four: Tier[] = [1, 5, 5, 5];
    expect(genericCeiling(four)).toBe(1);
    expect(rungFor(4, 5, 'fillin', 'feral', four)).toBe(1); // whole night capped
  });
});
