// FILL-IN FINISHER — spec 5.2 + HDRealRules2 Part III §2. Task D-121.
// A setup with a blank. Everyone but the Reader writes a punchline; answers go
// ONLY to the Reader's teleprompter — first exposure is ALWAYS out loud to the
// room. Perform, then vote: the ballot (full texts, randomized per phone) unlocks
// only after the read. The app carries the words; a human mouth does the damage.
//
// Shapes by table size:
//   N=3   — derangement mode: no Reader; each player performs ANOTHER player's
//           line ("you're performing SAM's filth — sell it"); all 3 vote among
//           the other two. Attribution is embraced, not hidden (2 derangements
//           exist at N=3 — everyone can do the math anyway).
//   N=4-6 — one setup, one Reader (spotlight-rotated; finale: never last place,
//           never the Bargain holder), everyone else writes.
//   N>=7  — two parallel setups, two Readers. Per setup the Reader performs <=4
//           answers in one run -> one vote picks the top -> grand face-off (both
//           Readers perform, final vote). Imps contribute 1 curated punchline as
//           a screen cameo — never performed, never on a ballot.
//
// Module law (games/module.ts): state lives opaquely in RoomState.gameState;
// view() is the ONLY serialization surface, so every redaction rule lives there.
// Pure module: time arrives as ctx.now, randomness as ctx.rand. Never a clock.
//
// Core integration (engine.ts "module step protocol") — built like roast:
//   $deal (4.5 ceremony, 5.5s, night-dedup writeback) -> CORE_DEALT
//   [bracket: a second $deal -> CORE_DEALT — two setups, two honest ceremonies]
//   -> $phase INPUT "WRITE" (60s skippable; PANIC = 2 curated fallbacks at half
//      points; empty at the timer auto-takes option A; Readers pick a read-tone)
//   -> $phase INPUT "PERFORM" (Reader-paced teleprompter, 90s cap schedule;
//      [NEXT] advances, [BURN LINE] strikes — struck lines never reach a ballot)
//   -> $phase INPUT "VOTE" (15s skippable; texts as memory-aid, order randomized
//      PER VIEWER; authors never vote their own line)
//   -> $phase REVEAL (core-held: flip beat, DESCEND, fire-decay)
//   -> CORE_REVEAL_DONE -> done. No blocking inputs anywhere: a silent writer
//      auto-panics, a silent voter abstains — this game never waits on anyone.
import type { CardBase, DealRequest, Effect, Player, PlayerId } from '../types.js';
import { cardLegal } from '../consent.js';
import { pick, rng } from '../rng.js';
import { pickSpotlight } from '../deal.js';
import { points, readerFirePoints } from '../scoring.js';
import { CORE_DEALT, CORE_REVEAL_DONE, type GameCtx, type GameModule, type GameStep } from './module.js';

// ===== tuning (spec 5.2 / 4.6 / 4.8) =====
export const WRITE_MS = 60_000; // WRITE is skippable: deadline -> auto-panic option A
export const VOTE_MS = 15_000; // VOTE is skippable: deadline -> non-voters auto-abstain
export const PERFORM_CAP_MS = 90_000; // Reader-paced, but the schedule caps the ham
export const MAX_ANSWER_CHARS = 140;
export const BRACKET_MIN_N = 7; // N>=7 -> two setups, two Readers
export const BRACKET_RUN_CAP = 4; // per-setup answers performed in one bracket run
export const IMP_WEIGHT = 0.5;
/** The Reader announces the register before the read — 4 options, all funerals. */
export const READ_TONES = ['a eulogy', 'a 911 call', 'a hostage tape', 'an apology video'] as const;
/**
 * Panic pool of last resort. Cards carry their own curated panicA/panicB; if an
 * injected deck forgot them, these module-curated lines keep auto-panic honest —
 * curated CONTENT is fair game (the 4.7 never-fabricate law is about truth values).
 */
export const PANIC_FALLBACKS: readonly [string, string] = [
  '[REDACTED ON ADVICE OF COUNSEL]',
  'whatever the group chat already decided',
];

// ===== deck =====
/** Fill-in setups are CardBase + the card's own curated panic pool. */
export interface FillinCard extends CardBase {
  deck: 'fillin';
  panicA?: string;
  panicB?: string;
}

// Card plumbing arrives with content integration (D-127/8.3); until then the core
// (or a test) injects a deck here. Stubs keep dev nights from tasting like lorem ipsum.
export const DEFAULT_FILLIN_DECK: FillinCard[] = [
  stub('001', "The last text you'd ever send is '____' and everyone here knows it.", 2, 3, 'observational', 'last-text', 'u up? sorry, wrong person', "this isn't as bad as it looks"),
  stub('002', "The cult flyer that would actually get this group: 'JOIN US. WE HAVE ____.'", 2, 4, 'absurdist', 'cult-flyer', 'a group chat with rules', 'snacks and zero accountability'),
  stub('003', "My therapist's notes just say '____' underlined three times.", 3, 3, 'deadpan', 'therapist-notes', 'knows what they did', "still calls it 'a phase'"),
  stub('004', "The passive-aggressive kitchen note that ends a friendship: 'WHOEVER ____ — WE KNOW.'", 2, 3, 'petty-domestic', 'kitchen-note', 'licked the butter', 'took one bite and put it back'),
  stub('005', "They couldn't say it at the funeral, so the group chat said it: '____.'", 3, 4, 'menace', 'groupchat-eulogy', 'owed literally all of us money', 'died doing what they loved: lying'),
  stub('006', "My LinkedIn says 'consultant' but my bank statement says '____.'", 2, 4, 'euphemism', 'linkedin-vs-bank', '3 AM gas station decisions', 'a subscription to everything'),
  stub('007', "If my search history leaked, the apology text would start with '____.'", 4, 4, 'table-aware', 'search-history', 'it was for a friend. the friend is me', 'context is coming. do not open it'),
  stub('008', "The true-crime narrator says: 'Friends described them as ____.'", 3, 3, 'parody', 'true-crime-neighbor', 'quiet, but in a load-bearing way', 'helpful with alibis'),
];
function stub(
  n: string,
  text: string,
  exposure: FillinCard['exposure'],
  chaos: FillinCard['chaos'],
  register: FillinCard['register'],
  skeleton: string,
  panicA: string,
  panicB: string,
): FillinCard {
  return { id: `fillin_stub_${n}`, deck: 'fillin', text, exposure, chaos, register, skeleton, panicA, panicB };
}

let deckCards: FillinCard[] = DEFAULT_FILLIN_DECK;
export function setFillinDeck(cards: readonly FillinCard[]): void {
  if (cards.length === 0) throw new Error('fillin deck cannot be empty');
  deckCards = [...cards];
}

