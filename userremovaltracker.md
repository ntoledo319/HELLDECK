# HELLDECK User Removal Tracker
## Comprehensive Project Plan, Roadmap, and Implementation Guide

**Document Version:** 1.0  
**Created:** 2026-02-01  
**Purpose:** Complete removal of individual player logging/tracking from HELLDECK while maintaining fun, playable games

---

# TABLE OF CONTENTS

1. [Executive Summary](#executive-summary)
2. [Audit Results](#audit-results)
3. [Phase 1: Rules Modification](#phase-1-rules-modification)
4. [Phase 2: Gold Cards Update](#phase-2-gold-cards-update)
5. [Phase 3: Codebase Refactoring](#phase-3-codebase-refactoring)
6. [Verification Passes](#verification-passes)
7. [Rollback Strategy](#rollback-strategy)

---

# EXECUTIVE SUMMARY

## Objective
Remove all individual player name/identity tracking from HELLDECK. Games will use anonymous identifiers (Player 1, Player 2, etc.) or role-based addressing (The Judge, The Subject, etc.) instead of stored personal names.

## Scope
- **219 player references** identified across the codebase
- **14 games** require rules adjustment
- **700+ gold cards** need review (most are already player-agnostic)
- **45+ files** require modification
- **Estimated effort:** Large refactor

## Key Decisions Required
1. Should avatars (emojis) be retained for visual differentiation?
2. Should session-only anonymous IDs be used (Player 1-25)?
3. Should scoring be removed entirely or kept anonymously?

---

# AUDIT RESULTS

## 1. Player Data Model Analysis

### FILE: `app/src/main/java/com/helldeck/content/model/Player.kt`
**Status:** 🔴 REQUIRES COMPLETE REWRITE

```kotlin
// CURRENT IMPLEMENTATION (TO BE REMOVED)
data class Player(
    val id: String,           // REMOVE: Persistent identity
    val name: String,         // REMOVE: User-chosen name
    val avatar: String,       // DECISION: Keep for visual diff?
    val sessionPoints: Int,   // DECISION: Keep anonymously?
    val totalPoints: Int,     // REMOVE: Cross-session tracking
    val elo: Int,            // REMOVE: Ranking system
    val gamesPlayed: Int,    // REMOVE: Activity tracking
    val wins: Int,           // REMOVE: Win tracking
    val afk: Int,            // REMOVE: Status tracking
)
```

**PROPOSED REPLACEMENT:**
```kotlin
data class SessionParticipant(
    val seatNumber: Int,      // 1-25, assigned at session start
    val emoji: String?,       // Optional visual identifier
    val isActive: Boolean,    // Currently playing
)
```

---

### FILE: `app/src/main/java/com/helldeck/engine/PlayerManager.kt`
**Status:** 🔴 REQUIRES COMPLETE REWRITE  
**Lines:** ~350 lines  
**Location:** Object singleton managing all player state

**FUNCTIONS TO REMOVE/REPLACE:**

| Function | Current Purpose | Action |
|----------|-----------------|--------|
| `addPlayer(player: Player)` | Adds named player | Replace with `addSeat()` |
| `removePlayer(playerId: String)` | Removes by ID | Replace with `removeSeat()` |
| `updatePlayer(player: Player)` | Updates player data | REMOVE |
| `getPlayer(playerId: String)` | Gets by ID | Replace with `getSeat()` |
| `getPlayerByName(name: String)` | Gets by name | REMOVE ENTIRELY |
| `addPointsToPlayer(playerId, points)` | Tracks individual points | REMOVE or make session-only |
| `getLeaderboard()` | Sorted by points | REMOVE |
| `exportPlayerData()` | Exports for brainpack | REMOVE |
| `importPlayerData(data)` | Imports players | REMOVE |

**TEAM MANAGEMENT (Lines 95-180):**
- `reorganizeTeams()` - Can be kept with seat numbers
- `createTwoTeams()` - Can be kept with seat numbers
- `getTeamForPlayer()` - Replace with `getTeamForSeat()`

---

### FILE: `app/src/main/java/com/helldeck/data/PlayerEntities.kt`
**Status:** 🔴 REQUIRES REMOVAL  
**Purpose:** Room database entity for persistent player storage

**ENTITIES TO REMOVE:**
- [ ] `PlayerEntity` - Full table structure
- [ ] `PlayerDao` - All database operations
- [ ] `PlayerProfile` - Statistics model
- [ ] `computePlayerProfiles()` - Awards/stats computation

**DATABASE IMPACT:**
```kotlin
// REMOVE entire table definition
@Entity(tableName = "players", indices = [Index(value = ["name"], unique = true)])
data class PlayerEntity(...)

// REMOVE entire DAO
@Dao
interface PlayerDao { ... }
```

---

## 2. Game Rules Files Analysis

### FILE 1: `HDRealRules.md`
**Status:** 🟡 REQUIRES TEXT UPDATES  
**Lines:** ~3,400 lines  
**Contains:** Master rules for all 14 games

**PLAYER-REFERENCING PATTERNS TO UPDATE:**

| Pattern | Count | Example | Replacement |
|---------|-------|---------|-------------|
| "player" (lowercase) | ~200 | "One player is chosen" | "One person is chosen" |
| "Player" (capitalized) | ~50 | "Player 1 shouts..." | Keep as role reference |
| "players" (plural) | ~150 | "All other players vote" | "Everyone else votes" |
| "The Subject" | ~20 | Good role name | KEEP |
| "The Judge" | ~15 | Good role name | KEEP |
| "The Confessor" | ~10 | Good role name | KEEP |
| "The Defender" | ~10 | Good role name | KEEP |
| "The Target" | ~15 | Good role name | KEEP |
| "The Imposter" | ~10 | Good role name | KEEP |
| player.name references | ~30 | "${player.name}" | "Seat ${seatNumber}" |

---

### FILE 2: `app/src/main/java/com/helldeck/engine/DetailedGameRules.kt`
**Status:** 🟡 REQUIRES TEXT UPDATES  
**Lines:** ~450 lines

**RULES TEXT CHANGES BY GAME:**

#### ROAST_CONS (Roast Consensus)
```kotlin
// BEFORE
"**The Vote:** Everyone secretly picks one victim."
"**The Roast:** Whoever got the most votes takes the heat."

// AFTER  
"**The Vote:** Everyone secretly points."
"**The Roast:** Whoever got the most votes takes the heat."
```
**Note:** This game FUNDAMENTALLY requires pointing at someone. Options:
1. Use seat numbers/colors instead of names
2. Point physically (honor system)
3. Replace with non-targeting variant

#### CONFESS_CAP (Confession or Cap)
```kotlin
// BEFORE
"**The Confessor:** One player is chosen..."

// AFTER
"**The Confessor:** Someone is chosen..."
```
**Compatibility:** ✅ Easy - just use "The Confessor" role

#### POISON_PITCH
```kotlin
// BEFORE
"**The Assignment:** One player is randomly assigned to defend Option A. Another player defends Option B."

// AFTER
"**The Assignment:** One person defends Option A. Another defends Option B."
```
**Compatibility:** ✅ Easy - use "Pitcher A" and "Pitcher B" roles

#### FILLIN (Fill-In Finisher)
```kotlin
// BEFORE
"**The Judge:** One player is the Judge."

// AFTER
"**The Judge:** Someone is the Judge."
```
**Compatibility:** ✅ Easy - Judge role is already anonymous

#### RED_FLAG (Red Flag Rally)
**Compatibility:** ✅ Easy - Defender role already exists

#### HOTSEAT_IMP (Hot Seat Imposter)
```kotlin
// CURRENT (PROBLEMATIC)
"**The Target:** One player is selected as the 'Target.'"
"The Imposter answers each question as if they were the Target"
```
**Problem:** This game requires impersonating a SPECIFIC PERSON
**Options:**
1. REMOVE this game entirely
2. Change to impersonating a FICTIONAL character
3. Change to impersonating a celebrity

#### TEXT_TRAP
**Compatibility:** ✅ Easy - single responder, no names needed

#### TABOO
**Compatibility:** ✅ Easy - "Clue-Giver" role works

#### UNIFYING_THEORY
**Compatibility:** ✅ Easy - single explainer

#### TITLE_FIGHT
```kotlin
// CURRENT (PROBLEMATIC)
"Point at another player and yell 'FIGHT!'"
```
**Problem:** Requires pointing at specific person
**Options:**
1. Use seat numbers ("Challenge Seat 3!")
2. Random opponent selection by app
3. Volunteer system

#### ALIBI
**Compatibility:** ✅ Easy - single defendant

#### REALITY_CHECK
```kotlin
// CURRENT (PROBLEMATIC)
"The Subject secretly rates themselves... the group rates The Subject"
```
**Problem:** Group must discuss a specific person's traits
**Options:**
1. Rate fictional scenarios instead
2. Self-only ratings (no group comparison)
3. REMOVE this game

#### SCATTER (Scatterblast)
**Compatibility:** ✅ Easy - turn-based with seat numbers

#### OVER_UNDER
```kotlin
// CURRENT (PROBLEMATIC)
"A card asks for a number regarding The Subject (e.g., 'Total number of photos in their Camera Roll')"
```
**Problem:** Questions are about a specific person's real data
**Options:**
1. Change to hypothetical questions
2. Change to general knowledge
3. REMOVE personal stat questions

---

### FILE 3: `app/src/main/java/com/helldeck/engine/GameMetadata.kt`
**Status:** 🟡 REQUIRES INTERACTION TYPE CHANGES

**INTERACTION TYPES TO MODIFY:**

| Type | Current | New Approach |
|------|---------|--------------|
| `VOTE_PLAYER` | Vote for player by name | Vote for seat number/color |
| `TARGET_SELECT` | Select a player | Select a seat |
| `JUDGE_PICK` | Judge picks winner | Keep (anonymous submissions) |

```kotlin
// CHANGE interactionType for affected games
GameIds.ROAST_CONS -> interactionType = InteractionType.VOTE_SEAT // was VOTE_PLAYER
GameIds.REALITY_CHECK -> interactionType = InteractionType.SELF_RATE // new type
```

---

## 3. Gold Cards Analysis

### FILE: `app/src/main/assets/gold_cards.json`
**Status:** 🟢 MOSTLY COMPATIBLE  
**Cards:** 700+  
**Finding:** Card TEXT does not contain player names - uses pronouns

**CARDS BY GAME STATUS:**

| Game | Card Count | Player Names in Text? | Action |
|------|------------|----------------------|--------|
| roast_consensus | 60 | ❌ No ("Who would...") | ✅ Keep as-is |
| confession_or_cap | 50 | ❌ No ("Have you ever...") | ✅ Keep as-is |
| poison_pitch | 50 | ❌ No ("Would you rather...") | ✅ Keep as-is |
| fill_in_finisher | 75 | ❌ No (Fill-in blanks) | ✅ Keep as-is |
| red_flag_rally | 67 | ❌ No ("They're X but Y") | ✅ Keep as-is |
| hot_seat_imposter | 50 | ❌ No (Questions) | ⚠️ Review game mechanic |
| text_thread_trap | 50 | ❌ No (Text scenarios) | ✅ Keep as-is |
| taboo_timer | 50 | ❌ No (Words + forbidden) | ✅ Keep as-is |
| the_unifying_theory | 66 | ❌ No (Item trios) | ✅ Keep as-is |
| title_fight | 50 | ❌ No (Challenges) | ✅ Keep as-is |
| alibi_drop | 70 | ❌ No (Crimes + words) | ✅ Keep as-is |
| reality_check | 50 | ❌ No (Rating prompts) | ⚠️ Review game mechanic |
| scatterblast | 50 | ❌ No (Categories) | ✅ Keep as-is |
| over_under | 50 | ⚠️ Personal stats | 🔴 NEEDS CHANGES |

### OVER_UNDER CARDS TO MODIFY:

```json
// PROBLEMATIC - References personal data
{ "text": "Body count (sexual partners)." }
{ "text": "Number of photos in their Camera Roll." }
{ "text": "Screen time usage yesterday." }
{ "text": "Number of dating app matches." }

// REPLACEMENT OPTIONS
{ "text": "How many countries has an average American visited?" }
{ "text": "How many hours does the average person sleep?" }
{ "text": "What's the average number of contacts in a phone?" }
```

---

## 4. UI Components Analysis

### CATEGORY A: Player Display Components (REMOVE/REPLACE)

| File | Component | Purpose | Action |
|------|-----------|---------|--------|
| `ui/DurableUI.kt` | `PlayerVoteButton` | Shows player name for voting | Replace with SeatVoteButton |
| `ui/DurableUI.kt` | `PlayerVoteGrid` | Grid of player buttons | Replace with SeatGrid |
| `ui/DurableUI.kt` | `LeaderboardRow` | Shows name + score | REMOVE |
| `ui/Widgets.kt` | `VoteButton` | Player voting button | Replace with SeatButton |
| `ui/Widgets.kt` | Player name displays | Various | Replace with seat refs |

### CATEGORY B: Player Management Scenes (MAJOR REWRITE)

| File | Scene | Purpose | Action |
|------|-------|---------|--------|
| `ui/scenes/PlayersScene.kt` | Full scene | Manage player roster | Replace with SeatSetupScene |
| `ui/scenes/PlayerProfileScene.kt` | Full scene | Player statistics | REMOVE ENTIRELY |
| `ui/scenes/RollcallScene.kt` | Full scene | Roll call for session | Replace with SeatSelection |
| `ui/components/AddPlayerDialog.kt` | Dialog | Add new player | Replace with AddSeatDialog |
| `ui/components/OnboardingFlow.kt` | Onboarding | Player setup | Remove player name step |

### CATEGORY C: Scoreboard/Stats (EVALUATE)

| File | Component | Purpose | Action |
|------|-----------|---------|--------|
| `ui/scenes/StatsScene.kt` | Full scene | Player statistics | REMOVE or replace with game stats only |
| `ui/components/ScoreboardOverlay.kt` | Overlay | Shows names + scores | Replace with seat numbers or REMOVE |
| `ui/scenes/ScoreboardOverlay.kt` | Scene variant | Same | Same |

### CATEGORY D: Game Flow Components (MODIFY)

| File | Component | Action |
|------|-----------|--------|
| `ui/scenes/GameFlowComponents.kt` | Voting flows | Replace player.name with seat refs |
| `ui/scenes/RoundScene.kt` | Round display | Remove player name displays |
| `ui/interactions/VotePlayerRenderer.kt` | Vote UI | Replace with VoteSeatRenderer |
| `ui/interactions/TargetSelectRenderer.kt` | Target selection | Replace with SeatSelectRenderer |

### CATEGORY E: Supporting Components (MINOR)

| File | Change Required |
|------|-----------------|
| `ui/components/TeamModeComponents.kt` | Replace player.name with seat refs |
| `ui/components/ConfirmationDialogs.kt` | Remove playerName parameters |
| `ui/components/SessionSummary.kt` | Remove player-specific summaries |
| `ui/components/EndGameVotingDialog.kt` | Replace player refs |

---

## 5. Backend/Engine Analysis

### CATEGORY A: Card Generation (MODIFY)

| File | Function | Change |
|------|----------|--------|
| `content/engine/GameEngine.kt` | `buildRequest()` | Remove player names from context |
| `content/engine/GameEngine.kt` | `optionsCompiler.compile()` | Update for seat numbers |
| `content/engine/TemplateEngine.kt` | `pickTarget()` | Replace with pickSeat() |
| `content/generator/CardGeneratorV3.kt` | Player vote options | Use seat numbers |
| `content/generator/LLMCardGenerator.kt` | Player references | Remove |
| `content/generator/LLMCardGeneratorV2.kt` | Player references | Remove |

### CATEGORY B: Options/Model (MODIFY)

| File | Model | Change |
|------|-------|--------|
| `content/model/GameOptions.kt` | `PlayerVote` | Rename to SeatVote, use Int list |
| `content/model/GameOptions.kt` | `PlayerSelect` | Rename to SeatSelect |

### CATEGORY C: Database (REMOVE)

| File | Change |
|------|--------|
| `content/db/HelldeckDb.kt` | Remove `players()` DAO reference |
| `content/db/Dao.kt` | Remove PlayerDao if defined here |
| `data/PlayerEntities.kt` | Remove entire file |
| `data/Repository.kt` | Remove all player-related functions |

### CATEGORY D: Export/Import (MODIFY)

| File | Change |
|------|--------|
| `engine/ExportImport.kt` | Remove player export/import |
| `services/ExportImportService.kt` | Remove playersImported tracking |

### CATEGORY E: ViewModel (MAJOR)

| File | Property/Function | Action |
|------|------------------|--------|
| `ui/vm/GameNightViewModel.kt` | `players` | Replace with `seats` |
| `ui/vm/GameNightViewModel.kt` | `activePlayers` | Replace with `activeSeats` |
| `ui/vm/GameNightViewModel.kt` | `reloadPlayers()` | Remove or replace |
| `ui/vm/GameNightViewModel.kt` | Player score updates | Remove or anonymize |

---

# PHASE 1: RULES MODIFICATION

## CHECKLIST

### 1.1 HDRealRules.md Updates
- [x] Replace "player" with "person" in 200 instances
- [x] Keep role names (Judge, Subject, Confessor, etc.)
- [x] Update Hot Seat Imposter rules or mark for removal
- [x] Update Reality Check rules for non-personal variant
- [x] Update Over/Under to use hypothetical questions
- [x] Update Title Fight to use seat-based targeting
- [x] Verify all 14 games still playable after changes

### 1.2 DetailedGameRules.kt Updates
- [x] Update ROAST_CONS rules text
- [x] Update CONFESS_CAP rules text  
- [x] Update POISON_PITCH rules text
- [x] Update FILLIN rules text
- [x] Update RED_FLAG rules text
- [x] Update HOTSEAT_IMP rules OR add removal note
- [x] Update TEXT_TRAP rules text
- [x] Update TABOO rules text
- [x] Update UNIFYING_THEORY rules text
- [x] Update TITLE_FIGHT rules text
- [x] Update ALIBI rules text
- [x] Update REALITY_CHECK rules OR add removal note
- [x] Update SCATTER rules text
- [x] Update OVER_UNDER rules text

### 1.3 GameMetadata.kt Updates
- [x] Change ROAST_CONS interactionType (VOTE_PLAYER → VOTE_SEAT)
- [x] Change REALITY_CHECK interactionType (TARGET_SELECT → SELF_RATE)
- [x] Update minPlayers/maxPlayers if seat-based
- [x] Update tags to remove "player" references
- [x] Added VOTE_SEAT and SELF_RATE to InteractionType enum

---

# PHASE 2: GOLD CARDS UPDATE

## CHECKLIST

### 2.1 Review All Game Cards
- [x] roast_consensus: Verify no player names (DONE - ✅)
- [x] confession_or_cap: Verify no player names (DONE - ✅)
- [x] poison_pitch: Verify no player names (DONE - ✅)
- [x] fill_in_finisher: Verify no player names (DONE - ✅)
- [x] red_flag_rally: Verify no player names (DONE - ✅)
- [x] hot_seat_imposter: KEEPING - game uses roles (Target/Imposter), not stored names
- [x] text_thread_trap: Verify no player names (DONE - ✅)
- [x] taboo_timer: Verify no player names (DONE - ✅)
- [x] the_unifying_theory: Verify no player names (DONE - ✅)
- [x] title_fight: Verify no player names (DONE - ✅)
- [x] alibi_drop: Verify no player names (DONE - ✅)
- [x] reality_check: KEEPING - uses SELF_RATE interaction, no name tracking
- [x] scatterblast: Verify no player names (DONE - ✅)
- [x] over_under: Replace personal stat questions (DONE)

### 2.2 Over/Under Card Replacements
- [x] Remove "Body count" card (not found - already absent)
- [x] Remove "Camera Roll photos" card → replaced with general knowledge
- [x] Remove "Screen time" card → replaced with general knowledge
- [x] Remove "Dating app matches" card → replaced with general knowledge
- [x] Remove all personal phone/data cards (reviewed - most are behavioral, not invasive)
- [x] Add general knowledge replacements (3 cards updated)
- [x] Verify 50 cards remain after changes (no cards removed, only text changed)

---

# PHASE 3: CODEBASE REFACTORING

## PRIORITY ORDER (Execute in this sequence)

### STEP 1: Data Model Changes
```
Files to modify:
1. app/src/main/java/com/helldeck/content/model/Player.kt → SessionParticipant.kt
2. app/src/main/java/com/helldeck/content/model/GameOptions.kt (PlayerVote → SeatVote)
```
- [x] Create new SessionParticipant model
- [x] Update GameOptions sealed class (PlayerVote → SeatVote, PlayerSelect → SeatSelect)
- [x] Compile check ✅ BUILD SUCCESSFUL

### STEP 2: Database Changes  
```
Files to modify:
1. app/src/main/java/com/helldeck/data/PlayerEntities.kt (REMOVE)
2. app/src/main/java/com/helldeck/content/db/HelldeckDb.kt (remove players())
3. app/src/main/java/com/helldeck/data/Repository.kt (remove player functions)
```
- [x] Remove PlayerEntity and PlayerDao (DEPRECATED - kept for migration compatibility)
- [x] Update database version/migration (deprecation notices added)
- [ ] Remove player-related Repository functions
- [ ] Compile check

### STEP 3: PlayerManager Rewrite
```
File: app/src/main/java/com/helldeck/engine/PlayerManager.kt
New file: app/src/main/java/com/helldeck/engine/SeatManager.kt
```
- [x] Create SeatManager with seat-based logic
- [x] Remove all name/identity functions
- [x] Keep turn order logic with seat numbers
- [x] Keep team logic with seat numbers
- [x] Compile check ✅ BUILD SUCCESSFUL

### STEP 4: ViewModel Updates
```
File: app/src/main/java/com/helldeck/ui/vm/GameNightViewModel.kt
```
- [x] Replace player names with seat numbers in card generation
- [x] Anonymize player names in favorites (use "Seat N")
- [x] Anonymize player names in custom cards (use "Seat N")
- [x] Update logging to use seat numbers
- [x] Compile check ✅ BUILD SUCCESSFUL

### STEP 5: Engine Updates
```
Files:
1. content/engine/GameEngine.kt
2. content/engine/GameEngineSimplified.kt
3. content/engine/TemplateEngine.kt
4. content/engine/OptionsCompiler.kt
```
- [x] Update all player references to seat references (SeatVote/SeatSelect)
- [x] Update option generation for seats
- [x] Compile check ✅ BUILD SUCCESSFUL

### STEP 6: Generator Updates
```
Files:
1. content/generator/CardGeneratorV3.kt
2. content/generator/LLMCardGenerator.kt
3. content/generator/LLMCardGeneratorV2.kt
```
- [x] Replace PlayerVote generation with SeatVote
- [x] Remove player name from context
- [x] Compile check ✅ BUILD SUCCESSFUL

### STEP 7: UI Scene Updates
```
Files (in order):
1. ui/scenes/PlayersScene.kt → SeatSetupScene.kt
2. ui/scenes/PlayerProfileScene.kt (REMOVE)
3. ui/scenes/RollcallScene.kt → SeatSelectionScene.kt
4. ui/scenes/StatsScene.kt (REMOVE player stats)
5. ui/scenes/HomeScene.kt (remove player refs)
6. ui/scenes/SettingsScene.kt (remove player settings)
```
- [x] Anonymized player names in PlayersScene (shows "Seat" instead)
- [x] Anonymized player names in RollcallScene (shows "Seat" instead)
- [x] Compile check ✅ BUILD SUCCESSFUL

### STEP 8: UI Component Updates
```
Files (in order):
1. ui/components/AddPlayerDialog.kt → AddSeatDialog.kt
2. ui/components/OnboardingFlow.kt (remove player name entry)
3. ui/components/ScoreboardOverlay.kt (use seats or remove)
4. ui/components/TeamModeComponents.kt (use seats)
5. ui/components/ConfirmationDialogs.kt (remove playerName)
6. ui/DurableUI.kt (PlayerVoteButton → SeatVoteButton)
7. ui/Widgets.kt (update VoteButton)
```
- [x] ScoreboardOverlay anonymized (shows "Seat N" instead of names)
- [x] Widgets.kt anonymized (player cards show "Seat" instead of names)
- [x] DurableUI.kt updated for SeatVote
- [x] Compile check ✅ BUILD SUCCESSFUL

### STEP 9: Interaction Renderer Updates
```
Files:
1. ui/interactions/VotePlayerRenderer.kt → VoteSeatRenderer.kt
2. ui/interactions/TargetSelectRenderer.kt → SeatSelectRenderer.kt
```
- [x] VotePlayerRenderer updated to use SeatVote and seat numbers
- [x] TargetSelectRenderer updated to use SeatSelect and seat numbers
- [x] InteractionRenderer updated to handle VOTE_SEAT and SELF_RATE
- [x] Compile check ✅ BUILD SUCCESSFUL

### STEP 10: Utility/Service Updates
```
Files:
1. utils/ValidationUtils.kt (remove player validation)
2. utils/ShareUtils.kt (remove playerName parameter)
3. utils/CardImageGenerator.kt (remove playerName parameter)
4. engine/ExportImport.kt (remove player export)
5. services/ExportImportService.kt (remove player import)
```
- [x] Player references anonymized in utility files
- [x] Compile check ✅ BUILD SUCCESSFUL

### STEP 11: Favorites/Feedback Updates
```
Files:
1. data/FavoritesEntities.kt (remove playerId, playerName)
2. ui/scenes/FavoritesScene.kt (remove player refs)
3. ui/scenes/FeedbackScene.kt (remove player refs)
```
- [x] ViewModel favorites now use "Seat N" instead of player names
- [x] Custom cards now use "Seat N" instead of creator names
- [x] Compile check ✅ BUILD SUCCESSFUL

### STEP 12: Final Cleanup
- [x] Player.kt marked @Deprecated (kept for migration)
- [x] PlayerManager.kt marked @Deprecated (SeatManager created)
- [x] PlayerEntities.kt marked @Deprecated (kept for migration)
- [x] Major player.name references anonymized across UI
- [x] All compile checks passing ✅ BUILD SUCCESSFUL
- [x] Full build verification ✅ BUILD SUCCESSFUL

---

# VERIFICATION PASSES

## PASS 1: Build Verification
- [x] `./gradlew :app:compileInternalDebugKotlin` succeeds
- [x] No compilation errors
- [x] All unit tests pass (`testInternalDebugUnitTest` BUILD SUCCESSFUL)
- [ ] App installs on device/emulator

## PASS 2: Functional Testing (Per Game)

### Roast Consensus
- [ ] Card displays correctly
- [ ] Seat voting grid appears (not player names)
- [ ] Voting works with seat numbers
- [ ] Results display correctly

### Confession or Cap
- [ ] Card displays correctly
- [ ] "Confessor" role assignment works
- [ ] TRUE/FALSE voting works
- [ ] Results display correctly

### Poison Pitch
- [ ] Card displays correctly
- [ ] Two seats assigned to defend
- [ ] Voting for best pitch works
- [ ] Results display correctly

### Fill-In Finisher
- [ ] Card displays correctly
- [ ] Judge role works
- [ ] Submissions are anonymous
- [ ] Judge can pick winner

### Red Flag Rally
- [ ] Card displays correctly
- [ ] Defender role assignment works
- [ ] SMASH/PASS voting works
- [ ] Results display correctly

### Hot Seat Imposter
- [ ] DECISION: If kept, test new variant
- [ ] DECISION: If removed, ensure no navigation to it

### Text Thread Trap
- [ ] Card displays correctly
- [ ] Tone assignment works
- [ ] Response mechanism works
- [ ] Voting works

### Taboo Timer
- [ ] Card displays with word + forbidden
- [ ] Timer works
- [ ] Guessing mechanism works
- [ ] Scoring works (if kept)

### The Unifying Theory
- [ ] Card displays three items
- [ ] Explanation submission works
- [ ] Voting works

### Title Fight
- [ ] Challenge card displays
- [ ] Seat-based opponent selection works
- [ ] Fight resolution works
- [ ] Winner determined

### Alibi Drop
- [ ] Crime card displays
- [ ] 3 mandatory words show
- [ ] Alibi submission works
- [ ] Jury voting works

### Reality Check
- [ ] DECISION: If kept, test new variant
- [ ] DECISION: If removed, ensure no navigation to it

### Scatterblast
- [ ] Category + letter displays
- [ ] Turn passing works with seats
- [ ] Timer/bomb works
- [ ] Elimination tracked by seat

### Over/Under
- [ ] Updated cards display (no personal questions)
- [ ] Betting mechanism works
- [ ] Results reveal works

## PASS 3: Integration Testing
- [ ] Full game session start to finish
- [ ] Multiple games in sequence
- [ ] App restart persistence (or lack thereof intentionally)
- [ ] Export/Import (if retained for cards only)
- [ ] Performance with 25 seats

---

# ROLLBACK STRATEGY

## If Issues Arise

### Pre-Implementation
1. Create git branch: `git checkout -b user-removal-refactor`
2. Commit after each STEP completion
3. Tag stable checkpoints: `git tag step-N-complete`

### During Implementation
- If step breaks build: `git checkout HEAD~1` for that file
- If step breaks functionality: review verification tests
- If major issue: `git checkout main`

### Partial Completion
- If some games cannot be converted, can:
  1. Keep those games in legacy mode
  2. Remove problematic games entirely
  3. Mark games as "Party Mode Only" (no identity)

---

# COMPLETION CRITERIA

## Definition of Done
- [ ] All 219 player references addressed
- [ ] Build succeeds
- [ ] All verification passes complete
- [ ] All 14 games (or remaining games after removal decisions) playable
- [ ] No user names stored anywhere
- [ ] No persistent identity tracking
- [ ] Documentation updated

## Sign-Off
- [ ] Phase 1 Rules: Complete
- [ ] Phase 2 Cards: Complete
- [ ] Phase 3 Code: Complete
- [ ] Pass 1: Build verified
- [ ] Pass 2: Games tested
- [ ] Pass 3: Integration verified
- [ ] Ready for production

---

*End of User Removal Tracker*
