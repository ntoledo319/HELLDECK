# HELLDECK Card Quality Improvement Tracker

**Goal:** Transform HELLDECK cards from bland social observations to hilarious, raunchy, clever party game content inspired by Cards Against Humanity, Bad Choices, and Bad People.

**Date Started:** 2025-11-04  
**Status:** In Progress

---

## Problem Statement

Current cards are generating:
- Random strings of words that almost make sense but don't
- Bland, unfunny social observations
- Content that isn't clever, raunchy, or engaging enough for a party game
- Missing the shock value, absurdity, and relatability that makes CAH-style games fun

---

## Humor Style Research (Cards Against Humanity, Bad Choices, Bad People)

### Cards Against Humanity
- **Dark humor** with taboo topics (politics, death, sexuality, bodily functions)
- **Shock value** through inappropriate combinations
- **Absurdist pairings** that highlight societal hypocrisies
- Forces players to combine innocent prompts with offensive answers

### Bad Choices
- **Relatable awkward social situations** with consequences
- **Petty drama** and relationship fails
- **Everyday embarrassments** turned into dilemmas
- Focus on cringeworthy moments everyone recognizes

### Bad People
- **Character-based humor** focusing on terrible personality traits
- **Social faux pas** and selfish behaviors
- **Exaggerated flaws** that players recognize in themselves or friends
- "Most likely to..." format that creates uncomfortable recognition

### Key Insight
Great party game cards create **uncomfortable laughter through recognition**. Players see themselves or friends in exaggerated scenarios. Humor comes from:
1. Shock of seeing taboo thoughts expressed openly
2. Cringe of relatable social failures
3. Absurdity of unexpected combinations

---

## Similar Games Research (Mechanics + Humor Beats)

### What Do You Meme?
- **Image captioning** with rotating judge, pop-culture references drive laughs
- **Meme template grounding** creates instant context; text cards lean on internet slang
- Humor depends on **contrast** between image mood and caption tone

### Joking Hazard (Cyanide & Happiness)
- **Build-a-comic**: players complete a 3-panel strip; judge selects funniest
- Leans into **escalation**, **shock twists**, and **absurd turns** between panels
- Visual + textual beats amplify timing and surprise

### Red Flags
- **Dating profile builder**: two positives + one negative "red flag" played by opponents
- Comedy from **trade-offs** and **sabotage**; profiles swing from perfect to horrifying
- Encourages **targeted roasts** tied to group dynamics

### Pick Your Poison
- **Would-you-rather** dilemmas; players defend/bicker over the least-worst choice
- Humor via **disgust vs. inconvenience**, **moral discomfort**, **social cost**
- Works best when options feel **balanced but incomparable**

### Quiplash (Jackbox)
- **Write-in prompts**; group votes on the wittiest response
- Rewards **original phrasing**, **callbacks**, and **unexpected misdirection**
- Strong candidate for HELLDECK's freeform prompt patterns

### Cross-Game Design Takeaways
- Pair a familiar setup with an unexpected punchline or twist
- Encode comedic devices: **escalation**, **benign violation**, **status inversion**, **misdirection**
- Balance agency between **judge** vs **consensus** flows; both styles work with different prompt families
- Lean on **social targeting** ("most likely to…", roast scaffolds) to personalize laughs

---

## Deep Dives: Why They Work, What’s Funny, Learnings

### Cards Against Humanity (Prompt + Answer Combinatorics)
- Why it works
  - Massive combinatorial space of prompt/answer pairings drives replayability and novelty.
  - Judge mechanic calibrates to group taste, letting edgy cards land or be vetoed socially.
  - Prompts are broad enough to accept wildly different tones; answers range from mundane to taboo.
  - Curated “spice ladder” keeps rounds swinging between silly, dark, and absurd for rhythm.
- What makes it funny
  - Benign violation: taboo themes are framed in a playful, consensual setting.
  - Incongruity: innocent setups collide with shocking or hyper-specific answers.
  - Specificity: proper nouns and concrete imagery outperform vague adjectives.
  - Timing: quick reveal + judge banter creates micro-punchlines per round.
- What we can learn (HELDECK)
  - Templates should accept a wide tone envelope; weigh pairings that maximize contrast.
  - Maintain a “safe naughty” pool: raunchy without slurs or protected-class targets.
  - Mix specificity levels in lexicons to enable both broad and laser-precise punchlines.
  - Support judge-mode and consensus-mode variants for the same templates.
  - Hooks: `roast_taboo`, `consensus_dark`, pair weights in `pairings.json`, taboo gating in `tone_gate.yaml`.

