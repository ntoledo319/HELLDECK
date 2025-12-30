#!/bin/bash
# Cross-platform fix script (Unix/macOS/Linux)
# Apply all autofixes (format + lint fixes)

set -euo pipefail

echo "🔧 Applying autofixes..."

# Format Kotlin
echo "📝 Formatting Kotlin code..."
./gradlew ktlintFormat spotlessApply || echo "⚠️  Kotlin formatting failed"

# Fix Kotlin lint issues (detekt autoCorrect enabled in config)
echo "🔍 Fixing Kotlin lint issues..."
./gradlew detekt || echo "⚠️  Kotlin lint fixing failed"

# Format Python
echo "📝 Formatting Python code..."
if command -v ruff >/dev/null 2>&1; then
  ruff format loader/ tools/ --exclude third_party || echo "⚠️  Python formatting failed"
else
  echo "⚠️  ruff not found, installing..."
  pip install ruff
  ruff format loader/ tools/ --exclude third_party || echo "⚠️  Python formatting failed"
fi

# Fix Python lint issues
echo "🔍 Fixing Python lint issues..."
if command -v ruff >/dev/null 2>&1; then
  ruff check --fix loader/ tools/ --exclude third_party || echo "⚠️  Python linting failed"
else
  ruff check --fix loader/ tools/ --exclude third_party || echo "⚠️  Python linting failed"
fi

echo "✅ All autofixes applied"

