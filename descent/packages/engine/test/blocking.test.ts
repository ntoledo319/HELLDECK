// Blocking-input terminal machine (spec 4.7) — task D-115. Unit layer for the
// reusable helper; the engine integration (WAITING_ON phase, seat lapse routing)
// lives in engine.test.ts.
import { describe, expect, it } from 'vitest';
import {
  BLOCKING_GRACE_MS,
  BLOCKING_PIT_MS,
  blockingBegin,
  blockingControl,
  blockingGraceTimer,
  blockingPitTimer,
  blockingPitVote,
  blockingResolveInput,
  blockingTimerFired,
  type BlockingJuror,
  type BlockingState,
} from '../src/engine.js';

const NOW = 500_000;
const KEY = '7';
const fresh = (): BlockingState => blockingBegin('P1', 'truth', KEY, NOW).blocking;
const JURORS: BlockingJuror[] = [
  { id: 'P0', weight: 1 },
  { id: 'P2', weight: 1 },
  { id: 'P3', weight: 1 },
];

describe('blockingBegin', () => {
  it('pauses the deadline (INPUT deadline null) and arms grace + pit timers', () => {
    const out = blockingBegin('P1', 'truth', KEY, NOW);
    expect(out.$phase).toEqual({ k: 'INPUT', sub: 'truth', deadline: null });
    expect(out.effects).toEqual([
      { k: 'SCHEDULE', timerId: blockingGraceTimer(KEY), atMs: NOW + BLOCKING_GRACE_MS },
      { k: 'SCHEDULE', timerId: blockingPitTimer(KEY), atMs: NOW + BLOCKING_PIT_MS },
    ]);
    expect(out.blocking.resolved).toBeNull();
    expect(out.blocking.pitOpen).toBe(false);
  });
});

describe('shame choreography', () => {
  it('12s grace -> public WAITING_ON the owner', () => {
    const out = blockingTimerFired(fresh(), blockingGraceTimer(KEY), NOW + BLOCKING_GRACE_MS);
    expect(out.$phase).toEqual({ k: 'WAITING_ON', who: 'P1', since: NOW + BLOCKING_GRACE_MS });
  });

  it('30s -> the pit vote opens for the room', () => {
    const out = blockingTimerFired(fresh(), blockingPitTimer(KEY), NOW + BLOCKING_PIT_MS);
    expect(out.blocking.pitOpen).toBe(true);
    expect(out.$phase).toBeUndefined(); // not a phase change — buttons appear via broadcast
  });

  it('foreign timer ids are inert', () => {
    const out = blockingTimerFired(fresh(), 'roast:hold:0:0', NOW + 1);
    expect(out.blocking).toEqual(fresh());
    expect(out.effects).toEqual([]);
  });
});

describe('pit vote (>=60% weighted = void)', () => {
  const open = (): BlockingState => ({ ...fresh(), pitOpen: true });

  it('votes before the pit opens are inert', () => {
    const out = blockingPitVote(fresh(), 'P2', 'pit', JURORS);
    expect(out.blocking.pitVotes).toEqual({});
  });

  it('2/3 pit (66%) voids the loop with the void sting', () => {
    const one = blockingPitVote(open(), 'P2', 'pit', JURORS);
    expect(one.blocking.resolved).toBeNull();
    const two = blockingPitVote(one.blocking, 'P3', 'pit', JURORS);
    expect(two.blocking.resolved).toBe('voided');
    expect(two.blocking.voidReason).toBe('pit');
    expect(two.effects).toContainEqual({ k: 'AUDIO', sting: 'void' });
    expect(two.effects).toContainEqual({ k: 'CANCEL', timerId: blockingGraceTimer(KEY) });
    expect(two.effects).toContainEqual({ k: 'CANCEL', timerId: blockingPitTimer(KEY) });
  });

  it('1/3 pit (33%) does not void; DRAG votes never void', () => {
    const one = blockingPitVote(open(), 'P2', 'pit', JURORS);
    const drag = blockingPitVote(one.blocking, 'P3', 'drag', JURORS);
    expect(drag.blocking.resolved).toBeNull();
  });

  it('imp half-weight counts toward the 60%', () => {
    const jurors: BlockingJuror[] = [
      { id: 'P0', weight: 1 },
      { id: 'I0', weight: 0.5 },
    ]; // total 1.5, threshold 0.9
    const one = blockingPitVote(open(), 'I0', 'pit', jurors);
    expect(one.blocking.resolved).toBeNull(); // 0.5 < 0.9
    const two = blockingPitVote(one.blocking, 'P0', 'pit', jurors);
    expect(two.blocking.resolved).toBe('voided'); // 1.5 >= 0.9
  });

  it('the owner and non-jurors cannot vote themselves into the pit', () => {
    expect(blockingPitVote(open(), 'P1', 'pit', JURORS).blocking.pitVotes).toEqual({});
    expect(blockingPitVote(open(), 'GHOST', 'pit', JURORS).blocking.pitVotes).toEqual({});
  });
});

