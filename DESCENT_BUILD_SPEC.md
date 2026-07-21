# DESCENT_BUILD_SPEC.md — HELLDECK 2.0 full build specification & tracker

_The squirrel-proof document. If a task in Part 12 is checked off, the acceptance criteria in that task were met — no interpretation allowed. Companion docs: `HDRealRules2.md` (game rules — the WHAT), `CONTENT_BIBLE.md` (card law), `CARD_AUTOPSY.md` (evidence). This doc is the HOW._

**Version:** 1.0 (2026-07-16) · **Status:** approved for build · **Owner:** Nick Toledo

---

# PART 0 — ORIENTATION

## 0.1 What we are building

A phones-only party game. One room of 3–12 friends, same physical space, every player on their own phone. No TV. The game brain runs server-side; every phone (host included) is a thin renderer. Joiners use a mobile browser — no install, no account. The host pays $9.99 once (first Night free).

## 0.2 What exists today (and its fate)

| Today | Fate |
|---|---|
| Android Kotlin/Compose app (~51k lines), pass-and-play | **Frozen.** Not deleted, not maintained. 2.0 is a new codebase in `descent/`. |
| On-device llama.cpp + 1.7GB GGUF models | **Dead.** Never fired in production; blocks Play publishing. Excluded from all new builds. |
| 917 gold cards + templates + lexicons | **Replaced** by rebuilt corpus (Part 8, Part 13). 123 keepers carried over. |
| Play Billing code (`PurchaseManager.kt`) | **Reference only.** New shell app re-implements against new product ID. |
| `Heimdall/`, `loader/`, kiosk/device-admin, `models/` | **Delete** in cleanup task D-903. |

## 0.3 Hard constraints

- **$0 infra until beta ends** (Cloudflare free tier), then Workers Paid $5/mo as a launch precondition (free tier hard-fails past ~43 room-nights/day).
- Solo developer. Anything not in this spec is out of scope for v1.
- Taste: UNHINGED, never WHOLESOME (repo `CLAUDE.md`). No Tailwind-default aesthetic, no purple-blue gradients, no corporate UI. The UI is a dive bar with great lighting, not a SaaS dashboard.
- Never say "AI-powered" anywhere user-facing.

## 0.4 Glossary

| Term | Meaning |
|---|---|
| **Night** | One full game session (5/7/9 circles) |
| **Circle** | One round: one mini-game played once (or 2 loops for single-performer games) |
| **DO** | Cloudflare Durable Object — one instance per room, the authoritative game engine |
| **Stage** | Optional mode where the host phone lies mid-table showing public-only info + audio |
| **Imp** | Latecomer/overflow participant: roastable, half-vote, converts to player at circle boundaries |
| **Brimstone** | Per-player veto token (2/night): burn a card or dodge a spotlight |
| **E / C** | Card rating axes: Exposure (how personal — capped by consent) / Chaos (how deranged — never capped) |
| **Blocking input** | An input that cannot be auto-played (truth-lock, receipt number, role-ack). Pauses timers; has terminal states. |
| **Skippable input** | Vote/punchline. Timer fires → auto-abstain / auto-panic. Game never waits. |

---

# PART 1 — SYSTEM ARCHITECTURE

## 1.1 Components

```
┌──────────────┐   HTTPS/WSS    ┌─────────────────────────────┐
│ Player phone │◄──────────────►│ Cloudflare Worker (router)  │
│ (browser)    │                │  /api/*  /ws/:code  static  │
└──────────────┘                └──────────┬──────────────────┘
┌──────────────┐                           │ 1 per room
│ Host phone   │                ┌──────────▼──────────────────┐
│ (browser OR  │◄──────────────►│ RoomDO (Durable Object)     │
│ Android      │                │ - @helldeck/engine (pure TS)│
│ shell)       │                │ - WS hub + clock sync       │
└──────────────┘                │ - SQLite snapshot + Alarms  │
                                └─────────────────────────────┘
Static assets: client bundle served via Workers Static Assets (free).
Payments: Stripe Payment Link (web) / Play Billing (Android shell).
```

## 1.2 Tech stack (pinned — do not substitute)

| Layer | Choice | Version policy |
|---|---|---|
| Language | TypeScript everywhere | 5.x, `strict: true` |
| Monorepo | pnpm workspaces | pnpm 9.x |
| Server | Cloudflare Workers + Durable Objects (SQLite-backed), `wrangler` | wrangler 4.x |
| Shared engine | `@helldeck/engine` — pure TS, zero I/O, zero Date.now (clock injected) | — |
| Web client | Preact 10 + Vite 6, hand-rolled CSS (no framework, no Tailwind) | bundle ≤ 200KB gzip |
| Tests | Vitest (engine, protocol), Miniflare/`wrangler dev` for DO integration | — |
| Android shell | Kotlin, single Activity + WebView, Play Billing Library 7.x | later phase |
| IDs | `nanoid` (player tokens), custom 4-letter room codes | — |

## 1.3 Repo layout (new top-level dir in existing repo)

```
descent/
  pnpm-workspace.yaml
  packages/
    engine/          # pure game logic. NO imports from server/client.
      src/
        types.ts     # Part 2 verbatim
        engine.ts    # reduce(state, event) -> {state, effects}
        arc.ts       # arc builder (Part 4.3)
        consent.ts   # ceiling filters (Part 4.4)
        scoring.ts   # Part 4.6
        deal.ts      # deal ceremony + burn absorption (Part 4.5)
        games/       # one file per game: roast.ts, fillin.ts, ...
        index.ts
      test/          # vitest; every rule in HDRealRules2 has a test
    server/
      src/
        worker.ts    # router: /api/room (create), /ws/:code, static
        room-do.ts   # RoomDO class: WS hub, alarms, persistence
        clock.ts     # ping/offset protocol
        entitle.ts   # host unlock verification (Stripe/Play)
      wrangler.toml
    client/
      src/
        main.tsx     # route: /, /:code, /host
        net/ws.ts    # socket, reconnect, epoch, clock offset
        screens/     # Part 6 inventory, one file per screen
        games/       # per-game render components (renderers ONLY)
        style/       # design tokens (Part 6.2)
      index.html
  content/
    decks/           # rebuilt corpus JSON (Part 8 schema)
    tools/           # lint_deck.py, dedup_skeletons.py, banlist.txt
  android-shell/     # phase M4
```

## 1.4 Environments

| Env | URL | Notes |
|---|---|---|
| dev | `wrangler dev` local | Miniflare DO emulation |
| beta | `helldeck-beta.<account>.workers.dev` | free tier, no payments enforced |
| prod | custom domain (task D-801 buys it; ~$10/yr, the one allowed expense) | Workers Paid on |

---

# PART 2 — DATA MODEL (`packages/engine/src/types.ts`, verbatim)

