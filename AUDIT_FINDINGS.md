# HELLDECK Codebase Audit Findings

## Status: Phase 2 Complete ✅

See REFACTORING_SUMMARY.md for complete details of all changes made.

## Phase 1: Initial Discovery ✅

### ✅ GOOD NEWS: Core Game Definitions Match HDRealRules.md

The `GameMetadata.kt` file contains exactly the 14 games specified in HDRealRules.md:

1. ✅ Roast Consensus (ROAST_CONSENSUS)
2. ✅ Confession or Cap (CONFESSION_OR_CAP)
3. ✅ Poison Pitch (POISON_PITCH)
4. ✅ Fill-In Finisher (FILL_IN_FINISHER)
5. ✅ Red Flag Rally (RED_FLAG_RALLY)
6. ✅ Hot Seat Imposter (HOT_SEAT_IMPOSTER)
7. ✅ Text Thread Trap (TEXT_THREAD_TRAP)
8. ✅ Taboo Timer (TABOO_TIMER)
9. ✅ The Unifying Theory (THE_UNIFYING_THEORY)
10. ✅ Title Fight (TITLE_FIGHT)
11. ✅ Alibi Drop (ALIBI_DROP)
12. ✅ Reality Check (REALITY_CHECK)
13. ✅ Scatterblast (SCATTERBLAST)
14. ✅ Over / Under (OVER_UNDER)

### ⚠️ ISSUES FOUND:

#### 1. Timer Discrepancies
Several games have incorrect timer values compared to HDRealRules.md:

- **Confession or Cap**: Code has 6s, HDRealRules.md doesn't specify (should be reasonable for voting)
- **Text Thread Trap**: Code has 6s, HDRealRules.md doesn't specify (should be reasonable for tone selection)
- **Reality Check**: Code has 6s, HDRealRules.md doesn't specify (should be reasonable for rating)
- **Over / Under**: Code has 6s, HDRealRules.md doesn't specify (should be reasonable for betting)
- **The Unifying Theory**: Code has 8s, HDRealRules.md doesn't specify exact timer

#### 2. Description Mismatches
Some game descriptions in code don't match the detailed rules in HDRealRules.md. Need to verify:
- Descriptions are too brief
- Missing key mechanics details
- Need to align with HDRealRules.md language

#### 3. Legacy Game References
The code mentions removed games (HYPE_YIKE, MAJORITY, ODD_ONE) in comments but need to verify they're completely removed from:
- Card generation logic
- UI components
- Database schemas
- Documentation

## Phase 2: Backend Fixes Complete ✅

### Fixed Code Files (6 files):
1. app/src/main/java/com/helldeck/engine/GamesRegistry.kt - Comment only ✅
2. app/src/main/java/com/helldeck/ui/DurableUI.kt - Icon mappings ✅ FIXED
3. app/src/main/java/com/helldeck/ui/GameIcons.kt - Icon mappings ✅ FIXED
4. app/src/main/java/com/helldeck/content/validation/AssetValidator.kt - Validation list ✅ FIXED
5. app/src/main/java/com/helldeck/content/tools/TemplateLint.kt - Template list ✅ FIXED
6. app/src/main/java/com/helldeck/content/generator/HumorScorer.kt - Scoring logic ✅ FIXED
7. app/src/test/java/com/helldeck/content/generator/RuleRegressionTest.kt - Test list ✅ FIXED
8. app/src/main/java/com/helldeck/engine/GameMetadata.kt - Descriptions and timers ✅ UPDATED

### Fixed Asset Files:
1. app/src/main/assets/templates_v3/hype_or_yike.json ✅ DELETED
2. app/src/main/assets/templates_v3/odd_one_out.json ✅ DELETED
3. app/src/main/assets/templates_v3/majority_report.json ✅ DELETED
4. app/src/main/assets/templates_v2/majority_report.json ✅ DELETED
5. app/src/main/assets/templates/templates.json ✅ CLEANED
6. app/src/main/assets/gold/gold_cards.json ✅ DELETED (bad file)
7. app/src/main/assets/gold_cards.json ✅ UPDATED (700 cards, all 14 games)

### Created Template Files:
- ✅ reality_check.json - CREATED
- ✅ over_under.json - CREATED
- ✅ the_unifying_theory.json - CREATED

### Gold Cards Quality:
- ✅ All 14 games present with 50 cards each
- ✅ All cards have quality_score = 10
- ✅ No duplicates
- ✅ Adult-appropriate content (25-40 audience)
- ✅ 5-pass verification system created and passing

### Remaining Work:
- ⚠️ ~69 references to legacy games still exist in UI components, content engine, and tests
- ⚠️ Documentation needs updating (README, ARCHITECTURE, etc.)
- ⚠️ Legacy audit baselines in docs/card_audit_baselines/ should be removed