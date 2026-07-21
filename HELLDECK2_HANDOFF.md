> ### 👉 NEW AGENT: READ **[`NEXT_AGENT.md`](NEXT_AGENT.md)** FIRST, THEN THIS FILE.
> It carries the owner's current mandate for your session (fully functional app, GUI pass with
> **OpenArt-API-only** assets, ready-for-testing), the three conflicts in that directive you must
> resolve **before** generating assets, and the real remaining work. **Correction 2026-07-20:**
> local live runtime is verified; the card-preview safety seam and granular UI/UX hardening pass are
> complete and published on `main`; monetization and human playtests are still absent. This file is
> the deep historical context; `NEXT_AGENT.md` is the current brief.

# ⛔ HELLDECK 2.0 — SESSION HANDOFF. READ THIS FIRST, IN FULL, BEFORE ANY ACTION.

**You are inheriting a large, multi-day, mid-flight project.** The previous session (Claude Fable 5, ultracode on) designed and began building "HELLDECK 2.0 — The Descent." You (the next agent, on Opus 4.8) must **finish it in the same spirit and to the same standard.** The owner's explicit instruction for this handoff: _"force the next agent to not just READ but LEARN it all."_ So this is not a summary you skim — it is an onboarding protocol you complete.

---

## 0. HARD STOP — THE ONBOARDING GATE

**Do not write code, edit files, launch a workflow, run a build, or reply to the user with a plan until you have completed the LEARN protocol in §1 and written `descent/handoff/ONBOARDING_PROOF.md`.** If the user's first message is a task, still do the protocol first (it takes one focused pass) — acting blind here will corrupt weeks of validated design. The one exception: if the user asks a direct question, answer it, then do the protocol before doing work.

Why so strict: every "obvious improvement" you might reach for was probably already litigated and decided against with evidence (see §4 Decided Questions). The specs look opinionated because they are load-bearing. Skimming produces regressions that look like progress.

---

## 1. THE LEARN PROTOCOL (do this first, once)

### 1a. Read these, in this exact order, completely:
1. `HELLDECK2_HANDOFF.md` — this file (you're in it).
2. `HDRealRules2.md` — the game rules (the WHAT). Canonical; supersedes `HDRealRules.md`.
3. `CARD_AUTOPSY.md` — why the old cards failed (the evidence that drives content law).
4. `CONTENT_BIBLE.md` — the binding writing law for all cards (Ten Commandments, EXPOSURE/CHAOS rating).
5. `DESCENT_BUILD_SPEC.md` — the implementation spec + tracker (the HOW). Parts 2–5 are the code law; Part 12 is the task tracker.
6. `descent/handoff/SESSION_TRANSCRIPT.readable.md` — the previous session's full reasoning trail (2,452 lines). This is where the _why_ behind every decision lives — the playtests, the attacks, the pivots. Read it. The faithful raw log is `SESSION_TRANSCRIPT.raw.jsonl` if you need the exact bytes.
7. Your durable memory file `helldeck-2-descent-pivot.md` (loaded via MEMORY.md) — the one-screen version.

### 1b. Prove you learned it (ACTIVE RECALL — this is the "learn not read" mechanism):
Create `descent/handoff/ONBOARDING_PROOF.md` and, **from memory (no re-opening docs while you write)**, answer all of these. Then re-open the docs and correct yourself in a second pass marked "CORRECTIONS." The gap between your first pass and the corrections IS the learning; do not skip it.

1. In one sentence each, what is the purpose of each of the 5 canonical docs?
2. Why is the game server-authoritative (Durable Object) instead of host-phone-authoritative? What business fact forced this? (§4)
3. What are the two card-rating axes, what does each control, and which one is NEVER allowed to be low? Why?
4. Name the 8 launch games and the 4 Content-Drop-1 games. Which game was CUT and why?
5. Explain "perform-then-vote" for Fill-In and why the naive order was fatal.
6. What is a "blocking input," give two examples, and state the law about what the engine must NEVER do with one.
7. What is the deal ceremony and why must a burned deal be byte-for-byte timing-identical to a clean deal?
8. State the engine purity law. What three JS built-ins are banned in `packages/engine` and why?
9. What is `redact.ts` and what is the one rule about it? Name three fields in `NEVER_SERIALIZE`.
10. What is the GameModule contract? What are `$deal`/`$phase` directives and `CORE_DEALT`/`CORE_REVEAL_DONE`?
11. Who earns points and who does not, and why (the anti-metagame rule)?
12. What is "lift-to-sin" and what contradiction does it resolve?
13. What is "PLEAD THE FIFTH" and what problem does it solve?
14. What is a private heat ceiling, and why is its value NEVER rendered anywhere?
15. What is the Fresh Meat protocol and Imp citizenship?
16. What is the monetization model and where exactly does the paywall sit? (§4)
17. What are the current git HEAD, the ONE known-failing test, and why it fails?
18. What is the exact next task (§6)?
19. What are the owner's cross-project hard rules and the HELLDECK taste law?
20. What is the workflow-resume pattern when a run dies on usage limits?

If you cannot answer any of these after your corrections pass, you have not finished onboarding. Do not proceed.

### 1c. Teach-back:
At the end of `ONBOARDING_PROOF.md`, write a 5-bullet "if I had to rebuild the mental model of this project for a third agent" summary. If you can teach it, you know it.

---

## 2. WHAT THIS PROJECT IS (the 90-second orientation)

HELLDECK was a single-Android, pass-and-play party game that **wasn't fun and had bad cards** (the owner's words). The previous session diagnosed both and executed a full pivot:

**The Descent** = a phones-only, Jackbox-model party game. 3–12 friends in one room, **every player on their own phone, no TV.** Host has a paid unlock; joiners use a mobile browser (QR + 4-letter code, no install, no account). The game brain runs **server-side** (one Cloudflare Durable Object per room); every phone — host included — is a thin renderer. A "Night" is a descent through 5/7/9 **Circles** (mini-game rounds); content heat escalates with depth; it ends in THE JUDGMENT (winner + telemetry superlatives + a shareable card that seeds the next host).

The gameplay model was validated through **9 simulated playtests + 4 adversarial audits across 3 design iterations** (all in the transcript). The card corpus was **autopsied** (14% keep rate of 864 cards) and is being **rebuilt** deck by deck against a binding content law.

**This is real, validated, in-progress software.** As of 2026-07-20, 476 tests pass and a live
local bot night reaches JUDGMENT. Do not treat it as a sketch—or as human-playtested.

---

## 3. CURRENT STATE (factual)

> **LATEST UPDATE — 2026-07-20 (Codex).** The older chronological notes below are retained as
> history, but this paragraph is authoritative. Active branch `main` is published. CLAIM and visible
> server errors are wired; spotlight and card-preview burns both use correlated private
> acknowledgements and reconstruct only the current viewer's live assignment on reconnect/RESYNC;
> public schedules remain fixed. Stage gates every private face, exposes a safe decision timer while
> flat, auto-flattens after own-ballot acknowledgement, and derives its mode from the live BEGIN
> roster. The client has a self-hosted condensed font, readable contrast tokens, 48px/focus/keyboard
> treatment, modal containment, reduced motion, mobile safe areas, truthful pending/error states,
> and responsive visual smoke at 320×568, 390×844, and 844×390. Verification: strict recursive build
> green; engine 342 + server 34 + client 123 = **499 tests**; production JS 59.43 KB gzip; the prior
> 1,024-card funnel and five-bot live night remain green evidence. Remaining product gates: human
> D-128/D-138/device playtests, `descent/` CI, room TTL, crew memory, D-412/413 entitlements/paywall,
> Android shell, beta deploy/domain/live keys.

