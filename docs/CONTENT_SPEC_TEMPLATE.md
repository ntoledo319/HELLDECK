# HELLDECK Game Content Specification Template

> **Purpose**: This document serves as the source of truth for game rules, content standards, and gold standard examples. Use this template to define or update content for all games.

---

## Universal Content Standards

### Spice Level Definitions
- **Level 1 (PG-13)**: Family-friendly, wholesome, safe for all audiences
- **Level 2 (Fun & Playful)**: Lightly edgy, adult humor but not offensive
- **Level 3 (Provocative)**: Edgy, provocative, adult themes (no slurs)
- **Level 4 (Wild)**: Unhinged, chaotic, maximum edge (stay playful, not cruel)
- **Level 5 (Reserved)**: Explicit sexual content (use sparingly)

### Quality Criteria (All Games)
✅ **Must Have**:
- Specificity over generic phrases
- Unexpected twists or absurdity
- 10-25 words optimal (varies by game)
- Playable with 3-8 people
- Clear win/lose conditions

❌ **Avoid**:
- Clichés and overused phrases
- Targeting protected classes
- Physical appearance mockery
- Genuinely cruel content
- Confusing or unclear cards

---

## Game Format Template

Copy this section for each game and fill in the details:

---

### Game [NUMBER]: [GAME NAME]

#### Game Mechanics
- **Game ID**: `GAME_ID_CONSTANT`
- **Interaction Type**: [VOTE_PLAYER | A_B_CHOICE | TRUE_FALSE | etc.]
- **Timer**: [seconds]
- **Min/Max Players**: [range]
- **Format**: [Describe card structure, e.g., "Most likely to [ACTION] because [REASON]"]

#### Gameplay Flow
1. Card is displayed to all players
2. [Describe how players interact]
3. [Describe how winner is determined]
4. Points awarded: [scoring system]

#### Content Guidelines

**Structure**:
```
[Describe exact format, including blanks, options, etc.]
```

**Key Elements**:
- Element 1: [Description]
- Element 2: [Description]
- Element 3: [Description]

**Tone**: [playful | witty | absurd | provocative | etc.]

**Length**: [word count range]

#### LLM Prompt Strategy

**System Prompt Add-ons**:
```
[Any game-specific instructions for the LLM]
```

**User Prompt Structure**:
```
Generate a [game name] card:

FORMAT:
{
  "text": "...",
  [additional fields if needed]
}

QUALITY CRITERIA:
✓ [Criterion 1]
✓ [Criterion 2]
✓ [Criterion 3]

EXAMPLES (emulate these):
[Will be auto-populated from gold cards below]

❌ AVOID:
- [Common mistake 1]
- [Common mistake 2]
- [Common mistake 3]

OUTPUT: Generate ONE unique card in valid JSON format.
```

#### Gold Standard Cards (Quality 8-10)

Provide 20-30 high-quality examples with annotations:

```json
[
  {
    "text": "[Card text exactly as it appears in game]",
    "quality_score": 10,
    "spice": 3,
    "optionA": "[if applicable]",
    "optionB": "[if applicable]",
    "word": "[if applicable]",
    "forbidden": ["word1", "word2", "word3"],
    "items": ["item1", "item2", "item3"],
    "words": ["word1", "word2", "word3"],
    "product": "[if applicable]",
    "category": "[if applicable]",
    "letter": "[if applicable]",
    "tones": ["tone1", "tone2", "tone3", "tone4"],
    
    "_why_it_works": "[Explain what makes this card great]",
    "_tags": ["tag1", "tag2", "tag3"]
  }
]
```

#### Common Mistakes & How to Fix Them

| ❌ Bad Example | ✅ Good Example | Why It's Better |
|---------------|-----------------|-----------------|
| [Generic card] | [Specific card] | [Explanation] |
| [Cliché card] | [Fresh card] | [Explanation] |
| [Unclear card] | [Clear card] | [Explanation] |

#### Testing Checklist
- [ ] Card makes sense when read aloud
- [ ] All players can participate equally
- [ ] Win condition is clear
- [ ] Card generates laughs or conversation
- [ ] No offensive content
- [ ] Spice level is appropriate
- [ ] Card hasn't been seen before (uniqueness)

---

## Completed Game Specifications

---

### Game 1: ROAST CONSENSUS

