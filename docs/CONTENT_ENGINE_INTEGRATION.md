# HELLDECK Content Engine v3.0 Integration Guide

This document outlines the integration points for the LLM-powered content engine in the HELLDECK Android application.

> **Note**: As of December 2024, the primary card generation system is `LLMCardGeneratorV2`, which uses on-device language models. The legacy template-based system (`CardGeneratorV3`) serves as a fallback.

## 1. Dependencies

The following dependencies have been added/updated in `app/build.gradle`:

```gradle
plugins {
    id 'com.android.application'
    id 'org.jetbrains.kotlin.android'
    id 'org.jetbrains.kotlin.plugin.serialization' version '1.9.24'
    id 'kotlin-kapt'
}

dependencies {
    implementation "org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3"

    implementation "androidx.room:room-runtime:2.6.1"
    kapt "androidx.room:room-compiler:2.6.1"
    implementation "androidx.room:room-ktx:2.6.1"
}
```

## 2. New Files Created

All new content engine related files are located under `app/src/main/java/com/helldeck/content/`.

### Models
* [`app/src/main/java/com/helldeck/content/model/Template.kt`](app/src/main/java/com/helldeck/content/model/Template.kt)
* [`app/src/main/java/com/helldeck/content/model/GameOptions.kt`](app/src/main/java/com/helldeck/content/model/GameOptions.kt)

### Utilities
* [`app/src/main/java/com/helldeck/content/util/SeededRng.kt`](app/src/main/java/com/helldeck/content/util/SeededRng.kt)

### Data Layer (Assets + Room)
* [`app/src/main/java/com/helldeck/content/data/LexiconRepository.kt`](app/src/main/java/com/helldeck/content/data/LexiconRepository.kt)
* [`app/src/main/java/com/helldeck/content/data/TemplateRepository.kt`](app/src/main/java/com/helldeck/content/data/TemplateRepository.kt)
* [`app/src/main/java/com/helldeck/content/data/ContentRepository.kt`](app/src/main/java/com/helldeck/content/data/ContentRepository.kt)

### Room (Entities, DAO, DB)
* [`app/src/main/java/com/helldeck/content/db/Entities.kt`](app/src/main/java/com/helldeck/content/db/Entities.kt)
* [`app/src/main/java/com/helldeck/content/db/Dao.kt`](app/src/main/java/com/helldeck/content/db/Dao.kt)
* [`app/src/main/java/com/helldeck/content/db/HelldeckDb.kt`](app/src/main/java/com/helldeck/content/db/HelldeckDb.kt)

### Engine (Fill, Select, Orchestrate)
* [`app/src/main/java/com/helldeck/content/engine/TemplateEngine.kt`](app/src/main/java/com/helldeck/content/engine/TemplateEngine.kt)
* [`app/src/main/java/com/helldeck/content/engine/Selection.kt`](app/src/main/java/com/helldeck/content/engine/Selection.kt)
* [`app/src/main/java/com/helldeck/content/engine/GameEngine.kt`](app/src/main/java/com/helldeck/content/engine/GameEngine.kt)

### Validation
* [`app/src/main/java/com/helldeck/content/validation/Preflight.kt`](app/src/main/java/com/helldeck/content/validation/Preflight.kt)

## 3. Starter Assets

The following asset files have been created under `app/src/main/assets/`:

* `templates/templates.json`
* `lexicons/friends.json`
* `lexicons/places.json`
* `lexicons/memes.json`
* `lexicons/icks.json`
* `lexicons/perks.json`
* `lexicons/red_flags.json`
* `lexicons/gross.json`
* `lexicons/social_disasters.json`
* `lexicons/sketchy_actions.json`
* `lexicons/tiny_rewards.json`
* `lexicons/guilty_prompts.json`
* `lexicons/categories.json`
* `lexicons/letters.json`
* `lexicons/forbidden.json`
* `lexicons/inbound_texts.json` (Optional)

## 4. LLM Card Generation (Primary System)

### Key Files
* [`app/src/main/java/com/helldeck/content/generator/LLMCardGeneratorV2.kt`](app/src/main/java/com/helldeck/content/generator/LLMCardGeneratorV2.kt) - Primary LLM-based generator
* [`app/src/main/java/com/helldeck/content/generator/GoldCardsLoader.kt`](app/src/main/java/com/helldeck/content/generator/GoldCardsLoader.kt) - Gold card examples and fallbacks
* [`app/src/main/assets/gold_cards.json`](app/src/main/assets/gold_cards.json) - Curated high-quality card examples

