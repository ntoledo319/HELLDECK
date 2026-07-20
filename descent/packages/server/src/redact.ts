// Per-socket redaction — DESCENT_BUILD_SPEC.md Part 3.4. Test D-113 asserts no secret
// field ever appears in a frame for the wrong player. This function is the ONLY path
// from RoomState to the wire.
import { assignsSpotlight, getModule, rng } from '@helldeck/engine';
import type { GameCtx, RoomState } from '@helldeck/engine';

export interface PlayerView {
  id: string;
  name: string;
  avatar: number;
  role: string;
  seat: number;
  connected: boolean;
  brimstones: number; // own only; others see -1
  score: number;
  sealed: boolean; // ceiling PICKED — a fact for the lobby's BEGIN mirror, never the value (3.4)
}

export interface RoomView {
  code: string;
  you: string;
  config: { depth: number; vibe: string; stageMode: boolean } | null;
  players: PlayerView[];
  phase: RoomState['phase'];
  circleIdx: number;
  arcLength: number;
  // gameState views are produced per game by games/<deck>.ts view functions (D-112+).
  gameView: unknown;
  // Public by construction: the engine computes this only on entering JUDGMENT —
  // winners, superlatives, and the Bargain revealed HERE, never earlier (Part 2 law).
  judgment: RoomState['judgment'];
}

// Key names that must never appear in any frame (spec 3.4 + Part 10). Substring guard —
// covers the raw state fields AND the deal ceremony's burn attribution.
const NEVER_SERIALIZE = [
  'heatCeiling',
  'ceilingSet',
  'devilsBargain',
  'usedCardIds',
  'telemetry',
  'crewId',
  'burnedId', // DealState: a burn is never attributable (4.5)
  'reserveIds', // SpotlightState: private candidate order
  'declinedIds', // SpotlightState: attributable safety-valve history
  'handoffTimerId',
  'completionTimerId',
  'moduleTimers',
  'nightStats',
  // Module-state internals (roast.ts RoastState) that must never survive view():
  'votes', // voter -> target map; only counts/own-ballot may serialize ("votedCount"/"youVoted" don't match this quoted-key guard)
  'shownEdges',
  'participation',
] as const;

// ===== per-game view (spec 3.4: gameState never serialized raw) =====
// The active GameModule's view() is the ONLY serialization surface for gameState —
// composed here per viewer via the engine's registry (getModule). Mirrors the engine's
// internal makeCtx(); views are read-only, so `now` is 0 (deadlines ride the AT channel
// plus view-carried mirrors) and `rand` is seeded per (room, epoch, viewer) — stable
// across re-broadcasts within an epoch, so a per-phone shuffle can't reroll mid-phase.
const CIRCLE_PHASES = new Set(['DEAL', 'INPUT', 'WAITING_ON', 'REVEAL']);

function composeGameView(s: RoomState, viewerId: string): unknown {
  const spec = s.arc[s.circleIdx];
  if (!spec) return null;
  // CIRCLE_INTRO/LADDER views are core-composed (public by construction): the intro
  // must name the game even when its module isn't registered yet, and the ladder's
  // deltas come from the core's circle-start score snapshot.
  if (s.phase.k === 'CIRCLE_INTRO') {
    const firstTime = !s.arc.slice(0, s.circleIdx).some((c) => c.game === spec.game);
    // 4.5 "WHO WANTS BLOOD?": spotlight games take volunteers during the intro. Only the
    // count + your own claim ever serialize — never the full claimant list (redaction).
    const claimable = assignsSpotlight(spec.game);
    return {
      deck: spec.game,
      sub: 'INTRO',
      firstTime,
      claimable,
      claimed: claimable ? s.volunteers.length : 0,
      youVolunteered: claimable && s.volunteers.includes(viewerId),
    };
  }
  if (s.phase.k === 'LADDER') {
    const deltas: Record<string, number> = {};
    for (const p of s.players) deltas[p.id] = p.score - (s.circleStartScores[p.id] ?? p.score);
    return { deck: spec.game, sub: 'LADDER', deltas };
  }
  if (!CIRCLE_PHASES.has(s.phase.k)) return null;
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
    volunteers: s.players.filter((p) => p.role !== 'imp' && s.volunteers.includes(p.id)),
  };
  return m.view(ctx, viewerId) ?? null;
}

function safeGameView(s: RoomState, viewerId: string): unknown {
  try {
    return composeGameView(s, viewerId);
  } catch {
    return null; // a view() crash must never take the broadcast down — degrade to nothing
  }
}

export function redactFor(s: RoomState, viewerId: string): RoomView {
  return {
    code: s.code,
    you: viewerId,
    config: s.config
      ? { depth: s.config.depth, vibe: s.config.vibe, stageMode: s.config.stageMode }
      : null,
    players: s.players.map((p) => ({
      id: p.id,
      name: p.name,
      avatar: p.avatar,
      role: p.role,
      seat: p.seat,
      connected: p.connected,
      brimstones: p.id === viewerId ? p.brimstones : -1,
      score: p.score,
      sealed: p.ceilingSet, // the FACT of a pick (BEGIN gate mirror); the value never leaves the engine
    })),
    phase: s.phase,
    circleIdx: s.circleIdx,
    arcLength: s.arc.length,
    gameView: safeGameView(s, viewerId), // per-viewer module view — never the raw gameState
    judgment: s.judgment,
  };
}

// Guard for tests: serialize a view and assert banned keys never appear.
export function assertNoSecrets(json: string): void {
  for (const key of NEVER_SERIALIZE) {
    if (json.includes(`"${key}"`)) throw new Error(`secret field leaked: ${key}`);
  }
}
