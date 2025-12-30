#!/usr/bin/env python3
"""
UI Verification Script
Checks that all 14 official games are properly integrated in the UI
"""

import os
import re
from pathlib import Path

# Official 14 games from HDRealRules.md
OFFICIAL_GAMES = [
    "ROAST_CONS",      # Roast Consensus
    "POISON_PITCH",    # Poison Pitch
    "HOTSEAT_IMP",     # Hot Seat Imposter
    "CONFESS_CAP",     # Confession or Cap
    "FILLIN",          # Fill-In Finisher
    "TEXT_TRAP",       # Text Thread Trap
    "TABOO",           # Taboo Timer
    "SCATTER",         # Scatterblast
    "ALIBI",           # Alibi Drop
    "TITLE_FIGHT",     # Title Fight
    "RED_FLAG",        # Red Flag Rally
    "OVER_UNDER",      # Over/Under
    "REALITY_CHECK",   # Reality Check
    "UNIFYING_THEORY"  # The Unifying Theory
]

def check_game_metadata():
    """Check GameMetadata.kt has all 14 games"""
    print("✓ Checking GameMetadata.kt...")
    metadata_file = Path("app/src/main/java/com/helldeck/engine/GameMetadata.kt")
    
    if not metadata_file.exists():
        print("  ✗ GameMetadata.kt not found!")
        return False
    
    content = metadata_file.read_text()
    found_games = []
    
    for game in OFFICIAL_GAMES:
        if f"GameIds.{game}" in content:
            found_games.append(game)
    
    if len(found_games) == 14:
        print(f"  ✓ All 14 games found in GameMetadata.kt")
        return True
    else:
        print(f"  ✗ Only {len(found_games)}/14 games found")
        missing = set(OFFICIAL_GAMES) - set(found_games)
        print(f"    Missing: {missing}")
        return False

def check_game_icons():
    """Check GameIcons.kt has icons for all 14 games"""
    print("✓ Checking GameIcons.kt...")
    icons_file = Path("app/src/main/java/com/helldeck/ui/GameIcons.kt")
    
    if not icons_file.exists():
        print("  ✗ GameIcons.kt not found!")
        return False
    
    content = icons_file.read_text()
    found_games = []
    
    for game in OFFICIAL_GAMES:
        if f'"{game}"' in content or f"GameIds.{game}" in content:
            found_games.append(game)
    
    if len(found_games) == 14:
        print(f"  ✓ All 14 games have icons defined")
        return True
    else:
        print(f"  ✗ Only {len(found_games)}/14 games have icons")
        missing = set(OFFICIAL_GAMES) - set(found_games)
        print(f"    Missing: {missing}")
        return False

def check_game_picker():
    """Check GamePickerSheet uses getAllGameIds()"""
    print("✓ Checking GamePickerSheet.kt...")
    picker_file = Path("app/src/main/java/com/helldeck/ui/components/GamePickerSheet.kt")
    
    if not picker_file.exists():
        print("  ✗ GamePickerSheet.kt not found!")
        return False
    
    content = picker_file.read_text()
    
    if "getAllGameIds()" in content:
        print(f"  ✓ GamePickerSheet uses getAllGameIds() - all games accessible")
        return True
    else:
        print(f"  ✗ GamePickerSheet doesn't use getAllGameIds()")
        return False

def check_interaction_renderer():
    """Check InteractionRenderer handles all interaction types"""
    print("✓ Checking InteractionRenderer.kt...")
    renderer_file = Path("app/src/main/java/com/helldeck/ui/interactions/InteractionRenderer.kt")
    
    if not renderer_file.exists():
        print("  ✗ InteractionRenderer.kt not found!")
        return False
    
    content = renderer_file.read_text()
    
    # Check for key interaction types
    interaction_types = [
        "VOTE_PLAYER",
        "TRUE_FALSE", 
        "A_B_CHOICE",
        "JUDGE_PICK",
        "REPLY_TONE",
        "TABOO_GUESS",
        "SPEED_LIST",
        "HIDE_WORDS",
        "MINI_DUEL",
        "ODD_EXPLAIN",
        "PREDICT_VOTE"
    ]
    
    found = sum(1 for it in interaction_types if it in content)
    
    if found >= 10:
        print(f"  ✓ InteractionRenderer handles {found} interaction types")
        return True
    else:
        print(f"  ✗ InteractionRenderer only handles {found} interaction types")
        return False

def main():
    print("=" * 60)
    print("HELLDECK UI VERIFICATION")
    print("=" * 60)
    print()
    
    os.chdir(Path(__file__).parent.parent)
    
    checks = [
        check_game_metadata(),
        check_game_icons(),
        check_game_picker(),
        check_interaction_renderer()
    ]
    
    print()
    print("=" * 60)
    
    if all(checks):
        print("✅ ALL UI CHECKS PASSED!")
        print("All 14 games are properly integrated in the UI")
        return 0
    else:
        print("❌ SOME UI CHECKS FAILED")
        print(f"Passed: {sum(checks)}/{len(checks)}")
        return 1

if __name__ == "__main__":
    exit(main())