// Shared fixtures + a deterministic night driver for engine tests.
import type {
  Card,
  CardBase,
  CircleSpec,
  Effect,
  GameEvent,
  NightConfig,
  Player,
  Role,
  RoomState,
  Tier,
} from '../src/types.js';
import { initialRoom, reduce } from '../src/engine.js';

export const SEED = 'test-seed';

export function mkPlayer(id: string, role: Role, seat: number, over: Partial<Player> = {}): Player {
  return {
    id,
    name: id,
    avatar: seat % 16,
    role,
    seat,
    connected: true,
    lastSeenAt: 0,
    heatCeiling: 5,
    ceilingSet: true,
    attested18: true,
    brimstones: 2,
    score: 0,
    spotlightCount: 0,
    freshMeat: false,
    ...over,
  };
}

export function mkPlayers(n: number, over: Partial<Player> = {}): Player[] {
  return Array.from({ length: n }, (_, i) => mkPlayer(`P${i}`, i === 0 ? 'host' : 'player', i, over));
}

export function mkConfig(depth: 5 | 7 | 9, over: Partial<NightConfig> = {}): NightConfig {
  return { depth, vibe: 'feral', stageMode: false, crewId: 'crew-test', irlFamiliar: true, ...over };
}

export function mkSpec(game: CircleSpec['game'], over: Partial<CircleSpec> = {}): CircleSpec {
  return { game, loops: 1, finale: false, outward: false, rung: 3, bargain: false, ...over };
}

export function mkCard(id: string, deck: CardBase['deck'] = 'poison', exposure: Tier = 3): Card {
  return {
    id,
    deck,
    text: `card ${id}`,
    exposure,
    chaos: 3,
    register: 'deadpan',
    skeleton: `sk-${id}`,
  };
}

/** A room already mid-night on a hand-crafted single-circle arc, at CIRCLE_INTRO. */
export function mkNightRoom(spec: CircleSpec, players: Player[], over: Partial<RoomState> = {}): RoomState {
  return {
    ...initialRoom('HELL', 0, true),
    config: mkConfig(5),
    players,
    arc: [spec],
    circleIdx: 0,
    phase: { k: 'CIRCLE_INTRO', circle: 0 },
    epoch: 1,
    ...over,
  };
}

/**
 * Deterministic harness: dispatches events through reduce(), mirrors the DO's
 * timer bookkeeping (SCHEDULE/CANCEL), and can fire the next pending alarm.
 */
export class Driver {
  state: RoomState;
  pending = new Map<string, number>(); // timerId -> atMs
  log: { event: GameEvent; effects: Effect[] }[] = [];

  constructor(state: RoomState) {
    this.state = state;
  }

  dispatch(event: GameEvent): Effect[] {
    const { state, effects } = reduce(this.state, event, SEED);
    this.state = state;
    for (const ef of effects) {
      if (ef.k === 'SCHEDULE') this.pending.set(ef.timerId, ef.atMs);
      else if (ef.k === 'CANCEL') this.pending.delete(ef.timerId);
    }
    this.log.push({ event, effects });
    return effects;
  }

  /** Fire the earliest pending timer (the DO alarm), returning its id. */
  fireNext(): string {
    let best: [string, number] | null = null;
    for (const [id, at] of this.pending) {
      if (best === null || at < best[1]) best = [id, at];
    }
    if (!best) throw new Error('no pending timers');
    this.pending.delete(best[0]);
    this.dispatch({ t: 'TIMER', timerId: best[0], at: best[1] });
    return best[0];
  }

  /** Drive timers until a predicate holds (or blow up — deadlocks must be loud). */
  runUntil(pred: (s: RoomState) => boolean, maxSteps = 500): void {
    for (let i = 0; i < maxSteps; i++) {
      if (pred(this.state)) return;
      this.fireNext();
    }
    throw new Error(`runUntil: predicate not reached in ${maxSteps} steps (phase=${this.state.phase.k})`);
  }
}

/** Build a lobby via real events: n players joined, attested, ceilinged, configured. */
export function lobbyDriver(n: number, depth: 5 | 7 | 9 = 5, ceilings?: Tier[]): Driver {
  const d = new Driver(initialRoom('HELL', 0, true));
  for (let i = 0; i < n; i++) {
    d.dispatch({ t: 'JOIN', id: `P${i}`, name: `P${i}`, avatar: i % 16, at: i });
    d.dispatch({ t: 'ATTEST18', id: `P${i}` });
    d.dispatch({ t: 'CEILING', id: `P${i}`, v: ceilings?.[i] ?? 5 });
  }
  d.dispatch({ t: 'CONFIG', id: 'P0', cfg: mkConfig(depth) });
  return d;
}
