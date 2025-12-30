o# HELLDECK
Single phone. One card per round. 14 mini-games. Learns what your crew finds funny.

## 🚨 Recent Major Updates (December 2024)

**Complete architectural overhaul with end-to-end rule enforcement:**

- ✅ **Engine Authority**: UI now renders exactly what GameEngine produces (no more UI recomputing options incorrectly)
- ✅ **Contract Validation**: All cards validated against game rules before display (prevents nonsense combinations)
- ✅ **Semantic Coherence Fixed**: Validator now uses slot types (not names) to prevent absurd combinations
- ✅ **Session Persistence**: Session IDs persist across rounds for proper anti-repetition
- ✅ **Feedback Loop**: Consistent LOL/MEH/TRASH → reward mapping improves content over time
- ✅ **Gold Fallbacks**: Guaranteed-valid fallback cards for all 14 interaction types
- ✅ **Unit Tests**: SemanticValidator tests ensure validation correctness

See [CHANGELOG.md](CHANGELOG.md) for complete details.

## 🎮 Game Overview

HELLDECK is a party game system designed for 3-16 players using a single Android device. The game features 14 unique mini-games that adapt to your group's sense of humor through machine learning.

### Game Modes
- **Roast Consensus** - Vote on the most likely target for a scenario
- **Confession or Cap** - Truth or bluff game with room voting
- **Poison Pitch** - Sell your side of a "Would You Rather" scenario
- **Fill-In Finisher** - Judge reads prompt and fills first blank; others write punchlines; judge picks favorite
- **Red Flag Rally** - Defend a dating scenario despite red flags
- **Hot Seat Imposter** - Everyone answers as the target player
- **Text Thread Trap** - Choose the perfect reply tone to a message
- **Taboo Timer** - Get your team to guess a word without saying forbidden terms
- **Odd One Out** - Pick the misfit from three options and explain why
- **Title Fight** - Mini-duels to challenge the current champion
- **Alibi Drop** - Weave secret words into an excuse without detection
- **Hype or Yike** - Pitch a ridiculous product with a straight face
- **Scatterblast** - Name three things in a category starting with a letter
- **Majority Report** - Predict how the room will vote before they do

## 🚀 Quick Start

### Prerequisites
- Android Studio Giraffe (2023.3.1) or later
- Android device with USB debugging enabled
- Python 3.7+ (for desktop loader)

### Build & Run

**Important**: Building requires network access for Gradle dependency resolution.

1. **Using System Gradle (if wrapper fails)**
   ```bash
   # If gradle wrapper is missing or broken, use system gradle:
   gradle :app:assembleDebug
   # Or for release:
   gradle :app:assembleRelease
   ```

2. **Using Gradle Wrapper (recommended)**
   ```bash
   ./gradlew :app:assembleRelease
   # Or for debug build:
   ./gradlew :app:assembleDebug
   ```

3. **Open in Android Studio**
   ```bash
   # Open the project root directory in Android Studio
   # Build → Make Project (Ctrl+F9)
   # Run → Run 'app' (Shift+F10)
   ```

3. **Install using Desktop Loader (optional)**
   ```bash
   # Install Python dependencies
   pip install -r loader/requirements.txt

   # Run the installer
   python loader/helldeck_loader.py
   ```

4. **Desktop Loader Steps**
   - Click **Browse** to select your built APK
   - Click **Install APK** to push to device
   - Click **Set Device Owner** (freshly reset device only)
   - Click **Reboot Device**

## 🎯 Controls

- **Long-press** = draw new card
- **Left/Right tap** = cycle options (where applicable)
- **Center tap** = confirm selection
- **Two-finger tap** = back/undo
- **Vibrate** = phase change indicator
- **Torch flash** = scoring lock confirmation

## 🧠 Brainpacks

The game learns from your feedback to show funnier content over time, and you can now back up that history.

- **Export learned data**: Home → **Export Brain** creates a zipped brainpack under the app cache (`cache/brainpacks/<name>.zip`) containing `brainpack.json`.
- **Import on new device**: Home → **Import Brain** and pick any brainpack `.zip`. Players, template stats, and recent exposures merge into the current database.
- Brainpacks are human-readable JSON – perfect for versioning or sanity-checking before sharing with other crews.

