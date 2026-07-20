# HDRealRules2.md — THE DESCENT

## The canonical rules for HELLDECK 2.0

_Successor to HDRealRules.md. That document described a pass-the-phone game; this one describes a possession._

_Every player on their own phone. Same room. No TV. One person pays; everyone else walks in through a browser. Screens carry secrets and votes — mouths carry the comedy._

**Design provenance:** this ruleset survived 9 simulated playtests (6 group archetypes + 3 re-validations) and 4 adversarial audits (pacing, social dynamics, technical reality, business). Final round: bar-table group 8/10 FUN, 13-player chaos party 7.5/10 FUN. Every rule below that looks weirdly specific exists because a simulation broke without it.

---

# PART I — THE NIGHT

## 1. Joining

- The host opens HELLDECK (web or Android app), starts a **Night**, gets a 4-letter room code + QR.
- Everyone else scans the QR or types the code into their phone browser. Name + avatar. No install. No account. Joiners are always free.
- The host plays too. Anyone can host from any phone (the game brain lives server-side).
- 3–12 players. Latecomers and overflow become **Imps** (see §9).

## 2. The Descent

A Night is a descent through **Circles**. One circle = one mini-game round, 3–4 minutes.

| Depth | Circles | Runtime (gameplay) |
|---|---|---|
| **Quick Dip** | 5 | ~17–22 min |
| **Standard Descent** | 7 | ~24–30 min |
| **Full Damnation** | 9 | ~28–36 min |

- Depth options are gated by pool arithmetic: at 3 players the lobby caps at Standard, with the warning: _"9 circles for 3 sinners means the wheel comes back around — bring more bodies for Full Damnation."_
- **The deeper you go, the hotter it gets.** Content heat ramps circle by circle (the 36-questions escalation curve). The finale is the most personal content the room's consent allows.
- Single-performer games run **2 loops with 2 different performers** per circle — keeps circles 3–4 min and doubles spotlight spread.

## 3. Consent (invisible, unhinged-preserving)

Two axes, decoupled — this is load-bearing for the whole content system:

> **Spice caps EXPOSURE (how personal), never CHAOS (how deranged).**
> A consent-capped night is still feral. The cap removes the knife, never the chaos.

- During join, every player privately picks their **heat ceiling** (1–5) on their own phone.
- A card **naming subject X never exceeds X's own ceiling.** The one shy player is fully protected; the feral majority still gets a feral night about each other.
- Generic (no-subject) cards obey the room ceiling: second-lowest pick (lowest at N≤4).
- Vote-emergent-victim games (Roast Consensus) obey the lowest pick above exposure 3.
- **No ceiling number is ever rendered anywhere.** The ceiling is a silent server-side deck filter. Nobody can tell "capped" from "that's what the deck dealt." No witch hunts.
- **Vibe check** (host, at lobby): SOBER / WARMED UP / FERAL sets the starting rung. Precedence: ceiling always wins; start = min(vibe rung, ceiling − 2), floor 1 — every night gets at least two escalation steps.
- 18+: the join page carries a one-tap 18+ attestation; the host attests the room at Night creation; depth 4–5 requires it.

## 4. The arc

The shuffler builds each Night's game sequence under these laws:

- Open with simultaneous-input games (everyone acts at once, no spotlight pressure).
- Physical spike mid-descent (Scatterblast; Title Fight is the second spike).
- Spotlight games only after the room is warm.
- **Anti-roast-fatigue grammar:** after circle 4, never two subject-targeting circles back-to-back; at least one aim-OUTWARD circle (Scatterblast, Taboo, Poison Pitch, Red Flag, Title Fight) per third of the night. A palate cleanser isn't wholesome — it's the room uncoiling so the next hit lands.
- N-aware: every game carries minN/maxN; the shuffler never deals a mathematically broken game.
- Repeats allowed at small N for proven repeaters (Roast, Scatterblast, Over/Under) with fresh content. Never repeat single-performer games.

## 5. The finale and THE JUDGMENT