// ===== module state (opaque to core; view() redacts) =====
export type FillinMode = 'derange' | 'single' | 'bracket';
/** Which perform/vote is live. 'single' covers N<=6 (derangement included). */
export type FillinStage = 'single' | 'A' | 'B' | 'faceoff';

export interface FillinAnswer {
  author: PlayerId;
  text: string;
  panic: boolean; // curated fallback taken -> half points if it wins
  burned: boolean; // BURN LINE strike — excluded from every ballot
}

export interface FillinSetupState {
  card: FillinCard | null; // null until its CORE_DEALT (the ceremony owns the secret)
  readerId: PlayerId | null; // null in derangement mode
  writerIds: PlayerId[];
  tone: number | null; // index into READ_TONES
  answers: FillinAnswer[]; // SECRET — only view() decides who sees text, and when
  runOrder: number[]; // answer indices, ctx.rand-shuffled at perform open (ballot ids = positions here)
  runIdx: number; // teleprompter cursor
  runStartedAt: number | null; // finale fire-window start
  finalistIdx: number | null; // bracket: this setup's vote winner (answers index)
}

export interface FillinResolution {
  voided: boolean;
  winner: { text: string; authorId: PlayerId; panic: boolean } | null;
  runnerUp: { text: string; authorId: PlayerId } | null;
  cameos: { text: string; authorId: PlayerId }[]; // imp screen cameos (N>=7)
  burnedCount: number;
}

export interface FillinState {
  sub: 'DEAL' | 'WRITE' | 'PERFORM' | 'VOTE' | 'REVEAL';
  mode: FillinMode;
  stage: FillinStage;
  setups: FillinSetupState[]; // 1 (derange/single) or 2 (bracket)
  cameos: FillinAnswer[]; // imp punchlines, bracket only — never performed, never balloted
  panicOpen: PlayerId[]; // writers who pressed PANIC (options visible in their view only)
  writeDeadline: number; // view mirrors; authoritative deadlines are the SCHEDULE effects
  performDeadline: number;
  voteDeadline: number;
  assignments: Record<PlayerId, number> | null; // derange: performer -> answers index (never their own)
  readDone: PlayerId[]; // derange performers / face-off Readers who finished their read
  votes: Record<PlayerId, number>; // current stage: voter -> answers index (face-off: finalist slot 0|1). SECRET
  votedEver: PlayerId[]; // once-per-circle participation ledger
  readFires: Record<PlayerId, number>; // readerId -> fires landed during THEIR run (finale bonus fuel)
  resolution: FillinResolution | null;
}

// ===== views =====
export interface FillinCardFace {
  id: string;
  text: string;
}
export interface FillinWriteSetupView {
  card: FillinCardFace;
  readerId: PlayerId | null;
  toneChosen: boolean; // the pick itself stays private until the read
  submitted: number; // counts only — never who, never what
  writers: number;
}
export type FillinWriteView = {
  deck: 'fillin';
  sub: 'WRITE';
  mode: FillinMode;
  deadline: number;
  setups: FillinWriteSetupView[];
  you: {
    setup: number | null; // which setup you write for (null: Reader/imp at N<=6)
    isReader: boolean;
    yourAnswer: string | null; // your OWN text echo, nobody else's
    yourAnswerPanic: boolean;
    panicOptions: { a: string; b: string } | null; // only after YOU pressed PANIC
    yourTone: number | null;
    toneOptions: readonly string[];
  };
};
export type FillinPerformView = {
  deck: 'fillin';
  sub: 'PERFORM';
  mode: FillinMode;
  stage: FillinStage;
  card: FillinCardFace | null; // live setup's card (face-off: null — the lines carry it)
  readerId: PlayerId | null;
  tone: number | null; // public NOW: the room hears it read this way anyway
  progress: { pos: number; total: number }; // "answer 3 of 7" — all a non-Reader phone ever gets
  deadline: number;
  teleprompter: { text: string; canBurn: boolean } | null; // the LIVE Reader's phone ONLY
  assignment: { text: string; authorId: PlayerId; authorName: string } | null; // derange: your read, sell it
};
export interface FillinBallotEntry {
  id: number; // opaque ballot id (runOrder position / face-off slot) — maps to no author
  text: string;
  yours: boolean; // your own line, so the UI can dead the button
}
export type FillinVoteView = {
  deck: 'fillin';
  sub: 'VOTE';
  mode: FillinMode;
  stage: FillinStage;
  deadline: number;
  ballot: FillinBallotEntry[]; // full texts as memory-aid, order randomized PER VIEWER
  eligible: number;
  votedCount: number; // counts only — never who
  youVoted: number | null; // your own ballot id, nobody else's
};
export type FillinRevealView = {
  deck: 'fillin';
  sub: 'REVEAL';
  mode: FillinMode;
  voided: boolean;
  winner: { text: string; authorId: PlayerId; panic: boolean } | null;
  runnerUp: { text: string; authorId: PlayerId } | null;
  cameos: { text: string; authorId: PlayerId }[];
  burnedCount: number;
};
export type FillinView = FillinWriteView | FillinPerformView | FillinVoteView | FillinRevealView;

// ===== helpers =====
export const writeTimerId = (circleIdx: number): string => `fillin:write:${circleIdx}`;
export const performTimerId = (circleIdx: number, stage: FillinStage): string => `fillin:perform:${circleIdx}:${stage}`;
export const voteTimerId = (circleIdx: number, stage: FillinStage): string => `fillin:vote:${circleIdx}:${stage}`;

export function modeFor(nPlayers: number): FillinMode {
  if (nPlayers <= 3) return 'derange';
  return nPlayers >= BRACKET_MIN_N ? 'bracket' : 'single';
}

const votersOf = (ctx: GameCtx): Player[] => [...ctx.players, ...ctx.imps]; // imps are citizens: they vote (at half)
const weightOf = (p: Player): number => (p.role === 'imp' ? IMP_WEIGHT : 1);

function readState(ctx: GameCtx): FillinState | null {
  const gs = ctx.state.gameState as FillinState | null | undefined;
  return gs &&
    (gs.sub === 'DEAL' || gs.sub === 'WRITE' || gs.sub === 'PERFORM' || gs.sub === 'VOTE' || gs.sub === 'REVEAL')
    ? gs
    : null;
}

function noop(ctx: GameCtx): GameStep {
  return { gameState: ctx.state.gameState, effects: [] };
}

function shuffleWith<T>(rand: () => number, arr: readonly T[]): T[] {
  const out = [...arr];
  for (let i = out.length - 1; i > 0; i--) {
    const j = Math.floor(rand() * (i + 1));
    const a = out[i] as T;
    out[i] = out[j] as T;
    out[j] = a;
  }
  return out;
}

/** Live reader-run setup index for a stage; -1 when no single setup owns the stage. */
const setupIdxOf = (stage: FillinStage): number => (stage === 'B' ? 1 : stage === 'faceoff' ? -1 : 0);

