# HELLDECK Developer Guide

## Development Environment Setup

### Prerequisites
- Android Studio (latest stable version)
- JDK 11 or higher
- Android SDK (API 26+)
- Git
- Python 3.8+ (for tools/scripts)

### Initial Setup

1. **Clone Repository**
   ```bash
   git clone https://github.com/yourusername/HELLDECK.git
   cd HELLDECK
   ```

2. **Initialize Submodules**
   ```bash
   git submodule update --init --recursive
   ```

3. **Open in Android Studio**
   - Open Android Studio
   - Select "Open Project"
   - Navigate to HELLDECK directory
   - Wait for Gradle sync to complete

4. **Build Project**
   ```bash
   ./gradlew :app:assembleDebug
   ```

## Project Architecture

### Core Components

See [ARCHITECTURE.md](ARCHITECTURE.md) for detailed architecture overview.

**Key Modules:**
- `content/` - Content generation engine
- `engine/` - Game logic and orchestration  
- `ui/` - Jetpack Compose UI components
- `llm/` - Local LLM integration
- `data/` - Database and repositories

### Tech Stack
- **Language:** Kotlin
- **UI:** Jetpack Compose
- **Database:** Room
- **Concurrency:** Coroutines + Flow
- **LLM:** llama.cpp (JNI)
- **Build:** Gradle

## Development Workflow

### Running the App

**Debug Build:**
```bash
./gradlew :app:assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

**Run from Android Studio:**
- Select device/emulator
- Click Run (▶️) or Shift+F10

### Testing

**Unit Tests:**
```bash
./gradlew :app:testDebugUnitTest
```

**Instrumented Tests:**
```bash
./gradlew :app:connectedAndroidTest
```

**Content Quality Tests:**
```bash
./gradlew :app:cardAudit -Pgame=ROAST_CONSENSUS -Pcount=100 -Pseed=1234
./gradlew :app:cardQuality -Pcount=80 -Pseeds=701,702,703
```

### Code Style

Follow [Kotlin Coding Conventions](https://kotlinlang.org/docs/coding-conventions.html)

**Key Points:**
- Use meaningful variable names
- Keep functions focused and small
- Add KDoc for public APIs
- Use data classes for models
- Prefer immutability

## Content Development

### Adding New Games

1. **Register Game in [`Games.kt`](../app/src/main/java/com/helldeck/engine/Games.kt)**
   ```kotlin
   object GameIds {
       const val NEW_GAME = "NEW_GAME"
   }
   ```

2. **Add Game Metadata**
   ```kotlin
   GameSpec(
       id = GameIds.NEW_GAME,
       name = "Game Name",
       interactionType = InteractionType.VOTE_AVATAR,
       timerSec = 10
   )
   ```

3. **Create Templates**
   - Add to `app/src/main/assets/templates_v3/new_game.json`
   - Follow [authoring.md](authoring.md) guidelines

4. **Implement UI Logic**
   - Update `RoundScene.kt` for game-specific interactions
   - Add to `GameIcons.kt` if custom icon needed

### Adding Content

**Templates (V3):**
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
  "constraints": {
    "max_words": 24,
    "distinct_slots": true
  }
}
```

**Lexicons (V2):**
```json
{
  "slot_type": "category_name",
  "entries": [
    {
      "text": "entry",
      "tags": ["tag1"],
      "tone": "playful",
      "spice": 1,
      "locality": 1,
      "pluralizable": false,
      "needs_article": "a"
    }
  ]
}
```

## Common Development Tasks

### Debugging Content Generation

Enable debug logging in [`Config`](../app/src/main/java/com/helldeck/engine/Config.kt):
```kotlin
debug {
    enable_template_selection_logging = true
    enable_quality_inspector_verbose = true
}
```

Check logs:
```bash
adb logcat | grep -E "GameEngine|TemplateEngine|CardQuality"
```

### Testing Content Quality

Use Card Lab in-app:
1. Settings → Developer → Card Lab
2. Enable "Force V3"
3. Select game type
4. Generate multiple cards with different seeds
5. Review quality metrics

Or use Gradle task:
```bash
./gradlew :app:cardQuality -Pcount=100 -Pseeds=701,702
```

### Modifying LLM Behavior

Edit augmentation prompts in [`GameEngine.kt`](../app/src/main/java/com/helldeck/content/engine/GameEngine.kt):
```kotlin
private fun styleGuideFor(gameId: String, tags: List<String>): String {
    // Customize per-game style guidance
}
```

### Database Migrations

When modifying entities:
1. Increment version in [`HelldeckDb.kt`](../app/src/main/java/com/helldeck/content/db/HelldeckDb.kt)
2. Add migration:
   ```kotlin
   val MIGRATION_X_Y = object : Migration(X, Y) {
       override fun migrate(database: SupportSQLiteDatabase) {
           // Migration SQL
       }
   }
   ```

## Build & Release

### Debug Build
```bash
./gradlew :app:assembleDebug
```

### Release Build
```bash
./gradlew :app:assembleRelease
# Sign APK manually or configure signing in build.gradle
```

### Kiosk Setup
```bash
python loader/helldeck_loader.py
```

## Performance Optimization

### Profiling

**Card Generation Performance:**
```bash
./gradlew :app:cardQuality -Pcount=1000
# Review p50, p95, p99 metrics in report
```

**Memory Profiling:**
- Use Android Studio Profiler
- Monitor during extended gameplay sessions
- Check for memory leaks

**Startup Performance:**
```bash
./gradlew :macrobenchmark:connectedBenchmarkAndroidTest
```

### Optimization Tips
- Keep lexicon entries concise (1-4 words)
- Minimize blueprint complexity
- Use indices on frequently queried database columns
- Cache compiled templates
- Lazy-load assets

## Troubleshooting Development Issues

### Gradle Sync Fails
- Check JDK version (11+)
- Invalidate caches: File → Invalidate Caches
- Delete `.gradle` folder and sync again

### Native Library Errors
- Rebuild C++ components: Build → Refresh Linked C++ Projects
- Check NDK version compatibility
- Verify llama.cpp submodule is initialized

### Asset Loading Issues
- Verify assets are in correct directory structure
- Check JSON syntax validity
- Clear app data and reinstall

## Contributing

### Pull Request Process

1. Fork repository
2. Create feature branch: `git checkout -b feature/my-feature`
3. Make changes following code style
4. Add/update tests
5. Run full test suite: `./gradlew test`
6. Commit with descriptive message
7. Push and create PR

### Code Review Checklist
- [ ] Code follows Kotlin conventions
- [ ] Tests added/updated
- [ ] Documentation updated
- [ ] No lint warnings
- [ ] Performance impact considered

## Resources

- [API Reference](API.md)
- [Architecture](ARCHITECTURE.md)
- [Content Authoring](authoring.md)
- [Kotlin Documentation](https://kotlinlang.org/docs/home.html)
- [Jetpack Compose](https://developer.android.com/jetpack/compose)

---
*Happy developing! 🚀*