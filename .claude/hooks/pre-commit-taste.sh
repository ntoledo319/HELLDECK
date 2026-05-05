#!/bin/bash
# Taste gate — blocks commits containing known anti-patterns

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"

VIOLATIONS=$(bash "$PROJECT_ROOT/.claude/skills/taste-engineering/scripts/specificity-test.sh" "$PROJECT_ROOT" 2>/dev/null | grep -c "!!\\|GENERIC COPY\\|TAILWIND DEFAULT")

if [ "$VIOLATIONS" -gt 0 ]; then
  echo ""
  echo "======================================================"
  echo "  TASTE GATE FAILED"
  echo "  $VIOLATIONS potential generic patterns detected."
  echo "  Run /taste-audit to review before committing."
  echo "======================================================"
  echo ""
  exit 1
fi
