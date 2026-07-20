# HELLDECK card council — cross-game verdict (2026-07-19)

**Overall: fix-first**

37 agents: per game a 3-lens council (mechanic-fit / laugh-test / addiction) -> a Chair synthesis, then one cross-game showrunner verdict.

| game | verdict | funny | fit | addiction |
|---|---|---|---|---|
| ROAST deck | fix-then-ship | 7 | 8 | 7 |
| fillin | fix-then-ship | 7 | 9 | 7 |
| overunder | fix-then-ship | 7 | 6 | 6 |
| CONFESSION deck | fix-then-ship | 8 | 8 | 7 |
| scatter | fix-then-ship | 6 | 8 | 6 |
| poison | fix-then-ship | 6 | 6 | 6 |
| REDFLAG | fix-then-ship | 6 | 7 | 6 |
| ALIBI | fix-then-ship | 6 | 8 | 6 |
| TITLEFIGHT | fix-then-ship | 6 | 6 | 5 |

## Will everyone enjoy it?

Not yet — and the reason is structural, not taste. Two verified engine facts decide this. (1) The consent system (packages/engine/src/consent.ts) is genuinely well-designed — per-player exposure ceilings, second-lowest rule at N>=5, min-of-all at N<=4, filtering never surfaced, plus the volunteer "WHO WANTS BLOOD?" valve on spotlight games. It correctly protects the shy friend from being exposed. But every one of the nine chairs independently flagged that their deck's SAG BLOCK is its mid tier: roast's gross-confession wall (082-096), poison's 20-card annoying-friend block (048-067), scatter's flat 34-card E3 plateau, overunder's E2 half-deck of stunts and domestic filth, alibi's funeral belly, fillin's court-record family, redflag's roommate-not-date opener. So a room with one cautious player gets capped to E3 and plays ONLY the material the council already condemned. The mixed friend group — the exact case the owner is asking about — is the group that gets the worst version of the game. (2) The exposure-consent architecture protects the tier of content but not the FREQUENCY of being named. Roast is vote-emergent, repeatable up to 2x/night, and eligible as opener, bargain slot, AND finale; spotlightCount fairness (deal.ts) does not touch ballot-elected victims. Combine that with the cross-deck romance monoculture and the same one or two "romantic" people get named at the climax of every single game all night. That is the person who does NOT have fun, and no ceiling catches it. The gross-out register is a smaller but real risk: overunder and fillin both lean C5 on revulsion rather than stakes, which splits a mixed table along a line the ceiling system does not model.

## Is it a little addictive?

