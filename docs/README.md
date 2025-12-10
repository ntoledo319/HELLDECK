# HELLDECK Documentation

## 📚 Documentation Overview

This directory contains comprehensive documentation for the HELLDECK party game system.

## 📁 Documentation Structure

### 🚀 [Quick Start Guide](QUICKSTART.md)
- Step-by-step setup instructions
- First run configuration
- Basic troubleshooting

### 🎮 [User Guide](USERGUIDE.md)
- Complete game rules for all 14 mini-games
- Player setup and management
- Game flow and controls
- Brainpack export/import

### 🔧 [Developer Guide](DEVELOPER.md)
- Architecture overview
- Development setup
- Code organization
- Testing strategies

### 🤖 [LLM & Quality Guide](LLM_AND_QUALITY.md)
- How the on-device LLM is bundled and loaded
- Running full game quality checks
- Interpreting reports and tuning

### 📖 [API Reference](API.md)
- Complete API documentation
- Class and method references
- Configuration options
- Integration examples

### 🛠️ [Troubleshooting](TROUBLESHOOTING.md)
- Common issues and solutions
- Debug information
- Performance optimization
- Error handling

## 🎯 Key Features Documented

### Core Systems
- **Game Engine** - Template selection, learning algorithms, round management
- **Data Layer** - Room database, repositories, entity relationships
- **UI Framework** - Jetpack Compose components, theming, animations
- **Kiosk Mode** - Device lockdown, admin permissions, immersive mode

### Game Mechanics
- **14 Mini-Games** - Complete rules and interactions for each game type
- **Learning System** - Multi-armed bandit algorithms, performance tracking
- **Player Management** - Statistics, ELO ratings, session tracking
- **Feedback System** - Multi-dimensional feedback, room consensus detection

### Technical Features
- **Haptics & Torch** - Vibration patterns, camera flash feedback
- **Export/Import** - Brainpack file format, data migration
- **Configuration** - YAML-based settings, runtime customization
- **Logging** - Comprehensive logging, performance monitoring

## 🎨 Game Documentation

### Game Types
1. **Roast Consensus** - Vote on most likely target
2. **Confession or Cap** - Truth or bluff detection
3. **Poison Pitch** - Sell awful "Would You Rather" options
4. **Fill-In Finisher** - Complete prompts with punchlines
5. **Red Flag Rally** - Defend dating scenarios
6. **Hot Seat Imposter** - Impersonate target player
7. **Text Thread Trap** - Choose perfect reply tones
8. **Taboo Timer** - Describe without forbidden words
9. **Odd One Out** - Identify and explain misfits
10. **Title Fight** - Mini-game duels for the crown
11. **Alibi Drop** - Hide words in excuses
12. **Hype or Yike** - Pitch absurd products
13. **Scatterblast** - Quick category naming
14. **Majority Report** - Predict room votes

### Template System
- **Dynamic Filling** - Slot-based template system
- **Lexicon Management** - Word lists for each slot type
- **Spice Levels** - Content intensity control
- **Family Tracking** - Diversity and variety management

## 🔧 Technical Documentation

### Architecture Patterns
- **MVVM** - Model-View-ViewModel for UI
- **Repository** - Data access abstraction
- **Observer** - Reactive state management
- **Strategy** - Algorithm selection patterns

### Performance Optimization
- **Database Indexing** - Optimized queries and relationships
- **Memory Management** - Lazy loading and object pooling
- **UI Optimization** - Compose performance best practices
- **Background Processing** - Async operations and coroutines

### Security Considerations
- **Kiosk Mode** - Device lockdown and admin controls
- **Data Privacy** - Local-only storage, no cloud dependencies
- **Content Filtering** - Forbidden word management
- **Permission Management** - Runtime permission handling

## 🚀 Getting Started

### For Players
1. Read the [Quick Start Guide](QUICKSTART.md)
2. Follow the setup instructions
3. Learn the game rules in the [User Guide](USERGUIDE.md)
4. Start playing and enjoy!

### For Developers
1. Review the [Developer Guide](DEVELOPER.md)
2. Study the [API Reference](API.md)
3. Set up your development environment
4. Start contributing to the project

## 📖 Documentation Conventions

### Code Examples
```kotlin
// Kotlin examples use standard formatting
fun exampleFunction(param: String): Boolean {
    return param.isNotBlank()
}
```

### Configuration Examples
```yaml
# YAML examples use standard indentation
setting:
  value: "example"
  enabled: true
```

### API Documentation
- **Parameters** - Listed with types and descriptions
- **Returns** - Return types and descriptions
- **Throws** - Exception types and conditions
- **Examples** - Usage examples where helpful

### Code Documentation Standards
- All public classes, methods, and properties include KDoc comments
- Private methods include documentation for maintainability
- Extension functions are documented with usage context
- Consistent formatting and clear descriptions for all APIs

## 🔍 Searching Documentation

### Key Topics
- **Game Rules** - Search for specific game names
- **API Reference** - Search for class or method names
- **Troubleshooting** - Search for error messages or issues
- **Configuration** - Search for setting names or options

### Navigation Tips
- Use the table of contents in each document
- Follow cross-references between documents
- Check the API reference for implementation details
- Review troubleshooting for common solutions

## 📝 Contributing to Documentation

### Documentation Standards
- Use clear, concise language
- Include code examples where helpful
- Add screenshots for UI documentation
- Keep information up-to-date with code changes

### Adding New Content
1. Follow existing documentation structure
2. Use consistent formatting and style
3. Add cross-references where appropriate
4. Include examples and use cases
5. Test all code examples

## 🎯 Documentation Roadmap

### High Priority
- [ ] Video tutorials for setup and gameplay
- [ ] Interactive API playground
- [ ] Mobile-optimized documentation
- [ ] Search functionality across all docs

### Medium Priority
- [ ] Advanced configuration examples
- [ ] Performance tuning guide
- [ ] Custom game development tutorial
- [ ] Deployment best practices

### Low Priority
- [ ] Internationalization guide
- [ ] Accessibility features documentation
- [ ] Advanced kiosk configurations
- [ ] Enterprise deployment guide

## 📞 Support and Feedback

### Getting Help
1. Check the [Troubleshooting](TROUBLESHOOTING.md) guide
2. Review the [API Reference](API.md) for technical details
3. Search existing documentation for your topic
4. Check the FAQ section in the [User Guide](USERGUIDE.md)

### Providing Feedback
- Report documentation issues or improvements
- Suggest new topics or examples
- Share your experience with the documentation
- Help improve clarity and completeness

## 📅 Documentation Updates

### Version History
- **v1.0.0** - Initial comprehensive documentation
- Complete API reference for all components
- User guides for all 14 game types
- Developer setup and contribution guidelines

### Maintenance Schedule
- Documentation is updated with each release
- API changes are documented immediately
- New features include documentation
- Quarterly review for accuracy and completeness

---

*This documentation is continuously improved to provide the best possible experience for both players and developers of the HELLDECK party game system.*
