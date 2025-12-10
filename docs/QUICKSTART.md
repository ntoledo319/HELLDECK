# HELLDECK Quick Start Guide

## 🚀 Installation

### Prerequisites
- Android device running Android 8.0 (Oreo) or later
- Minimum 2GB RAM recommended
- 500MB free storage space

### Installation Steps

1. **Download the APK**
   - Get the latest version from our [releases page](#)
   - Or build from source using: `./gradlew :app:assembleRelease`

2. **Install the App**
   - Enable "Unknown Sources" in Android settings
   - Tap on the downloaded APK file to install
   - Grant necessary permissions when prompted

3. **First Launch**
   - App will initialize content engine (may take 30-60 seconds)
   - Accept terms and conditions
   - Complete brief onboarding tutorial

## ⚡ Quick Setup

### For Players
1. Open HELLDECK app
2. Tap "Quick Play" on home screen
3. Select number of players (3-8 recommended)
4. Choose game mode or let app select
5. Start playing!

### For Kiosk Mode
1. Enable Developer Options on Android device
2. Connect device to computer via USB
3. Run: `python loader/helldeck_loader.py`
4. Follow on-screen instructions to configure kiosk mode
5. Device will reboot into locked gameplay mode

## 🎮 Basic Gameplay

### Starting a Game
1. From home screen, tap "New Game"
2. Select players (or use "Quick Rollcall")
3. Choose game type or use "Random Selection"
4. Adjust settings (spice level, time limits)
5. Tap "Start Game"

### Game Controls
- **Main Interaction**: Tap large zone buttons (Left/Center/Right)
- **Voting**: Tap player avatars or option buttons
- **Feedback**: Use emoji reactions after each round
- **Navigation**: Swipe between scenes or use back button

### Game Modes
- **Quick Play**: Random game selection, 5-minute rounds
- **Tournament**: Structured competition across multiple games
- **Custom**: Manual game and rule selection
- **Kiosk**: Continuous gameplay for public devices

## 🛠️ Basic Configuration

### Adjusting Settings
1. Tap "Settings" from home screen
2. Modify preferences:
   - Spice level (0-3)
   - Game duration
   - Player limits
   - Haptic feedback intensity
3. Save changes

### Player Management
1. Tap "Players" from home screen
2. Add new players with names and emoji
3. Edit or remove existing players
4. Mark players as present/absent for current session

## 🆘 Troubleshooting Quick Fixes

### App Won't Launch
- Clear app cache and restart
- Reinstall the latest version
- Check device compatibility

### Games Not Loading
- Restart the app
- Check storage permissions
- Verify asset files are intact

### Performance Issues
- Close background apps
- Reduce spice level for simpler content
- Restart device if needed

## 📚 Next Steps

- **Learn Advanced Features**: Check out the [User Guide](USERGUIDE.md)
- **Customize Content**: See [Content Authoring Guide](authoring.md)
- **Developer Setup**: Review [Developer Guide](DEVELOPER.md)
- **Troubleshooting**: Visit [Troubleshooting Guide](TROUBLESHOOTING.md)

---
*Need more help? Check the full [User Guide](USERGUIDE.md) or contact support.*