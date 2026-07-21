// DESCENT_BUILD_SPEC.md Part 2 — the data model. This file is the spec made compilable.
// LAW: engine code never reads clocks, sockets, or storage. All time arrives inside events.

// ===== identity =====
export type PlayerId = string; // nanoid(12), stable across reconnects
export type RoomCode = string; // 4 chars from CODE_ALPHABET

export const CODE_ALPHABET = 'BCDFGHJKLMNPRSTVWXYZ'; // no vowels, no 0/O/1/I lookalikes

export type Role = 'host' | 'player' | 'imp';

export interface Player {
  id: PlayerId;
  name: string; // 1..14 chars, trimmed, uniqued per room ("SAM (2)")
  avatar: number; // 0..15
  role: Role;
  seat: number; // join order, 0-based; host = 0
  connected: boolean;
  lastSeenAt: number; // server ms, from events
  heatCeiling: 1 | 2 | 3 | 4 | 5; // PRIVATE — never broadcast (redaction test D-113/114)
  ceilingSet: boolean; // BEGIN gate: defaults false; a CEILING event flips it (value alone can't prove a pick)
  attested18: boolean;
  brimstones: number; // starts at 2
  score: number;
  spotlightCount: number; // fairness counter
  freshMeat: boolean; // first night with this crew
}

// ===== night config =====
export type Vibe = 'sober' | 'warm' | 'feral';

export interface NightConfig {
  depth: 5 | 7 | 9;
  vibe: Vibe;
  stageMode: boolean; // default nPlayers >= 5
  crewId: string; // hash of sorted lowercased names
  irlFamiliar: boolean;
}

// ===== cards =====
export type Register =
  | 'observational'
  | 'absurdist'
  | 'deadpan'
  | 'menace'
  | 'petty-domestic'
  | 'gross'
  | 'physical'
  | 'parody'
  | 'table-aware'
  | 'euphemism';

export type DeckId =
  | 'roast'
  | 'fillin'
  | 'overunder'
  | 'confession'
  | 'scatter'
  | 'poison'
  | 'redflag'
  | 'alibi'
  | 'texttrap'
  | 'reality'
  | 'taboo'
  | 'hotseat'
  | 'titlefight';

export type Tier = 1 | 2 | 3 | 4 | 5;

export interface CardBase {
  id: string; // "<deck>_v3_<n>"
  deck: DeckId;
  text: string; // may contain {NAME} / {NAME2}
  exposure: Tier;
  chaos: Tier; // invariant: chaos >= 3 (lint-enforced)
  register: Register;
  skeleton: string; // dedup slug
}

export interface OverUnderCard extends CardBase {
  deck: 'overunder';
  receiptSurface: string; // e.g. "Settings → Screen Time"
  timebox: string; // "today" | "this week" | "this month" | "right now" | bounded-event
}
export interface PoisonCard extends CardBase {
  deck: 'poison';
  optionA: string;
  optionB: string;
}
export interface RedFlagCard extends CardBase {
  deck: 'redflag';
  perk: string;
  flag: string;
}
export interface AlibiCard extends CardBase {
  deck: 'alibi';
  accusation: string;
  words: [string, string, string];
  decoys: [string, string, string, string, string];
}
export interface ScatterCard extends CardBase {
  deck: 'scatter';
  category: string; // <= 7 words
  letter: string; // single uppercase letter
}
export interface TabooCard extends CardBase {
  deck: 'taboo';
  word: string;
  forbidden: string[]; // exactly 5
}
export interface TextTrapCard extends CardBase {
  deck: 'texttrap';
  sender: string;
  message: string;
  tone: string;
}

export type Card =
  | CardBase
  | OverUnderCard
  | PoisonCard
  | RedFlagCard
  | AlibiCard
  | ScatterCard
  | TabooCard
  | TextTrapCard;

// ===== night state machine =====
export type Phase =
  | { k: 'LOBBY' }
  | { k: 'CONSENT' }
  | { k: 'CIRCLE_INTRO'; circle: number }
  | { k: 'DEAL'; circle: number }
  | { k: 'INPUT'; circle: number; sub: string; deadline: number | null } // null = paused (blocking)
  | { k: 'WAITING_ON'; circle: number; who: PlayerId; since: number }
  | { k: 'REVEAL'; circle: number; holdSince: number }
  | { k: 'LADDER'; circle: number }
  | { k: 'JUDGMENT' };

