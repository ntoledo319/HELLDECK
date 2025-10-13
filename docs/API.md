# HELLDECK API Documentation

## Overview

HELLDECK is a comprehensive party game system with learning capabilities, kiosk mode functionality, and extensive customization options.

## Core Components

### Game Engine

#### `GameEngine`
Main orchestrator for game logic and state management.

```kotlin
class GameEngine(
    private val ctx: Context,
    private val repo: Repository,
    private val templateEngine: TemplateEngine
)
```

**Key Methods:**
- `initialize()` - Initialize the game engine
- `nextFilledCard(gameId: String): FilledCard` - Generate next card for a game
- `commitRound(...)` - Process round results and update learning
- `getGameStats()` - Get game statistics
- `reset()` - Reset game state

#### `TemplateEngine`
Handles template filling with dynamic content.

```kotlin
class TemplateEngine(private val ctx: Context)
```

**Key Methods:**
- `fill(template: TemplateDef, slotProvider: suspend (String) -> String): String`
- `getCandidatesForGame(game: String): List<TemplateDef>`
- `getTemplateById(id: String): TemplateDef?`

#### `Selection`
Template selection algorithms using multi-armed bandit approaches.

```kotlin
object Selection
```

**Key Methods:**
- `pickNext(candidates: List<TemplateEntity>, recentFamilies: List<String>, roundIdx: Int): TemplateEntity`
- `pickNextEpsilonGreedy(...)` - Epsilon-greedy algorithm
- `pickNextUCB(...)` - Upper Confidence Bound algorithm
- `pickNextThompson(...)` - Thompson Sampling algorithm

#### `Learning`
Machine learning system for template performance optimization.

```kotlin
object Learning
```

**Key Methods:**
- `scoreCard(...)` - Calculate score for a card based on feedback
- `updateTemplateScore(...)` - Update template score using EMA
- `calculateSelectionScore(...)` - Calculate contextual selection score

### Data Layer

#### `Repository`
High-level data access facade.

```kotlin
class Repository private constructor(private val ctx: Context)
```

**Key Methods:**
- `getActivePlayers(): Flow<List<PlayerEntity>>`
- `recordRound(...)` - Record a game round
- `loadTemplatesFromAssets(...)` - Load templates from assets
- `loadLexiconFromAssets(name: String, assetPath: String)` - Load lexicon data
- `exportBrainpack(...)` - Export learning data
- `importBrainpack(...)` - Import learning data

#### Database Entities
- `TemplateEntity` - Game card templates
- `RoundEntity` - Game round data
- `PlayerEntity` - Player information and statistics
- `CommentEntity` - Round feedback comments
- `LexiconEntity` - Word lists for template filling
- `SettingEntity` - App configuration
- `GameSessionEntity` - Game session tracking

### UI Components

#### `HelldeckTheme`
Custom Material Design 3 theme optimized for party environments.

```kotlin
@Composable
fun HelldeckTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
)
```

#### `CardFace`
Main game card display component.

```kotlin
@Composable
fun CardFace(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
)
```

#### `BigZones`
Three large touch zones for easy interaction.

```kotlin
@Composable
fun BigZones(
    modifier: Modifier = Modifier,
    onLeft: () -> Unit = {},
    onCenter: () -> Unit = {},
    onRight: () -> Unit = {},
    onLong: () -> Unit = {}
)
```

#### `FeedbackStrip`
Player feedback collection interface.

```kotlin
@Composable
fun FeedbackStrip(
    modifier: Modifier = Modifier,
    onLol: () -> Unit = {},
    onMeh: () -> Unit = {},
    onTrash: () -> Unit = {},
    onComment: (String, Set<String>) -> Unit = { _, _ -> }
)
```

### Configuration

#### `Config`
Game configuration management.

```kotlin
object Config
```

**Key Properties:**
- `current: HelldeckCfg` - Current configuration
- `spicyMode: Boolean` - Enable spicy mode (70% threshold vs 60%)

**Key Methods:**
- `load(context: Context)` - Load configuration from assets
- `roomHeatThreshold()` - Get current room heat threshold
- `validate()` - Validate configuration integrity

### Kiosk Mode

#### `Kiosk`
Device lockdown functionality for dedicated gameplay.

```kotlin
object Kiosk
```

**Key Methods:**
- `enableImmersiveMode(decorView: View)` - Enable immersive fullscreen
- `enableLockTask(context: Context)` - Enable lock task mode
- `startLockTask(activity: ComponentActivity)` - Start kiosk mode
- `isKioskModeConfigured(context: Context)` - Check kiosk configuration

#### `HelldeckDeviceAdminReceiver`
Device administrator for kiosk permissions.