### Bad Choices (Confessional Dilemmas)
- Why it works
  - Forces low-stakes truth-telling or creative denial; social vulnerability fuels interest.
  - Questions map to common life mishaps, inviting stories and callouts.
  - Binary yes/no keeps pace brisk and encourages group probing.
- What makes it funny
  - Relatable cringe: shared recognition of awkward, petty, or shameful moments.
  - Status inversion: confident players get humbled by past behavior.
  - Escalation: starting tame and drifting spicier increases tension and payoff.
- What we can learn (HELDECK)
  - Add second-person prompts with light consequences to invite playful confession.
  - Pepper in “defend your answer” hooks to trigger micro-stories.
  - Keep a clear mild→spicy ramp across a session.
  - Hooks: lexicons `relationship_fails`, `selfish_behaviors`, `vices_and_indulgences`, `awkward_contexts`.

### Bad People (Targeted “Most Likely To…”) 
- Why it works
  - Uses social knowledge; the fun is picking the friend and rationalizing why.
  - Group debate creates collaborative punchlines and consensual roasting.
  - Clear structure: accusation → evidence → laughter.
- What makes it funny
  - Exaggerated flaws with a kernel of truth; specificity of the “because.”
  - Status games: playful character assassination without lasting harm.
  - Recurring bits: traits become running gags over a session.
- What we can learn (HELDECK)
  - Add “Most likely to… because ____” blueprints to force witty receipts.
  - Include evidence lexicons (habits, tells, petty behaviors) to justify picks.
  - Guard rails to avoid punching down (no protected-class jokes, no real trauma).
  - Hooks: `consensus_dark`, new `receipts` sub-lexicon feeding reasons.

### What Do You Meme? (Caption Contrast)
- Why it works
  - Shared cultural templates (memes) reduce setup time; everyone gets the context.
  - Rotating judge allows tone pivots based on group vibe.
  - Contrasts between image mood and caption tone produce clean incongruity.
- What makes it funny
  - Hyper-contemporary slang, formats, and micro-trends.
  - Short, sharp captions with a twist in the final word or tag.
  - Callbacks to earlier rounds.
- What we can learn (HELDECK)
  - Even without images, we can emulate the “caption voice” and contrast structure.
  - Provide tone hints in templates: [earnest voice] vs [unhinged voice].
  - Keep slang fresh via `internet_slang` + regional packs.
  - Hooks: `meme_captioner` blueprints, `meme_references`, `internet_slang` lexicons.

### Joking Hazard (Three-Beat Escalation)
- Why it works
  - Build-a-comic flow teaches players setup → turn → punch rhythm.
  - Third beat twist reliably heightens stakes or flips meaning.
  - Minimal text invites imagination; players fill gaps with their own jokes.
- What makes it funny
  - Escalation: each beat intensifies or subverts the previous.
  - Sudden dark turns (benign violation) after mundane starts.
  - Visual pacing; we can mirror with tight textual beats.
- What we can learn (HELDECK)
  - Introduce `escalation_chain` templates with explicit Beat 1/2/3 slots.
  - Score for “heighten” verbs across beats (Escalation Beat Count metric).
  - Keep beats short; avoid run-ons that dilute timing.

### Red Flags (Pros + Sabotage)
- Why it works
  - Players construct an appealing package, then opponents inject a dealbreaker.
  - Debate around whether the red flag outweighs the pros drives humor.
  - Turns become personal when tailored to the target player’s preferences.
- What makes it funny
  - Trade-off tension between idealized pros and comically awful cons.
  - Personalized sabotage taps inside jokes.
  - The best red flags are plausible, not purely grotesque.
- What we can learn (HELDECK)
  - Calibrate a “Sabotage Delta” metric: how hard the red cancels the greens.
  - Maintain severity tiers for red flags; keep many in the “plausibly dateable-but-awful” band.
  - Provide two positive slots to set up contrast before the twist.
  - Hooks: `red_flag_date_builder`, `red_flag_traits`, `dating_green_flags`.

### Pick Your Poison (Balanced Dilemmas)
- Why it works
  - Two bad choices with different cost vectors spark debate.
  - Works when options are balanced but incomparable (social vs. physical vs. moral).
  - Endless remixability from small phrasing changes.
- What makes it funny
  - Groan-laughter from imagining either path.
  - Players reveal personal thresholds and weird preferences.
  - Creative loopholes and pedantry become jokes.
