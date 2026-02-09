# HELLDECK Comprehensive Fix Plan

_Full audit of content, generation, validation, and architecture — with prioritized fixes._

---

## Implementation Status

| # | Fix | Status |
|---|-----|--------|
| 1.1 | GoldCardsLoader: flat array parsing + v2 file path | ✅ DONE |
| 1.2 | ComedyScienceValidator: real thresholds (was all zeros) | ✅ DONE |
| 1.3 | Poison Pitch templates: two bad options (was perk+problem) | ✅ DONE |
| 2.1 | Text Trap: 22 mandatory tones + 3 new lexicons | ✅ DONE |
| 2.2 | Title Fight: 50 gold cards rewritten (Brain/Body/Soul) | ✅ DONE |
| 2.3 | Text Trap templates: sender + message slots | ✅ DONE |
| 3.2 | Wire ComedyScienceValidator into CardGeneratorV3 | ✅ DONE |
| 3.3 | CardQualityInspector: redundant options + instruction leakage | ✅ DONE |
| 5.1 | LLM timeout: 2.5s → 6s | ✅ DONE |
| 5.2 | Template fallback: real quality scoring via scoreCard() | ✅ DONE |
| 5.3 | StyleGuides: enriched with HDRealRules.md formulas | ✅ DONE |
| 5.4 | LLMCardGeneratorV2: ~400 lines dead code removed | ✅ DONE |
| 5.5 | OptionsCompiler: Poison Pitch slot name fallback | ✅ DONE |
| 3.1 | HumorScorer: text coherence + text specificity checks | ✅ DONE |
| 4.1 | Lexicon quality: categories (58 creative), bodily_functions, gross_problem, sexual_innuendo | ✅ DONE |
| 4.2 | Gold cards for Alibi/Scatter/HotSeat — already good quality in v2 | ✅ N/A |
| 4.3 | Reality Check: 50 cards rewritten across 5 format variants | ✅ DONE |
| 2.4 | Confession or Cap — already normalized to "Have you ever..." in v2 | ✅ N/A |
| **FRESH AUDIT** | | |
| A.1 | 11 missing lexicon files created (phone_stat, life_stat, physical_challenge, guilty_behavior, skill_trait, personality_trait, social_context, survival_scenario, random_item, celebrity, abstract_concept) | ✅ DONE |
| A.2 | OptionProviderType enum: added JUDGE_PICK, RATING_1_10, OVER_UNDER, NONE (was crashing on template deserialization) | ✅ DONE |
| A.3 | CardGeneratorV3.resolveOptions(): handle all 6 OptionProviderType values | ✅ DONE |
| A.4 | Deduplicated 1 taboo gold card (CRYPTOCURRENCY) | ✅ DONE |

---

## Executive Summary

After a deep audit of every template blueprint (17 files), all 28 lexicons, all gold cards (6500+ lines), all core generators (`LLMCardGeneratorV2`, `CardGeneratorV3`, `LLMCardGenerator`), all validators (`ComedyScienceValidator`, `CardQualityInspector`, `GameContractValidator`, `HumorScorer`), the `OptionsCompiler`, `StyleGuides`, `GoldCardsLoader`, `ComedySciencePrompts`, `GameEngine`, `GameMetadata`, `GamesRegistry`, and the full `HDRealRules.md` design doc — the following root causes explain why the game isn't fun:

### The Big Picture

1. **Critical Bug**: `GoldCardsLoader` reads `gold_cards.json` (v1), not `gold_cards_v2.json`. The entire v2 gold card library may be unused.
2. **Validator thresholds are all zero**: `ComedyScienceValidator` has minimum specificity and total scores set to `0` for every game — validation exists but rejects almost nothing.
3. **Template-generated cards are random slot combos**, not jokes. They lack the comedy principles documented in `HDRealRules.md`.
4. **Game-mechanic mismatches**: Poison Pitch templates pair perk+problem instead of two bad options. Text Trap templates lack actual text messages. Title Fight gold cards are generic trivia categories instead of Speed/Guts/Brain challenges.
5. **Humor scoring evaluates metadata, not content**: `HumorScorer` checks slot types and spice numbers, not whether the assembled sentence is actually funny.
6. **Text Trap tones mismatch**: The game defines 22 specific character tones (Seductive Whisper, Raging Karen, etc.) but the options compiler provides generic tones like "Deadpan, Feral, Chaotic."
7. **LLM timeout too tight**: 2.5 seconds for local inference frequently fails, cascading to weaker fallbacks.

