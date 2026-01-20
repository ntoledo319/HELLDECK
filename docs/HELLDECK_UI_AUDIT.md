# HELLDECK UI/UX Audit Report

**Date:** January 20, 2026  
**Auditor:** Cascade AI  
**Version:** 1.0

---

## Executive Summary

This audit evaluates HELLDECK's UI components against the "Hell's Living Room" design system. The core theme and widget infrastructure is **solid**, but the **game interaction renderers are critically deficient** and fail the Drunk Person Test™.

### Overall Scores

| Component | Score | Status |
|-----------|-------|--------|
| Theme.kt | 9/10 | ✅ PASS |
| Widgets.kt | 8/10 | ✅ PASS |
| Interaction Renderers | 3/10 | ❌ CRITICAL |
| Scenes.kt | 8/10 | ✅ PASS |
| RoundScene.kt | 7/10 | ⚠️ NEEDS WORK |

---

## Phase 1: Detailed Audit Results

### Theme.kt

**Drunk Person Test: PASS**  
The design tokens are correctly defined and accessible.

**Current State: 9/10**

#### ✅ Correct Implementations
- Color system matches specification exactly
- Typography scale is party-proof (18sp+ for interactive)
- Spacing system follows 4/8/12/16/24/32 scale
- Animation timing tokens defined (Instant/Fast/Normal/Slow)
- Border radius system complete (Small→Pill)
- Accessibility flags: `LocalReducedMotion`, `LocalHighContrast`, `LocalNoFlash`
- High contrast mode adjustments

#### Issues Found
1. **[Missing Constants]**: Touch target size constants not defined
   → Add `MinimumTapTarget = 48.dp` and `RecommendedTapTarget = 60.dp` to HelldeckHeights

**Priority: Low**

**HELLDECK Alignment:**
- [x] Dark-first aesthetic
- [x] Neon accent usage
- [x] Party-proof legibility
- [x] Animation support
- [ ] Touch target constants (missing)

---

### Widgets.kt

**Drunk Person Test: PASS**  
Core components are well-designed and functional.

**Current State: 8/10**

#### ✅ Correct Implementations
- `CardFace`: Gradient borders, glow effects, auto-resizing text, stakes labels
- `FeedbackStrip`: 72dp circular buttons, proper colors (LOL/MEH/TRASH)
- `VoteButton`: 120x72dp size, spring animations, glow on selection, pulse effect
- `GameTimer`: Color transitions (cyan→orange→red), pulse on critical
- `EnhancedFeedbackButton`: Proper glow, counter badges
- Reduced motion support throughout

#### Issues Found
1. **[Timer Color]**: `GameTimer` uses `HelldeckColors.Yellow` for normal state instead of `colorAccentCool` (cyan)
   → Change line 767: `else -> HelldeckColors.TimerNormal` (which is cyan)

2. **[Reduced Motion]**: `PulsingEffect` composable doesn't check `LocalReducedMotion`
   → Add reduced motion check

3. **[PlayerAvatar]**: Size default is 64dp but interactive - should respect touch target minimum
   → Ensure clickable avatars have 48dp+ touch area

**Priority: Medium**

**HELLDECK Alignment:**
- [x] Dark-first aesthetic
- [x] Neon accent usage
- [x] Party-proof legibility
- [x] Touch-friendly targets (mostly)
- [x] Animation support
- [ ] Timer color consistency (minor)

---

### Interaction Renderers

**Drunk Person Test: FAIL**  
These components would confuse a sober person, let alone someone 3 drinks in.

**Current State: 3/10**

#### Critical Issues (ALL RENDERERS)

Every renderer shares these fundamental problems:

1. **No HELLDECK Styling**: Uses generic `MaterialTheme.colorScheme` instead of `HelldeckColors`
2. **No Spring Physics**: Selections are instant, not bouncy/satisfying
3. **No Glow Effects**: No accent-colored shadows on selection
4. **No Reduced Motion Support**: `LocalReducedMotion` not checked
5. **Missing Visual Hierarchy**: Plain cards/buttons with no gradient borders
6. **Small Touch Targets**: Some buttons may be below 48dp effective area
7. **No Stakes Clarity**: What's at risk is not shown
8. **No Feedback Animations**: No visual response to interaction

#### Per-Renderer Analysis

| Renderer | Expected Feel | Current State | Gap |
|----------|--------------|---------------|-----|
| VotePlayerRenderer | Player grid with avatars, glow on select | Plain cards, no avatars | CRITICAL |
| ABChoiceRenderer | Split screen debate feel | Two plain buttons | HIGH |
| TrueFalseRenderer | Lie detector vibe | Two plain buttons | HIGH |
| SmashPassRenderer | Tinder-style cards | Two plain buttons | CRITICAL |
| SpeedListRenderer | Bomb timer urgency | Plain text field | HIGH |
| TabooGuessRenderer | Forbidden words display | Plain input | HIGH |
| MiniDuelRenderer | VS screen competitive | Two plain buttons | HIGH |
| JudgePickRenderer | Judge spotlight | Just "Continue" button | CRITICAL |
| ReplyToneRenderer | Message bubbles | Column of buttons | MEDIUM |
| TargetSelectRenderer | Player spotlight | Unknown | NEEDS REVIEW |
| HideWordsRenderer | Hidden text reveal | Unknown | NEEDS REVIEW |
| OddExplainRenderer | Triple display | Unknown | NEEDS REVIEW |
| PredictVoteRenderer | Betting interface | Unknown | NEEDS REVIEW |
| SalesPitchRenderer | Pitch presentation | Unknown | NEEDS REVIEW |