const answerOf = (setup: FillinSetupState, author: PlayerId): FillinAnswer | undefined =>
  setup.answers.find((a) => a.author === author);

function upsertAnswer(setup: FillinSetupState, author: PlayerId, text: string, panic: boolean): FillinSetupState {
  const next: FillinAnswer = { author, text, panic, burned: false };
  const i = setup.answers.findIndex((a) => a.author === author);
  const answers = i >= 0 ? setup.answers.map((a, j) => (j === i ? next : a)) : [...setup.answers, next];
  return { ...setup, answers };
}

function panicText(card: FillinCard | null, opt: 'A' | 'B'): string {
  if (opt === 'A') return card?.panicA ?? PANIC_FALLBACKS[0];
  return card?.panicB ?? PANIC_FALLBACKS[1];
}

/** Fires landed in [from, to] — core telemetry, module-readable (finale Reader bonus). */
function firesBetween(ctx: GameCtx, from: number, to: number): number {
  let n = 0;
  for (const ev of ctx.state.telemetry) {
    if (ev.t === 'fires' && ev.at >= from && ev.at <= to) n += ev.fires ?? 0;
  }
  return n;
}

// ===== payload parsing =====
function rec(payload: unknown): Record<string, unknown> | null {
  return typeof payload === 'object' && payload !== null ? (payload as Record<string, unknown>) : null;
}
function parseAnswer(payload: unknown): string | null {
  const v = rec(payload)?.['answer'];
  if (typeof v !== 'string') return null;
  const text = v.trim();
  return text.length >= 1 && text.length <= MAX_ANSWER_CHARS ? text : null; // over-140 rejected, never truncated
}
function parseTone(payload: unknown): number | null {
  const v = rec(payload)?.['tone'];
  return typeof v === 'number' && Number.isInteger(v) && v >= 0 && v < READ_TONES.length ? v : null;
}
function parseTake(payload: unknown): 'A' | 'B' | null {
  const v = rec(payload)?.['take'];
  return v === 'A' || v === 'B' ? v : null;
}
function parseVote(payload: unknown): number | null {
  const v = rec(payload)?.['vote'];
  return typeof v === 'number' && Number.isInteger(v) && v >= 0 ? v : null;
}
const parseFlag = (payload: unknown, key: 'panic' | 'next' | 'burn' | 'read'): boolean =>
  rec(payload)?.[key] === true;

// ===== card + Reader selection =====
// Consent (4.4): setups are subjectless — the WRITERS aim them — so the generic
// ceiling applies. E-curve rung preference (4.3) and night-dedup ride on top.
function legalPool(ctx: GameCtx): FillinCard[] {
  const ceilings = ctx.players.map((p) => p.heatCeiling);
  let legal = deckCards.filter((c) => cardLegal(c.exposure, { ceilings }));
  if (legal.length === 0) {
    // Content bug (nothing legal): degrade to the mildest cards rather than deadlock.
    const minE = Math.min(...deckCards.map((c) => c.exposure));
    legal = deckCards.filter((c) => c.exposure === minE);
  }
  const underRung = legal.filter((c) => c.exposure <= ctx.circle.rung);
  return underRung.length > 0 ? underRung : legal;
}

/** Primary + reserved backup (4.5), night-deduped via usedCardIds (core writes back). */
function pickPair(ctx: GameCtx): { primary: FillinCard; backup: FillinCard } {
  const legal = legalPool(ctx);
  const fresh = legal.filter((c) => !ctx.state.usedCardIds.includes(c.id));
  const pool = fresh.length > 0 ? fresh : legal; // deck exhausted: a repeat beats a dead night
  const primary = pick(ctx.rand, pool);
  const rest = pool.filter((c) => c.id !== primary.id);
  const fallback = legal.filter((c) => c.id !== primary.id);
  const backup = rest.length > 0 ? pick(ctx.rand, rest) : fallback.length > 0 ? pick(ctx.rand, fallback) : primary;
  return { primary, backup };
}

/**
 * Reader selection (5.2): spotlight-rotated (lowest spotlightCount weighted first),
 * connected preferred. Finale law: never current last place, never the Bargain
 * holder — nobody watches the climax from the floor. Exclusions yield whenever
 * honoring them would leave no Reader at all.
 */
export function pickReaders(ctx: GameCtx, count: number): PlayerId[] {
  let candidates = [...ctx.players];
  if (ctx.circle.finale) {
    const minScore = Math.min(...candidates.map((p) => p.score));
    const notLast = candidates.filter((p) => p.score > minScore);
    if (notLast.length >= count) candidates = notLast;
    const bargainHolder = ctx.state.devilsBargain?.holder ?? null;
    if (bargainHolder !== null) {
      const noBargain = candidates.filter((p) => p.id !== bargainHolder);
      if (noBargain.length >= count) candidates = noBargain;
    }
  }
  const connected = candidates.filter((p) => p.connected);
  if (connected.length >= count) candidates = connected;
  const readers: PlayerId[] = [];
  let pool = candidates;
  for (let i = 0; i < count && pool.length > 0; i++) {
    const r = pickSpotlight(pool, ctx.rand);
    readers.push(r.id);
    pool = pool.filter((p) => p.id !== r.id);
  }
  return readers;
}

// ===== phase openers =====
function emptySetup(readerId: PlayerId | null, writerIds: PlayerId[]): FillinSetupState {
  return {
    card: null,
    readerId,
    writerIds,
    tone: null,
    answers: [],
    runOrder: [],
    runIdx: 0,
    runStartedAt: null,
    finalistIdx: null,
  };
}

/** Ceremony done: the setup is public. Open the 60s skippable WRITE. */
function openWrite(ctx: GameCtx, st: FillinState): GameStep {
  const deadline = ctx.now + WRITE_MS;
  const gameState: FillinState & { $phase: { k: 'INPUT'; sub: string; deadline: number } } = {
    ...st,
    sub: 'WRITE',
    writeDeadline: deadline,
    $phase: { k: 'INPUT', sub: 'WRITE', deadline },
  };
  return { gameState, effects: [{ k: 'SCHEDULE', timerId: writeTimerId(ctx.circleIdx), atMs: deadline }] };
}

const allWritten = (st: FillinState): boolean =>
  st.setups.every((s) => s.writerIds.every((w) => answerOf(s, w) !== undefined));
const allToned = (st: FillinState): boolean => st.setups.every((s) => s.readerId === null || s.tone !== null);

/** Everyone's in early — don't make the room stare at a countdown. */
function maybeCloseWrite(ctx: GameCtx, st: FillinState): GameStep {
  if (allWritten(st) && allToned(st)) {
    return openPerform(ctx, st, st.mode === 'bracket' ? 'A' : 'single', [
      { k: 'CANCEL', timerId: writeTimerId(ctx.circleIdx) },
    ]);
  }
  return { gameState: st, effects: [{ k: 'BROADCAST' }] };
}