## 🏗️ Project Structure

```
helldeck/
├── loader/                  # Desktop installation tool
│   ├── helldeck_loader.py   # Python ADB wrapper with GUI
│   └── requirements.txt     # Python dependencies
├── app/                     # Android application
│   ├── src/main/
│   │   ├── java/com/helldeck/
│   │   │   ├── MainActivity.kt      # App entry point
│   │   │   ├── HelldeckApp.kt       # Application class
│   │   │   ├── admin/               # Device admin receiver
│   │   │   ├── data/                # Room database layer
│   │   │   ├── engine/              # Game logic and AI
│   │   │   └── ui/                  # Jetpack Compose UI
│   │   ├── res/                     # Android resources
│   │   └── assets/                  # Game data and config
│   │       ├── settings/default.yaml
│   │       ├── templates/templates.json
│   │       └── lexicons/            # Word lists for each slot type
│   └── build.gradle
├── settings.gradle          # Gradle project configuration
├── build.gradle            # Root build configuration
└── README.md              # This file
```

## 🔧 Development

### LLM-Powered Card Generation (Primary)

HELLDECK uses on-device language models (TinyLlama/Qwen) for quality-first card generation:

- **Primary generator**: `LLMCardGeneratorV2` generates unique cards using quality-focused prompts
- **Gold examples**: High-quality curated cards in `app/src/main/assets/gold_cards_v2.json` guide LLM prompts and serve as emergency fallbacks
- **Spice → Temperature**: Spice level (1-5) controls LLM creativity (0.5-0.9 temperature)
- **Quality validation**: Cards must pass quality score (≥0.6), cliché filtering, and length checks
- **3-retry strategy**: Up to 3 attempts with 2.5s timeout each before falling back

Fallback chain:
1. **LLM generation** (primary)
2. **Gold cards** (`gold_cards_v2.json`)
3. **Template system** (legacy `CardGeneratorV3`)

### Legacy Template System (Fallback Only)

The template-based system serves as fallback when LLM is unavailable:

- Blueprints: `app/src/main/assets/templates_v3/` (per-game JSON lists)
- Lexicons: `app/src/main/assets/lexicons_v2/` (typed word lists with metadata)
- Model artifacts: `app/src/main/assets/model/` (rules, priors, pairings, banned lists)

### Card Lab & Diagnostics

- Card Lab: Settings → Developer → Open Card Lab
- CLI audit: `./gradlew :app:cardAudit -Pgame=POISON_PITCH -Pcount=100 -Pseed=12345`
- Quality sweeps: `./gradlew :app:cardQuality -Pcount=80 -Pseeds=701,702,703,704,705,706,707,708 -Pspice=2`
- Lint lexicons: `python tools/lexicon_lint.py`

### Configuration

Game behavior is controlled by `app/src/main/assets/settings/default.yaml` which is now loaded on startup (with automatic fallback to hard-coded defaults if parsing fails):
- `learning`: AI adaptation parameters
- `timers`: phase timing in milliseconds
- `players`: player count preferences
- `scoring`: point values and thresholds
- `mechanics`: game rule toggles

### Local LLMs

- Offline models live in `app/src/main/assets/models/` (e.g. TinyLlama, Qwen) and are bundled with the APK by default. On first launch they copy to internal storage and load on a background thread; the UI remains responsive.
- The native bridge `helldeck_llama` is shipped in the app. If `third_party/llama.cpp` is present at build time it links to the full implementation; otherwise a safe stub is bundled and the engine falls back to authored copy.
- Paraphrasing & classification happen automatically once the model is ready — no toggle required. If a model isn’t available the system gracefully falls back.

### Card Generator V3 (offline)

- Blueprints live in `app/src/main/assets/templates_v3/` and describe sentence structures with typed slots.
- Lexicon V2 assets (`app/src/main/assets/lexicons_v2/`) provide entries with metadata (spice, locality, tone) for each slot type.
- Offline trained artifacts in `app/src/main/assets/model/` supply blueprint priors, slot pair compatibility weights, a tiny logistic scorer, and safety lists.
- The generator tries up to three blueprints per card. If all fail validation, it immediately serves a curated gold card from `app/src/main/assets/gold/gold_cards.json`.
- Runtime flags (in `settings/default.yaml`): `safe_mode_gold_only` forces gold cards; `enable_v3_generator` activates the CSP + scoring pipeline. Thresholds live in `model/rules.yaml`.

