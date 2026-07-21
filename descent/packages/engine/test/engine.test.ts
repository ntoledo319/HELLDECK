// Night machine (spec 4.2/4.5/4.7 + 2.1) — tasks D-111/D-115/D-116 core.
// Integration layer: real events through reduce(), timers via the Driver harness.
// Fake modules register under Drop-1 deck ids (texttrap/reality/taboo) — buildArc
// never emits those in v1, so full-night runs stay pure roast + skip.
import { describe, expect, it } from 'vitest';
import type { PlayerId, RoomState, SpotlightRequest, SpotlightResolution } from '../src/types.js';
import type { GameCtx, GameModule } from '../src/games/module.js';
import {
  CORE_DEALT,
  CORE_REVEAL_DONE,
  CORE_SPOTLIGHT_DONE,
  EXPLAINER_FIRST_MS,
  INTRO_TRANSITION_MS,
  LADDER_MS,
  REVEAL_MIN_HOLD_MS,
  REVEAL_SOFTCAP_MS,
  SEAT_HOLD_MS,
  blockingBegin,
  blockingControl,
  blockingPitVote,
  blockingResolveInput,
  blockingTimerFired,
  computeJudgment,
  registerModule,
  uniqueName,
  type BlockingState,
} from '../src/engine.js';
import { PREVIEW_MS } from '../src/deal.js';
import { SPOTLIGHT_TOTAL_MS, SPOTLIGHT_WINDOW_MS } from '../src/spotlight.js';
import { Driver, lobbyDriver, mkCard, mkNightRoom, mkPlayer, mkPlayers, mkSpec } from './helpers.js';

// ===== fake modules =====
const revealFake: GameModule = {
  deck: 'texttrap',
  minN: 3,
  start: () => ({ gameState: { loop: 0, $phase: { k: 'REVEAL' } }, effects: [] }),
  input: (ctx) => ({ gameState: ctx.state.gameState, effects: [] }),
  timer: (ctx, id) => {
    const gs = ctx.state.gameState as { loop: number };
    if (id !== CORE_REVEAL_DONE) return { gameState: gs, effects: [] };
    if (gs.loop === 0) return { gameState: { loop: 1, $phase: { k: 'REVEAL' } }, effects: [] };
    return { gameState: gs, effects: [], done: true };
  },
  control: (ctx) => ({ gameState: ctx.state.gameState, effects: [] }),
  view: () => null,
};

const PRIMARY = mkCard('reality_primary', 'reality');
const BACKUP = mkCard('reality_backup', 'reality');
const dealFake: GameModule = {
  deck: 'reality',
  minN: 3,
  start: () => ({
    gameState: { stage: 'deal', $deal: { primary: PRIMARY, backup: BACKUP, subjectId: 'P1' } },
    effects: [],
  }),
  input: (ctx) => ({ gameState: ctx.state.gameState, effects: [] }),
  timer: (ctx, id) =>
    id === CORE_DEALT
      ? { gameState: { stage: 'debate', $phase: { k: 'INPUT', sub: 'debate', deadline: ctx.now + 25_000 } }, effects: [] }
      : { gameState: ctx.state.gameState, effects: [] },
  control: (ctx) => ({ gameState: ctx.state.gameState, effects: [] }),
  view: () => null,
};

const blockFake: GameModule = {
  deck: 'taboo',
  minN: 3,
  start: (ctx) => {
    const out = blockingBegin('P1', 'truth', String(ctx.state.epoch), ctx.now);
    return { gameState: { blk: out.blocking, $phase: out.$phase }, effects: out.effects };
  },
  timer: (ctx, id) => {
    const { blk } = ctx.state.gameState as { blk: BlockingState };
    const out = blockingTimerFired(blk, id, ctx.now);
    return {
      gameState: { blk: out.blocking, ...(out.$phase ? { $phase: out.$phase } : {}) },
      effects: out.effects,
    };
  },
  input: (ctx, pid, payload) => {
    const { blk } = ctx.state.gameState as { blk: BlockingState };
    const p = (payload ?? {}) as Record<string, unknown>;
    if (p['pit'] === 'pit' || p['pit'] === 'drag') {
      const jurors = [
        ...ctx.players.filter((q) => q.id !== blk.owner).map((q) => ({ id: q.id, weight: 1 })),
        ...ctx.imps.map((q) => ({ id: q.id, weight: 0.5 })),
      ];
      const out = blockingPitVote(blk, pid, p['pit'] as 'pit' | 'drag', jurors);
      return {
        gameState: { blk: out.blocking },
        effects: out.effects,
        done: out.blocking.resolved === 'voided',
      };
    }
    if ('answer' in p && pid === blk.owner && blk.resolved === null) {
      const out = blockingResolveInput(blk); // a REAL human answer — the only 'input' path
      return { gameState: { blk: out.blocking }, effects: out.effects, scores: { [pid]: 2 }, done: true };
    }
    return { gameState: ctx.state.gameState, effects: [] };
  },
  control: (ctx, pid, kind) => {
    const { blk } = ctx.state.gameState as { blk: BlockingState };
    if (kind !== 'FIFTH' && kind !== 'VOID') return { gameState: ctx.state.gameState, effects: [] };
    const hostId = ctx.players.find((q) => q.role === 'host')?.id ?? null;
    const out = blockingControl(blk, pid, kind, hostId);
    return { gameState: { blk: out.blocking }, effects: out.effects, done: out.blocking.resolved !== null };
  },
  view: () => null,
};

