// Deal ceremony + burn absorption (spec 4.5) — task D-115 unit layer.
// The engine-level byte-equality test lives in engine.test.ts; this file covers
// the ceremony helper itself.
import { describe, expect, it } from 'vitest';
import { CEREMONY_MS, PREVIEW_MS, beginDeal, burnDeal, completeDeal, pickSpotlight } from '../src/deal.js';
import { rng } from '../src/rng.js';
import { mkCard, mkPlayer } from './helpers.js';

const NOW = 1_000_000;
const primary = mkCard('poison_v3_001');
const backup = mkCard('poison_v3_002');

describe('beginDeal', () => {
  it('no subject: fixed 5.5s ritual, no preview SEND', () => {
    const { deal, effects } = beginDeal({ primary, backup, subjectId: null }, 'deal:2', NOW);
    expect(deal.completesAt).toBe(NOW + CEREMONY_MS);
    expect(effects).toEqual([{ k: 'SCHEDULE', timerId: 'deal:2', atMs: NOW + CEREMONY_MS }]);
  });

  it('named subject: 10s ceremony and a PRIVATE preview to the subject ONLY', () => {
    const { deal, effects } = beginDeal({ primary, backup, subjectId: 'P1' }, 'deal:2', NOW);
    expect(deal.completesAt).toBe(NOW + PREVIEW_MS);
    expect(deal.burnWindowEndsAt).toBe(NOW + PREVIEW_MS);
    expect(effects).toEqual([
      { k: 'SCHEDULE', timerId: 'deal:2', atMs: NOW + PREVIEW_MS },
      {
        k: 'SEND',
        to: 'P1',
        kind: 'preview',
        payload: { card: primary, burnDeadline: NOW + PREVIEW_MS },
      },
    ]);
  });
});

describe('burnDeal', () => {
  const fresh = (): ReturnType<typeof beginDeal> =>
    beginDeal({ primary, backup, subjectId: 'P1' }, 'deal:2', NOW);

  it('subject burn inside the window swaps to the backup and quarantines the vetoed card', () => {
    const { deal, burned } = burnDeal(fresh().deal, 'P1', NOW + 4000);
    expect(burned).toBe(true);
    expect(deal.card.id).toBe(backup.id);
    expect(deal.backup).toBeNull();
    expect(deal.burnedId).toBe(primary.id);
    expect(deal.completesAt).toBe(NOW + PREVIEW_MS); // the schedule NEVER moves
  });

  it('non-subject burns are inert', () => {
    const { deal, burned } = burnDeal(fresh().deal, 'P2', NOW + 4000);
    expect(burned).toBe(false);
    expect(deal.card.id).toBe(primary.id);
  });

  it('burns after the window are inert', () => {
    const { burned } = burnDeal(fresh().deal, 'P1', NOW + PREVIEW_MS + 1);
    expect(burned).toBe(false);
  });

  it('a second burn is inert (one backup per ceremony)', () => {
    const once = burnDeal(fresh().deal, 'P1', NOW + 2000).deal;
    const { deal, burned } = burnDeal(once, 'P1', NOW + 3000);
    expect(burned).toBe(false);
    expect(deal.card.id).toBe(backup.id);
  });

  it('burns on a completed deal are inert', () => {
    const done = completeDeal(fresh().deal).deal;
    expect(burnDeal(done, 'P1', NOW + 1000).burned).toBe(false);
  });
});

describe('completeDeal', () => {
  it('clean deal consumes only the dealt card (the backup returns to the pool)', () => {
    const { deal, usedCardIds } = completeDeal(beginDeal({ primary, backup, subjectId: null }, 'deal:2', NOW).deal);
    expect(deal.done).toBe(true);
    expect(usedCardIds).toEqual([primary.id]);
  });

  it('burned deal consumes the backup AND the vetoed card', () => {
    const burnt = burnDeal(beginDeal({ primary, backup, subjectId: 'P1' }, 'deal:2', NOW).deal, 'P1', NOW + 1000).deal;
    expect(completeDeal(burnt).usedCardIds).toEqual([backup.id, primary.id]);
  });
});

describe('pickSpotlight', () => {
  it('weights toward the lowest spotlightCount', () => {
    const players = [
      mkPlayer('HOG', 'player', 0, { spotlightCount: 9 }),
      mkPlayer('SHY', 'player', 1, { spotlightCount: 0 }),
    ];
    const rand = rng('spotlight');
    let shy = 0;
    for (let i = 0; i < 200; i++) if (pickSpotlight(players, rand).id === 'SHY') shy++;
    expect(shy).toBeGreaterThan(150); // weight 10 vs 1
  });

  it('is deterministic for a given rand stream', () => {
    const players = [mkPlayer('A', 'player', 0), mkPlayer('B', 'player', 1)];
    expect(pickSpotlight(players, rng('x')).id).toBe(pickSpotlight(players, rng('x')).id);
  });
});