- Final circle = **×3 points**, restricted to all-players-score games (Roast Consensus, Fill-In Finisher, Over/Under; at N≤4 prefer Reality Check or Over/Under). In finale Fill-In, last place is never dealt Reader, and the Reader earns performance points (🔥 during the read converts) — nobody is excluded from the climax.
- **Devil's Bargain:** last place secretly carries ×2 on a late all-players-score circle. Revealed with drama at Judgment ("SAM SOLD THEIR SOUL AT CIRCLE 6"), never silently applied.
- **THE JUDGMENT:** winner crowned + superlatives generated from the night's real telemetry (Most Roasted, The Liar, Fastest Fingers, Biggest Ego Gap…). A screenshot-shaped **share card** lands on every phone — each joiner's copy carries a "host your own descent" link.
- No elimination, ever. Option to descend again.

---

# PART II — HOW ROUNDS RUN

## 6. Pacing laws (server-enforced)

1. One task per phone per moment. All input is simultaneous. No sequential turns anywhere.
2. **SKIPPABLE inputs** (votes, punchlines): timer fires → auto-abstain / auto-panic-answer. The game never waits for stragglers.
3. **BLOCKING inputs** (truth-locks, subject numbers, role acks): timer pauses with a public shame countdown ("WAITING ON SAM'S RECEIPTS…"). Every blocking input has terminal states:
   - 30–45s → the room votes **DRAG THEM BACK or FEED THEM TO THE PIT** (60% = round voided, mocking sting, re-deal a no-subject game), and the host gets **VOID ROUND**.
   - Seat expiry (90s disconnected) auto-voids any blocking input it owns.
   - **PLEAD THE FIFTH** is always available to the subject: forfeit round points, outward sting ("THE WITNESS TAKES THE FIFTH. COWARDICE NOTED."), costs nothing. Never fabricate ground truth; never trap a human.
4. **Reveals hold for the roast.** Reveals render simultaneously on every phone (visual-first: flash/pulse — audio is garnish, bars are loud). Then the reveal HOLDS while the table riffs; any player can DESCEND after the 45s soft cap, and reaction-decay (no 🔥 for 8s past 20s) auto-advances. Server owns inputs; humans own aftermath.
5. **I REST MY CASE:** performers can end their own timer. Bold is rewarded; dead air is not.
6. **SKIP-'EM re-deals the CARD, never the human:** if 60% of the room piles on during a stalled performance, same performer gets new ammo — sting blames the card ("THAT CARD WAS TRASH — NEW WEAPON"). Once per circle. Disabled after I REST MY CASE and in the final 10s.
7. **Deal ceremony:** every deal runs a fixed 5–6s ritual on all phones ("THE DECK IS CHOOSING ITS VICTIM…"). It absorbs exactly one Brimstone burn with the backup card pre-fetched — burned and clean deals are timing-identical.
8. Explainers: 15s the first time a game appears for this group; 5s title card after.
9. Reveal grammar: any reveal payload must be glanceable in <2 seconds (a name, a number, a verdict) or be stage-sequenced into beats.

## 7. Scoring

- **Points go to the bold:** winning punchlines, survived pitches, believed lies, planted words. Voters earn a flat +1 for voting — never for aligning with the majority (no predictable-vote metagame; a player who never risks anything must not win the night).
- Exception: Roast Consensus keeps majority-alignment scoring + **Room Heat** (consensus IS that game): unanimity-minus-victim at N≤5, 60% at N≥8.
- Finale ×3. Devil's Bargain ×2. Descent Rank ladder shows 5s between circles.
- Feedback is invisible: 🔥 reaction spam during reveal-holds doubles as card telemetry. There is no feedback phase.

## 8. Safety valves (the license for exposure 4–5)

- **Brimstone tokens** — 2 per player per night, use-or-lose (no hoarding reward): burn to veto a card or dodge a spotlight.
- **Subject pre-view:** any card naming a subject renders on the subject's phone **≥10 seconds** before the room (inside the deal ceremony — never make a safety valve faster than a sip of beer). Silent BURN → backup ships with zero timing delta. No animation, no trace, no counter.
- **Spotlight burn window:** performer assignments arrive privately 10s before the room learns who was picked; the room sees a generic "DEALING…" face.
- UGC hook: the Reader/host can strike any written punchline at the teleprompter ("BURNED — NEW LINE").
- Writing-level bans (deck-side, not runtime): no card targets appearance, family death, or protected traits; above exposure 3, no card whose truthful answer names or implicates specific present third parties (the "in this room" desire/history class is dead), and none whose answer can out orientation, fidelity, or health.

## 9. Imps (latecomers & overflow)