```kotlin
class HelldeckDeviceAdminReceiver : DeviceAdminReceiver()
```

### Haptics & Torch

#### `HapticsTorch`
Vibration and camera flash feedback system.

```kotlin
object HapticsTorch
```

**Key Methods:**
- `buzz(context: Context, durationMs: Long, intensity: VibrationIntensity)`
- `flash(context: Context, durationMs: Long, intensity: FlashIntensity)`
- `buzzPattern(context: Context, pattern: LongArray)`
- `flashPattern(context: Context, pattern: List<FlashPattern>)`

#### `GameFeedback`
Coordinated feedback for game events.

```kotlin
object GameFeedback
```

**Key Methods:**
- `triggerFeedback(context: Context, event: GameEvent)`
- `triggerRoundResultFeedback(context: Context, result: RoundResult)`

### Export/Import

#### `ExportImport`
Brainpack file management for learning data transfer.

```kotlin
object ExportImport
```

**Key Methods:**
- `exportBrainpack(context: Context, filename: String): Uri`
- `importBrainpack(context: Context, uri: Uri): ImportResult`

## Game Flow

### 1. Initialization
```kotlin
// Initialize systems
Config.load(context)
val repo = Repository.get(context)
val templateEngine = TemplateEngine(context)
val engine = GameEngine(context, repo, templateEngine)
engine.initialize()
```

### 2. Game Setup
```kotlin
// Add players
val player = repo.addPlayer("Player Name", "😀")

// Load game assets
repo.loadTemplatesFromAssets()
repo.loadLexiconFromAssets("friends", "lexicons/friends.json")
```

### 3. Round Execution
```kotlin
// Generate card
val card = engine.nextFilledCard("ROAST_CONSENSUS")

// Process player interactions
// ... voting, feedback collection ...

// Commit round
val result = engine.commitRound(
    card = card,
    feedback = Feedback(lol = 2, meh = 1, trash = 0),
    judgeWin = true,
    points = 2,
    latencyMs = 1500
)
```

### 4. Learning Update
```kotlin
// Learning happens automatically in commitRound()
// Template scores are updated based on feedback
// Selection algorithm adapts for future rounds
```

## Configuration

### Game Configuration (`settings/default.yaml`)

```yaml
learning:
  alpha: 0.3                    # Learning rate
  epsilon_start: 0.25          # Initial exploration
  epsilon_end: 0.05            # Final exploration
  decay_rounds: 20             # Exploration decay period

scoring:
  win: 2                       # Points for winning
  room_heat_bonus: 1           # Room consensus bonus
  room_heat_threshold: 0.60    # Heat threshold (0.70 in spicy mode)
  trash_penalty: -2            # Room trash penalty

timers:
  vote_binary_ms: 8000         # Binary vote timer
  vote_avatar_ms: 10000        # Avatar vote timer
  judge_pick_ms: 6000          # Judge selection timer
```

### Template Format (`templates/templates.json`)

```json
{
  "id": "rc1",
  "game": "ROAST_CONSENSUS",
  "text": "Most likely to {sketchy_action} for {tiny_reward}.",
  "family": "roast_action_reward",
  "spice": 1,
  "locality": 1,
  "max_words": 16
}
```

### Lexicon Format (`lexicons/*.json`)

```json
[
  "word1",
  "word2",
  "word3"
]
```

## Error Handling

### Exception Types
- `GameEngineException` - Game logic errors
- `TemplateEngineException` - Template processing errors
- `DatabaseException` - Data layer errors
- `ConfigurationException` - Configuration errors

### Error Recovery
```kotlin
try {
    val card = engine.nextFilledCard(gameId)
} catch (e: GameEngineException) {
    // Handle game logic errors
    Logger.e("Game engine error", e)
    // Fallback to default template or skip round
}
```

## Performance Considerations

### Database Optimization
- Use Room's built-in query optimization
- Implement proper indexing on frequently queried columns
- Use transactions for batch operations
- Clean up old data periodically

### Memory Management
- Lazy load large assets (templates, lexicons)
- Use appropriate data structures for game state
- Implement object pooling for frequently created objects
- Monitor memory usage in long game sessions

### UI Performance
- Use Compose's built-in performance optimizations
- Implement proper state hoisting
- Use remember/derivedStateOf for expensive computations
- Implement proper list virtualization for large datasets

## Testing

### Unit Tests
```kotlin
@Test
fun testTemplateFilling() {
    val template = TemplateDef("test", "GAME", "Hello {name}", "test")
    val result = templateEngine.fill(template) { "World" }
    assertEquals("Hello World", result)
}
```

