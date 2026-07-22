// D-137 — adversarial bot-fuzz over the whole night machine. A seeded generator hoses
// reduce() with malformed payloads, foreign/unknown player ids, wrong-phase controls,
// double-submits, membership churn, and hostile event ordering — then asserts the four
// properties a server-authoritative engine must never lose:
//   1. TOTALITY   — reduce() never throws, on any garbage a client could send.
//   2. INVARIANTS — scores stay finite, spotlightCount non-negative, epoch monotonic,
//                   phase always a known kind, players well-formed.
//   3. REDACTION  — no viewer's view() ever carries a NEVER_SERIALIZE secret (the wire
//                   surface, checked at the engine's only serialization point: view()).
//   4. DETERMINISM— the exact event stream replays byte-identical (engine purity).
// A second block proves LIVENESS-UNDER-NOISE: a night still being driven always reaches
// JUDGMENT despite the noise — noise can annoy the machine, never deadlock it.
//
// The fuzz clock is monotonic on purpose: a malicious phone can forge payloads/ids/verbs
// and their arrival order, but NOT the DO's alarm clock — that's the real threat model.
// Failures print the seed + offending event so any red is reproducible from the seed alone.
import { describe, expect, it } from 'vitest';
import type { GameCtx } from '../src/games/module.js';
import type { GameEvent, RoomState, Tier } from '../src/types.js';
import { getModule, initialRoom, reduce } from '../src/engine.js';
import { rng } from '../src/rng.js';

// Mirror of server/redact.ts NEVER_SERIALIZE — the keys that must never survive view().
// (heatCeiling/crewId/etc. live on Player/config, not gameView, so they simply never
// appear; the module-state internals below are the ones view() could actually leak.)
const NEVER_SERIALIZE = [
  'heatCeiling', 'ceilingSet', 'devilsBargain', 'usedCardIds', 'telemetry', 'crewId',
  'burnedId', 'moduleTimers', 'nightStats', 'votes', 'shownEdges', 'participation',
];
const CIRCLE_PHASES = new Set(['DEAL', 'INPUT', 'WAITING_ON', 'REVEAL']);
const PHASE_KINDS = new Set([
  'LOBBY', 'CONSENT', 'CIRCLE_INTRO', 'DEAL', 'INPUT', 'WAITING_ON', 'REVEAL', 'LADDER', 'JUDGMENT',
]);

/** Compose a module's per-viewer view exactly as server/redact.ts composeGameView does. */
function viewFor(s: RoomState, viewerId: string): unknown {
  const spec = s.arc[s.circleIdx];
  if (!spec || !CIRCLE_PHASES.has(s.phase.k)) return null;
  const m = getModule(spec.game);
  if (!m) return null;
  const ctx: GameCtx = {
    state: s,
    circle: spec,
    circleIdx: s.circleIdx,
    players: s.players.filter((p) => p.role !== 'imp'),
    imps: s.players.filter((p) => p.role === 'imp'),
    now: 0,
    rand: rng(`${s.code}:view:${s.epoch}:${viewerId}`),
    finaleMult: spec.finale ? 3 : 1,
    volunteers: [],
  };
  return m.view(ctx, viewerId) ?? null;
}

/** A small deterministic PRNG stream, seeded — every knob the fuzzer turns comes from here. */
function chaos(seed: string) {
  const r = rng(seed);
  const pick = <T,>(xs: readonly T[]): T => xs[Math.floor(r() * xs.length)] as T;
  return {
    r,
    pick,
    int: (lo: number, hi: number): number => lo + Math.floor(r() * (hi - lo + 1)),
    chance: (p: number): boolean => r() < p,
  };
}

type C = ReturnType<typeof chaos>;

