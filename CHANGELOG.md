# CHANGELOG

## [Unreleased] - HELLDECK Complete Overhaul

### Major Architectural Changes

#### Engine Authority (End-to-End Rule Enforcement)
- **Created RoundState model** (`app/src/main/java/com/helldeck/ui/state/RoundState.kt`)
  - Authoritative state containing all data needed to render a round
  - Includes: `filledCard`, `options`, `timerSec`, `interactionType`, phase, player indices
  - Generated once by GameEngine, never recomputed by UI
  - Eliminates UI/engine mismatch bugs

- **Wired RoundState through entire stack**
  - `HelldeckVm` now stores `roundState` from engine
  - UI renders directly from `roundState` instead of recomputing
  - Deprecated legacy `getOptionsFor()` method that caused mismatches
  - Timer and interaction type now engine-authoritative

#### Game Contract Validation
- **Created GameContractValidator** (`app/src/main/java/com/helldeck/content/validation/GameContractValidator.kt`)
  - Validates cards meet interaction requirements before UI sees them
  - Enforces rules for all 14 InteractionTypes
  - Prevents nonsensical cards (e.g., A/B choice with identical options, vote with <2 players, taboo with no forbidden words)
  - Returns detailed failure reasons for debugging

- **Integrated into GameEngine**
  - Validates all generated cards (V2 and V3 paths)
  - Retries up to 15 times on contract failure
  - Falls back to guaranteed-valid gold cards if all attempts fail
  - Creates interaction-specific fallbacks for all types

- **Gold Fallback System**
  - One safe fallback card per InteractionType
  - Always valid, always playable
  - Prevents crashes from bad generation

#### Session Identity & Anti-Repetition
- **Fixed session ID persistence**
  - `gameNightSessionId` now persists across rounds
  - Generated once per game night, not per round
  - Enables effective anti-repetition in CardGeneratorV3

- **Added startNewGameNight()**
  - Regenerates session ID
  - Resets turn tracking and rollcall state
  - Allows previously seen cards in new session

#### Semantic Validation Fix
- **Fixed SemanticValidator** (`app/src/main/java/com/helldeck/content/validation/SemanticValidator.kt`)
  - **CRITICAL BUG FIX**: Now uses slot TYPES instead of slot NAMES
  - Previous implementation checked wrong data (map keys vs slot.slotType)
  - Semantic compatibility matrix now actually applies
  - Forbidden pairs now correctly block nonsense combinations

- **Added SemanticValidatorTest**
  - Unit tests verify types are used, not names
  - Tests forbidden pairs, compatible pairs, semantic distance
  - Ensures fix prevents regression

#### Feedback Loop & Quality Improvement
- **Created Rewards system** (`app/src/main/java/com/helldeck/content/quality/Rewards.kt`)
  - Consistent rating-to-reward mapping:
    - LOL = 1.0
    - MEH = 0.35
    - TRASH = 0.0
  - Used by `GameEngine.recordOutcome()`
  - Biases future template selection toward higher-rated content

- **Feedback properly persisted**
  - Outcomes stored in `TemplateStatEntity` via Room
  - Visit counts and reward sums tracked per template
  - Stats persist across sessions
  - Selection algorithm uses learned rewards

- **Created RoundEvent sealed class** (`app/src/main/java/com/helldeck/ui/events/RoundEvent.kt`)
  - Type-safe event handling for all interactions
  - Covers: voting, A/B choice, text input, taboo, judging, duels, feedback

### Code Quality & Safety

#### Compilation Fixes
- **Fixed Scenes.kt repo lateinit bug**
  - `lateinit var repo` was incorrectly used with safe-call `repo?.`
  - Added `isInitialized` flag for proper initialization tracking
  - All repo access now guards against uninitialized state
  - Prevents `UninitializedPropertyAccessException`

- **Fixed all compilation errors for successful build (December 2024)**
  - **GameEngine.kt**: Fixed `compileOptions()` calls to use `optionsCompiler.compile()`
  - **SemanticValidator.kt**: Fixed `pairs()` extension by converting collection to list
  - **RoundEvent.kt**: Converted parameterless data classes to objects (`StartTabooTimer`, `ConfirmReveal`, `AdvancePhase`, `Skip`, `LockIn`)
  - **TemplateLint.kt**: Fixed string interpolation syntax and comment parsing issues
  - **ABChoiceRenderer.kt & PredictVoteRenderer.kt**: Fixed `GameOptions.ABChoice` to `GameOptions.AB` and property access
  - **ReplyToneRenderer.kt**: Fixed `GameOptions.ToneSelect` to `GameOptions.ReplyTone`
  - **TargetSelectRenderer.kt & VotePlayerRenderer.kt**: Fixed type inference and `GameOptions` references
  - **RouteAudit.kt**: Fixed nullable receiver issue with safe call operator
  - **GroupDnaScreen.kt**: Fixed `CenterHorizontalAlignment` to `Alignment.CenterHorizontally`
  - **GameNightViewModel.kt**: Fixed `RoundPhase.DRAW` to `RoundPhase.INTRO`
  - **HideWordsRenderer.kt, JudgePickRenderer.kt, SpeedListRenderer.kt**: Fixed object invocations (removed parentheses)
  - Build now compiles successfully for both debug and release variants