Imps are citizens, not ghosts:
- Their names enter the card pool (roastable = included).
- They vote at 0.5 weight and fuel Room Heat.
- At N≥7 Fill-In they contribute 1 curated answer, shown as an on-screen cameo.
- Conversion to full player at circle boundaries, in join order, up to the 12-cap — announced as a role ("ZOE IS TONIGHT'S IMP — feed her souls"). Residual Imps get a guaranteed Judgment superlative and first-in-line conversion.

---

# PART III — THE GAMES

## LAUNCH 8

### 1. Roast Consensus 🎯 (minN 3 · launch · 150+ cards)
Three prompts per circle. Everyone secretly votes which PRESENT player fits ("Who would…"). Synced reveal: victim's name in lights; reveal holds for the roast.
- Plurality tie → **"THE ROOM IS TORN — DOUBLE ROAST"** (both names in lights; either vote scores).
- N=3–4 → attributed mode **"FACE YOUR ACCUSERS"** (votes shown with names — the negotiation is the comedy). Capped at exposure ≤3. Edge-repeat suppression is per-CIRCLE and display-only (never a ballot constraint — at N=3 only 6 voter→victim edges exist; constraining ballots would leave prompt 3 with zero legal votes).
- Cards must make the vote contested (2–3 plausible victims in any group) and hand the room roast ammunition.

### 2. Fill-In Finisher ✍️ (minN 3 · launch · 150+ cards)
A setup with a blank. Everyone but the Reader writes a punchline (panic button = 2 curated fallbacks at half points). Answers go ONLY to the Reader's teleprompter. **Perform, then vote:** while others write, the Reader picks the read-tone ("read these as: a eulogy / a 911 call / a hostage tape"); then performs every answer aloud — first exposure is ALWAYS out loud to the room. Vote grid (answer texts, randomized order) unlocks after the last read; no self-votes; winner + author reveal.
- N≥7: **two Readers**, one per setup, alternating. Per setup: ≤4 answers in one run → one vote picks the top → single cross-setup grand face-off.
- N=3: no Reader — reads assigned by random derangement (never your own). Authorship is deducible at N=3 (both derangements are 3-cycles), so the game leans INTO attribution: the read prompt says "you're performing Sam's filth — sell it," and the vote is on the text, not the mystery.

### 3. Over/Under 📉 (minN 3 · launch · 150+ cards)
Two subjects per circle. A stat about the subject appears; the room debates the line OUT LOUD (that argument is the game); everyone secretly bets OVER/UNDER; the subject fetches the truth (phone receipts preferred: screen time, camera roll count, unread emails). Receipt fetch is a BLOCKING input (pause + shame countdown + terminals). Synced reveal: gasp or vindication.

### 4. Confession or Cap 🤥 (minN 3 · launch · 150+ cards)
Confessor is dealt **3 confessions, privately picks 1** (consent and strategy in one move), locks TRUE/FALSE, performs the story aloud. Jury secretly votes believe/cap. Tie → confessor wins ("HUNG JURY — THE LIAR WALKS").

### 5. Scatterblast 💣 (minN 3 · launch · 80 cards)
Category + letter on all phones; the stage phone (or any volunteered phone) is the bomb with a hidden fuse; the turn passes around the circle by voice. BOOM → every phone flashes **"WHO DIED?"** — majority tap in 5s assigns the loser ("THE TABLE HAS SPOKEN").

### 6. Poison Pitch 💀 (minN 3 · launch · 100 cards)
Two players privately assigned opposite sides of a horrifying would-you-rather; 30s pitches each (both options must be EQUALLY terrible — balance is the mechanic); secret vote for best pitch. Tie → 10s sudden-death rebuttals + revote; second tie → split pot. Down-weighted at N=4.
- N=3 auto-variant: the judge rates each pitch 1–5 on a damage meter (non-zero-sum) and pitchers secretly predict their own score for bonus.

### 7. Red Flag Rally 🚩 (minN 3 · launch · 100 cards)
Defender gets PERK + RED FLAG, 45s to sell the date; secret SMASH/PASS; synced verdict. Tie → SMASH (performers win ties). The perk must genuinely tempt, the flag must genuinely disqualify — the gap is the comedy.

### 8. Alibi Drop 🕵️ (minN 3 · launch · 80 cards)
The accused gets the accusation + 3 mandatory words on their phone only; 30s improvised alibi aloud; the jury privately picks 3 suspected plants from an 8-word lineup. Reveal is stage-sequenced word-by-word with a sting per planted word.