export interface CircleSpec {
  game: DeckId;
  loops: number; // roast=3 prompts, overunder=2 subjects, scatter=3 bombs, fillin=1|2 setups, single-performer=2
  finale: boolean; // x3
  outward: boolean; // arc-grammar class (aim-outward game)
  rung: Tier; // E-curve rung for this circle (4.3) — computed at arc build, deterministic
  bargain: boolean; // Devil's Bargain attaches here (holder assigned at this circle's start)
}

// ===== deal ceremony (4.5) =====
export interface DealRequest {
  primary: Card;
  backup: Card; // reserved up-front so a burn swap needs zero extra picks
  subjectId: PlayerId | null; // named subject gets the private pre-view + burn window
}

export interface DealState {
  card: Card; // current card (the backup after a burn) — SECRET until done
  backup: Card | null; // null once consumed by a burn
  burnedId: string | null; // vetoed card id — never re-dealt this night, never attributed
  subjectId: PlayerId | null;
  startedAt: number;
  burnWindowEndsAt: number; // startedAt + 10s when a subject is named, else startedAt (no window)
  completesAt: number; // FIXED — a burn never moves it (timing-identical law)
  timerId: string;
  done: boolean; // true once the card is public
}

/**
 * Socket-private card preview protocol. The preview id is stable for the whole
 * fixed ceremony, so clients can correlate a burn acknowledgement without
 * inferring anything from public state or timing.
 */
export interface CardPreviewAssignedPayload {
  status: 'assigned';
  previewId: string;
  card: Card;
  burnDeadline: number;
  revealAt: number;
  canBurn: boolean;
}

export interface CardPreviewReleasedPayload {
  status: 'released';
  previewId: string;
}

export type CardPreviewPrivatePayload =
  | CardPreviewAssignedPayload
  | CardPreviewReleasedPayload;

// ===== spotlight assignment ceremony (4.5 / D-134) =====
// A spotlight is deliberately separate from a card deal. Games declare who may be
// assigned and which role(s) they need; the core owns private delivery, burns, fixed
// timing, and final fairness accounting.
export type SpotlightRole =
  | 'subject'
  | 'confessor'
  | 'defender'
  | 'accused'
  | 'pitcher-a'
  | 'pitcher-b'
  | 'fighter-a'
  | 'fighter-b';

export interface SpotlightRequest {
  roles: SpotlightRole[]; // one or two distinct performer roles
  eligibleIds: PlayerId[]; // module-filtered for consent/loop exclusions; core validates active players
}

export interface SpotlightAssignment {
  role: SpotlightRole;
  playerId: PlayerId | null; // null when every safe replacement for this slot was exhausted
}

export interface SpotlightResolution {
  assignments: SpotlightAssignment[];
}

export interface SpotlightAssignedPayload {
  status: 'assigned';
  ceremonyId: string;
  role: SpotlightRole;
  burnDeadline: number;
  announceAt: number;
  canBurn: boolean;
}

export interface SpotlightReleasedPayload {
  status: 'released';
  ceremonyId: string;
}

export type SpotlightPrivatePayload = SpotlightAssignedPayload | SpotlightReleasedPayload;

export interface SpotlightSlotState extends SpotlightAssignment {
  burnable: boolean; // true only during this assignee's private 10s window
}

export interface SpotlightState {
  ceremonyId: string;
  slots: SpotlightSlotState[];
  reserveIds: PlayerId[]; // deterministic, private replacement order
  declinedIds: PlayerId[]; // never reassign within this ceremony
  window: 'primary' | 'replacement' | 'done';
  startedAt: number;
  handoffAt: number; // fixed T+10, scheduled even when nobody burns
  completesAt: number; // fixed T+20, scheduled even when nobody burns
  handoffTimerId: string;
  completionTimerId: string;
}

// ===== module → core directives =====
// GameModule.gameState may carry these reserved keys; the core (engine.ts) consumes
// and strips them after every step. This is how a module drives the night's Phase
// without owning it. See engine.ts "module step protocol".
export type PhaseDirective =
  | { k: 'INPUT'; sub: string; deadline: number | null } // null deadline = paused (blocking)
  | { k: 'WAITING_ON'; who: PlayerId; since: number }
  | { k: 'REVEAL' }; // core stamps holdSince and arms softcap + fire-decay timers

export interface ModuleDirectives {
  $phase?: PhaseDirective;
  $deal?: DealRequest;
  $spotlight?: SpotlightRequest;
}