---

## Phase 1: Critical Bugs (Do First)

### 1.1 Fix GoldCardsLoader File Path

**File**: `GoldCardsLoader.kt:39`
**Bug**: Loads `gold_cards.json` (v1) instead of `gold_cards_v2.json`
**Impact**: ALL gold card improvements in v2 are potentially unused. The LLM gets stale examples, fallback cards are v1 quality.

```
// CURRENT (broken)
val json = context.assets.open("gold_cards.json").bufferedReader().use { it.readText() }

// FIX
val json = context.assets.open("gold_cards_v2.json").bufferedReader().use { it.readText() }
```

**Also verify**: The v2 JSON structure matches what `GoldCardsLoader.load()` expects. The v2 file is a flat array, but the loader expects `{ "games": { "game_key": { "cards": [...] } } }`. If the structures differ, the loader needs to be updated to parse the v2 format (flat array with `"game"` field per card).

**Priority**: 🔴 CRITICAL — single biggest impact fix

---

### 1.2 Fix ComedyScienceValidator Thresholds

**File**: `ComedyScienceValidator.kt:528-555`
**Bug**: `getMinSpecificity()` returns `0` for ALL games. `getMinTotal()` returns `0` for most games. The validator is a no-op.

**Fix**: Set real thresholds that enforce the comedy science principles:

```kotlin
private fun getMinSpecificity(gameId: String): Int {
    return when (gameId) {
        // Scenario games - card itself must be vivid
        GameIds.ROAST_CONS, GameIds.CONFESS_CAP -> 2
        GameIds.POISON_PITCH, GameIds.RED_FLAG -> 1
        GameIds.TEXT_TRAP -> 1

        // Prompt games - comedy comes from player response
        GameIds.FILL_IN, GameIds.TABOO, GameIds.ALIBI -> 0
        GameIds.HOTSEAT_IMP, GameIds.REALITY_CHECK, GameIds.OVER_UNDER -> 0

        // Challenge games
        GameIds.SCATTER -> 0  // Category creativity checked by mechanic validator
        GameIds.TITLE_FIGHT, GameIds.UNIFYING_THEORY -> 1

        else -> 0
    }
}

private fun getMinTotal(gameId: String): Int {
    return when (gameId) {
        // Scenario games need higher total
        GameIds.ROAST_CONS -> 3
        GameIds.CONFESS_CAP -> 3
        GameIds.POISON_PITCH -> 2
        GameIds.RED_FLAG -> 3
        GameIds.TEXT_TRAP -> 2

        // Prompt games can have lower total
        GameIds.FILL_IN -> 1
        GameIds.HOTSEAT_IMP -> 1

        else -> 1
    }
}
```

**Priority**: 🔴 CRITICAL — enables the entire quality gate

---

### 1.3 Fix Poison Pitch Template Mechanic Mismatch

**File**: `templates_v3/poison_pitch.json` and `poison_pitch_enhanced.json`
**Bug**: Many templates pair a perk (e.g., `perks_plus`) with a problem (e.g., `gross_problem`), producing "This perk BUT this problem" cards. However, Poison Pitch is "Would You Rather" between **two equally bad options**.

**Fix**: Rewrite templates to pair two bad slot types:
- `gross_problem` vs `red_flag_issue`
- `bodily_functions` vs `would_you_rather_costs`
- `vices_and_indulgences` vs `taboo_topics`

Remove all `perks_plus` slots from Poison Pitch templates. Every blueprint should produce two options the player must choose between, not "good thing BUT bad thing" (that's Red Flag Rally's mechanic).