> **SESSION UPDATE — 2026-07-19 (Opus 4.8).** HEAD is now `669c862` (after `b330686`).
> **M2-INT is DONE** (commit `06bdb35`): the one known-failing test is fixed — it now drives a
> full *mixed* night through the real `RoomDO` (over/under blocking-truth, roast redaction, fill-in,
> scatter-skip → JUDGMENT + D-113 sweep); `botlogic.ts` is a shared view-driven bot driver;
> `bots.ts` drives all 4 games live incl. both blocking inputs; the **Fill-In client seam** was
> realigned to the landed engine module; `engine/test/night.test.ts` proves every REGISTERED game
> is played and never skipped (only unbuilt games skip). **Wave-1 content is DONE** (commit
> `669c862`): `over_under.json` written (157 cards); all 4 Wave-1 decks pass lint+dedup+stats
> (620 cards, no near-dups). Tests now: **engine 306, server 28, client 87 — 421 pass, 0 fail.**
> Onboarding proof: `descent/handoff/ONBOARDING_PROOF.md`. Durable facts re-saved to memory
> (the memory dir was empty): dev-env build recipe, content funnel, and a real ARC quirk —
> **spotlight games (confession/poison/redflag/alibi) only ever appear at depth 9**, never in a
> Quick Dip or Standard Descent (flagged for the arc owner). **Wave-2 content SEED committed**
> (`11de079`): scatter 58 / poison 35 / redflag 52 / alibi 28 = 173 adversarially-verified,
> funnel-green cards for the 4 unbuilt games — UNDER the W-2 targets (80/100/100/80) because the
> mechanic gates culled hard (poison balance gate; alibi no-word-repeats). **W-2 needs a top-up
> round** (esp. poison + alibi) to hit targets — folds into W-4 calibration and lands with M3. All
> 8 decks pass lint+dedup+stats (793 cards, no near-dups).
>
> **M3 ENGINE DONE** (commit `4f586a6`): all 5 remaining games — scatter, poison, redflag, alibi,
> titlefight — now have GameModules, registered in `engine.ts`. **The skip path is RETIRED:** every
> LAUNCH8 game + the Title Fight spike is playable engine-side. `night.test.ts` drives 24 depth-9
> nights playing ALL 9 games with ZERO skips (`played.size == arc.length`); the depth-5 + DO
> protocol mixed-night now play scatter. `botlogic.ts` + both test drivers extended. Built by 5
> parallel builders following roast/confession, integrated inline. **D-127 DONE** (commit `6da67f3`):
> the real 793-card corpus is wired into the runtime (`server/src/content.ts` → setXDeck at Worker
> load); `content.test.ts` proves real `_v3_` cards are dealt, not stubs. Tests: **engine 306,
> server 30, client 87 — 423 pass, 0 fail.**
>
> **SESSION 2026-07-19 (cont.) — M3 CLIENT + POLISH + W-2 CONTENT DONE.** Tests now **engine 309,
> server 30, client 87 = 426 pass, 0 fail**; full monorepo build green.
> - **D-136 DONE** (`2297274` + taste pass `2d992ab`): all 5 M3 game screens (scatter/poison/redflag/
>   alibi/titlefight) built as per-viewer Preact renderers + wired into `main.tsx` INPUT/REVEAL
>   routing + one M3 CSS section in `games.css`. **Every one of the 9 games is now human-playable
>   end-to-end** — the `InputFallback` gap is closed. taste-critic gated them (fixed a hardcoded
>   "HE" pronoun + a Tinder-default swipe line; rest cleared DISTINCTIVE).
> - **D-134 (spotlightCount half) DONE** (`bb5ff4f`): added a one-shot `GameStep.spotlight` channel;
>   the core bumps it in `applyStep` (auto-derives single-subject games from `$deal.subjectId`;
>   poison/titlefight list both fighters). `spotlightCount` finally increments → pickSpotlight
>   fairness + MOST WANTED superlative re-armed. **STILL OPEN:** the volunteer-first "WHO WANTS BLOOD"
>   valve half of D-134.
> - **D-137 DONE** (`8c3d146`): adversarial bot-fuzz (`engine/test/fuzz.test.ts`) — totality/
>   invariants/redaction/determinism across 60 hostile nights + liveness-under-noise to JUDGMENT.
> - **W-2 CONTENT TOP-UP DONE** (`7f8074e`/`9f35dbb`/`80c27cb`/`94410f9` + count test `above`):
>   poison 35→**102**, scatter 58→**81**, redflag 52→**96**, alibi 28→**88**. **Corpus = 987 cards**,
>   all funnel-clean (lint/dedup/stats), zero near-dups deck-wide. Pipeline: 3 parallel writers/deck
>   (exposure-weighted, writer-namespaced skeletons/ids) → deterministic merge/cull (`scratchpad/
>   merge_topup.py`, enforces near-dup + skeleton-budget + alibi word-uniqueness) → funnel gate →
>   independent taste-auditor cut (dropped generic/duplicate/cruel cards; taste-auditor logged a
>   cross-deck systemic finding: the "secret ranking/dossier of your friends" premise over-produces
>   — treat as a banned default in future batches). redflag landed at 96 (not 100) deliberately:
>   cutting same-y template-dups beat padding the count.
>
> **D-134 COMPLETE** (`cb69e16`): the volunteer-first "WHO WANTS BLOOD?" valve — a 5s CLAIM
>   window on every spotlight game's CIRCLE_INTRO (new `assignsSpotlight` predicate covers
>   overunder + the 5 spotlight games); a shared `pickSpotlightPreferring` prefers eligible
>   claimants (still lowest-count-weighted), falling back to weighted-random. Engine + redact
>   (count-only view) + client claim button + night-test proof; the D-137 fuzz now spams CLAIM.
> - **D-135 COMPLETE** (`b758147`): THE STAGE + lift-to-sin WEB fallback (manual flip). At N>=5 the
>   host phone is the face-up shared screen; `StageShell` shows public faces + "PICK UP TO SIN" and
>   lifts to the private ballot with a sticky "lay it back" bar, auto-dropping to flat on each new
>   sub-phase. Transparent no-op for everyone else. (The Android accelerometer sensor is M4 garnish.)
>
> **THE FULLY-AUTONOMOUS ROADMAP (M3 tail) IS NOW EXHAUSTED.** Everything left is OWNER-GATED and
> cannot be done here: **D-128/D-138 real playtests** (the actual next gate — play the current build);
> **M4** Android shell (`?shell=1`, `window.HDShell` bridge: Play Billing `descent_host_unlock`, stage
> audio foreground service, the real lift accelerometer, wake-lock); **M5** beta deploy (needs
> Cloudflare Workers Paid + `wrangler deploy`); **M6** domain purchase + Stripe live keys + Play Console
> release. Nice-to-have future autonomous polish only: a lobby toggle to make stage mode opt-in rather
> than auto-on at N>=5, and W-2 → target parity (redflag sits at 96 vs 100, by taste choice).

