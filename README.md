# HELLDECK 🎯

> The AI-powered party game that learns your crew's sense of humor

**One Device. 14 Mini-Games. Infinite Chaos.**

HELLDECK is an Android party game for 3-16 players that combines classic party game mechanics with on-device AI to generate personalized content. The game learns from player feedback to deliver funnier, more targeted cards over time.

## What Makes HELLDECK Different

- **On-Device AI**: Uses TinyLlama/Qwen language models running locally for privacy and speed
- **Adaptive Learning**: Thompson Sampling algorithm improves content based on LOL/MEH/TRASH feedback
- **Quality-First Generation**: LLM generates unique cards with gold standard examples as guidance
- **Smart Fallback Chain**: LLM → Curated Gold Cards → Template System ensures playable content
- **Contract Validation**: Every card validated against game rules before display
- **Brainpack Export/Import**: Save and share your crew's learned preferences

## The 14 Game Modes

All games sourced from **[HDRealRules.md](HDRealRules.md)** - the canonical game design document.

1. 🎯 **Roast Consensus** (20s) - Vote for who best fits the roast prompt
2. 🤥 **Confession or Cap** (15s) - TRUE/FALSE confessions with lie detection
3. 💀 **Poison Pitch** (30s) - Debate two terrible "Would You Rather" options
4. ✍️ **Fill-In Finisher** (60s) - Judge sets up, others write punchlines
5. 🚩 **Red Flag Rally** (45s) - Defend undateable people, vote SMASH/PASS
6. 🎭 **Hot Seat Imposter** (15s) - Impersonate a player, fool the room
7. 📱 **Text Thread Trap** (15s) - Reply to texts in mandatory tones
8. ⏱️ **Taboo Timer** (60s) - Describe words without forbidden terms
9. 📐 **The Unifying Theory** (30s) - Connect three unrelated items
10. 🥊 **Title Fight** (15s) - Instant head-to-head challenges
11. 🕵️ **Alibi Drop** (30s) - Hide mandatory words in your alibi
12. 🪞 **Reality Check** (20s) - Self-awareness test with group consensus
13. 💣 **Scatterblast** (10-60s) - Category listing before bomb explodes
14. 📉 **Over/Under** (20s) - Bet on personal statistics

**Full rules:** See [HDRealRules.md](HDRealRules.md) for complete gameplay mechanics and scoring.

## � Documentation Index

- **[README.md](README.md)** - This file (quick start, overview)
- **[ARCHITECTURE.md](ARCHITECTURE.md)** - System design, data flow, component breakdown
- **[FEATURES.md](FEATURES.md)** - Complete feature documentation (gameplay, AI, UI)
- **[API.md](docs/API.md)** - Developer API reference
- **[DEVELOPMENT.md](DEVELOPMENT.md)** - Development setup, building, testing
- **[DEPLOYMENT.md](docs/DEPLOYMENT.md)** - Build configuration, release process
- **[CHANGELOG.md](CHANGELOG.md)** - Version history and changes
- **[CONTRIBUTING.md](CONTRIBUTING.md)** - Contribution guidelines
- **[HDRealRules.md](HDRealRules.md)** - Official game rules (source of truth)

## 🚀 Quick Start

### Prerequisites
- **Android Studio** Giraffe (2023.3.1) or later
- **Android SDK** 21+ (target: 34)
- **Kotlin** 1.9.25+
- **Java** 17+
- **Python** 3.7+ (optional, for tools)
- **Device** Android 5.0+ with USB debugging

### Build & Run

**Method 1: Android Studio (Recommended)**
```bash
# Clone repository
git clone https://github.com/your-org/HELLDECK.git
cd HELLDECK

# Open in Android Studio
# File → Open → Select HELLDECK directory
# Wait for Gradle sync
# Run → Run 'app' (Shift+F10)
```

**Method 2: Command Line**
```bash
# Debug build
./gradlew :app:assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk

# Release build
./gradlew :app:assembleRelease
adb install -r app/build/outputs/apk/release/app-release.apk
```

**Method 3: Desktop Loader (GUI)**
```bash
cd loader
pip install -r requirements.txt
python helldeck_loader.py
```

> **Note:** Builds require network access for Gradle dependency resolution.

## � Controls

| Gesture | Action |
|---------|--------|
| **Long-press** | Draw new card |
| **Left/Right tap** | Cycle options (where applicable) |
| **Center tap** | Confirm selection |
| **Two-finger tap** | Back/undo |
| **Vibrate** | Phase change indicator |
| **Torch flash** | Scoring lock confirmation |

## 🧠 Brainpacks

The game learns from your feedback to show funnier content over time, and you can now back up that history.

- **Export learned data**: Home → **Export Brain** creates a zipped brainpack under the app cache (`cache/brainpacks/<name>.zip`) containing `brainpack.json`.
- **Import on new device**: Home → **Import Brain** and pick any brainpack `.zip`. Players, template stats, and recent exposures merge into the current database.
- Brainpacks are human-readable JSON – perfect for versioning or sanity-checking before sharing with other crews.

