// Scoring (spec 4.6) — one test per table row, plus multipliers and the
// Devil's Bargain double. Task D-125 engine side.
import { describe, expect, it } from 'vitest';
import {
  SCORE_TABLE,
  applyMultipliers,
  awardScores,
  damageMeterPoints,
  points,
  readerFirePoints,
  roomHeatHit,
} from '../src/scoring.js';
import { mkNightRoom, mkPlayers, mkSpec } from './helpers.js';

describe('the 4.6 table, row by row', () => {
  it('Fill-In: winning punchline +3', () => expect(points('fillin.win')).toBe(3));
  it('Fill-In: finale Reader +1 per 10 fires during the read, cap +3', () => {
    expect(readerFirePoints(0)).toBe(0);
    expect(readerFirePoints(9)).toBe(0);
    expect(readerFirePoints(10)).toBe(1);
    expect(readerFirePoints(29)).toBe(2);
    expect(readerFirePoints(30)).toBe(3);
    expect(readerFirePoints(999)).toBe(3);
  });
  it('Fill-In: panic-button win +1 (half of +3, rounded down)', () =>
    expect(points('fillin.panicWin')).toBe(Math.floor(3 / 2)));
  it('Roast: voted the plurality victim +2', () => expect(points('roast.pluralityVote')).toBe(2));
  it('Roast: Room Heat +1 extra to plurality voters', () => expect(points('roast.roomHeat')).toBe(1));
  it('Roast: heat thresholds scale with N (unanimity <=5, 80% at 6-7, 60% at >=8)', () => {
    expect(roomHeatHit(2, 2, 3)).toBe(true); // unanimity minus victim
    expect(roomHeatHit(1, 2, 3)).toBe(false);
    expect(roomHeatHit(4, 5, 6)).toBe(true); // 0.8 at N=6-7
    expect(roomHeatHit(3, 5, 7)).toBe(false); // 0.6 < 0.8
    expect(roomHeatHit(3, 5, 8)).toBe(true); // 0.6 at N>=8
    expect(roomHeatHit(2, 5, 12)).toBe(false);
    expect(roomHeatHit(0, 0, 5)).toBe(false);
  });
  it('Over/Under: correct bet +2', () => expect(points('overunder.correctBet')).toBe(2));
  it('Over/Under: exact-line push -> everyone +1', () => expect(points('overunder.push')).toBe(1));
  it('Confession: jury fooled -> confessor +3', () => expect(points('confession.fooled')).toBe(3));
  it('Confession: correct believe/cap vote +1', () => expect(points('confession.correctVote')).toBe(1));
  it("Poison: winning pitch +3", () => expect(points('poison.pitchWin')).toBe(3));
  it('Poison N=3: damage-meter score = rating; exact self-predict +2', () => {
    expect(damageMeterPoints(1)).toBe(1);
    expect(damageMeterPoints(5)).toBe(5);
    expect(points('poison.predictExact')).toBe(2);
  });
  it('Red Flag: SMASH verdict -> defender +3', () => expect(points('redflag.smash')).toBe(3));
  it('Alibi: jury +1 per planted word found', () => expect(points('alibi.plantFound', 3)).toBe(3));
  it('Alibi: accused +1 per word missed by >50% of jury', () => expect(points('alibi.wordMissed', 2)).toBe(2));
  it('Scatter: survivors +1, holder +0', () => {
    expect(points('scatter.survive')).toBe(1);
    expect(points('scatter.holder')).toBe(0);
  });
  it('Text Trap: SURVIVED verdict +3', () => expect(points('texttrap.survived')).toBe(3));
  it('Reality: ego-gap <=1 -> subject +2; gap >=4 -> every debater +1', () => {
    expect(points('reality.egoGap')).toBe(2);
    expect(points('reality.calledIt')).toBe(1);
  });
  it('participation: +1, and the table row is worth exactly 1 (once-per-circle is module-enforced)', () =>
    expect(points('participation')).toBe(1));
  it('PLEAD THE FIFTH / voided round: 0 to all', () => expect(points('fifth')).toBe(0));
  it('the table has no stray rows (every id is intentional)', () => {
    expect(Object.keys(SCORE_TABLE).sort()).toMatchInlineSnapshot(`
      [
        "alibi.plantFound",
        "alibi.wordMissed",
        "confession.correctVote",
        "confession.fooled",
        "fifth",
        "fillin.panicWin",
        "fillin.win",
        "overunder.correctBet",
        "overunder.push",
        "participation",
        "poison.pitchWin",
        "poison.predictExact",
        "reality.calledIt",
        "reality.egoGap",
        "redflag.smash",
        "roast.pluralityVote",
        "roast.roomHeat",
        "scatter.holder",
        "scatter.survive",
        "texttrap.survived",
      ]
    `);
  });
});

describe('multipliers', () => {
  it('finale x3', () => expect(applyMultipliers(2, { finale: true, bargain: false })).toBe(6));
  it('Devil’s Bargain x2', () => expect(applyMultipliers(2, { finale: false, bargain: true })).toBe(4));
  it('x2 stacks before x3 (impossible by construction, but the order is law)', () =>
    expect(applyMultipliers(1, { finale: true, bargain: true })).toBe(6));
  it('no multipliers = base', () => expect(applyMultipliers(3, { finale: false, bargain: false })).toBe(3));
});

describe('awardScores', () => {
  it('applies pre-multiplier module scores to players', () => {
    const s = mkNightRoom(mkSpec('roast'), mkPlayers(4));
    const out = awardScores(s, { P1: 3, P2: 1 }, 0);
    expect(out.players.map((p) => p.score)).toEqual([0, 3, 1, 0]);
  });

  it('finale circle: all points x3', () => {
    const s = mkNightRoom(mkSpec('roast', { finale: true }), mkPlayers(4));
    const out = awardScores(s, { P1: 2 }, 0);
    expect(out.players[1]?.score).toBe(6);
  });

  it('Devil’s Bargain holder doubles ON THEIR CIRCLE only; nobody else doubles', () => {
    const s = mkNightRoom(mkSpec('roast'), mkPlayers(4), {
      devilsBargain: { holder: 'P2', circle: 0 },
    });
    const out = awardScores(s, { P1: 2, P2: 2 }, 0);
    expect(out.players[1]?.score).toBe(2); // not the holder
    expect(out.players[2]?.score).toBe(4); // holder, bargain circle -> x2
  });

  it('the bargain does NOT apply on other circles', () => {
    const s = mkNightRoom(mkSpec('roast'), mkPlayers(4), {
      devilsBargain: { holder: 'P2', circle: 3 },
    });
    const out = awardScores(s, { P2: 2 }, 0);
    expect(out.players[2]?.score).toBe(2);
  });
});
