// Local mirrors of the per-socket view the server broadcasts (spec 3.2 STATE / 3.4 redaction).
// LAW: never import server internals — the wire is the contract; these are the client's half.
// Engine types are fair game via @helldeck/engine. Game views mirror the engine modules'
// view() output verbatim: objects discriminated by `deck` + `sub`.
import type { Phase, Role } from '@helldeck/engine';

export interface PlayerView {
  id: string;
  name: string;
  avatar: number; // 0..15
  role: Role;
  seat: number;
  connected: boolean;
  brimstones: number; // own only; others arrive as -1
  score: number;
  sealed?: boolean; // ceiling PICKED (a fact, never the value — spec 3.4)
}

export interface JudgmentView {
  winners: string[]; // ties share the crown
  superlatives: { title: string; playerId: string }[];
  bargain: { holder: string; circle: number } | null; // revealed HERE, never earlier
}

export interface RoomView {
  code: string;
  you: string;
  config: { depth: number; vibe: string; stageMode: boolean } | null;
  players: PlayerView[];
  phase: Phase;
  circleIdx: number;
  arcLength: number;
  gameView: unknown; // per-game redacted view; runtime-guarded via asView()
  judgment: JudgmentView | null; // present only at phase JUDGMENT
}

// ===== game views (wire mirrors of engine view() shapes; guarded at runtime) =====
export interface IntroView {
  deck: string;
  sub: 'INTRO';
  firstTime: boolean; // 15s explainer vs 5s title card (server owns the clock)
  claimable?: boolean; // 4.5: spotlight game — the room can claim the spotlight during the intro
  claimed?: number; // count of volunteers so far (never who — redaction)
  youVolunteered?: boolean; // your own claim, echoed back
}

export interface LadderGameView {
  deck: string;
  sub: 'LADDER';
  deltas: Record<string, number>; // playerId -> points gained this circle
}

export interface RoastVoteView {
  deck: 'roast';
  sub: 'VOTE';
  loop: number; // 0-based prompt index
  loops: number; // 3 per circle (spec 5.1)
  attributed: boolean; // N<=4 FACE YOUR ACCUSERS mode
  prompt: { id: string; text: string };
  deadline: number; // server ms mirror; the countdown rides phase.deadline/AT
  eligible: number;
  votedCount: number; // counts only — never who
  youVoted: string | null; // own ballot, nobody else's
}

export interface RoastRevealView {
  deck: 'roast';
  sub: 'REVEAL';
  loop: number;
  loops: number;
  attributed: boolean;
  prompt: { id: string; text: string };
  victims: string[]; // 2 entries = DOUBLE ROAST; 0 = voided/silent room
  doubleRoast: boolean;
  roomHeat: boolean;
  voided: boolean;
  youVoted: string | null;
  edges?: { from: string; to: string }[]; // N<=4 only; `to` may be "▮▮▮" (suppressed repeat)
  spread?: { target: string; weight: number }[]; // N>=5 anonymous bars
}

// Runtime guard: the wire hands us `unknown`; trust nothing without a matching `sub`.
export function asView<T extends { sub: string }>(gv: unknown, sub: T['sub']): T | null {
  return gv != null && typeof gv === 'object' && (gv as { sub?: unknown }).sub === sub ? (gv as T) : null;
}