## 🏗️ Project Structure

```
HELLDECK/
├── app/                              # Android application
│   ├── src/main/
│   │   ├── java/com/helldeck/
│   │   │   ├── MainActivity.kt       # App entry point
│   │   │   ├── HelldeckApp.kt        # Application class  
│   │   │   ├── content/              # Content generation system
│   │   │   │   ├── engine/           # GameEngine, OptionsCompiler
│   │   │   │   ├── generator/        # LLMCardGeneratorV2, CardGeneratorV3
│   │   │   │   ├── validation/       # Contract & semantic validators
│   │   │   │   └── model/            # FilledCard, GameOptions
│   │   │   ├── engine/               # Core game mechanics
│   │   │   │   ├── GameMetadata.kt   # 14 game definitions
│   │   │   │   ├── Config.kt         # YAML config loader
│   │   │   │   └── PlayerManager.kt  # Player state management
│   │   │   ├── ui/                   # Jetpack Compose UI
│   │   │   │   ├── Scenes.kt         # Main UI orchestrator
│   │   │   │   ├── scenes/           # 16 scene composables
│   │   │   │   ├── interactions/     # 14 game renderers
│   │   │   │   └── vm/               # GameNightViewModel
│   │   │   ├── data/                 # Room database entities
│   │   │   ├── llm/                  # LocalLLM, ModelManager
│   │   │   └── analytics/            # Telemetry, metrics
│   │   ├── assets/
│   │   │   ├── gold_cards.json       # 700 curated cards (50/game)
│   │   │   ├── lexicons_v2/          # 28 typed word lists
│   │   │   ├── templates_v3/         # 17 blueprint files
│   │   │   ├── model/                # Trained artifacts, rules
│   │   │   ├── settings/             # default.yaml config
│   │   │   └── models/               # LLM .gguf files (bundled)
│   │   └── cpp/                      # Native llama.cpp bridge
│   └── build.gradle                  # App build config
├── third_party/llama.cpp/            # Submodule for LLM inference
├── Heimdall/                         # USB device filtering (macOS)
├── loader/                           # Python GUI installer
├── tools/                            # Quality verification scripts
├── docs/                             # Extended documentation
├── build.gradle                      # Root build config
└── CMakeLists.txt                    # Native build config
```

## 🔧 Development

See **[DEVELOPMENT.md](DEVELOPMENT.md)** for complete development guide.

### Quick Commands

**Quality Checks (Makefile):**
```bash
make fix      # Apply all autofixes (format + lint)
make check    # Run checks without changes (CI-safe)
make test     # Run unit tests
make ci       # Full CI pipeline
```

**Build & Test:**
```bash
./gradlew :app:assembleDebug          # Build debug APK
./gradlew :app:assembleRelease        # Build release APK
./gradlew test                        # Run unit tests
./gradlew connectedAndroidTest        # Run instrumented tests
```

**Card Quality Tools:**
```bash
# Audit specific game (generates CSV report)
./gradlew :app:cardAudit -Pgame=POISON_PITCH -Pcount=100 -Pseed=12345

# Quality sweep across all games
./gradlew :app:cardQuality -Pcount=80 -Pseeds=701,702,703 -Pspice=2

# Lint lexicons
python tools/lexicon_lint.py
```

## 🧬 Content Generation System

**Three-tier fallback chain ensures playable content:**

### 1. LLM Generation (Primary)
- **Generator**: `LLMCardGeneratorV2` with TinyLlama/Qwen models
- **Strategy**: Quality-focused prompts with gold card examples
- **Temperature**: Spice level (1-5) → (0.5-0.9 temperature)
- **Validation**: Quality score ≥0.6, cliché filtering, length checks
- **Retry**: 3 attempts, 2.5s timeout each
- **Location**: `app/src/main/java/com/helldeck/content/generator/LLMCardGeneratorV2.kt`

### 2. Gold Cards (Fallback)
- **Count**: 700 curated cards (50 per game)
- **Quality**: All cards rated 9-10/10
- **Format**: Structured JSON with metadata
- **Location**: `app/src/main/assets/gold_cards.json`

### 3. Template System (Final Fallback)
- **Generator**: `CardGeneratorV3` with blueprint CSP solver
- **Blueprints**: 17 per-game template files
- **Lexicons**: 28 typed word lists with metadata
- **Artifacts**: Trained priors, compatibility weights, banned lists
- **Location**: `app/src/main/assets/templates_v3/`, `lexicons_v2/`, `model/`

**See [ARCHITECTURE.md](ARCHITECTURE.md) for technical details.**

## 🎨 UI Components

**Jetpack Compose + Material3** with custom dark theme for party environments.

