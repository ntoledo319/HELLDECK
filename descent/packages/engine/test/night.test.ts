// Full-night integration (M2-INT) — drives complete nights through reduce() with an
// engine-side view-driven bot, proving the four registered M2 modules (roast, fillin,
// overunder, confession) integrate with the core night machine and NEVER get skipped
// when the arc deals them. The engine's "no module registered" path (engine.ts) still
// skips the unbuilt launch games (scatter/poison/redflag/alibi/titlefight, all M3) —
// this test asserts the exact contract for M2: every REGISTERED game a night deals is
// played end-to-end (INPUT..REVEAL, scored), only UNREGISTERED games are skipped, and
// the night always reaches JUDGMENT. It also exercises both blocking inputs (over/under
// TRUTH number, confession TRUE/FALSE lock) — the ones the skippable games can't fake.
//
// This driver is the engine-side twin of packages/server/test/botlogic.ts: same game
// contracts, independent implementation (one over reduce(), one over the wire) — so the
// two cross-check each module's view()/input() surface from both directions.
import { describe, expect, it } from 'vitest';
import type { GameCtx } from '../src/games/module.js';
import type { GameEvent, Player, RoomState } from '../src/types.js';
import { computeJudgment, getModule, initialRoom } from '../src/engine.js';
import { GAMES } from '../src/arc.js';
import { rng } from '../src/rng.js';
import { Driver, mkConfig } from './helpers.js';

// M3: every LAUNCH8 game + the Title Fight spike now has a module — the arc's skip path is retired.
const REGISTERED = new Set(['roast', 'fillin', 'overunder', 'confession', 'scatter', 'poison', 'redflag', 'alibi', 'titlefight']);
const CIRCLE_PHASES = new Set(['DEAL', 'INPUT', 'WAITING_ON', 'REVEAL']);

type View = Record<string, unknown> | null;

/** Compose a module's per-viewer view exactly as server/redact.ts composeGameView does. */
function viewFor(s: RoomState, viewerId: string): View {
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
  try {
    return (m.view(ctx, viewerId) ?? null) as View;
  } catch {
    return null;
  }
}

type Wire = { t: string; [k: string]: unknown };
const inp = (p: Record<string, unknown>): Wire => ({ t: 'INPUT', p });

/** Engine-side twin of botlogic.botMoves — decides a seat's events from its own view. */
function movesFor(v: View, youId: string, roster: readonly Player[], isHost: boolean): Wire[] {
  if (!v) return [];
  const deck = v['deck'];
  const sub = v['sub'];
  if (sub === 'REVEAL') return isHost ? [{ t: 'FIRE', n: 1 }, { t: 'DESCEND' }] : [{ t: 'FIRE', n: 1 }];
  const other = (): string | null => roster.find((p) => p.id !== youId)?.id ?? null;

  if (deck === 'roast') {
    if (sub === 'VOTE' && v['youVoted'] == null) {
      const t = other();
      return t ? [inp({ vote: t })] : [];
    }
    return [];
  }
  if (deck === 'fillin') {
    if (sub === 'WRITE') {
      const you = v['you'] as Record<string, unknown> | undefined;
      if (!you) return [];
      const out: Wire[] = [];
      if (you['isReader'] === true && you['yourTone'] == null) out.push(inp({ tone: 0 }));
      if (you['setup'] != null && you['yourAnswer'] == null) out.push(inp({ answer: `${youId} regrets nothing` }));
      return out;
    }
    if (sub === 'PERFORM') {
      if (v['teleprompter']) return v['mode'] === 'derange' || v['stage'] === 'faceoff' ? [inp({ read: true })] : [inp({ next: true })];
      if (v['assignment']) return [inp({ read: true })];
      return [{ t: 'FIRE', n: 1 }];
    }
    if (sub === 'VOTE' && v['youVoted'] == null) {
      const ballot = Array.isArray(v['ballot']) ? (v['ballot'] as Record<string, unknown>[]) : [];
      const e = ballot.find((x) => x['yours'] !== true);
      return e ? [inp({ vote: e['id'] })] : [];
    }
    return [];
  }
  if (deck === 'overunder') {
    if (sub === 'DEBATE' && v['youAreScribe'] === true) return [inp({ line: 5, lock: true })];
    if (sub === 'BET' && v['youAreSubject'] !== true && v['youBet'] == null) return [inp({ bet: 'over' })];
    if (sub === 'TRUTH' && v['youAreSubject'] === true) return [inp({ truth: 7 })]; // the blocking number
    return [];
  }
  if (deck === 'confession') {
    if (sub === 'PICK' && v['youAreConfessor'] === true) return [inp({ pick: 0 })];
    if (sub === 'LOCK' && v['youAreConfessor'] === true) return [inp({ truth: true })]; // the blocking lock
    if (sub === 'PERFORM' && v['youAreConfessor'] === true) return [{ t: 'REST' }];
    if (sub === 'JURY' && v['youAreConfessor'] !== true && v['youVoted'] == null) return [inp({ vote: 'believe' })];
    return [];
  }
  // ===== M3 games =====
  if (deck === 'scatter') {
    if (sub === 'BOOM' && v['youTapped'] == null) {
      const t = other();
      return t ? [inp({ tap: t })] : [];
    }
    return []; // BOMB self-advances on its hidden fuse
  }
  if (deck === 'redflag') {
    if (sub === 'DEFENSE' && v['youAreDefender'] === true) return [{ t: 'REST' }];
    if (sub === 'VOTE' && v['youAreDefender'] !== true && v['youVoted'] == null) return [inp({ vote: 'smash' })];
    return [];
  }
  if (deck === 'poison') {
    if (sub === 'PITCH' && v['youArePitcher'] != null) return [{ t: 'REST' }];
    if (sub === 'VOTE' && v['youArePitcher'] == null && v['youVoted'] == null)
      return v['ratingMode'] === true ? [inp({ rate: { A: 4, B: 2 } })] : [inp({ vote: 'A' })];
    return [];
  }
  if (deck === 'alibi') {
    if (sub === 'ALIBI' && v['youAreAccused'] === true) return [{ t: 'REST' }];
    if (sub === 'HUNT' && v['youAreAccused'] !== true && v['youPicked'] == null) {
      const lineup = Array.isArray(v['lineup']) ? (v['lineup'] as string[]) : [];
      return lineup.length >= 3 ? [inp({ picks: lineup.slice(0, 3) })] : [];
    }
    return [];
  }
  if (deck === 'titlefight') {
    if (sub === 'BOUT' && v['youAreFighter'] === true) return [{ t: 'REST' }];
    if (sub === 'VOTE' && v['youAreFighter'] !== true && v['youVoted'] == null) {
      const a = v['fighterA'];
      return typeof a === 'string' ? [inp({ vote: a })] : [];
    }
    return [];
  }
  return [];
}

