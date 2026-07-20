# CARD_AUTOPSY.md — Why the cards aren't fun

_Full-corpus pathology of all 917 gold cards (13 of 14 decks card-by-card; Unifying Theory excluded — cut from the game). Conducted by 13 per-deck pathologists + 3 cross-corpus analysts against the new mechanics in HDRealRules2.md. Companion doc: CONTENT_BIBLE.md (the rebuild laws)._

## The verdict

**Of 864 cards audited: 449 KILL (52%) · 292 REWRITE (34%) · 123 KEEP (14%).**

| Deck | Kill | Rewrite | Keep | Keep % | Dominant failure |
|---|---|---|---|---|---|
| Roast Consensus | 48 | 66 | 26 | 19% | GENERIC_INTERNET (53) |
| Confession or Cap | 37 | 24 | 19 | 24% | PREWRITTEN_JOKE (20) |
| Fill-In Finisher | 43 | 22 | 15 | 19% | ZERO ROOM LEVERAGE (80/80) |
| Over/Under | 34 | 16 | 12 | 19% | UNVERIFIABLE_REVEAL (26) |
| Text Thread Trap | 20 | 18 | 12 | 24% | NO ROOM LEVERAGE (50/50) |
| Scatterblast | 21 | 23 | 9 | 17% | MECHANIC_MISMATCH (53) |
| Reality Check | 26 | 11 | 8 | 18% | UNDEBATABLE_PRIVATE_FACT (16) |
| Taboo Timer | 29 | 13 | 8 | 16% | Forbidden lists don't block easy routes (23) |
| Hot Seat Imposter | 36 | 12 | 5 | 9% | WHOLESOME_CORPORATE (18) |
| Poison Pitch | 52 | 30 | 3 | 4% | DUPLICATE_SHAPE (34) |
| Red Flag Rally | 22 | 25 | 3 | 6% | DUPLICATE_SHAPE (50), broken data (19/50) |
| Alibi Drop | 44 | 27 | 2 | 3% | ANSWER_KEY_WORDS (62) |
| Title Fight | 37 | 5 | 1 | 2% | Recycled Reddit canon (16) |

## The five root causes

**1. The cards were written for scrolling, not for a table.**
~84% of the corpus is one voice: relatable millennial-Twitter observational humor — recognition ("haha so true"), not comedy. 38% of ALL cards name a phone/app/online/dating/therapy artifact. Recognition humor works silently on a feed; a party game needs cards that make the ROOM do something. The proof: Fill-In Finisher and Text Thread Trap have **zero** cards (0/130) that reference the people playing.

**2. The card tells the joke, so the players can't.**
The decks with the lowest player-generativity scores are the worst decks (Red Flag Rally 3/10, Confession or Cap 3.5/10, Poison Pitch 5/10): perk+flag both printed, confession punchlines pre-written ("...It was a promotional text from DoorDash"), both would-you-rather horns fully authored. Jackbox's law — prompts set PLAYERS up to be funny — is inverted; players are a laugh track. This is also why the deck decays like CAH: prewritten jokes are memorized by the second night.

**3. One number ("spice") priced two different things, and both mispriced.**
60% of sampled cards can't be encoded by the single spice number because EXPOSURE (how personal) and CHAOS (how deranged) are different axes — conflation hits 87% at spice 5. Consequences: **29.7% of the corpus (272 cards) is corporate-icebreaker tier** (WHOLESOME — the banned word: Starbucks orders, Pixar crying, pineapple pizza), much of it hiding at spice 3-4; meanwhile perfect feral-impersonal cards (shovel/bleach/tarp) are wrongly locked behind max-spice consent gates, and 20+ cards are "dark-not-funny" (interrogation prompts wearing a party costume: body counts, who-made-you-cry). 8 cards violate the hard ban outright (truthful answers implicate present third parties or can out orientation/fidelity — e.g. "Number of people in this room they've thought about hooking up with").

**4. Template fatigue: the corpus is a few jokes photocopied.**
59 cards mention an ex (12 of 14 decks). 17+ cards are the same skeleton ("private digital artifact exposed at public venue"). 15 jokes appear near-verbatim in TWO different decks (Build-A-Bear, accidental-cult-robes, rotisserie chicken…), plus outright verbatim dupes within decks. Alibi Drop is three skeletons in a trenchcoat ("What happened at X" ×14). Players clock the template by round five and the deck reads as one joke.

**5. Cards ignore their game's mechanic.**
Each game has a core verb the card must feed — a contested vote, a lie-worthy claim, a debatable line, a smuggleable word. Whole failure classes exist per deck: Over/Under stats with no receipt (subject invents a number; bet can't resolve), Alibi words that are natural answers to their own accusation (zero contortion = zero comedy), Reality Check stats only the subject could know (room holds no evidence = no debate), Taboo forbidden lists that leave the first-instinct clue legal, Roast prompts where everyone qualifies (no vote tension), Red Flag cards where display text and data fields contain two different jokes (19/50 — a build bug shipped as content).

## What the 123 keepers share (the HELLDECK card, discovered)

A **specific filmed scene** (place, object, number — never a vibe-verdict), a **self-incriminating euphemism** that opens a loop the table must close ("...and call it research"), **table-awareness** ("Who has blackmail material on everyone in this room?"), and **two-axis vote tension** (crime × incompetence: "Who would accidentally send a voice note of themselves talking trash?"). Model cards and per-deck rebuild requirements: see CONTENT_BIBLE.md.

## Systems findings

- **The madlib template pipeline cannot ship a joke** and is unsalvageable: 75/75 sexual_innuendo lexicon entries grammar-break their host template; 73/75 carry embedded punchlines that collide with any context; the funny in the best cards is engineered dependency between halves — exactly what random slot-sampling can't produce. Verdict: 100% hand-written corpus; {NAME} is the only safe substitution.
- **quality_score=10 on all 917 cards** — the curation field carries zero signal.
- Old spice ratings are unusable as-is (60% conflated); every card gets re-rated on the two-axis rubric during rebuild.
