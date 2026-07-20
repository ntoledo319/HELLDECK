// ============================================================================
// THE SEAM — client wire contracts for Fill-In (5.2), Over/Under (5.3),
// Confession or Cap (5.4). Tasks D-121/D-122/D-123, client half.
//
// Contract status per deck (update as engine modules land):
//   overunder  — ALIGNED to the landed engine module games/overunder.ts:
//                subs DEBATE/BET/TRUTH/REVEAL, payload keys {line,lock} {bet}
//                {truth} {claim} {pit}. Field-for-field mirror of its view().
//   confession — ALIGNED to the landed engine module games/confession.ts:
//                subs PICK/LOCK/PERFORM/JURY/REVEAL, payload keys {pick:index}
//                {truth:boolean} {vote:'believe'|'cap'} {pit}. PICK is anonymous
//                for non-confessors (no confessorId in their frames).
//   fillin     — ALIGNED to the landed engine module games/fillin.ts:
//                subs WRITE/PERFORM/VOTE/REVEAL, payload keys {answer} {panic}
//                {take} {tone} {next} {burn} {read} {vote}. Field-for-field
//                mirror of its view(); the ballot order is randomized PER VIEWER
//                server-side (view() seeds the shuffle per socket).
//
// Every view object carries `deck` + `sub` (roast.ts discriminant style).
// Redaction law lives server-side in view(); fields below marked "viewer-only"
// are per-socket. Server redact.ts bans the quoted keys "votes"/"participation"
// from any frame — module views must not name fields that exactly.
//
// Client -> server INPUT payloads (the `p` of {t:'INPUT', p}) are built ONLY via
// the *Payload builders at the bottom so the contract has one address.
// Controls ride the spec 3.2 wire verbs, not INPUT: {t:'FIFTH'} {t:'REST'} {t:'VOID'}.
// ============================================================================

// ===== shared =====
export interface SetupRef {
  id: string;
  text: string;
}

/** Blocking-terminal mirror (spec 4.7): pit-vote state rides the module view.
 * Field names match the engine's blocking helpers (overunder.ts sets the style). */
export interface PitMirror {
  pitOpen: boolean; // t+30s: the room gets DRAG THEM BACK / FEED THEM TO THE PIT
  youPitVoted: 'drag' | 'pit' | null; // your own pit ballot, nobody else's
}

/** Deck+sub runtime guard — asView() checks sub only, which collides across games
 * (roast VOTE vs fillin VOTE, four REVEALs). Trust nothing without both. */
export function asDeckView<T extends { deck: string; sub: string }>(
  gv: unknown,
  deck: T['deck'],
  sub: T['sub'],
): T | null {
  if (gv === null || typeof gv !== 'object') return null;
  const g = gv as { deck?: unknown; sub?: unknown };
  return g.deck === deck && g.sub === sub ? (gv as T) : null;
}

export function deckOf(gv: unknown): string | null {
  if (gv === null || typeof gv !== 'object') return null;
  const d = (gv as { deck?: unknown }).deck;
  return typeof d === 'string' ? d : null;
}

export function subOf(gv: unknown): string | null {
  if (gv === null || typeof gv !== 'object') return null;
  const s = (gv as { sub?: unknown }).sub;
  return typeof s === 'string' ? s : null;
}

// ============================================================================
// FILL-IN FINISHER — spec 5.2, ALIGNED to engine games/fillin.ts view() verbatim.
// Subs: WRITE -> PERFORM -> VOTE -> REVEAL. Modes: derange (N=3, no Reader —
// everyone performs ANOTHER player's line) / single (N=4-6, one Reader) /
// bracket (N>=7, two setups, two Readers, one grand face-off).
// Acceptance law the shapes enforce: answer text NEVER reaches a non-performing
// phone before VOTE — PERFORM carries `teleprompter` (the LIVE performer's phone
// only) or `assignment` (a derange performer's own line) and nulls both for
// everyone else. Authorship exists on the wire only at REVEAL.
// ============================================================================

/** Mirror of engine games/fillin.ts READ_TONES — the Reader picks by INDEX (0..3). */
export const FILLIN_READ_TONES = ['a eulogy', 'a 911 call', 'a hostage tape', 'an apology video'] as const;