## CONTENT DROP 1 (free, post-launch)

### 9. Text Thread Trap 📱 (minN 3)
Incoming text + mandatory tone delivered privately; performer reads the text aloud and improvises the reply IN TONE; SURVIVED/BLOCKED vote; tie → SURVIVED.

### 10. Reality Check 🪞 (minN 3)
Subject privately self-rates 1–10. The room debates their REAL number OUT LOUD in front of them (do not privatize the debate — it IS the game); a named non-subject scribe enters the consensus; synced ego-gap reveal. Preferred N≤4 finale.

### 11. Taboo Timer ⏱️ (minN 4)
Clue-giver sees word + forbidden list; a designated Buzzer phone also sees the forbidden list (the role finally works); guessers' phones show only the timer.

### 12. Hot Seat Imposter 🎭 (minN 5)
Private roles (Spyfall pattern); askers get suggested questions; performance aloud; secret REAL/FAKE vote. Gated to IRL-familiar crews (the lobby asks once per crew).

## SPIKES & CUTS
- **Title Fight 🥊** — scheduled as the arc's second physical spike + host-triggered "Fight Break." Living-room-executable duels with a crowd-judgeable winner.
- **The Unifying Theory** — CUT (highest cognitive load, worst dead-air risk). Salvage its best trios as Poison Pitch angles and Scatterblast categories.

---

# PART IV — SYSTEMS

## 10. Personalization & crew memory
- {NAME} slots use real lobby names; a spotlight-fairness counter spreads targeting.
- **Fresh Meat protocol:** first-night names are excluded from knowledge-dependent subject roles (Hot Seat, Reality Check, personal-history Over/Under) and draw first-impression variants ("Who's most likely to have already judged everyone's shoes?").
- **IRL-familiarity gate** (once per crew): online-first crews get persona/phone-legible pools; Hot Seat & Reality Check gated out.
- **Crew memory stores ONLY:** scores, win records, superlative titles, night count. **Never:** card text, confession truth values, who-voted-whom, any answers. Memory lives host-side; server room state auto-expires after ~30 days idle.
- Cloud AI is a content-refresh pipeline (draft → human curation) only. Never a runtime dependency. Never the joke author. Never a judge.

## 11. Platform & architecture
- **The game brain is server-side** (one Cloudflare Durable Object per room, TypeScript, authoritative). Every phone — host included — is a renderer over WebSocket. Phones background, die, and go to the kitchen; the room survives all of it.
- **One UI runtime: the web client.** The Android app is a thin shell (Play Billing, stage audio foreground service, lift-to-sin accelerometer bridge, wake-lock) around the same web bundle. No game screen is ever built twice.
- Synced reveals via clock-offset scheduling (≤150ms p95 skew); countdowns derive from one deadline; reveals are idempotent state (rejoiners render current truth). Reaction taps animate locally and coalesce.
- Rejoin is automatic (room code in URL + player token in localStorage). Blocking-input seats auto-void at 90s. Host controls fail over to the next player in join order.
- **The Stage** (optional, N≥5): host phone face-up mid-table renders PUBLIC-ONLY faces (timers, tallies, reveals, audio). **Lift-to-sin** for short ballots: "NICK — PICK UP TO SIN" → lift flips to a shielded private ballot (thumb-hold to reveal), lay flat restores the stage. Long host inputs suspend the stage to a "THE HOST IS SINNING" banner. No phase may assume the stage phone is stationary.

## 12. Monetization
- Joiners: free, always, forever. No ads, ever. No subscription.
- New host's first full Night: free.
- **$9.99 one-time "Host the Descent"** — the paywall sits on the SECOND Night's "BEGIN THE DESCENT" button, lobby already assembled behind it (buy in under 30 seconds at the moment of blocked intent). The Judgment share card seeds the loop: every joiner leaves with a "host your own descent" link.
- Web checkout for browser hosts; Play Billing in the Android shell. Content packs ($2.99–4.99) later. Rating: Mature 17+.

## 13. Dead by design (v1 systems this ruleset kills)
On-device LLM + 1.7GB bundled models (never fired in production; blocked Play publishing) · pass-the-phone voting · "Seat N" anonymized cards · LOL/MEH/TRASH feedback modal · Thompson sampling over bypassed templates · madlib runtime card assembly · per-device premium unlock · kiosk/device-admin mode · infinite session with buried End Game.