// A grab-bag of INPUT payloads: valid-for-some-game, valid-shape-wrong-value, and pure junk.
// Every game's parser must reject what isn't its own without throwing.
const PAYLOADS: unknown[] = [
  {}, null, undefined, [], 42, 'sabotage', true,
  { vote: 'A' }, { vote: 'B' }, { vote: 'smash' }, { vote: 'pass' }, { vote: 'over' }, { vote: 'P0' },
  { vote: 999 }, { vote: null }, { bet: 'over' }, { bet: 'under' }, { bet: 'sideways' },
  { truth: 7 }, { truth: -3 }, { truth: 1e9 }, { truth: 'nope' }, { truth: NaN },
  { line: 5, lock: true }, { line: 'x', lock: true }, { pick: 0 }, { pick: 2 }, { pick: 99 },
  { tap: 'P0' }, { tap: 'GHOST' }, { tap: 42 }, { picks: ['a', 'b', 'c'] }, { picks: [] }, { picks: ['a'] },
  { rate: { A: 4, B: 2 } }, { rate: { A: 99, B: -1 } }, { rate: 'bad' },
  { answer: 'i regret nothing' }, { answer: '' }, { answer: 'x'.repeat(500) },
  { tone: 0 }, { tone: 9 }, { read: true }, { next: true }, { burn: true }, { panic: true }, { take: 'A' },
  { claim: 5 }, { pit: 'drag' }, { pit: 'pit' }, { foo: 1, bar: [2] },
];

/** Fabricate ONE adversarial event against the live state. `churn` allows membership chaos. */
function evilEvent(s: RoomState, c: C, at: number, churn: boolean): GameEvent {
  const ids = s.players.map((p) => p.id);
  const foreign = ['GHOST', 'P99', '', 'P0 '];
  const anyId = (): string => (c.chance(0.72) && ids.length ? c.pick(ids) : c.pick(foreign));
  const host = s.players.find((p) => p.role === 'host')?.id ?? 'P0';

  const menu: (() => GameEvent)[] = [
    () => ({ t: 'INPUT', id: anyId(), payload: c.pick(PAYLOADS), at }),
    () => ({ t: 'INPUT', id: anyId(), payload: c.pick(PAYLOADS), at }), // weight INPUT heavily
    () => ({ t: 'REST_CASE', id: anyId(), at }),
    () => ({ t: 'SKIP_EM', id: anyId(), at }),
    () => ({ t: 'PLEAD_FIFTH', id: anyId(), at }),
    () => ({ t: 'VOID_ROUND', id: c.chance(0.5) ? host : anyId(), at }),
    () => ({ t: 'DESCEND', id: anyId(), at }),
    () => ({ t: 'FIRE', id: anyId(), n: c.pick([1, 3, -5, 1000, 0]), at }),
    () => ({ t: 'BURN', id: anyId(), kind: c.pick(['card', 'spotlight'] as const), at }),
    () => ({ t: 'CLAIM', id: anyId(), at }), // 4.5 WHO WANTS BLOOD — spam it from anyone, any phase
  ];
  if (churn) {
    menu.push(
      () => ({ t: 'JOIN', id: c.pick(['P0', 'LATE1', 'LATE2', 'GHOST']), name: 'x', avatar: c.int(0, 15), at }),
      () => ({ t: 'LEAVE', id: anyId(), at }),
      () => ({ t: 'RECONNECT', id: anyId(), at }),
      () => ({ t: 'ATTEST18', id: anyId() }),
      () => ({ t: 'CEILING', id: anyId(), v: c.pick([1, 3, 5]) as Tier }),
      () => ({ t: 'BEGIN', id: c.chance(0.5) ? host : anyId(), at }),
      () => ({ t: 'CONFIG', id: anyId(), cfg: { depth: c.pick([5, 7, 9]) as 5 | 7 | 9, vibe: 'feral', stageMode: false, crewId: 'x', irlFamiliar: true } }),
    );
  }
  return c.pick(menu)();
}