Partially — the pull is real but it fires in the wrong place. What CREATES it is proven and repeatable: table-aware "someone in this room" cards (roast 105/061, scatter 072/074, confession 083, alibi's E4/E5 accusations, poison 082) make the room itself the crime scene and detonate an argument rather than settling a vote. That is the screenshot moment and the "run it back" engine. What KILLS it, unanimously across nine independent councils, is that every deck saves its worst-designed tier for its peak. Roast's E4/E5 collapses into all-dating. Fillin's E5 is frameless first-person confessions with nothing for the Reader to perform. Overunder's finale is fifteen flavors of ex-pining with no receipt to open. Confession's entire E5 is sexual. Poison stops being a pitch game at E4 and becomes Confession in a jacket. Redflag runs the partner-scorecard joke ~25-30 times at the summit. Alibi pairs its best accusations with its deadest cheese/dance word lists. Scatter banks all its table-aware spikes at the very end so the dopamine fires once. Titlefight burns a third of its 10-duel pool in one session and throws away the belt it hands out every fight. The mechanical consequence: night one is fun, night two the table pattern-matches the shape before the card finishes reading, and there is no night four. Average addiction score is 6.2/10, and not one deck cleared 7. A little addictive is achievable from here — the ingredients exist and are identified — but the current build peaks into fatigue rather than escalation.

## Strongest

- CONFESSION (23/30) — the only deck to score 8 on funny. The deadpan reframe-button ('shovel and tarp, for errands'; 'I did not hurry') is the single best writing device in the whole corpus: it makes one sentence readable as both a true confession and a bald-faced lie, and hands the bluffer their own defense. That is the mechanic and the joke being the same object. Cruel irony: the arc grammar means most groups never see it.
- FILLIN (23/30, and the only 9 anywhere — fit) — the most shape-disciplined deck in the box. Nearly every blank lands in true finisher or reaction-tag position ('which is fine', 'and the class pedaled harder', 'labeled ceremonial'), so a trailing clause makes ANY submission land. That is craft, not luck, and it means weak players still get laughs — which matters enormously for a mixed group.
- ROAST (22/30) — the highest-ceiling table-aware register in the project: rank the room, a private nickname for everyone here, the running ledger of debts, the itemized review after one drink. These do not merely produce a plurality, they detonate an argument. This is the platonic form of the whole game's thesis and it should be studied and cloned into every other deck.
- The consent architecture itself (packages/engine/src/consent.ts + the D-134 volunteer valve) — invisible filtering, no ceiling value ever on the wire, second-lowest rule so one shy player caps cards ABOUT them rather than the whole night. Most party games in this register have nothing like it. It is a genuine competitive moat and it is already built.

## Weakest

- TITLEFIGHT (17/30, the only sub-6 addiction score) — and it is the most dangerous weak deck because it is FORCED into every depth>=7 night at slot mid+2 by arc.ts. Groups cannot route around it. Ten fixed duels for a spike that fires 2-3x/night means night two is nearly all reruns, effective variety is really ~5-6 once duds get skipped and favourites get over-picked, 8-9 of 10 duels are talk-not-physical despite the mechanic promising a PHYSICAL palate cleanser, and the belt it awards every fight is immediately discarded instead of compounding into a rivalry.
- POISON (18/30) — switches games halfway up its own escalation ladder. The E4/E5 tier (083-102) stops being a two-sided cursed pitch and becomes a single player choosing self-expose vs get-exposed, i.e. Confession wearing a Poison jacket, at exactly the peak-stakes tier where the signature hook should be strongest. Plus a ~20-card annoying-friend dead block mid-deck and six melancholy A-vs-sad-B cards that read WISTFUL — the polar opposite of the one word that must describe this project.
- REDFLAG (19/30) — a killer top tier buried under the wrong game. A large early band is framed as 'would you LIVE with them' (rent spreadsheets, condiment fridges, house-rule binders); nobody votes PASS on a DATE over oat milk, so the SMASH/PASS tension is dead on arrival. Then the E4/E5 finale runs ONE joke — the partner ranks/logs/adjudicates you — some 25-30 times. Also notably chaste for a deck whose identity is UNHINGED: zero flags touch intimacy or split the room on values.
- OVERUNDER (19/30) — the widest gap between writing quality and mechanical function in the project. The voice is DISTINCTIVE ('protected wetland', 'gravity files an incident report') and ~50-60 cards of real phone/bank/map receipts are best-in-class. But roughly 40% of the deck resolves to 'honest count, all time' with no screen to open — and the receipt IS the game. A bet the subject just declares has no tension, nothing to screenshot, and dies after one telling. A ~25-card physical-stunt glut expose nothing about the person and would fit unchanged in any icebreaker deck.

## Top cross-game actions (highest leverage first)

1. **FIX THE ARC GRAMMAR SO SPOTLIGHT GAMES REACH DEPTH 5 AND 7 (packages/engine/src/arc.ts). Verified: at depth 5 the forced placements — simultaneous opener at slot 0, scatter at mid(2), bargain at bargainSlotFor(5)=3, all-players-score finale at 4 — consume every slot at idx>=3, and the spotlight gate blocks idx<3. Depth 7 is identical: scatter at 3, titlefight at 5, bargain at 4, finale at 6. Result: CONFESSION, POISON, REDFLAG and ALIBI can only ever be dealt in a Full Damnation.**
   - _why:_ This is the single highest-leverage item in the entire review and no deck-level chair could see it. Confession is the best-scoring deck in the box (8/8/7) and the default 'Standard Descent' never deals it. Most groups' first and possibly only night is drawn from just four decks with sanctioned repeats. Every card fix below is worth less than making the good decks actually reachable. Loosen the spotlight gate to idx>=2, or make the bargain/finale slots spotlight-eligible, or drop the forced titlefight at depth 7 — then re-run the 1000-seed property tests.

2. **BREAK THE CROSS-DECK ROMANCE MONOCULTURE AT E4/E5. Cap romance/exes/hookups at roughly one third of every deck's deep tiers and backfill with non-romance archetypes (the cheap one, the petty accountant, the snitch, the grudge-holder, the menace, table-aware implication). Roast E4/E5 is almost all dating; confession's entire E5 is sexual; overunder's finale is ~15 flavors of ex-pining; fillin's E5 is sex/shame confessions; redflag's summit is partner-scorecards.**
   - _why:_ Five of nine chairs found this independently INSIDE their own deck without knowing it was universal. Across a full night it compounds catastrophically: every game's biggest moment lands on the same topic, so the same one or two 'romantic' people get named at every climax. That is typecasting, it is the fastest way to make one specific friend stop wanting to play, and the consent ceilings do not catch it because it is a frequency problem, not an exposure-tier problem.

3. **REBUILD THE MIDDLES (E2/E3), NOT THE PEAKS — this is the 'everyone has fun' fix. Every deck's identified sag block sits in its mid tier: roast 082-096, poison 048-067, scatter's 34-card E3 plateau, overunder's E2 (half the deck), alibi's funeral belly, fillin's court-record family, redflag's roommate band. Thread table-aware 'someone in this room' spikes through these middles rather than banking them at the end.**
   - _why:_ consent.ts caps a mixed room at the second-lowest ceiling (min-of-all at N<=4), which means any group with one cautious friend plays almost entirely in E2/E3 — exactly the material nine councils just condemned. Right now the game's quality is inversely correlated with how mixed the group is. Fixing the middles also fixes 'the night sags before it peaks' for every group, so this single pass serves both questions the owner asked.

4. **GIVE EVERY DECK A REAL C2/E1 ON-RAMP. Roast's chaos floor never drops below C3, confession's floor is C3, redflag OPENS at full menace (go-bag/vanish, padlocked chest freezer, deadbolt on the outside sitting in E1). Add a small low-stakes-but-still-pointed opening rung to each deck and demote the misplaced menace cards out of E1.**
   - _why:_ Three chairs flagged this separately. The first two circles are when a mixed group decides whether this is their kind of game or something they need to survive. Opening hot is how you lose the two most cautious people at the table in the first ten minutes — and once they disengage, the rest of the night's consensus votes have fewer live candidates and the whole engine degrades.

5. **GLOBAL TEMPLATE-FATIGUE CULL: cut roughly 10-15% of every deck, specifically its dominant repeated skeleton. Fillin's ~20 'official record euphemizes my crime' cards, redflag's ~25-30 scorecard cards, poison's ~20 annoying-friend block, alibi's four rigid category-triplet templates (each running 15-26 cards straight), scatter's ~50-of-81 first-person confessional shape, overunder's ~25 physical stunts, confession's ~15 self-justification tags. Target ~120-150 cards deleted corpus-wide.**
   - _why:_ 987 cards is the headline number; effective variety is far lower and every chair said so in different words. Template repetition is what makes a table say 'this again' on night two, and it is the specific tell the project's own taste law bans as content-farm output. Elimination before addition is already the house rule — the corpus is big enough to cut hard without falling below coverage.

6. **TITLEFIGHT: grow the roster from 10 to ~24 duels in the tf_006/009/010 register, cap crowd-judged performance at ~half the pool, add genuinely physical objective duels (wall-sit-off, last-to-flinch, balance-while-insulting), and add a persistent belt meta — champion defends next TITLEFIGHT, loser may call one grudge rematch, forfeits stack.**
   - _why:_ Ordinarily a 17/30 deck would just get cut. But arc.ts FORCES titlefight into slot mid+2 of every depth>=7 night, so it is guaranteed to hit every standard descent and groups cannot route around it. It is also the only deck with a compounding-rivalry mechanic sitting unused — the belt is handed out every fight and immediately forgotten. That meta is the cheapest 'one more round' engine available anywhere in this project.

7. **ADD A TYPECAST GOVERNOR FOR VOTE-EMERGENT VICTIMS. deal.ts weights spotlight assignment by spotlightCount so no one hogs the mic, but Roast's victim emerges from ballots and is untouched by that counter — while roast is repeatable up to 2x/night and eligible as opener, bargain slot AND finale. Track how often each player has been the consensus target and soft-steer card selection toward archetypes with different obvious candidates once someone crosses a threshold.**
   - _why:_ The fairness system currently protects against a loudmouth taking every spotlight, but not against a quiet person being ELECTED the answer eight times in one night. That is the actual harm case in a mixed friend group, it is invisible to the exposure ceilings, and it is the mechanism by which one friend quietly stops accepting invitations.

8. **RUN THE D-128/D-138 PLAYTEST GATE BEFORE ANY MORE CONTENT WORK — and run it at depth 5 and depth 7 specifically, not the depth-9 config the engine tests use. Validate the arc fix and the romance rebalance live before writing another card.**
   - _why:_ The autonomous roadmap is exhausted and everything remaining is owner-gated. Nine councils just produced a large, convergent, opinionated fix list built entirely from reading cards — none of it has met a real table. The depth-9 test config is precisely why the spotlight-gating problem survived to this point: the integration test runs at 9 so confession gets exercised, which masks the fact that no normal group will ever reach it.

## Bottom line

Fix-first — and I want to be clear that this is a stronger verdict than any individual council gave, because the cross-game view is worse than the sum of its parts. All nine chairs said fix-then-ship, which read alone sounds like a light polish pass. It is not. Three findings only visible from up here change the call. First, the arc grammar means the best-scoring deck in the box (Confession, 8/8/7) literally cannot be dealt in a Quick Dip or a Standard Descent — verified in arc.ts, where forced placements consume every slot the spotlight gate permits — so most groups will never play the best thing you built. Second, the romance/ex monoculture that five chairs each found inside their own deck is actually systemic: every deck's climax lands on the same topic, so across a full night the same one or two people get named at every peak, and the consent ceilings cannot catch it because it is a frequency problem, not an exposure-tier problem. Third, and most damaging to the owner's actual question, the consent system routes mixed rooms into E2/E3 — which is exactly where every single deck's council-condemned sag block lives. The game is currently at its worst for precisely the mixed friend group it is designed for. Against that: the foundation is genuinely strong and none of this is a rewrite. The mechanics are sound, the consent architecture is a real moat, the taste is UNHINGED and not slop, the corpus is large enough that most fixes are subtraction, and the ceiling cards (roast's rank-the-room, confession's shovel-for-errands, fillin's baby monitor, alibi's leverage folder) prove the writers can hit DISTINCTIVE on demand. But an average addiction score of 6.2 with no deck above 7, and every deck peaking into its own worst-designed tier, is not a launch-ready party game — it is a strong two-night game that will not survive to night four. Do the arc fix, the romance rebalance, the mid-tier rebuild, and the template cull, then take it to a real table at depth 5 and 7 before you write another card.
---

## Verified corrections to this council (checked against the code, 2026-07-20)

The council reviewed CARDS, not the engine, and two of its claims did not survive verification.

**1. "TITLEFIGHT fires 2-3x/night, so one session burns a third of the 10-duel pool."**
FALSE as stated. In `arc.ts`, titlefight is `repeatable: false` with `loops: () => 1`, placed at
exactly one slot (`mid + 2`, and only when `depth >= 7`). It fires **once per night**, and never
at depth 5. The staleness conclusion still held for a different reason — 10 duels with one fire
per night means repeats inside ~10 nights, sooner once duds get skipped and favourites over-picked
— so the roster expansion to 32 was still correct, and now buys ~32 nights of freshness.

**2. "Add a persistent belt meta: the champion DEFENDS at the next TITLEFIGHT, the loser may call
one grudge rematch."** NOT APPLICABLE as designed. Because titlefight fires once per night, there
is no second bout in a session for a belt to compound into. A meaningful belt would need
cross-NIGHT persistence, but a room is per-night (`resetNight` clears scores/spotlight and rooms
are keyed per game code). This is a real idea but it belongs to a future "crew profile / persistent
rivalry" feature, not to D-135-era titlefight. **Do not implement it against the current model.**

**Council recommendation #7 (typecast governor) was verified TRUE and is now implemented** —
roast's balloted victims were genuinely invisible to `deal.ts` spotlight fairness; they now feed
`GameStep.spotlight` (commit `bf7f091`).