function dispatchWire(d: Driver, id: string, m: Wire, at: number): void {
  if (m.t === 'INPUT') d.dispatch({ t: 'INPUT', id, payload: m['p'], at });
  else if (m.t === 'DESCEND') d.dispatch({ t: 'DESCEND', id, at });
  else if (m.t === 'REST') d.dispatch({ t: 'REST_CASE', id, at });
  else if (m.t === 'FIRE') d.dispatch({ t: 'FIRE', id, n: Number(m['n']) || 1, at });
}

interface NightOutcome {
  played: Set<number>; // circle indices that reached a live phase (INPUT/WAITING_ON/REVEAL)
  revealDecks: Set<string>; // decks that produced a REVEAL view
  reachedJudgment: boolean;
}

/** Drive one full night to JUDGMENT (or blow up loud on a deadlock). */
function driveNight(d: Driver, startAt: number): NightOutcome {
  const played = new Set<number>();
  const revealDecks = new Set<string>();
  let clock = startAt;
  for (let step = 0; step < 6000; step++) {
    const s = d.state;
    if (s.phase.k === 'JUDGMENT') return { played, revealDecks, reachedJudgment: true };
    if (CIRCLE_PHASES.has(s.phase.k) && s.phase.k !== 'DEAL') played.add(s.circleIdx);
    const actives = s.players.filter((p) => p.role !== 'imp');
    for (const p of actives) {
      const v = viewFor(s, p.id);
      if (v && v['sub'] === 'REVEAL' && typeof v['deck'] === 'string') revealDecks.add(v['deck']);
    }
    let acted = false;
    for (const p of actives) {
      for (const m of movesFor(viewFor(d.state, p.id), p.id, actives, p.role === 'host')) {
        dispatchWire(d, p.id, m, clock);
        acted = true;
      }
    }
    if (!acted) {
      if (d.pending.size === 0) break; // no inputs, no timers -> genuine deadlock; assert below catches it
      let minAt = Infinity;
      for (const at of d.pending.values()) minAt = Math.min(minAt, at);
      clock = Math.max(clock, minAt);
      d.fireNext();
    }
  }
  return { played, revealDecks, reachedJudgment: d.state.phase.k === 'JUDGMENT' };
}

/** A lobby with a per-arc crewId knob (crewId is in buildArc's rng seed) so we can vary arcs. */
function beganNight(n: number, depth: 5 | 7 | 9, crew: string): Driver {
  const d = new Driver(initialRoom('HELL', 0, true));
  for (let i = 0; i < n; i++) {
    d.dispatch({ t: 'JOIN', id: `P${i}`, name: `P${i}`, avatar: i % 16, at: i });
    d.dispatch({ t: 'ATTEST18', id: `P${i}` });
    d.dispatch({ t: 'CEILING', id: `P${i}`, v: 5 });
  }
  d.dispatch({ t: 'CONFIG', id: 'P0', cfg: mkConfig(depth, { crewId: crew }) });
  d.dispatch({ t: 'BEGIN', id: 'P0', at: 1000 } as GameEvent);
  return d;
}