/** WRITE deadline: silence auto-takes panic option A (5.2 acceptance); toneless Readers default to option 0. */
function autoPanicFill(st: FillinState): FillinState {
  const setups = st.setups.map((s) => {
    let next = s;
    for (const w of s.writerIds) {
      if (answerOf(next, w) === undefined) next = upsertAnswer(next, w, panicText(next.card, 'A'), true);
    }
    if (next.readerId !== null && next.tone === null) next = { ...next, tone: 0 };
    return next;
  });
  return { ...st, setups };
}

/**
 * N=3 derangement: performers in seat order, authors rotated by k in [1, n-1] —
 * a rotation of the same ordering is always a derangement (nobody reads their own).
 */
function buildAssignments(ctx: GameCtx, setup: FillinSetupState): Record<PlayerId, number> {
  const ring = [...ctx.players].sort((a, b) => a.seat - b.seat);
  const n = ring.length;
  const k = 1 + Math.floor(ctx.rand() * (n - 1));
  const assignments: Record<PlayerId, number> = {};
  for (let i = 0; i < n; i++) {
    const performer = ring[i] as Player;
    const author = ring[(i + k) % n] as Player;
    const idx = setup.answers.findIndex((a) => a.author === author.id);
    if (idx >= 0) assignments[performer.id] = idx;
  }
  return assignments;
}

function openPerform(ctx: GameCtx, st: FillinState, stage: FillinStage, pre: Effect[]): GameStep {
  const idx = setupIdxOf(stage);
  const setup = st.setups[idx];
  if (!setup || setup.card === null) return noop(ctx); // half-built loop: die silently
  let runOrder = shuffleWith(ctx.rand, setup.answers.map((_, i) => i));
  if (st.mode === 'bracket') runOrder = runOrder.slice(0, BRACKET_RUN_CAP); // <=4 answers per bracket run
  if (runOrder.length === 0) {
    // Nobody wrote anything and the card had no panic pool — a dead setup.
    if (st.mode === 'bracket') return advancePastStage(ctx, { ...st, stage }, pre);
    return resolveVoided(ctx, st, pre, 'void');
  }
  const deadline = ctx.now + PERFORM_CAP_MS;
  const setups = st.setups.map((s, i) => (i === idx ? { ...s, runOrder, runIdx: 0, runStartedAt: ctx.now } : s));
  const base: FillinState = { ...st, sub: 'PERFORM', stage, setups, performDeadline: deadline, readDone: [] };
  const assignments = st.mode === 'derange' ? buildAssignments(ctx, setups[idx] as FillinSetupState) : st.assignments;
  const gameState: FillinState & { $phase: { k: 'INPUT'; sub: string; deadline: number } } = {
    ...base,
    assignments,
    $phase: { k: 'INPUT', sub: 'PERFORM', deadline },
  };
  return {
    gameState,
    effects: [...pre, { k: 'SCHEDULE', timerId: performTimerId(ctx.circleIdx, stage), atMs: deadline }],
  };
}

/** The run is over: bank the Reader's fire window, open the 15s skippable ballot. */
function openVote(ctx: GameCtx, st: FillinState, pre: Effect[]): GameStep {
  let readFires = st.readFires;
  const idx = setupIdxOf(st.stage);
  const setup = idx >= 0 ? st.setups[idx] : undefined;
  if (setup?.readerId != null && setup.runStartedAt !== null) {
    const banked = readFires[setup.readerId] ?? 0;
    readFires = { ...readFires, [setup.readerId]: banked + firesBetween(ctx, setup.runStartedAt, ctx.now) };
  }
  const deadline = ctx.now + VOTE_MS;
  const gameState: FillinState & { $phase: { k: 'INPUT'; sub: string; deadline: number } } = {
    ...st,
    sub: 'VOTE',
    votes: {},
    voteDeadline: deadline,
    readFires,
    $phase: { k: 'INPUT', sub: 'VOTE', deadline },
  };
  return {
    gameState,
    effects: [...pre, { k: 'SCHEDULE', timerId: voteTimerId(ctx.circleIdx, st.stage), atMs: deadline }],
  };
}

// ===== ballots =====
interface BallotEntry {
  ballotId: number; // opaque wire id: runOrder position (or face-off slot)
  key: number; // stored vote value: answers index (or face-off slot)
  setup: number;
  author: PlayerId;
  text: string;
  panic: boolean;
}

/** The votable lines for the live stage. Burned and never-performed lines never reach a ballot. */
function ballotEntries(st: FillinState): BallotEntry[] {
  if (st.stage === 'faceoff') {
    const entries: BallotEntry[] = [];
    for (const slot of [0, 1]) {
      const setup = st.setups[slot];
      const fi = setup?.finalistIdx ?? null;
      const a = fi !== null ? setup?.answers[fi] : undefined;
      if (a && !a.burned) entries.push({ ballotId: slot, key: slot, setup: slot, author: a.author, text: a.text, panic: a.panic });
    }
    return entries;
  }
  const idx = setupIdxOf(st.stage);
  const setup = st.setups[idx];
  if (!setup) return [];
  const entries: BallotEntry[] = [];
  setup.runOrder.forEach((answersIdx, pos) => {
    const a = setup.answers[answersIdx];
    if (a && !a.burned) entries.push({ ballotId: pos, key: answersIdx, setup: idx, author: a.author, text: a.text, panic: a.panic });
  });
  return entries;
}

/** Voters with at least one line that isn't theirs — the only people a deadline waits for. */
function eligibleVoters(ctx: GameCtx, st: FillinState): Player[] {
  const entries = ballotEntries(st);
  return votersOf(ctx).filter((v) => entries.some((e) => e.author !== v.id));
}

function leaders(t: Map<number, number>): number[] {
  let max = 0;
  for (const v of t.values()) if (v > max) max = v;
  if (max <= 0) return [];
  return [...t.entries()]
    .filter(([, v]) => v === max)
    .map(([k]) => k)
    .sort((a, b) => a - b);
}

// One winner must emerge (4.8 imp clause, roast's shape bent to a single seat):
// imps count at half weight when no tie is in play; a tie among PLAYERS' full votes
// is never decided by imps — ctx.rand flips the coin instead (a committee would
// kill the reveal; the coin is at least dramatic).
function decideOne(votes: Record<PlayerId, number>, voters: Player[], rand: () => number): number | null {
  const weighted = new Map<number, number>();
  const full = new Map<number, number>();
  for (const voter of voters) {
    const key = votes[voter.id];
    if (key === undefined) continue;
    weighted.set(key, (weighted.get(key) ?? 0) + weightOf(voter));
    if (voter.role !== 'imp') full.set(key, (full.get(key) ?? 0) + 1);
  }
  const fl = leaders(full);
  const wl = leaders(weighted);
  if (fl.length === 1) return wl.length === 1 ? (wl[0] as number) : (fl[0] as number);
  if (fl.length >= 2) return fl[Math.floor(rand() * fl.length)] as number;
  if (wl.length >= 1) return wl.length === 1 ? (wl[0] as number) : (wl[Math.floor(rand() * wl.length)] as number);
  return null;
}

