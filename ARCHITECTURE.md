# HELLDECK Architecture Documentation

## System Overview

HELLDECK is a party game Android application that generates dynamic, context-aware cards using an LLM-powered content generation engine. The system uses on-device language models to create unique, high-quality cards with gold standard examples as guidance and fallback.

## Core Components

### 1. Content Generation Pipeline

```
User Request → LLMCardGeneratorV2 → Quality Prompting → LLM Generation →
Quality Validation → [Fallback: Gold Cards → Templates] → Card Output
```

#### 1.1 LLMCardGeneratorV2 (Primary Generator)
- **Location**: `app/src/main/java/com/helldeck/content/generator/LLMCardGeneratorV2.kt`
- **Purpose**: Quality-first LLM card generation using gold standard examples
- **Key Features**:
  - On-device LLM generation (TinyLlama/Qwen)
  - Example-driven quality prompts
  - 3-retry strategy with 2.5s timeout per attempt
  - Game-specific prompt templates
  - Quality validation and scoring
  - Graceful fallback chain

#### 1.2 Gold Cards System
- **Location**: `app/src/main/assets/gold_cards.json`
- **Purpose**: High-quality curated examples for prompts and fallbacks
- **Loader**: `app/src/main/java/com/helldeck/content/generator/GoldCardsLoader.kt`
- **Structure**:
  ```json
  {
    "games": {
      "roast_consensus": {
        "cards": [
          {
            "text": "Most likely to...",
            "quality_score": 9,
            "spice": 2
          }
        ]
      }
    }
  }
  ```

#### 1.3 CardGeneratorV3 (Legacy Fallback)
- **Location**: `app/src/main/java/com/helldeck/content/generator/CardGeneratorV3.kt`
- **Purpose**: Template-based fallback when LLM is unavailable
- **Note**: Only used when LLM fails and no gold cards match

### 2. Lexicon System

#### 2.1 Lexicon Structure
- **Location**: `app/src/main/assets/lexicons_v2/`
- **Format**: JSON with metadata
- **Entry Structure**:
  ```json
  {
    "text": "entry text",
    "tags": ["category", "type", "mood"],
    "tone": "playful|wild|cringe|dry|witty",
    "spice": 1-3,
    "locality": 1-3,
    "pluralizable": true|false,
    "needs_article": "none|a|an|the"
  }
  ```

#### 2.2 Available Lexicons (28 total)
- **Core Lexicons**: internet_slang, meme_references, social_reason
- **Behavioral**: selfish_behaviors, vices_and_indulgences, sketchy_action
- **Dating**: dating_green_flags, red_flag_traits, red_flag_issue, relationship_fails
- **Social**: awkward_contexts, audience_type, social_disasters
- **Content**: meme_item, product_item, taboo_forbidden, taboo_topics
- **Physical**: bodily_functions, gross_problem
- **Game Mechanics**: would_you_rather_costs, chaotic_plan, evidence_reason, receipts
- **Utility**: categories, letters, secret_word, perks_plus, reply_tone, sexual_innuendo

### 3. Validation System

#### 3.1 SemanticValidator
- **Location**: `app/src/main/java/com/helldeck/content/validation/SemanticValidator.kt`
- **Purpose**: Prevents nonsensical slot combinations
- **Configuration**: `app/src/main/assets/model/semantic_compatibility.json`
- **Features**:
  - Domain-based compatibility checking
  - Forbidden pair detection
  - Coherence scoring (0.0-1.0)
  - Context-aware validation

#### 3.2 Semantic Compatibility Matrix
```json
{
  "domains": {
    "social": ["social_reason", "audience_type", "awkward_contexts"],
    "bodily": ["bodily_functions", "gross_problem"],
    "sexual": ["sexual_innuendo"],
    "wholesome": ["dating_green_flags", "perks_plus"],
    "taboo": ["taboo_forbidden", "taboo_topics"],
    "awkward": ["awkward_contexts", "social_disasters"]
  },
  "forbidden_pairs": [
    ["bodily_functions", "dating_green_flags"],
    ["gross_problem", "wholesome"],
    ["sexual_innuendo", "wholesome"]
  ]
}
```

### 4. Humor Scoring System

#### 4.1 HumorScorer
- **Location**: `app/src/main/java/com/helldeck/content/scoring/HumorScorer.kt`
- **Purpose**: Evaluates comedic quality of generated cards
- **Metrics** (8 total):
  1. **Absurdity** (20%): Measures unexpectedness and incongruity
  2. **Shock Value** (15%): Evaluates spice level with decay
  3. **Benign Violation** (20%): Checks for playful framing of edgy content
  4. **Specificity** (15%): Rewards concrete details over vague descriptions
  5. **Surprise** (10%): Measures novelty and unexpectedness
  6. **Timing** (10%): Validates punchline position (last 30% of text)
  7. **Relatability** (5%): Checks for common experiences
  8. **Wordplay** (5%): Detects linguistic creativity