> **CARD COUNCIL (2026-07-19/20) — verdict: FIX-FIRST.** 37 agents: a 3-lens council
> (mechanic-fit / laugh-test / addiction) per game → a Chair per deck → one cross-game showrunner.
> Full record committed at **`descent/content/council/`** (`VERDICT.md`, `council-raw.json`,
> `FIX_<deck>.md` = the binding fix list per deck). All nine decks came back *fix-then-ship*;
> the cross-game view was harsher: avg addictiveness **6.2/10, no deck above 7**, because
> "every deck saves its worst-designed tier for its peak."
>
> **DONE from the council:**
> - **THE ARC BUG (its #1 finding, `da3e73d`)** — the spotlight gate (idx>=3) collided with the
>   forced opener/scatter/titlefight/bargain/finale slots, so confession/poison/redflag/alibi
>   (**432 cards, ~44% of the corpus**) could only be dealt at depth 9 — and the lobby defaults to
>   depth 7. Gate → idx>=2: depth 7 went **0% → ~100%** of nights dealing a spotlight game.
> - **titlefight** (`211fa86`) 10 → **32** duels, promoted to a funnel-managed deck (it is FORCED
>   into every depth>=7 night, so its 10-duel pool was the worst staleness risk).
> - **57-card cull** (`38844aa`) across 6 decks — the dead stretches, template dupes, no-target
>   absurdists, and one-joke-repeated summits.
> - **scatter re-lettering** (`6350829`) — 4 broken rounds (letters yielding <4 answers) fixed,
>   S-overload spread onto starved letters.
> - **overunder receipts** (`609f6a3`) — 8 re-pointed to real receipts, 10 duds cut.
>
> **THE WRITER ROUND IS DONE** (`fad5577`) — 8 parallel surgical revisers, one per deck, each
> implementing its `FIX_<deck>.md`. Unimplicated cards preserved byte-for-byte; every KEEPER lives.
> Corpus **952 → 1024**. Headlines:
> - **overunder: no-receipt cards 65 → 0** (the project's #1 fit defect — the receipt IS the game).
> - **roast: E4/E5 romance ~89% → 37%**; 12 gross binaries reframed to dispositional "Who WOULD"
>   and scattered so they never run in sequence; table-aware 14% → 27%.
> - **confession: E5 went 20/20 sexual → 8/20**; all 10 vague-withhold cards made concrete.
> - **fillin: C5 10 → 20**, all performance-framed (the flat peak is gone).
> - **poison: 36 cards re-cast as true two-sided dilemmas** (regex-audited, zero passive receipts);
>   all 6 melancholy B-sides killed.
> - **scatter: table-aware spikes in E2/E3 0 → 9** (the peak recurs instead of firing once).
> - **redflag: exposure inversion fixed**, intimacy/values register added.
> - **alibi: ~40 unpronounceable plants de-obscured** (the 7 survivors sit in decoy slots only,
>   never spoken); the 4 rigid category templates dissolved into ~25 rotating domains.
> - **Cross-deck verification (mine, not an agent's): corpus E4/E5 romance share = 15%**
>   (48/321), every deck under the council's 33% cap. The monoculture is broken.
> - **Typecast governor** (`bf7f091`) — roast's balloted victims now feed spotlight fairness.
> - Two council claims were **verified FALSE** and recorded in `content/council/VERDICT.md`:
>   titlefight fires 1x/night (not 2-3x), so the proposed **belt meta is N/A** on the current
>   per-night room model — do not build it.
>
> **REMAINING (owner-gated only):** the council's action #8 — **take it to a real table at depth 5
> AND 7** (D-128/D-138) before writing another card. Everything else is M4-M6 (Android shell + Play
> billing, beta deploy, domain + Stripe + Play release).

**Branch:** `descent` (NOT main). No upstream is configured; nothing from this branch has been
pushed. Current trust/safety work is intentionally still uncommitted for owner review.

**Build:** `cd descent && CI=true pnpm -r --config.verifyDepsBeforeRun=false build` → **GREEN**.
**Tests:** the matching recursive test command → engine **338**, client **105**, server **33** —
**476/476 green**. Content: **1,024/1,024** lint/dedup/stats clean. Live: five bots reached
JUDGMENT against local Wrangler/RoomDO/WebSockets on 2026-07-20.

**Repo layout:**
- Root docs: the 5 canonical `.md` files above.
- `descent/` — the new codebase (pnpm monorepo): `packages/engine` (pure TS game logic), `packages/server` (Cloudflare Worker + RoomDO), `packages/client` (Preact web client), `content/` (rebuilt decks + lint tools), `handoff/` (this handoff's transcript + proof).
- The OLD Android app is **frozen**, untouched, still in the repo root (`app/`, etc.). Do not maintain it. Cleanup is tracker task D-903 (much later).

**Milestones done in software:** M0–M3 implementation, all nine registered games, all client
screens, real corpus wiring, web Stage fallback, volunteer flow, fixed-timing spotlight burns,
fuzz/property tests, and local live-runtime smoke. Milestone acceptance that specifically requires
humans or physical devices remains open; do not convert software completion into fake playtest data.

**Content:** `descent/content/decks/` contains nine funnel-managed decks / **1,024 cards**. Lint
tools live in `descent/content/tools/`.

### ✅ THE ONE KNOWN FAILING TEST — FIXED (2026-07-19)
`descent/packages/server/test/protocol.test.ts` "full roast night" was a stale M1 test that assumed a roast-only arc; with M2 games registered the arc is mixed, so it hit a non-roast REVEAL. It is now **"full mixed night"**: it drives every registered game (over/under's blocking truth, roast redaction, fill-in) plus the scatter skip to JUDGMENT via `botlogic.ts`, and runs the D-113 sweep over the whole night — strictly stronger than before. Server is 28/28.

---

## 4. THE DOCTRINE — DECIDED QUESTIONS (do NOT relitigate without new evidence)

These were argued and settled with playtest/attack evidence. Reopening them wastes the validation.

- **Browser joiners, never a joiner app.** Host can be web (any phone, incl. iPhone) or an Android shell; joiners are always browser.
- **Server-authoritative Durable Object**, one per room. Phones (host included) are renderers. _Why:_ phones background/die/leave; and Android-only hosting would have killed ~75–80% of conversions (US under-35 skews iPhone) — the DO lets any phone host.
- **One UI runtime: the web client.** No game screen is ever built twice. The Android app is a thin WebView shell (Play Billing, stage audio, lift-to-sin sensor).
- **Monetization:** free for joiners forever, no ads, no subscription. New host's first full Night is free. **$9.99 one-time "Host the Descent" unlock, paywalled at the SECOND Night's BEGIN button** (moment of blocked intent), never mid-party.
- **Spice is dead; EXPOSURE + CHAOS replace it.** Exposure = how personal (capped by private consent). Chaos = how deranged (NEVER capped, NEVER low; `chaos >= 3` is a lint invariant). A low-exposure card must still be feral ("feral-but-impersonal"). Wholesome does not exist.
- **8 deep launch decks, not 12 thin ones.** The Unifying Theory is CUT. Title Fight is a physical arc-spike, not a rotation game.
- **Points go to the bold** (performers, writers, liars, subjects), not to voters who align with the majority (except Roast, where consensus IS the game). Prevents the predictable-vote metagame.
- **Consent is invisible.** Per-player heat ceilings are picked privately and NEVER rendered — no ceiling number appears anywhere, so nobody can be witch-hunted for capping the room.
- **On-device LLM is DEAD** (1.7GB models blocked Play publishing and never fired). AI is only an offline draft→human-curate content pipeline; never a runtime dependency, never the joke author, never a judge.

**Taste law (from repo `CLAUDE.md`):** UNHINGED, never WHOLESOME. Dive bar at 2am, affectionate violence. No Tailwind-default aesthetic, no purple/blue gradients, no corporate-quiz-app UI. Every screen must fail the test "could this be a SaaS dashboard?"

**Owner's cross-project hard rules (from `~/.claude/CLAUDE.md`):** $0 budget; no Upwork/Fiverr/marketplace bidding; AA anonymity (never reference recovery); premium taste, no AI-slop; never say "AI-powered" to customers; don't autonomously edit live sites.

---

## 5. HOW THE PREVIOUS SESSION WORKED (adopt this operating model)

- **Ultracode is ON.** Use the **Workflow tool** for substantive work — fan out parallel subagents with **disjoint file ownership** + an **integrator** pass that makes it build and wires seams. The M1/M2 workflows are the template; read them under the session's `workflows/scripts/`.
- **Engine purity law:** `packages/engine` is pure — **no `Date.now()`, no `Math.random()`, no I/O.** Time arrives inside events (`event.at`); randomness comes from the seeded `rng(seed)` in `rng.ts`. This is what makes every rule unit-testable and the night replayable. Never violate it.
- **The redaction law:** `packages/server/src/redact.ts` is the ONLY path from `RoomState` to the wire. It redacts per-socket; `NEVER_SERIALIZE` lists forbidden fields (heatCeiling, devilsBargain, votes, ...). Any new secret MUST be added there and asserted absent by a frame test.
- **The GameModule contract** (`packages/engine/src/games/module.ts`): a module owns one circle via
  `$deal`/`$spotlight`/`$phase` directives in gameState; the core feeds it `CORE_DEALT`,
  `CORE_SPOTLIGHT_DONE`, or `CORE_REVEAL_DONE`. `roast.ts` remains the non-burnable ballot reference;
  the six performer modules demonstrate the core-owned burnable assignment boundary.
- **Usage limits:** subagents die on session/credit limits repeatedly (this happened ~5×). The fix is **resume from cache:** `Workflow({ scriptPath, resumeFromRunId })` — completed agents replay instantly, only the killed ones re-run. Reset times were ~5h apart. Do not restart from scratch.
- **Commit discipline:** commit at every green checkpoint with the two-line trailer (Co-Authored-By + Claude-Session). Branch is `descent`. Do NOT push or open PRs unless the owner asks.
- **Content funnel:** every card passes `lint_deck.py` (invariants + banlist) → `dedup_skeletons.py` (skeleton budget + near-dupes) → `deck_stats.py` (quotas). Registers get normalized to the canonical enum with the original saved in a `voice` field.

---

## 6. WHAT TO DO NEXT (exact order — this is the live tracker head)

Confirm the build/test state yourself first (this env needs the CI workaround — see the
`helldeck-descent-dev-env` memory):
`cd descent && CI=true pnpm -r --config.verifyDepsBeforeRun=false build && CI=true pnpm -r --config.verifyDepsBeforeRun=false test`.

1. **Run D-128 with humans** at depth 5 and 7 if a group is available. Record interventions,
   confusion, actual laughs, and would-play-again; a bot night cannot answer those questions.
2. **Finish the adjacent card-preview safety seam:** accepted-burn acknowledgement, no optimistic
   success, deadline behavior, and private preview reconstruction on reconnect/RESYNC.
3. **Add product-relevant CI and lifecycle:** a `descent/` build/test/content workflow, RoomDO TTL /
   cleanup semantics, and tests. The root workflows currently target the frozen Android app.
4. **Build D-412/413 in test mode:** free-night device token, second-night BEGIN paywall, Stripe
   verify + signed unlock persistence. Live credentials, purchases, and deploy remain owner-gated.
5. Add crew memory/recent-card exclusion, then proceed to Android shell/beta only when the human
   playtest and owner-controlled infrastructure gates are satisfied. Keep this handoff current.

**Ephemeral note:** the previous session's design scratch (spec_v1/v2/v3, autopsy JSONs, playtest reports) lived in `/private/tmp/.../scratchpad/` and may be gone. Everything durable was distilled into the 5 committed docs + the transcript render. You do not need the scratch.

---

## 7. THE FULL CHAT HISTORY

- `descent/handoff/SESSION_TRANSCRIPT.readable.md` — every user + assistant turn, tool calls compressed to one line, long results truncated. **This is your learning text.**
- `descent/handoff/SESSION_TRANSCRIPT.raw.jsonl` — the faithful, complete 2.4MB log (exact bytes) if you ever need to verify what was actually said or done.

Read the readable render end-to-end during onboarding (§1). It contains the reasoning you cannot reconstruct from the specs alone: the playtest failure modes, the attacker kill-shots, the pivots, and why each fix is shaped the way it is.

---

_Handoff authored at the owner's request when the session hit its usage limit. Next session: Opus 4.8. The bar is: finish it as if you'd designed it. Now go write `ONBOARDING_PROOF.md`._