interface SpotlightFakeState {
  loop: number;
  loops: number;
  resolutions: SpotlightResolution[];
}

function subjectSpotlight(ctx: GameCtx): SpotlightRequest {
  return { roles: ['subject'], eligibleIds: ctx.players.map((player) => player.id) };
}

// `hotseat` is intentionally unregistered in production. This fake exercises the
// core's private assignment protocol without replacing any shipping game module.
const spotlightFake: GameModule = {
  deck: 'hotseat',
  minN: 3,
  start: (ctx) => ({
    gameState: {
      loop: 0,
      loops: ctx.circle.loops,
      resolutions: [],
      $spotlight: subjectSpotlight(ctx),
    },
    effects: [],
  }),
  timer: (ctx, id) => {
    const gs = ctx.state.gameState as SpotlightFakeState;
    if (id !== CORE_SPOTLIGHT_DONE || !ctx.spotlight) {
      return { gameState: gs, effects: [] };
    }
    const loop = gs.loop + 1;
    const resolutions = [...gs.resolutions, ctx.spotlight];
    if (loop < gs.loops) {
      return {
        gameState: { ...gs, loop, resolutions, $spotlight: subjectSpotlight(ctx) },
        effects: [],
      };
    }
    return {
      gameState: {
        ...gs,
        loop,
        resolutions,
        $phase: { k: 'INPUT', sub: 'resolved', deadline: ctx.now + 25_000 },
      },
      effects: [],
    };
  },
  input: (ctx) => ({ gameState: ctx.state.gameState, effects: [] }),
  control: (ctx) => ({ gameState: ctx.state.gameState, effects: [] }),
  view: () => null,
};

registerModule(revealFake);
registerModule(dealFake);
registerModule(blockFake);
registerModule(spotlightFake);

const T = 100_000;
function nightDriver(module: GameModule['deck'], over: Parameters<typeof mkNightRoom>[2] = {}): Driver {
  const d = new Driver(mkNightRoom(mkSpec(module as never), mkPlayers(4), over));
  d.pending.set('intro:1', T); // the intro alarm the core would have armed
  return d;
}

// ===== BEGIN validation (D-111) =====
describe('BEGIN validation', () => {
  it('happy path: LOBBY -> arc built -> CIRCLE_INTRO with descend sting + intro timer', () => {
    const d = lobbyDriver(4, 5);
    const fx = d.dispatch({ t: 'BEGIN', id: 'P0', at: 1000 });
    expect(d.state.phase).toEqual({ k: 'CIRCLE_INTRO', circle: 0 });
    expect(d.state.arc).toHaveLength(5);
    expect(d.state.epoch).toBe(1);
    expect(fx).toContainEqual({ k: 'AUDIO', sting: 'descend' });
    expect(fx).toContainEqual({
      k: 'SCHEDULE',
      timerId: 'intro:1',
      atMs: 1000 + INTRO_TRANSITION_MS + EXPLAINER_FIRST_MS,
    });
  });

  it('derives Stage mode from the live roster when BEGIN is pressed', () => {
    const grew = lobbyDriver(5, 5);
    expect(grew.state.config?.stageMode).toBe(false); // deliberately stale fixture
    grew.dispatch({ t: 'BEGIN', id: 'P0', at: 1000 });
    expect(grew.state.config?.stageMode).toBe(true);

    const shrank = lobbyDriver(4, 5);
    shrank.state = { ...shrank.state, config: { ...shrank.state.config!, stageMode: true } };
    shrank.dispatch({ t: 'BEGIN', id: 'P0', at: 1000 });
    expect(shrank.state.config?.stageMode).toBe(false);
  });

  it('rejects: non-host, <3 actives, unset ceiling, missing attestation, no config, unentitled', () => {
    const base = lobbyDriver(4, 5);
    const before = base.state;
    base.dispatch({ t: 'BEGIN', id: 'P2', at: 1000 }); // not host
    expect(base.state).toBe(before);

    const two = lobbyDriver(2, 5);
    two.dispatch({ t: 'BEGIN', id: 'P0', at: 1000 });
    expect(two.state.phase.k).toBe('LOBBY');

    const noCeil = lobbyDriver(4, 5);
    noCeil.state = {
      ...noCeil.state,
      players: noCeil.state.players.map((p) => (p.id === 'P3' ? { ...p, ceilingSet: false } : p)),
    };
    noCeil.dispatch({ t: 'BEGIN', id: 'P0', at: 1000 });
    expect(noCeil.state.phase.k).toBe('LOBBY');

    const noAttest = lobbyDriver(4, 5);
    noAttest.state = {
      ...noAttest.state,
      players: noAttest.state.players.map((p) => (p.id === 'P1' ? { ...p, attested18: false } : p)),
    };
    noAttest.dispatch({ t: 'BEGIN', id: 'P0', at: 1000 });
    expect(noAttest.state.phase.k).toBe('LOBBY');

    const noConfig = new Driver(lobbyDriver(4, 5).state);
    noConfig.state = { ...noConfig.state, config: null };
    noConfig.dispatch({ t: 'BEGIN', id: 'P0', at: 1000 });
    expect(noConfig.state.phase.k).toBe('LOBBY');

    const broke = lobbyDriver(4, 5);
    broke.state = { ...broke.state, entitled: false };
    broke.dispatch({ t: 'BEGIN', id: 'P0', at: 1000 });
    expect(broke.state.phase.k).toBe('LOBBY');
  });

  it('CONFIG is host-only and lobby-only; CEILING locks at BEGIN for actives', () => {
    const d = lobbyDriver(4, 5);
    const cfg = d.state.config;
    d.dispatch({ t: 'CONFIG', id: 'P2', cfg: { ...cfg!, depth: 9 } });
    expect(d.state.config?.depth).toBe(5);
    d.dispatch({ t: 'BEGIN', id: 'P0', at: 1000 });
    d.dispatch({ t: 'CONFIG', id: 'P0', cfg: { ...cfg!, depth: 9 } });
    expect(d.state.config?.depth).toBe(5);
    d.dispatch({ t: 'CEILING', id: 'P1', v: 1 });
    expect(d.state.players[1]?.heatCeiling).toBe(5); // locked mid-night
  });

  it('imps may still pick a ceiling mid-night (pre-conversion)', () => {
    const d = lobbyDriver(4, 5);
    d.dispatch({ t: 'BEGIN', id: 'P0', at: 1000 });
    d.dispatch({ t: 'JOIN', id: 'I0', name: 'IMP', avatar: 0, at: 2000 });
    d.dispatch({ t: 'CEILING', id: 'I0', v: 2 });
    expect(d.state.players.find((p) => p.id === 'I0')?.heatCeiling).toBe(2);
  });
});

