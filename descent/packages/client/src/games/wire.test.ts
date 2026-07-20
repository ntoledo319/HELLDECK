import { describe, expect, it } from 'vitest';
import {
  asDeckView,
  confessionPayload,
  deckOf,
  fillinPayload,
  overunderPayload,
  subOf,
  type FillinVoteView,
  type OverUnderDebateView,
} from './wire';

describe('asDeckView (deck+sub guard — sub alone collides across games)', () => {
  const fillinVote = { deck: 'fillin', sub: 'VOTE', ballot: [] };
  it('accepts only a matching deck AND sub', () => {
    expect(asDeckView<FillinVoteView>(fillinVote, 'fillin', 'VOTE')).not.toBeNull();
  });
  it("rejects roast's VOTE for fillin and vice versa (the asView() trap)", () => {
    const roastVote = { deck: 'roast', sub: 'VOTE' };
    expect(asDeckView<FillinVoteView>(roastVote, 'fillin', 'VOTE')).toBeNull();
    expect(asDeckView<OverUnderDebateView>(fillinVote, 'overunder', 'DEBATE')).toBeNull();
  });
  it('null / primitives / missing discriminants never match', () => {
    expect(asDeckView(null, 'fillin', 'VOTE')).toBeNull();
    expect(asDeckView('VOTE', 'fillin', 'VOTE')).toBeNull();
    expect(asDeckView({ sub: 'VOTE' }, 'fillin', 'VOTE')).toBeNull();
  });
});

describe('deckOf / subOf', () => {
  it('reads discriminants defensively', () => {
    expect(deckOf({ deck: 'overunder', sub: 'BET' })).toBe('overunder');
    expect(subOf({ deck: 'overunder', sub: 'BET' })).toBe('BET');
    expect(deckOf(null)).toBeNull();
    expect(subOf({ deck: 'x' })).toBeNull();
    expect(deckOf({ deck: 42 })).toBeNull();
  });
});

// Over/Under payload keys are LAW: engine games/overunder.ts parseLine reads
// {line, lock}, parseBet reads {bet}, parseCount reads {truth}/{claim},
// parsePit reads {pit}. A renamed key silently no-ops the input.
describe('overunderPayload — engine parser contract', () => {
  it('dial moves ride the "line" key, no lock flag', () => {
    expect(overunderPayload.dial(42)).toEqual({ line: 42 });
  });
  it('LOCK THE LINE carries the final value + lock:true', () => {
    expect(overunderPayload.lock(47)).toEqual({ line: 47, lock: true });
  });
  it('bets, truths, host claims, pit ballots', () => {
    expect(overunderPayload.bet('over')).toEqual({ bet: 'over' });
    expect(overunderPayload.truth(63)).toEqual({ truth: 63 });
    expect(overunderPayload.claim(12)).toEqual({ claim: 12 }); // scribe-downgrade (4.7)
    expect(overunderPayload.pit('pit')).toEqual({ pit: 'pit' });
    expect(overunderPayload.pit('drag')).toEqual({ pit: 'drag' });
  });
});

// Fill-In keys are LAW: engine games/fillin.ts parseAnswer reads {answer},
// parseTone reads {tone:index}, parseTake reads {take:'A'|'B'}, parseVote reads
// {vote:id}, and the panic/next/burn/read flags are read as {key:true}.
// A renamed key silently no-ops the input.
describe('fillinPayload — engine parser contract', () => {
  it('answer submits, tone rides a numeric index (not a label)', () => {
    expect(fillinPayload.answer('my line')).toEqual({ answer: 'my line' });
    expect(fillinPayload.tone(0)).toEqual({ tone: 0 });
    expect(fillinPayload.tone(3)).toEqual({ tone: 3 });
  });
  it('the panic shelf is two steps: OPEN ({panic:true}) then TAKE ({take:A|B})', () => {
    expect(fillinPayload.panicOpen()).toEqual({ panic: true });
    expect(fillinPayload.panicTake('A')).toEqual({ take: 'A' });
    expect(fillinPayload.panicTake('B')).toEqual({ take: 'B' });
  });
  it('teleprompter flags: next / burn / read are bare booleans', () => {
    expect(fillinPayload.next()).toEqual({ next: true });
    expect(fillinPayload.burn()).toEqual({ burn: true });
    expect(fillinPayload.read()).toEqual({ read: true });
  });
  it('vote carries the ballot entry id as a number', () => {
    expect(fillinPayload.vote(3)).toEqual({ vote: 3 });
  });
});

// Confession keys are LAW: engine games/confession.ts parsePick reads {pick:index},
// parseTruth reads {truth:boolean}, parseJuryVote reads {vote}, parsePit reads {pit}.
describe('confessionPayload — engine parser contract', () => {
  it('pick is a HAND INDEX, not a card id (the room must never see ids it could count)', () => {
    expect(confessionPayload.pick(2)).toEqual({ pick: 2 });
  });
  it('truth-lock, verdicts, pit', () => {
    expect(confessionPayload.lock(false)).toEqual({ truth: false });
    expect(confessionPayload.lock(true)).toEqual({ truth: true });
    expect(confessionPayload.verdict('cap')).toEqual({ vote: 'cap' });
    expect(confessionPayload.verdict('believe')).toEqual({ vote: 'believe' });
    expect(confessionPayload.pit('drag')).toEqual({ pit: 'drag' });
  });
});