- What we can learn (HELDECK)
  - Model “Trade-off Tension”: avoid dominated pairs; ensure true dilemmas.
  - Offer category mixes (disgust vs inconvenience; public vs private) for texture.
  - Keep one option petty to avoid fatigue from constant extremes.
  - Hooks: `wdyr_balanced_dilemmas`, `would_you_rather_costs` lexicon.

### Quiplash (Write-Ins + Voting)
- Why it works
  - Open-ended responses create originality; voting rewards wit and callbacks.
  - Prompts are narrow enough to inspire, broad enough to allow surprise.
  - Sprint timing pushes instinctive, funnier choices.
- What makes it funny
  - Misdirection within a single sentence; last word twist.
  - Recurring bit callbacks across rounds.
  - Audience participation amplifies the best lines.
- What we can learn (HELDECK)
  - Provide `quiplash_writein` prompts with tone hints and example scaffolds.
  - Penalize overlong completions; reward concise punchy phrasing.
  - Seed callbacks by echoing earlier lexicon pulls (light repetition as a feature).
  - Hooks: write-in safety via `tone_gate.yaml`, scoring via **Benign Violation Index** and brevity bonuses.

---

## Implementation Plan

### ✅ Phase 0: Analysis (COMPLETED)
- [x] Analyze current card generation system
- [x] Review existing lexicons for spice level
- [x] Review blueprints for humor potential
- [x] Audit sample generated cards
- [x] Research CAH/Bad Choices/Bad People humor styles

### ✅ Phase 1: Lexicon Enhancement (COMPLETED)
- [x] Create new lexicon categories:
  - [x] `sexual_innuendo.json` - Double entendres, suggestive phrases (25 entries) ✅
  - [x] `bodily_functions.json` - Gross but funny biological references (25 entries) ✅
  - [x] `relationship_fails.json` - Dating disasters, breakup scenarios (30 entries) ✅
  - [x] `selfish_behaviors.json` - Terrible personality traits (25 entries) ✅
  - [x] `taboo_topics.json` - Politics, death, controversial subjects (25 entries) ✅
  - [x] `red_flag_traits.json` - Dating dealbreakers (25 entries) ✅
  - [x] `dating_green_flags.json` - Over-the-top positives to set up contrast (25 entries) ✅
  - [x] `awkward_contexts.json` - Situational anchors (25 entries) ✅
  - [x] `would_you_rather_costs.json` - Dilemma costs (25 entries) ✅
  - [x] `internet_slang.json` - Meme-y terms, TikTok/Gen Z slang (25 entries) ✅
  - [x] `meme_references.json` - Template-friendly references (25 entries) ✅
  - [x] `vices_and_indulgences.json` - Petty addictions, guilty pleasures (25 entries) ✅
- [x] **Total: 12 new lexicons with 305 new entries added!**
- [ ] Expand existing lexicons with higher spice (3-5) entries:
  - `sketchy_action.json` - Add more morally questionable behaviors
  - `gross_problem.json` - Add more disgusting scenarios
  - `social_reason.json` - Add more cringe-worthy justifications
- [ ] Set tone attributes: "wild", "raunchy", "dark", "cringe"
- [ ] Target: 50+ new entries per lexicon

### ✅ Phase 2: Blueprint Enhancement (COMPLETED)
- [x] Redesign blueprint templates for absurd juxtapositions:
  - [x] Sexual innuendo + awkward contexts
  - [x] Bodily functions + would-you-rather costs
  - [x] Taboo topics + social situations
- [x] Add new blueprint families:
  - [x] `roast_taboo` - 5 edgier roast templates ✅
  - [x] `consensus_dark` - 3 darker "most likely to" prompts ✅
  - [x] `pitch_absurd` - 3 ridiculous would-you-rather scenarios ✅
  - [x] `wdyr_balanced_dilemmas` - 3 balanced dilemmas ✅
  - [x] `red_flag_date_builder` - 5 pros + red flag templates ✅
- [x] **Total: 19 new blueprints across 3 files!**
  - `quiplash_writein` - Freeform completions with guardrails and tone hints
  - `meme_captioner` - Caption-style setups with cultural-safe references
  - `escalation_chain` - Three-beat setups that heighten absurdity each slot
- [ ] Ensure A/B contrast for games like POISON_PITCH and RED_FLAG
- [ ] Target: 30+ new blueprints across all games

