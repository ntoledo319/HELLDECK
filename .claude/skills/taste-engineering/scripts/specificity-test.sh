#!/bin/bash
# Specificity Test — finds copy and names that are too generic
# Usage: bash specificity-test.sh <directory>

TARGET_DIR="${1:-.}"

echo "======================================================="
echo "  SPECIFICITY TEST"
echo "  Scanning: $TARGET_DIR"
echo "======================================================="
echo ""

# Generic copy patterns
echo "-- GENERIC COPY DETECTED --"
grep -rn --include="*.tsx" --include="*.jsx" --include="*.ts" --include="*.js" --include="*.html" --include="*.md" --include="*.mdx" --include="*.kt" --include="*.xml" \
  -iE "(unlock the power|seamlessly|take .* to the next level|whether you.re|supercharge|revolutionize|empower|in today.s fast-paced|built for teams|all-in-one platform|leverage )" \
  "$TARGET_DIR" 2>/dev/null | head -30
echo ""

# Generic component names
echo "-- GENERIC COMPONENT NAMES --"
grep -rn --include="*.tsx" --include="*.jsx" --include="*.kt" \
  -E "^(export .*)?(function|const|fun|class) (Card|Hero|Section|Feature|Testimonial|Footer|Header|Sidebar|Modal|Button|Badge|Banner|Widget|Container|Wrapper|Item|List|Grid|Layout)\b" \
  "$TARGET_DIR" 2>/dev/null | head -20
echo ""

# Default Tailwind patterns (potential anti-patterns)
echo "-- TAILWIND DEFAULT AESTHETIC --"
grep -rn --include="*.tsx" --include="*.jsx" --include="*.html" --include="*.xml" \
  -E "(bg-gradient-to-r from-purple|from-blue-500 to-purple|rounded-xl shadow-lg|glassmorphism|backdrop-blur)" \
  "$TARGET_DIR" 2>/dev/null | head -15
echo ""

# Generic file structure check
echo "-- FRAMEWORK DEFAULT STRUCTURE --"
for dir in "components/ui" "components/common" "components/shared" "components/layout"; do
  if [ -d "$TARGET_DIR/$dir" ]; then
    echo "  !! Found: $dir/ (framework default — consider domain-based organization)"
  fi
done
echo ""

echo "======================================================="
echo "  Review each match. Ask: 'Could this exist"
echo "  in any other project without modification?'"
echo "======================================================="