#### 4.2 Scoring Thresholds
- **Minimum Humor Score**: 0.55 (increased from 0.40)
- **Minimum Coherence Score**: 0.25 (increased from 0.10)
- **Maximum Word Count**: 20 (reduced from 32)
- **Maximum Repetition Ratio**: 0.25 (reduced from 0.35)

### 5. Quality Control

#### 5.1 Content Filtering
- **Banned Words**: `app/src/main/assets/model/banned.json`
- **Word Boundary Matching**: Prevents false positives
- **Categories**: slurs, protected_classes, minors, violence

#### 5.2 Quality Profiles
- **Location**: `app/src/main/java/com/helldeck/content/model/GameQualityProfiles.kt`
- **Per-Game Thresholds**: Different games have different quality requirements
- **Adaptive Filtering**: Adjusts based on game type and spice level

### 6. Data Flow (LLM V2 Pipeline)

```
1. User initiates game
2. LLMCardGeneratorV2 receives request
3. LLM Generation Phase:
   - Check if LocalLLM is ready
   - Build quality-focused prompt with gold examples
   - Set temperature based on spice level (0.5-0.9)
   - Up to 3 generation attempts, 2.5s timeout each
4. Quality Validation:
   - Parse JSON response from LLM
   - Check minimum quality score (≥0.6)
   - Filter cliché phrases per game type
   - Validate minimum text length (≥15 chars)
5. Fallback Chain (if LLM fails):
   a. Gold Cards: Curated high-quality cards from gold_cards.json
   b. Templates: Legacy CardGeneratorV3 slot-filling system
6. Result Assembly:
   - Build FilledCard with metadata (usedLLM, qualityScore)
   - Parse game-specific options from JSON
   - Determine timer and interaction type
7. Output:
   - Card delivered to game UI
   - Generation method tracked for analytics
```

## Performance Considerations

### Caching Strategy
- Lexicons loaded once at startup
- Templates cached in memory
- Semantic compatibility matrix pre-computed
- Blueprint priors updated periodically

### Generation Speed
- Target: <100ms per card
- Parallel candidate generation
- Early rejection of invalid combinations
- Lazy evaluation where possible

### Memory Management
- Lexicon entries: ~2MB total
- Template cache: ~500KB
- Semantic matrix: ~100KB
- Total footprint: <5MB

## Extension Points

### Adding New Lexicons
1. Create JSON file in `lexicons_v2/`
2. Add slot type mapping in `LexiconRepository.kt`
3. Update semantic compatibility matrix
4. Add to relevant blueprints

### Adding New Games
1. Create template file in `templates_v3/`
2. Define game type enum
3. Add quality profile
4. Create UI components
5. Register in game selection

### Modifying Validation
1. Update `SemanticValidator.kt` for new rules
2. Modify `semantic_compatibility.json` for new domains
3. Adjust thresholds in `rules.yaml`
4. Test with existing content

## Testing Strategy

### Unit Tests
- Lexicon loading and parsing
- Semantic validation logic
- Humor scoring calculations
- Template rendering

### Integration Tests
- End-to-end card generation
- Quality threshold enforcement
- Multi-slot coherence
- Performance benchmarks

### Content Tests
- Lexicon coverage
- Blueprint variety
- Semantic compatibility
- Humor score distribution

## Deployment

### Build Process
1. Lexicon validation
2. Template compilation
3. Asset packaging
4. APK generation

### Release Checklist
- [ ] All lexicons validated
- [ ] Templates tested
- [ ] Quality thresholds verified
- [ ] Performance benchmarks met
- [ ] Content reviewed
- [ ] Analytics configured

## Monitoring

### Key Metrics
- Card generation success rate
- Average humor score
- Semantic coherence distribution
- Player engagement (laughs, skips)
- Repetition frequency
- Performance (generation time)

### Analytics Events
- Card generated
- Card displayed
- Player reaction (laugh, skip, report)
- Game completed
- Session duration

## Future Enhancements

### Planned Features
1. **Session Learning**: Adapt to player preferences over time
2. **Dynamic Difficulty**: Adjust spice based on group comfort
3. **Trending Topics**: Integrate current events and memes
4. **Seasonal Content**: Holiday and event-specific cards
5. **Player Profiles**: Personalized content based on history
6. **A/B Testing**: Experiment with different generation strategies

### Technical Debt
1. Refactor blueprint system for better extensibility
2. Implement proper dependency injection
3. Add comprehensive error handling
4. Improve test coverage (target: 80%+)
5. Optimize lexicon loading
6. Add telemetry for production debugging

## Contributing

### Code Style
- Follow Kotlin conventions
- Use meaningful variable names
- Document complex logic
- Write tests for new features

### Content Guidelines
- Keep entries concise (3-8 words)
- Maintain consistent tone within lexicons
- Avoid offensive content
- Test semantic compatibility
- Verify humor scoring

### Review Process
1. Code review by team
2. Content review by moderators
3. QA testing
4. Performance validation
5. Staged rollout

---

**Last Updated**: December 2024
**Version**: 3.0
**Maintainers**: NinjaTech AI Team