**Priority**: 🔴 CRITICAL — fundamental game mechanic mismatch

---

## Phase 2: Game-Content Mismatches (High Priority)

### 2.1 Fix Text Trap Tones

**Files**: `OptionsCompiler.kt:80-90`, `templates_v3/text_trap.json`
**Problem**: HDRealRules.md defines 22 specific character tones (Seductive Whisper, Raging Karen, 1920s News Anchor, Malfunctioning Robot, etc.) but the code uses generic tones: "Deadpan, Feral, Chaotic, Wholesome, Petty, Thirsty."

**Fix**:
1. Create a `mandatory_tones.json` lexicon with all 22 tones from HDRealRules.md
2. Update `OptionsCompiler.compileTextTrap()` to load from this lexicon
3. Each card should get ONE mandatory tone, not a list of 4 to choose from
4. The game mechanic is: receive text → get assigned a tone → improvise reply in that tone

**The tone IS the comedy** — matching "Seductive Whisper" to "Grandma is in the hospital" is where the humor lives. Generic tones like "Chaotic" have no performative instructions.

**Priority**: 🟠 HIGH

---

### 2.2 Rewrite Title Fight Gold Cards

**File**: `gold_cards_v2.json` (title_fight section, lines ~2403-2800)
**Problem**: Gold cards are 90% generic trivia categories ("Category: Pizza Toppings", "Category: Animals", "Category: Countries in Europe") and basic physical challenges. The game has THREE modes per HDRealRules.md:
- **Brain (Categories)**: Ping-pong answers on a topic
- **Body (Speed)**: Physical race to complete a task
- **Soul (Guts)**: Tests of willpower/awkwardness

**Fix**: Rewrite gold cards with a balanced mix:
- **Brain**: Use creative/absurd categories, not generic trivia. "Things that would get you uninvited from a wedding" not "Animals"
- **Body**: More creative physical challenges. "First person to find and show a screenshot they're embarrassed about" not just "First person to touch a doorknob"
- **Soul**: More social/emotional challenges. "Maintain eye contact while the other person describes your most embarrassing moment" not just "Staring contest"

Tag each card with its mode (brain/body/soul) so the game can balance variety.

**Priority**: 🟠 HIGH

---

### 2.3 Fix Text Trap Templates

**File**: `templates_v3/text_trap.json`
**Problem**: Templates produce "Pick the perfect reply vibe" without an actual text message scenario. Gold cards have great text messages ("Mom: We need to talk when you get home") but templates don't generate anything like this.

**Fix**: Text Trap templates need:
1. A `sender` slot (populated from a new `text_senders` lexicon: Mom, Boss, Ex, Landlord, HR, Doctor, etc.)
2. A `message` slot (populated from a new `text_messages` lexicon: "We need to talk", "Call me ASAP", "I know what you did", etc.)
3. Output format: `"[Sender]: '[Message]'"`

The template currently uses `toneA`/`toneB` slots for options, but the game should assign ONE tone as a mandatory constraint, not offer a choice of tones.

**Priority**: 🟠 HIGH

---

### 2.4 Fix Confession or Cap Gold Cards Format

**Problem**: Some gold cards use "Confession or cap: I once..." format, but the actual game mechanic is "Have you ever...?" questions. The `HDRealRules.md` formula is `"Have you ever [EMBARRASSING ACTION]?"` with the Confessor answering TRUE or FALSE.

**Fix**: Ensure all Confession or Cap cards follow the "Have you ever..." format OR clearly support the two variants documented in the code. Audit and normalize.

**Priority**: 🟡 MEDIUM

---

## Phase 3: Validation & Scoring Fixes

### 3.1 Add Content-Aware Humor Validation

**File**: `HumorScorer.kt`
**Problem**: Scores are computed from slot metadata (type, tone, spice), not from the actual assembled text. A card with the "right" slot types but a nonsensical sentence gets a high score.

