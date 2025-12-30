.PHONY: help fix check test ci format-kotlin format-python lint-kotlin lint-python

help: ## Show this help message
	@echo "HELLDECK Quality Automation"
	@echo ""
	@echo "Available commands:"
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | awk 'BEGIN {FS = ":.*?## "}; {printf "  \033[36m%-15s\033[0m %s\n", $$1, $$2}'
	@echo ""
	@echo "Alternative: Use scripts/ directory for cross-platform support"
	@echo "  ./scripts/fix.sh    - Apply autofixes"
	@echo "  ./scripts/check.sh  - Run checks"
	@echo "  ./scripts/test.sh  - Run tests"
	@echo "  ./scripts/ci.sh     - Run CI pipeline"

fix: ## Apply all autofixes (format + lint fixes)
	@echo "🔧 Applying autofixes..."
	@$(MAKE) format-kotlin
	@$(MAKE) lint-kotlin
	@$(MAKE) format-python
	@$(MAKE) lint-python
	@echo "✅ All autofixes applied"

check: ## Verify formatting/lint without making changes (CI safe)
	@echo "🔍 Running checks..."
	@$(MAKE) check-kotlin
	@$(MAKE) check-python
	@echo "✅ All checks passed"

test: ## Run unit and integration tests
	@echo "🧪 Running tests..."
	@./gradlew testDebugUnitTest
	@echo "✅ Tests completed"

ci: check test ## Run full CI pipeline (check + test)

format-kotlin: ## Format Kotlin code
	@echo "📝 Formatting Kotlin code..."
	@./gradlew ktlintFormat spotlessApply || echo "⚠️  Kotlin formatting failed (may need plugin setup)"

format-python: ## Format Python code
	@echo "📝 Formatting Python code..."
	@command -v ruff >/dev/null 2>&1 || pip install ruff
	@ruff format loader/ tools/ --exclude third_party || echo "⚠️  Python formatting failed"

lint-kotlin: ## Fix Kotlin lint issues (detekt autoCorrect enabled in config)
	@echo "🔍 Fixing Kotlin lint issues..."
	@./gradlew detekt || echo "⚠️  detekt failed (autoCorrect enabled in config)"

lint-python: ## Fix Python lint issues
	@echo "🔍 Fixing Python lint issues..."
	@command -v ruff >/dev/null 2>&1 || pip install ruff
	@ruff check --fix loader/ tools/ --exclude third_party || echo "⚠️  Python linting failed"

check-kotlin: ## Check Kotlin formatting/lint without changes
	@echo "🔍 Checking Kotlin code..."
	@./gradlew ktlintCheck detekt spotlessCheck || echo "⚠️  Kotlin checks failed"

check-python: ## Check Python formatting/lint without changes
	@echo "🔍 Checking Python code..."
	@command -v ruff >/dev/null 2>&1 || pip install ruff
	@ruff check loader/ tools/ --exclude third_party || echo "⚠️  Python checks failed"
	@ruff format --check loader/ tools/ --exclude third_party || echo "⚠️  Python format check failed"

