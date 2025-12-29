# HELLDECK Content Authoring Guide

**Version:** 2.0  
**Last Updated:** December 2024  
**For:** Content Authors

---

## Table of Contents

1. [Overview](#overview)
2. [Gold Cards (Primary Content)](#gold-cards-primary-content)
3. [Blueprint V3 Schema (Fallback)](#blueprint-v3-schema-fallback)
4. [Lexicon V2 Schema](#lexicon-v2-schema)
5. [Content Guidelines](#content-guidelines)
6. [Examples](#examples)
7. [Validation & Testing](#validation--testing)
8. [Common Mistakes](#common-mistakes)

---

## Overview

As of December 2024, HELLDECK uses an **LLM-powered generation system** (`LLMCardGeneratorV2`) as the primary card generator. Content flows through this hierarchy:

### Content Hierarchy

1. **Gold Cards (Primary):** High-quality curated examples in `gold_cards.json`
   - Used as examples in LLM prompts
   - Serve as emergency fallbacks
   - Quality-scored (1-10) for ranking

2. **LLM Generation:** On-device language models generate unique cards
   - Uses gold examples to guide quality
   - Game-specific prompts ensure format compliance
   - 3 retry attempts with quality validation

3. **Template System (Fallback):** Blueprints + Lexicons
   - Used when LLM is unavailable
   - Deterministic slot-filling approach
   - Still useful for guaranteed variety

### Recommended Authoring Focus

**Primary:** Add high-quality cards to `gold_cards.json`
- These guide LLM generation
- Quality score determines prompt inclusion

**Secondary:** Maintain blueprints for fallback reliability
- Ensures offline mode works
- Provides deterministic backup

---

## Gold Cards (Primary Content)

### File Location
`app/src/main/assets/gold_cards.json`

### Structure

```json
{
  "games": {
    "roast_consensus": {
      "cards": [
        {
          "text": "Most likely to screenshot your story and send it back because they live-tweet their own life",
          "quality_score": 9,
          "spice": 2,
          "optionA": null,
          "optionB": null
        }
      ]
    },
    "poison_pitch": {
      "cards": [
        {
          "text": "Would you rather have perfect memory or never need sleep?",
          "quality_score": 8,
          "spice": 1,
          "optionA": "perfect memory",
          "optionB": "never need sleep"
        }
      ]
    }
  }
}
```

### Field Definitions

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `text` | string | ✅ | The card text displayed to players |
| `quality_score` | number | ✅ | Quality rating 1-10 (higher = used first in prompts) |
| `spice` | number | ✅ | Content rating: 1=PG, 2=PG-13, 3=edgy |
| `optionA`, `optionB` | string | ⚠️  | Required for AB games (POISON_PITCH, MAJORITY_REPORT) |
| `word` | string | ⚠️  | Required for TABOO_TIMER |
| `forbidden` | array | ⚠️  | Required for TABOO_TIMER |
| `items` | array | ⚠️  | Required for ODD_ONE_OUT (3 items) |
| `words` | array | ⚠️  | Required for ALIBI_DROP (secret words) |
| `product` | string | ⚠️  | Required for HYPE_OR_YIKE |
| `category`, `letter` | string | ⚠️  | Required for SCATTERBLAST |
| `tones` | array | ⚠️  | Required for TEXT_THREAD_TRAP (4 tones) |

### Quality Scoring Guidelines

| Score | Description | Use |
|-------|-------------|-----|
| 9-10 | Exceptional - always funny | Top examples in LLM prompts |
| 7-8 | High quality - reliably good | Good prompt examples |
| 5-6 | Average - acceptable | Fallback-only use |
| 1-4 | Below average | Consider removal |

---

## Blueprint V3 Schema (Fallback)

> **Note:** Blueprints are used as fallback when LLM generation fails. Focus new content on gold cards.

---

## Blueprint V3 Schema

### File Location
`app/src/main/assets/templates_v3/{game_family}.json`

### Structure

```json
[
  {
    "id": "unique_blueprint_id",
    "game": "GAME_ID", 
    "family": "family_name",
    "weight": 1.0,
    "spice_max": 2,
    "locality_max": 2,
    "blueprint": [
      { "type": "text", "value": "Static text here" },
      { 
        "type": "slot", 
        "name": "slot_name", 
        "slot_type": "lexicon_slot_type",
        "mods": ["upper", "title", "lower"]
      }
    ],
    "constraints": {
      "max_words": 24,
      "distinct_slots": true,
      "min_players": 0
    },
    "option_provider": {
      "type": "AB",
      "options": [
        { "from_slot": "slot_a" },
        { "from_slot": "slot_b" }
      ]
    }
  }
]
```

### Field Definitions

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `id` | string | ✅ | Unique identifier (use pattern: `{game}_{family}_blueprint_{n}`) |
| `game` | string | ✅ | Game ID constant (e.g., `ROAST_CONSENSUS`, `POISON_PITCH`) |
| `family` | string | ✅ | Family grouping for similar blueprints |
| `weight` | number | ✅ | Selection priority (0.0-1.0, higher = more likely) |
| `spice_max` | number | ✅ | Maximum spice allowed (0-3: 0=safe, 3=edgy) |
| `locality_max` | number | ✅ | Maximum cultural specificity (1-3: 1=universal, 3=regional) |
| `blueprint` | array | ✅ | Sequence of text segments and typed slots |
| `constraints` | object | ✅ | Generation constraints |
| `option_provider` | object | ⚠️  | Defines how game options are generated (required for AB games) |

### Blueprint Segments

**Text Segment:**
```json
{ "type": "text", "value": "Your sentence fragment here" }
```

**Slot Segment:**
```json
{
  "type": "slot",
  "name": "unique_slot_name",
  "slot_type": "lexicon_category",
  "mods": ["title", "upper", "lower"]
}
```

**Slot Modifiers:**
- `title`: Title Case Each Word
- `upper`: UPPERCASE
- `lower`: lowercase

### Constraints

```json
{
  "max_words": 24,           // Hard limit on total word count
  "distinct_slots": true      // Enforce all slot values are unique
}
```

### Option Providers

**Player Vote:**
```json
{
  "type": "PLAYER_VOTE"
}
```

**A/B Choice:**
```json
{
  "type": "AB",
  "options": [
    { "from_slot": "slot_name_a" },
    { "from_slot": "slot_name_b" }
  ]
}
```

For contrast-driven AB games (`POISON_PITCH`, `RED_FLAG_RALLY`), map `options` to different slot types (e.g., `perks_plus` vs `gross_problem`). The validator checks that `from_slot` names exist in the blueprint and encourages contrast.

---

## Lexicon V2 Schema

### File Location
`app/src/main/assets/lexicons_v2/{slot_type}.json`

### Structure

```json
{
  "slot_type": "category_name",
  "entries": [
    {
      "text": "entry text",
      "tags": ["tag1", "tag2"],
      "tone": "playful",
      "spice": 1,
      "locality": 1,
      "pluralizable": false,
      "needs_article": "a"
    }
  ]
}
```

### Field Definitions

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `slot_type` | string | ✅ | Must match lexicon filename without extension |
| `text` | string | ✅ | The actual word/phrase to insert |
| `tags` | array | ✅ | Semantic tags for filtering/pairing |
| `tone` | string | ✅ | Emotional flavor: `neutral`, `playful`, `dry`, `witty`, `wild`, `grim` |
| `spice` | number | ✅ | Content rating: 0=G, 1=PG, 2=PG-13, 3=R |
| `locality` | number | ✅ | Cultural specificity: 1=universal, 2=Western, 3=US-specific |
| `pluralizable` | boolean | ✅ | Can this be made plural? |
| `needs_article` | string | ✅ | Article handling: `"a"`, `"an"`, `"none"` |

### Article Handling Examples

```json
{ "text": "umbrella", "needs_article": "an" }     → "an umbrella"
{ "text": "cat", "needs_article": "a" }           → "a cat" or "an cat" (vowel-aware)
{ "text": "unlimited tacos", "needs_article": "none" } → "unlimited tacos"
```

---

## Content Guidelines

### ✅ DO

**For Blueprints:**
- Keep sentences under 25 words for readability
- Use clear, conversational phrasing
- Ensure distinct slots use complementary lexicon types
- Test with `distinct_slots: true` when A/B options need to differ
- Provide explicit option_provider for AB games

**For Lexicons:**
- Use lowercase for consistency (mods will handle casing)
- Be specific and vivid
- Vary tone across entries
- Check article correctness (`"a elephant"` ❌ vs `"an elephant"` ✅)
- Tag appropriately for pair scoring

**For Content Quality:**
- Spice 0-1: Default, broadest appeal
- Spice 2: Slightly edgy, still party-safe
- Spice 3: Use sparingly, risky content
- Locality 1: Universally understood
- Locality 2-3: May not translate across cultures

### ❌ DON'T

**Avoid:**
- Hate speech, slurs, harassment
- Extreme political/religious content
- Direct personal attacks
- Content that punches down
- Overly long phrases (>50 characters for lexicon entries)
- Duplicate entries within a lexicon
- Slots that reference placeholder syntax (`{name}` in text segments)

**Technical Don'ts:**
- Don't use special characters in IDs: `,` `"` `\` `{` `}`
- Don't exceed max_words in blueprint constraints
- Don't create circular dependencies in pairings
- Don't forget slot_type must match lexicon filename

---

## Examples

### Example 1: Roast Consensus Blueprint

```json
{
  "id": "roast_consensus_blueprint_5",
  "game": "ROAST_CONSENSUS",
  "family": "roast_core",
  "weight": 0.9,
  "spice_max": 2,
  "locality_max": 2,
  "blueprint": [
    { "type": "text", "value": "Who would " },
    { "type": "slot", "name": "action", "slot_type": "sketchy_action" },
    { "type": "text", "value": " and blame Mercury retrograde?" }
  ],
  "constraints": {
    "max_words": 18,
    "distinct_slots": false
  },
  "option_provider": {
    "type": "PLAYER_VOTE"
  }
}
```

**Why it works:**
- Clear roast structure: "Who would [action] and [punchline]?"
- Single slot keeps it simple
- Word count leaves room for longer actions
- Playful blame deflection adds humor

### Example 2: Poison Pitch (AB Game)

```json
{
  "id": "poison_pitch_blueprint_5",
  "game": "POISON_PITCH",
  "family": "pitch_core",
  "weight": 0.8,
  "spice_max": 2,
  "locality_max": 2,
  "blueprint": [
    { "type": "text", "value": "Choose: unlimited " },
    { "type": "slot", "name": "perk", "slot_type": "perks_plus" },
    { "type": "text", "value": " but you must " },
    { "type": "slot", "name": "problem", "slot_type": "gross_problem" },
    { "type": "text", "value": "." }
  ],
  "constraints": {
    "max_words": 26,
    "distinct_slots": true
  },
  "option_provider": {
    "type": "AB",
    "options": [
      { "from_slot": "perk" },
      { "from_slot": "problem" }
    ]
  }
}
```

**Why it works:**
- Clear A vs B structure
- `distinct_slots: true` ensures perk ≠ problem
- Options derived from slots for coherence
- Balanced "good vs bad" tension

### Example 3: Lexicon Entry (perks_plus)

```json
{
  "text": "perfect parking karma",
  "tags": ["perk", "convenience"],
  "tone": "playful",
  "spice": 1,
  "locality": 1,
  "pluralizable": false,
  "needs_article": "none"
}
```

**Why it works:**
- Specific, relatable perk
- Low spice for broad appeal
- Universal (locality: 1)
- Tags support pairing with gross_problem

---

## Validation & Testing

### Schema Validation

The [`AssetValidator`](../app/src/main/java/com/helldeck/content/validation/AssetValidator.kt:1) runs at boot and checks:

- ✅ Valid JSON syntax
- ✅ Required fields present
- ✅ Reasonable entry counts
- ✅ File accessibility

### Quality Checks

Cards pass coherence gates if:
- Word count: 5-32 words
- No placeholders `{` or `}`
- Repetition ratio ≤ 0.35
- Pair score ≥ 0.0 (if applicable)
- AB options are distinct (A ≠ B)

### Testing Your Content

**1. Use Card Lab:**
```
1. Open HELLDECK app
2. Navigate to Settings → Developer → Card Lab
3. Enable "Force V3" toggle
4. Select your game
5. Generate 50+ cards with different seeds
6. Check for issues in details view
```

**2. Run Card Audit:**
```bash
./gradlew :app:cardAudit -Pgame=YOUR_GAME -Pcount=200 -Pseed=12345
```

**Output:** 
- CSV: `app/build/reports/cardlab/audit_{game}_{seed}_{count}.csv`
- JSON: `app/build/reports/cardlab/audit_{game}_{seed}_{count}.json`
- HTML: `app/build/reports/cardlab/audit_{game}_{seed}_{count}.html`

**3. Check Metrics:**
- Pass rate should be ≥ 95%
- p95 generation time ≤ 12ms
- No placeholders in any output
- AB options always distinct

---

## Common Mistakes

### ❌ Mistake 1: Wrong Article

```json
// WRONG
{ "text": "umbrella", "needs_article": "a" }

// RIGHT
{ "text": "umbrella", "needs_article": "an" }
```

### ❌ Mistake 2: Missing Option Provider for AB Games

```json
// WRONG - AB game without options
{
  "game": "POISON_PITCH",
  "blueprint": [...]
  // Missing option_provider!
}

// RIGHT
{
  "game": "POISON_PITCH",
  "blueprint": [...],
  "option_provider": {
    "type": "AB",
    "options": [
      { "from_slot": "a" },
      { "from_slot": "b" }
    ]
  }
}
```

### ❌ Mistake 3: Distinct Slots Not Enforced

```json
// WRONG - AB slots can be identical
{
  "constraints": {
    "distinct_slots": false  // ❌ A and B could be the same!
  }
}

// RIGHT
{
  "constraints": {
    "distinct_slots": true  // ✅ Ensures A ≠ B
  }
}
```

### ❌ Mistake 4: Overly Long Phrases

```json
// WRONG
{ 
  "text": "an extremely comprehensive and detailed explanation of quantum physics for beginners",
  "needs_article": "an"
}

// RIGHT
{
  "text": "quantum physics for beginners",
  "needs_article": "none"
}
```

### ❌ Mistake 5: Inconsistent Spice Ratings

```json
// WRONG - Harsh content rated too low
{
  "text": "publicly shame someone",
  "spice": 1  // ❌ This is at least spice 3!
}

// RIGHT
{
  "text": "playfully roast someone",
  "spice": 1  // ✅ Light, party-appropriate
}
```

---

## Spice & Locality Reference

### Spice Levels

| Level | Description | Examples |
|-------|-------------|----------|
| 0 | G-rated, all audiences | "pizza", "puppies", "ice cream" |
| 1 | PG, playful teasing | "embarrassing story", "bad haircut" |
| 2 | PG-13, edgier humor | "awkward ex encounter", "cringe moment" |
| 3 | R-rated, use sparingly | Reserved for adult-focused sessions |

### Locality Levels

| Level | Description | Examples |
|-------|-------------|----------|
| 1 | Universal | "pizza", "umbrella", "sunrise" |
| 2 | Western/Internet culture | "TikTok", "Netflix", "brunch RSVPs" |
| 3 | US/Regional specific | "Taco Bell", "DMV wait", "jury duty" |

---

## Quick Reference: Slot Types

### Common Slot Types

- `perks_plus`: Positive traits/benefits
- `gross_problem`: Unpleasant scenarios (for would-you-rather)
- `red_flag_issue`: Dating/relationship quirks
- `meme_item`: Internet memes
- `product_item`: Absurd product ideas
- `categories`: Scattergories-style categories
- `letters`: A-Z for word games
- `secret_word`: Words for Taboo/Alibi games
- `taboo_forbidden`: Forbidden words for Taboo
- `sketchy_action`: Questionable behaviors (roast prompts)
- `social_reason`: Social media/relationship reasons
- `chaotic_plan`: Wild schemes
- `reply_tone`: Text message vibes
- `audience_type`: Who you're trying to impress

---

## Workflow: Adding New Content

### 1. Identify Need
- Run audit: `./gradlew :app:cardAudit -Pgame=TARGET_GAME -Pcount=100`
- Check which blueprints have low variety or high fail rates

### 2. Author Content
- Add blueprint to appropriate `templates_v3/{game}.json`
- Expand lexicons used by blueprint slots
- Keep spice/locality appropriate

### 3. Validate Locally
```bash
# Test compilation
./gradlew :app:assembleDebug

# Run property tests
./gradlew testDebugUnitTest --tests GeneratorV3InvariantsTest

# Generate audit report
./gradlew :app:cardAudit -Pgame=YOUR_GAME -Pcount=200
```

### 4. Review Output
- Open HTML report: `app/build/reports/cardlab/audit_*.html`
- Check pass rate ≥ 95%
- Review "Worst 20 Samples" for quality issues
- Verify blueprint stats show even distribution

### 5. Iterate
- If pass rate < 95%: adjust constraints, refine slot types
- If variety low: add more blueprints with different structures
- If pair scores negative: update `model/pairings.json` compatibility

---

## Best Practices

### For Roast Consensus
- Target spice: 1-2
- Pattern: "Who would [action] because [reason]?"
- Ensure action is lighthearted, not mean

### For Poison Pitch
- Balance perks vs problems
- Use `distinct_slots: true` always
- Spice: 1-2 (can go to 3 for adult groups)

### For Red Flag Rally
- Perk + red_flag pairing
- Keep relatable (dating/relationship context)
- Spice: 1-2 typically

---

## Rules & Tuning (for Authors)

You can tune generator behavior without code changes via `app/src/main/assets/model/rules.yaml`:

- `soft_repetition_margin`: fraction below `max_repetition_ratio` that counts as high repetition (soft gating feature).
- `near_word_limit_margin`: fraction of `max_word_count` considered near-limit (soft gating feature).
- `attempts_by_game`: per‑game blueprint attempt budget (e.g., more tries for `POISON_PITCH`).
- `tone_preference_low` / `tone_preference_high`: preferred tone ordering used by the tone‑aware selector for low/high spice.

## Local Tools

- Compare audit CSVs to baselines: `python tools/card_audit_diff.py`
- Lint lexicons for punctuation/emoji/article issues: `python tools/lexicon_lint.py`
- Generate baselines for many games: `bash tools/gen_audit_baselines.sh`

### For Majority Report
- Neutral phrasing for balanced vote
- Use meme_item or perks for options
- No leading language

### For Text Thread Trap
- Simple prompt about reply vibes
- Use reply_tone slot type
- Spice: 0-1 (safe for all)

### For Odd One Out
- Three distinct items required
- Use `distinct_slots: true`
- Items should have subtle connections

---

## Pairing Compatibility

Edit `app/src/main/assets/model/pairings.json` to define slot type compatibility:

```json
{
  "slot_type": "perks_plus",
  "compatibility": {
    "gross_problem": 0.45,      // Strong pairing (perk vs gross)
    "red_flag_issue": 0.35,     // Good pairing (perk vs red flag)
    "perks_plus": -0.25         // Avoid pairing with itself
  }
}
```

**Scoring:**
- Positive values: encourage pairing
- Negative values: discourage pairing
- Range: typically -0.5 to +0.5

---

## Quality Checklist

Before submitting new content:

- [ ] All blueprint IDs are unique
- [ ] max_words is reasonable (≤ 32)
- [ ] Spice ratings are honest (don't under-rate edgy content)
- [ ] Locality reflects actual cultural dependency  
- [ ] Articles are correct (a/an/none)
- [ ] AB games have distinct_slots: true
- [ ] Option providers match game type
- [ ] No typos or grammar errors in lexicon entries
- [ ] Ran local audit with pass rate ≥ 95%
- [ ] Tested in Card Lab with Force V3 enabled

---

## Support & Questions

**Validation Errors?**  
Check logs for specific asset validation failures. The system will gracefully fall back to gold mode on critical errors.

**Low Pass Rates?**  
Review "Top Failure Reasons" in HTML audit report. Common fixes:
- Adjust max_words constraint
- Enable distinct_slots for AB games
- Refine lexicon pairing compatibility
- Check for overly restrictive spice/locality caps

**Need More Variety?**  
Add 2-3 blueprints per game family with different:
- Sentence structures
- Slot type combinations
- Constraint profiles

---

**Happy Authoring! 🎮**

For technical questions, see [`ARCHITECTURE.md`](ARCHITECTURE.md) or run the Card Audit tool for data-driven insights.