### Testing quick-start

- Run `./run_tests.sh` for the focused JVM checks (engine heuristics, YAML loader, brainpack round-trip). Full `./gradlew test` still includes Compose UI suites that require instrumentation; execute those from Android Studio or via `./gradlew connectedAndroidTest` on a device/emulator.

**New tests added:**
- `SemanticValidatorTest`: Validates semantic coherence logic
- Contract validation framework (tests in development)
- Generation smoke tests (framework established)

### Cleaning build artifacts

- Use `./gradlew clean` to remove all compiled outputs, including native builds under `app/.cxx` and Gradle intermediates under `app/build`.
- For a manual nuke of cached brainpacks or models during debugging, clear `cache/brainpacks/` and `files/models/` from the app’s sandbox on device/emulator.

## 🎨 UI Architecture

The app uses Jetpack Compose with a custom dark theme optimized for party environments. Key components:

- **BigZones**: Three large touch zones for easy interaction
- **CardFace**: Main game card display
- **FeedbackStrip**: Post-round rating interface
- **AvatarVoteFlow**: Player selection voting
- **ABVoteFlow**: Binary choice voting
- **JudgePickFlow**: Judge selection interface

### Newly added (2025-10)
- **RollcallScene**: “Who’s here?” attendance at launch or any time from Home.
  - Toggle present players, quick add with emoji, swipe-to-delete with confirm + Undo.
- **Settings** additions:
  - Toggle “Ask ‘Who’s here?’ at launch”, manage players inline (add/toggle active), and jump to full Players.
- **Players management**:
  - Inline name edit (tap name → edit → save/cancel), tap avatar to change emoji via picker.
  - Swipe-to-delete with confirmation dialog and Undo snackbar.
- **EmojiPicker** bottom sheet:
  - 200+ emoji across categories with search (names/keywords) and paste support.
- **Home polish**:
  - Top bar quick actions: Rollcall, Scores, Stats, Rules, Settings.
  - Quick-access buttons row includes Rollcall, Rules, Settings.
- **Accessibility/legibility**:
  - Dark theme neutrals retuned; card titles/subtitles now ellipsize and wrap safely.
  - Progress indicators migrated to the latest Compose API.

## 🤖 AI Learning System

The game uses a custom learning algorithm that:
- Tracks which templates perform well
- Adapts to group preferences over time
- Balances variety with proven winners
- Uses epsilon-greedy exploration strategy

**Feedback rewards (now consistent):**
- LOL = 1.0 (best possible)
- MEH = 0.35 (below average but not banned)
- TRASH = 0.0 (effectively bans the template)

Rewards are persisted per template and bias future selection toward higher-rated content.

## 🔒 Kiosk Mode

The app includes device administrator functionality to:
- Lock the device to the HELLDECK app
- Prevent navigation away from the game
- Disable system UI elements
- Enable immersive fullscreen mode

## 📱 System Requirements

- **Minimum Android API**: 21 (Android 5.0)
- **Target Android API**: 34 (Android 14)
- **Permissions**: Camera (torch), Vibrator, Wake Lock
- **Storage**: ~10MB for app + assets

## 🐛 Troubleshooting

**App won't install:**
- Enable "Install from unknown sources"
- Check USB debugging is enabled
- Try `adb devices` to verify connection

**Kiosk mode not working:**
- Device must be freshly reset for device owner setup
- Alternative: Use system settings → Security → Screen pinning

**Game not learning properly:**
- Export/Import brainpack to transfer learned data
- Check that feedback is being recorded in rounds table

## 📄 License

This project is designed for personal and educational use. The game mechanics and assets are custom-created for this implementation.

## 🤝 Contributing

To contribute improvements:
1. Test changes thoroughly across different devices
2. Ensure kiosk mode still functions correctly
3. Maintain the single-phone, party-game focus
4. Add appropriate documentation for new features

---

*Built with ❤️ for party game enthusiasts everywhere*