### Usage

```kotlin
// Initialize with LocalLLM and fallback
val llmGenerator = LLMCardGeneratorV2(
    llm = localLLM,  // From llm module
    context = appContext,
    templateFallback = cardGeneratorV3
)

// Generate a card
val result = llmGenerator.generate(
    LLMCardGeneratorV2.GenerationRequest(
        gameId = GameIds.ROAST_CONS,
        players = listOf("nikki", "jay", "mo"),
        spiceMax = 3,  // Controls LLM temperature (0.5-0.9)
        sessionId = "session123",
        roomHeat = 0.6
    )
)

// Result includes:
// - filledCard: The generated card content
// - options: Game-specific options (AB, PlayerVote, Taboo, etc.)
// - timer: Seconds for this game type
// - interactionType: UI interaction mode
// - usedLLM: Whether LLM or fallback was used
// - qualityScore: Estimated quality (0.0-1.0)
```

### Spice → Temperature Mapping
| Spice | Temperature | Description |
|-------|-------------|-------------|
| 1 | 0.5 | Conservative, family-friendly |
| 2 | 0.6 | Playful with light edge |
| 3 | 0.75 | Edgy and provocative |
| 4 | 0.85 | Wild and unhinged |
| 5+ | 0.9 | Maximum chaos |

### Generation Flow
1. Check if LocalLLM is ready
2. Build quality-focused prompt with gold examples
3. Up to 3 attempts, 2.5s timeout each
4. Parse and validate response
5. Fallback chain: Gold Cards → Template Generator

## 5. Legacy Integration Callsites (Fallback Only)

### Initialize on app start (debug: run preflight)

```kotlin
val repo = ContentRepository(appContext)
repo.initialize()
// DEBUG ONLY
com.helldeck.content.validation.Preflight.validate(repo)
```

### Per session engine with seed (legacy fallback)

```kotlin
val engine = GameEngine(repo, com.helldeck.content.util.SeededRng(1337L))
val card = engine.nextFilledCard(
    GameEngine.Request(
        gameId = "ROAST_CONSENSUS",
        spiceMax = 2,
        players = listOf("nikki","jay","mo")
    )
)
val options = engine.getOptionsFor(card) // For A/B, Taboo, Scatterblast UI
```

### Telemetry after resolution

```kotlin
engine.recordOutcome(card.id, laughsScore01 = 0.80)
```

## 5. Acceptance Tests (by inspection)

1.  **Deterministic Output**: Given identical session seeds and inputs, the `nextFilledCard` method should always produce the same sequence of filled cards.
2.  **Preflight Validation**: The `Preflight.validate(repo)` function must crash loudly if any non-dynamic template slot references a missing or empty lexicon.
3.  **Slot Modifiers**: All specified slot modifiers (`unique`, `#N`, `sep='…'`, `a_an`, `title/upper/lower`) must function exactly as described in the prompt.
4.  **UCB1 Behavior**: The UCB1 selection algorithm in `Selection.kt` should prioritize templates with higher rewards while still exploring less-chosen options. During cold-start (no prior visits), it should select randomly among unseen templates.
5.  **`getOptionsFor` Correctness**:
    *   For `MAJORITY_REPORT` game IDs, `getOptionsFor` should return `GameOptions.AB` with correctly parsed options.
    *   For `TABOO_TIMER` game IDs, `getOptionsFor` should return `GameOptions.Taboo` with a main word and a list of forbidden words.
    *   For `SCATTERBLAST` game IDs, `getOptionsFor` should return `GameOptions.Scatter` with a category and a letter.
    *   For all other game IDs, `getOptionsFor` should return `GameOptions.None`.

## 6. Notes

*   Lexicon entries should be lowercase, 1-4 words, and contain 20-50 entries each.
*   Dynamic slots (`target_name`, `inbound_text`) are intentionally ignored by the preflight validator.
*   The existing `GamesRegistry` can remain as-is; game IDs are matched by string.
*   The Room database is focused solely on persisting template statistics, not content storage. Asset files remain the source of truth for content.