/** Weighted tally order (desc) for runner-up extraction. */
function weightedOrder(votes: Record<PlayerId, number>, voters: Player[]): number[] {
  const weighted = new Map<number, number>();
  for (const voter of voters) {
    const key = votes[voter.id];
    if (key === undefined) continue;
    weighted.set(key, (weighted.get(key) ?? 0) + weightOf(voter));
  }
  return [...weighted.entries()].sort((a, b) => b[1] - a[1] || a[0] - b[0]).map(([k]) => k);
}

// ===== resolution =====
interface AnswerRef {
  setup: number;
  idx: number;
}

function refAnswer(st: FillinState, ref: AnswerRef): FillinAnswer | null {
  return st.setups[ref.setup]?.answers[ref.idx] ?? null;
}

/** Loop dies scoreless (host VOID / Reader fled / dead content): 0 to all (4.6). */
function resolveVoided(ctx: GameCtx, st: FillinState, pre: Effect[], sting: 'void' | 'fled'): GameStep {
  const cancels: Effect[] =
    st.sub === 'WRITE'
      ? [{ k: 'CANCEL', timerId: writeTimerId(ctx.circleIdx) }]
      : st.sub === 'PERFORM'
        ? [{ k: 'CANCEL', timerId: performTimerId(ctx.circleIdx, st.stage) }]
        : st.sub === 'VOTE'
          ? [{ k: 'CANCEL', timerId: voteTimerId(ctx.circleIdx, st.stage) }]
          : [];
  const resolution: FillinResolution = {
    voided: true,
    winner: null,
    runnerUp: null,
    cameos: st.cameos.map((c) => ({ text: c.text, authorId: c.author })),
    burnedCount: st.setups.reduce((n, s) => n + s.answers.filter((a) => a.burned).length, 0),
  };
  const gameState: FillinState & { $phase: { k: 'REVEAL' } } = {
    ...st,
    sub: 'REVEAL',
    resolution,
    $phase: { k: 'REVEAL' },
  };
  return { gameState, effects: [...pre, ...cancels, { k: 'AUDIO', sting }, { k: 'SNAPSHOT' }], scores: {} };
}

// Scoring (pre-multiplier — core applies finale x3 / Bargain x2):
//   winner +3, or +1 when the winning line was a panic take (half, rounded down —
//   the machine wrote your joke, you split the check). Participation +1 once per
//   circle for any ballot cast. Finale Readers convert fires landed during THEIR
//   run: +1 per 10, cap +3 (readerFirePoints) — performing IS scoring in the climax.
function resolveFinal(ctx: GameCtx, st: FillinState, winner: AnswerRef | null, runnerUp: AnswerRef | null, pre: Effect[]): GameStep {
  const w = winner !== null ? refAnswer(st, winner) : null;
  const r = runnerUp !== null ? refAnswer(st, runnerUp) : null;
  const scores: Record<string, number> = {};
  const add = (id: PlayerId, pts: number): void => {
    if (pts > 0) scores[id] = (scores[id] ?? 0) + pts;
  };
  if (w) add(w.author, points(w.panic ? 'fillin.panicWin' : 'fillin.win'));
  for (const v of st.votedEver) add(v, points('participation'));
  if (ctx.circle.finale) {
    for (const setup of st.setups) {
      if (setup.readerId !== null) add(setup.readerId, readerFirePoints(st.readFires[setup.readerId] ?? 0));
    }
  }
  const resolution: FillinResolution = {
    voided: false,
    winner: w ? { text: w.text, authorId: w.author, panic: w.panic } : null,
    runnerUp: r ? { text: r.text, authorId: r.author } : null,
    cameos: st.cameos.map((c) => ({ text: c.text, authorId: c.author })),
    burnedCount: st.setups.reduce((n, s) => n + s.answers.filter((a) => a.burned).length, 0),
  };
  // $phase REVEAL: the core stamps holdSince and owns the hold — flip beat, DESCEND
  // softcap, fire-decay. The winner's name in lights is the core's 3-2-1 payload.
  const gameState: FillinState & { $phase: { k: 'REVEAL' } } = {
    ...st,
    sub: 'REVEAL',
    resolution,
    $phase: { k: 'REVEAL' },
  };
  return { gameState, effects: [...pre, { k: 'AUDIO', sting: 'boom' }, { k: 'SNAPSHOT' }], scores };
}

/** Bracket: stage A done -> Reader B's run; stage B done -> the grand face-off. */
function advancePastStage(ctx: GameCtx, st: FillinState, pre: Effect[]): GameStep {
  if (st.stage === 'A') return openPerform(ctx, st, 'B', pre);
  return openFaceoff(ctx, st, pre);
}

function openFaceoff(ctx: GameCtx, st: FillinState, pre: Effect[]): GameStep {
  const f0 = st.setups[0]?.finalistIdx ?? null;
  const f1 = st.setups[1]?.finalistIdx ?? null;
  if (f0 === null && f1 === null) return resolveVoided(ctx, st, pre, 'void'); // both setups died
  if (f0 === null || f1 === null) {
    // A one-legged face-off is a coronation: the surviving finalist takes it outright.
    const slot = f0 !== null ? 0 : 1;
    return resolveFinal(ctx, { ...st, stage: 'faceoff' }, { setup: slot, idx: (f0 ?? f1) as number }, null, pre);
  }
  const deadline = ctx.now + PERFORM_CAP_MS;
  const gameState: FillinState & { $phase: { k: 'INPUT'; sub: string; deadline: number } } = {
    ...st,
    sub: 'PERFORM',
    stage: 'faceoff',
    readDone: [],
    performDeadline: deadline,
    $phase: { k: 'INPUT', sub: 'PERFORM', deadline },
  };
  return {
    gameState,
    effects: [...pre, { k: 'SCHEDULE', timerId: performTimerId(ctx.circleIdx, 'faceoff'), atMs: deadline }],
  };
}