export type FillinMode = 'derange' | 'single' | 'bracket';
/** Which perform/vote is live. 'single' covers N<=6 (derangement included). */
export type FillinStage = 'single' | 'A' | 'B' | 'faceoff';

export interface FillinCardFace {
  id: string;
  text: string;
}

export interface FillinWriteSetup {
  card: FillinCardFace;
  readerId: string | null; // null in derangement mode
  toneChosen: boolean; // the pick itself stays private until the read
  submitted: number; // counts only — never who, never what
  writers: number;
}

export interface FillinWriteView {
  deck: 'fillin';
  sub: 'WRITE';
  mode: FillinMode;
  deadline: number; // 60s skippable; empty write auto-takes panic option A (engine)
  setups: FillinWriteSetup[]; // 1 (derange/single) or 2 (bracket)
  you: {
    setup: number | null; // which setup you write for (null: Reader/imp at N<=6)
    isReader: boolean;
    yourAnswer: string | null; // your OWN text echo, nobody else's
    yourAnswerPanic: boolean; // your current line is a shelf take (half points)
    panicOptions: { a: string; b: string } | null; // only after YOU pressed PANIC
    yourTone: number | null; // Reader's chosen tone index (into toneOptions)
    toneOptions: readonly string[];
  };
}

export interface FillinPerformView {
  deck: 'fillin';
  sub: 'PERFORM';
  mode: FillinMode;
  stage: FillinStage;
  card: FillinCardFace | null; // live setup's card (face-off: null — the lines carry it)
  readerId: string | null;
  tone: number | null; // index into FILLIN_READ_TONES; public NOW — the room hears it anyway
  progress: { pos: number; total: number }; // reader-paced: 1-based line cursor; derange/face-off: reads-done
  deadline: number; // 90s performance cap
  teleprompter: { text: string; canBurn: boolean } | null; // the LIVE performer's phone ONLY
  assignment: { text: string; authorId: string; authorName: string } | null; // derange: your read, sell it
}

export interface FillinBallotEntry {
  id: number; // opaque ballot id (runOrder position / face-off slot) — maps to no author
  text: string;
  yours: boolean; // your own line, so the UI can dead the button
}

export interface FillinVoteView {
  deck: 'fillin';
  sub: 'VOTE';
  mode: FillinMode;
  stage: FillinStage;
  deadline: number; // 15s skippable
  ballot: FillinBallotEntry[]; // full texts as memory-aid, order randomized PER VIEWER server-side
  eligible: number;
  votedCount: number; // counts only — never who
  youVoted: number | null; // your own ballot id, nobody else's
}

export interface FillinRevealView {
  deck: 'fillin';
  sub: 'REVEAL';
  mode: FillinMode;
  voided: boolean;
  winner: { text: string; authorId: string; panic: boolean } | null; // null = voided/no votes
  runnerUp: { text: string; authorId: string } | null;
  cameos: { text: string; authorId: string }[]; // imp screen cameos (N>=7) — never performed, never balloted
  burnedCount: number; // lines struck live on the teleprompter
}

export type FillinView = FillinWriteView | FillinPerformView | FillinVoteView | FillinRevealView;

// ============================================================================
// OVER/UNDER — spec 5.3, ALIGNED to engine games/overunder.ts view() verbatim.
// Subs: DEBATE -> BET -> TRUTH (blocking) -> REVEAL. The DEAL + subject
// pre-view interstitial is the CORE 4.5 ceremony (PreviewOverlay renders the
// PRIVATE preview) — view() returns null during it; no module sub exists.
// TRUTH is a blocking input (4.7): paused deadline, WAITING_ON, pit vote,
// FIFTH, host VOID, scribe-downgrade. The truth number appears NOWHERE
// on the wire before REVEAL.
// ============================================================================
export interface OverUnderCardFace {
  id: string;
  text: string;
  receiptSurface: string; // "Settings → Screen Time" — where the subject digs
  timebox: string;
}

interface OverUnderViewBase {
  deck: 'overunder';
  loop: number; // 0-based subject index (2 subjects/circle)
  loops: number;
  subjectId: string; // public from CORE_DEALT on
  scribeId: string | null; // next seat after subject; engine reassigns on scribe lapse
}