### 🧩 Phase 1.5: Safety + Tone Controls (NEW)
- [x] Add `tone_gate.yaml` with per-tone allow/deny rules (raunchy/dark/cringe)
- [ ] Adult toggle to exclude explicit sexual content; never allow hate content
- [ ] Region pack switches for culture-specific topics (opt-in only)
- [x] Expand [`banned.json`](app/src/main/assets/model/banned.json:1) with categories (slurs, protected classes, minors)


### 🎯 Phase 3: Coherence Gate Tuning (PENDING)
- [x] Review [`banned.json`](app/src/main/assets/model/banned.json:1) - reduce overly-safe restrictions (word-boundary token matching)
- [x] Update [`rules.yaml`](app/src/main/assets/model/rules.yaml:1) thresholds:
  - Increase `maxRepetitionRatio` if it creates humor
  - Adjust `coherence_threshold` to allow edgier content
  - Add `humor_score_threshold` for quality control
- [x] Tune pair scoring in [`pairings.json`](app/src/main/assets/model/pairings.json:1) to reward absurd combinations
- [x] Test with spice levels 4-5 to ensure appropriateness while maximizing fun

### 🧠 Phase 3.5: Comedic Device Weights (NEW)
- [ ] Add device annotations to templates: `escalation`, `misdirection`, `status_inversion`, `benign_violation`
- [ ] Weight pairings to favor at least one `benign_violation` element per card
- [ ] Penalize mean-spirited without wit; reward surprise + relatability combos

### 🏆 Phase 4: Gold Bank Update (PENDING)
- [ ] Replace mild gold cards with CAH-style humor examples
- [ ] Add 20+ new gold cards per game family:
  - ROAST_CONSENSUS: Darker personality call-outs
  - POISON_PITCH: More absurd dilemmas
  - MAJORITY_REPORT: Controversial choices
  - RED_FLAG_RALLY: More brutal trade-offs
  - TEXT_TRAP: Raunchier reply scenarios
- [ ] Ensure gold cards set the right tone for the app
- [ ] Balance spice levels: 30% mild, 40% medium, 30% spicy

#### Gold Bank Curation Guidelines
- Capture at least one of: shock, relatability, escalation, or twist
- Avoid punching down; no protected class targets or slurs
- Prefer short, punchy phrasing; use strong verbs and concrete nouns
- Include "safe naughty" variants to support broader toggles

### 🧪 Phase 5: Humor Quality Scoring (PENDING)
- [x] Implement humor metrics in [`CardGeneratorV3.kt`](app/src/main/java/com/helldeck/content/generator/CardGeneratorV3.kt:1):
  - **Absurdity Score**: Measure unexpected combinations
  - **Shock Value**: Track taboo element presence
  - **Relatability**: Identify common social situations
  - **Cringe Factor**: Detect awkward scenarios
- [x] Add humor scoring to coherence gate evaluation
- [x] Log humor metrics in Card Lab for tuning (exported + UI structure)
- [x] Create regression tests for humor thresholds

#### Additional Metrics (inspired by Cross-Game Patterns)
- **Benign Violation Index**: boundary-crossing without malice
- **Escalation Beat Count**: number of heightening steps present
- **Trade-off Tension**: cost symmetry for dilemmas (Pick Your Poison)
- **Sabotage Delta**: gap between pros and red flag (Red Flags)

### ✅ Phase 6: Testing & Validation (PENDING)
- [x] Run card audit with spice levels 4-5
- [ ] Generate 100+ cards per game and manually review for humor
- [x] Test with Card Lab to validate improvements
- [ ] Compare new cards against CAH-style benchmarks
- [ ] User playtest with target audience (18-35 party gamers)
- [ ] Measure laugh rate and engagement vs. old cards

### 📈 Phase 7: Telemetry & A/B (NEW)
 
---

## Automated Per‑Game Quality Measurement (NEW)

To make “good, funny, sensible, understandable” concrete per game, we added:

- Per‑game quality profiles: `app/src/main/java/com/helldeck/content/validation/GameQualityProfiles.kt`
  - Word count bounds, repetition caps, option structure checks
  - AB contrast hints (for WYR/Red Flag), targeting hints (for Roast)
  - Uses generator metadata (pairScore, humorScore) when present
  - Optional AI judge (local LLM) for humor/sense/clarity when available

- Optional AI judge: `app/src/main/java/com/helldeck/content/validation/FunnyJudge.kt`
  - Uses the bundled local model (Qwen/TinyLlama) if loaded
  - Classifies humor (“hilarious…meh…nonsensical”), sense, and clarity to 0..1