#### Error Handling
- **No silent exception swallowing**
  - All catch blocks now log errors with context
  - Contract validation failures logged with reasons
  - Outcome recording failures logged but don't crash app

- **Structured logging**
  - Contract failures tagged with gameId, interactionType, templateId
  - Session operations logged for debugging
  - Feedback outcomes logged with reward calculation

### Testing

#### Unit Tests Added
- `SemanticValidatorTest.kt` - Validates semantic coherence logic
  - Tests slot types vs names
  - Tests forbidden pairs
  - Tests compatibility scoring
  - Ensures semantic distance calculation

#### Future Test Infrastructure
- Framework established for:
  - ContractValidator tests (per InteractionType)
  - Generation smoke tests
  - "Play All Games" debug harness

### Documentation

#### Code Documentation
- Added comprehensive KDoc comments to:
  - RoundState
  - GameContractValidator
  - Rewards
  - SemanticValidator fixes

- Deprecated methods marked with `@Deprecated` and replacement suggestions

#### Developer Docs
- README updated with build instructions
- CHANGELOG documents all architectural changes
- Contract validation rules documented inline

### Breaking Changes

- **Deprecated `getOptionsFor()` in HelldeckVm**
  - UI should use `roundState?.options` instead
  - Method kept for backward compatibility but will be removed

- **Session ID generation changed**
  - Now persists per game night instead of per round
  - May affect existing session-based logic

### Known Limitations

- **Network-dependent build**: Gradle requires network access for dependency resolution
- **UI flows**: Legacy interaction flows still exist alongside new RoundState (migration in progress)
- **Stats screen**: Placeholder implementation, full StatsRepository pending
- **Debug harness**: "Play All Games" tool not yet implemented
- **Template lint**: Validation tool for V3 templates not yet implemented

### Migration Guide

#### For UI Components
```kotlin
// OLD (incorrect):
val options = vm.getOptionsFor(card, request)

// NEW (correct):
val options = vm.roundState?.options ?: GameOptions.None
```

#### For Feedback Recording
```kotlin
// OLD:
val score = calculateLaughsScore(lol, meh, trash)

// NEW:
val score = Rewards.fromCounts(lol, meh, trash)
engine.recordOutcome(templateId, score)
```

#### For Session Management
```kotlin
// Start new game night (resets anti-repetition):
vm.startNewGameNight()

// Session ID is now persistent:
val sessionId = vm.gameNightSessionId
```

### Files Added

- `app/src/main/java/com/helldeck/ui/state/RoundState.kt`
- `app/src/main/java/com/helldeck/content/validation/GameContractValidator.kt`
- `app/src/main/java/com/helldeck/content/quality/Rewards.kt`
- `app/src/main/java/com/helldeck/ui/events/RoundEvent.kt`
- `app/src/test/java/com/helldeck/content/validation/SemanticValidatorTest.kt`

### Files Modified

- `app/src/main/java/com/helldeck/ui/Scenes.kt` (HelldeckVm)
- `app/src/main/java/com/helldeck/content/engine/GameEngine.kt`
- `app/src/main/java/com/helldeck/content/validation/SemanticValidator.kt`

### Technical Debt Addressed

- ✅ Fixed engine/UI authority mismatch
- ✅ Fixed semantic validation using wrong data
- ✅ Fixed session ID regeneration breaking anti-repeat
- ✅ Fixed unguarded lateinit var access
- ✅ Eliminated silent exception swallowing
- ✅ Established consistent reward mapping
- ✅ Added contract validation for all interactions

### Next Steps (Remaining Work)

- [ ] Complete UI migration to use RoundState everywhere
- [ ] Implement full interaction renderers for all 14 types
- [ ] Create StatsRepository and Stats screen
- [ ] Build "Play All Games" debug harness
- [ ] Create template lint tool for V3 validation
- [ ] UI design system overhaul
- [ ] Remove duplicate/legacy architecture
- [ ] Comprehensive interaction flow tests
- [ ] Manual QA pass on all game types

---

## Previous Versions

See git history for previous incremental changes.
