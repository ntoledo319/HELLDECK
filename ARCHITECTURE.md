# HELLDECK System Architecture

> Complete technical design documentation for developers and architects

**Last Updated:** January 2026  
**Version:** 1.0.1  
**Status:** ✅ Complete

---

## System Overview

HELLDECK is an Android party game application featuring:
- **14 mini-games** with distinct mechanics (voting, judging, word games, duels)
- **On-device AI** using TinyLlama/Qwen models for content generation
- **Adaptive learning** via Thompson Sampling algorithm
- **Three-tier fallback** ensuring playable content (LLM → Gold → Templates)
- **Contract validation** guaranteeing game rule compliance

**Architecture Pattern:** Layered architecture with reactive UI (Jetpack Compose)

## Table of Contents

1. [System Layers](#system-layers)
2. [Content Generation Pipeline](#content-generation-pipeline)
3. [Game Engine](#game-engine)
4. [Data Layer](#data-layer)
5. [UI Architecture](#ui-architecture)
6. [Native Layer](#native-layer)
7. [Learning System](#learning-system)
8. [Data Flow](#data-flow)
9. [Performance](#performance)
10. [Deployment](#deployment)

---

## System Layers

```
┌─────────────────────────────────────────────────────────────┐
│                    UI Layer (Compose)                        │
│  Scenes, Interactions, ViewModels, Navigation               │
└─────────────────────────────────────────────────────────────┘
                            ↓↑
┌─────────────────────────────────────────────────────────────┐
│                   Business Logic Layer                       │
│  GameEngine, PlayerManager, Config, GameMetadata            │
└─────────────────────────────────────────────────────────────┘
                            ↓↑
┌─────────────────────────────────────────────────────────────┐
│                 Content Generation Layer                     │
│  LLMCardGeneratorV2, GoldCards, CardGeneratorV3             │
│  SemanticValidator, GameContractValidator                    │
└─────────────────────────────────────────────────────────────┘
                            ↓↑
┌─────────────────────────────────────────────────────────────┐
│                      Data Layer                              │
│  Room Database, DataStore, ContentRepository                │
└─────────────────────────────────────────────────────────────┘
                            ↓↑
┌─────────────────────────────────────────────────────────────┐
│                     Native Layer                             │
│  llama.cpp (C++), JNI Bridge, Model Loading                 │
└─────────────────────────────────────────────────────────────┘
```

---

## Content Generation Pipeline

### Three-Tier Fallback Architecture

**Priority 1: LLM Generation (Primary)**
- **File**: `LLMCardGeneratorV2.kt`
- **Models**: TinyLlama-1.1B, Qwen-0.5B
- **Strategy**: Quality-focused prompts with gold examples
- **Timeout**: 2.5s per attempt, 3 attempts max
- **Temperature**: Spice-based (1→0.5, 2→0.6, 3→0.75, 4→0.85, 5→0.9)
- **Validation**: Quality score ≥0.6, cliché filter, length check

**Priority 2: Gold Cards (Curated Fallback)**
- **File**: `gold_cards.json` (700 cards, 50 per game)
- **Loader**: `GoldCardsLoader.kt`
- **Quality**: All cards rated 9-10/10
- **Use Case**: LLM timeout, quality failure, model unavailable

**Priority 3: Template System (Final Fallback)**
- **Generator**: `CardGeneratorV3.kt`
- **Method**: Blueprint CSP solver with semantic validation
- **Blueprints**: 17 per-game JSON files (`templates_v3/`)
- **Lexicons**: 28 typed word lists (`lexicons_v2/`)
- **Artifacts**: Priors, weights, rules (`model/`)

### Contract Validation

**Purpose**: Ensure cards meet game-specific requirements

**File**: `GameContractValidator.kt`

**Validation Rules by Interaction Type:**

| Interaction | Contract Rules |
|-------------|----------------|
| `VOTE_PLAYER` | ≥2 players available |
| `A_B_CHOICE` | Options A ≠ B, both non-empty |
| `TABOO_GUESS` | Target word + ≥3 forbidden words |
| `JUDGE_PICK` | ≥3 players (1 judge + 2 contestants) |
| `HIDE_WORDS` | Exactly 3 mandatory words |
| `REPLY_TONE` | Valid tone from 22 options |
| `SMASH_PASS` | Perk + red flag both present |
| `TARGET_SELECT` | ≥1 player targetable |
| `TRUE_FALSE` | Binary choice available |
| `SPEED_LIST` | Category + letter specified |
| `OVER_UNDER_BET` | Numeric line set |
| `MINI_DUEL` | ≥2 players for duel |
| `ODD_EXPLAIN` | Exactly 3 items |

**Retry Strategy**: Up to 15 contract validation attempts before gold fallback

### Semantic Validation

**File**: `SemanticValidator.kt`

**Purpose**: Prevent nonsensical slot combinations (e.g., "bodily_functions" + "dating_green_flags")

**Configuration**: `semantic_compatibility.json`

**Domains**:
- `social`: social_reason, audience_type, awkward_contexts
- `bodily`: bodily_functions, gross_problem
- `sexual`: sexual_innuendo
- `wholesome`: dating_green_flags, perks_plus
- `taboo`: taboo_forbidden, taboo_topics
- `awkward`: awkward_contexts, social_disasters

**Forbidden Pairs**:
- bodily_functions ↔ dating_green_flags
- gross_problem ↔ wholesome
- sexual_innuendo ↔ wholesome

**Coherence Score**: 0.0-1.0 (minimum threshold: 0.25)

### Lexicon System

**28 Typed Word Lists** (`lexicons_v2/`):

| Category | Lexicons | Count |
|----------|----------|-------|
| **Social** | awkward_contexts, audience_type, social_disasters, social_reason | 4 |
| **Dating** | dating_green_flags, red_flag_traits, red_flag_issue, relationship_fails | 4 |
| **Behavioral** | selfish_behaviors, vices_and_indulgences, sketchy_action | 3 |
| **Content** | meme_references, internet_slang, meme_item, product_item | 4 |
| **Taboo** | taboo_forbidden, taboo_topics | 2 |
| **Physical** | bodily_functions, gross_problem | 2 |
| **Game Mechanics** | would_you_rather_costs, chaotic_plan, evidence_reason, receipts | 4 |
| **Utility** | categories, letters, secret_word, perks_plus, reply_tone, sexual_innuendo | 6 |

**Entry Metadata**:
```json
{
  "text": "word or phrase",
  "tags": ["type", "mood"],
  "tone": "playful|wild|cringe|dry|witty",
  "spice": 1-3,
  "locality": 1-3,
  "pluralizable": true|false,
  "needs_article": "none|a|an|the"
}
```

---

## Game Engine

### Core Orchestrator

**File**: `GameEngine.kt`

**Responsibilities**:
1. Card generation (delegates to content generators)
2. Contract validation (ensures game rules compliance)
3. Options compilation (generates game-specific choices)
4. Outcome recording (stores feedback for learning)

**Request/Response Model**:
```kotlin
data class Request(
    val sessionId: String,
    val gameId: String?,
    val players: List<String>,
    val activePlayer: String?,
    val roomHeat: Double = 0.6,
    val spiceMax: Int = 3,
    val recentFamilies: List<String> = emptyList(),
    val avoidTemplateIds: Set<String> = emptySet()
)

data class Result(
    val filledCard: FilledCard,
    val options: GameOptions,
    val timer: Int,
    val interactionType: InteractionType
)
```

### Game Metadata Registry

**File**: `GameMetadata.kt`

**14 Game Definitions**:

| ID | Name | Timer | Interaction | Min Players | Spice |
|----|------|-------|-------------|-------------|-------|
| ROAST_CONS | Roast Consensus | 20s | VOTE_PLAYER | 3 | 2 |
| CONFESS_CAP | Confession or Cap | 15s | TRUE_FALSE | 3 | 1 |
| POISON_PITCH | Poison Pitch | 30s | A_B_CHOICE | 3 | 2 |
| FILLIN | Fill-In Finisher | 60s | JUDGE_PICK | 3 | 1 |
| RED_FLAG | Red Flag Rally | 45s | SMASH_PASS | 3 | 2 |
| HOTSEAT_IMP | Hot Seat Imposter | 15s | JUDGE_PICK | 3 | 1 |
| TEXT_TRAP | Text Thread Trap | 15s | REPLY_TONE | 3 | 1 |
| TABOO | Taboo Timer | 60s | TABOO_GUESS | 3 | 1 |
| UNIFYING_THEORY | The Unifying Theory | 30s | ODD_EXPLAIN | 3 | 1 |
| TITLE_FIGHT | Title Fight | 15s | MINI_DUEL | 3 | 1 |
| ALIBI | Alibi Drop | 30s | HIDE_WORDS | 3 | 1 |
| REALITY_CHECK | Reality Check | 20s | TARGET_SELECT | 3 | 2 |
| SCATTERBLAST | Scatterblast | 10-60s | SPEED_LIST | 3 | 1 |
| OVER_UNDER | Over/Under | 20s | OVER_UNDER_BET | 3 | 1 |

**Metadata Structure**:
```kotlin
data class GameInfo(
    val id: String,
    val title: String,
    val description: String,
    val category: GameCategory,
    val difficulty: GameDifficulty,
    val timerSec: Int,
    val minPlayers: Int,
    val maxPlayers: Int,
    val interaction: Interaction,
    val interactionType: InteractionType,
    val tags: Set<String>,
    val spice: Int
)
```

### Player Management

**File**: `PlayerManager.kt`

**Features**:
- Player roster (add/remove/toggle active)
- Emoji avatar assignment
- Active player filtering
- Player statistics tracking

**Database**: `PlayerEntity` (Room)

---

## Data Layer

### Room Database

**File**: `HelldeckDb.kt`

**Entities**:

| Entity | Purpose | Key Fields |
|--------|---------|------------|
| `PlayerEntity` | Player profiles | id, name, emoji, isActive |
| `GameNightEntity` | Session tracking | sessionId, startTime, playerCount |
| `TemplateStatEntity` | Template performance | templateId, visitCount, rewardSum |
| `TemplateExposureEntity` | Anti-repetition | sessionId, templateId, timestamp |
| `RoundEntity` | Round history | roundId, gameId, outcome, timestamp |
| `CustomCardEntity` | User-created cards | cardId, gameId, text, creator |

**DAOs**:
- `PlayerDao` - CRUD for players
- `TemplateStatsDao` - Learning statistics
- `TemplateExposureDao` - Session exposures
- `GameNightDao` - Session management

### ContentRepository

**File**: `ContentRepository.kt`

**Responsibilities**:
1. Load lexicons from assets (startup cache)
2. Load templates from JSON files
3. Provide word lists by slot type
4. Access to DAOs for persistence

**Initialization**:
```kotlin
class ContentRepository(context: Context) {
    suspend fun initialize() {
        // Load lexicons (28 files)
        // Load templates (17 blueprint files)
        // Initialize database
    }
}
```

### DataStore Preferences

**Files**: `SettingsStore.kt`, `CrewBrainStore.kt`

**Stored Settings**:
- User preferences (spice level, ask rollcall at launch)
- Last played game
- Theme preferences
- Developer mode toggles

---

## UI Architecture

### Jetpack Compose Stack

**Framework**: Compose 1.6.8 + Material3

**Navigation**: Compose Navigation with type-safe routes

**State Management**: ViewModel + StateFlow

### Main UI Orchestrator

**File**: `Scenes.kt`

**HelldeckVm (Main ViewModel)**:
```kotlin
class HelldeckVm : ViewModel() {
    val roundState: StateFlow<RoundState?>
    val players: StateFlow<List<Player>>
    val currentScene: StateFlow<Scene>
    
    suspend fun drawCard()
    fun recordFeedback(rating: FeedbackRating)
    fun startNewGameNight()
}
```

### Scene Types (16 total)

**Main Scenes**:
- `HomeScene` - Main menu with game selection
- `RollcallScene` - Player attendance
- `RoundScene` - Active gameplay
- `GameRulesScene` - In-game rules reference
- `StatsScreen` - Player statistics
- `SettingsScreen` - Configuration
- `PlayersScreen` - Player management
- `CardLabScene` - Developer card testing

**Interaction Renderers** (14, one per game):
- `VotePlayerRenderer` - Avatar voting
- `ABChoiceRenderer` - Binary choices
- `JudgePickRenderer` - Judge selection
- `TabooGuessRenderer` - Word guessing
- `ReplyToneRenderer` - Tone selection
- `TargetSelectRenderer` - Player targeting
- `HideWordsRenderer` - Word smuggling
- `SpeedListRenderer` - Category listing
- `TrueFalseRenderer` - Binary confession
- `SmashPassRenderer` - Dating voting
- `PredictVoteRenderer` - Belief voting
- `OddExplainRenderer` - Unifying theory
- `MiniDuelRenderer` - Head-to-head challenges
- `OverUnderBetRenderer` - Numeric betting

### Theming

**File**: `Theme.kt`

**Custom Dark Theme**:
- Optimized for party environments (low light)
- High contrast for readability
- Large touch targets (56dp minimum)
- Haptic feedback integration

**Color Palette**:
```kotlin
val HelldeckDark = darkColorScheme(
    primary = Color(0xFFE63946),      // Red accent
    secondary = Color(0xFF457B9D),    // Blue accent
    background = Color(0xFF1D1D1D),   // Dark background
    surface = Color(0xFF2A2A2A),      // Card surface
    onPrimary = Color.White,
    onBackground = Color(0xFFE0E0E0)
)
```

---

## Native Layer

### llama.cpp Integration

**Submodule**: `third_party/llama.cpp/`

**JNI Bridge**: `helldeck_llama.so` (C++17)

**Build Configuration** (`CMakeLists.txt`):
```cmake
add_library(helldeck_llama SHARED
    cpp/llama_jni.cpp
    ${LLAMA_CPP_SOURCES}
)

target_compile_options(helldeck_llama PRIVATE
    -std=c++17 -O3 -ffast-math
)

target_link_libraries(helldeck_llama
    android log
)
```

**NDK Configuration**:
- **Target ABI**: arm64-v8a only (ARMv8 64-bit)
- **STL**: c++_shared
- **Min SDK**: 21

### Model Loading

**File**: `ModelManager.kt`

**Process**:
1. Copy .gguf files from assets to internal storage (first launch)
2. Load model on background thread
3. Keep model in memory (reuse for all generations)
4. Graceful fallback if loading fails

**Model Specs**:
- **TinyLlama-1.1B**: ~637MB, 4-bit quantized
- **Qwen-0.5B**: ~280MB, 4-bit quantized

### LocalLLM Interface

**File**: `LocalLLM.kt`

**API**:
```kotlin
interface LocalLLM {
    suspend fun generate(
        prompt: String,
        temperature: Float,
        maxTokens: Int,
        timeoutMs: Long
    ): String?
    
    fun isReady(): Boolean
    fun unload()
}
```

---

## Learning System

### Thompson Sampling Algorithm

**File**: `ContextualSelector.kt`

**Algorithm**: Upper Confidence Bound (UCB) for Multi-Armed Bandits

**Formula**:
```
UCB(template) = μ(template) + c * √(ln(N) / n(template))

Where:
- μ = mean reward (rewardSum / visitCount)
- c = confidence parameter (1.5)
- N = total template visits
- n = visits for this template
```

**Selection Strategy**:
1. Epsilon-greedy exploration (ε = 0.1)
2. With probability ε: random selection
3. With probability 1-ε: highest UCB score
4. Apply context filters (game ID, spice, avoid recent)

### Feedback Loop

**Reward Mapping**:
```kotlin
LOL   → 1.0   // Best possible
MEH   → 0.35  // Below average
TRASH → 0.0   // Effectively banned
```

**Persistence**:
```kotlin
@Entity(tableName = "template_stats")
data class TemplateStatEntity(
    @PrimaryKey val templateId: String,
    val visitCount: Int,
    val rewardSum: Double,
    val lastUsedTimestamp: Long
)
```

**Update Process**:
1. Player provides feedback (LOL/MEH/TRASH)
2. Convert to reward value
3. Update `visitCount++` and `rewardSum += reward`
4. Persist to database
5. Future selections biased by mean reward

### Anti-Repetition

**Session-Based Exposure Tracking**:

```kotlin
@Entity(tableName = "template_exposure")
data class TemplateExposureEntity(
    @PrimaryKey(autoGenerate = true) val id: Long,
    val sessionId: String,
    val templateId: String,
    val timestamp: Long
)
```

**Lookback Window**: Last 50 cards per session

**Session Management**:
- New session ID on app launch
- Persists across rounds
- Reset via "Start New Game Night"

---

## Data Flow

### Typical Round Flow

```
1. User Action: Tap "Draw Card"
   ↓
2. GameNightViewModel.drawCard()
   ↓
3. GameEngine.next(Request)
   ↓
4. Content Generation:
   a. LLMCardGeneratorV2.generate()
   b. If fails → GoldCardsLoader.getCard()
   c. If fails → CardGeneratorV3.generate()
   ↓
5. Contract Validation:
   GameContractValidator.validate()
   ↓
6. Options Compilation:
   OptionsCompiler.compile()
   ↓
7. Result Assembly:
   Result(filledCard, options, timer, interactionType)
   ↓
8. ViewModel State Update:
   roundState.emit(RoundState)
   ↓
9. UI Render:
   Compose recomposition → Interaction renderer
   ↓
10. User Interaction:
    Player votes/answers/judges
   ↓
11. Feedback Recording:
    GameEngine.recordOutcome(templateId, reward)
   ↓
12. Stats Update:
    TemplateStatsDao.updateStats()
```

### Configuration Loading

```
App Startup
  ↓
Config.load(context)
  ↓
Parse assets/settings/default.yaml
  ↓
If parse fails → Use hard-coded defaults
  ↓
Config.current available globally
```

### Model Initialization

```
App Startup (Background Thread)
  ↓
ModelManager.initialize()
  ↓
Check if models exist in files/models/
  ↓
If not → Copy from assets/models/*.gguf
  ↓
Load model via llama.cpp JNI
  ↓
ModelManager.isReady = true
  ↓
LLM generation available
```

---

## Performance

### Target Metrics

| Operation | Target | Actual |
|-----------|--------|--------|
| LLM generation | <2500ms | ~1800ms avg |
| Template generation | <100ms | ~45ms avg |
| Gold fallback | <10ms | ~3ms avg |
| UI frame render | <16ms | ~8ms avg (60fps) |
| Database query | <50ms | ~12ms avg |
| Asset loading (startup) | <3s | ~1.8s avg |

### Memory Footprint

| Component | Size |
|-----------|------|
| LLM model (loaded) | ~640MB |
| Lexicons (cached) | ~2MB |
| Templates (cached) | ~500KB |
| UI state | ~5MB |
| Database | ~2MB |
| **Total (with LLM)** | **~650MB** |
| **Total (without LLM)** | **~10MB** |

### Optimization Strategies

**Asset Loading**:
- Lazy initialization
- Background thread loading
- Singleton caching

**Database**:
- Indexed queries
- Coroutine-based async
- Batch operations

**UI**:
- `remember` for expensive calculations
- `derivedStateOf` for computed state
- LazyColumn for lists

**LLM**:
- Keep model in memory (don't reload)
- Timeout-based cancellation
- Fallback chain prevents blocking

---

## Deployment

### Build Variants

**Debug**:
- Minify: Off
- Debuggable: On
- Proguard: Off
- Signing: Debug keystore

**Release**:
- Minify: On
- Shrink resources: On
- Proguard: On (optimize.txt)
- Signing: Release keystore (env vars)

### APK Structure

```
app-release.apk (~150MB)
├── classes.dex          # Kotlin bytecode
├── lib/
│   └── arm64-v8a/
│       ├── libhelldeck_llama.so  # ~2MB
│       └── libc++_shared.so      # ~1MB
├── assets/
│   ├── models/
│   │   └── tinyllama-1.1b.gguf   # ~637MB (uncompressed)
│   ├── gold_cards.json           # ~120KB
│   ├── lexicons_v2/              # ~500KB
│   ├── templates_v3/             # ~200KB
│   └── settings/default.yaml     # ~5KB
└── resources.arsc       # Android resources
```

### Release Checklist

- [ ] All tests passing (`./gradlew test`)
- [ ] Quality checks passing (`make ci`)
- [ ] Version bump in `build.gradle`
- [ ] CHANGELOG.md updated
- [ ] Release notes drafted
- [ ] Signing keys configured
- [ ] ProGuard rules verified
- [ ] APK size checked (<200MB)
- [ ] Test on physical device
- [ ] Smoke test all 14 games

---

**Last Updated:** January 2, 2026  
**Architecture Version:** 3.0  
**Maintainers:** HELLDECK Development Team