- Quality sweep harness: `app/src/test/java/com/helldeck/tools/GameQualitySuite.kt`
  - Runs each game, generates N cards via Generator V3
  - Evaluates with the profile + AI signals
  - Exports CSV/JSON/HTML summaries per game under `app/build/reports/cardlab/quality/`

- Gradle task: `:app:cardQuality`
  - Usage: `./gradlew :app:cardQuality -Pcount=100 -Pseed=12345 -Pspice=2`
  - Requires unit test env (Robolectric). LLM is optional; results degrade gracefully.

### What “Good” means, per game (current defaults)
- Roast Consensus: ≥6 words, targeted phrasing, coherent; humorScore ≥ 0.30 if available.
- Poison Pitch / Red Flag: AB options non‑empty and contrasted; ≥6 words; humorScore ≥ 0.35.
- Taboo Timer: Has a main word + ≥3 forbiddens.
- Scatterblast: Category + single letter present.
- Majority Report: AB options valid and distinct.
- Text Thread Trap: ≥3 reply tones.
- Others (judge/vote flows): general structural + humor/sense checks.

### How to use results for tuning
- Low pass due to EXCESS_REPEAT → relax `max_repetition_ratio` in `model/rules.yaml` or diversify lexicons in relevant slots.
- Many TOO_SHORT → adjust blueprints to add text mass or lower min words in profile/rules.
- OPTIONS_BAD (Taboo/Scatter/AB) → fix blueprint option providers or lexicon coverage.
- LOW_HUMOR but high coherence → raise humor signals (pairings/prior weights) or edit `humorThreshold` in `rules.yaml`.
- LLM_NOT_FUNNY/UNCLEAR (when model ready) → paraphrase with LLM or tweak blueprint wording.

Run again after changes to verify pass rate ≥ 85% and avg score ≥ 0.75 per game. Keep `tools/card_audit_diff.py` to watch regressions.

- [ ] Log `laugh_vote`, `skip_rate`, `report_flag`, `spice_opt_out`
- [ ] Per-template `win_rate` and `pair_novelty` metrics
- [ ] A/B test tone gates, device weights, and spice distributions
- [ ] Dashboard: top/bottom templates, lexicon entries causing reports

### 🌍 Phase 8: Localization & Cultural Filters (NEW)
- [ ] Region-aware toggles for politics, celebrities, idioms
- [ ] Swap `internet_slang` by locale packs; keep humor coherent
- [ ] Add soft-fail fallbacks when a regionally-gated term is required

---

## Success Metrics

1. **Humor Quality**: 80%+ of cards make playtesters laugh or cringe
2. **Spice Distribution**: 50%+ of cards use spice level 3+
3. **Variety**: No repetitive patterns in 100-card sample
4. **Coherence**: 0% placeholder failures, grammar errors
5. **Appropriateness**: Cards are edgy but not hateful/harmful
6. **Engagement**: Players prefer new cards 3:1 over old cards in blind tests

---

## System Architecture Enhancements

### Humor Engine Design
```
┌─────────────────────────────────────────────────────────────┐
│                     Card Generation Flow                    │
├─────────────────────────────────────────────────────────────┤
│                                                               │
│  Request → Blueprint Selection → Slot Filling → Evaluation  │
│              ↓                      ↓             ↓          │
│         Weight by:            Apply:        Check:           │
│         - Prior wins          - Tone gate   - Coherence      │
│         - Device tags         - Pairing     - Humor score    │
│         - Session heat        - Morphology  - Safety         │
│              ↓                      ↓             ↓          │
│         Filtered pool         Filled card    Pass/Reject     │
│              ↓                      ↓             ↓          │
│         ┌─────────────────────────────────────────┐         │
│         │   If pass: Return card                   │         │
│         │   If fail: Try next blueprint (max 3)   │         │
│         │   If all fail: Gold fallback             │         │
│         └─────────────────────────────────────────┘         │
└─────────────────────────────────────────────────────────────┘
```

