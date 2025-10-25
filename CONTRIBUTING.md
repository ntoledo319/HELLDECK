# Contributing to HELLDECK

Thank you for your interest in contributing to HELLDECK! This guide will help you get started with adding new games, templates, and lexicons to the project.

## Table of Contents

- [Getting Started](#getting-started)
- [Development Setup](#development-setup)
- [Project Structure](#project-structure)
- [Adding New Games](#adding-new-games)
- [Creating Templates](#creating-templates)
- [Adding Lexicons](#adding-lexicons)
- [Testing](#testing)
- [Code Style](#code-style)
- [Submitting Changes](#submitting-changes)
- [Reporting Issues](#reporting-issues)

## Getting Started

### Prerequisites

- Android Studio Arctic Fox or later
- Kotlin 1.8.0+
- Android SDK 33+
- Git
- Basic understanding of Jetpack Compose
- Familiarity with Material Design 3

### Development Setup

1. **Clone the repository**
   ```bash
   git clone https://github.com/your-org/HELLDECK.git
   cd HELLDECK
   ```

2. **Open in Android Studio**
   - Open the project in Android Studio
   - Wait for Gradle sync to complete
   - Ensure all dependencies are resolved

3. **Run the app**
   - Connect an Android device or start an emulator
   - Run the app to verify setup works

## Project Structure

```
app/src/main/java/com/helldeck/
├── analytics/          # Analytics and crash reporting
├── content/           # Game content and templates
│   ├── data/          # Data repositories
│   ├── engine/         # Game engine logic
│   ├── model/          # Data models
│   └── validation/     # Content validation
├── data/              # Database entities and DAOs
├── engine/             # Core game mechanics
├── llm/               # Local LLM integration
├── services/           # Background services
├── ui/                # User interface
│   ├── components/      # Reusable UI components
│   ├── scenes/          # Screen composables
│   └── theme/           # Theme and styling
└── utils/              # Utility classes
```

### Key Directories

- **`app/src/main/assets/`** - Game content files
  - `lexicons/` - Word lists for different categories
  - `templates/` - Game templates (V1 format)
  - `templates_v2/` - Game templates (V2 format)
  - `settings/` - Configuration files

- **`app/src/test/`** - Unit tests
- **`app/src/androidTest/`** - Integration tests

## Adding New Games

### 1. Define Game Metadata

Add your game to [`GamesRegistry.kt`](app/src/main/java/com/helldeck/engine/GamesRegistry.kt):

```kotlin
val YOUR_GAME = GameSpec(
    id = "your_game_id",
    name = "Your Game Name",
    description = "Brief description of your game",
    icon = "🎮", // Emoji icon
    interaction = Interaction.YOUR_INTERACTION_TYPE,
    minPlayers = 2,
    maxPlayers = 16,
    estimatedDuration = Duration.MEDIUM,
    spiceLevel = 2,
    tags = setOf("fun", "social", "creative")
)
```

### 2. Implement Game Logic

Create a new file in the appropriate scene directory or extend existing scenes:

```kotlin
@Composable
fun YourGameScene(
    vm: HelldeckVm,
    modifier: Modifier = Modifier
) {
    // Implement your game UI here
    // Use vm.currentCard for the filled template
    // Use vm.players for player list
    // Use vm.onPreChoice(), vm.onAvatarVote(), etc. for user input
}
```

### 3. Add Game to Navigation

Update the scene navigation in [`Scenes.kt`](app/src/main/java/com/helldeck/ui/Scenes.kt):

```kotlin
Scene.YOUR_GAME -> YourGameScene(vm)
```

### 4. Create Templates

See [Creating Templates](#creating-templates) for detailed instructions.

### 5. Add Tests

Create unit tests for your game logic:

```kotlin
@Test
fun `your game logic works correctly`() {
    // Test your game mechanics
}
```

## Creating Templates

### Template Format (V2)

Templates use the V2 format with structured slots:

```json
{
  "id": "unique_template_id",
  "game": "game_id",
  "family": "template_family",
  "text": "Template text with {slot} placeholders",
  "slots": [
    {
      "name": "slot_name",
      "type": "lexicon_category",
      "required": true,
      "spice": 1
    }
  ],
  "min_players": 2,
  "max_players": 8,
  "spice": 2,
  "weight": 1.0,
  "tags": ["fun", "social"],
  "metadata": {
    "author": "Your Name",
    "difficulty": "medium",
    "notes": "Additional context"
  }
}
```

### Slot Types

Available lexicon categories for slots:

- `players` - Player names
- `friends` - Friend names
- `places` - Location names
- `gross` - Gross/icky things
- `icks` - Pet peeves
- `memes` - Internet memes
- `red_flags` - Relationship red flags
- `social_disasters` - Social awkward situations
- `tiny_rewards` - Small rewards
- `perks` - Special abilities
- `categories` - General categories
- `forbidden` - Forbidden words (for Taboo-style games)
- `letters` - Letters (for word games)
- `inbound_texts` - Received messages
- `guilty_prompts` - Confession prompts
- `sketchy_actions` - Suspicious activities

### Template Guidelines

1. **Keep it concise** - Templates should be readable and quick to understand
2. **Use appropriate spice levels** - 1 = mild, 2 = moderate, 3 = spicy
3. **Test with different player counts** - Ensure templates work with 2-16 players
4. **Include variety** - Mix different interaction types and themes
5. **Follow content guidelines** - Keep it fun but appropriate

### Adding Templates

1. **Create JSON file** in `app/src/main/assets/templates_v2/`
2. **Follow naming convention** - Use descriptive names like `your_game_templates.json`
3. **Test templates** - Use the template engine to verify they work
4. **Update ContentRepository** - Ensure new templates are loaded

## Adding Lexicons

### Lexicon Format

Lexicons are JSON arrays of words/phrases:

```json
[
  "word1",
  "word2",
  "phrase with spaces",
  "another_entry"
]
```

### Lexicon Categories

Create lexicons in the appropriate category:

- `friends.json` - Friend names
- `places.json` - Location names
- `gross.json` - Gross/icky things
- `icks.json` - Pet peeves
- `memes.json` - Internet memes
- `red_flags.json` - Relationship red flags
- `social_disasters.json` - Social awkward situations
- `tiny_rewards.json` - Small rewards
- `perks.json` - Special abilities
- `categories.json` - General categories
- `forbidden.json` - Forbidden words
- `letters.json` - Letters for word games
- `inbound_texts.json` - Received messages
- `guilty_prompts.json` - Confession prompts
- `sketchy_actions.json` - Suspicious activities

### Lexicon Guidelines

1. **Keep content appropriate** - Follow content guidelines
2. **Use consistent formatting** - Each entry should be a string
3. **Avoid duplicates** - Check existing lexicons first
4. **Consider variety** - Include different types of content
5. **Test with templates** - Ensure words work in template contexts

## Testing

### Running Tests

```bash
# Run unit tests
./gradlew test

# Run integration tests
./gradlew connectedAndroidTest

# Run specific test class
./gradlew test --tests "*:YourTestClass"
```

### Test Coverage

- Aim for >80% test coverage on new code
- Test edge cases (empty inputs, max values, etc.)
- Test with different player counts
- Test error conditions

### Test Templates

Use the template engine test utilities:

```kotlin
@Test
fun `template fills correctly`() {
    val template = loadTemplate("your_template_id")
    val result = templateEngine.fill(template, context)
    
    assertNotNull(result)
    assertTrue(result.text.isNotEmpty())
    assertFalse(result.text.contains("{"))
}
```

## Code Style

### Kotlin Guidelines

- Use Kotlin idioms and conventions
- Prefer `val` over `var` when possible
- Use null safety features
- Follow Android Kotlin style guide

### Compose Guidelines

- Use `@Composable` functions for UI components
- Prefer `remember` for expensive calculations
- Use `derivedStateOf` for computed state
- Keep composables pure and side-effect free

### Documentation

- Add KDoc comments to public APIs
- Include parameter descriptions
- Document return values
- Add usage examples

```kotlin
/**
 * Brief description of what the function does.
 * 
 * @param parameter1 Description of parameter1
 * @param parameter2 Description of parameter2
 * @return Description of return value
 * @throws ExceptionType When this exception can occur
 */
fun yourFunction(parameter1: String, parameter2: Int): String {
    // Implementation
}
```

## Submitting Changes

### 1. Create Branch

```bash
git checkout -b feature/your-feature-name
```

### 2. Make Changes

- Follow the coding standards
- Add tests for new functionality
- Update documentation

### 3. Test Changes

```bash
# Run tests
./gradlew test

# Run lint checks
./gradlew lint

# Build project
./gradlew build
```

### 4. Commit Changes

```bash
git add .
git commit -m "feat: add your feature description

- Add new game: Your Game Name
- Implement template logic
- Add unit tests
- Update documentation"
```

### 5. Create Pull Request

- Use descriptive title
- Fill out PR template
- Link relevant issues
- Request review from maintainers

## Reporting Issues

### Bug Reports

Include:
- Device information
- App version
- Steps to reproduce
- Expected vs actual behavior
- Screenshots if applicable

### Feature Requests

Include:
- Use case description
- Problem it solves
- Proposed solution
- Alternative approaches considered

## Content Guidelines

### General Principles

1. **Keep it fun** - HELLDECK is a party game
2. **Be inclusive** - Avoid offensive or exclusionary content
3. **Stay age-appropriate** - Target teen+ audience
4. **Encourage creativity** - Allow for humorous and unexpected responses
5. **Test thoroughly** - Ensure content works in various contexts

### Specific Guidelines

- **No hate speech or discrimination**
- **Avoid overly sensitive topics**
- **Keep profanity minimal and contextual**
- **Ensure content works for all player counts**
- **Test for cultural sensitivity**

## Getting Help

### Resources

- [Android Developer Documentation](https://developer.android.com/)
- [Jetpack Compose Documentation](https://developer.android.com/jetpack/compose)
- [Kotlin Documentation](https://kotlinlang.org/docs/)
- [Material Design 3](https://m3.material.io/)

### Contact

- Create an issue for questions
- Join our Discord community
- Check existing documentation first

## Review Process

### What We Look For

1. **Functionality** - Does it work as intended?
2. **Code quality** - Is it well-written and maintainable?
3. **Tests** - Are there adequate tests?
4. **Documentation** - Is it properly documented?
5. **Performance** - Does it impact app performance?
6. **Accessibility** - Is it accessible to all users?

### Review Timeline

- Initial review: 1-3 days
- Feedback provided: 3-5 days
- Revision window: 1 week
- Final decision: 1-2 days after revisions

## License

By contributing to HELLDECK, you agree that your contributions will be licensed under the same license as the project.

---

Thank you for contributing to HELLDECK! Your contributions help make the game better for everyone.