#!/bin/bash
# Cross-platform CI script (Unix/macOS/Linux)
# Run full CI pipeline (check + test)

set -euo pipefail

echo "🚀 Running CI pipeline..."

# Run checks
./scripts/check.sh

# Run tests
./scripts/test.sh

echo "✅ CI pipeline completed successfully"