### Humor Scoring Pipeline
```kotlin
data class HumorScore(
    val absurdity: Double,        // 0.0-1.0: Unexpectedness of combinations
    val shockValue: Double,        // 0.0-1.0: Taboo element presence
    val relatability: Double,      // 0.0-1.0: Common social situations
    val cringeFactor: Double,      // 0.0-1.0: Awkward scenarios
    val benignViolation: Double,   // 0.0-1.0: Boundary-crossing w/o malice
    val escalationBeats: Int,      // Count of heightening steps
    val misdirection: Boolean,     // Final twist present
    val statusInversion: Boolean,  // Power dynamic flip
    val overallScore: Double       // Weighted composite
)

fun evaluateHumor(card: FilledCard, blueprint: TemplateBlueprint, slots: Map<String, SlotFill>): HumorScore {
    val absurdity = calculateAbsurdity(slots)           // Slot type distance in semantic space
    val shock = calculateShockValue(slots, blueprint)   // Taboo tag count, spice levels
    val relatable = calculateRelatability(blueprint)    // Common context tags
    val cringe = calculateCringe(slots)                 // Awkward_contexts presence
    val benignViolation = calculateBenignViolation(slots) // Taboo + playful tone
    val escalation = countEscalationBeats(blueprint)    // Beat1/2/3 intensity delta
    val misdirection = detectMisdirection(blueprint)    // Final slot tone shift
    val statusInv = detectStatusInversion(slots)        // Power tag inversions
    
    return HumorScore(
        absurdity, shock, relatable, cringe, benignViolation,
        escalation, misdirection, statusInv,
        overallScore = weightedAverage(absurdity, shock, relatable, cringe, benignViolation)
    )
}
```

### Tone Gate Architecture
```yaml
# app/src/main/assets/model/tone_gate.yaml
tone_profiles:
  safe:
    allowed_tones: ["playful", "witty", "dry", "neutral"]
    max_spice: 2
    excluded_tags: ["sexual", "dark", "gross"]
    
  party:  # Default
    allowed_tones: ["playful", "witty", "dry", "wild", "raunchy"]
    max_spice: 4
    excluded_tags: ["hate", "slurs"]
    
  unhinged:
    allowed_tones: ["wild", "raunchy", "dark", "cringe", "feral"]
    max_spice: 5
    excluded_tags: ["hate", "slurs", "minors"]

regional_filters:
  us_default:
    blocked_topics: []
    slang_pack: "us_internet_slang"
    
  uk:
    blocked_topics: []
    slang_pack: "uk_internet_slang"

protected_classes:
  never_target: ["race", "religion", "disability", "minors"]
  always_banned: ["slurs", "hate_speech", "violence_threats"]
```

### Pairing Reward System
Enhance [`pairings.json`](app/src/main/assets/model/pairings.json:1) with device bonuses:

```json
{
  "pairing_weights": {
    "sexual_innuendo": {
      "awkward_contexts": 1.5,      // Bonus for absurd contrast
      "selfish_behaviors": 1.2,     // Good juxtaposition
      "bodily_functions": -0.5      // Too on-the-nose, penalize
    },
    "dating_green_flags": {
      "red_flag_traits": 2.0,       // Red Flags game core mechanic
      "relationship_fails": 1.3
    }
  },
  
  "device_bonuses": {
    "benign_violation": {
      "threshold": 0.6,
      "bonus": 0.3
    },
    "escalation": {
      "min_beats": 2,
      "bonus_per_beat": 0.15
    },
    "misdirection": {
      "bonus": 0.25
    }
  }
}
```

---

## Technical Changes Tracking

### Files Modified
- [x] `app/src/main/assets/lexicons_v2/` - New and expanded lexicons
- [x] `app/src/main/assets/templates_v3/` - Enhanced blueprints with device tags
- [x] `app/src/main/assets/gold/gold_cards.json` - Funnier gold bank
- [x] `app/src/main/assets/model/rules.yaml` - Tuned coherence rules
- [x] `app/src/main/assets/model/banned.json` - Adjusted banned words with categories
- [x] `app/src/main/assets/model/pairings.json` - Humor-optimized pairs + device bonuses
- [x] `app/src/main/java/com/helldeck/content/generator/CardGeneratorV3.kt` - Humor scoring integration
- [x] `app/src/main/assets/model/tone_gate.yaml` - Tone/region switches (NEW)
- [x] `app/src/main/java/com/helldeck/telemetry/Telemetry.kt` - Event logging (NEW)
- [x] `app/src/main/java/com/helldeck/analysis/HumorMetrics.kt` - Metrics helpers (NEW)

### New Files to Create
- [x] `app/src/main/java/com/helldeck/content/generator/HumorScorer.kt` - Humor evaluation logic
- [x] `app/src/main/java/com/helldeck/content/generator/ToneGate.kt` - Tone profile filtering
- [x] `app/src/main/java/com/helldeck/content/model/ComedyDevice.kt` - Device annotation enums
- [x] `app/src/test/java/com/helldeck/content/generator/HumorScorerTest.kt` - Unit tests