/** Seat a lobby and BEGIN, returning the running state + the exact event log (for replay). */
function beginFuzzNight(c: C): { s: RoomState; log: GameEvent[]; pending: Map<string, number>; clock: number } {
  const n = c.int(3, 12);
  const depth = c.pick([5, 7, 9]) as 5 | 7 | 9;
  const log: GameEvent[] = [];
  let s = initialRoom('HELL', 0, true);
  const pending = new Map<string, number>();
  const dispatch = (e: GameEvent, at: number): void => {
    log.push(e);
    const r = reduce(s, e, 'HELL');
    s = r.state;
    for (const ef of r.effects) {
      if (ef.k === 'SCHEDULE') pending.set(ef.timerId, ef.atMs);
      else if (ef.k === 'CANCEL') pending.delete(ef.timerId);
    }
  };
  for (let i = 0; i < n; i++) {
    dispatch({ t: 'JOIN', id: `P${i}`, name: `P${i}`, avatar: i % 16, at: i }, i);
    dispatch({ t: 'ATTEST18', id: `P${i}` }, i);
    dispatch({ t: 'CEILING', id: `P${i}`, v: c.pick([1, 3, 5]) as Tier }, i);
  }
  dispatch({ t: 'CONFIG', id: 'P0', cfg: { depth, vibe: c.pick(['feral', 'cursed', 'mild']) as 'feral', stageMode: c.chance(0.3), crewId: 'fuzz', irlFamiliar: c.chance(0.7) } }, n);
  dispatch({ t: 'BEGIN', id: 'P0', at: 1000 }, 1000);
  return { s, log, pending, clock: 1000 };
}

/** Replay an event log from scratch and return the final state — must equal the live run. */
function replay(log: readonly GameEvent[]): RoomState {
  let s = initialRoom('HELL', 0, true);
  for (const e of log) s = reduce(s, e, 'HELL').state;
  return s;
}

/** Assert the per-step invariants (cheap, every player) + redaction (on a sampled set of
 * viewers — over hundreds of steps every viewer gets hit many times, so a leak still can't
 * hide, but we pay for ~3 view() compositions per step instead of N). */
function assertHealthy(s: RoomState, prevEpoch: number, tag: string, viewers: readonly string[]): void {
  expect(PHASE_KINDS.has(s.phase.k), `${tag}: unknown phase ${s.phase.k}`).toBe(true);
  expect(s.epoch, `${tag}: epoch went backwards`).toBeGreaterThanOrEqual(prevEpoch);
  for (const p of s.players) {
    expect(Number.isFinite(p.score), `${tag}: ${p.id} score not finite (${p.score})`).toBe(true);
    expect(Number.isInteger(p.spotlightCount) && p.spotlightCount >= 0, `${tag}: ${p.id} bad spotlightCount ${p.spotlightCount}`).toBe(true);
    expect(typeof p.id === 'string' && p.id.length > 0, `${tag}: malformed player id`).toBe(true);
  }
  // Redaction at the only serialization surface: no viewer's view() may carry a secret.
  for (const viewer of viewers) {
    let json: string;
    try {
      json = JSON.stringify(viewFor(s, viewer)) ?? '';
    } catch (err) {
      throw new Error(`${tag}: view(${viewer}) threw: ${String(err)}`);
    }
    for (const key of NEVER_SERIALIZE) {
      expect(json.includes(`"${key}"`), `${tag}: view(${viewer}) leaked secret "${key}"`).toBe(false);
    }
  }
}

/** A rotating sample of viewers to redaction-check: up to 3 real seats + the unknown GHOST. */
function sampleViewers(s: RoomState, c: C): string[] {
  const ids = s.players.map((p) => p.id);
  const out = new Set<string>(['GHOST']);
  for (let i = 0; i < 3 && ids.length; i++) out.add(c.pick(ids));
  return [...out];
}

