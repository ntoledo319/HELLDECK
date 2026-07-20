# ONBOARDING_PROOF.md — Opus 4.8 session, 2026-07-19

Written to satisfy the §1 LEARN gate in `HELLDECK2_HANDOFF.md`. I read all 5 canonical
docs + this handoff, verified the live build/test/content state myself, then answered the
20 active-recall questions below. Because I hold all the docs in context at once, my
"corrections" pass is not a memory-gap audit — it is a **ground-truth audit**: every place
where the live repo differs from the handoff's stated snapshot, found by running the build,
the tests, and counting the decks. Those deltas are the load-bearing part for the next agent.

---

## The 20 answers

**1. Purpose of each canonical doc.**
- `HELLDECK2_HANDOFF.md` — the onboarding protocol + current-state pointer (the door).
- `HDRealRules2.md` — the game's WHAT: rules of the Night, the 8 launch games, consent, pacing, monetization (canonical; supersedes v1).
- `CARD_AUTOPSY.md` — the evidence: why 86% of the old 864-card corpus fails at a table (the pathology that justifies the content law).
- `CONTENT_BIBLE.md` — the binding writing law: Voice, two-axis E/C rating, the Ten Commandments, per-deck laws, the funnel.
- `DESCENT_BUILD_SPEC.md` — the HOW: architecture, data model (Part 2 verbatim types), protocol (Part 3), night engine (Part 4), per-game specs (Part 5), content system (Part 8), and the Part 12 delivery tracker.

**2. Why server-authoritative DO, not host-phone.** Phones background, die, and leave (kitchen runs, dead batteries, tab eviction under iOS memory pressure) — a host-phone brain loses the whole room when the host's phone hiccups. The **business fact** that forced it: hosting can't be Android-only, because US under-35 skews iPhone; an Android-only host would have killed ~75–80% of conversions. One Cloudflare Durable Object per room lets **any** phone host (web), and every phone — host included — is a thin renderer over WebSocket.

**3. The two card axes.** EXPOSURE (how personal — what a truthful answer confesses about a present person; this is what consent ceilings cap) and CHAOS (how deranged — feral imagery/absurdity). **CHAOS is never allowed to be low** (`chaos >= 3` is a lint invariant, C≥3 at every exposure tier). Why: the whole game is "UNHINGED, never WHOLESOME." Capping exposure removes the knife (protects the shy player), but the night must stay feral regardless — "feral-but-impersonal" (E1/C5: shovel-bleach-tarp) is the backbone of low-E play. A low-C card could open a corporate offsite; that card does not exist here.

**4. The 8 launch games / 4 Drop-1 / the cut.** Launch 8: Roast Consensus, Fill-In Finisher, Over/Under, Confession or Cap, Scatterblast, Poison Pitch, Red Flag Rally, Alibi Drop. Content Drop 1: Text Thread Trap, Reality Check, Taboo Timer, Hot Seat Imposter. **CUT: The Unifying Theory** (highest cognitive load, worst dead-air risk — its best trios salvaged into Poison Pitch angles and Scatterblast categories). Title Fight is not a rotation game — it's a scheduled physical arc-spike.

**5. Perform-then-vote (Fill-In).** Writers write punchlines to a setup; answers go ONLY to the Reader's teleprompter; the Reader performs every answer **aloud** first (first exposure is always out loud, in a chosen read-tone), and only AFTER the last read does the vote grid unlock. The naive order (vote before performance, or answers shown on all phones) is fatal because it turns the Reader into a laugh track and lets players read silently — the comedy only exists in the performance; silent-funny is a tweet, cut it.

**6. Blocking input.** An input that cannot be auto-played without fabricating a fact — a truth-lock, a receipt number, a role-ack. Two examples: Over/Under's receipt TRUTH number; Confession's TRUE/FALSE lock. **Law: the engine must NEVER auto-fabricate a truth value** for a blocking input. It pauses the timer (deadline = null) with a public shame countdown and terminal states (DRAG THEM BACK / FEED THEM TO THE PIT vote, host VOID, 90s seat lapse, always-available PLEAD THE FIFTH). Test D-115 guards this.

**7. Deal ceremony / byte-identical burn.** A fixed 5.5s ritual on all phones ("THE DECK IS CHOOSING ITS VICTIM…") that absorbs exactly one Brimstone burn with a backup card pre-fetched. A burned deal must be timing-identical to a clean deal (same ceremony length, no animation delta, burn not visibly decremented, not attributably in telemetry) so that **nobody can tell "capped/burned" from "that's what the deck dealt"** — otherwise a burn becomes a social signal and the safety valve becomes a witch-hunt trigger.