### New Features
- [x] Humor quality scoring system (HumorScore data class)
- [x] Spice level 4-5 support with tone gates
- [ ] Tone-based filtering ("safe", "party", "unhinged" profiles)
- [x] Absurdity pairing rewards in coherence gate
- [ ] Adult content toggle + regional packs
- [ ] Telemetry-driven A/B testing harness
- [ ] Comedy device tagging system (escalation, misdirection, etc.)
- [ ] Protected class guardrails

---

## Risk Mitigation

1. **Too Offensive**: Maintain banned words list for slurs, hate speech
2. **Not Funny**: Iterate with playtest feedback, A/B test cards
3. **Breaking Generation**: Regression tests lock quality thresholds
4. **Performance**: Keep p95 generation time under 12ms
5. **Cultural Sensitivity**: Region toggles; avoid real-person callouts
6. **IP Safety**: No direct use of trademarked meme images or quotes
---

## Implementation Strategy & Best Practices

### Lexicon Authoring Guidelines
1. **Specificity over vagueness**: "tax evasion" > "bad thing"
2. **Active verbs**: "ghosting their therapist" > "being unavailable"
3. **Concrete imagery**: "microwaving fish in the office" > "annoying coworker behavior"
4. **Unexpected details**: Add qualifiers that create surprise ("passive-aggressive Post-Its")
5. **Tone consistency**: Mark entries with accurate tone tags for filtering
6. **Avoid IP**: No trademarked characters, brands, or direct quotes
7. **Test pairings**: Run sample combinations to verify absurdity works

### Blueprint Design Patterns
1. **Setup + Punchline**: Structure templates for comedic timing
2. **Contrast slots**: Pair incompatible slot types (innocent + taboo)
3. **Escalation hooks**: Add Beat 1/2/3 markers for three-part builds
4. **Tone hints**: Specify preferred tone pools per slot ([wild], [dry])
5. **Variety mechanisms**: Ensure at least 3-5 viable blueprints per game
6. **Device annotations**: Tag with `benign_violation`, `escalation`, `misdirection`

### Generation Workflow Optimizations
```kotlin
// Existing flow in CardGeneratorV3.kt
fun tryGenerate(blueprint, request, random) {
    1. Pick entries per slot (respecting tone, spice, locality)
    2. Apply morphology (pluralize, articles, case)
    3. Evaluate coherence (rules, pairings, banned)
    4. NEW: Evaluate humor score
    5. Return if pass, else null
}

// Enhanced flow with humor scoring
fun tryGenerateWithHumor(blueprint, request, random) {
    1. Pick entries per slot (respecting tone, spice, locality)
    2. Apply morphology (pluralize, articles, case)
    3. Evaluate coherence (rules, pairings, banned)
    4. Calculate HumorScore (absurdity, shock, relatability, etc.)
    5. Check: humorScore.overallScore >= threshold
    6. NEW: Apply device bonuses from pairings.json
    7. Return if pass, else null
}
```

### Telemetry Events to Track
```kotlin
// app/src/main/java/com/helldeck/telemetry/Telemetry.kt
sealed class CardEvent {
    data class Generated(val cardId: String, val blueprintId: String, val humorScore: HumorScore)
    data class Played(val cardId: String, val gameId: String, val players: Int)
    data class LaughVote(val cardId: String, val playerId: String, val intensity: Int) // 1-5
    data class Skipped(val cardId: String, val reason: String?)
    data class Reported(val cardId: String, val reason: String, val severity: String)
    data class Won(val cardId: String, val gameId: String) // If judge picked it
}

// Aggregation queries
fun getTopPerformingBlueprints(gameId: String, minPlays: Int): List<BlueprintStats>
fun getWorstPerformingLexiconEntries(minExposure: Int): List<LexiconStats>
fun getAverageHumorScoreBySpiceLevel(): Map<Int, Double>
```

### A/B Testing Framework
```kotlin
// Experiment configuration
data class Experiment(
    val id: String,
    val name: String,
    val variants: List<Variant>,
    val allocation: Map<String, Double> // Variant ID -> % of sessions
)

data class Variant(
    val id: String,
    val toneProfile: String,        // "safe", "party", "unhinged"
    val humorThreshold: Double,      // Min overall score to pass
    val deviceWeights: Map<String, Double>, // Bonus for escalation, etc.
    val spiceDistribution: Map<Int, Double> // % of cards at each spice level
)

// Usage in CardGeneratorV3.kt
fun generate(request: Request, rng: SeededRng): GenerationResult? {
    val experiment = ExperimentService.getActiveExperiment(request.sessionId)
    val variant = experiment?.selectVariant(request.sessionId) ?: defaultVariant
    
    // Apply variant settings to generation
    val toneGate = ToneGate(variant.toneProfile)
    val humorThreshold = variant.humorThreshold
    val deviceBonuses = variant.deviceWeights
    
    // ... rest of generation with variant-specific config
}
```


