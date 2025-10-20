# HELLDECK
Single phone. One card per round. 14 mini-games. Learns what your crew finds funny.

## 🎮 Game Overview

HELLDECK is a party game system designed for 3-16 players using a single Android device. The game features 14 unique mini-games that adapt to your group's sense of humor through machine learning.

### Game Modes
- **Roast Consensus** - Vote on the most likely target for a scenario
- **Confession or Cap** - Truth or bluff game with room voting
- **Poison Pitch** - Sell your side of a "Would You Rather" scenario
- **Fill-In Finisher** - Complete a prompt with the funniest punchline
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

1. **Open in Android Studio**
   ```bash
   # Open the project root directory in Android Studio
   ```

2. **Build APK**
   ```bash
   ./gradlew :app:assembleRelease
   # Or for debug build:
   ./gradlew :app:assembleDebug
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

The game learns from your feedback to show funnier content over time.

- **Export learned data**: Home → **Export Brain** (creates `.hhdb` file)
- **Import on new device**: Home → **Import Brain** (select `.hhdb` file)

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

### Adding New Templates

Templates are stored in `app/src/main/assets/templates/templates.json`. Each template needs:
- `id`: unique identifier
- `game`: which mini-game it belongs to
- `text`: template with `{slot_name}` placeholders
- `family`: grouping for variety tracking
- `spice`: humor intensity (1-3)
- `locality`: cultural specificity (1-3)

### Adding New Lexicons

Word lists are stored in `app/src/main/assets/lexicons/`. Each JSON file contains an array of strings for a specific slot type (friends, places, memes, etc.).

### Configuration

Game behavior is controlled by `app/src/main/assets/settings/default.yaml`:
- `learning`: AI adaptation parameters
- `timers`: phase timing in milliseconds
- `players`: player count preferences
- `scoring`: point values and thresholds
- `mechanics`: game rule toggles

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
