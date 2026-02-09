# HELLDECK Player Feedback System

## Overview

The feedback system collects implicit and explicit signals about card quality to improve content over time. It operates transparently during gameplay without interrupting the party flow.

## Signal Definitions

| Signal | Type | Reward Impact | Capture Point |
|--------|------|---------------|---------------|
| MVP Card vote | Explicit | +1.0 | End of game (EndGameVotingDialog) |
| Dud Card flag | Explicit | -1.0 | End of game (EndGameVotingDialog) |
| Card Skipped | Implicit | -0.3 | Skip button (FeedbackScene) |
| Round Completed | Implicit | +0.1 | commitFeedbackAndNext() |
| Game Completed | Implicit | +0.05 | End game trigger |
| Quick 🔥 reaction | Bonus | +0.2 | During REVEAL phase (RoundScene) |

## Score Calculation Formula

```kotlin
computedScore = (
    (mvpVotes * 1.0) +
    (quickFires * 0.2) +
    (completions * 0.1) +
    (impressions * 0.05) -
    (skips * 0.3) -
    (dudFlags * 1.0)
) / impressions

// Clamped to 0.0 - 1.0
```

## Database Schema

### card_impressions Table
Records every card shown during gameplay. One row per impression.

| Column | Type | Description |
|--------|------|-------------|
| id | Long | Primary key, auto-generated |
| sessionId | Long | Links to game session |
| cardId | String | Unique card identifier |
| gameId | String | Game type (e.g., "roast_consensus") |
| timestamp | Long | When card was shown |
| wasSkipped | Boolean | Player skipped this card |
| roundCompleted | Boolean | Round finished normally |
| quickFire | Boolean | Player tapped 🔥 |
| wasMvp | Boolean | Voted as MVP |
| wasDud | Boolean | Flagged as dud |

### card_scores Table
Aggregated quality scores per card, computed from impressions.

| Column | Type | Description |
|--------|------|-------------|
| cardId | String | Primary key |
| gameId | String | Game type |
| impressions | Int | Total times shown |
| skips | Int | Times skipped |
| completions | Int | Rounds completed |
| quickFires | Int | 🔥 reactions received |
| mvpVotes | Int | MVP votes |
| dudFlags | Int | Dud flags |
| computedScore | Float | 0.0-1.0 quality score |
| lastUpdated | Long | Last recomputation time |

## Integration Points

### GameNightViewModel
- `feedbackSessionId`: Numeric session identifier for tracking
- `currentImpressionId`: Active impression being tracked
- `sessionCardIds`: Cards shown this session
- `skipCurrentCard()`: Marks skip and advances
- `triggerQuickFire()`: Records 🔥 reaction
- `markCardAsMvp()` / `markCardAsDud()`: Explicit votes
- `recomputeCardScore()`: Updates computed score
- `exportCardQualityReport()`: Generates quality report

### RoundScene
- 🔥 button appears during REVEAL phase

### FeedbackScene
- 🏁 End Game button triggers voting dialog
- Skip button marks card as skipped

### GoldCardsLoader
- `AUTO_EXCLUDE_THRESHOLD = 0.15f`: Cards below excluded
- `LOW_PRIORITY_THRESHOLD = 0.35f`: Cards shown less often
- `cardScores` parameter for weighted selection

## Thresholds

| Threshold | Value | Effect |
|-----------|-------|--------|
| Auto-exclude | 0.15 | Card never shown |
| Low priority | 0.35 | 50% selection penalty |
| High performer | 0.70 | 50% selection boost |

## Quality Report

The `exportCardQualityReport()` function generates:
- Total tracked cards
- Average score across all cards
- Count of high/low performers
- Top 10 cards by score
- Bottom 10 cards by score

## Design Principles

1. **Non-intrusive**: Feedback collection doesn't interrupt party flow
2. **Optional voting**: MVP/Dud selection is optional at game end
3. **Gradual learning**: Scores improve over many sessions
4. **Fail-safe**: Cards start at 0.5 score (neutral)
5. **Reversible**: Low scores reduce priority, not permanent removal

## Files Modified

- `CardFeedbackEntities.kt` - Room entities and DAO
- `HelldeckDb.kt` - Database registration
- `GameNightViewModel.kt` - Tracking logic
- `RoundScene.kt` - 🔥 button
- `FeedbackScene.kt` - Skip tracking, End Game button
- `EndGameVotingDialog.kt` - MVP/Dud voting UI
- `GoldCardsLoader.kt` - Weighted selection