#### Game Mechanics
- **Game ID**: `ROAST_CONS`
- **Interaction Type**: VOTE_PLAYER
- **Timer**: 15 seconds
- **Min/Max Players**: 3-8
- **Format**: "Most likely to [SPECIFIC ACTION] because [ABSURD REASON]"

#### Gameplay Flow
1. Card displays a "Most likely to..." scenario
2. All players vote on who best fits the description
3. Most votes wins the round
4. Points awarded: 1 point to voted player

#### Content Guidelines

**Structure**:
```
Most likely to [SPECIFIC OBSERVABLE ACTION/BEHAVIOR] because [UNEXPECTED BUT BELIEVABLE REASON]
```

**Key Elements**:
- **Specificity**: Avoid generic actions like "be late" or "eat pizza"
- **Absurdity**: Exaggerated but relatable scenarios
- **Visual**: Create a mental image players can laugh at
- **Playful**: Roast the behavior, not the person
- **Unexpected**: Surprise with the "because" clause

**Tone**: Playful, absurd, relatable

**Length**: 15-25 words

#### LLM Prompt Strategy

**Emphasis**:
- Demand specificity in prompts
- Include 5 gold examples with quality scores
- Explicitly ban clichés like "be late", "eat all the pizza"
- Show negative examples of what to avoid

#### Gold Standard Cards

See `gold_cards.json` → `roast_consensus` section for 20 curated examples with quality scores 7-10.

**Top 3 Examples**:
1. "Most likely to fail a vibe check from a house plant because even the ficus looks disappointed" (Quality: 10, Spice: 3)
2. "Most likely to get into a philosophical debate with a Roomba because they think it's judging their life choices" (Quality: 9, Spice: 2)
3. "Most likely to start a cult accidentally because they gave unsolicited advice at Whole Foods" (Quality: 9, Spice: 3)

#### Common Mistakes & How to Fix Them

| ❌ Bad Example | ✅ Good Example | Why It's Better |
|---------------|-----------------|-----------------|
| "Most likely to be late" | "Most likely to argue with a GPS and still get lost because 'I know a shortcut'" | Specific, visual, includes absurd reason |
| "Most likely to eat all the pizza" | "Most likely to become emotionally dependent on their phone's autocorrect because it's the only one who gets them" | Fresh concept, specific scenario, relatable |
| "Most likely to be annoying" | "Most likely to schedule a meeting just to cancel it because that's the only power they have" | Concrete behavior, satirical, specific |

---

### Game 2: POISON PITCH

#### Game Mechanics
- **Game ID**: `POISON_PITCH`
- **Interaction Type**: A_B_CHOICE
- **Timer**: 15 seconds
- **Min/Max Players**: 3-8
- **Format**: "Would you rather [OPTION A] OR [OPTION B]?"

#### Gameplay Flow
1. Card presents two equally terrible options
2. Players must choose one option
3. Majority wins OR minority defends their choice
4. Points awarded based on game mode

#### Content Guidelines

**Structure**:
```
Would you rather [SPECIFIC BAD THING A] OR [SPECIFIC BAD THING B]?
```

**Key Elements**:
- **Equal Difficulty**: Both options must be equally bad/awkward
- **Specificity**: Vivid, concrete scenarios (not vague)
- **Genuine Dilemma**: No obvious "better" choice
- **Visual**: Easy to imagine both outcomes
- **Creative**: Unexpected pairings

**Tone**: Dilemma-inducing, absurd, thought-provoking

**Length**: 15-30 words total (both options combined)

#### Gold Standard Cards

See `gold_cards.json` → `poison_pitch` section for 20 examples.

**Top 3 Examples**:
1. "Would you rather have your browser history made public OR your bank statement made public?" (Quality: 10, Spice: 4)
2. "Would you rather have hiccups every time you lie OR sneeze every time someone says your name?" (Quality: 10, Spice: 2)
3. "Would you rather fight one horse-sized duck OR explain cryptocurrency to your grandma every single day for a year?" (Quality: 9, Spice: 2)

---

### Game 3-14: [TO BE FILLED]

*Use the template above to document the remaining 12 games:*
- Game 3: FILL_IN_FINISHER
- Game 4: RED_FLAG_RALLY
- Game 5: HOTSEAT_IMPOSTER
- Game 6: TEXT_THREAD_TRAP
- Game 7: TABOO_TIMER
- Game 8: ODD_ONE_OUT
- Game 9: TITLE_FIGHT
- Game 10: ALIBI_DROP
- Game 11: HYPE_OR_YIKE
- Game 12: SCATTERBLAST
- Game 13: MAJORITY_REPORT
- Game 14: CONFESS_OR_CAP