describe('terminal controls', () => {
  it('PLEAD THE FIFTH: owner only, fifth sting, timers cancelled', () => {
    const out = blockingControl(fresh(), 'P1', 'FIFTH', 'P0');
    expect(out.blocking.resolved).toBe('fifth');
    expect(out.effects).toContainEqual({ k: 'AUDIO', sting: 'fifth' });
    expect(blockingControl(fresh(), 'P2', 'FIFTH', 'P0').blocking.resolved).toBeNull();
  });

  it('host VOID kills the loop', () => {
    const out = blockingControl(fresh(), 'P0', 'VOID', 'P0');
    expect(out.blocking.resolved).toBe('voided');
    expect(out.blocking.voidReason).toBe('host');
    expect(out.effects).toContainEqual({ k: 'AUDIO', sting: 'void' });
  });

  it("seat-lapse VOID (owner's id, not host) auto-voids with THE WITNESS FLED", () => {
    const out = blockingControl(fresh(), 'P1', 'VOID', 'P0');
    expect(out.blocking.resolved).toBe('voided');
    expect(out.blocking.voidReason).toBe('lapse');
    expect(out.effects).toContainEqual({ k: 'AUDIO', sting: 'fled' });
  });

  it('VOID from a random non-host non-owner is inert', () => {
    expect(blockingControl(fresh(), 'P2', 'VOID', 'P0').blocking.resolved).toBeNull();
  });

  it('resolved states are terminal — later controls, votes, and timers are inert', () => {
    const voided = blockingControl(fresh(), 'P0', 'VOID', 'P0').blocking;
    expect(blockingControl(voided, 'P1', 'FIFTH', 'P0').blocking.resolved).toBe('voided');
    expect(blockingPitVote({ ...voided, pitOpen: true }, 'P2', 'pit', JURORS).blocking).toEqual({
      ...voided,
      pitOpen: true,
    });
    expect(blockingTimerFired(voided, blockingGraceTimer(KEY), NOW + 99).$phase).toBeUndefined();
  });
});

describe('NEVER fabricate a truth value (D-115)', () => {
  it("no terminal path resolves to 'input': only blockingResolveInput does, with a real answer in hand", () => {
    const terminals = [
      blockingControl(fresh(), 'P1', 'FIFTH', 'P0').blocking,
      blockingControl(fresh(), 'P0', 'VOID', 'P0').blocking,
      blockingControl(fresh(), 'P1', 'VOID', 'P0').blocking,
      blockingPitVote(
        blockingPitVote({ ...fresh(), pitOpen: true }, 'P2', 'pit', JURORS).blocking,
        'P3',
        'pit',
        JURORS,
      ).blocking,
    ];
    for (const b of terminals) expect(b.resolved).not.toBe('input');
    // The machine carries no answer field at all — there is nothing TO fabricate.
    expect(Object.keys(fresh()).some((k) => /answer|value|truth/i.test(k))).toBe(false);
    // And the only 'input' path is the explicit resolve call:
    expect(blockingResolveInput(fresh()).blocking.resolved).toBe('input');
  });
});
