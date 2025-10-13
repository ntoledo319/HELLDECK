# HELLDECK Test Suite

## Quick Start

This directory contains the comprehensive unit test suite for HELLDECK.

### Running Tests in Android Studio

1. Right-click on `app/src/test` directory
2. Select "Run 'Tests in 'test''"
3. View results in the "Run" window

### Running Specific Test Classes

Navigate to any test file and click the green play icon in the gutter next to the class name.

## Test Organization

```
test/
├── java/com/helldeck/
│   ├── data/              # Data layer tests (235 tests)
│   │   ├── dao/           # DAO tests (110 tests)
│   │   └── entities/      # Entity tests (180 tests)
│   ├── engine/            # Core engine tests (65 tests)
│   └── ui/                # UI component tests (67 tests)
│       └── components/
├── fixtures/              # Test data factories
└── resources/             # Test configuration files
```

## Test Categories

### Core Engine Tests (65 tests)
- **GameEngineTest**: Game state management and round processing
- **TemplateEngineTest**: Template filling and slot replacement
- **LearningTest**: Scoring algorithms and EMA calculations
- **SelectionTest**: Template selection algorithms
- **SmartLearningTest**: Exploration vs exploitation balance

### Data Layer Tests (235 tests)
- **Repository**: Data access layer operations
- **DAOs**: Database operations (Template, Player, Round, Lexicon, Comment)
- **Entities**: Data model validation and edge cases

### UI Component Tests (67 tests)
- **GiantButton**: Touch target and interaction testing
- **CardFace**: Card display and content rendering
- **FeedbackStrip**: Feedback collection UI
- **BigZones**: Touch zone interaction testing

## Test Utilities

### Base Classes
- **BaseTest**: Common test utilities and coroutine setup
- **DatabaseTest**: Database-specific test utilities with in-memory Room

### Test Data Factory
- **TestDataFactory**: Consistent test data generation
- Located in `sharedTest/java/com/helldeck/fixtures/`

### Test Extensions
- **TestExtensions**: Helper functions for testing
- Flow utilities, assertion helpers, retry logic

## Running Tests

### All Tests
```bash
./gradlew test
```

### Specific Package
```bash
./gradlew test --tests "com.helldeck.engine.*"
```

### With Coverage
```bash
./gradlew testDebugUnitTestCoverage
```

## Test Reports

After running tests, view the HTML report at:
```
app/build/reports/tests/testDebugUnitTest/index.html
```

## Best Practices

1. **Run tests before committing** - Ensure all tests pass
2. **Add tests for new features** - Maintain high coverage
3. **Update tests when fixing bugs** - Prevent regressions
4. **Keep tests fast** - Unit tests should be <100ms
5. **Use descriptive names** - Explain what is being tested

## Coverage Goals

- Core Engine: >90% ✓
- Data Layer: >85% ✓
- UI Components: >70% ✓
- Overall: >80% ✓

## Common Issues

### Tests Not Running
- Ensure Android Studio is configured correctly
- Check that dependencies are synced
- Verify JDK version is 17 or higher

### Compilation Errors
- Clean and rebuild: `./gradlew clean build`
- Invalidate caches in Android Studio
- Sync Gradle files

### Mock Issues
- Verify MockK is properly configured
- Check mock setup in test
- Ensure relaxed mocks for complex objects

## Contributing

When adding new tests:
1. Follow existing naming conventions
2. Use TestDataFactory for test data
3. Include edge cases and error scenarios
4. Add descriptive test names with backticks
5. Document complex test scenarios

## Resources

- [Testing Documentation](../../../testing-plan.md)
- [Implementation Guide](../../../testing-implementation-guide.md)
- [Execution Guide](../../../TEST_EXECUTION_GUIDE.md)
- [Test Summary](../../../TEST_SUMMARY.md)