**Fix**: Add text-level checks:
1. **Coherence check**: Does the assembled sentence read as grammatically plausible English? (Simple heuristic: no repeated words, no dangling prepositions, subject-verb agreement indicators)
2. **Specificity check**: Count specific nouns, brand names, numbers, places in the actual text (similar to `ComedyScienceValidator` but applied to template-generated cards too)
3. **Contrast check**: For Red Flag / Poison Pitch, verify the two halves have semantic opposition (not just random slots jammed together)

**Priority**: 🟡 MEDIUM

---

### 3.2 Wire ComedyScienceValidator into Template Pipeline

**File**: `CardGeneratorV3.kt`
**Problem**: `ComedyScienceValidator` is only used in `LLMCardGeneratorV2.validateQuality()`. Template-generated cards from `CardGeneratorV3` bypass it entirely, relying only on `HumorScorer` and the weaker `evaluateCoherence` heuristic.

**Fix**: After `CardGeneratorV3` assembles a card, run it through `ComedyScienceValidator.validate()` before accepting it. Reject cards that fail the mechanic alignment or specificity checks.

**Priority**: 🟡 MEDIUM

---

### 3.3 Improve CardQualityInspector

**File**: `CardQualityInspector.kt`
**Problem**: Only checks structural issues (blank, short, long, placeholders, options usable). Doesn't verify content quality.

**Fix**: Add checks:
1. **Redundancy**: Reject cards where option A and option B are semantically near-identical
2. **Instruction leakage**: Reject cards containing meta-instructions like "Pick the perfect", "Choose your", "Vote for" (these are UI concerns, not card content)
3. **Game-text mismatch**: For Poison Pitch, verify the card text actually presents a dilemma. For Roast Consensus, verify it's a "who would" question.

**Priority**: 🟡 MEDIUM

---

## Phase 4: Content Quality Upgrades

### 4.1 Lexicon Quality Pass

**Problem**: Many lexicon entries are abstract/vague rather than specific/visual.

**Examples of weak entries**:
- `bodily_functions.json`: "emergency bathroom situations" → should be "sprinting to the bathroom mid-meeting because you trusted a fart"
- `bodily_functions.json`: "public restroom confidence" → should be "taking a phone call on the toilet in a public restroom"
- `gross_problem.json`: Contains clinical descriptions instead of vivid scenarios

**Fix approach**: For each of the 28 lexicon files:
1. Flag entries with fewer than 5 words (too vague)
2. Flag entries without a visual verb or concrete noun
3. Rewrite flagged entries following the `ComedySciencePrompts` specificity principle: "The more specific, the funnier"
4. Add 20-30 new high-quality entries per lexicon, prioritizing entries that score 3+ on the `ComedyScienceValidator` specificity metric

**Priority**: 🟡 MEDIUM (large effort, moderate impact since LLM + gold cards bypass lexicons)

---

### 4.2 Expand Gold Cards for New Games

**Problem**: Some games have thin gold card coverage:
- **Alibi Drop**: 0 gold cards in v2 (only template-generated)
- **Scatterblast**: 0 gold cards in v2 (only template-generated)
- **Hot Seat Imposter**: 0 gold cards in v2

**Fix**: Write 50 gold cards per game following the `HDRealRules.md` card archetypes:
- **Alibi**: Crime scenarios + 3 unrelated hidden words per card
- **Scatterblast**: Creative/absurd categories (not generic trivia) + letters
- **Hot Seat Imposter**: Questions that trip up fakers but are obvious to friends

**Priority**: 🟡 MEDIUM

---

### 4.3 Reality Check Gold Cards — Add Variety

**Problem**: All 50 Reality Check gold cards follow the exact same format: "Rating: Your [trait]". The cards are functional but formulaic and lack the personality described in HDRealRules.md.

**Fix**: Introduce variant formats:
- "How impressed people are by your [trait]"
- "How often your [trait] makes people [reaction]"
- "On a scale of 1-10, how much does the group trust your [ability]"

Also add more Dunning-Kruger goldmine traits (per ComedySciencePrompts): humor, intelligence, driving, social skills, attractiveness — traits people consistently overrate.

