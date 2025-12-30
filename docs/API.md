# HELLDECK API Reference

Quick reference for HELLDECK's core APIs. For tutorials and examples, see [DEVELOPER.md](DEVELOPER.md).

## Core Engine APIs

### GameEngine

Main orchestrator for game logic and state management.

```kotlin
class GameEngine(
    repo: ContentRepository,
    rng: SeededRng,
    selector: ContextualSelector,
    augmentor: Augmentor?,
    modelId: String,
    cardGeneratorV3: CardGeneratorV3?
)
```

**Methods:**
- `suspend fun next(req: Request): Result` - Generate next game card
- `fun recordOutcome(templateId: String, reward01: Double)` - Record round result for learning
- `fun getOptionsFor(card: FilledCard, req: Request): GameOptions` - Get game-specific options

**Data Classes:**
```kotlin
data class Request(
    val sessionId: String,
    val gameId: String? = null,
    val players: List<String> = emptyList(),
    val spiceMax: Int = 3,
    val localityMax: Int = 3,
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

### ContentRepository

Data access layer for templates, lexicons, and stats.

```kotlin
class ContentRepository(context: Context)
```

**Methods:**
- `fun initialize()` - Initialize repository and load assets
- `fun templates(): List<Template>` - Get legacy templates
- `fun templatesV2(): List<TemplateV2>` - Get V2 blueprint templates  
- `fun wordsFor(slot: String): List<String>` - Get lexicon words for slot type
- `val statsDao: TemplateStatsDao` - Access to template statistics

### ContextualSelector

Thompson Sampling-based template selection algorithm.

```kotlin
class ContextualSelector(rng: SeededRng)
```

**Methods:**
- `fun pick(ctx: Context, pool: List<TemplateV2>): TemplateV2` - Select template using UCB
- `fun update(templateId: String, reward: Double)` - Update template statistics

### LLMCardGeneratorV2 (Primary)

Quality-first LLM card generation using gold standard examples.

```kotlin
class LLMCardGeneratorV2(
    llm: LocalLLM?,
    context: Context,
    templateFallback: CardGeneratorV3
)
```

**Methods:**
- `suspend fun generate(request: GenerationRequest): GenerationResult?` - Generate LLM-powered card with fallback chain

**Data Classes:**
```kotlin
data class GenerationRequest(
    val gameId: String,
    val players: List<String>,
    val spiceMax: Int,  // Controls temperature: 1→0.5, 2→0.6, 3→0.75, 4→0.85, 5+→0.9
    val sessionId: String,
    val roomHeat: Double = 0.6
)