### Integration Tests
```kotlin
@Test
fun testGameRound() {
    val card = engine.nextFilledCard("ROAST_CONSENSUS")
    assertNotNull(card)
    assertTrue(card.text.isNotBlank())
}
```

### UI Tests
```kotlin
@Test
fun testGameScene() {
    composeTestRule.setContent {
        GameScene(gameState = mockGameState)
    }
    // Test UI interactions and state changes
}
```

## Deployment

### Build Commands
```bash
# Debug build
./gradlew :app:assembleDebug

# Release build
./gradlew :app:assembleRelease

# Install via ADB
adb install -r app/build/outputs/apk/debug/app-debug.apk

# Setup kiosk mode
python loader/helldeck_loader.py
```

### Kiosk Setup
1. Enable Developer Options and USB Debugging
2. Build and install APK
3. Run desktop loader for kiosk configuration
4. Set device owner (fresh device only)
5. Reboot device

## Troubleshooting

### Common Issues

**APK Installation Fails**
- Check USB debugging is enabled
- Verify ADB connection with `adb devices`
- Ensure no other device admin apps are active

**Kiosk Mode Not Working**
- Device must be freshly reset for device owner setup
- Alternative: Use system settings → Security → Screen pinning
- Check device admin permissions in settings

**Templates Not Loading**
- Verify assets are properly included in APK
- Check file paths in assets directory
- Validate JSON format in template files

**Learning Not Working**
- Check database integrity
- Verify feedback is being recorded
- Export/import brainpack to transfer learned data

### Debug Information
```kotlin
// Get app version and device info
val appVersion = AppUtils.getAppVersion(context)
val deviceInfo = AppUtils.getDeviceInfo(context)

// Get game statistics
val stats = engine.getGameStats()

// Export logs for debugging
Logger.exportLogs(context, logFile)
```

## API Reference

### Enums

#### `Interaction`
- `VOTE_AVATAR` - Player avatar voting
- `TRUE_FALSE` - Binary choice voting
- `AB_VOTE` - A/B option voting
- `JUDGE_PICK` - Judge selection
- `SMASH_PASS` - Smash or pass voting
- `TARGET_PICK` - Target player selection
- `REPLY_TONE` - Reply tone selection
- `TABOO_CLUE` - Taboo word game
- `ODD_REASON` - Odd one out explanation
- `DUEL` - Player duel
- `SMUGGLE` - Hide words in story
- `PITCH` - Sales pitch
- `SPEED_LIST` - Quick listing game

#### `RoundPhase`
- `IDLE` - Waiting to start
- `DRAW` - Drawing card
- `PERFORM` - Players performing
- `RESOLVE` - Resolving results
- `FEEDBACK` - Collecting feedback

#### `GameEvent`
- `PHASE_CHANGE` - Game phase changed
- `SCORING_LOCK` - Scoring locked in
- `ROOM_HEAT` - Room heat achieved
- `CARD_DRAW` - New card drawn
- `VOTE_CONFIRM` - Vote confirmed
- `ERROR` - Error occurred
- `WIN` - Player won

### Data Classes

#### `FilledCard`
```kotlin
data class FilledCard(
    val templateId: String,
    val game: String,
    val text: String,
    val options: List<String> = emptyList(),
    val meta: Map<String, String> = emptyMap()
)
```

#### `Feedback`
```kotlin
data class Feedback(
    val lol: Int = 0,
    val meh: Int = 0,
    val trash: Int = 0,
    val latencyMs: Int = 0,
    val tags: Set<String> = emptySet()
)
```

#### `RoundResult`
```kotlin
data class RoundResult(
    val points: Int,
    val judgeWin: Int = 0,
    val roundScore: Double = 0.0,
    val roomHeat: Boolean = false,
    val roomTrash: Boolean = false,
    val streakBonus: Int = 0
)
```

## Contributing

### Code Style
- Follow Kotlin coding conventions
- Use meaningful variable names
- Add documentation for public APIs
- Write unit tests for new functionality

### Adding New Games
1. Add game ID to `GameIds` object
2. Create game specification in `Games` list
3. Add templates to `templates.json`
4. Implement game-specific UI logic
5. Add game rules to documentation

### Adding New Features
1. Design API following existing patterns
2. Implement with proper error handling
3. Add comprehensive tests
4. Update documentation
5. Test on multiple devices

## License

This project is designed for personal and educational use. Game mechanics and assets are custom-created for this implementation.

## Support

For support and questions:
- Check the troubleshooting section
- Review the example code
- Examine the test cases
- Check the changelog for recent updates

---

*Built with ❤️ for party game enthusiasts everywhere*