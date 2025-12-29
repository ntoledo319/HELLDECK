#!/bin/bash

# HELLDECK Card Generation System Cleanup Script
# This script removes legacy files after streamlining to unified LLM system
# 
# IMPORTANT: Review all deletions before running!
# Run with: bash STREAMLINING_CLEANUP.sh

set -e

echo "======================================"
echo "HELLDECK System Streamlining Cleanup"
echo "======================================"
echo ""
echo "This script will delete legacy card generation files."
echo "The new unified system uses:"
echo "  - LLMCardGenerator.kt (unified)"
echo "  - GameEngineSimplified.kt"
echo "  - gold_cards.json (single asset file)"
echo ""
read -p "Continue with cleanup? (yes/no): " confirm

if [ "$confirm" != "yes" ]; then
    echo "Cleanup cancelled."
    exit 0
fi

echo ""
echo "Starting cleanup..."
echo ""

# Counter for tracking
DELETED_COUNT=0

# Function to safely delete file
delete_file() {
    local file="$1"
    if [ -f "$file" ]; then
        echo "  ✓ Deleting: $file"
        rm "$file"
        ((DELETED_COUNT++))
    else
        echo "  ⊘ Not found (already deleted?): $file"
    fi
}

# Function to safely delete directory
delete_dir() {
    local dir="$1"
    if [ -d "$dir" ]; then
        echo "  ✓ Deleting directory: $dir"
        rm -rf "$dir"
        ((DELETED_COUNT++))
    else
        echo "  ⊘ Not found (already deleted?): $dir"
    fi
}

echo "===== PHASE 1: Legacy Generator Code ====="
echo ""

# Old LLM generators
delete_file "app/src/main/java/com/hell deck/content/generator/LLMCardGeneratorV2.kt"
delete_file "app/src/main/java/com/helldeck/content/generator/LLMCardGeneratorV1.kt"

# Template-based generator and dependencies
delete_file "app/src/main/java/com/helldeck/content/generator/CardGeneratorV3.kt"
delete_file "app/src/main/java/com/helldeck/content/generator/BlueprintRepositoryV3.kt"
delete_file "app/src/main/java/com/helldeck/content/generator/GeneratorArtifacts.kt"
delete_file "app/src/main/java/com/helldeck/content/generator/HumorScorer.kt"
delete_file "app/src/main/java/com/helldeck/content/generator/Template.kt"
delete_file "app/src/main/java/com/helldeck/content/generator/TemplateBlueprint.kt"

# Lexicon system (no longer needed)
delete_file "app/src/main/java/com/helldeck/content/generator/LexiconRepository.kt"
delete_file "app/src/main/java/com/helldeck/content/generator/LexiconRepositoryV2.kt"
delete_file "app/src/main/java/com/helldeck/content/generator/LexiconEntry.kt"

# Gold bank system (replaced by GoldCardsLoader)
delete_file "app/src/main/java/com/helldeck/content/generator/gold/GoldBank.kt"
delete_file "app/src/main/java/com/helldeck/content/generator/gold/GoldCard.kt"

# Semantic validation (over-engineered)
delete_file "app/src/main/java/com/helldeck/content/validation/SemanticValidator.kt"
delete_file "app/src/main/java/com/helldeck/content/validation/SlotFill.kt"

# Template engine components
delete_file "app/src/main/java/com/helldeck/content/engine/TemplateEngine.kt"
delete_file "app/src/main/java/com/helldeck/content/engine/ContextualSelector.kt"
delete_file "app/src/main/java/com/helldeck/content/engine/OptionsCompiler.kt"
delete_file "app/src/main/java/com/helldeck/content/engine/augment/Augmentor.kt"
delete_file "app/src/main/java/com/helldeck/content/engine/augment/StyleGuides.kt"

echo ""
echo "===== PHASE 2: Legacy Asset Files ====="
echo ""

# Old gold cards
delete_dir "app/src/main/assets/gold"
delete_file "app/src/main/assets/gold_cards.json"

# Lexicon files (48 files total)
delete_dir "app/src/main/assets/lexicons"
delete_dir "app/src/main/assets/lexicons_v2"

# Template files (17 files total)
delete_dir "app/src/main/assets/templates"
delete_dir "app/src/main/assets/templates_v2"
delete_dir "app/src/main/assets/templates_v3"

# Model configuration files (for template system)
delete_file "app/src/main/assets/model/pairings.json"
delete_file "app/src/main/assets/model/logit.json"
delete_file "app/src/main/assets/model/semantic_compatibility.json"
delete_file "app/src/main/assets/model/tone_gate.yaml"
delete_file "app/src/main/assets/model/priors.json"
delete_file "app/src/main/assets/model/rules.yaml"

# Settings files for old system
delete_file "app/src/main/assets/settings/default.yaml"

echo ""
echo "===== PHASE 3: Documentation Updates Needed ====="
echo ""
echo "The following files need manual updates:"
echo "  - ARCHITECTURE.md (update to reflect new system)"
echo "  - README.md (update quick start and architecture section)"
echo "  - docs/DEVELOPER.md (update generator information)"
echo "  - docs/LLM_AND_QUALITY.md (update quality system docs)"
echo ""

echo "===== Cleanup Summary ====="
echo ""
echo "Files/directories deleted: $DELETED_COUNT"
echo ""
echo "Remaining core files:"
echo "  Code:"
echo "    ✓ LLMCardGenerator.kt (unified)"
echo "    ✓ GameEngineSimplified.kt"
echo "    ✓ GoldCardsLoader.kt"
echo "  Assets:"
echo "    ✓ gold_cards.json"
echo "    ✓ model/banned.json"
echo ""
echo "Next steps:"
echo "  1. Update GameEngine.kt references to use GameEngineSimplified"
echo "  2. Update dependency injection / initialization code"
echo "  3. Test all 14 games with new system"
echo "  4. Update documentation"
echo "  5. Fill in CONTENT_SPEC_TEMPLATE.md with your new content"
echo ""
echo "✅ Cleanup complete!"
