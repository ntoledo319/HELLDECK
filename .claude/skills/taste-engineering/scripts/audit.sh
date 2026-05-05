#!/bin/bash
# Quick Pre-Audit — fast grep for known anti-patterns before full taste audit
# Usage: bash audit.sh <directory>

TARGET_DIR="${1:-.}"

echo "======================================================="
echo "  QUICK TASTE PRE-AUDIT"
echo "  Scanning: $TARGET_DIR"
echo "======================================================="
echo ""

VIOLATIONS=0

# Check for banned copy patterns
echo "-- BANNED COPY PATTERNS --"
COPY_HITS=$(grep -rn --include="*.kt" --include="*.xml" --include="*.json" --include="*.tsx" --include="*.jsx" --include="*.ts" --include="*.js" --include="*.md" \
  -icE "(unlock the power|seamlessly|take .* to the next level|supercharge|revolutionize|empower|elevate your|in today.s fast-paced|all-in-one)" \
  "$TARGET_DIR" 2>/dev/null)
if [ -n "$COPY_HITS" ]; then
  echo "$COPY_HITS" | head -20
  COUNT=$(echo "$COPY_HITS" | wc -l | tr -d ' ')
  VIOLATIONS=$((VIOLATIONS + COUNT))
else
  echo "  None found."
fi
echo ""

# Check for generic naming
echo "-- GENERIC NAMING --"
NAME_HITS=$(grep -rn --include="*.kt" --include="*.tsx" --include="*.jsx" \
  -E "(class|fun|function|const) (Card|Hero|Section|Feature|Widget|Container|Wrapper)\b" \
  "$TARGET_DIR" 2>/dev/null)
if [ -n "$NAME_HITS" ]; then
  echo "$NAME_HITS" | head -15
  COUNT=$(echo "$NAME_HITS" | wc -l | tr -d ' ')
  VIOLATIONS=$((VIOLATIONS + COUNT))
else
  echo "  None found."
fi
echo ""

# Check for decoration-only patterns
echo "-- DECORATION-ONLY PATTERNS --"
DECO_HITS=$(grep -rn --include="*.kt" --include="*.xml" --include="*.tsx" --include="*.jsx" \
  -iE "(glassmorphism|backdrop-blur|gradient.*purple.*blue|neon.*glow)" \
  "$TARGET_DIR" 2>/dev/null)
if [ -n "$DECO_HITS" ]; then
  echo "$DECO_HITS" | head -10
  COUNT=$(echo "$DECO_HITS" | wc -l | tr -d ' ')
  VIOLATIONS=$((VIOLATIONS + COUNT))
else
  echo "  None found."
fi
echo ""

echo "======================================================="
echo "  TOTAL POTENTIAL VIOLATIONS: $VIOLATIONS"
if [ "$VIOLATIONS" -gt 0 ]; then
  echo "  Run a full /taste-audit for detailed classification."
else
  echo "  Clean scan. Consider a full audit for deeper analysis."
fi
echo "======================================================="