```ts
// ===== identity =====
export type PlayerId = string;            // nanoid(12), stable across reconnects
export type RoomCode = string;            // 4 chars from ALPHABET (see 4.1)

export interface Player {
  id: PlayerId;
  name: string;                           // 1..14 chars, trimmed, uniqued per room ("SAM (2)")
  avatar: number;                         // index into avatar set (0..15)
  role: 'host' | 'player' | 'imp';
  seat: number;                           // join order, 0-based; host = 0
  connected: boolean;
  lastSeenAt: number;                     // server ms
  heatCeiling: 1|2|3|4|5;                 // private; NEVER broadcast (see 3.4 redaction)
  attested18: boolean;
  brimstones: number;                     // starts 2
  score: number;
  spotlightCount: number;                 // fairness counter
  freshMeat: boolean;                     // first night with this crew
}

// ===== night config =====
export interface NightConfig {
  depth: 5 | 7 | 9;
  vibe: 'sober' | 'warm' | 'feral';       // start rung = min(vibe→{1,2,3}, genericCeiling-2), floor 1
  stageMode: boolean;                     // default: nPlayers >= 5
  crewId: string;                         // hash of sorted names; keys crew memory
  irlFamiliar: boolean;
}

// ===== cards =====
export type Register = 'observational'|'absurdist'|'deadpan'|'menace'|'petty-domestic'|'gross'|'physical'|'parody'|'table-aware'|'euphemism';
export interface CardBase {
  id: string;                             // "<deck>_v3_<n>"
  deck: DeckId;
  text: string;                           // may contain {NAME} / {NAME2}
  exposure: 1|2|3|4|5;
  chaos: 1|2|3|4|5;                       // invariant: chaos >= 3
  register: Register;
  skeleton: string;                       // dedup slug
}
export type DeckId = 'roast'|'fillin'|'overunder'|'confession'|'scatter'|'poison'|'redflag'|'alibi'
                   | 'texttrap'|'reality'|'taboo'|'hotseat'|'titlefight';
// per-deck extras (Part 8.1 for full field law):
export interface OverUnderCard extends CardBase { receiptSurface: string; timebox: string; }
export interface PoisonCard    extends CardBase { optionA: string; optionB: string; }
export interface RedFlagCard   extends CardBase { perk: string; flag: string; }
export interface AlibiCard     extends CardBase { accusation: string; words: [string,string,string]; decoys: [string,string,string,string,string]; }
export interface ScatterCard   extends CardBase { category: string; letter: string; }
export interface TabooCard     extends CardBase { word: string; forbidden: string[]; }   // exactly 5
export interface TextTrapCard  extends CardBase { sender: string; message: string; tone: string; }

// ===== night state machine =====
export type Phase =
  | { k: 'LOBBY' }
  | { k: 'CONSENT' }                      // private ceiling picks + 18+ attestation
  | { k: 'CIRCLE_INTRO'; circle: number } // 8s descend transition + explainer
  | { k: 'DEAL'; circle: number }         // 5-6s ceremony (absorbs burns)
  | { k: 'INPUT'; circle: number; sub: string; deadline: number | null }  // null = paused (blocking)
  | { k: 'WAITING_ON'; circle: number; who: PlayerId; since: number }     // blocking shame state
  | { k: 'REVEAL'; circle: number; holdSince: number }
  | { k: 'LADDER'; circle: number }       // 5s scoreboard
  | { k: 'JUDGMENT' };

export interface CircleSpec {
  game: DeckId;
  loops: number;                          // 1, or 2 for single-performer games, 3 prompts for roast...
  finale: boolean;                        // x3
  outward: boolean;                       // arc-grammar class
}

export interface RoomState {
  code: RoomCode;
  createdAt: number;
  config: NightConfig | null;             // null until host configures
  players: Player[];
  phase: Phase;
  arc: CircleSpec[];                      // built at descent start (4.3)
  circleIdx: number;
  gameState: unknown;                     // per-game state, typed per game file
  usedCardIds: string[];                  // night-scope dedup
  usedSkeletons: Record<string, number>;
  devilsBargain: { holder: PlayerId; circle: number } | null;
  epoch: number;                          // bumped on every phase change; stale msgs dropped
  entitled: boolean;                      // host unlock or free night
  telemetry: TelemetryEvent[];            // flushed to snapshot, whitelist only
}

export interface TelemetryEvent { t: string; cardId?: string; fires?: number; at: number; }
// PRIVACY WHITELIST (Part 10): scores, win records, superlative TITLES, night count,
// per-card fire counts. NEVER: card text linkage to persons, confession truth values,
// who-voted-whom, ceiling values, burn events attributable to a person.
```

## 2.1 Engine contract (the one function)

```ts
// engine.ts — the ONLY way state changes. Pure. Deterministic given (state, event, rng seed).
export type GameEvent =
  | { t: 'JOIN'; player: {...} } | { t: 'LEAVE'; id: PlayerId }
  | { t: 'CONFIG'; cfg: NightConfig } | { t: 'CEILING'; id: PlayerId; v: 1|2|3|4|5 }
  | { t: 'BEGIN' } | { t: 'INPUT'; id: PlayerId; payload: unknown }
  | { t: 'BURN'; id: PlayerId; kind: 'card'|'spotlight' }
  | { t: 'DESCEND'; id: PlayerId } | { t: 'VOID_ROUND'; id: PlayerId }
  | { t: 'PLEAD_FIFTH'; id: PlayerId } | { t: 'SKIP_EM'; id: PlayerId }
  | { t: 'REST_CASE'; id: PlayerId } | { t: 'FIRE'; id: PlayerId; n: number }
  | { t: 'TIMER'; timerId: string }     // fired by DO alarm — timers are events, not effects
  | { t: 'RECONNECT'; id: PlayerId };

export interface Effect {                 // engine returns effects; DO executes them
  k: 'SCHEDULE' | 'CANCEL' | 'BROADCAST' | 'SEND' | 'SNAPSHOT' | 'AUDIO';
  // SCHEDULE {timerId, atMs} -> DO Alarm; SEND {to: PlayerId, msg}; AUDIO {sting: string}
}
export function reduce(s: RoomState, e: GameEvent, seed: string): { state: RoomState; effects: Effect[] };
```

**Law:** the engine never reads clocks, sockets, or storage. The DO owns I/O. This is what makes every rule unit-testable (Part 11) and the whole game replayable from an event log.

---

# PART 3 — WEBSOCKET PROTOCOL

## 3.1 Connection lifecycle

1. Client opens `wss://<host>/ws/<CODE>?token=<playerToken>&name=<urlenc>&v=1`.
   - No token → server issues one in `WELCOME` (client stores in `localStorage["hd:<CODE>:token"]`).
   - Existing token + same room → silent reseat (reconnect), full state push.
2. Server sends `WELCOME`, then `STATE` (full redacted snapshot), then deltas.
3. Client answers every `PING` immediately (clock sync, 3.3).
4. On tab hide/show: client sets `epochLocal++`, discards queued renders, sends `{t:"RESYNC"}` on visible → server replies with full `STATE`.

## 3.2 Messages (complete list — anything not here does not exist)

