# HELLDECK Development Guide

> Complete setup, build, test, and workflow documentation for developers

**Last Updated:** January 2026  
**Version:** 1.0.1

---

## Table of Contents

1. [Prerequisites](#prerequisites)
2. [Development Setup](#development-setup)
3. [Building](#building)
4. [Testing](#testing)
5. [Code Quality](#code-quality)
6. [Development Workflow](#development-workflow)
7. [Debugging](#debugging)
8. [Performance](#performance)
9. [Common Issues](#common-issues)

---

## Prerequisites

### Required Software

| Tool | Version | Purpose |
|------|---------|---------|
| **Android Studio** | Giraffe (2023.3.1)+ | IDE and build tools |
| **JDK** | 17+ | Java compilation |
| **Kotlin** | 1.9.25 | Language runtime |
| **Gradle** | 8.5.2 | Build system |
| **CMake** | 3.22.1+ | Native builds |
| **Android SDK** | API 21-34 | Android platform |
| **NDK** | r25c+ | Native development |
| **Python** | 3.7+ | Tools/scripts |
| **Git** | 2.x | Version control |

### Optional Tools

| Tool | Purpose |
|------|---------|
| **adb** | Device debugging |
| **scrcpy** | Device mirroring |
| **ruff** | Python linting |
| **pre-commit** | Git hooks |

### Hardware Requirements

- **Development Machine**: 8GB+ RAM, 10GB free disk
- **Test Device**: Android 5.0+ (arm64-v8a recommended)
- **Emulator**: API 21+ with 2GB RAM

---

## Development Setup

### 1. Clone Repository

```bash
git clone https://github.com/your-org/HELLDECK.git
cd HELLDECK

# Initialize submodules (llama.cpp)
git submodule update --init --recursive
```

### 2. Configure Android Studio

**Open Project:**
```bash
# Launch Android Studio
# File → Open → Select HELLDECK directory
# Wait for Gradle sync to complete
```

**SDK Configuration:**
- Android Studio → Settings → Appearance & Behavior → System Settings → Android SDK
- Install SDK Platforms: API 21, 34
- Install SDK Tools: NDK, CMake, Build-Tools 34.0.0

**Gradle Configuration:**
```bash
# Optional: Set Gradle JVM
# Android Studio → Settings → Build → Build Tools → Gradle
# Gradle JDK: 17

# Optional: Increase heap size
# ~/.gradle/gradle.properties
org.gradle.jvmargs=-Xmx4096m -XX:MaxMetaspaceSize=512m
```

### 3. Verify Setup

```bash
# Check Java version
java -version
# Should show Java 17+

# Check Gradle
./gradlew --version

# Verify NDK
ls $ANDROID_SDK_ROOT/ndk/

# Test build
./gradlew :app:assembleDebug
```

### 4. Install Python Tools (Optional)

```bash
# For loader and quality tools
cd loader
pip install -r requirements.txt

cd ../
pip install pre-commit ruff

# Install pre-commit hooks
pre-commit install
```

---

## Building

### Debug Builds

**Android Studio:**
- Build → Make Project (Ctrl+F9)
- Run → Run 'app' (Shift+F10)

**Command Line:**
```bash
# Build debug APK
./gradlew :app:assembleDebug

# Output: app/build/outputs/apk/debug/app-debug.apk

# Install to device
adb install -r app/build/outputs/apk/debug/app-debug.apk

# Build and install in one command
./gradlew :app:installDebug
```

### Release Builds

**Configure Signing:**
```bash
# Create keystore (first time only)
keytool -genkey -v -keystore release.keystore \
  -alias helldeck -keyalg RSA -keysize 2048 -validity 10000

# Set environment variables
export KEYSTORE_PATH=/path/to/release.keystore
export KEYSTORE_PASSWORD=your_password
export KEY_ALIAS=helldeck
export KEY_PASSWORD=your_key_password
```

**Build Release APK:**
```bash
./gradlew :app:assembleRelease

# Output: app/build/outputs/apk/release/app-release.apk
```

### Build Variants

| Variant | Minify | Debuggable | Proguard | Use Case |
|---------|--------|------------|----------|----------|
| **debug** | No | Yes | No | Development |
| **release** | Yes | No | Yes | Production |

### Native Builds

**llama.cpp Integration:**
```bash
# Clean native builds
./gradlew :app:clean
rm -rf app/.cxx/

# Force native rebuild
./gradlew :app:assembleDebug -Pandroid.injected.invoked.from.ide=false

# Check native libraries
unzip -l app/build/outputs/apk/debug/app-debug.apk | grep libhelldeck_llama.so
```

**Build Flags:**
```cmake
# CMakeLists.txt
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -std=c++17 -O3")
set(CMAKE_ANDROID_STL_TYPE c++_shared)
```

---

## Testing

### Unit Tests

**Run All Tests:**
```bash
# Run all unit tests
./gradlew test

# Run specific test class
./gradlew test --tests "com.helldeck.content.validation.SemanticValidatorTest"

# Run with coverage
./gradlew testDebugUnitTest jacocoTestReport
```

**In Android Studio:**
- Right-click test file → Run 'TestName'
- View → Tool Windows → Run
- Coverage: Run → Run 'TestName' with Coverage

### Instrumented Tests

**Run on Device:**
```bash
# Run all instrumented tests
./gradlew connectedAndroidTest

# Run specific test
./gradlew connectedAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.helldeck.ui.scenes.RollcallSceneTest
```

**In Android Studio:**
- Right-click androidTest folder → Run 'All Tests'
- Requires connected device or emulator

### Card Quality Tests

**Audit Specific Game:**
```bash
./gradlew :app:cardAudit \
  -Pgame=POISON_PITCH \
  -Pcount=100 \
  -Pseed=12345 \
  -Pspice=2

# Output: reports/card_audit_*.csv
```

**Quality Sweep (All Games):**
```bash
./gradlew :app:cardQuality \
  -Pcount=80 \
  -Pseeds=701,702,703,704,705 \
  -Pspice=2

# Output: reports/quality_*.{json,csv,html}
```

**Baseline Comparison:**
```bash
# Generate new baseline
./gradlew :app:cardAudit -Pgame=ROAST_CONSENSUS -Pcount=50 -Pseed=999

# Compare to baseline
python tools/card_audit_diff.py \
  docs/card_audit_baselines/audit_ROAST_CONSENSUS_*.csv \
  reports/audit_ROAST_CONSENSUS_*.csv
```

### Test Coverage

**Generate Coverage Report:**
```bash
./gradlew testDebugUnitTest jacocoTestReport

# View report
open app/build/reports/jacoco/jacocoTestReport/html/index.html
```

**Coverage Goals:**
- Overall: 70%+
- Core engine: 85%+
- Content generation: 75%+
- UI components: 50%+ (Compose testing complex)

---

## Code Quality

### Quick Commands (Makefile)

```bash
# Apply all autofixes
make fix

# Check without changes (CI-safe)
make check

# Run tests
make test

# Full CI pipeline
make ci
```

### Kotlin Quality

**Format Code:**
```bash
# ktlint formatting
./gradlew ktlintFormat

# Spotless formatting
./gradlew spotlessApply

# Both
make format-kotlin
```

**Static Analysis:**
```bash
# Detekt (with autocorrect)
./gradlew detekt

# Check only (no fixes)
./gradlew detektMain detektTest
```

**Configuration:**
- `.editorconfig` - Editor settings
- `config/detekt.yml` - Detekt rules
- `build.gradle` - Spotless configuration

### Python Quality

**Format & Lint:**
```bash
# Format with ruff
ruff format loader/ tools/ --exclude third_party

# Lint with ruff
ruff check --fix loader/ tools/ --exclude third_party

# Both
make format-python
```

**Configuration:**
- `pyproject.toml` - Ruff settings
- `.pre-commit-config.yaml` - Pre-commit hooks

### Pre-commit Hooks

**Install:**
```bash
pip install pre-commit
pre-commit install
```

**Run Manually:**
```bash
pre-commit run --all-files
```

**Hooks:**
- trailing-whitespace
- end-of-file-fixer
- check-yaml
- ruff-format
- ruff-lint

### CI/CD Workflows

**GitHub Actions:**

`.github/workflows/quality.yml`:
```yaml
jobs:
  kotlin-quality:
    - ktlint check
    - detekt check
    - spotless check
  
  python-quality:
    - ruff format --check
    - ruff check
  
  tests:
    - ./gradlew test
```

**Required Status Checks:**
- ✅ Kotlin Code Quality
- ✅ Python Code Quality
- ✅ Unit Tests

**Branch Protection:**
- Require PR before merge
- Require status checks to pass
- Require up-to-date branches

---

## Development Workflow

### Git Workflow

**Branch Naming:**
```bash
feature/add-new-game-mode
bugfix/fix-llm-timeout
hotfix/critical-crash
refactor/simplify-validator
docs/update-architecture
```

**Commit Messages:**
```bash
# Format: type(scope): description

feat(games): add Reality Check game mode
fix(llm): handle timeout gracefully in LLMCardGeneratorV2
refactor(validation): simplify SemanticValidator logic
docs(api): update GameEngine API reference
test(generator): add CardGeneratorV3 unit tests
```

**Typical Flow:**
```bash
# Create feature branch
git checkout -b feature/my-feature

# Make changes
# ... edit files ...

# Format code
make fix

# Run tests
make test

# Commit
git add .
git commit -m "feat(feature): add my feature"

# Push
git push origin feature/my-feature

# Open PR on GitHub
```

### Adding New Game Mode

**1. Define Game Metadata:**

`GameMetadata.kt`:
```kotlin
GameIds.MY_GAME to GameInfo(
    id = GameIds.MY_GAME,
    title = "My Game",
    description = "Game description",
    category = GameCategory.MAIN,
    difficulty = GameDifficulty.MEDIUM,
    timerSec = 30,
    minPlayers = 3,
    maxPlayers = 16,
    interaction = Interaction.MY_INTERACTION,
    interactionType = InteractionType.MY_TYPE,
    tags = setOf("tag1", "tag2"),
    spice = 2
)
```

**2. Create Interaction Renderer:**

`ui/interactions/MyGameRenderer.kt`:
```kotlin
@Composable
fun MyGameRenderer(
    card: FilledCard,
    options: GameOptions.MyGameOptions,
    onEvent: (RoundEvent) -> Unit
) {
    // UI implementation
}
```

**3. Add Gold Cards:**

`assets/gold_cards.json`:
```json
{
  "games": {
    "my_game": {
      "cards": [
        {
          "text": "Example card",
          "quality_score": 9,
          "spice": 2
        }
      ]
    }
  }
}
```

**4. Create Templates (Optional):**

`assets/templates_v3/my_game.json`:
```json
[
  {
    "id": "my_game_001",
    "game": "my_game",
    "text": "Template with {slot1} and {slot2}",
    "slots": [
      {"name": "slot1", "type": "social_reason"},
      {"name": "slot2", "type": "meme_item"}
    ]
  }
]
```

**5. Update Documentation:**
- Add to `HDRealRules.md`
- Update `FEATURES.md`
- Add to `README.md`

**6. Add Tests:**
```kotlin
@Test
fun `my game generates valid cards`() {
    val card = engine.next(Request(
        gameId = "my_game",
        players = testPlayers
    ))
    assertNotNull(card)
    assertTrue(card.filledCard.game == "my_game")
}
```

### Code Review Checklist

**Before Submitting PR:**
- [ ] Code formatted (`make fix`)
- [ ] Tests pass (`make test`)
- [ ] Documentation updated
- [ ] No console errors
- [ ] Tested on physical device
- [ ] Screenshots/video for UI changes
- [ ] CHANGELOG.md updated

**Reviewer Checklist:**
- [ ] Code follows Kotlin conventions
- [ ] Tests cover new functionality
- [ ] No performance regressions
- [ ] Documentation is clear
- [ ] Edge cases handled
- [ ] Error handling present

---

## Debugging

### Logcat Filtering

**View HELLDECK Logs:**
```bash
# All app logs
adb logcat -s HELLDECK_DEBUG

# Specific tags
adb logcat -s GameEngine:D ContentGen:D

# Clear and monitor
adb logcat -c && adb logcat | grep -i helldeck
```

**Log Levels:**
```kotlin
Logger.v("Verbose message")    // VERBOSE
Logger.d("Debug message")      // DEBUG
Logger.i("Info message")       // INFO
Logger.w("Warning message")    // WARNING
Logger.e("Error message", ex)  // ERROR
```

### Android Studio Debugger

**Breakpoints:**
- Click gutter next to line number
- Right-click breakpoint → Condition for conditional breaks
- View → Tool Windows → Debug

**Evaluate Expression:**
- While paused, select expression
- Alt+F8 → Evaluate Expression

**Inspect Database:**
- View → Tool Windows → App Inspection
- Select HELLDECK process
- Database Inspector shows Room tables live

### Card Lab (Interactive Testing)

**Access:**
- Settings → Developer Options → Open Card Lab

**Features:**
- Select any game mode
- Adjust spice level (1-5)
- Generate cards on demand
- View quality metrics
- Inspect JSON structure

### Native Debugging

**lldb for C++:**
```bash
# Attach debugger
adb shell am start -D -n com.helldeck/.MainActivity
adb forward tcp:5039 jdwp:$(adb shell pidof com.helldeck)

# In another terminal
lldb
(lldb) platform select remote-android
(lldb) platform connect unix-abstract-connect:///data/local/tmp/debug.sock
(lldb) attach --pid <pid>
```

**Check Native Crashes:**
```bash
adb logcat | grep -i "fatal\|crash\|sigsegv"
```

### Memory Profiling

**Android Studio Profiler:**
- View → Tool Windows → Profiler
- Select HELLDECK process
- Monitor Memory, CPU, Network

**Check for Leaks:**
```bash
# Trigger GC and dump heap
adb shell am dumpheap com.helldeck /data/local/tmp/heap.hprof
adb pull /data/local/tmp/heap.hprof

# Analyze with Android Studio:
# File → Open → Select heap.hprof
```

---

## Performance

### Benchmarking

**Card Generation Speed:**
```kotlin
val start = System.nanoTime()
val card = engine.next(request)
val duration = (System.nanoTime() - start) / 1_000_000.0
Logger.d("Generation took ${duration}ms")
```

**Target Metrics:**
- LLM generation: <2500ms
- Template generation: <100ms
- Gold fallback: <10ms
- UI render: <16ms (60fps)

### Optimization Tips

**Asset Loading:**
```kotlin
// Load once, cache in memory
private val lexicons by lazy {
    loadLexiconsFromAssets()
}
```

**Database Queries:**
```kotlin
// Use coroutines for DB operations
@Dao
interface TemplateStatsDao {
    @Query("SELECT * FROM template_stats WHERE templateId = :id")
    suspend fun getStats(id: String): TemplateStatEntity?
}
```

**Compose Performance:**
```kotlin
// Use remember for expensive calculations
val expensiveValue = remember(dependency) {
    calculateExpensiveValue()
}

// Use derivedStateOf for derived state
val isValid by remember {
    derivedStateOf { input.length > 5 }
}
```

---

## Common Issues

### Build Issues

**Problem: "Could not resolve dependencies"**
```bash
# Solution: Clear cache and rebuild
./gradlew clean
./gradlew --refresh-dependencies :app:assembleDebug
```

**Problem: "NDK not found"**
```bash
# Solution: Install NDK via SDK Manager
# Android Studio → Settings → Android SDK → SDK Tools → NDK
# Or set ANDROID_NDK_HOME environment variable
```

**Problem: "Out of memory during build"**
```bash
# Solution: Increase Gradle heap
# ~/.gradle/gradle.properties
org.gradle.jvmargs=-Xmx4096m
```

### Runtime Issues

**Problem: "LLM not loading"**
```bash
# Check if models exist
adb shell ls /data/data/com.helldeck/files/models/

# Check logcat for errors
adb logcat | grep -i llm
```

**Problem: "Cards repeating"**
```bash
# Reset session
# In app: Home → Start New Game Night
# Or export/import brainpack to reset
```

**Problem: "App crashes on startup"**
```bash
# Check crash logs
adb logcat | grep -E "AndroidRuntime|FATAL"

# Clear app data
adb shell pm clear com.helldeck
```

### Testing Issues

**Problem: "Tests fail with UninitializedPropertyAccessException"**
```kotlin
// Solution: Initialize lateinit vars in @Before
@Before
fun setup() {
    MockKAnnotations.init(this)
    // Initialize dependencies
}
```

**Problem: "Compose tests timeout"**
```kotlin
// Solution: Use composeTestRule.waitUntil
composeTestRule.waitUntil(timeoutMillis = 5000) {
    // Condition
}
```

---

## Additional Resources

### Documentation
- [README.md](README.md) - Project overview
- [ARCHITECTURE.md](ARCHITECTURE.md) - System design
- [FEATURES.md](FEATURES.md) - Feature catalog
- [API.md](docs/API.md) - API reference
- [TROUBLESHOOTING.md](docs/TROUBLESHOOTING.md) - Common problems

### External Resources
- [Android Developer Docs](https://developer.android.com/)
- [Jetpack Compose Guide](https://developer.android.com/jetpack/compose)
- [Kotlin Language Reference](https://kotlinlang.org/docs/)
- [llama.cpp Documentation](https://github.com/ggerganov/llama.cpp)

### Community
- GitHub Issues: Report bugs
- GitHub Discussions: Ask questions
- Pull Requests: Contribute code

---

**Last Updated:** January 2026  
**Maintainers:** HELLDECK Development Team  
**Status:** Active Development
