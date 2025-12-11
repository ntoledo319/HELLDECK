# HELLDECK Content Creation Guidelines

## Overview

This guide provides comprehensive instructions for creating, expanding, and maintaining content for HELLDECK. Follow these guidelines to ensure consistency, quality, and comedic effectiveness.

## Lexicon Creation

### Entry Structure

Every lexicon entry must include:

```json
{
  "text": "the actual content",
  "tags": ["category", "subcategory", "mood"],
  "tone": "playful|wild|cringe|dry|witty",
  "spice": 1-3,
  "locality": 1-3,
  "pluralizable": true|false,
  "needs_article": "none|a|an|the"
}
```

### Field Definitions

#### text
- **Purpose**: The actual content that appears in cards
- **Length**: 3-8 words (optimal), max 12 words
- **Style**: Conversational, modern, relatable
- **Examples**:
  - ✅ "chronically online behavior"
  - ✅ "main character energy"
  - ❌ "the person who is always on the internet" (too long)
  - ❌ "online" (too short, not specific)

#### tags
- **Purpose**: Categorize entries for filtering and analytics
- **Count**: 3 tags recommended
- **Structure**: [primary_category, secondary_category, mood/context]
- **Examples**:
  - `["slang", "modern", "personality"]`
  - `["dating", "red-flag", "toxic"]`
  - `["social", "awkward", "public"]`

#### tone
- **playful**: Light-hearted, fun, not mean-spirited
- **wild**: Edgy, bold, pushes boundaries
- **cringe**: Awkward, embarrassing, relatable discomfort
- **dry**: Deadpan, subtle, understated humor
- **witty**: Clever, sharp, intelligent humor

#### spice
- **1 (Mild)**: Safe for all audiences, wholesome
  - Examples: "golden retriever energy", "always early"
- **2 (Medium)**: Slightly edgy, adult humor
  - Examples: "drunk texting exes", "ghosting people"
- **3 (Spicy)**: Edgy, controversial, adult-only
  - Examples: "emotional cheating", "gaslighting"

#### locality
- **1 (Universal)**: Understood globally
- **2 (Western)**: Common in Western cultures
- **3 (Niche)**: Specific to subcultures or regions

#### pluralizable
- **true**: Can add 's' for plural (e.g., "friend" → "friends")
- **false**: Cannot be pluralized (e.g., "main character energy")

#### needs_article
- **none**: No article needed ("having main character energy")
- **a**: Needs "a" ("having a meltdown")
- **an**: Needs "an" ("having an existential crisis")
- **the**: Needs "the" ("stealing the aux cord")

## Content Quality Standards

### DO: Create Specific, Vivid Content

✅ **Good Examples**:
- "caught in 4K" (specific, visual)
- "crop dusting a room" (specific action)
- "drunk texting the ex at 3am" (specific, relatable)
- "posting thirst traps for validation" (specific behavior)

❌ **Bad Examples**:
- "being bad" (too vague)
- "doing something wrong" (not specific)
- "acting weird" (not descriptive)
- "being annoying" (lacks detail)

### DO: Use Modern, Current Language

✅ **Good Examples**:
- "chronically online"
- "living rent free in their head"
- "understood the assignment"
- "ate and left no crumbs"

❌ **Bad Examples**:
- "using the internet too much"
- "being memorable"
- "doing well"
- "succeeding"

### DO: Create Relatable Scenarios

✅ **Good Examples**:
- "accidentally liking an old photo"
- "sending a screenshot to the person in it"
- "your autocorrect betraying you"
- "walking in on your roommate"

❌ **Bad Examples**:
- "making a mistake online"
- "having a phone problem"
- "experiencing awkwardness"
- "being in a bad situation"

### DON'T: Use Offensive Content

❌ **Avoid**:
- Slurs or hate speech
- Content targeting protected classes
- Content involving minors
- Graphic violence or gore
- Non-consensual sexual content

### DON'T: Create Redundant Entries

❌ **Avoid**:
- Near-duplicates: "texting ex" and "messaging ex"
- Synonyms without distinction: "ghosting" and "disappearing"
- Same concept, different words: "being late" and "showing up late"

## Lexicon-Specific Guidelines

### internet_slang.json
- **Focus**: Current internet culture, Gen Z language
- **Tone**: Mostly playful, some wild
- **Spice**: 1-2
- **Examples**: "no cap", "bussin", "it's giving", "slay"
- **Update Frequency**: Quarterly (trends change fast)