data class GenerationResult(
    val filledCard: FilledCard,
    val options: GameOptions,
    val timer: Int,
    val interactionType: InteractionType,
    val usedLLM: Boolean,
    val qualityScore: Double = 0.0
)
```

**Generation Flow:**
1. Check if LocalLLM is ready
2. Build quality-focused prompt with gold examples from `gold_cards.json`
3. Up to 3 attempts, 2.5s timeout each
4. Parse and validate response (quality score ≥0.6, no clichés)
5. Fallback chain: Gold Cards → CardGeneratorV3

### CardGeneratorV3 (Legacy Fallback)

Blueprint-based card generation with quality gating. Used as fallback when LLM is unavailable.

```kotlin
class CardGeneratorV3(repo: RepositoriesV3, rng: SeededRng)
```

**Methods:**
- `fun generate(req: GameEngine.Request, rng: SeededRng): GenerationResult?` - Generate quality-gated card
- `fun goldOnly(req: GameEngine.Request, rng: SeededRng): GenerationResult?` - Use gold fallback only

### GoldCardsLoader

High-quality curated cards for prompts and fallbacks.

```kotlin
object GoldCardsLoader {
    fun load(context: Context): Map<String, List<GoldCard>>
    fun getExamplesForGame(context: Context, gameId: String, count: Int = 5): List<GoldCard>
    fun getRandomFallback(context: Context, gameId: String): GoldCard?
}
```

## Data Layer

### Database Entities

**Core Entities:**
- `TemplateEntity` - Game card templates
- `TemplateV2Entity` - Blueprint-based templates
- `LexiconEntity` - Word lists for slot filling
- `PlayerEntity` - Player data and statistics
- `RoundEntity` - Round history
- `TemplateStatEntity` - Template learning stats
- `GameSessionEntity` - Session tracking

### DAOs

**TemplateStatsDao:**
```kotlin
interface TemplateStatsDao {
    suspend fun get(templateId: String): TemplateStatEntity?
    suspend fun upsert(stat: TemplateStatEntity)
    suspend fun getAll(): List<TemplateStatEntity>
}
```

## UI Components

### Core Composables

**HelldeckAppUI:**
```kotlin
@Composable
fun HelldeckAppUI(vm: GameNightViewModel, modifier: Modifier = Modifier)
```

**Scene Composables:**
- `HomeScene(vm: GameNightViewModel)` - Main menu with game selection
- `RoundScene(vm: GameNightViewModel)` - Game round interface with timer and interactions
- `OnboardingFlow(onComplete: () -> Unit)` - Interactive tutorial for new users
- `RollcallScene(vm: GameNightViewModel)` - Player attendance management
- `PlayersScene(vm: GameNightViewModel)` - Player management and profiles
- `FeedbackScene(vm: GameNightViewModel)` - Post-round rating interface
- `SettingsScene(vm: GameNightViewModel)` - App configuration
- `StatsScene(vm: GameNightViewModel)` - Player statistics and game history

**Enhanced Button Components:**
- `PrimaryButton(onClick, text, modifier, enabled, loading, icon)` - Animated primary action button with haptics
- `SecondaryButton(onClick, text, modifier, enabled)` - Outlined secondary button
- `TextButton(onClick, text, modifier, enabled)` - Text-only button
- `ToggleButton(selected, onClick, text, modifier)` - Toggle state button with animations
- `IconButton(onClick, icon, modifier, enabled)` - Icon-only button with scale feedback
- `FloatingActionButton(onClick, icon, modifier)` - Floating action button

**Interactive Components:**
- `BigZones(onLeft, onCenter, onRight, onLong)` - Three-zone touch interface
- `FeedbackStrip(onLol, onMeh, onTrash, onComment)` - Round feedback
- `EmojiPicker(show, onDismiss, onPick)` - Emoji avatar selection
- `SpiceSlider(value, onValueChange, modifier)` - Spice level selector with visual feedback
- `InteractionRenderer(roundState, onEvent, modifier)` - Master dispatcher for game interactions

## Configuration

### Config

Runtime configuration management.

```kotlin
object Config {
    val current: ConfigData
    fun load(context: Context)
}
```

**ConfigData Structure:**
```kotlin
data class ConfigData(
    val generator: GeneratorConfig,
    val scoring: ScoringConfig,
    val debug: DebugConfig
)
```

### Template Format

**Blueprint V3 (templates_v3/*.json):**
```json
{
  "id": "unique_id",
  "game": "GAME_ID",
  "family": "template_family",
  "weight": 1.0,
  "spice_max": 2,
  "locality_max": 2,
  "blueprint": [
    {"type": "text", "value": "Static text"},
    {"type": "slot", "name": "slot1", "slot_type": "lexicon_name"}
  ],
  "constraints": {"max_words": 24, "distinct_slots": true}
}
```

**Lexicon V2 (lexicons_v2/*.json):**
```json
{
  "slot_type": "category_name",
  "entries": [{
    "text": "entry",
    "tags": ["tag1"],
    "tone": "playful",
    "spice": 1,
    "locality": 1,
    "pluralizable": false,
    "needs_article": "a"
  }]
}
```

## Kiosk Mode

### Kiosk

Device lockdown for dedicated gameplay.

```kotlin
object Kiosk {
    fun enableImmersiveMode(decorView: View)
    fun startLockTask(activity: ComponentActivity)
    fun stopLockTask(activity: ComponentActivity)
    fun isKioskModeConfigured(context: Context): Boolean
}
```

### HelldeckDeviceAdminReceiver

```kotlin
class HelldeckDeviceAdminReceiver : DeviceAdminReceiver()
```

## Feedback & Haptics

### HapticsTorch

Vibration and camera flash feedback.

```kotlin
object HapticsTorch {
    fun buzz(context: Context, durationMs: Long, intensity: VibrationIntensity)
    fun flash(context: Context, durationMs: Long, intensity: FlashIntensity)
}
```

### GameFeedback

```kotlin
object GameFeedback {
    fun triggerFeedback(context: Context, event: GameEvent)
    fun triggerRoundResultFeedback(context: Context, result: RoundResult)
}
```

## Enums

### InteractionType
- `VOTE_PLAYER` - Vote for player (Roast Consensus, Hot Seat Imposter)
- `TRUE_FALSE` - Binary choice (Confession or Cap)
- `AB_VOTE` - A/B option selection (Poison Pitch, Majority Report, Over/Under)
- `JUDGE_PICK` - Judge selection (Fill-In Finisher, Title Fight)
- `REPLY_TONE` - Text reply tone (Text Thread Trap)
- `TABOO_CLUE` - Taboo word game (Taboo Timer)
- `ODD_EXPLAIN` - Odd one out explanation (Odd One Out, The Unifying Theory)
- `SALES_PITCH` - Sales pitch (Hype or Yike)
- `SPEED_LIST` - Quick listing (Scatterblast)
- `HIDE_WORDS` - Hide words in story (Alibi Drop)
- `MINI_DUEL` - Mini-duel format (Title Fight)
- `PREDICT_VOTE` - Predict room vote (Majority Report)
- `REALITY_CHECK` - Self-rating vs group rating (Reality Check)

### GameOptions
- `PlayerVote(players: List<String>)` - List of players to vote for
- `AB(optionA: String, optionB: String)` - Two-choice options
- `Taboo(word: String, forbidden: List<String>)` - Target word and forbidden terms
- `Scatter(category: String, letter: String)` - Category and starting letter
- `ReplyTone(tones: List<String>)` - Available reply tone options
- `OddOneOut(items: List<String>)` - Three items to choose from
- `OverUnder(line: Int, actual: Int?)` - Betting line and actual value (if revealed)
- `RealityCheck(subjectScore: Int, groupScore: Int?)` - Self-rating and group rating
- `None` - No special options

## Export/Import

### ExportImport

Brainpack file management for learning data.

```kotlin
object ExportImport {
    fun exportBrainpack(context: Context, filename: String): Uri
    fun importBrainpack(context: Context, uri: Uri): ImportResult
}
```

## Error Handling

### Exception Types
- `GameEngineException` - Game logic errors
- `TemplateEngineException` - Template processing errors
- `DatabaseException` - Data layer errors
- `ConfigurationException` - Configuration errors

## See Also

- [Developer Guide](DEVELOPER.md) - Setup, examples, and tutorials
- [User Guide](USERGUIDE.md) - Game rules and player documentation
- [Content Authoring](authoring.md) - Creating templates and lexicons
- [Troubleshooting](TROUBLESHOOTING.md) - Common issues and solutions
- [Architecture](ARCHITECTURE.md) - System design and patterns

---