describe('full night integration (M2-INT: registered games never skipped)', () => {
  // NOTE (arc fact, corrected 2026-07-19 after the card council): the spotlight gate was
  // idx>=3, which — with the forced opener/scatter/titlefight/bargain/finale slots — made
  // confession/poison/redflag/alibi UNREACHABLE at depth 5 AND 7: nearly half the corpus
  // (432 cards) could only be dealt in a Full Damnation. The gate is now idx>=2, so every
  // depth-7 Standard Descent deals one spotlight game (~27/27/25/22% split) and depth 9
  // rose to ~72-80% each. Depth 5 (Quick Dip) is still structurally spotlight-free — only
  // slot 1 is ever free there, and seating a single-performer game in circle 2 of a cold
  // room would break the warm-up invariant the grammar exists to protect. This exhaustive
  // pass still runs at depth 9 because that's where ALL nine games can co-occur.
  it('N=6 depth-9 nights complete, play EVERY circle (no skips), and exercise all 9 games', () => {
    const seen = new Set<string>();
    let nightsWithConfession = 0;
    let nightsWithBlockingTruth = 0;

    for (let k = 0; k < 24; k++) {
      const d = beganNight(6, 9, `crew-${k}`);
      const arc = d.state.arc.map((c) => c.game);
      const { played, revealDecks, reachedJudgment } = driveNight(d, 1000);

      // 1. the night always finishes — no deadlock, no stuck input
      expect(reachedJudgment, `seed ${k} arc=[${arc.join(',')}] stuck at ${d.state.phase.k}`).toBe(true);

      // 2. NOTHING is skipped anymore — every circle reached a live phase (M3 retired the skip path)
      expect(played.size, `seed ${k}: only ${played.size}/${arc.length} circles played — arc=[${arc.join(',')}]`).toBe(arc.length);

      // 3. every circle's game is registered (the arc never deals a game with no module)
      arc.forEach((game) => expect(getModule(game), `seed ${k}: arc dealt unbuilt ${game}`).toBeDefined());

      for (const dck of revealDecks) seen.add(dck);
      if (arc.includes('confession')) nightsWithConfession++;
      if (revealDecks.has('overunder') || revealDecks.has('confession')) nightsWithBlockingTruth++;
    }

    // Across the 24 varied depth-9 arcs, ALL nine games get exercised — including the M3
    // spotlight games and both blocking-input games.
    for (const g of ['roast', 'fillin', 'overunder', 'confession', 'scatter', 'poison', 'redflag', 'alibi', 'titlefight'])
      expect(seen.has(g), `game ${g} never reached a REVEAL across 24 depth-9 nights`).toBe(true);
    expect(nightsWithConfession).toBeGreaterThan(0);
    expect(nightsWithBlockingTruth).toBeGreaterThan(0);
  });

  it('a Quick Dip (depth 5) mixed night reaches JUDGMENT with every circle played (incl. scatter)', () => {
    // depth 5 @ N=5 for crewId "crew-test" is the same shape the DO/protocol test drives:
    // arc [overunder, roast, scatter, fillin, roast] — scatter is now PLAYED, not skipped.
    const d = beganNight(5, 5, 'crew-test');
    const arc = d.state.arc.map((c) => c.game);
    const { played, revealDecks, reachedJudgment } = driveNight(d, 1000);
    expect(reachedJudgment).toBe(true);
    const topScore = Math.max(...d.state.players.map((p) => p.score));
    expect(topScore).toBeGreaterThan(0); // games award points; a night of pure skips would be 0-0
    expect(played.size).toBe(arc.length); // nothing skipped
    expect(revealDecks.has('scatter')).toBe(true); // the mid-spike now actually plays
  });

  it('D-134: spotlight assignments bump spotlightCount, feed MOST WANTED, and stay deterministic', () => {
    for (let k = 0; k < 6; k++) {
      const d = beganNight(6, 9, `spot-${k}`);
      driveNight(d, 1000);
      expect(d.state.phase.k, `seed ${k} never reached JUDGMENT`).toBe('JUDGMENT');

      const counts = new Map(d.state.players.map((p) => [p.id, p.spotlightCount]));
      const total = [...counts.values()].reduce((a, b) => a + b, 0);
      const spotlit = [...counts.values()].filter((c) => c > 0).length;

      // the bump fired at all: a depth-9 arc always deals spotlight games (subjects +
      // poison/titlefight fighters), and every one of those assignments must land a bump.
      expect(total, `seed ${k}: not one spotlight bump across a full depth-9 night`).toBeGreaterThan(0);
      // fairness (pickSpotlight leans toward the lowest count) spreads it past one victim.
      expect(spotlit, `seed ${k}: the whole night's spotlight fell on a single player`).toBeGreaterThan(1);

      // the superlative the counter exists to feed now actually fires, on a real spotlit id.
      const mw = computeJudgment(d.state).superlatives.find((sp) => sp.title === 'MOST WANTED');
      expect(mw, `seed ${k}: MOST WANTED never fired despite spotlight bumps`).toBeDefined();
      expect(counts.get(mw?.playerId ?? ''), `seed ${k}: MOST WANTED points at an unspotlit id`).toBeGreaterThan(0);

      // engine purity: the same seed replays byte-identical counts (no clock/rng leak).
      const d2 = beganNight(6, 9, `spot-${k}`);
      driveNight(d2, 1000);
      expect(new Map(d2.state.players.map((p) => [p.id, p.spotlightCount]))).toEqual(counts);
    }
  });

  it('D-134: "WHO WANTS BLOOD?" — a volunteer during CIRCLE_INTRO takes that circle\'s spotlight', () => {
    const SPOTLIGHT = new Set(['overunder', 'confession', 'redflag', 'alibi', 'poison', 'titlefight']);
    // The claimant is the spotlit one iff their OWN view says so — via a per-game "youAre*"
    // flag (confession hides the id, so the flag is the only tell) OR a public spotlight-id
    // field naming them (overunder's DEBATE exposes subjectId, not youAreSubject).
    const isSpotlit = (v: View, who: string): boolean => {
      if (!v) return false;
      if (
        v['youAreSubject'] === true ||
        v['youAreConfessor'] === true ||
        v['youAreDefender'] === true ||
        v['youAreAccused'] === true ||
        v['youAreFighter'] === true ||
        (v['youArePitcher'] !== null && v['youArePitcher'] !== undefined)
      )
        return true;
      for (const f of ['subjectId', 'confessorId', 'defenderId', 'accusedId', 'pitcherA', 'pitcherB', 'fighterA', 'fighterB'])
        if (v[f] === who) return true;
      return false;
    };

    let verified = 0;
    for (let k = 0; k < 16 && verified < 4; k++) {
      const d = beganNight(6, 9, `blood-${k}`);
      const actives = d.state.players.filter((p) => p.role !== 'imp');
      let clock = 1000;
      let volunteer: string | null = null;
      let claimedCircle = -1;
      let checked = false;

      for (let step = 0; step < 6000 && !checked; step++) {
        const s = d.state;
        if (s.phase.k === 'JUDGMENT') break;
        const game = s.arc[s.circleIdx]?.game;

        // At an untouched spotlight circle's intro, one eligible player claims the spotlight.
        if (s.phase.k === 'CIRCLE_INTRO' && game && SPOTLIGHT.has(game) && s.circleIdx !== claimedCircle) {
          // a NON-host active player (host isn't excluded, but keep the pick unambiguous)
          volunteer = actives[1 + ((s.circleIdx + k) % (actives.length - 1))]!.id;
          d.dispatch({ t: 'CLAIM', id: volunteer, at: clock });
          claimedCircle = s.circleIdx;
          continue; // let the intro timer fire next, running m.start with the claim in hand
        }
        // First live phase of the claimed circle: the claimant must be the one on the spike.
        if (volunteer && s.circleIdx === claimedCircle && (s.phase.k === 'INPUT' || s.phase.k === 'WAITING_ON' || s.phase.k === 'REVEAL')) {
          expect(isSpotlit(viewFor(s, volunteer), volunteer), `seed ${k} circle ${s.circleIdx} (${game}): volunteer ${volunteer} did NOT get the spotlight`).toBe(true);
          verified++;
          checked = true;
          break;
        }

        // advance the night (play moves; else fire the earliest timer)
        let acted = false;
        for (const p of actives) {
          for (const m of movesFor(viewFor(d.state, p.id), p.id, actives, p.role === 'host')) {
            dispatchWire(d, p.id, m, clock);
            acted = true;
          }
        }
        if (!acted) {
          if (d.pending.size === 0) break;
          let minAt = Infinity;
          for (const at of d.pending.values()) minAt = Math.min(minAt, at);
          clock = Math.max(clock, minAt);
          d.fireNext();
        }
      }
    }
    expect(verified, 'never reached a spotlight circle to verify the valve').toBeGreaterThan(0);
  });

  it('sanity: every AVAILABLE game has a registered module — the skip path is fully retired', () => {
    const available = (Object.keys(GAMES) as (keyof typeof GAMES)[]).filter((g) => GAMES[g].available);
    for (const g of available) expect(getModule(g), `${g} is available in the arc but has no module`).toBeDefined();
    expect(available.filter((g) => !getModule(g))).toEqual([]);
  });
});
