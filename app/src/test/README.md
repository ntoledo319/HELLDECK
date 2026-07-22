# Legacy Android test suite

The maintained Android checks are split between local JVM tests in `src/test` and API-level device
tests in `src/androidTest`.

## Commands

```bash
# Local JVM/Robolectric tests used by CI
./gradlew :app:testProductionDebugUnitTest

# Compile and package the device-test APK without starting an emulator
./gradlew :app:assembleProductionDebugAndroidTest

# Run the device suite against a connected emulator or device
./gradlew :app:connectedProductionDebugAndroidTest

# Formatting and static checks
./gradlew ktlintCheck detekt spotlessCheck :app:lintProductionDebug
```

HTML unit-test reports are written to
`app/build/reports/tests/testProductionDebugUnitTest/index.html`. Connected-test reports are
written beneath `app/build/reports/androidTests/connected/production/debug/`.

## Current coverage

- `src/test`: generator contracts, content quality, selector behavior, configuration, import/export,
  routes, and Compose components.
- `src/androidTest/ScreenSmokeTests.kt`: representative screen rendering.
- `src/androidTest/integration/DatabaseIntegrationTest.kt`: isolated in-memory Room workflows and
  transactional failure behavior.
- `src/androidTest/integration/CompleteGameFlowTest.kt`: seeded selection, filling, feedback
  persistence, fallback behavior, and session diversity.
- `src/androidTest/qa/ComprehensiveQATest.kt`: every official game at its supported player bounds,
  first-run onboarding, home accessibility, and navigation into a live round.

Device tests must be deterministic and self-contained: use seeded random sources, in-memory Room
databases, and behavior assertions instead of runner-dependent heap or wall-clock thresholds.

## Android test naming

Use camelCase method names in `src/androidTest`. Kotlin backtick names containing spaces can produce
synthetic coroutine class names that API 29's DEX format rejects before instrumentation starts.
Backtick names remain safe for local JVM-only tests in `src/test`.