**Priority**: 🟢 LOW

---

## Phase 5: Architecture Improvements

### 5.1 Increase LLM Timeout

**File**: `LLMCardGeneratorV2.kt:47`
**Problem**: 2.5 second timeout frequently causes LLM generation to fail, cascading to gold/template fallbacks.

**Fix**: Increase to 5-8 seconds. A slightly slower but higher-quality card is better than a fast but bad template-generated card.

```kotlin
val candidate = withTimeout(6000) { // 6 sec timeout
    generateWithLLM(request, attempt)
}
```

**Priority**: 🟡 MEDIUM

---

### 5.2 Improve Template Fallback Quality Score

**File**: `LLMCardGeneratorV2.kt:910`
**Problem**: Template-generated cards get a hardcoded `qualityScore = 0.5`. This means they always pass the `validateQuality` check (threshold is 0.65, but this bypass skips it).

**Fix**: Actually compute quality score for template cards using `ComedyScienceValidator.scoreCard()`:

```kotlin
return templateFallback.generate(templateRequest, rng)?.let {
    val score = ComedyScienceValidator.scoreCard(it.filledCard.text, request.gameId)
    val normalizedScore = score / 10.0
    GenerationResult(
        filledCard = it.filledCard,
        options = it.options,
        timer = it.timer,
        interactionType = it.interactionType,
        usedLLM = false,
        qualityScore = normalizedScore,
    )
}
```

**Priority**: 🟡 MEDIUM

---

### 5.3 Enrich StyleGuides

**File**: `StyleGuides.kt`
**Problem**: Style guides are one-liners. `HDRealRules.md` has pages of detail per game. The LLM augmentor uses these guides for paraphrasing but gets minimal guidance.

**Fix**: Expand each guide to include:
1. The game's "formula" (e.g., `"Who would [SPECIFIC ACTION]?"` for Roast)
2. What to emphasize (specificity, visual imagery, contrast)
3. What to avoid (generic phrases, appearance attacks, clichés)
4. Tone guidance (playful not cruel, absurd not boring)

Keep each guide under 200 words to avoid overwhelming the LLM.

**Priority**: 🟢 LOW

---

### 5.4 Remove Duplicate Prompt Methods in LLMCardGeneratorV2

**File**: `LLMCardGeneratorV2.kt:139-578`
**Problem**: The file contains both old `buildXPrompt()` methods AND references `ComedySciencePrompts.getPromptForGame()`. The old methods (lines 139-578) are dead code — the actual prompt building goes through `ComedySciencePrompts` (line 130).

**Fix**: Delete the old `buildRoastPrompt`, `buildPoisonPitchPrompt`, etc. methods (lines 139-578) to reduce confusion and file size.

**Priority**: 🟢 LOW (cleanup)

---

### 5.5 Fix OptionsCompiler Poison Pitch Fallback

**File**: `OptionsCompiler.kt:94-98`
**Problem**: Tries to find `"gross"` and `"social_disaster"` slot values from card metadata, falling back to repository lookups for `"gross"` and `"social_disasters"`. But these repo keys may not exist, and the slot names may not match what templates actually produce.

**Fix**: Since Poison Pitch is "Would You Rather", the options should be the two bad options from the card text. Parse them from the template's `option_provider` → `AB` configuration, not from arbitrary slot names.

**Priority**: 🟡 MEDIUM

---

## Phase 6: Missing Game Content

### 6.1 Add Missing Templates for New Games

**Problem**: These games from `HDRealRules.md` have no templates in `templates_v3/`:
- **The Unifying Theory** — gold-cards-only, no template generation
- **Reality Check** — gold-cards-only
- **Over/Under** — gold-cards-only

While gold cards work, these games can't generate fresh content when golds are exhausted.

**Fix**: Create template files for each:
- `the_unifying_theory.json`: Blueprints combining 3 slots from different lexicon categories
- `reality_check.json`: Already exists, verify it works end-to-end
- `over_under.json`: Already exists, verify it works end-to-end