**Client → Server** (all include nothing else; server knows who you are from the socket):
```jsonc
{"t":"JOIN","name":"SAM","avatar":3}
{"t":"ATTEST18"}                       // one-tap attestation
{"t":"CEILING","v":4}                  // private consent pick
{"t":"CONFIG","depth":7,"vibe":"feral","stage":true}   // host only
{"t":"BEGIN"}                          // host only; server validates entitlement first
{"t":"INPUT","p":{...}}                // per-game payload, validated per game schema
{"t":"BURN","kind":"card"}            // during pre-view window only
{"t":"BURN","kind":"spotlight"}
{"t":"DESCEND"}                        // during REVEAL; host always, everyone after 45s softcap
{"t":"VOID"}                           // host only, during WAITING_ON
{"t":"FIFTH"}                          // blocking-input subject only
{"t":"SKIPEM"}                         // during a live performance
{"t":"REST"}                           // performer only
{"t":"FIRE","n":4}                     // coalesced: max 1 msg / 500ms, n = taps since last
{"t":"PONG","id":17,"cl":1720900000000}
{"t":"RESYNC"}
```

**Server → Client:**
```jsonc
{"t":"WELCOME","you":"<playerId>","token":"<playerToken>","code":"HELL","sv":1720900000123}
{"t":"STATE","s":{...},"epoch":42}         // FULL redacted state (3.4); render source of truth
{"t":"PATCH","epoch":43,"ops":[...]}       // RFC6902-lite: only replace/add on known paths
{"t":"PRIVATE","k":"role"|"card"|"words"|"preview"|"spotlight","p":{...}}  // secrets, per-socket only
{"t":"PING","id":17,"sv":1720900000123}
{"t":"AT","timerId":"input:c3","at":1720900012000}  // deadline in SERVER time; client maps via offset
{"t":"AUDIO","sting":"boom"|"bell"|"burn"|"descend"|"judgment"}  // host phone honors, others ignore
{"t":"HEAT","n":37}                        // fire-tap buckets, ≤4Hz
{"t":"ERR","code":"ROOM_FULL"|"BAD_INPUT"|"NOT_HOST"|"NO_ENTITLEMENT"|"ROOM_EXPIRED","msg":"..."}
```

## 3.3 Clock sync (the reveal-simultaneity mechanism)

- `WELCOME.sv` seeds the client offset before the first `STATE`/`PRIVATE`, so a skewed phone cannot
  discard a live private safety window; the ping batch below refines that first estimate.
- On connect and every 60s: server sends 5 `PING`s 200ms apart; client `PONG`s with its clock.
- Server computes per-client offset = median of 5 samples; stores on socket.
- Every deadline is broadcast as **server time** (`AT`). Client renders countdowns from `serverTime - offset`.
- Reveals: server schedules `AT` ≥ 1000ms in the future → all phones flip within ~30–80ms of each other (target: p95 ≤ 150ms skew).
- Countdown ticks are NEVER individual messages — clients derive them from the single deadline.

## 3.4 Redaction (secrets never transit to the wrong phone)

`STATE`/`PATCH` are computed **per socket**. Before send, strip: other players' `heatCeiling` (all ceilings — even your own is only echoed to you), pending secret inputs (votes cast, truth-locks), role assignments, pre-view cards, alibi words, taboo words (except clue-giver + buzzer sockets), Devil's Bargain holder (until Judgment). Secrets travel only via `PRIVATE` to the entitled socket. **Test D-113 asserts a serialized broadcast frame contains none of these fields.**

## 3.5 Idempotency & epochs

- Every phase change bumps `epoch`. Client tags rendered UI with epoch; any `PATCH` with `epoch <= current` is dropped.
- `INPUT` for a phase that already ended → server replies `ERR BAD_INPUT` silently (client ignores).
- Reveals are **state, not events**: a phone reconnecting 2s late renders the current reveal from `STATE` — nothing is missed, nothing double-fires.

---

# PART 4 — THE NIGHT ENGINE

## 4.1 Room creation & codes

- `POST /api/room` → `{code}`. Code alphabet: `BCDFGHJKLMNPRSTVWXYZ` (20 consonants, no vowels → no accidental words, no 0/O/1/I confusion). 20^4 = 160k rooms.
- Room TTL: DO Alarm deletes state 30 min after last socket closes (10 min if Night never began). Crew memory whitelist is exported to the host client before deletion (10.2).

## 4.2 Global flow

```
LOBBY ──(host CONFIG + ≥3 players + all CEILING set + entitlement)──► CONSENT done
  └─► arc = buildArc(config, players)                     // 4.3
  └─► for circle in arc:
        CIRCLE_INTRO (8s; 15s first-time explainer, 5s title card after)
        DEAL (5–6s ceremony; absorbs ≤1 burn invisibly)   // 4.5
        [game-specific INPUT phases]                       // Part 5 per game
        REVEAL (hold ≥20s; DESCEND: host anytime, anyone ≥45s; fire-decay ≥8s quiet past 20s)
        LADDER (5s)
  └─► JUDGMENT (winner, superlatives, share card, "descend again?")
```

## 4.3 Arc builder (`arc.ts`) — deterministic given (config, players, seed)

```
pool = LAUNCH8 filtered by: minN ≤ nPlayers, irlFamiliar gate, content available at legal E-tiers
assert circles ≤ uniqueLegalGames + sanctionedRepeats(nPlayers)  // else lobby caps depth (UI enforces first)
slots[0]        = simultaneous-input game (roast | overunder | fillin)
slots[mid]      = scatter (physical spike);  if depth ≥ 7: second spike = titlefight at mid+2
slots[last]     = finale: all-players-score ∩ legal (roast|fillin|overunder; N≤4 prefer overunder)
fill remaining: no two subject-targeting circles back-to-back after slot 4;
                ≥1 outward game per ⌈depth/3⌉ block; spotlight games only after slot 2;
                repeats only {roast, scatter, overunder}, never adjacent, max 1 repeat each
loops per circle: roast=3 prompts, overunder=2 subjects, scatter=best-of-3,
                  confession/texttrap/reality/redflag/alibi/poison = 2 loops, fillin=1 (N≤6) | 2 setups (N≥7)
E-curve: startRung = max(1, min(vibe∈{sober:1,warm:2,feral:3}, genericCeiling−2))
         rung(c) = min(startRung + floor((c−1) * (maxLegal−startRung) / (depth−1)), tierCeiling(circle))
devilsBargain: if depth ≥ 5 → attach to a random all-players-score circle in last third, holder = current last place at that circle's start
```

## 4.4 Consent filter (`consent.ts`)

```
genericCeiling  = nPlayers ≤ 4 ? min(ceilings) : secondLowest(ceilings)
cardLegal(card, subjectIds):
  if card names subjects → E ≤ min(ceiling of each named subject)
  else if game is vote-emergent (roast) and E > 3 → E ≤ min(all ceilings)
  else → E ≤ genericCeiling
NEVER surface any ceiling value, or whether filtering occurred, anywhere. (Test D-114.)
```

