#!/bin/bash
# Cross-platform check script (Unix/macOS/Linux)
# Verify formatting/lint without making changes (CI safe)

set -euo pipefail

echo "🔍 Running checks..."

# Check Kotlin
echo "🔍 Checking Kotlin code..."
./gradlew ktlintCheck detekt spotlessCheck || {
  echo "❌ Kotlin checks failed"
  exit 1
}

# Check Python
echo "🔍 Checking Python code..."
if command -v ruff >/dev/null 2>&1; then
  ruff check loader/ tools/ --exclude third_party || {
    echo "❌ Python lint checks failed"
    exit 1
  }
  ruff format --check loader/ tools/ --exclude third_party || {
    echo "❌ Python format checks failed"
    exit 1
  }
else
  echo "⚠️  ruff not found, installing..."
  pip install ruff
  ruff check loader/ tools/ --exclude third_party || {
    echo "❌ Python lint checks failed"
    exit 1
  }
  ruff format --check loader/ tools/ --exclude third_party || {
    echo "❌ Python format checks failed"
    exit 1
  }
fi

echo "✅ All checks passed"

