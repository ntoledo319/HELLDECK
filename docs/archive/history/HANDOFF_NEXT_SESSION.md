# HELLDECK Build Fix - Session Handoff

**Date:** 2025-10-25  
**Status:** Build failing with ~50 errors remaining  
**Next Action:** Continue complete fix approach

---

## ✅ PROGRESS SO FAR (~60+ errors fixed)

### Build System - COMPLETE
- ✅ KAPT → KSP migration
- ✅ Kotlin 1.9.24 → 1.9.25
- ✅ Compose Compiler v1.5.15
- ✅ kotlinx-serialization-json added
- ✅ ARM64-v8a architecture fixed

### Code Fixes - COMPLETE  
- ✅ Logger.e() signatures (AnalyticsManager ~25 fixes)
- ✅ Compose imports (GameViewModel, NavigationViewModel, PlayerViewModel)
- ✅ InteractionType enum created
- ✅ SeededRng.random accessor
- ✅ GameEngine collection references
- ✅ ScoreboardOverlay animation imports

### Cleanup - COMPLETE
- ✅ Removed: CodeStyleGuide, CodeCleanup, MemoryProfiler
- ✅ Removed: PerformanceOptimizer, Preflight, Accessibility

---

## 🔴 NEXT PRIORITY FIXES (Ordered by Impact)

### BATCH 1: Fix Error Handling Conflicts (~15 errors)
**Files:** ErrorBoundary.kt, ErrorHandling.kt

**Issues:**
- Duplicate HelldeckError definitions
- Conflicting SafeComposable functions  
- Try-catch around composables not supported

**Fix Strategy:**
1. Remove ErrorHandling.kt duplicate (keep ErrorBoundary.kt)
2. OR rename one to avoid conflicts
3. Remove try-catch from composable contexts

**Estimated Impact:** ~15 errors fixed

---

### BATCH 2: Create Missing UI Stubs (~10 errors)
**Files:** HelldeckApp.kt, Scenes.kt, ErrorBoundary.kt

**Missing Components:**
- HelldeckLoadingSpinner
- GiantButton
- HelldeckBackgroundPattern
- HelldeckAnimations
- HelldeckSpacing
- HelldeckColors (theme object)

**Fix Strategy:**
Create stub file: `app/src/main/java/com/helldeck/ui/ThemeComponents.kt`
```kotlin
package com.helldeck.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

object HelldeckColors {
    val Yellow = Color(0xFFFFD700)
    val Orange = Color(0xFFFF8C00)
    val White = Color.White
    val DarkGray = Color(0xFF2B2B2B)
    val MediumGray = Color(0xFF4A4A4A)
    val LightGray = Color(0xFF9E9E9E)
}

object HelldeckSpacing {
    // Add spacing constants
}

object HelldeckAnimations {
    // Add animation specs
}

@Composable
fun HelldeckLoadingSpinner(modifier: Modifier = Modifier) {
    // Stub implementation
}

@Composable
fun HelldeckBackgroundPattern(modifier: Modifier = Modifier) {
    // Stub implementation
}

@Composable
fun GiantButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Stub implementation
}
```

**Estimated Impact:** ~10 errors fixed

---

### BATCH 3: Fix Scenes.kt Issues (~15 errors)
**File:** app/src/main/java/com/helldeck/ui/Scenes.kt

**Issues:**
- Missing GameRulesScene reference
- Missing ContentEngineProvider
- Suspend function calls outside coroutine
- Overload resolution ambiguities

**Fix Strategy:**
1. Wrap `initOnce()` in LaunchedEffect coroutine
2. Create GameRulesScene stub or remove reference
3. Create ContentEngineProvider stub
4. Fix composable lambda issues (lines 215-225)

**Estimated Impact:** ~15 errors fixed

---

### BATCH 4: Fix Remaining Scene Files (~10 errors)
**Files:** RollcallScene.kt, RoundScene.kt, RulesSheet.kt, StatsScene.kt

**Known Issues:**
- Missing HelldeckColors references
- Missing theme imports
- Missing DAO methods (deleteById, deleteAll)
- Missing GameMetadata.getGameStats()
- Access modifier issues (private engine)

**Fix Strategy:**
1. Import HelldeckColors from ThemeComponents
2. Add missing methods to PlayerDao
3. Add getGameStats() to GameMetadata (already has stub)
4. Make HelldeckVm.engine public or add accessors

**Estimated Impact:** ~10 errors fixed

---

## 📋 DETAILED FIX COMMANDS

### Fix 1: Remove ErrorHandling.kt duplicate
```bash
rm app/src/main/java/com/helldeck/ui/ErrorHandling.kt
```

### Fix 2: Create ThemeComponents.kt
```bash
# Create file with stub implementations (see BATCH 2 above)
```

### Fix 3: Fix HelldeckApp.kt imports
```kotlin
// Remove: import com.helldeck.content.validation.Preflight
// Remove: Preflight.run() call
```

### Fix 4: Add missing GameMetadata method
```kotlin
// Already exists:
fun getGameStats(): Map<String, Any> = mapOf("totalGames" to getAllGames().size)
```

### Fix 5: Add missing DAO methods
```kotlin
// In PlayerDao.kt:
@Query("DELETE FROM players WHERE id = :id")
suspend fun deleteById(id: String)

@Query("DELETE FROM players")
suspend fun deleteAll()
```

---

## 🎯 SUCCESS CRITERIA

- [ ] Zero compilation errors
- [ ] `./gradlew assembleDebug` succeeds
- [ ] APK generated at `app/build/outputs/apk/debug/app-debug.apk`
- [ ] Device connected via ADB
- [ ] APK installed on device: `adb install app/build/outputs/apk/debug/app-debug.apk`
- [ ] App launches without crashing

---

## 📁 REFERENCE FILES

- **Tracker:** BUILD_FIX_TRACKER.md (comprehensive error categorization)
- **This File:** HANDOFF_NEXT_SESSION.md (immediate next steps)

---

## 🚀 RESUME COMMAND

```bash
# Start next session with:
cd /Users/nicholastoledo/CascadeProjects/HELLDECK
./gradlew assembleDebug --no-daemon 2>&1 | grep -E "^e: |BUILD" | head -n 80

# Then apply fixes in BATCH order above
```

**Estimated Completion Time:** 1-2 hours for remaining ~50 errors