// ===== the full night (stub roast + module-less skips) =====
describe('full night happy path', () => {
  it('reaches JUDGMENT with epoch bumping on every phase change', () => {
    const d = lobbyDriver(4, 5);
    d.dispatch({ t: 'BEGIN', id: 'P0', at: 1000 });
    const phasesSeen: string[] = [d.state.phase.k];
    let steps = 0;
    while (d.state.phase.k !== 'JUDGMENT') {
      if (++steps > 500) throw new Error('night deadlocked');
      const prevPhase = d.state.phase;
      const prevEpoch = d.state.epoch;
      d.fireNext();
      expect(d.state.epoch).toBeGreaterThanOrEqual(prevEpoch);
      if (d.state.phase !== prevPhase) expect(d.state.epoch).toBeGreaterThan(prevEpoch);
      if (d.state.phase.k !== phasesSeen[phasesSeen.length - 1]) phasesSeen.push(d.state.phase.k);
    }
    expect(phasesSeen.filter((k) => k === 'CIRCLE_INTRO').length).toBeGreaterThanOrEqual(4); // 5 circles, dedup collapses repeats
    expect(phasesSeen.filter((k) => k === 'LADDER').length).toBeGreaterThanOrEqual(4);
    expect(d.state.judgment).not.toBeNull();
    expect(d.state.judgment?.winners.length).toBeGreaterThan(0);
  });

  it('Devil’s Bargain: holder assigned to last place at the bargain circle intro, revealed at Judgment', () => {
    const d = lobbyDriver(4, 5);
    d.dispatch({ t: 'BEGIN', id: 'P0', at: 1000 });
    const bargainIdx = d.state.arc.findIndex((c) => c.bargain);
    expect(bargainIdx).toBeGreaterThanOrEqual(0);
    d.runUntil((s) => s.phase.k === 'CIRCLE_INTRO' && s.circleIdx === bargainIdx);
    expect(d.state.devilsBargain).not.toBeNull();
    expect(d.state.devilsBargain?.circle).toBe(bargainIdx);
    const holder = d.state.devilsBargain?.holder as PlayerId;
    const minScore = Math.min(...d.state.players.filter((p) => p.role !== 'imp').map((p) => p.score));
    expect(d.state.players.find((p) => p.id === holder)?.score).toBe(minScore);
    d.runUntil((s) => s.phase.k === 'JUDGMENT');
    expect(d.state.judgment?.bargain).toEqual(d.state.devilsBargain);
  });

  it('latecomer joins as imp mid-night and converts at the next circle intro', () => {
    const d = lobbyDriver(4, 5);
    d.dispatch({ t: 'BEGIN', id: 'P0', at: 1000 });
    d.runUntil((s) => s.phase.k === 'LADDER');
    d.dispatch({ t: 'JOIN', id: 'Z0', name: 'ZOE', avatar: 7, at: 60_000 });
    expect(d.state.players.find((p) => p.id === 'Z0')?.role).toBe('imp');
    d.runUntil((s) => s.phase.k === 'CIRCLE_INTRO' && s.circleIdx >= 1);
    expect(d.state.players.find((p) => p.id === 'Z0')?.role).toBe('player');
  });

  it('a 13th body in the lobby becomes an imp (12-cap)', () => {
    const d = lobbyDriver(12, 5);
    d.dispatch({ t: 'JOIN', id: 'X', name: 'X', avatar: 0, at: 99 });
    expect(d.state.players.find((p) => p.id === 'X')?.role).toBe('imp');
  });

  it('descend-again: host DESCEND at JUDGMENT resets the night, keeps the crew', () => {
    const d = lobbyDriver(4, 5);
    d.dispatch({ t: 'BEGIN', id: 'P0', at: 1000 });
    d.runUntil((s) => s.phase.k === 'JUDGMENT');
    const epochAtJudgment = d.state.epoch;
    d.dispatch({ t: 'DESCEND', id: 'P2', at: 999_999 }); // not the host
    expect(d.state.phase.k).toBe('JUDGMENT');
    d.dispatch({ t: 'DESCEND', id: 'P0', at: 999_999 });
    expect(d.state.phase.k).toBe('LOBBY');
    expect(d.state.epoch).toBe(epochAtJudgment + 1);
    expect(d.state.arc).toEqual([]);
    expect(d.state.judgment).toBeNull();
    expect(d.state.players.every((p) => p.score === 0 && p.brimstones === 2 && !p.freshMeat)).toBe(true);
    expect(d.state.config).not.toBeNull(); // lobby stays configured; BEGIN re-validates
  });
});