describe('D-137 bot-fuzz: the engine survives hostile clients', () => {
  it('never throws, never corrupts state, never leaks a secret, and replays deterministically', () => {
    for (let trial = 0; trial < 60; trial++) {
      const seed = `fuzz-A-${trial}`;
      const c = chaos(seed);
      const run = beginFuzzNight(c);
      let { s } = run;
      const { log, pending } = run;
      let clock = run.clock;
      let prevEpoch = s.epoch;

      for (let step = 0; step < 400; step++) {
        if (s.phase.k === 'JUDGMENT') break;
        clock += c.int(1, 4000); // monotonic wall clock — never forge time backwards
        // 45% inject hostile noise; otherwise fire the earliest real alarm to make progress.
        const injectNoise = c.chance(0.45) || pending.size === 0;
        let ev: GameEvent | null = null;
        if (injectNoise) {
          ev = evilEvent(s, c, clock, /* churn */ true);
        } else {
          let best: [string, number] | null = null;
          for (const [id, at] of pending) if (best === null || at < best[1]) best = [id, at];
          if (best) {
            pending.delete(best[0]);
            ev = { t: 'TIMER', timerId: best[0], at: Math.max(clock, best[1]) };
          } else {
            ev = evilEvent(s, c, clock, true);
          }
        }
        log.push(ev);
        let r;
        try {
          r = reduce(s, ev, 'HELL');
        } catch (err) {
          throw new Error(`[${seed} step ${step}] reduce threw on ${JSON.stringify(ev)}: ${String(err)}`);
        }
        s = r.state;
        for (const ef of r.effects) {
          if (ef.k === 'SCHEDULE') pending.set(ef.timerId, ef.atMs);
          else if (ef.k === 'CANCEL') pending.delete(ef.timerId);
        }
        assertHealthy(s, prevEpoch, `${seed} step ${step} after ${ev.t}`, sampleViewers(s, c));
        prevEpoch = s.epoch;
      }

      // Purity: the whole hostile stream replays to a byte-identical final state.
      expect(replay(log), `${seed}: replay diverged from the live run`).toEqual(s);
    }
  }, 60_000);

  it('liveness under noise: a night still being driven always reaches JUDGMENT', () => {
    for (let trial = 0; trial < 30; trial++) {
      const seed = `fuzz-B-${trial}`;
      const c = chaos(seed);
      const run = beginFuzzNight(c);
      let { s } = run;
      const { pending } = run;
      let clock = run.clock;
      let prevEpoch = s.epoch;
      const step = (ev: GameEvent, tag: string): void => {
        let r;
        try {
          r = reduce(s, ev, 'HELL');
        } catch (err) {
          throw new Error(`[${seed}] reduce threw on ${JSON.stringify(ev)}: ${String(err)}`);
        }
        s = r.state;
        for (const ef of r.effects) {
          if (ef.k === 'SCHEDULE') pending.set(ef.timerId, ef.atMs);
          else if (ef.k === 'CANCEL') pending.delete(ef.timerId);
        }
        assertHealthy(s, prevEpoch, `${seed} ${tag}`, sampleViewers(s, c));
        prevEpoch = s.epoch;
      };
      const host = s.players.find((p) => p.role === 'host')?.id ?? 'P0';

      // Phase 1: noisy but membership-stable (no JOIN/LEAVE/CONFIG/BEGIN) so the drive stays coherent.
      for (let i = 0; i < 150 && s.phase.k !== 'JUDGMENT'; i++) {
        clock += c.int(1, 3000);
        if (c.chance(0.4)) {
          step(evilEvent(s, c, clock, /* churn */ false), `noise ${i}`);
          continue;
        }
        let best: [string, number] | null = null;
        for (const [id, at] of pending) if (best === null || at < best[1]) best = [id, at];
        if (best) {
          pending.delete(best[0]);
          step({ t: 'TIMER', timerId: best[0], at: Math.max(clock, best[1]) }, `timer ${i}`);
        } else if (s.phase.k === 'INPUT' || s.phase.k === 'WAITING_ON') {
          step({ t: 'VOID_ROUND', id: host, at: clock }, `void ${i}`); // unstick a blocking owner
        }
      }

      // Phase 2: drain — fire every alarm, host-VOID any blocking wait, until JUDGMENT.
      for (let i = 0; i < 600 && s.phase.k !== 'JUDGMENT'; i++) {
        clock += 100;
        let best: [string, number] | null = null;
        for (const [id, at] of pending) if (best === null || at < best[1]) best = [id, at];
        if (best) {
          pending.delete(best[0]);
          step({ t: 'TIMER', timerId: best[0], at: Math.max(clock, best[1]) }, `drain ${i}`);
        } else if (s.phase.k === 'INPUT' || s.phase.k === 'WAITING_ON') {
          step({ t: 'VOID_ROUND', id: host, at: clock }, `drain-void ${i}`);
        } else {
          break; // no timer, not blocking, not JUDGMENT -> genuine deadlock, caught below
        }
      }
      expect(s.phase.k, `${seed}: noise derailed the night — stuck at ${s.phase.k}`).toBe('JUDGMENT');
    }
  }, 60_000);
});