### meme_references.json
- **Focus**: Specific meme formats and references
- **Tone**: Playful, witty
- **Spice**: 1-2
- **Examples**: "the 'let him cook' friend", "certified 'down bad' moment"
- **Update Frequency**: Monthly (memes evolve quickly)

### dating_green_flags.json
- **Focus**: Positive relationship behaviors
- **Tone**: Playful, wholesome
- **Spice**: 1
- **Examples**: "remembers small details", "respects boundaries"
- **Avoid**: Anything toxic or manipulative

### red_flag_traits.json
- **Focus**: Warning signs in relationships
- **Tone**: Wild, dry
- **Spice**: 2-3
- **Examples**: "love bombing", "gaslighting", "future faking"
- **Avoid**: Normalizing abuse

### awkward_contexts.json
- **Focus**: Universally relatable awkward situations
- **Tone**: Cringe, playful
- **Spice**: 1-2
- **Examples**: "running into your ex at the gym", "accidentally unmuting on Zoom"
- **Avoid**: Situations that are traumatic rather than awkward

### bodily_functions.json
- **Focus**: Physical/biological humor
- **Tone**: Wild, cringe
- **Spice**: 2-3
- **Examples**: "crop dusting a room", "audible stomach gurgling"
- **Avoid**: Graphic medical conditions

### sexual_innuendo.json
- **Focus**: Subtle to obvious sexual references
- **Tone**: Playful, wild
- **Spice**: 2-3
- **Examples**: "Netflix and chill", "come up for coffee"
- **Avoid**: Explicit sexual content

### vices_and_indulgences.json
- **Focus**: Bad habits and guilty pleasures
- **Tone**: Playful, wild
- **Spice**: 1-2
- **Examples**: "doom scrolling at 3am", "revenge bedtime procrastination"
- **Avoid**: Glorifying dangerous addictions

## Blueprint Creation

### Structure Principles

1. **Punchline Last**: Place the funniest/most surprising element at the end
2. **Brevity**: Target 15-20 words maximum
3. **Clarity**: Easy to read aloud and understand immediately
4. **Variety**: Use 2-3 different slot types per blueprint

### Good Blueprint Examples

✅ **Effective Structure**:
```json
{
  "blueprint": [
    {"type": "text", "value": "Most likely to "},
    {"type": "slot", "name": "action", "slot_type": "sketchy_action"},
    {"type": "text", "value": " because "},
    {"type": "slot", "name": "reason", "slot_type": "social_reason"},
    {"type": "text", "value": "."}
  ]
}
```
- Clear structure
- "Because" clause adds context
- Punchline (reason) comes last
- Total: ~12-18 words

✅ **Another Good Example**:
```json
{
  "blueprint": [
    {"type": "text", "value": "Call out whoever would "},
    {"type": "slot", "name": "action", "slot_type": "chaotic_plan"},
    {"type": "text", "value": " just to impress "},
    {"type": "slot", "name": "audience", "slot_type": "audience_type"},
    {"type": "text", "value": "."}
  ]
}
```
- Specific scenario
- Motivation clause ("just to impress")
- Punchline (audience) last
- Total: ~15-20 words

### Bad Blueprint Examples

❌ **Too Long**:
```json
{
  "blueprint": [
    {"type": "text", "value": "Call out whoever's guilty of "},
    {"type": "slot", "name": "vice"},
    {"type": "text", "value": " while pretending they have standards and acting like they're better than everyone else"},
    {"type": "text", "value": "."}
  ]
}
```
- Too wordy (30+ words)
- Redundant phrases
- Loses comedic timing

❌ **Punchline Not Last**:
```json
{
  "blueprint": [
    {"type": "text", "value": "Because "},
    {"type": "slot", "name": "reason"},
    {"type": "text", "value": ", most likely to "},
    {"type": "slot", "name": "action"},
    {"type": "text", "value": "."}
  ]
}
```
- Punchline (reason) comes first
- Loses surprise element
- Awkward structure

❌ **Too Vague**:
```json
{
  "blueprint": [
    {"type": "text", "value": "Who would do "},
    {"type": "slot", "name": "action"},
    {"type": "text", "value": "?"}
  ]
}
```
- Too simple
- No context
- Not funny

## Testing Your Content

### Manual Testing Checklist

