# ⛔ NEXT AGENT — READ THIS FIRST, THEN `HELLDECK2_HANDOFF.md`

Originally written 2026-07-20 and updated after the UI/UX hardening pass.
Active branch **`main`**. The Descent import and trust/safety baseline are published to
`origin/main`; do not assume the older `descent`-only/no-push notes are still current.

> **⚡ 2026-07-21 UPDATE (Opus 4.8) — MONETIZATION (D-412/413) IS DONE in test mode, plus `descent/`
> CI landed.** The "biggest gap" below (§2 item 1, "MONETIZATION IS 0%") is CLOSED end-to-end and
> taste-approved. Read the authoritative account in `HELLDECK2_HANDOFF.md` §3 (2026-07-21 paragraph)
> and the checked D-412/D-413 in `DESCENT_BUILD_SPEC.md` Part 12. Uncommitted on `main` as of this
> writing — the owner commits when they choose. What's still open (see §2, revised): real human
> playtests (owner-gated), `STRIPE_SECRET` live key (owner-gated), deploy/domain/Android/store
> (owner-gated), and the remaining autonomous ops — **room TTL/expiry** (the `ROOM_EXPIRED` protocol
> code is still unwired) and **crew-memory persistence**. The §4 OpenArt asset directive is unchanged
> and still needs owner sign-off before generating anything.

---

## 1. THE OWNER'S MANDATE FOR YOUR SESSION

Verbatim intent, as given:

> Get this app **fully functional**, with a **new GUI if one is needed**, where **all new visual
> assets are generated using ONLY the OpenArt API**. By the end of your work it should be
> **fully ready for testing — basically a finished product.**

Read §4 before you generate a single asset. There are three hard conflicts in that directive that
you must resolve with the owner or design around. Do not blindly generate 200 images.

---

## 2. WHERE THE APP ACTUALLY IS

**HELLDECK "The Descent"** — a phones-only, no-TV party game. 3–12 friends, each on their own
phone. Server-authoritative: one Cloudflare Durable Object per room runs a pure-TS engine; every
phone (host included) is a thin renderer over WebSocket. Code lives in `descent/` (pnpm monorepo:
`packages/{engine,server,client}` + `content/`).

### DONE and verified ✅

| layer | state |
|---|---|
| Engine (pure TS, deterministic) | **342 tests.** Adversarial bot-fuzz plus core-owned, burnable spotlight and card-preview ceremonies |
| Server (Worker + RoomDO) | **34 tests.** Snapshotting, alarms, per-socket redaction, CLAIM, private spotlight/card replay |
| Client (Preact, hand-rolled CSS) | **123 tests.** All 9 games + acknowledgement-safe private UI + Stage privacy gating |
| Content | **1024 cards / 9 decks**, all funnel-clean, council-reviewed and remediated |
| Consent + fairness | heat ceilings, volunteer valve, 20s fixed-timing spotlight burns/replacements, typecast governor |
| Live runtime | **Verified locally 2026-07-20.** Five WebSocket bots completed a real depth-5 Wrangler/RoomDO night to JUDGMENT in 360.1s |

**499 tests green. `pnpm -r build` clean.** All 9 games play end-to-end against the real corpus.

### REAL REMAINING WORK — this is your actual work list

1. ~~**MONETIZATION IS 0%.**~~ **DONE 2026-07-21 (test mode).** The hardcode is gone: entitlement
   is re-resolved against the host DEVICE at every BEGIN; per-device `LedgerDO` holds the one free
   night; the paid path is a stateless device-bound HMAC unlock in localStorage; `worker.ts` serves
   `/api/entitle/{status,checkout,verify,dev-unlock}` with real Stripe test-mode Checkout + verify;
   the UNHINGED paywall overlay (`screens/paywall.tsx`, taste-approved) opens on `NO_ENTITLEMENT`.
   **Only owner-gated tail left: set `STRIPE_SECRET` to a live key.** (test mode + non-prod dev-unlock
   work now.) See `HELLDECK2_HANDOFF.md` §3.
2. **NEVER PLAYED BY A HUMAN.** The live bot night is real infrastructure evidence, not fun or
   usability evidence. D-128/D-138 remain the most important product gate. See §5.