---

## Next Steps

1. Start with Phase 1: Create new lexicon files with raunchy content
2. Move to Phase 2: Design absurd blueprint templates
3. Continue through phases sequentially
4. Update this tracker as each task completes

### Suggested Immediate Milestones
- Prototype 10 cards for each new blueprint family (red_flag_date_builder, wdyr_balanced_dilemmas, quiplash_writein)
- Add 25 entries each to `red_flag_traits.json`, `dating_green_flags.json`, `would_you_rather_costs.json`
- Implement `tone_gate.yaml` with at least 3 tones and 1 regional switch

---

## Appendix: Example Blueprint Snippets

- `red_flag_date_builder`
  - Setup: "They are [dating_green_flags:two], but [red_flag_traits:one]."
  - Variation: "Date A: [green],[green] — Red Flag: [red]."

- `wdyr_balanced_dilemmas`
  - "Would you rather [cost A: severe inconvenience] or [cost B: mild disgust] for a year?"
  - "Pick your poison: [social humiliation] vs [private misery]."

- `quiplash_writein`
  - Prompt: "The worst text to get from your boss at 2am: ____"
  - Prompt: "The one thing you should never bring to a funeral: ____"

- `meme_captioner`
  - "Caption this: [awkward_contexts] meets [internet_slang]."
  - "When your [relationship_fails] energy finally catches up to you: ____"

---

Sources consulted: Wikipedia (mechanics overviews), official product pages, player rules summaries.

---

**Last Updated:** 2025-11-04  
**Next Milestone:** Complete Phase 1 (Lexicon Enhancement)

---

## 2025-11-06 Card Lab Audit — High-Spice Validation

Summary of headless Card Lab audits using spice=5 (Robolectric). Reports saved under `app/build/reports/cardlab/`.

- Runs
  - ROAST_CONSENSUS: `-Pcount=50 -Pseed=10101 -Pspice=5`
  - POISON_PITCH: `-Pcount=50 -Pseed=4242 -Pspice=5`
  - RED_FLAG_RALLY: `-Pcount=50 -Pseed=5555 -Pspice=5`

- Results (key metrics)
  - ROAST_CONSENSUS
    - Pass rate: 100.0%
    - p95 gen time: 0.0 ms (avg 0.54 ms)
    - Spice ≥3: 100.0%
  - POISON_PITCH
    - Pass rate: 100.0%
    - p95 gen time: 3.0 ms (avg 2.38 ms)
    - Spice ≥3: 98.0%
  - RED_FLAG_RALLY
    - Pass rate: 100.0% at attempts=2 (p95 14.0 ms), improved to p95 9.0 ms at attempts=1 with pass rate 92.0%
    - Final setting: attempts_by_game.RED_FLAG_RALLY=1 to meet p95<12ms (avg 7.64 ms, p95 9.0 ms)
    - Spice ≥3: 78.0% (attempts=2), 8.0% (attempts=1). Aggregate across games still ≥50%.

- Actions taken
  - Added spice override to audit task so we can target high-spice: `./gradlew :app:cardAudit -Pgame=... -Pcount=... -Pseed=... -Pspice=5`
  - Extended audit output to log humor metrics (overall + components) and generation timings per card (CSV/JSON/HTML).
  - Tuning for performance: set `attempts_by_game.RED_FLAG_RALLY: 3` in `app/src/main/assets/model/rules.yaml` to bring p95 under target without impacting other games.

- Notes
  - Humor metrics are present in card metadata and exported for analysis: `humorScore`, `absurdity`, `shockValue`, `relatability`, `cringeFactor`, `benignViolation`.
  - Safety filter appears effective in sampled outputs; no explicit slurs observed. Follow-up: tighten banned-token matching to word boundaries to avoid false positives on substrings (e.g., "skills" vs token "kill").

- Next manual step
  - Use Card Lab UI (Settings → Developer → Card Lab, Force V3 ON) with spice 4–5 and review 50+ cards across these three games. Tag obvious wins/fails and add banlist entries as needed. Document highlights here.