**Priority: CRITICAL**

**HELLDECK Alignment:**
- [ ] Dark-first aesthetic
- [ ] Neon accent usage
- [ ] Party-proof legibility
- [ ] Touch-friendly targets
- [ ] Animation support
- [ ] Spring physics
- [ ] Glow effects
- [ ] Stakes clarity

---

### RoundScene.kt

**Drunk Person Test: PASS (barely)**  
The main game flow works, but relies on custom flows instead of renderers.

**Current State: 7/10**

#### ✅ Correct Implementations
- Uses `HelldeckColors` consistently
- Has stakes labels for each game
- Timer is prominent and well-positioned
- Uses proper spacing system
- Bottom bar has proper button sizes (60dp height)
- Primary CTA uses `HelldeckRadius.Pill`

#### Issues Found
1. **[Architecture]**: Interaction logic is in RoundScene instead of InteractionRenderer dispatch
   → The `when (game?.interaction)` block duplicates what renderers should handle

2. **[Help Button]**: Just shows "?" - could be more discoverable
   → Consider icon or more prominent styling

**Priority: Medium (architectural concern)**

---

## Phase 2: Prioritized Fix List

### 🔴 CRITICAL (Must Fix)

1. **Rewrite all interaction renderers** to use:
   - `HelldeckColors` instead of `MaterialTheme.colorScheme`
   - Spring animations with `dampingRatio = 0.6f`
   - Glow effects via `.shadow()` with accent colors
   - `LocalReducedMotion` checks
   - 48dp+ touch targets (60dp preferred)
   - Gradient borders on selected states

### 🟠 HIGH (Should Fix)

2. **Fix GameTimer normal color** to use cyan (`HelldeckColors.TimerNormal`)
3. **Add reduced motion check** to `PulsingEffect`
4. **Add touch target constants** to `HelldeckHeights`

### 🟡 MEDIUM (Nice to Have)

5. **Review remaining renderers** (TargetSelect, HideWords, OddExplain, PredictVote, SalesPitch)
6. **Consider consolidating** RoundScene interaction logic into renderer system

---

## Phase 3: Implementation Templates

### Template: HELLDECK-Compliant Button

```kotlin
@Composable
fun HelldeckButton(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    accentColor: Color = HelldeckColors.colorPrimary,
    modifier: Modifier = Modifier,
) {
    val reducedMotion = LocalReducedMotion.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = when {
            isSelected -> 1.05f
            isPressed -> 0.95f
            else -> 1f
        },
        animationSpec = if (reducedMotion) {
            tween(HelldeckAnimations.Instant)
        } else {
            spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessHigh)
        },
        label = "button_scale",
    )

    val glowAlpha by animateFloatAsState(
        targetValue = if (isSelected) 0.6f else 0.2f,
        animationSpec = tween(if (reducedMotion) HelldeckAnimations.Instant else HelldeckAnimations.Fast),
        label = "glow_alpha",
    )

    Button(
        onClick = onClick,
        modifier = modifier
            .height(HelldeckHeights.Button.dp)
            .scale(scale)
            .shadow(
                elevation = if (isSelected) 8.dp else 4.dp,
                shape = RoundedCornerShape(HelldeckRadius.Medium),
                spotColor = accentColor.copy(alpha = glowAlpha),
                ambientColor = accentColor.copy(alpha = glowAlpha * 0.5f),
            ),
        interactionSource = interactionSource,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) accentColor.copy(alpha = 0.25f) else HelldeckColors.surfacePrimary,
            contentColor = HelldeckColors.colorOnDark,
        ),
        border = BorderStroke(
            width = if (isSelected) 3.dp else 1.dp,
            color = if (isSelected) accentColor else HelldeckColors.colorMuted,
        ),
        shape = RoundedCornerShape(HelldeckRadius.Medium),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
        )
    }
}
```

### Template: Selection Animation

```kotlin
val scale by animateFloatAsState(
    targetValue = if (isSelected) 1.05f else 1f,
    animationSpec = if (LocalReducedMotion.current) {
        tween(HelldeckAnimations.Instant)
    } else {
        spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessHigh)
    },
    label = "selection_scale",
)
```

---

## Quality Gates Checklist

Before marking any component complete, verify:

- [ ] **Drunk Person Test**: 3 drinks in, dim room, arm's length—still usable?
- [ ] **Dark Mode Excellence**: Does it pop on OLED? No washed-out grays?
- [ ] **Neon Coherence**: Accent colors used consistently and intentionally?
- [ ] **Touch-Friendly**: All targets ≥48dp, ideally 60dp?
- [ ] **Animation Respect**: Reduced motion properly supported?
- [ ] **Stakes Clarity**: Is it obvious what's at risk?
- [ ] **Spring Physics**: Do selections feel bouncy and satisfying?
- [ ] **Glow Effects**: Accent-colored shadows on interactive elements?
- [ ] **Performance**: Smooth 60fps animations, no jank?

---

## Next Steps

1. ✅ Audit complete
2. 🔄 Fix Theme.kt (add touch target constants)
3. 🔄 Fix Widgets.kt (timer color, reduced motion)
4. 🔄 Rewrite interaction renderers with HELLDECK styling
5. ⏳ Test in party conditions

---

*Built for chaos. Designed for glory. Welcome to Hell's Living Room.* 🔥