3. **Operational lifecycle is partly done.** `descent/` CI now exists (`.github/workflows/descent-ci.yml`
   — build+test+content-funnel, with `allowBuilds` committed to `pnpm-workspace.yaml` so a fresh
   install is non-interactive). **Still open (autonomous):** room TTL/expiry (the `ROOM_EXPIRED`
   protocol code is defined but never emitted — rooms live forever in DO storage; wire a TTL check in
   the RoomDO + an alarm-driven cleanup, and mind the delicate alarm-multiplexing in `room-do.ts`),
   and crew-memory persistence.
4. **No deploy, no Android shell, no domain, no store listing** (M4–M6).

---

## 3. THE GUI — READ BEFORE YOU REPLACE ANYTHING

**There is already a complete, taste-passed design system.** It is not a placeholder:
- `packages/client/src/style/style.css` + `games.css` — hellfire palette (`--pit`, `--blood`,
  `--ember`, `--bone`, `--ash`, `--char`), a self-hosted Barlow Condensed subset, **sharp corners
  everywhere**, reveals that SNAP, holds that BREATHE, reduced-motion and mobile/safe-area rules.
- All imagery today is **inline SVG** (`screens/bits.tsx`: `Devil`, `Flame`, `Crown`, `Ring`) —
  **the app currently ships ZERO raster assets.**
- Every screen was reviewed by an adversarial `taste-critic` agent and cleared. Two defects were
  found and fixed; the rest was rated DISTINCTIVE.

So: **"new GUI if needed" is a judgment call, not a mandate.** Evaluate honestly. Replacing a
taste-passed system with generated imagery can *regress* quality. If you do rebuild, you must clear
the same bar (§6).

---

## 4. ⚠️ THE OPENART-ONLY ASSET DIRECTIVE — THREE CONFLICTS TO RESOLVE FIRST

The owner wants all new visual assets generated via the OpenArt API. Before you generate anything:

**CONFLICT 1 — the project's own taste law bans generic AI imagery.**
`CLAUDE.md`'s Anti-Pattern Registry explicitly BANS *"Hero sections with stock-photo-style AI
imagery"* and *"Geometric abstract illustrations as hero backgrounds."* The owner's global rules
add *"premium taste, no AI-slop"* and *"never say 'AI-powered' to customers."*
→ These are not contradictory if you art-direct hard: use the API for a **deliberate, narrow,
style-locked set** (devil/imp iconography, card backs, the descent depth art, grain/soot textures)
driven by one written style spec — **not** decorative hero slop. Every asset must serve meaning
(the taste law's rule 4: *"It signals X to the user because Y"*). Generic output = ban violation.

**CONFLICT 2 — you almost certainly have no API key, and the budget is $0.**
OpenArt API access needs an account and likely payment. The owner's standing rule is a **$0
budget**. **You cannot purchase this.** Check for credentials first; if absent, STOP and ask the
owner rather than silently substituting another tool (the directive says *only* OpenArt) or
faking assets.

**CONFLICT 3 — the performance budget.**
This is a phones-only game played on **bar wifi**. Spec 6.3 works to a ~200KB asset budget; the
client currently ships ~26KB CSS + ~182KB JS and **no images at all**. Raster art is a real
regression risk on the exact network this game lives on. If you add imagery: WebP/AVIF, aggressive
sizing, lazy-load anything not on the critical path, and **re-measure the bundle**. A gorgeous app
that takes 8s to load in a loud bar has failed.

**Recommendation:** get owner sign-off on (a) credentials, (b) a written style spec, and (c) an
asset budget, before generating at volume.

---

## 5. WHAT "READY FOR TESTING / FINISHED" HONESTLY REQUIRES

Be straight with the owner about this — do not claim "finished" without it.

**You CAN do:**
- Re-run the verified `wrangler dev` + live bot path and run a multi-phone LAN night.
- Build entitlements end-to-end (D-412/413): device free-night token, paywall UI at the 2nd BEGIN,
  Stripe checkout + verify + HMAC unlock token surviving a browser restart. Use Stripe **test
  mode** — live keys are owner-gated.
- GUI/asset pass per §3–4.
- Fix anything the first real playtest surfaces.

**You CANNOT do (owner-gated — say so plainly, never fake it):**
- **Real playtests** (`D-128`, `D-138`) — needs actual humans in a room. **This is the true next
  gate and it is not yours to close.**
- Cloudflare **Workers Paid** + deploy (`D-802`), **domain** purchase (`D-801`), **Stripe live**
  keys, **Play Console** release (`D-803`), Android shell signing.

So the realistic definition of done for the next major session: **a live-runnable, monetization-complete,
art-directed build that a real group could test tonight on a LAN or a beta deploy** — with the
store/live-payment tail still owner-blocked.

---

## 6. HOW TO WORK IN THIS REPO (learned the hard way)

**Build/test** (the `CI=true` and the flag are both required — no TTY here):
```
cd descent
CI=true pnpm -r --config.verifyDepsBeforeRun=false build
CI=true pnpm -r --config.verifyDepsBeforeRun=false test
```
⚠️ Run these **from `descent/`**, never the repo root — the root has a frozen legacy `webui`
package whose test script runs a failing e2e suite.

**Content funnel** — any card change must pass all three, and cross-deck dedup is authoritative:
```
cd descent/content
python3 tools/lint_deck.py decks/*.json
python3 tools/dedup_skeletons.py decks/*.json      # cross-deck; run over ALL decks
python3 tools/deck_stats.py decks/*.json
```
If you change deck sizes, update the count assertion in `packages/server/test/content.test.ts`.

**Taste gates** — this project runs adversarial review. Use the `taste-critic` / `taste-auditor`
subagents on any new UI or copy. They have persistent memory under
`descent/content/.claude/agent-memory/taste-auditor/` including a standing finding: *the
"protagonist secretly ranks/files/scores the group" premise is a banned default*, and *hand-written
batches drift WHOLESOME in the mild E2/E3 middle — audit that tier hardest.*

**The card council** — a 37-agent review (3 lenses/game → chair → showrunner) is committed at
`descent/content/council/`: `VERDICT.md`, `council-raw.json`, and `FIX_<deck>.md` per game. Its
verdict was **fix-first**; that remediation is now DONE (see §7). `VERDICT.md` also records two of
its claims that I **verified FALSE** — read those before acting on it.

**Engine law:** `packages/engine` has NO `Date.now`/`Math.random`/IO — time is `event.at`,
randomness is seeded `rng(seed)`. `packages/server/src/redact.ts` is the ONLY path to the wire;
secrets go in `NEVER_SERIALIZE`. Breaking either will fail the fuzz suite.

**Landmines:**
- Subagents hit a session rate limit mid-flight once and died. They left the repo intact (git is
  your rollback), but **check `git status` after any parallel agent round.**
- Parallel deck agents racing on `dedup_skeletons.py decks/*.json` can read a half-written file —
  have each dedup **only its own deck**, and run the cross-deck pass yourself at the end.
- **`DESCENT_BUILD_SPEC.md` Part 12's checkboxes are ALL unchecked and badly stale** — including
  D-101, which passes right now. It reads as "nothing done" when M0–M3 are complete. Trust
  `HELLDECK2_HANDOFF.md` §3 instead. **Syncing Part 12 is a genuinely useful, unclaimed task.**

---

## 7. WHAT JUST LANDED (so you don't redo it)

**2026-07-20 granular UI/UX + private-card hardening pass:**

- Replaced the unchecked/optimistic card-preview cast with a strict assigned/released protocol,
  correlated private burn acknowledgement, deadline state, and reconnect/RESYNC replay of only the
  current viewer's still-live unburned preview. Public timing remains fixed and silent.
- Hardened Stage: private decisions show a safe countdown while flat, lift permission is revoked on
  every context or own-ballot acknowledgement, and roster size is re-derived at BEGIN so stale lobby
  config cannot select the wrong Stage mode.
- Added modal focus containment/background inertness, keyboard-safe hold controls, named avatar and
  flame controls, persistent non-color selection rails, 48px targets, readable contrast tokens,
  reduced-motion/forced-colors support, dynamic viewport and safe-area handling.
- Added honest pending/error/cancel states for room creation, ceiling sealing, card/spotlight burns,
  and share sheets; disconnected sends no longer paint false locked ballots. Invalid room recovery,
  code normalization, passive-role timers, clearer corner/vote copy, and full ladder deltas also landed.
- Self-hosted the licensed Barlow Condensed Latin subset. Production JS is **59.43 KB gzip**; fonts
  plus JS/CSS remain below the binding 200 KB mobile asset budget. Visual smoke passed at 320×568,
  390×844, and 844×390.
- Verification: engine **342**, server **34**, client **123** = **499 tests**, strict recursive build,
  and `git diff --check` green. Human/device playtests remain open; screenshots are not a substitute.

**2026-07-20 trust/safety + live-runtime pass:**

- Wired the client-visible `CLAIM` button through the server protocol. Spoofed identity/time are
  ignored; a real RoomDO test proves only the claimant sees their own volunteer flag.
- Added a transient, dismissible, accessible in-room server-error banner. Rejected actions no
  longer disappear silently after joining.
- Closed the Stage leak: card previews and spotlight roles now mount inside `StageShell`; a flat
  host phone shows only generic `PICK UP TO SIN`, and phase/sub/circle/payload/role changes revoke
  lift synchronously.
- Implemented the missing spotlight Brimstone valve across Over/Under, Confession, Red Flag,
  Alibi, Poison, and Title Fight. Core runs a fixed T+10/T+20 private ceremony; burns get a private
  acknowledgement; replacements get a full 10 seconds; public timing never changes; reconnect /
  RESYNC reconstructs only the current viewer's role; final assignees alone affect fairness.
- Verified strict build, **476/476 tests**, all 1,024 content cards, and a real five-client
  `wrangler dev` night to JUDGMENT in **360.1s**. No deploy or external purchase was performed.

**Earlier 2026-07-19/20 work:**

Roughly: finished the M3 tail, then ran a card council and executed its remediation.

- **D-136** all 5 remaining game screens → every one of the 9 games is human-playable.
- **D-134** `spotlightCount` channel **and** the "WHO WANTS BLOOD?" volunteer valve.
- **D-137** adversarial bot-fuzz.
- **D-135** THE STAGE + lift-to-sin **web fallback** (manual flip; the accelerometer is M4).
- **W-2 content** top-up, then the council remediation. Corpus **793 → 1024**.
- **THE ARC BUG** (the council's #1 find, and the most important thing I fixed): the spotlight
  gate `idx>=3` collided with the forced opener/scatter/titlefight/bargain/finale slots, so
  **confession, poison, redflag and alibi — 432 cards — could only ever be dealt at depth 9**,
  while the lobby defaults to depth 7. Gate → `idx>=2`. Depth 7 went **0% → ~99%**. If you touch
  `arc.ts` placement, **re-measure per-depth reachability** with a temp probe calling `buildArc`
  over ~300 seeds/depth. A gate that reads harmless can silently orphan half the content.
- **Typecast governor**: roast's victims are elected by ballot, so spotlight fairness never saw
  them; they now feed the same channel so other games lean away from a repeatedly-named player.

Verified outcomes: overunder no-receipt cards **65 → 0**; corpus E4/E5 romance share **15%**
(was ~89% in roast alone); confession's E5 **20/20 sexual → 8/20**; titlefight **10 → 32** duels.

**Caveat I want you to inherit honestly:** those are *measured fixes to named defects*, not new
scores. The council's "6.2/10 addictiveness, no deck above 7" came from reading the OLD decks.
Nobody has re-scored the revised corpus. **Re-running the council against it would give a genuine
before/after** — worth doing, and worth ~1.5M tokens.

---

## 8. SUGGESTED ORDER FOR YOUR SESSION

1. `git log --oneline -15`, read `HELLDECK2_HANDOFF.md` §3, confirm `build` + `test` are green.
2. If humans are available, run D-128 immediately. Otherwise add `descent/` CI and define/test
   RoomDO expiry before expanding features.
3. Build D-412/413 entitlements in Stripe test mode; keep live keys and purchases owner-gated.
4. Resolve the §4 asset conflicts with the owner (credentials, style spec, budget) **before**
   generating.
5. GUI/asset pass only if real-phone/playtest evidence identifies a weakness; taste-gate and
   re-measure the bundle.
6. Update `HELLDECK2_HANDOFF.md`, keep `DESCENT_BUILD_SPEC.md` Part 12 honest, and write the next
   `NEXT_AGENT.md`.

Publishing is no longer categorically forbidden: the owner explicitly authorized `main` for this
pass. Still do not deploy, buy anything, or enable live payments without fresh owner authorization.
Tell the owner exactly what is real, what is stubbed, and what still needs their hands.