// ===== seats: LEAVE / RECONNECT / lapse / host failover =====
describe('seat handling (90s hold)', () => {
  it('LEAVE arms the seat-hold; RECONNECT cancels it', () => {
    const d = lobbyDriver(4, 5);
    const fx = d.dispatch({ t: 'LEAVE', id: 'P2', at: 5000 });
    expect(fx).toContainEqual({ k: 'SCHEDULE', timerId: 'seathold:P2', atMs: 5000 + SEAT_HOLD_MS });
    expect(d.state.players[2]?.connected).toBe(false);
    const fx2 = d.dispatch({ t: 'RECONNECT', id: 'P2', at: 9000 });
    expect(fx2).toContainEqual({ k: 'CANCEL', timerId: 'seathold:P2' });
    expect(d.state.players[2]?.connected).toBe(true);
  });

  it('a reconnect beats a stale lapse timer', () => {
    const d = lobbyDriver(4, 5);
    d.dispatch({ t: 'LEAVE', id: 'P2', at: 5000 });
    d.dispatch({ t: 'RECONNECT', id: 'P2', at: 9000 });
    d.dispatch({ t: 'TIMER', timerId: 'seathold:P2', at: 5000 + SEAT_HOLD_MS }); // DO fired anyway
    expect(d.state.players.find((p) => p.id === 'P2')).toBeDefined();
  });

  it('lapse in LOBBY frees the seat entirely', () => {
    const d = lobbyDriver(4, 5);
    d.dispatch({ t: 'LEAVE', id: 'P2', at: 5000 });
    d.dispatch({ t: 'TIMER', timerId: 'seathold:P2', at: 5000 + SEAT_HOLD_MS });
    expect(d.state.players.find((p) => p.id === 'P2')).toBeUndefined();
  });

  it('host lapse in LOBBY promotes the next seat', () => {
    const d = lobbyDriver(4, 5);
    d.dispatch({ t: 'LEAVE', id: 'P0', at: 5000 });
    d.dispatch({ t: 'TIMER', timerId: 'seathold:P0', at: 5000 + SEAT_HOLD_MS });
    expect(d.state.players.find((p) => p.id === 'P0')).toBeUndefined();
    expect(d.state.players.find((p) => p.id === 'P1')?.role).toBe('host');
  });

  it('mid-night lapse keeps the seat (name, score, roastability) but fails host over', () => {
    const d = lobbyDriver(4, 5);
    d.dispatch({ t: 'BEGIN', id: 'P0', at: 1000 });
    d.dispatch({ t: 'LEAVE', id: 'P0', at: 2000 });
    d.dispatch({ t: 'TIMER', timerId: 'seathold:P0', at: 2000 + SEAT_HOLD_MS });
    const p0 = d.state.players.find((p) => p.id === 'P0');
    expect(p0).toBeDefined();
    expect(p0?.role).toBe('player');
    expect(d.state.players.find((p) => p.id === 'P1')?.role).toBe('host');
  });
});