**Core Interaction Components:**
- `VotePlayerRenderer` - Avatar-based voting (Roast Consensus)
- `ABChoiceRenderer` - Binary choice voting (Poison Pitch)
- `JudgePickRenderer` - Judge selection (Fill-In Finisher)
- `ReplyToneRenderer` - Tone selection (Text Thread Trap)
- `TabooGuessRenderer` - Word guessing with forbidden terms
- `TargetSelectRenderer` - Player targeting (Reality Check)
- `HideWordsRenderer` - Word smuggling (Alibi Drop)
- `SpeedListRenderer` - Category listing (Scatterblast)

**Feature Screens:**
- `RollcallScene` - Player attendance with emoji avatars
- `GameRulesScene` - In-game rule reference
- `CardLabScene` - Developer card testing tool
- `StatsScreen` - Player performance analytics
- `SettingsScreen` - Configuration and preferences

**See [FEATURES.md](FEATURES.md) for complete UI feature documentation.**

## 🤖 AI Learning System

**Thompson Sampling** with contextual bandit optimization:

- **Algorithm**: `ContextualSelector` using Upper Confidence Bound (UCB)
- **Feedback**: LOL (1.0) > MEH (0.35) > TRASH (0.0)
- **Persistence**: Template statistics stored in Room database
- **Anti-Repetition**: Session-based exposure tracking
- **Exploration**: Epsilon-greedy strategy balances variety vs quality

**Database Entities:**
- `TemplateStatEntity` - Visit counts, reward sums per template
- `TemplateExposureEntity` - Recent exposures per session
- `GameNightEntity` - Session tracking for game nights

**See [ARCHITECTURE.md](ARCHITECTURE.md) for learning algorithm details.**

## 🔒 Kiosk Mode

**Device administrator features for dedicated party devices:**

- **App Locking**: Device locked to HELLDECK only
- **UI Suppression**: System UI elements hidden
- **Fullscreen**: Immersive mode for distraction-free gameplay
- **Setup**: Requires freshly reset device for device owner assignment
- **Alternative**: Use Android's built-in Screen Pinning (Settings → Security)

**Implementation:** `HelldeckDeviceAdminReceiver.kt`, `Kiosk.kt`

## 📱 System Requirements

| Requirement | Specification |
|-------------|---------------|
| **Minimum OS** | Android 5.0 (API 21) |
| **Target OS** | Android 14 (API 34) |
| **Architecture** | arm64-v8a (ARMv8 64-bit) |
| **RAM** | 2GB+ recommended for LLM inference |
| **Storage** | ~150MB (app + models + assets) |
| **Permissions** | Camera (torch), Vibrator, Wake Lock |
| **Network** | Optional (build-time only) |

## 🐛 Troubleshooting

**Build Issues:**
```bash
# Clean build artifacts
./gradlew clean

# Sync Gradle and rebuild
./gradlew --refresh-dependencies :app:assembleDebug

# Check native dependencies
cd third_party/llama.cpp && git submodule update --init
```

**Installation Issues:**
- Enable "Install from unknown sources" in device settings
- Verify USB debugging: `adb devices`
- Check storage space (need ~150MB)

**Runtime Issues:**
- **LLM not loading**: Check `files/models/` directory on device
- **Cards repeating**: Export/import brainpack to reset session
- **Kiosk mode fails**: Requires factory reset device for device owner

**See [TROUBLESHOOTING.md](docs/TROUBLESHOOTING.md) for complete guide.**

## 📊 Tech Stack

| Layer | Technology |
|-------|------------|
| **UI** | Jetpack Compose, Material3, Compose Navigation |
| **Language** | Kotlin 1.9.25, Java 17 |
| **Database** | Room 2.6.1, DataStore Preferences |
| **AI/ML** | llama.cpp (TinyLlama, Qwen), Thompson Sampling |
| **Serialization** | Kotlinx Serialization, Gson, SnakeYAML |
| **Quality** | ktlint, detekt, spotless, ruff |
| **Testing** | JUnit 5, MockK, Robolectric, Compose Testing |
| **Build** | Gradle 8.5.2, CMake 3.22.1, KSP 1.9.25 |
| **Native** | C++17, NDK, llama.cpp submodule |

## 🤝 Contributing

See **[CONTRIBUTING.md](CONTRIBUTING.md)** for contribution guidelines.

**Quick checklist:**
- ✅ Read [HDRealRules.md](HDRealRules.md) for game design principles
- ✅ Follow code style guidelines (ktlint, detekt)
- ✅ Add tests for new features
- ✅ Update documentation
- ✅ Test on physical device (3+ players ideal)

## 📄 License

This project is designed for personal and educational use. Game mechanics and content are original creations.

## 🙏 Acknowledgments

- **llama.cpp** - Georgi Gerganov and contributors
- **TinyLlama** - StatNLP Research Group
- **Qwen** - Alibaba Cloud
- **Jetpack Compose** - Google Android Team

---

**Last Updated:** January 2026  
**Version:** 1.0.1  
**Build:** See `BuildConfig.GIT_HASH` for commit

*Built with ❤️ for party game enthusiasts everywhere*