1. **Read Aloud**: Does it sound natural?
2. **Timing**: Is the punchline at the end?
3. **Length**: Is it under 20 words?
4. **Clarity**: Is it immediately understandable?
5. **Humor**: Does it make you laugh or smile?
6. **Appropriateness**: Is it within spice level guidelines?
7. **Uniqueness**: Is it different from existing entries?

### Semantic Compatibility Testing

Test your entries with different slot combinations:

```
Example: "Most likely to [action] because [reason]"

✅ Good Combinations:
- "ghost someone" + "they're chronically online"
- "drunk text their ex" + "they have no impulse control"
- "start drama" + "they're bored"

❌ Bad Combinations:
- "have good hygiene" + "they're gross" (contradictory)
- "be kind to animals" + "they're mean" (contradictory)
- "respect boundaries" + "they're toxic" (contradictory)
```

### Humor Score Estimation

Estimate how your content will score:

- **High Absurdity**: Unexpected combinations
- **Good Specificity**: Concrete details
- **Strong Timing**: Punchline last
- **Relatable**: Common experiences
- **Appropriate Spice**: Matches target audience

## Expansion Strategy

### When to Expand a Lexicon

Expand when:
- Repetition is noticed in gameplay
- New cultural trends emerge
- Player feedback requests more variety
- Lexicon has <50 entries

### How to Expand

1. **Analyze Existing**: Review current entries for patterns
2. **Identify Gaps**: Find missing subcategories
3. **Research Trends**: Check current internet culture
4. **Create Batch**: Write 20-30 entries at once
5. **Test Compatibility**: Verify semantic coherence
6. **Review Quality**: Ensure consistency
7. **Deploy Gradually**: A/B test new content

### Expansion Priorities

1. **High-Frequency Lexicons**: Used in many blueprints
   - internet_slang, meme_references, social_reason
2. **Low-Variety Lexicons**: <50 entries
   - Expand to 75+ entries
3. **Trending Topics**: Current events and memes
   - Update quarterly
4. **Player Requests**: Based on feedback
   - Address specific gaps

## Quality Assurance

### Pre-Deployment Checklist

- [ ] All entries have required fields
- [ ] Tone and spice are appropriate
- [ ] No offensive content
- [ ] No duplicates or near-duplicates
- [ ] Semantic compatibility verified
- [ ] Humor potential validated
- [ ] Length within guidelines
- [ ] Grammar and spelling correct
- [ ] Tags are accurate
- [ ] Articles are correct

### Post-Deployment Monitoring

Track:
- Usage frequency
- Player reactions (laughs, skips)
- Semantic coherence scores
- Humor scores
- Repetition rates
- Player feedback

### Iteration Process

1. **Collect Data**: Monitor for 2-4 weeks
2. **Analyze Performance**: Identify low-performing entries
3. **Gather Feedback**: Review player comments
4. **Plan Updates**: Prioritize improvements
5. **Test Changes**: A/B test modifications
6. **Deploy**: Gradual rollout
7. **Monitor**: Track impact

## Common Mistakes to Avoid

### Content Mistakes

1. **Too Generic**: "being bad" instead of "ghosting people"
2. **Too Long**: 15+ words when 5 would work
3. **Not Current**: Using outdated slang
4. **Offensive**: Crossing the line into harmful content
5. **Redundant**: Creating near-duplicates
6. **Vague**: Not specific enough to be funny
7. **Contradictory**: Entries that don't work together

### Technical Mistakes

1. **Wrong JSON Format**: Missing commas, brackets
2. **Incorrect Field Types**: String instead of number
3. **Missing Required Fields**: Forgetting tags or tone
4. **Wrong Article**: Using "a" when "an" is needed
5. **Incorrect Pluralization**: Marking non-pluralizable as true
6. **Wrong Spice Level**: Marking offensive content as spice 1

## Resources

### Inspiration Sources

- Twitter/X trending topics
- TikTok trends
- Reddit (r/meirl, r/me_irl, r/relationship_advice)
- Instagram meme accounts
- Urban Dictionary
- Know Your Meme

### Testing Tools

- Humor score calculator (internal tool)
- Semantic validator (internal tool)
- Blueprint tester (internal tool)
- A/B testing framework (internal tool)

### Style Guides

- AP Stylebook (for grammar)
- Urban Dictionary (for slang)
- Know Your Meme (for references)
- Internal tone guide (for consistency)

---

**Last Updated**: December 2024
**Version**: 1.0
**Maintainers**: NinjaTech AI Content Team