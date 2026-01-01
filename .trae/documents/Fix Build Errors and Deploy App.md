I will fix the build errors and warnings, then deploy the app to your device.

### 1. Fix Compilation Errors
I will update the following files to resolve "Unresolved reference" and type inference errors:
- **`PerformanceTracker.kt`**: Ensure all `synchronized` blocks use explicit types (`mutableListOf<Long>()`).
- **`Theme.kt`**: Add missing colors `Blue` and `Purple` to `HelldeckColors` to support `GameRulesScene.kt`.
- **`FeedbackScene.kt`**, **`GameFlowComponents.kt`**, **`HomeScene.kt`**, **`RoundScene.kt`**: Add missing imports (`BorderStroke`, `FontWeight`, `TextAlign`, etc.).
- **`RoundScene.kt`**: Update incorrect Game ID references (e.g., `REDFLAG_RALLY` → `GameIds.RED_FLAG`) to match `GameIds` definitions.

### 2. Verify and Polish
- Run `./gradlew assembleDebug` to confirm 0 errors and 0 warnings.
- If any warnings remain (e.g., in C++ code), I will resolve them.

### 3. Deploy
- Uninstall the old application: `adb uninstall com.helldeck`
- Install the new build: `adb install -r app/build/outputs/apk/debug/app-debug.apk`
