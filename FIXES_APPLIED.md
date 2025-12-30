# Game Rules Fixes Applied

## Summary
All critical timer mismatches and incomplete game metadata have been fixed to match the official rules in `HDRealRules.md`.

## Timer Updates ✅

### Fixed Timers:
1. **Roast Consensus**: 8s → **20s** ✅
2. **Poison Pitch**: 6s → **30s** ✅
3. **Red Flag Rally**: 6s → **45s** ✅
4. **Taboo Timer**: 8s → **60s** ✅
5. **Alibi Drop**: 3s → **30s** ✅

### Timers Already Correct:
- **Fill-In Finisher**: 60s ✅ (already correct)

### Note on Scatterblast:
- Current: 10s visible timer
- Rule: Invisible random timer (20-60s)
- **Status**: Requires timer system changes (not just metadata). Left at 10s for now.

## Game Metadata Fixes ✅

### Completed Incomplete Entries:

1. **The Unifying Theory** ✅
   - Added complete metadata
   - Description: "Explain why three unrelated items are the same. Spice 4+ requires inappropriate connections."
   - Timer: 8s (no specific rule)
   - Category: CREATIVE
   - Interaction: ODD_REASON → ODD_EXPLAIN

2. **Reality Check** ✅
   - Added complete metadata
   - Description: "Subject rates themselves 1-10; group rates subject 1-10; reveal both simultaneously."
   - Timer: 6s (no specific rule)
   - Category: VOTING
   - Interaction: TARGET_PICK → TARGET_SELECT
   - Note: Requires custom 1-10 rating UI (not yet implemented)

3. **Over/Under** ✅
   - Fixed incorrect description (was "From three options, choose the misfit")
   - New description: "Group sets betting line; everyone bets OVER or UNDER on subject's number; reveal truth."
   - Fixed interaction type: Changed from ODD_REASON to AB_VOTE → PREDICT_VOTE
   - Category: VOTING
   - Note: Uses PREDICT_VOTE interaction (same as Majority Report), which should work for betting

## UI Updates ✅

### Rules Descriptions Added:
- Added `gameHowTo` descriptions for:
  - The Unifying Theory
  - Reality Check
  - Over/Under

### Icons Added:
- The Unifying Theory: 📐
- Reality Check: 🪞
- Over/Under: 📉

## Files Modified:

1. `app/src/main/java/com/helldeck/engine/GameMetadata.kt`
   - Updated 5 timer values
   - Completed 3 incomplete game entries
   - Fixed Over/Under description and interaction type

2. `app/src/main/java/com/helldeck/ui/scenes/RulesSheet.kt`
   - Added game descriptions for new games
   - Added icons for new games

3. `app/src/main/java/com/helldeck/ui/scenes/GameRulesScene.kt`
   - Added game descriptions for new games

4. `app/src/main/java/com/helldeck/ui/GameIcons.kt`
   - Added icons for new games

## Remaining Issues (Require Additional Implementation):

### 1. Scatterblast Timer
- **Issue**: Should have invisible random timer (20-60s)
- **Current**: 10s visible timer
- **Action**: Requires timer system modification to support invisible random timers

### 2. Reality Check Rating UI
- **Issue**: Needs custom 1-10 rating interface
- **Current**: Uses TARGET_SELECT (player selection)
- **Action**: Create custom rating renderer with 1-10 scale

### 3. Over/Under Betting UI
- **Issue**: Needs line-setting interface before betting
- **Current**: Uses PREDICT_VOTE (should work, but may need enhancement)
- **Action**: Verify PREDICT_VOTE works for OVER/UNDER, or create custom renderer

### 4. Text Thread Trap Tones
- **Issue**: Rules specify 22 mandatory tones, but implementation uses lexicon
- **Current**: Lexicon-based tone selection (provides variety)
- **Action**: Verify if lexicon approach is acceptable or if specific 22 tones are required

### 5. Game Mechanics Verification Needed:
- Simultaneous reveal in Roast Consensus
- Side assignment (not choice) in Poison Pitch
- Buzzer role and penalties in Taboo Timer
- Challenge rule in Scatterblast
- Spice level enforcement in Unifying Theory

## Testing Recommendations:

1. Test all games with updated timers
2. Verify game flows match rules
3. Test Reality Check and Over/Under with current interaction types
4. Verify scoring logic matches rules (especially Confession or Cap, Red Flag Rally)

## Next Steps:

1. Implement custom rating UI for Reality Check
2. Enhance Over/Under with line-setting if needed
3. Consider implementing invisible random timer for Scatterblast
4. Verify and test all game mechanics against rules
5. Update scoring logic to match rules exactly