export interface OverUnderDebateView extends OverUnderViewBase {
  sub: 'DEBATE';
  card: OverUnderCardFace;
  line: number | null; // live dial value, public — the argument is out loud anyway
  deadline: number; // 25s; deadline locks the dial where the argument left it
  youAreScribe: boolean; // only the scribe's phone renders the line-setter dial
}

export interface OverUnderBetView extends OverUnderViewBase {
  sub: 'BET';
  card: OverUnderCardFace;
  line: number; // locked
  deadline: number; // 12s skippable -> auto-abstain
  eligible: number; // everyone except the subject (imps included)
  betCount: number; // counts only — never who
  youBet: 'over' | 'under' | null; // viewer-only
  youAreSubject: boolean; // subject's phone shows "fetch your receipt" instead of buttons
}

export interface OverUnderTruthView extends OverUnderViewBase, PitMirror {
  sub: 'TRUTH';
  card: OverUnderCardFace;
  line: number;
  waitingOn: string; // the subject
  youBet: 'over' | 'under' | null; // viewer-only
  youAreSubject: boolean;
}

export interface OverUnderRevealView extends OverUnderViewBase {
  sub: 'REVEAL';
  card: OverUnderCardFace | null; // null only when the loop died during the ceremony
  line: number | null;
  truth: number | null; // exists on the wire HERE and nowhere earlier. null = voided — NEVER fabricated (D-115)
  push: boolean; // truth == line: everyone +1, subject roasted for being median
  unverified: boolean; // scribe-downgrade: host relayed the verbal claim, no receipt bonus
  voided: boolean; // fifth / pit / host void / seat lapse — the loop died scoreless
  winners: string[]; // correct bettors, public at reveal
  split: { over: number; under: number }; // weighted spread (imps 0.5) — informational
  youBet: 'over' | 'under' | null; // viewer-only
}

export type OverUnderView = OverUnderDebateView | OverUnderBetView | OverUnderTruthView | OverUnderRevealView;

// ============================================================================
// CONFESSION OR CAP — spec 5.4, ALIGNED to engine games/confession.ts view()
// verbatim. Subs: PICK -> LOCK (blocking) -> PERFORM -> JURY -> REVEAL.
// The truth-lock value exists ONLY in the REVEAL view — not even the confessor's
// own frames carry it earlier (face-up phones must never spoil the stamp). The
// two unchosen sins exist ONLY on the confessor's PICK view. PICK frames for
// everyone else carry no confessorId — the room doesn't learn who is choosing.
// ============================================================================
export interface ConfessionCardFace {
  id: string;
  text: string;
}

export type ConfessionVerdict = 'FOOLED' | 'CAUGHT' | 'HUNG';

interface ConfessionViewBase {
  deck: 'confession';
  loop: number; // 0-based confessor index (2/circle)
  loops: number;
}

export interface ConfessionPickView extends ConfessionViewBase {
  sub: 'PICK';
  deadline: number; // 20s — timeout auto-picks card #1 (engine; skippable, not blocking)
  youAreConfessor: boolean;
  hand?: ConfessionCardFace[]; // viewer-only: the 3 sins, confessor's phone ONLY
}

export interface ConfessionLockView extends ConfessionViewBase, PitMirror {
  sub: 'LOCK';
  confessorId: string; // public from CORE_DEALT on
  card: ConfessionCardFace; // public — the ceremony announced the chosen sin
  youAreConfessor: boolean; // your phone renders TRUE/FALSE + [FIFTH]; others wait
}

export interface ConfessionPerformView extends ConfessionViewBase {
  sub: 'PERFORM';
  confessorId: string;
  card: ConfessionCardFace; // the room reads along and watches their hands
  deadline: number; // 45s; [REST] ends it early
  youAreConfessor: boolean;
}

export interface ConfessionJuryView extends ConfessionViewBase {
  sub: 'JURY';
  confessorId: string;
  card: ConfessionCardFace;
  deadline: number; // 12s skippable
  eligible: number;
  votedCount: number; // counts only — never who
  youVoted: 'believe' | 'cap' | null; // viewer-only
  youAreConfessor: boolean; // "they're deciding your fate"
}