## 4.5 Deal ceremony & burns (`deal.ts`)

```
On DEAL: pick card + backup card (both legal, both reserved).
  t=0     broadcast ceremony start ("THE DECK IS CHOOSING ITS VICTIM…", fixed 5.5s)
  t=0     if card names a subject → PRIVATE preview to subject
          {status:"assigned", previewId, card, burnDeadline:t+10s, revealAt:t+10s, canBurn}
          (preview windows only during heads-down phases; ceremony IS heads-down)
  t≤10s   subject may BURN → swap to backup, NO trace: same ceremony length, no animation delta,
          subject-burn not decremented visibly, not in telemetry attributably; acknowledge only to
          that socket with PRIVATE {status:"released", previewId}
  t=10s   fixed deadline broadcasts the chosen card to all → next phase
Spotlight assignment: core-owned, PRIVATE, and burn-independent; room sees only "DEALING…".
  t=0      choose one/two distinct primary assignees; PRIVATE role notice, burn deadline t+10s
  t=0      schedule BOTH fixed timers up front: handoff t+10s and completion t+20s
  t≤10s    a primary may BURN spotlight; spend one Brimstone privately, leave that slot vacant
  t=10s    fixed handoff always fires; fill burned slots from the private reserve order and give
           each replacement a full PRIVATE 10s window; kept primaries are locked
  t≤20s    a replacement may BURN; that slot resolves null (no third timing-revealing window)
  t=20s    commit final role ids; only non-null final assignees increment spotlightCount;
           an incomplete required cast voids the loop rather than fabricating/coercing a performer
Burn acceptance is acknowledged only to the burner. Burns never change public frames, schedules,
audio, or telemetry. Reconnect/RESYNC replays only the viewer's current role and original deadline.
Volunteer-first: 5s "WHO WANTS BLOOD?" claim button precedes random assignment (weighted to lowest spotlightCount).
```

## 4.6 Scoring (`scoring.ts`) — complete table

| Event | Points |
|---|---|
| Fill-In: your punchline wins the vote | +3 (finale Reader: +1 per 10 fires during read, cap +3) |
| Fill-In: panic-button answer wins | +1 (half, rounded down from +3 → +1) |
| Roast: you voted the plurality victim | +2 |
| Roast: Room Heat (unanimity−victim at N≤5; ≥60% at N≥8; ≥80% else) | +1 extra to plurality voters |
| Over/Under: correct bet | +2; exact-line push → everyone +1 |
| Confession: jury fooled (majority wrong) | confessor +3 |
| Confession: your believe/cap vote correct | +1 |
| Poison: your pitch wins | +3 (N=3 damage-meter: score = rating; predict-own-score exact → +2) |
| Red Flag: SMASH verdict (defense survived) | defender +3 |
| Alibi: jury member per planted word found | +1; accused per word MISSED by >50% of jury | +1 |
| Scatter: survive (not holding at boom) | +1; holder +0 |
| Text Trap: SURVIVED verdict | performer +3 |
| Reality: ego-gap ≤1 | subject +2; every debater +1 if gap ≥4 ("called it") |
| Any vote cast at all (participation) | +1 (max once per circle) |
| PLEAD THE FIFTH / voided round | 0 to all for that loop |
| Finale circle | all points ×3 |
| Devil's Bargain holder, on their bargain circle | circle points ×2 (stacked before finale ×3 if same circle — not possible by construction: bargain never on finale) |

**Law (from playtests): no points for aligning with a majority except Roast. Performers, writers, liars, and subjects earn; spectators tread water.**

## 4.7 Blocking-input terminal machine

```
INPUT(blocking, owner=X):
  deadline = null (timer paused), broadcast WAITING_ON X after 12s grace
  X's phone: input UI + permanent [PLEAD THE FIFTH] button → round loop voided, sting, 0 pts
  t+30s: all phones get [DRAG THEM BACK] / [FEED THEM TO THE PIT] → ≥60% PIT = void loop, sting,
         re-deal a no-subject filler prompt if loop had not produced content yet
  host always has [VOID ROUND]
  X disconnects >90s (seat lapse): auto-void, sting "THE WITNESS FLED"
  scribe-downgrade (overunder only): host may enter X's verbal claim, flagged UNVERIFIED (no receipt bonus)
NEVER auto-fabricate a truth value. (Test D-115.)
```

## 4.8 Imp lifecycle

```
join while phase != LOBBY or nPlayers = 12 → role = imp
imp rights: name enters {NAME} pool; FIRE; votes at 0.5 weight (ceil on ties never decided by imps);
            contributes 1 curated punchline in N≥7 fillin (screen cameo, never performed)
at each CIRCLE_INTRO: convert imps → players in join order while nPlayers < 12,
  announce as role ("ZOE IS TONIGHT'S IMP — feed her souls" → "ZOE HAS CLAWED HER WAY UP")
residual imps at JUDGMENT: guaranteed superlative ("STILL IN HELL'S WAITING ROOM")
```

---

# PART 5 — PER-GAME SPECIFICATIONS (LAUNCH 8)