// ===== night stats & judgment =====
export interface PlayerNightStats {
  fires: number; // fire taps sent (superlative fuel — "Fastest Fingers" class)
  fifths: number; // times they pled the fifth
}

export interface Superlative {
  title: string;
  playerId: PlayerId;
}

export interface JudgmentSummary {
  winners: PlayerId[]; // ties share the crown
  superlatives: Superlative[];
  bargain: { holder: PlayerId; circle: number } | null; // revealed HERE, never earlier
}

export interface TelemetryEvent {
  t: string;
  cardId?: string;
  fires?: number;
  at: number;
}
// PRIVACY WHITELIST (spec Part 10): scores, win records, superlative TITLES, night count,
// per-card fire counts. NEVER: card-to-person linkage, confession truth values,
// who-voted-whom, ceiling values, attributable burn events.

export interface RoomState {
  code: RoomCode;
  createdAt: number;
  config: NightConfig | null;
  players: Player[];
  phase: Phase;
  arc: CircleSpec[];
  circleIdx: number;
  gameState: unknown; // per-game state; each games/<deck>.ts owns its shape
  usedCardIds: string[];
  usedSkeletons: Record<string, number>;
  devilsBargain: { holder: PlayerId; circle: number } | null;
  epoch: number; // bumped on every phase change
  entitled: boolean;
  telemetry: TelemetryEvent[];
  deal: DealState | null; // live deal ceremony (core-owned; modules request via $deal)
  spotlight?: SpotlightState | null; // optional for pre-D134 snapshots; live state is persisted and never serialized raw
  moduleTimers: Record<string, { atMs: number; setAt: number }>; // module-scheduled timers the core can fire early (DESCEND on module-held reveals)
  lastFireAt: number | null; // reveal-hold fire-decay tracking
  nightStats: Record<PlayerId, PlayerNightStats>;
  judgment: JudgmentSummary | null; // computed on entering JUDGMENT
  circleStartScores: Record<PlayerId, number>; // snapshot at CIRCLE_INTRO — the LADDER's "biggest mover" delta base (public data)
  volunteers: PlayerId[]; // 4.5 "WHO WANTS BLOOD?" claimants for THIS circle's spotlight assignment; reset each CIRCLE_INTRO
}

// ===== events (the ONLY way anything changes) =====
export type GameEvent =
  | { t: 'JOIN'; id: PlayerId; name: string; avatar: number; at: number }
  | { t: 'LEAVE'; id: PlayerId; at: number }
  | { t: 'ATTEST18'; id: PlayerId }
  | { t: 'CONFIG'; id: PlayerId; cfg: NightConfig }
  | { t: 'CEILING'; id: PlayerId; v: Tier }
  | { t: 'BEGIN'; id: PlayerId; at: number }
  | { t: 'INPUT'; id: PlayerId; payload: unknown; at: number }
  | { t: 'BURN'; id: PlayerId; kind: 'card' | 'spotlight'; at: number }
  | { t: 'DESCEND'; id: PlayerId; at: number }
  | { t: 'VOID_ROUND'; id: PlayerId; at: number }
  | { t: 'PLEAD_FIFTH'; id: PlayerId; at: number }
  | { t: 'SKIP_EM'; id: PlayerId; at: number }
  | { t: 'REST_CASE'; id: PlayerId; at: number }
  | { t: 'FIRE'; id: PlayerId; n: number; at: number }
  | { t: 'CLAIM'; id: PlayerId; at: number } // 4.5 "WHO WANTS BLOOD?" — volunteer for this circle's spotlight during CIRCLE_INTRO
  | { t: 'TIMER'; timerId: string; at: number } // fired by DO Alarm
  | { t: 'RECONNECT'; id: PlayerId; at: number };

// ===== effects (engine asks; DO executes) =====
export type Effect =
  | { k: 'SCHEDULE'; timerId: string; atMs: number }
  | { k: 'CANCEL'; timerId: string }
  | { k: 'BROADCAST' } // DO re-serializes redacted state/patch per socket
  | { k: 'SEND'; to: PlayerId; kind: 'preview'; payload: CardPreviewPrivatePayload }
  | { k: 'SEND'; to: PlayerId; kind: 'spotlight'; payload: SpotlightPrivatePayload }
  | { k: 'SEND'; to: PlayerId; kind: 'role' | 'card' | 'words'; payload: unknown }
  | { k: 'SNAPSHOT' }
  | { k: 'AUDIO'; sting: string };

export interface ReduceResult {
  state: RoomState;
  effects: Effect[];
}