export interface ConfessionRevealView extends ConfessionViewBase {
  sub: 'REVEAL';
  confessorId: string;
  card: ConfessionCardFace | null; // null only when the loop died before a sin was chosen
  truth: boolean | null; // the lock, revealed HERE and nowhere earlier. null = fifth/voided
  verdict: ConfessionVerdict | null; // HUNG = tie, the liar walks (confessor wins). null = voided
  spread: { believe: number; cap: number }; // weighted jury counts (imps 0.5)
  voided: boolean;
  youVoted: 'believe' | 'cap' | null; // viewer-only
}

export type ConfessionView =
  | ConfessionPickView
  | ConfessionLockView
  | ConfessionPerformView
  | ConfessionJuryView
  | ConfessionRevealView;

// ============================================================================
// INPUT payload builders — the client half of each module's input() parser.
// Every key here is LAW: the engine parsers read exactly these strings, and a
// renamed key silently no-ops the input (overunder parseLine/parseBet/parseCount/
// parsePit; fillin parseAnswer/parseTone/parseTake/parseVote + panic/next/burn/read
// flags; confession parsePick/parseTruth/parseJuryVote/parsePit).
// ============================================================================
export const fillinPayload = {
  /** WRITE (writer): your line (1..140 chars, engine re-validates). Last submission wins until deadline. */
  answer: (text: string): Record<string, unknown> => ({ answer: text }),
  /** WRITE (writer): open the panic shelf — your two curated fallbacks unlock in your NEXT view. */
  panicOpen: (): Record<string, unknown> => ({ panic: true }),
  /** WRITE (writer): grab shelf line A or B (PANIC first) — engine flags it for half points. */
  panicTake: (opt: 'A' | 'B'): Record<string, unknown> => ({ take: opt }),
  /** WRITE (Reader): pick the read tone by INDEX into FILLIN_READ_TONES (0..3). */
  tone: (i: number): Record<string, unknown> => ({ tone: i }),
  /** PERFORM (single-mode Reader): advance the teleprompter. */
  next: (): Record<string, unknown> => ({ next: true }),
  /** PERFORM (single-mode Reader): UGC strike — the current line is struck, never balloted (spec 10.3). */
  burn: (): Record<string, unknown> => ({ burn: true }),
  /** PERFORM (derange performer / face-off Reader): signal you finished reading it aloud. */
  read: (): Record<string, unknown> => ({ read: true }),
  /** VOTE: the ballot entry's numeric id. Own-line ids are rejected engine-side too. */
  vote: (id: number): Record<string, unknown> => ({ vote: id }),
};

export const overunderPayload = {
  /** DEBATE (scribe): live dial move — public mirror for the arguing room. */
  dial: (n: number): Record<string, unknown> => ({ line: n }),
  /** DEBATE (scribe): LOCK THE LINE. Carries the value so lost dial frames can't skew it. */
  lock: (n: number): Record<string, unknown> => ({ line: n, lock: true }),
  /** BET: over or under the locked line. Re-bet allowed until deadline; last one wins. */
  bet: (side: 'over' | 'under'): Record<string, unknown> => ({ bet: side }),
  /** TRUTH (subject): the real number. The ONLY path to a verified truth value. */
  truth: (n: number): Record<string, unknown> => ({ truth: n }),
  /** TRUTH (host): scribe-downgrade — subject's verbal claim, flagged UNVERIFIED (4.7). */
  claim: (n: number): Record<string, unknown> => ({ claim: n }),
  /** TRUTH (room, pit open): DRAG THEM BACK / FEED THEM TO THE PIT. */
  pit: (choice: 'drag' | 'pit'): Record<string, unknown> => ({ pit: choice }),
};

// Confession keys are LAW: engine games/confession.ts parsePick/parseTruth/
// parseJuryVote/parsePit read exactly these.
export const confessionPayload = {
  /** PICK (confessor): hand index 0..2 of the chosen sin. */
  pick: (i: number): Record<string, unknown> => ({ pick: i }),
  /** LOCK (confessor): the truth. Never echoed anywhere until REVEAL. */
  lock: (truth: boolean): Record<string, unknown> => ({ truth }),
  /** JURY: believe or cap. */
  verdict: (v: 'believe' | 'cap'): Record<string, unknown> => ({ vote: v }),
  /** LOCK (room, pit open): the shame vote. */
  pit: (choice: 'drag' | 'pit'): Record<string, unknown> => ({ pit: choice }),
};