// ===== core-owned REVEAL hold machinery (D-116) =====
describe('reveal hold (core-owned via $phase REVEAL)', () => {
  function atReveal(): Driver {
    const d = nightDriver('texttrap');
    d.fireNext(); // intro:1 -> DEAL -> module start -> REVEAL
    expect(d.state.phase).toEqual({ k: 'REVEAL', circle: 0, holdSince: T });
    return d;
  }

  it('entering REVEAL arms softcap + decay and bumps the epoch', () => {
    const d = atReveal();
    expect(d.pending.get(`reveal:softcap:${d.state.epoch}`)).toBe(T + REVEAL_SOFTCAP_MS);
    expect(d.pending.get(`reveal:decay:${d.state.epoch}`)).toBe(T + REVEAL_MIN_HOLD_MS + 8000);
    expect(d.state.epoch).toBe(3); // intro(1) -> DEAL(2) -> REVEAL(3)
  });

  it('host DESCENDs anytime; the module advances to its next loop', () => {
    const d = atReveal();
    const fx = d.dispatch({ t: 'DESCEND', id: 'P0', at: T + 1000 });
    expect(fx).toContainEqual({ k: 'CANCEL', timerId: 'reveal:softcap:3' });
    expect(d.state.phase).toEqual({ k: 'REVEAL', circle: 0, holdSince: T + 1000 }); // loop 2
  });

  it('non-host DESCEND is rejected before the 45s softcap, honored after', () => {
    const d = atReveal();
    d.dispatch({ t: 'DESCEND', id: 'P2', at: T + 30_000 });
    expect(d.state.phase).toEqual({ k: 'REVEAL', circle: 0, holdSince: T });
    d.dispatch({ t: 'DESCEND', id: 'P2', at: T + REVEAL_SOFTCAP_MS });
    expect((d.state.phase as { holdSince: number }).holdSince).toBe(T + REVEAL_SOFTCAP_MS);
  });

  it('softcap timer is a broadcast, not a phase change', () => {
    const d = atReveal();
    const epoch = d.state.epoch;
    const fx = d.dispatch({ t: 'TIMER', timerId: `reveal:softcap:${epoch}`, at: T + REVEAL_SOFTCAP_MS });
    expect(fx).toEqual([{ k: 'BROADCAST' }]);
    expect(d.state.epoch).toBe(epoch);
  });

  it('quiet room: decay timer ends the hold at min-hold + 8s', () => {
    const d = atReveal();
    d.dispatch({ t: 'TIMER', timerId: 'reveal:decay:3', at: T + REVEAL_MIN_HOLD_MS + 8000 });
    expect((d.state.phase as { holdSince: number }).holdSince).toBe(T + REVEAL_MIN_HOLD_MS + 8000); // loop 2
  });

  it('fires keep the hold alive: decay reschedules to lastFire + 8s', () => {
    const d = atReveal();
    d.dispatch({ t: 'FIRE', id: 'P2', n: 4, at: T + 25_000 });
    expect(d.state.lastFireAt).toBe(T + 25_000);
    const fx = d.dispatch({ t: 'TIMER', timerId: 'reveal:decay:3', at: T + 28_000 });
    expect(d.state.phase).toEqual({ k: 'REVEAL', circle: 0, holdSince: T }); // still holding
    expect(fx).toEqual([{ k: 'SCHEDULE', timerId: 'reveal:decay:3', atMs: T + 25_000 + 8000 }]);
    d.dispatch({ t: 'TIMER', timerId: 'reveal:decay:3', at: T + 33_000 });
    expect((d.state.phase as { holdSince: number }).holdSince).toBe(T + 33_000); // advanced
  });

  it('FIRE tallies nightStats + telemetry (whitelisted counts only)', () => {
    const d = atReveal();
    d.dispatch({ t: 'FIRE', id: 'P2', n: 4, at: T + 21_000 });
    d.dispatch({ t: 'FIRE', id: 'P2', n: 3, at: T + 22_000 });
    expect(d.state.nightStats['P2']?.fires).toBe(7);
    expect(d.state.telemetry.filter((e) => e.t === 'fires')).toHaveLength(2);
  });

  it('after the final loop: LADDER (5s) then JUDGMENT', () => {
    const d = atReveal();
    d.dispatch({ t: 'DESCEND', id: 'P0', at: T + 1000 }); // -> loop 2
    d.dispatch({ t: 'DESCEND', id: 'P0', at: T + 2000 }); // -> done
    expect(d.state.phase).toEqual({ k: 'LADDER', circle: 0 });
    expect(d.pending.get(`ladder:${d.state.epoch}`)).toBe(T + 2000 + LADDER_MS);
    d.fireNext();
    expect(d.state.phase.k).toBe('JUDGMENT');
  });

  it('module-held reveals (roast pattern): DESCEND fires the :hold: timer early, host only pre-softcap', () => {
    const d = nightDriver('texttrap');
    // simulate a module-managed hold instead of the core one
    d.state = {
      ...d.state,
      phase: { k: 'INPUT', circle: 0, sub: 'reveal', deadline: null },
      arc: [mkSpec('roast')],
      gameState: null,
      moduleTimers: { 'roast:hold:0:0': { atMs: T + 20_000, setAt: T } },
    };
    const before = d.state;
    d.dispatch({ t: 'DESCEND', id: 'P2', at: T + 5000 }); // non-host, pre-softcap
    expect(d.state).toBe(before);
    const fx = d.dispatch({ t: 'DESCEND', id: 'P0', at: T + 5000 }); // host: fire it early
    expect(fx).toContainEqual({ k: 'CANCEL', timerId: 'roast:hold:0:0' });
  });
});