**8. Engine purity law.** `packages/engine` is pure: deterministic given (state, event, seed), zero I/O. **Three banned JS built-ins: `Date.now()`, `Math.random()`, and any I/O (sockets/storage/fetch).** Time arrives inside events (`event.at`); randomness comes from the seeded `rng(seed)` in `rng.ts`. This is what makes every rule unit-testable and the whole night replayable from an event log; the DO owns all I/O.

**9. `redact.ts`.** The ONLY path from `RoomState` to the wire; it computes `STATE`/`PATCH` **per socket**. The one rule: every secret must be stripped here and asserted absent by a frame test — secrets travel only via `PRIVATE` to the entitled socket. Three `NEVER_SERIALIZE` fields: `heatCeiling`, `devilsBargain` (holder, until Judgment), and cast `votes`/ballots. (Also: role assignments, pre-view cards, alibi words, taboo words except clue-giver+buzzer, truth-locks.)

**10. GameModule contract.** `packages/engine/src/games/module.ts`: each game exports `{ deck, minN, start, input, timer, control, view }`. A module owns one circle's loops; core owns the Night. `$deal`/`$phase` are the directives by which a module drives its sub-phases inside `gameState`; `CORE_DEALT` and `CORE_REVEAL_DONE` are synthetic timer ids the **core** feeds back into the module (`module.timer`) so the core can run the shared deal ceremony and reveal-hold on the module's behalf without the module scheduling real DO alarms for them. `roast.ts` is the reference implementation; new games copy its plumbing.

**11. Who scores (anti-metagame).** Points go to **the bold**: performers, writers, believed liars, planted-word survivors, subjects. Voters get a flat +1 for participating, **never** a bonus for aligning with the majority — otherwise the optimal strategy is "vote with the crowd, risk nothing," and a player who never performs could win the night. **Exception: Roast Consensus**, where consensus IS the game (majority-alignment scoring + Room Heat).

**12. Lift-to-sin.** In Stage mode the host phone lies face-up mid-table showing PUBLIC-only faces. "NICK — PICK UP TO SIN" → lifting the phone flips it to a shielded private ballot (thumb-hold to reveal), laying it flat restores the public stage. It resolves the contradiction between "the phone is a shared public display on the table" and "this input is a secret ballot that must not be seen by the table."

**13. PLEAD THE FIFTH.** Always available to the subject of a blocking input: forfeit the round's points, take an outward sting ("THE WITNESS TAKES THE FIFTH. COWARDICE NOTED."), costs nothing else. It solves "a blocking input can trap a real human who can't/won't answer" — the engine must never fabricate ground truth, so the human always has a dignified, point-only exit.

**14. Private heat ceiling.** Each player privately picks a 1–5 ceiling ("how hot can cards about YOU run?"); it's a silent server-side deck filter (a card naming subject X never exceeds X's ceiling). **Its value is never rendered anywhere** — no number, no "capped" indicator — so nobody can tell a capped deal from a normal one and no one gets witch-hunted for capping the room. (Consent is invisible.)

**15. Fresh Meat / Imp citizenship.** Fresh Meat: a first-night-with-this-crew player is excluded from knowledge-dependent subject roles (Hot Seat, Reality Check, personal-history Over/Under) and draws first-impression card variants. Imps: latecomers/overflow (join after LOBBY or past 12) are **citizens, not ghosts** — their names enter the {NAME} pool (roastable), they FIRE, they vote at 0.5 weight, contribute a curated cameo answer in N≥7 Fill-In, and convert to full players at circle boundaries in join order; residual imps get a guaranteed Judgment superlative.

**16. Monetization / paywall location.** Free for joiners forever (no ads, no subscription). A new host's **first full Night is free**. **$9.99 one-time "Host the Descent" unlock**, paywalled at the **SECOND Night's "BEGIN THE DESCENT" button** — the moment of blocked intent, lobby already assembled behind it — **never mid-party**. The Judgment share card seeds the loop (every joiner leaves with a "host your own descent" link).

**17. Git HEAD / the one failing test / why.** HEAD = `b330686` (branch `descent`, nothing pushed). The one failing test: `packages/server/test/protocol.test.ts` → "full roast night: frames carry the module view and never a ballot", failing `expected undefined to be defined` at line 527 (`expect(v['spread']).toBeDefined()`). Why: it's a **stale M1 test** that assumed a roast-only arc. Now that fillin/overunder/confession are registered, `buildArc` yields a **mixed** arc, so the first `sub:'REVEAL'` the scripted bot reaches is a **non-roast** reveal (which has no `spread`). Not a leak, not a bug — the M2 integrator's job.

**18. Exact next task.** M2-INT: fix that stale test and extend `packages/server/test/bots.ts` so a bot night drives ≥1 fillin, ≥1 overunder, ≥1 confession circle (plausible inputs incl. a blocking truth number); verify each module's `view()` discriminants match the client components in `packages/client/src/screens/{fillin,overunder,confession}.tsx` + `games/wire.ts`; add an engine test simulating a depth-7 N=6 night across all 4 registered games asserting **no skipped circles**; then commit.

