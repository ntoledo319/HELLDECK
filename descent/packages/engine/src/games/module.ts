// The per-game contract. Each games/<deck>.ts exports a GameModule. The core engine
// (engine.ts) owns the night; a module owns one circle's loops. Spec Part 5.
// OWNERSHIP RULE for parallel work: only engine-core edits engine.ts/types.ts;
// game modules keep their own state/payload types INSIDE their file.
import type { CircleSpec, Effect, Player, RoomState, SpotlightResolution } from '../types.js';

// Synthetic timer ids the core feeds to module.timer (never scheduled on the DO).
// They live HERE (not engine.ts) so modules can import them without a cycle.
export const CORE_DEALT = 'core:dealt';
export const CORE_REVEAL_DONE = 'core:revealDone';
export const CORE_SPOTLIGHT_DONE = 'core:spotlightDone';

export interface GameCtx {
  state: RoomState;
  circle: CircleSpec;
  circleIdx: number;
  players: Player[]; // active players (host+players), imps excluded unless module opts in
  imps: Player[];
  now: number; // event time — modules NEVER read clocks
  rand: () => number; // seeded
  finaleMult: number; // 1 or 3
  volunteers: Player[]; // 4.5 "WHO WANTS BLOOD?" claimants (active players only); core assignment ranks these first
  /** Present only while core calls timer(CORE_SPOTLIGHT_DONE). Modules commit the
   * final role ids here, after both private burn windows have closed. */
  spotlight?: SpotlightResolution;
}

export interface GameStep {
  gameState: unknown; // module-owned shape, persisted opaquely in RoomState.gameState
  effects: Effect[];
  scores?: Record<string, number>; // playerId -> points to award NOW (pre-multiplier)
  done?: boolean; // circle complete -> core advances to LADDER
  // D-134: player ids that ENTER the spotlight as a result of THIS step (one-shot,
  // like `scores` — emit only on the assigning step, never re-list a standing holder).
  // The core bumps each one's Player.spotlightCount, driving pickSpotlight's
  // lean-toward-lowest fairness across loops/circles and the MOST WANTED superlative.
  // Burnable assignments use $spotlight instead; the core bumps only the final ids.
  // This remains for non-burnable/elected spotlights such as Roast victims.
  spotlight?: string[];
}

export interface GameModule {
  deck: string;
  minN: number;
  /** Circle begins (after CIRCLE_INTRO). Deal cards, schedule first phase. */
  start(ctx: GameCtx): GameStep;
  /** A player input arrived for this circle. Validate; ignore stale/foreign. */
  input(ctx: GameCtx, playerId: string, payload: unknown): GameStep;
  /** A timer this module scheduled fired (timerId it chose). */
  timer(ctx: GameCtx, timerId: string): GameStep;
  /** Module-scoped controls (REST_CASE, SKIP_EM, PLEAD_FIFTH, VOID) routed by core. */
  control(ctx: GameCtx, playerId: string, kind: 'REST' | 'SKIPEM' | 'FIFTH' | 'VOID'): GameStep;
  /** Redacted per-viewer view of gameState — the ONLY thing serialized to clients. */
  view(ctx: GameCtx, viewerId: string): unknown;
}