// ===== deal ceremony integration (D-115) =====
describe('deal ceremony (core-run via $deal)', () => {
  function runCeremony(burn: boolean): { d: Driver; effects: unknown[]; publicTimeline: string } {
    const d = nightDriver('reality');
    const collected: Array<{ k: string }> = [];
    collected.push(...d.dispatch({ t: 'TIMER', timerId: 'intro:1', at: T }));
    if (burn) collected.push(...d.dispatch({ t: 'BURN', id: 'P1', kind: 'card', at: T + 5000 }));
    collected.push(...d.dispatch({ t: 'TIMER', timerId: 'deal:2', at: T + PREVIEW_MS }));
    return {
      d,
      effects: collected,
      publicTimeline: JSON.stringify(collected.filter((effect) => effect.k !== 'SEND')),
    };
  }

  it('subject pre-view goes ONLY to the subject, ceremony completes into the module', () => {
    const { d } = runCeremony(false);
    const sends = d.log.flatMap((l) => l.effects).filter((e) => e.k === 'SEND');
    expect(sends).toEqual([
      {
        k: 'SEND',
        to: 'P1',
        kind: 'preview',
        payload: {
          status: 'assigned',
          previewId: 'deal:2',
          card: PRIMARY,
          burnDeadline: T + PREVIEW_MS,
          revealAt: T + PREVIEW_MS,
          canBurn: true,
        },
      },
    ]);
    expect(d.state.deal?.done).toBe(true);
    expect(d.state.deal?.card.id).toBe(PRIMARY.id);
    expect(d.state.usedCardIds).toEqual([PRIMARY.id]);
    expect(d.state.phase).toEqual({ k: 'INPUT', circle: 0, sub: 'debate', deadline: T + PREVIEW_MS + 25_000 });
  });

  it('BURNED vs CLEAN ceremonies have identical public effects and telemetry (D-115)', () => {
    const clean = runCeremony(false);
    const burned = runCeremony(true);
    expect(burned.publicTimeline).toBe(clean.publicTimeline);
    expect(burned.d.state.telemetry).toEqual(clean.d.state.telemetry);
    expect(burned.effects).toContainEqual({
      k: 'SEND',
      to: 'P1',
      kind: 'preview',
      payload: { status: 'released', previewId: 'deal:2' },
    });
    expect(burned.d.state.deal?.card.id).toBe(BACKUP.id); // but the card silently swapped
    expect(burned.d.state.usedCardIds).toEqual([BACKUP.id, PRIMARY.id]); // veto quarantined
    expect(burned.d.state.players[1]?.brimstones).toBe(1); // own count decremented (invisible to others)
  });

  it('burns from non-subjects and brimstone-less subjects are inert', () => {
    const d = nightDriver('reality');
    d.dispatch({ t: 'TIMER', timerId: 'intro:1', at: T });
    expect(d.dispatch({ t: 'BURN', id: 'P2', kind: 'card', at: T + 3000 })).toEqual([]);
    expect(d.state.deal?.card.id).toBe(PRIMARY.id);

    const broke = nightDriver('reality');
    broke.state = {
      ...broke.state,
      players: broke.state.players.map((p) => (p.id === 'P1' ? { ...p, brimstones: 0 } : p)),
    };
    broke.dispatch({ t: 'TIMER', timerId: 'intro:1', at: T });
    expect(broke.dispatch({ t: 'BURN', id: 'P1', kind: 'card', at: T + 3000 })).toEqual([]);
    expect(broke.state.deal?.card.id).toBe(PRIMARY.id);
  });

  it('expired and duplicate burns never receive a release acknowledgement', () => {
    const late = nightDriver('reality');
    late.dispatch({ t: 'TIMER', timerId: 'intro:1', at: T });
    expect(
      late.dispatch({ t: 'BURN', id: 'P1', kind: 'card', at: T + PREVIEW_MS + 1 }),
    ).toEqual([]);

    const duplicate = nightDriver('reality');
    duplicate.dispatch({ t: 'TIMER', timerId: 'intro:1', at: T });
    expect(duplicate.dispatch({ t: 'BURN', id: 'P1', kind: 'card', at: T + 1000 })).toHaveLength(1);
    expect(duplicate.dispatch({ t: 'BURN', id: 'P1', kind: 'card', at: T + 2000 })).toEqual([]);
  });

  it('no code path fabricates content: an unburned backup returns to the pool untraced', () => {
    const { d } = runCeremony(false);
    expect(d.state.usedCardIds).not.toContain(BACKUP.id);
    expect(JSON.stringify(d.log.flatMap((l) => l.effects))).not.toContain(BACKUP.id);
  });
});