**19. Owner rules + taste law.** Owner cross-project: $0 budget; no Upwork/Fiverr/marketplace bidding; AA anonymity (never reference recovery/AA); premium taste, no AI-slop; never say "AI-powered" to customers; don't autonomously edit live sites. HELLDECK taste law: **UNHINGED, never WHOLESOME** — dive bar at 2am, affectionate violence; no Tailwind-default aesthetic, no purple/blue gradients, no corporate-quiz UI; every screen must fail "could this be a SaaS dashboard?"

**20. Workflow-resume on usage death.** Subagents die on session/credit limits; do NOT restart from scratch. Resume from cache: `Workflow({ scriptPath, resumeFromRunId })` — completed agents replay instantly from the journal, only the killed ones re-run (same script + same args → 100% cache hit). Reset windows were ~5h apart.

---

## CORRECTIONS — ground truth verified against the live repo (2026-07-19)

Ran `pnpm -r build` (green, all 3 packages; client bundle 37.97 KB gzip) and `pnpm -r test`.
Deltas from the handoff's stated snapshot:

- **Git HEAD moved:** handoff §3 says HEAD `66b057f`; actual HEAD is **`b330686`** ("Session handoff…"), one commit later — that commit only added the handoff/transcript/proto docs, no code. Working tree clean.
- **Test counts grew** (handoff said 330/331): actual today = **engine 303 pass**, **client 65 pass** (logic 31, wire 10, coalesce 5, qr 15, ws 4), **server 27 pass / 1 fail**. Total **395 pass, 1 fail** — same single known failure, more coverage than the handoff snapshot.
- **Content decks (Wave 1)** actual card counts: roast **154**, fillin **163**, confession **146**. `overunder.json` is **absent** — Wave 1's 4th deck is genuinely not written yet (matches handoff: over_under writers died on credits).
- **pnpm gotcha (new, environment-specific):** `pnpm -r build/test` aborts before running because pnpm's pre-run deps-status check wants to purge/reinstall `node_modules` (ignored build scripts: esbuild/sharp/workerd) and fails with no TTY. Workaround that WORKS here: `CI=true pnpm install --frozen-lockfile --config.confirmModulesPurge=false` once, then run recursive scripts with `--config.verifyDepsBeforeRun=false`. Network IS available in this session (frozen install fetched fine).
- **Registered games:** the M2 pool the arc builder can currently draw from is exactly **{roast, fillin, overunder, confession}** (games/ dir). scatter/poison/redflag/alibi/titlefight are NOT yet implemented, so the arc's physical-spike slots have no scatter/titlefight to place — the arc builder degrades to the available pool. This is why the depth-7-all-4-games engine test (task 18) must assert "no skipped circles" against a 4-game pool, not an 8-game one.
- **Memory not loaded:** the durable memory file `helldeck-2-descent-pivot.md` referenced in the handoff is NOT present in this session's memory dir (`…/-home-nick-Development-active-HELLDECK/memory/` is empty). MEMORY.md is empty too. I will re-establish the memory from the handoff as part of this session's closeout.

---

## Teach-back — the mental model for a third agent (5 bullets)

1. **Two brains, one truth.** The engine is a pure reducer `reduce(state, event, seed) → {state, effects}` (no clocks, no random, no I/O); the DO is the only thing that touches sockets/storage/alarms and turns effects into wire frames. Everything testable, everything replayable. Never leak I/O into the engine or rules into the DO.
- 2. **One circle = one GameModule; core owns the Night.** Games are plug-ins with a fixed 7-method contract; they never invent plumbing — they copy `roast.ts`. The core runs the shared deal ceremony and reveal-hold via `CORE_DEALT`/`CORE_REVEAL_DONE` so every game's timing (and every burn) is identical.
3. **Secrets die at one door.** `redact.ts` is the sole path to the wire, per-socket; any new secret is added to `NEVER_SERIALIZE` and asserted absent by a frame test. Consent (ceilings) is invisible by construction — that invisibility is a game-design feature, not just privacy.
4. **Content is a law, not a vibe.** Every card is E/C-rated with C≥3, filmed-scene-not-verdict, room-answers-not-card, deduped by skeleton, run through lint/dedup/stats. The autopsy is the proof that breaking the law kills the game; the CONTENT_BIBLE is the law.
5. **The doctrine is settled — build, don't relitigate.** Server-authoritative, browser joiners, one web UI, $9.99 at 2nd BEGIN, E/C over spice, 8 deep decks. Ship against the Part 12 tracker; parallelize with workflows using disjoint file ownership + an integrator pass; resume-from-cache when a run dies on credits.