Format per game: **phases** (name · duration · who acts · every phone's screen), **input class**, **resolution/ties**, **small-N variants**, **acceptance criteria**. All timers in seconds; all input phases are simultaneous. "All:" = every phone. Performer screens always also show the permanent Brimstone/status strip (thin, bottom).

## 5.1 ROAST CONSENSUS (`games/roast.ts`) — minN 3 · 3 prompts/circle

| # | Phase | Dur | Screens |
|---|---|---|---|
| 1 | DEAL ceremony | 5.5 | All: roulette animation |
| 2 | VOTE (skippable) | 20 | All: prompt + tappable avatar grid (self disabled) + countdown. Stage (if on): prompt + "4/6 voted" tally only |
| 3 | REVEAL | hold | All: 3-2-1 flash → victim's name in lights + vote spread (bar per player, anonymous). Fire button live |
| 4 | → next prompt (×3) or LADDER | | |

- Resolution: plurality victim. Tie → **DOUBLE ROAST** (both names in lights; a vote for either counts as plurality). Room Heat per 4.6.
- N=3–4: **FACE YOUR ACCUSERS** — vote spread shows names→names (attributed). Display-suppression per circle: an edge shown once this circle renders as "▮▮▮" the second time. Prompts capped E≤3 in this mode. Ballots are never constrained.
- Acceptance: at N=6, full 3-prompt circle ≤ 3:30 with 20s holds; tie path renders DOUBLE ROAST; no vote identity recoverable from broadcast frames at N≥5 (test asserts).

## 5.2 FILL-IN FINISHER (`games/fillin.ts`) — minN 3

| # | Phase | Dur | Screens |
|---|---|---|---|
| 1 | DEAL | 5.5 | All: ceremony → setup card shown to all |
| 2 | WRITE (skippable) | 60 | Writers: setup + text field (140 chars) + [PANIC] (2 curated fallbacks, tap to take, −half pts). Reader: setup + read-tone picker ("read these as: a eulogy / a 911 call / a hostage tape / an apology video") |
| 3 | PERFORM | Reader-paced, cap 90 | Reader: teleprompter — one answer at a time, big type, [NEXT] + [BURN LINE] (UGC strike). All others: "LISTEN UP — answer 3 of 7" + fire button. NO answer text on non-Reader phones |
| 4 | VOTE (skippable) | 15 | All except authors-for-own: full answer texts as memory-aid ballot, randomized order per phone |
| 5 | REVEAL | hold | All: winning line + author name in lights + runner-up |

- N≥7: two setups, writers split; per setup Reader performs ≤4 answers → one vote picks top → grand face-off (both Readers perform, final vote). Two Readers total, alternating.
- N=3: no Reader. Derangement assignment: each performs another's line aloud, prompt shows "you're performing {NAME}'s filth — sell it." Vote on text after all reads.
- Reader for finale: never last place / Bargain holder.
- Acceptance: answers NEVER appear on any non-Reader phone before phase 4 (frame test); panic answers flagged internally for scoring; empty write auto-submits panic option A.

## 5.3 OVER/UNDER (`games/overunder.ts`) — minN 3 · 2 subjects/circle

| # | Phase | Dur | Screens |
|---|---|---|---|
| 1 | DEAL + subject pre-view | 10 | Subject (private, during ceremony): card + [BURN]. Then all: stat + subject name + receipt surface ("Settings → Screen Time") |
| 2 | LINE DEBATE (verbal) | 25 | All: "ARGUE. OUT LOUD." + the stat + a big line-setter dial on the SCRIBE's phone (scribe = next seat after subject). Scribe locks the room's line |
| 3 | BET (skippable) | 12 | All except subject: OVER / UNDER buttons. Subject: "fetch your receipt" instruction |
| 4 | TRUTH (blocking) | paused | Subject: number pad + [PLEAD THE FIFTH]. All others: "WAITING ON {NAME}'S RECEIPTS…" + shame countdown per 4.7 |
| 5 | REVEAL | hold | All: 3-2-1 → true number vs line, winners flash, subject vindicated/exposed banner |

- Push (number == line): everyone +1, subject roasted for being median.
- Acceptance: timer provably paused during phase 4 (no auto-number ever — test D-115 covers); subject app-switch + return within seat-hold resumes phase 4 intact; scribe reassignment if scribe disconnects.

## 5.4 CONFESSION OR CAP (`games/confession.ts`) — minN 3 · 2 confessors/circle

| # | Phase | Dur | Screens |
|---|---|---|---|
| 1 | DEAL 3 (private) | 20 | Confessor: 3 confession cards, pick 1 (auto-pick #1 at timeout). Others: "THE ACCUSED IS CHOOSING THEIR SIN" |
| 2 | LOCK (blocking) | paused | Confessor: TRUE / FALSE lock + [FIFTH]. Others: waiting screen (rarely >5s) |
| 3 | PERFORM (verbal) | 45, [REST] | Confessor: the card + "sell it" + [I REST MY CASE]. Others: card text + "watch their hands" + fire |
| 4 | JURY VOTE (skippable) | 12 | Others: BELIEVE / CAP. Confessor: "they're deciding your fate" |
| 5 | REVEAL | hold | All: 3-2-1 → TRUE/FALSE stamp + jury spread + fooled/caught banner |

- Tie: confessor wins ("HUNG JURY — THE LIAR WALKS").
- Acceptance: truth-lock value never in any broadcast frame before phase 5; pick-of-3 never reveals the 2 unchosen cards to the room (they return to the pool).

## 5.5 SCATTERBLAST (`games/scatter.ts`) — minN 3 · best-of-3 bombs

| # | Phase | Dur | Screens |
|---|---|---|---|
| 1 | DEAL | 5.5 | All: CATEGORY + LETTER, huge type |
| 2 | BOMB (verbal circle) | hidden 15–45 | Stage/volunteer phone: ticking bomb face (audio+pulse). All others: category + letter + subtle pulse (visual-first; bar-proof) |
| 3 | BOOM | 5 | ALL phones: full-screen flash "WHO DIED?" + avatar grid tap |
| 4 | REVEAL | hold | All: loser's name + "THE TABLE HAS SPOKEN" (majority tap) |

- Fuse: uniform random 15–45s, biased +5s per bomb number (bomb 3 longest). Turn passing is verbal — the app never tracks turns.
- Acceptance: boom flash lands on all phones ≤150ms skew p95; WHO DIED majority resolves ties by earliest-tap-timestamp median; category+letter re-render instantly on rejoin.

## 5.6 POISON PITCH (`games/poison.ts`) — minN 3 · 2 duels/circle

| # | Phase | Dur | Screens |
|---|---|---|---|
| 1 | ASSIGN (private, volunteer-first) | 10 | 5s "WHO WANTS BLOOD?" ×2 → two pitchers get PRIVATE side assignment + [BURN spotlight] |
| 2 | DEAL | 5.5 | All: both options rendered as a fight card ("A vs B") + pitcher names |
| 3 | PITCH A → PITCH B (verbal) | 30+30, [REST] | Pitcher: own option + angle hints ("workaround / silver lining / attack") + [REST]. All: fight card + bell timer + fire + [SKIP-'EM] |
| 4 | VOTE (skippable) | 12 | Non-pitchers: A / B (which PITCH won, not which option) |
| 5 | REVEAL | hold | All: winner banner + vote split |

- Tie: 10s sudden-death rebuttal each → revote → still tied = split pot (+2 each).
- N=3 variant (auto): single judge rates each pitch 1–5 damage meter; pitchers pre-predict own rating (exact → +2).
- Acceptance: sides never leak before phase 2; SKIP-'EM re-deals the CARD only (same pitchers, new dilemma, once per circle).

## 5.7 RED FLAG RALLY (`games/redflag.ts`) — minN 3 · 2 defenses/circle

Phases: ASSIGN (volunteer-first, private, burnable) → DEAL (perk+flag fight card to all) → DEFENSE (45s verbal, [REST], [SKIP-'EM]) → VOTE SMASH/PASS (12s, skippable) → REVEAL (hold).
- Tie → SMASH (performer wins ties). Card law: display text is the ONLY joke source (one card = one joke; build lint rejects field mismatch).
- Acceptance: defender assignment respects spotlightCount weighting; verdict stamp animation ≤2s glanceable.

## 5.8 ALIBI DROP (`games/alibi.ts`) — minN 3 · 2 accused/circle

| # | Phase | Dur | Screens |
|---|---|---|---|
| 1 | ASSIGN + words (private) | 12 | Accused: accusation + 3 mandatory words + [BURN]. Others: "DEALING…" |
| 2 | ACCUSATION | 5 | All: the accusation, big |
| 3 | ALIBI (verbal) | 30, [REST] | Accused: words pinned top of screen. Others: accusation + "hunt the plants" + fire |
| 4 | WORD HUNT (skippable) | 20 | Jury: 8-word lineup (3 plants + 5 decoys from the card), pick exactly 3 |
| 5 | REVEAL | hold | All: stage-sequenced — each word flips PLANTED/DECOY one at a time with sting (1.2s apart), then scores |

- Acceptance: lineup order randomized per phone; accused's words NEVER in broadcast frames before phase 5; reveal sequence is server-timed beats (AT per word), not a text dump.

## 5.9 Content Drop 1 games

Text Thread Trap, Reality Check, Taboo Timer, Hot Seat Imposter ship in Drop 1 (tracker D-7xx) with the same spec discipline; their phase tables are derived from HDRealRules2 Part III §9–12 and reviewed at Drop-1 kickoff. Not in v1 scope. Title Fight ships at launch as arc spike: ASSIGN two fighters → card names the duel verb → 30s bout (all phones = crowd meter) → point-at-winner vote.

---

# PART 6 — WEB CLIENT

## 6.1 Routes & screens (complete inventory)

| Route | Screen | Elements (exhaustive) |
|---|---|---|
| `/` | Landing/Host | wordmark, "START A NIGHT" (creates room → `/HOST` view), "JOIN A NIGHT" (code input, 4 uppercase letter boxes), 18+ line, no marketing fluff |
| `/:code` | Join | name field (14 max), avatar picker (16 devils/sinners), [ENTER HELL], 18+ attest checkbox-as-button, error states: ROOM_FULL→"HELL IS FULL (become an Imp?)", bad code→"NO SUCH PIT" |
| `/:code` in-room | Lobby | roster with avatars + connection dots, room code HUGE + QR (client-side generated), depth picker (host: QUICK DIP/STANDARD/FULL DAMNATION with duration + N-gate), vibe check (host: SOBER/WARMED UP/FERAL), private heat-ceiling picker (EVERY player, 1–5 flame dial, "how hot can cards about YOU run?" — value never shown to others), [BEGIN THE DESCENT] (host; disabled until ≥3 players + all ceilings + entitlement; shows paywall if 2nd+ night unpaid) |
| 〃 | Circle intro | circle number as descent depth art, game title, 15s/5s explainer |
| 〃 | Game screens | per Part 5, one component per (game × role × phase) |
| 〃 | Reveal | full-bleed flash → payload ≤2s glanceable, fire button (hold-to-spam), [DESCEND] per rules |
| 〃 | Ladder | 5s rank strip, biggest mover highlighted |
| 〃 | Judgment | winner crown, superlatives (staggered reveals, 2s each), SHARE CARD (canvas-rendered PNG: night stats + wordmark + "host your own descent" link), [DESCEND AGAIN] |
| 〃 | Reconnect | "CRAWLING BACK…" overlay, auto (no tap), shows current phase on success |
| any | Imp overlay | "YOU'RE AN IMP — feed on their sins" + rights explainer, conversion toast |

## 6.2 Design language (binding, from repo CLAUDE.md)

- Palette: near-black `#0B0A0C` base, hellfire spectrum (deep red `#8E1B1B` → ember orange `#E2571B`) for action/heat, bone `#E8E2D6` for text. ONE accent per screen. No gradients-as-decoration: fire gradient appears ONLY on heat/fire elements (it signals heat).
- Type: display = heavy condensed grotesque (self-hosted, subset); body = system stack. Numbers get the display face (reveals are numbers).
- Motion: reveals SNAP (≤120ms in), holds breathe (slow ember pulse). Nothing eases politely; nothing bounces.
- Every screen passes: "could this be a corporate quiz app screen?" → if yes, redesign. No card-grid-with-icons layouts. No rounded-xl-gray anything.
- All sync moments visual-first: full-screen flash + max-brightness pulse; audio garnish from host phone only.

## 6.3 Client hard requirements

- Wake Lock API on join (re-acquire on visibilitychange); audio unlock on first tap (host).
- Reconnect: exponential backoff 0.5→8s, RESYNC on visible, epoch discipline (3.5).
- Text inputs: keyboard-safe layout (timer pinned visible above keyboard — tested on iOS Safari).
- Bundle ≤200KB gzip; joins on bar LTE in <2s (Lighthouse task D-124).
- QR: client-generated (no external service), dark-bar tested (max contrast, quiet zone).

---

# PART 7 — ANDROID SHELL (phase M4)

- Single Activity + WebView (`androidx.webkit`), loads prod client URL with `?shell=1`.
- JS bridge (`window.HDShell`): `getEntitlement(): 'owned'|'none'`, `buyHostUnlock(): Promise<'owned'|'cancelled'>` (Play Billing product `descent_host_unlock`, one-time, $9.99), `setStageAudio(on)`, `onLift(cb)` (accelerometer: gravity-vector low-pass, >50° sustained 250ms = lifted; <40° = flat, asymmetric hysteresis), `keepAwake(on)` (FLAG_KEEP_SCREEN_ON), `sting(name)` (foreground-service audio for stage mode).
- Client detects `?shell=1` → prefers `HDShell` entitlement + lift events; otherwise web equivalents (manual stage-flip button).
- versionCode restart at 20000; applicationId stays `com.helldeck` (upgrade path over old app); the old game is NOT bundled.
- Play listing: Mature 17+, Data Safety per Part 10.4.

---

# PART 8 — CONTENT SYSTEM

## 8.1 Deck files (`descent/content/decks/<deck>.json`)

```jsonc
{
  "deck": "roast", "schema": 3, "updated": "2026-07-16",
  "cards": [{
    "id": "roast_v3_001",
    "text": "Who has blackmail material on everyone in this room?",
    "exposure": 3, "chaos": 4,
    "register": "table-aware", "skeleton": "room-dossier",
    // + per-deck extras exactly per Part 2 types (overunder: receiptSurface+timebox;
    //   alibi: accusation+words[3]+decoys[5]; poison: optionA/optionB; redflag: perk/flag;
    //   scatter: category+letter; taboo: word+forbidden[5]; texttrap: sender+message+tone)
  }]
}
```
Invariants (lint-enforced): `chaos >= 3`; E∈1..5; skeleton count ≤2 per deck; no banned vocabulary; deck sizes ≥ targets (roast/fillin/overunder/confession 150; poison/redflag 100; scatter/alibi 80); `{NAME}` never in an E>3 factual-implication slot; every ID unique corpus-wide.

## 8.2 Content tools (`descent/content/tools/`, Python 3 stdlib only)

| Tool | Gate |
|---|---|
| `lint_deck.py <deck.json>` | schema, invariants, char limits (text ≤120, category ≤7 words), extras present per deck |
| `dedup_skeletons.py` | skeleton budget within deck + cross-deck near-dup scan (normalized shingles); exits 1 on violation |
| `banlist.txt` + grep stage in lint | the CONTENT_BIBLE vocabulary bans, one pattern per line |
| `deck_stats.py` | register %, E-spread vs 15/20/30/20/15 target, phone-reference % (≤20%) |
CI (task D-142): all four run green before any deck lands in `main`.

## 8.3 Runtime selection

Cards compile into the Worker as a static import (no KV round-trips). Selection: filter legal (4.4) → exclude `usedCardIds` (night) + crew-recent (last 2 nights' IDs from crew memory blob) → weight by (register diversity within night, spotlight fairness for named subjects) → seeded pick + reserved backup. Telemetry: per-card fire-count buckets only.

## 8.4 Refresh pipeline (post-launch)

Monthly: telemetry cull (bottom-decile fire rates flagged) + new cards written per CONTENT_BIBLE funnel (AI drafts allowed as raw input; every shipped card passes the human funnel; AI is never autonomous author). This is the Part 13 waves machinery, reused.

---

# PART 9 — MONETIZATION & ENTITLEMENTS

## 9.1 States

`entitled = ownedUnlock || freeNightAvailable`. Free night: `localStorage["hd:freeNightUsed"]` + server-side count keyed by a device token issued at first room-create (soft enforcement is ACCEPTED v1 risk — a wiped browser gets another free night; do not build more).

## 9.2 Purchase flows

- **Web:** Judgment/paywall → Stripe Payment Link (prod mode, $9.99, product `descent-host-unlock`) with `client_reference_id = deviceToken` → success URL `/unlocked?session={CHECKOUT_SESSION_ID}` → Worker `GET /api/entitle/verify?session=` calls Stripe API, validates paid + matching reference → signs an **unlock token** (HMAC, `env.UNLOCK_SECRET`) → client stores `localStorage["hd:unlock"]`. Room-create and BEGIN verify the token signature server-side.
- **Android shell:** Play Billing purchase → `HDShell.getEntitlement()` → client posts receipt to `/api/entitle/play` → same unlock token issued (server-side receipt check against Play Developer API is task D-411; until then TRUST-CLIENT flagged risk).
- Paywall copy at the second Night's BEGIN: lobby stays assembled behind it; purchase completes in ≤30s or falls back to "start anyway" ONLY during beta.

## 9.3 The share loop

Judgment share card PNG carries `helldeck.<domain>/?crew=<crewId-short>` — landing pre-fills "HOST YOUR OWN DESCENT". Track param → conversion event (count only).

---

# PART 10 — PRIVACY, SAFETY, COMPLIANCE

1. **Crew memory whitelist** (only persisted data): `{crewId, nightCount, winRecords: [{name, wins}], superlativeTitles: [{name, title, at}], recentCardIds}`. Lives in a host-side localStorage blob, mirrored transiently in DO; DO state expires ≤30 min post-night (4.1). NEVER stored: card texts linked to people, confession truths, votes, ceilings, burns.
2. **18+**: join-page attestation (one tap) + host room attestation; depth 4–5 locked without full-room attestation. No age verification beyond attestation (general-audience posture; revisit per-state law at launch, task D-812).
3. **UGC moderation**: Reader [BURN LINE] strike + host VOID = the Play-policy moderation affordances; report email in Play listing.
4. **Play Data Safety form**: collects = none personal beyond display names (session-scoped); no ads; no third-party sharing; purchases via Play Billing. Must match reality (D-813 audit).
5. **Logging**: no card-to-person logs; server logs are room-code + event-type + timings only.

---

# PART 11 — TESTING & VERIFICATION

| Layer | What | How |
|---|---|---|
| Engine unit | every rule in Parts 4–5: scoring table, tie paths, N∈{3,4,6,8,10,12} vote math, arc grammar, consent filter, deal/burn timing-equivalence, blocking terminals, imp conversion | Vitest against `reduce()`; ≥1 test per table row; property tests: arc builder never violates grammar for 1,000 seeds × all N×depth combos |
| Redaction | no secret field in any broadcast frame across a full simulated night | serializer test D-113/114 |
| Protocol | reconnect mid-every-phase; epoch discard; double-input rejection; clock-offset math | Miniflare DO harness |
| Bot nights | 12 headless WS clients play full random nights (fuzz inputs, random disconnects/rejoins, imp joins) — 500 nights in CI, zero deadlocks/unhandled states | `packages/server/test/bots.ts` |
| Devices | iPhone Safari (backgrounding, keyboard, wake lock), cheap Android Chrome, 6 real phones for a skew measurement night | manual checklist per milestone |
| Load math | 1 room × 12 sockets × 40 min message budget vs free-tier counters; assert coalescing ≤4Hz heat | instrumented counters (D-311) |
| Real playtests | M2: 1 real friend-group night; M3: 3 nights incl. one bar; instrument everything, LOL-audit by ear | the only test that counts |

---

# PART 12 — DELIVERY TRACKER

Rules: a task is checked ONLY when its acceptance criteria (AC) pass. Order within a milestone is the build order. `[dep: X]` = don't start before X.

> **STATUS 2026-07-20 (see `NEXT_AGENT.md` for the concise live head).** M0–M3 software is
> implemented: all nine games use the real 1,024-card corpus; the unregistered-game skip path is
> retired; CLAIM is wired end-to-end; Stage private overlays are lift-gated and auto-flatten after
> the viewer's decision acknowledgement; all six burnable performer games use the core-owned
> two-window spotlight ceremony above; and card-preview burns now have the same correlated private
> acknowledgement/reconnect guarantees. The granular UI pass added the binding self-hosted condensed
> font, accessible contrast/focus/touch/modal states, honest pending errors, reduced motion, safe-area
> responsiveness, and small-phone/landscape smoke coverage. Verification is green: engine **342**,
> server **34**, client **123** = **499 tests**; strict build; all content gates; and
> a five-client depth-5 night against real `wrangler dev`/Durable Object/WebSockets reached JUDGMENT
> in 360.1s. The tracker checkboxes below remain acceptance-criterion-specific: real-device skew,
> human playtests, the 500-night CI target, payments, shell, and deploy are not implied complete.

## M0 — Skeleton (goal: two phones see the same tick)
- [ ] **D-101** pnpm workspace + packages scaffolded per 1.3, `tsc --strict` green. AC: `pnpm -r build` exits 0.
- [ ] **D-102** Worker router: `POST /api/room` → code (4.1 alphabet); `/ws/:code` upgrades into RoomDO; static serving. AC: curl creates room; wscat connects.
- [ ] **D-103** RoomDO: socket hub, JOIN/WELCOME/STATE, SQLite snapshot on mutation, Alarm-based timer events. AC: kill DO mid-night in Miniflare → state restores from snapshot.
- [ ] **D-104** Clock sync (3.3) + `AT` deadlines. AC: two browser tabs render a countdown hitting zero ≤150ms apart.
- [ ] **D-105** Client shell: routes, WS module (reconnect/epoch/RESYNC), lobby join flow with QR. AC: 3 phones join a lobby on LAN dev.

## M1 — One playable game end-to-end (Roast Consensus)
- [ ] **D-111** Engine core: `reduce()`, phase machine (4.2), LOBBY→CONSENT→CIRCLE→JUDGMENT happy path with 1 game. [dep: D-103]
- [ ] **D-112** Roast per 5.1 incl. DOUBLE ROAST + FACE YOUR ACCUSERS + Room Heat N-scaling. AC: unit tests for N=3,5,8,10 spreads/ties pass.
- [ ] **D-113** Redaction serializer + frame test. AC: scripted night's every broadcast frame free of secret fields.
- [ ] **D-114** Consent filter (4.4) + private ceiling UI. AC: ceiling never appears in any frame; E-filter property test green.
- [ ] **D-115** Deal ceremony + subject pre-view burns (4.5); blocking-terminal machine (4.7). AC: burned vs clean deals byte-identical in timing frames; no code path fabricates a truth value (grep + test).
- [ ] **D-116** Reveal-hold + DESCEND + fire coalescing + LADDER + JUDGMENT with superlatives + share PNG. AC: full 5-circle roast-only night playable by 4 real phones.

## M2 — Full night, 4 big games
- [ ] **D-121** Fill-In per 5.2 (Reader teleprompter, perform-then-vote, panic, N≥7 two-Reader bracket, N=3 derangement). AC: frame test — no answer text on non-Reader phones pre-vote.
- [ ] **D-122** Over/Under per 5.3 (scribe line dial, blocking truth, FIFTH). [dep: D-115]
- [ ] **D-123** Confession per 5.4 (pick-of-3, truth-lock, HUNG JURY).
- [ ] **D-124** Arc builder (4.3) full grammar + depth gating UI + vibe/ceiling precedence. AC: 1,000-seed property test.
- [ ] **D-125** Scoring engine complete per 4.6 + Devil's Bargain. AC: every table row unit-tested.
- [ ] **D-126** Imp lifecycle (4.8). AC: bot test with 3 late joiners at N=10.
- [ ] **D-127** Wave-1 content integrated (4 decks from Part 13) + selection (8.3). [dep: content W-1]
- [ ] **D-128** 🎯 **REAL PLAYTEST #1** (one friend group, Standard Descent). AC: night completes, no manual interventions; collect fire-rate telemetry.

## M3 — All launch games + polish
- [ ] **D-131** Scatterblast 5.5 (bomb device claim, boom flash, WHO DIED). Skew measured on 6 phones.
- [ ] **D-132** Poison Pitch 5.6 (volunteer-first, SKIP-'EM card-re-deal, sudden death, N=3 damage meter).
- [ ] **D-133** Red Flag 5.7 + Alibi 5.8 (stage-sequenced word reveal) + Title Fight spike.
- [x] **D-134** Spotlight valves complete: WHO WANTS BLOOD, fixed private burn/replacement windows,
  reconnect replay, and I REST MY CASE everywhere applicable. Verified 2026-07-20.
- [ ] **D-135** Stage mode + lift-to-sin web fallback (manual flip button); host-control failover.
- [ ] **D-136** Design pass per 6.2 on every screen + explainer cards. AC: taste review — zero screens pass the "corporate quiz app" test.
- [ ] **D-137** Bot-night fuzz suite 500 nights green (Part 11). 
- [ ] **D-138** 🎯 **REAL PLAYTESTS #2–4** (incl. one loud-bar night, one 3-player night). AC: would-play-again ≥ 4/5 groups; median laughs/round ≥2 by ear-count.

## M4 — Android shell + money
- [ ] **D-411** Shell per Part 7 (WebView, bridge, billing, stage audio, lift-to-sin sensor). AC: bridge contract tests; purchase in internal testing track.
- [ ] **D-412** Entitlements per 9.1–9.2 web path (Stripe link + verify + HMAC token). AC: paywall on 2nd night; unlock survives browser restart.
- [ ] **D-413** Free-night device tokens + paywall UI at BEGIN. 
- [ ] **D-414** Play listing: Mature 17+ IARC, Data Safety (10.4), screenshots in brand.

## M5 — Beta
- [ ] **D-511** Deploy beta env; 10 crews recruited (share-card links traced). 
- [ ] **D-512** Telemetry dashboard (fires/card, completion rate, night length, share-taps) — a static page reading exported counters, nothing fancy.
- [ ] **D-513** Fix cycle; content cull pass #1 from fire data.
- [ ] **D-514** Exit criteria: ≥70% of started nights reach JUDGMENT; ≥30% of joiner devices tap the share link; zero deadlock reports.

## M6 — Launch
- [ ] **D-801** Domain purchased + attached; **D-802** Workers Paid ON + counter alerts at 60% (both are launch BLOCKERS).
- [ ] **D-803** Stripe live mode + Play production release.
- [ ] **D-812** State age-law recheck; **D-813** Data-safety audit vs reality.
- [ ] **D-901** README/docs updated to point at descent/; **D-902** old-app code paths marked frozen; **D-903** repo cleanup (delete Heimdall/, loader/, models/, kiosk code, stray build dirs; migrate .md graveyard to docs/archive/).

# PART 13 — CONTENT REBUILD TRACKER (runs parallel to M1–M3)

- [ ] **W-1** Wave 1: roast/fillin/overunder/confession — 3 register-assigned writers + curator + 2 adversarial verifiers per deck (workflow staged: `rebuild-wave1.js`). AC: ≥150/deck post-verification, lint+dedup+stats green, 123-keeper merge done.
- [ ] **W-2** Wave 2: scatter (80), poison (100), redflag (100), alibi (80 + decoys). Same machine, per-deck extras enforced.
- [ ] **W-3** Wave 3 (Drop 1): texttrap (60 + 22 tones), reality (60), taboo (60 + forbidden lists per BODY-COUNT protocol), hotseat (60), titlefight (40).
- [ ] **W-4** Calibration pass: one human (Nick) reads every gold deck aloud once; kills on gut are final. AC: signed off per deck.
- [ ] **W-5** E/C re-rate audit on the 123 keepers + integration into deck files.

# PART 14 — RISKS & DECIDED QUESTIONS

| Risk | Stance |
|---|---|
| Free-night soft enforcement is trivially resettable | Accepted v1. Data > lockdown. |
| iOS Safari kills tabs under memory pressure during receipt fetches | Mitigated: URL+token auto-rejoin (D-105), blocking-input pause survives (D-122). |
| One writer (Nick) is the content bottleneck | The funnel accepts AI drafts as raw input; human funnel is mandatory; W-4 is the taste backstop. |
| Play review flags spice-5 content | Mature 17+, consent architecture documented in review notes, UGC affordances present (10.3). Fallback: E5 pool ships web-only. |
| Stage lift-to-sin sensor flakiness | Web fallback = manual flip button ships FIRST (D-135); sensor is M4 garnish. |
| Session cost of cloud AI content refresh | Batch API, pennies; not a runtime dependency, can lapse without breaking the game. |

**Decided (do not reopen without new evidence):** browser joiners (never a joiner app) · server-authoritative DO · one web UI runtime · anyone-can-host incl. iPhone via web · $9.99 one-time, first night free, paywall at 2nd BEGIN · 8 deep launch decks · E/C two-axis replaces spice · no ads, no subscription, ever.