---

## LLM Reliability Best Practices

### What Makes LLM Generation Fail

1. **Vague prompts** → Solution: Add 5+ concrete examples
2. **No constraints** → Solution: Specify exact JSON format
3. **Generic requests** → Solution: Demand specificity in system prompt
4. **Insufficient examples** → Solution: Show gold standard cards inline
5. **No negative examples** → Solution: Explicitly list what to avoid

### Prompt Enhancement Checklist

- [ ] System prompt includes game-specific rules
- [ ] User prompt shows 5+ gold examples
- [ ] Format is explicitly defined with JSON structure
- [ ] Negative examples (what NOT to do) are listed
- [ ] Spice level is clearly defined
- [ ] Quality criteria are measurable
- [ ] Output format is unambiguous (JSON only)

### Retry Strategy

1. **Attempt 1**: Temperature 0.7, standard prompt
2. **Attempt 2**: Temperature 0.8, add "Be MORE creative"
3. **Attempt 3**: Temperature 0.9, add "COMPLETELY different approach"
4. **Fallback**: Use gold cards from curated set

---

## Quality Scoring Rubric

### Automatic Accept (9-10)
- Unexpected twist that surprises
- Highly specific details (not generic)
- Perfect tone for the game
- Genuinely funny or thought-provoking
- Novel concept (not seen before)
- Structurally perfect

### Conditional Accept (7-8)
- Solid execution with minor issues
- One or two cliché elements present
- "Good but not great"
- Playable and functional
- Minor structural issues

### Automatic Reject (<7)
- Generic or boring content
- Offensive or cruel
- Overused cliché
- Too similar to existing cards
- Confusing or unclear
- Wrong format or structure
- Contains banned words

---

## Gold Card Curation Process

### Adding New Gold Cards

1. **Playtest**: Test with real players - did they laugh/engage?
2. **Rate Honestly**: Use 1-10 scale based on rubric above
3. **Tag Appropriately**: Assign correct spice level
4. **Document**: Explain why it works in `_why_it_works` field
5. **Diversify**: Ensure variety across themes and patterns

### Target Distribution

- **20-30 cards per game** (280-420 total for 14 games)
- **Quality distribution**: 30% are 9-10, 50% are 8, 20% are 7
- **Spice distribution**: 20% level 1, 40% level 2, 30% level 3, 10% level 4+

### Maintenance Schedule

- **Monthly**: Review player feedback, retire low-performing cards
- **Quarterly**: Add 5-10 new cards per game based on trends
- **Annually**: Major refresh, update 25% of gold cards

---

## Content Update Workflow

### Proposing New Content

1. Copy this template
2. Fill in game specifications
3. Add 20+ gold standard cards with quality scores
4. Test with players
5. Submit for review
6. Integrate into `gold_cards.json`

### Review Criteria

- [ ] All fields completed
- [ ] Minimum 20 gold cards per game
- [ ] Quality scores assigned honestly
- [ ] Follows content guidelines
- [ ] Tested with real players
- [ ] No offensive content
- [ ] Fits existing game mechanics

---

## Appendix: JSON Schema Reference

### Card Object Structure

```json
{
  "text": "string (required) - Main card text",
  "quality_score": "integer 1-10 (required)",
  "spice": "integer 1-5 (required)",
  
  // Optional fields (game-specific):
  "optionA": "string - First option for A/B choices",
  "optionB": "string - Second option for A/B choices",
  "word": "string - Word to guess (Taboo)",
  "forbidden": ["array", "of", "strings"] - Forbidden words (Taboo)",
  "items": ["array", "of", "strings"] - Items for Odd One Out",
  "words": ["array", "of", "strings"] - Words for Alibi Drop",
  "product": "string - Product for Hype or Yike",
  "category": "string - Category for Scatterblast",
  "letter": "string - Letter for Scatterblast",
  "tones": ["array", "of", "strings"] - Tones for Text Trap",
  
  // Metadata (optional, for internal use):
  "_why_it_works": "string - Explanation of quality",
  "_tags": ["array", "of", "strings"] - Categorization tags",
  "_created_date": "string - ISO date",
  "_author": "string - Content creator"
}
```

---

**Last Updated**: [DATE]  
**Version**: [VERSION]  
**Maintainer**: [NAME]