**Priority**: 🟢 LOW (gold cards + LLM cover these adequately)

---

## Implementation Order (Recommended)

| Order | Fix | Impact | Effort | Files |
|-------|-----|--------|--------|-------|
| 1 | 1.1 GoldCardsLoader file path | 🔴 Critical | 5 min | `GoldCardsLoader.kt` |
| 2 | 1.2 ComedyScienceValidator thresholds | 🔴 Critical | 30 min | `ComedyScienceValidator.kt` |
| 3 | 1.3 Poison Pitch template mechanic | 🔴 Critical | 2 hrs | `poison_pitch.json`, `poison_pitch_enhanced.json` |
| 4 | 2.1 Text Trap tones | 🟠 High | 2 hrs | `OptionsCompiler.kt`, `text_trap.json`, new lexicon |
| 5 | 2.2 Title Fight gold cards | 🟠 High | 3 hrs | `gold_cards_v2.json` |
| 6 | 2.3 Text Trap templates | 🟠 High | 2 hrs | `text_trap.json`, new lexicons |
| 7 | 3.2 Wire validator to templates | 🟡 Medium | 1 hr | `CardGeneratorV3.kt` |
| 8 | 5.1 LLM timeout | 🟡 Medium | 5 min | `LLMCardGeneratorV2.kt` |
| 9 | 5.5 OptionsCompiler Poison Pitch | 🟡 Medium | 1 hr | `OptionsCompiler.kt` |
| 10 | 4.2 Expand gold cards | 🟡 Medium | 4 hrs | `gold_cards_v2.json` |
| 11 | 4.1 Lexicon quality pass | 🟡 Medium | 6 hrs | 28 lexicon files |
| 12 | 3.1 Content-aware humor scoring | 🟡 Medium | 3 hrs | `HumorScorer.kt` |
| 13 | 3.3 CardQualityInspector upgrade | 🟡 Medium | 2 hrs | `CardQualityInspector.kt` |
| 14 | 5.2 Template quality scoring | 🟡 Medium | 30 min | `LLMCardGeneratorV2.kt` |
| 15 | 5.3 Enrich StyleGuides | 🟢 Low | 2 hrs | `StyleGuides.kt` |
| 16 | 5.4 Remove dead prompt methods | 🟢 Low | 15 min | `LLMCardGeneratorV2.kt` |
| 17 | 2.4 Confession format normalization | 🟡 Medium | 1 hr | `gold_cards_v2.json` |
| 18 | 4.3 Reality Check variety | 🟢 Low | 1 hr | `gold_cards_v2.json` |

---

## Files Audited

### Content Files
- `app/src/main/assets/gold/gold_cards_v2.json` (6502 lines)
- `app/src/main/assets/templates_v3/` — all 17 template files
- `app/src/main/assets/lexicons_v2/` — all 28 lexicon files

### Core Code
- `content/generator/LLMCardGeneratorV2.kt` (915 lines) — primary generator
- `content/generator/LLMCardGenerator.kt` (901 lines) — legacy generator
- `content/generator/CardGeneratorV3.kt` (494 lines) — template generator
- `content/generator/GoldCardsLoader.kt` (194 lines) — gold card loading
- `content/generator/ComedyScienceValidator.kt` (573 lines) — quality validation
- `content/generator/ComedySciencePrompts.kt` (1057 lines) — LLM prompts
- `content/generator/HumorScorer.kt` (399 lines) — humor scoring
- `content/engine/GameEngine.kt` (377 lines) — orchestration
- `content/engine/OptionsCompiler.kt` (132 lines) — option building
- `content/engine/StyleGuides.kt` (34 lines) — augmentation guides
- `content/validation/CardQualityInspector.kt` (99 lines) — structural checks
- `content/validation/GameContractValidator.kt` (265 lines) — contract validation
- `engine/GameMetadata.kt` (282 lines) — game definitions
- `engine/GamesRegistry.kt` (108 lines) — enums

### Documentation
- `HDRealRules.md` (2162 lines) — complete game design + AI generation guide