/** A stage ballot closed: crown a stage finalist (bracket) or settle the night (single/derange/face-off). */
function resolveVote(ctx: GameCtx, st: FillinState, pre: Effect[]): GameStep {
  const voters = votersOf(ctx);
  const entries = ballotEntries(st);
  let winnerKey = decideOne(st.votes, voters, ctx.rand);
  if (winnerKey === null && entries.length > 0 && (st.stage === 'A' || st.stage === 'B' || st.stage === 'faceoff')) {
    // A silent room still owes the bracket a finalist — the coin performs when nobody votes.
    winnerKey = (entries[Math.floor(ctx.rand() * entries.length)] as BallotEntry).key;
  }
  const votedEver = [...new Set([...st.votedEver, ...Object.keys(st.votes)])];
  const base: FillinState = { ...st, votedEver };

  if (st.stage === 'A' || st.stage === 'B') {
    const idx = setupIdxOf(st.stage);
    const setups = base.setups.map((s, i) => (i === idx ? { ...s, finalistIdx: winnerKey } : s));
    return advancePastStage(ctx, { ...base, setups }, pre);
  }
  if (st.stage === 'faceoff') {
    if (winnerKey === null) return resolveVoided(ctx, base, pre, 'void'); // unreachable: openFaceoff guarantees entries
    const loserSlot = winnerKey === 0 ? 1 : 0;
    const winIdx = base.setups[winnerKey]?.finalistIdx ?? null;
    const loseIdx = base.setups[loserSlot]?.finalistIdx ?? null;
    return resolveFinal(
      ctx,
      base,
      winIdx !== null ? { setup: winnerKey, idx: winIdx } : null,
      loseIdx !== null ? { setup: loserSlot, idx: loseIdx } : null,
      pre,
    );
  }
  // single / derange: winner + runner-up straight from the one ballot
  const order = weightedOrder(st.votes, voters);
  const runnerKey = order.find((k) => k !== winnerKey) ?? null;
  return resolveFinal(
    ctx,
    base,
    winnerKey !== null ? { setup: 0, idx: winnerKey } : null,
    runnerKey !== null ? { setup: 0, idx: runnerKey } : null,
    pre,
  );
}