// ===== private spotlight ceremony integration (D-134) =====
describe('spotlight ceremony (core-run via $spotlight)', () => {
  function atSpotlight(loops = 1): Driver {
    const d = nightDriver('hotseat', {
      arc: [mkSpec('hotseat', { loops })],
    });
    d.fireNext(); // intro -> DEAL -> module start -> private assignment
    expect(d.state.phase).toEqual({ k: 'DEAL', circle: 0 });
    expect(d.state.spotlight?.window).toBe('primary');
    return d;
  }

  function publicTimeline(d: Driver): string {
    return JSON.stringify(
      d.log.flatMap(({ effects }) => effects).filter((effect) => effect.k !== 'SEND'),
    );
  }

  it('replaces a burned primary at fixed T+10 and commits only the final id at fixed T+20', () => {
    const d = atSpotlight();
    const begun = d.state.spotlight!;
    const primary = begun.slots[0]!.playerId!;
    expect(d.pending.get(begun.handoffTimerId)).toBe(T + SPOTLIGHT_WINDOW_MS);
    expect(d.pending.get(begun.completionTimerId)).toBe(T + SPOTLIGHT_TOTAL_MS);
    expect(d.state.players.every((player) => player.spotlightCount === 0)).toBe(true);

    const burnFx = d.dispatch({ t: 'BURN', id: primary, kind: 'spotlight', at: T + 5_000 });
    expect(burnFx).toEqual([
      {
        k: 'SEND',
        to: primary,
        kind: 'spotlight',
        payload: { status: 'released', ceremonyId: begun.ceremonyId },
      },
    ]);
    expect(d.state.players.find((player) => player.id === primary)?.brimstones).toBe(1);
    expect(d.state.players.every((player) => player.spotlightCount === 0)).toBe(true);

    expect(d.fireNext()).toBe(begun.handoffTimerId);
    const replacement = d.state.spotlight!.slots[0]!.playerId!;
    expect(replacement).not.toBe(primary);
    expect(d.log.at(-1)?.effects).toEqual([
      expect.objectContaining({
        k: 'SEND',
        to: replacement,
        kind: 'spotlight',
        payload: expect.objectContaining({
          status: 'assigned',
          burnDeadline: T + SPOTLIGHT_TOTAL_MS,
          announceAt: T + SPOTLIGHT_TOTAL_MS,
        }),
      }),
    ]);
    expect(d.state.players.every((player) => player.spotlightCount === 0)).toBe(true);

    expect(d.fireNext()).toBe(begun.completionTimerId);
    expect(d.state.spotlight).toBeNull();
    expect(d.state.phase).toEqual({
      k: 'INPUT',
      circle: 0,
      sub: 'resolved',
      deadline: T + SPOTLIGHT_TOTAL_MS + 25_000,
    });
    const gs = d.state.gameState as SpotlightFakeState;
    expect(gs.resolutions).toEqual([
      { assignments: [{ role: 'subject', playerId: replacement }] },
    ]);
    expect(d.state.players.find((player) => player.id === replacement)?.spotlightCount).toBe(1);
    expect(d.state.players.find((player) => player.id === primary)?.spotlightCount).toBe(0);
  });

  it('keeps clean and burned public effect timelines byte-identical', () => {
    function run(burn: boolean): Driver {
      const d = atSpotlight();
      if (burn) {
        const primary = d.state.spotlight!.slots[0]!.playerId!;
        d.dispatch({ t: 'BURN', id: primary, kind: 'spotlight', at: T + 1_000 });
      }
      d.fireNext();
      d.fireNext();
      return d;
    }

    const clean = run(false);
    const burned = run(true);
    expect(publicTimeline(burned)).toBe(publicTimeline(clean));
    expect((burned.state.gameState as SpotlightFakeState).resolutions)
      .not.toEqual((clean.state.gameState as SpotlightFakeState).resolutions);
  });

  it('strictly isolates card burns from spotlight burns', () => {
    const spotlight = atSpotlight();
    const beforeSpotlight = spotlight.state;
    expect(
      spotlight.dispatch({
        t: 'BURN',
        id: spotlight.state.spotlight!.slots[0]!.playerId!,
        kind: 'card',
        at: T + 1_000,
      }),
    ).toEqual([]);
    expect(spotlight.state).toBe(beforeSpotlight);

    const deal = nightDriver('reality');
    deal.fireNext();
    const beforeDeal = deal.state;
    expect(deal.dispatch({ t: 'BURN', id: 'P1', kind: 'spotlight', at: T + 1_000 })).toEqual([]);
    expect(deal.state).toBe(beforeDeal);
  });

  it('commits an explicit null when a replacement also burns, with no fairness bump', () => {
    const d = atSpotlight();
    const primary = d.state.spotlight!.slots[0]!.playerId!;
    d.dispatch({ t: 'BURN', id: primary, kind: 'spotlight', at: T + 1_000 });
    d.fireNext();
    const replacement = d.state.spotlight!.slots[0]!.playerId!;
    const replacementFx = d.dispatch({
      t: 'BURN',
      id: replacement,
      kind: 'spotlight',
      at: T + SPOTLIGHT_WINDOW_MS + 1_000,
    });
    expect(replacementFx).toEqual([
      expect.objectContaining({
        k: 'SEND',
        to: replacement,
        payload: expect.objectContaining({ status: 'released' }),
      }),
    ]);
    d.fireNext();

    expect((d.state.gameState as SpotlightFakeState).resolutions).toEqual([
      { assignments: [{ role: 'subject', playerId: null }] },
    ]);
    expect(d.state.players.every((player) => player.spotlightCount === 0)).toBe(true);
    expect(d.state.players.find((player) => player.id === primary)?.brimstones).toBe(1);
    expect(d.state.players.find((player) => player.id === replacement)?.brimstones).toBe(1);
  });

  it('preserves a new ceremony requested synchronously by the completion callback', () => {
    const d = atSpotlight(2);
    const first = d.state.spotlight!;
    const epoch = d.state.epoch;
    d.fireNext(); // first handoff
    d.fireNext(); // first complete -> module requests loop two

    const second = d.state.spotlight!;
    expect(second.window).toBe('primary');
    expect(second.ceremonyId).not.toBe(first.ceremonyId);
    expect(d.state.epoch).toBe(epoch); // DEAL never changed, but ceremony ids remain unique
    expect((d.state.gameState as SpotlightFakeState).resolutions).toHaveLength(1);
    expect(d.pending.get(second.handoffTimerId)).toBe(T + SPOTLIGHT_TOTAL_MS + SPOTLIGHT_WINDOW_MS);
    expect(d.pending.get(second.completionTimerId)).toBe(T + 2 * SPOTLIGHT_TOTAL_MS);

    d.fireNext();
    d.fireNext();
    const gs = d.state.gameState as SpotlightFakeState;
    expect(d.state.spotlight).toBeNull();
    expect(gs.resolutions).toHaveLength(2);
    expect(d.state.players.reduce((sum, player) => sum + player.spotlightCount, 0)).toBe(2);
  });
});

