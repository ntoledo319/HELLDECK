#!/bin/bash
# Cross-platform test script (Unix/macOS/Linux)
# Run unit and integration tests

set -euo pipefail

echo "🧪 Running tests..."
./gradlew testDebugUnitTest
echo "✅ Tests completed"

