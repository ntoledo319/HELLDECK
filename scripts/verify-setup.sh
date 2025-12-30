#!/bin/bash
# Verification script to check if quality automation is properly set up

set -euo pipefail

echo "🔍 Verifying HELLDECK Quality Automation Setup..."
echo ""

ERRORS=0

# Check Makefile exists
if [ -f "Makefile" ]; then
    echo "✅ Makefile found"
else
    echo "❌ Makefile not found"
    ERRORS=$((ERRORS + 1))
fi

# Check scripts directory
if [ -d "scripts" ]; then
    echo "✅ scripts/ directory found"
    for script in fix.sh check.sh test.sh ci.sh; do
        if [ -f "scripts/$script" ]; then
            echo "  ✅ scripts/$script exists"
        else
            echo "  ❌ scripts/$script missing"
            ERRORS=$((ERRORS + 1))
        fi
    done
else
    echo "❌ scripts/ directory not found"
    ERRORS=$((ERRORS + 1))
fi

# Check PowerShell scripts (optional)
for script in fix.ps1 check.ps1 test.ps1 ci.ps1; do
    if [ -f "scripts/$script" ]; then
        echo "  ✅ scripts/$script exists (Windows support)"
    fi
done

# Check config files
if [ -f "config/detekt.yml" ]; then
    echo "✅ config/detekt.yml found"
else
    echo "❌ config/detekt.yml not found"
    ERRORS=$((ERRORS + 1))
fi

if [ -f "pyproject.toml" ]; then
    echo "✅ pyproject.toml found"
else
    echo "❌ pyproject.toml not found"
    ERRORS=$((ERRORS + 1))
fi

if [ -f ".editorconfig" ]; then
    echo "✅ .editorconfig found"
else
    echo "❌ .editorconfig not found"
    ERRORS=$((ERRORS + 1))
fi

if [ -f ".pre-commit-config.yaml" ]; then
    echo "✅ .pre-commit-config.yaml found"
else
    echo "⚠️  .pre-commit-config.yaml not found (optional)"
fi

# Check GitHub Actions workflows
if [ -d ".github/workflows" ]; then
    echo "✅ .github/workflows/ directory found"
    for workflow in quality.yml autofix.yml; do
        if [ -f ".github/workflows/$workflow" ]; then
            echo "  ✅ .github/workflows/$workflow exists"
        else
            echo "  ❌ .github/workflows/$workflow missing"
            ERRORS=$((ERRORS + 1))
        fi
    done
else
    echo "❌ .github/workflows/ directory not found"
    ERRORS=$((ERRORS + 1))
fi

# Check Gradle plugins in build.gradle
if grep -q "org.jlleitschuh.gradle.ktlint" build.gradle; then
    echo "✅ ktlint plugin configured in build.gradle"
else
    echo "❌ ktlint plugin not found in build.gradle"
    ERRORS=$((ERRORS + 1))
fi

if grep -q "io.gitlab.arturbosch.detekt" build.gradle; then
    echo "✅ detekt plugin configured in build.gradle"
else
    echo "❌ detekt plugin not found in build.gradle"
    ERRORS=$((ERRORS + 1))
fi

if grep -q "com.diffplug.spotless" build.gradle; then
    echo "✅ spotless plugin configured in build.gradle"
else
    echo "❌ spotless plugin not found in build.gradle"
    ERRORS=$((ERRORS + 1))
fi

# Check app/build.gradle has plugins applied
if grep -q "id 'org.jlleitschuh.gradle.ktlint'" app/build.gradle; then
    echo "✅ ktlint plugin applied in app/build.gradle"
else
    echo "❌ ktlint plugin not applied in app/build.gradle"
    ERRORS=$((ERRORS + 1))
fi

echo ""
if [ $ERRORS -eq 0 ]; then
    echo "✅ All checks passed! Quality automation is properly set up."
    echo ""
    echo "Next steps:"
    echo "  1. Run 'make fix' to apply initial formatting"
    echo "  2. Commit the changes"
    echo "  3. Set up branch protection in GitHub (see README)"
    exit 0
else
    echo "❌ Found $ERRORS error(s). Please review the setup."
    exit 1
fi