// ===== blocking terminals through the core (D-115 / 4.7) =====
describe('blocking machine integration', () => {
  function atBlocking(): Driver {
    const d = nightDriver('taboo');
    d.fireNext(); // intro -> DEAL -> start -> blocking INPUT
    expect(d.state.phase).toEqual({ k: 'INPUT', circle: 0, sub: 'truth', deadline: null }); // timer PAUSED
    return d;
  }

  it('12s grace -> WAITING_ON phase; 30s -> pit vote opens', () => {
    const d = atBlocking();
    d.dispatch({ t: 'TIMER', timerId: 'blk:grace:2', at: T + 12_000 });
    expect(d.state.phase).toEqual({ k: 'WAITING_ON', circle: 0, who: 'P1', since: T + 12_000 });
    d.dispatch({ t: 'TIMER', timerId: 'blk:pit:2', at: T + 30_000 });
    expect((d.state.gameState as { blk: BlockingState }).blk.pitOpen).toBe(true);
  });

  it('>=60% FEED THEM TO THE PIT voids the loop with the void sting', () => {
    const d = atBlocking();
    d.dispatch({ t: 'TIMER', timerId: 'blk:grace:2', at: T + 12_000 });
    d.dispatch({ t: 'TIMER', timerId: 'blk:pit:2', at: T + 30_000 });
    d.dispatch({ t: 'INPUT', id: 'P2', payload: { pit: 'pit' }, at: T + 31_000 });
    expect(d.state.phase.k).toBe('WAITING_ON'); // 1/3 is not 60%
    const fx = d.dispatch({ t: 'INPUT', id: 'P3', payload: { pit: 'pit' }, at: T + 32_000 });
    expect(fx).toContainEqual({ k: 'AUDIO', sting: 'void' });
    expect(d.state.phase.k).toBe('LADDER');
  });

  it('PLEAD THE FIFTH: owner resolves scoreless, sting fires, Judgment remembers', () => {
    const d = atBlocking();
    const fx = d.dispatch({ t: 'PLEAD_FIFTH', id: 'P1', at: T + 15_000 });
    expect(fx).toContainEqual({ k: 'AUDIO', sting: 'fifth' });
    expect(d.state.phase.k).toBe('LADDER');
    expect(d.state.nightStats['P1']?.fifths).toBe(1);
    expect(d.state.players[1]?.score).toBe(0);
  });

  it('host VOID ROUND works; non-host VOID is rejected by the core', () => {
    const d = atBlocking();
    d.dispatch({ t: 'VOID_ROUND', id: 'P2', at: T + 15_000 });
    expect(d.state.phase.k).toBe('INPUT'); // core guard: NOT_HOST
    const fx = d.dispatch({ t: 'VOID_ROUND', id: 'P0', at: T + 16_000 });
    expect(fx).toContainEqual({ k: 'AUDIO', sting: 'void' });
    expect(d.state.phase.k).toBe('LADDER');
  });

  it('seat lapse (90s gone) auto-voids the blocking input: THE WITNESS FLED', () => {
    const d = atBlocking();
    d.dispatch({ t: 'LEAVE', id: 'P1', at: T + 20_000 });
    const fx = d.dispatch({ t: 'TIMER', timerId: 'seathold:P1', at: T + 20_000 + SEAT_HOLD_MS });
    expect(fx).toContainEqual({ k: 'AUDIO', sting: 'fled' });
    expect(d.state.phase.k).toBe('LADDER');
  });

  it('a REAL answer resolves and scores — the only path that ever produces a truth value', () => {
    const d = atBlocking();
    const fx = d.dispatch({ t: 'INPUT', id: 'P1', payload: { answer: 42 }, at: T + 18_000 });
    expect(d.state.phase.k).toBe('LADDER');
    expect(d.state.players[1]?.score).toBe(2);
    expect(fx.filter((e) => e.k === 'AUDIO')).toEqual([]); // no shame stings on honesty
  });
});

// ===== judgment (winner + superlatives from real telemetry) =====
describe('computeJudgment', () => {
  it('crowns ties together, feeds superlatives from night stats, guarantees residual imps theirs', () => {
    const s: RoomState = {
      ...mkNightRoom(mkSpec('roast'), [
        mkPlayer('P0', 'host', 0, { score: 9, brimstones: 2 }),
        mkPlayer('P1', 'player', 1, { score: 9, brimstones: 1, spotlightCount: 4 }),
        mkPlayer('P2', 'player', 2, { score: 3, brimstones: 0 }),
        mkPlayer('IMP', 'imp', 3),
      ]),
      nightStats: { P2: { fires: 40, fifths: 1 }, P0: { fires: 12, fifths: 0 } },
      devilsBargain: { holder: 'P2', circle: 3 },
    };
    const j = computeJudgment(s);
    expect(j.winners).toEqual(['P0', 'P1']); // tie shares the crown
    expect(j.superlatives).toContainEqual({ title: 'THE ARSONIST', playerId: 'P2' });
    expect(j.superlatives).toContainEqual({ title: 'MOST WANTED', playerId: 'P1' });
    expect(j.superlatives).toContainEqual({ title: 'COWARDICE NOTED', playerId: 'P2' });
    expect(j.superlatives).toContainEqual({ title: 'STONE COLD', playerId: 'P0' });
    expect(j.superlatives).toContainEqual({ title: "STILL IN HELL'S WAITING ROOM", playerId: 'IMP' });
    expect(j.bargain).toEqual({ holder: 'P2', circle: 3 }); // revealed HERE, never earlier
  });
});

// ===== misc =====
describe('names', () => {
  it('uniques duplicates the SAM (2) way and never ships an empty name', () => {
    expect(uniqueName('sam', ['SAM'])).toBe('SAM (2)');
    expect(uniqueName('sam', ['SAM', 'SAM (2)'])).toBe('SAM (3)');
    expect(uniqueName('   ', [])).toBe('SINNER');
  });
});