// ===== the module =====
export const fillinModule = {
  deck: 'fillin',
  minN: 3,

  start(ctx: GameCtx): GameStep {
    const n = ctx.players.length;
    const mode = modeFor(n);
    const readers = mode === 'derange' ? [] : pickReaders(ctx, mode === 'bracket' ? 2 : 1);
    const nonReaders = ctx.players.filter((p) => !readers.includes(p.id)).sort((a, b) => a.seat - b.seat);
    const setups: FillinSetupState[] =
      mode === 'bracket'
        ? [0, 1].map((i) =>
            emptySetup(readers[i] ?? null, nonReaders.filter((_, j) => j % 2 === i).map((p) => p.id)),
          )
        : [emptySetup(readers[0] ?? null, nonReaders.map((p) => p.id))];
    const { primary, backup } = pickPair(ctx);
    const gameState: FillinState & { $deal: DealRequest } = {
      sub: 'DEAL',
      mode,
      stage: mode === 'bracket' ? 'A' : 'single',
      setups,
      cameos: [],
      panicOpen: [],
      writeDeadline: 0,
      performDeadline: 0,
      voteDeadline: 0,
      assignments: null,
      readDone: [],
      votes: {},
      votedEver: [],
      readFires: {},
      resolution: null,
      $deal: { primary, backup, subjectId: null }, // subjectless: setups aim at nobody until the writers aim them
    };
    return { gameState, effects: [] };
  },

  input(ctx: GameCtx, playerId: string, payload: unknown): GameStep {
    const st = readState(ctx);
    if (!st) return noop(ctx);

    if (st.sub === 'WRITE') {
      const imp = ctx.imps.find((p) => p.id === playerId);
      if (imp) {
        // Imp cameo (4.8): one curated punchline at N>=7 — screen cameo, never performed.
        if (st.mode !== 'bracket') return noop(ctx);
        const text = parseAnswer(payload);
        if (text === null) return noop(ctx);
        const i = st.cameos.findIndex((c) => c.author === playerId);
        const cameo: FillinAnswer = { author: playerId, text, panic: false, burned: false };
        const cameos = i >= 0 ? st.cameos.map((c, j) => (j === i ? cameo : c)) : [...st.cameos, cameo];
        return { gameState: { ...st, cameos }, effects: [{ k: 'BROADCAST' }] };
      }
      const readerSetupIdx = st.setups.findIndex((s) => s.readerId === playerId);
      if (readerSetupIdx >= 0) {
        // The Reader's only WRITE-phase job: pick how this gets read.
        const tone = parseTone(payload);
        if (tone === null) return noop(ctx);
        const setups = st.setups.map((s, i) => (i === readerSetupIdx ? { ...s, tone } : s));
        return maybeCloseWrite(ctx, { ...st, setups });
      }
      const writerSetupIdx = st.setups.findIndex((s) => s.writerIds.includes(playerId));
      if (writerSetupIdx < 0) return noop(ctx);
      const setup = st.setups[writerSetupIdx] as FillinSetupState;
      const text = parseAnswer(payload);
      if (text !== null) {
        // Re-submit allowed until the deadline: last text wins, and it clears any panic flag.
        const setups = st.setups.map((s, i) => (i === writerSetupIdx ? upsertAnswer(s, playerId, text, false) : s));
        return maybeCloseWrite(ctx, { ...st, setups });
      }
      if (parseFlag(payload, 'panic')) {
        // PANIC: the two curated fallbacks unlock — in THIS writer's view only.
        if (st.panicOpen.includes(playerId)) return noop(ctx);
        return { gameState: { ...st, panicOpen: [...st.panicOpen, playerId] }, effects: [{ k: 'BROADCAST' }] };
      }
      const take = parseTake(payload);
      if (take !== null) {
        if (!st.panicOpen.includes(playerId)) return noop(ctx); // no blind takes: PANIC first
        const setups = st.setups.map((s, i) =>
          i === writerSetupIdx ? upsertAnswer(s, playerId, panicText(setup.card, take), true) : s,
        );
        return maybeCloseWrite(ctx, { ...st, setups });
      }
      return noop(ctx);
    }

    if (st.sub === 'PERFORM') {
      if (st.stage === 'faceoff') {
        // Both Readers perform their finalist; a read-done tap each, then the final ballot.
        if (!parseFlag(payload, 'read')) return noop(ctx);
        const isReader = st.setups.some((s) => s.readerId === playerId);
        if (!isReader || st.readDone.includes(playerId)) return noop(ctx);
        const readDone = [...st.readDone, playerId];
        if (st.setups.every((s) => s.readerId === null || readDone.includes(s.readerId))) {
          return openVote(ctx, { ...st, readDone }, [{ k: 'CANCEL', timerId: performTimerId(ctx.circleIdx, 'faceoff') }]);
        }
        return { gameState: { ...st, readDone }, effects: [{ k: 'BROADCAST' }] };
      }
      if (st.mode === 'derange') {
        // Every player is a performer; the ballot opens when the last one sells it.
        if (!parseFlag(payload, 'read')) return noop(ctx);
        if (st.assignments?.[playerId] === undefined || st.readDone.includes(playerId)) return noop(ctx);
        const readDone = [...st.readDone, playerId];
        if (Object.keys(st.assignments).every((id) => readDone.includes(id))) {
          return openVote(ctx, { ...st, readDone }, [{ k: 'CANCEL', timerId: performTimerId(ctx.circleIdx, st.stage) }]);
        }
        return { gameState: { ...st, readDone }, effects: [{ k: 'BROADCAST' }] };
      }
      // Reader-paced teleprompter: [NEXT] advances, [BURN LINE] strikes then advances.
      const idx = setupIdxOf(st.stage);
      const setup = st.setups[idx];
      if (!setup || playerId !== setup.readerId) return noop(ctx);
      const burn = parseFlag(payload, 'burn');
      if (!burn && !parseFlag(payload, 'next')) return noop(ctx);
      const effects: Effect[] = [];
      let setups = st.setups;
      if (burn) {
        const aIdx = setup.runOrder[setup.runIdx];
        if (aIdx === undefined) return noop(ctx);
        setups = setups.map((s, i) =>
          i === idx ? { ...s, answers: s.answers.map((a, j) => (j === aIdx ? { ...a, burned: true } : a)) } : s,
        );
        effects.push({ k: 'AUDIO', sting: 'burn' }); // the UGC hook lands loud
      }
      if (setup.runIdx + 1 < setup.runOrder.length) {
        setups = setups.map((s, i) => (i === idx ? { ...s, runIdx: s.runIdx + 1 } : s));
        return { gameState: { ...st, setups }, effects: [...effects, { k: 'BROADCAST' }] };
      }
      // Last line read: the run is over, the ballot unlocks.
      return openVote(ctx, { ...st, setups }, [
        ...effects,
        { k: 'CANCEL', timerId: performTimerId(ctx.circleIdx, st.stage) },
      ]);
    }

    if (st.sub === 'VOTE') {
      const ballotId = parseVote(payload);
      if (ballotId === null) return noop(ctx);
      const voter = votersOf(ctx).find((p) => p.id === playerId);
      if (!voter) return noop(ctx);
      const entry = ballotEntries(st).find((e) => e.ballotId === ballotId);
      if (!entry || entry.author === playerId) return noop(ctx); // your own line is never votable
      const votes = { ...st.votes, [playerId]: entry.key }; // re-vote allowed until deadline: last ballot wins
      const next: FillinState = { ...st, votes };
      if (Object.keys(votes).length >= eligibleVoters(ctx, st).length) {
        return resolveVote(ctx, next, [{ k: 'CANCEL', timerId: voteTimerId(ctx.circleIdx, st.stage) }]);
      }
      return { gameState: next, effects: [{ k: 'BROADCAST' }] }; // count ticks up; view() hides who
    }

    return noop(ctx); // DEAL / REVEAL: nothing to say
  },

  timer(ctx: GameCtx, timerId: string): GameStep {
    const st = readState(ctx);
    if (!st) return noop(ctx);
    if (st.sub === 'DEAL' && timerId === CORE_DEALT) {
      const dealt = ctx.state.deal?.done === true ? ctx.state.deal.card : null;
      if (dealt === null || dealt.deck !== 'fillin') return noop(ctx); // stale CORE_DEALT
      const idx = st.setups.findIndex((s) => s.card === null);
      if (idx < 0) return noop(ctx);
      const setups = st.setups.map((s, i) => (i === idx ? { ...s, card: dealt as FillinCard } : s));
      const next: FillinState = { ...st, setups };
      if (setups.some((s) => s.card === null)) {
        // Bracket: setup B gets its own honest ceremony (and its own dedup writeback —
        // usedCardIds already carries card A, so the pair pick can't collide).
        const { primary, backup } = pickPair(ctx);
        const gameState: FillinState & { $deal: DealRequest } = { ...next, $deal: { primary, backup, subjectId: null } };
        return { gameState, effects: [] };
      }
      return openWrite(ctx, next);
    }
    if (st.sub === 'WRITE' && timerId === writeTimerId(ctx.circleIdx)) {
      // Deadline: silence auto-takes panic option A; a toneless Reader reads it as a eulogy.
      const filled = autoPanicFill(st);
      return openPerform(ctx, filled, filled.mode === 'bracket' ? 'A' : 'single', []);
    }
    if (st.sub === 'PERFORM' && timerId === performTimerId(ctx.circleIdx, st.stage)) {
      if (st.stage === 'faceoff' || st.mode === 'derange') {
        return openVote(ctx, st, []); // cap: whoever hasn't finished reading is finished anyway
      }
      // Reader-paced cap: lines past the cursor were never spoken aloud — first exposure
      // is ALWAYS out loud, so the un-performed never reach a ballot. Truncate the run.
      const idx = setupIdxOf(st.stage);
      const setup = st.setups[idx];
      if (!setup) return noop(ctx);
      const setups = st.setups.map((s, i) => (i === idx ? { ...s, runOrder: s.runOrder.slice(0, s.runIdx + 1) } : s));
      return openVote(ctx, { ...st, setups }, []);
    }
    if (st.sub === 'VOTE' && timerId === voteTimerId(ctx.circleIdx, st.stage)) {
      return resolveVote(ctx, st, []); // deadline: non-voters auto-abstain — the game never waits
    }
    if (st.sub === 'REVEAL' && timerId === CORE_REVEAL_DONE) {
      return { gameState: st, effects: [], done: true }; // one flow per circle -> core LADDER
    }
    return noop(ctx); // stale timer for a dead sub-phase
  },

  control(ctx: GameCtx, playerId: string, kind: 'REST' | 'SKIPEM' | 'FIFTH' | 'VOID'): GameStep {
    const st = readState(ctx);
    if (!st) return noop(ctx);
    if (kind !== 'VOID') return noop(ctx); // no performer clock, no blocking input: REST/SKIPEM/FIFTH are inert

    const live = st.sub === 'WRITE' || st.sub === 'PERFORM' || st.sub === 'VOTE';
    const host = ctx.players.find((p) => p.id === playerId && p.role === 'host');
    if (host) {
      // Host kill-switch (4.7): the loop dies scoreless at any live sub-phase.
      return live ? resolveVoided(ctx, st, [], 'void') : noop(ctx); // DEAL rides out; REVEAL already settled
    }
    // Seat-lapse convention (engine 4.7): VOID carrying a NON-host id = that seat lapsed.
    if (!live) return noop(ctx);
    if (st.setups.some((s) => s.readerId === playerId)) {
      // A Reader fled. Nobody else may see the teleprompter (any substitute is an
      // author — handing them the pile pre-vote breaks the redaction law), so the
      // loop dies loud instead: THE WITNESS FLED.
      return resolveVoided(ctx, st, [], 'fled');
    }
    if (st.sub === 'PERFORM' && st.mode === 'derange' && st.assignments?.[playerId] !== undefined && !st.readDone.includes(playerId)) {
      // A derange performer fled mid-read: their line still stands for the vote —
      // the author wrote it, the room half-heard it, the ballot shows it.
      const readDone = [...st.readDone, playerId];
      if (Object.keys(st.assignments).every((id) => readDone.includes(id))) {
        return openVote(ctx, { ...st, readDone }, [{ k: 'CANCEL', timerId: performTimerId(ctx.circleIdx, st.stage) }]);
      }
      return { gameState: { ...st, readDone }, effects: [{ k: 'BROADCAST' }] };
    }
    return noop(ctx); // a lapsed writer auto-panics at the deadline; a lapsed voter abstains
  },

  // The ONLY serialization surface. Redaction law (3.4 + 5.2):
  // - during the DEAL ceremony NOTHING serializes;
  // - WRITE: your own text echo, your own panic options (only after YOU pressed
  //   PANIC), your own tone — everyone else gets counts;
  // - PERFORM: answer text exists in exactly ONE frame — the live Reader's
  //   teleprompter (derange: each performer's own assignment). Every other phone
  //   gets "answer 3 of 7";
  // - VOTE: the ballot is the first on-screen exposure (texts, authorless, order
  //   randomized PER VIEWER via a viewer-seeded shuffle); you see your own ballot
  //   and the counts, never anyone else's;
  // - REVEAL: winner + author + runner-up — authorship exists on the wire HERE
  //   and nowhere earlier; per-loop score deltas never serialize (a +3 would out
  //   the tally before the flip beat).
  view(ctx: GameCtx, viewerId: string): FillinView | null {
    const st = readState(ctx);
    if (!st || st.sub === 'DEAL') return null;

    if (st.sub === 'WRITE') {
      const setupViews: FillinWriteSetupView[] = [];
      for (const s of st.setups) {
        if (s.card === null) return null; // half-built loop: never leak on a bug
        setupViews.push({
          card: { id: s.card.id, text: s.card.text },
          readerId: s.readerId,
          toneChosen: s.tone !== null,
          submitted: s.answers.length,
          writers: s.writerIds.length,
        });
      }
      const mySetupIdx = st.setups.findIndex((s) => s.writerIds.includes(viewerId));
      const myReaderIdx = st.setups.findIndex((s) => s.readerId === viewerId);
      const mySetup = mySetupIdx >= 0 ? st.setups[mySetupIdx] : undefined;
      const myAnswer =
        mySetup !== undefined
          ? answerOf(mySetup, viewerId)
          : st.cameos.find((c) => c.author === viewerId); // imps see their own cameo echo
      const panicOptions =
        mySetup !== undefined && st.panicOpen.includes(viewerId)
          ? { a: panicText(mySetup.card, 'A'), b: panicText(mySetup.card, 'B') }
          : null;
      return {
        deck: 'fillin',
        sub: 'WRITE',
        mode: st.mode,
        deadline: st.writeDeadline,
        setups: setupViews,
        you: {
          setup: mySetupIdx >= 0 ? mySetupIdx : null,
          isReader: myReaderIdx >= 0,
          yourAnswer: myAnswer?.text ?? null,
          yourAnswerPanic: myAnswer?.panic ?? false,
          panicOptions,
          yourTone: myReaderIdx >= 0 ? (st.setups[myReaderIdx]?.tone ?? null) : null,
          toneOptions: READ_TONES,
        },
      };
    }

    if (st.sub === 'PERFORM') {
      if (st.stage === 'faceoff') {
        const mySlot = st.setups.findIndex((s) => s.readerId === viewerId);
        const myFinalist =
          mySlot >= 0 && st.setups[mySlot]?.finalistIdx != null
            ? (st.setups[mySlot]?.answers[st.setups[mySlot]?.finalistIdx as number] ?? null)
            : null;
        return {
          deck: 'fillin',
          sub: 'PERFORM',
          mode: st.mode,
          stage: 'faceoff',
          card: null,
          readerId: null,
          tone: null,
          progress: { pos: st.readDone.length, total: st.setups.filter((s) => s.readerId !== null).length },
          deadline: st.performDeadline,
          teleprompter: myFinalist && !st.readDone.includes(viewerId) ? { text: myFinalist.text, canBurn: false } : null,
          assignment: null,
        };
      }
      const idx = setupIdxOf(st.stage);
      const setup = st.setups[idx];
      if (!setup || setup.card === null) return null;
      const face: FillinCardFace = { id: setup.card.id, text: setup.card.text };
      if (st.mode === 'derange') {
        const myIdx = st.assignments?.[viewerId];
        const mine = myIdx !== undefined ? setup.answers[myIdx] : undefined;
        const author = mine ? ctx.players.find((p) => p.id === mine.author) : undefined;
        return {
          deck: 'fillin',
          sub: 'PERFORM',
          mode: 'derange',
          stage: st.stage,
          card: face,
          readerId: null,
          tone: null,
          progress: { pos: st.readDone.length, total: Object.keys(st.assignments ?? {}).length },
          deadline: st.performDeadline,
          teleprompter: null,
          assignment:
            mine && author ? { text: mine.text, authorId: author.id, authorName: author.name } : null, // "you're performing {NAME}'s filth — sell it"
        };
      }
      const current = setup.answers[setup.runOrder[setup.runIdx] ?? -1];
      return {
        deck: 'fillin',
        sub: 'PERFORM',
        mode: st.mode,
        stage: st.stage,
        card: face,
        readerId: setup.readerId,
        tone: setup.tone,
        progress: { pos: setup.runIdx + 1, total: setup.runOrder.length }, // "answer 3 of 7"
        deadline: st.performDeadline,
        teleprompter:
          viewerId === setup.readerId && current !== undefined ? { text: current.text, canBurn: true } : null,
        assignment: null,
      };
    }

    if (st.sub === 'VOTE') {
      const entries = ballotEntries(st);
      // Randomized PER VIEWER: a viewer-seeded shuffle — deterministic for the same
      // socket (re-renders stable), different across phones (no shoulder-surf sync).
      const order = shuffleWith(
        rng(`fillin:${ctx.circleIdx}:${st.stage}:${viewerId}`),
        entries,
      );
      const myKey = st.votes[viewerId];
      const youVoted = myKey !== undefined ? (entries.find((e) => e.key === myKey)?.ballotId ?? null) : null;
      return {
        deck: 'fillin',
        sub: 'VOTE',
        mode: st.mode,
        stage: st.stage,
        deadline: st.voteDeadline,
        ballot: order.map((e) => ({ id: e.ballotId, text: e.text, yours: e.author === viewerId })),
        eligible: eligibleVoters(ctx, st).length,
        votedCount: Object.keys(st.votes).length,
        youVoted,
      };
    }

    const res = st.resolution;
    if (!res) return null; // unreachable by construction; never leak on a bug
    return {
      deck: 'fillin',
      sub: 'REVEAL',
      mode: st.mode,
      voided: res.voided,
      winner: res.winner,
      runnerUp: res.runnerUp,
      cameos: res.cameos,
      burnedCount: res.burnedCount,
    };
  },
} satisfies GameModule;
