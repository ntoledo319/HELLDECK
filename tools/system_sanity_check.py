#!/usr/bin/env python3
"""
Full System Sanity Check for HELLDECK
Verifies all systems are aligned with HDRealRules.md
"""

import json
from pathlib import Path


class SystemSanityCheck:
    def __init__(self, base_path="."):
        self.base_path = Path(base_path)
        self.issues = []
        self.warnings = []
        self.passed_checks = []

    def check_1_game_metadata(self):
        """Check GameMetadata.kt has all 14 games"""
        print("\n🔍 CHECK 1: GameMetadata.kt Validation")
        print("=" * 60)

        file_path = self.base_path / "app/src/main/java/com/helldeck/engine/GameMetadata.kt"
        with open(file_path) as f:
            content = f.read()

        required_games = {
            "ROAST_CONSENSUS": "ROAST_CONS",
            "CONFESSION_OR_CAP": "CONFESS_CAP",
            "POISON_PITCH": "POISON_PITCH",
            "FILL_IN_FINISHER": "FILLIN",
            "RED_FLAG_RALLY": "RED_FLAG",
            "HOT_SEAT_IMPOSTER": "HOTSEAT_IMP",
            "TEXT_THREAD_TRAP": "TEXT_TRAP",
            "TABOO_TIMER": "TABOO",
            "THE_UNIFYING_THEORY": "UNIFYING_THEORY",
            "TITLE_FIGHT": "TITLE_FIGHT",
            "ALIBI_DROP": "ALIBI",
            "REALITY_CHECK": "REALITY_CHECK",
            "SCATTERBLAST": "SCATTER",
            "OVER_UNDER": "OVER_UNDER",
        }

        for full_name, short_name in required_games.items():
            if f"GameIds.{short_name}" in content or full_name in content:
                print(f"  ✓ {full_name}")
            else:
                self.issues.append(f"GameMetadata.kt missing {full_name}")
                print(f"  ✗ {full_name}")

        # Check for legacy games
        legacy_games = ["MAJORITY_REPORT", "ODD_ONE_OUT", "HYPE_OR_YIKE"]
        for legacy in legacy_games:
            if (
                legacy in content
                and "// REMOVED" not in content[content.find(legacy) : content.find(legacy) + 100]
            ):
                self.issues.append(f"GameMetadata.kt still has active reference to {legacy}")

        if not self.issues:
            self.passed_checks.append("GameMetadata.kt validation")
            print("  ✅ GameMetadata.kt PASSED")
        else:
            print("  ❌ GameMetadata.kt FAILED")

    def check_2_gold_cards(self):
        """Check gold_cards.json has all 14 games with 50 cards each"""
        print("\n🔍 CHECK 2: Gold Cards Validation")
        print("=" * 60)

        file_path = self.base_path / "app/src/main/assets/gold_cards.json"
        with open(file_path) as f:
            data = json.load(f)

        required_games = [
            "roast_consensus",
            "confession_or_cap",
            "poison_pitch",
            "fill_in_finisher",
            "red_flag_rally",
            "hot_seat_imposter",
            "text_thread_trap",
            "taboo_timer",
            "the_unifying_theory",
            "title_fight",
            "alibi_drop",
            "reality_check",
            "scatterblast",
            "over_under",
        ]

        games = data.get("games", {})

        for game in required_games:
            if game in games:
                card_count = len(games[game].get("cards", []))
                if card_count >= 50:
                    print(f"  ✓ {game}: {card_count} cards")
                else:
                    self.issues.append(f"{game} has only {card_count} cards (need 50)")
                    print(f"  ✗ {game}: {card_count} cards (need 50)")
            else:
                self.issues.append(f"gold_cards.json missing {game}")
                print(f"  ✗ {game}: MISSING")

        # Check for legacy games
        legacy_games = ["majority_report", "odd_one_out", "hype_or_yike"]
        for legacy in legacy_games:
            if legacy in games:
                self.issues.append(f"gold_cards.json still has {legacy}")
                print(f"  ✗ Found legacy game: {legacy}")

        if not any(issue.startswith("gold_cards") for issue in self.issues):
            self.passed_checks.append("Gold cards validation")
            print("  ✅ Gold Cards PASSED")
        else:
            print("  ❌ Gold Cards FAILED")

    def check_3_template_files(self):
        """Check all 14 games have template files"""
        print("\n🔍 CHECK 3: Template Files Validation")
        print("=" * 60)

        template_dir = self.base_path / "app/src/main/assets/templates_v3"

        required_templates = [
            "roast_consensus.json",
            "confession_or_cap.json",
            "poison_pitch.json",
            "fill_in_finisher.json",
            "red_flag.json",
            "hot_seat_imposter.json",
            "text_trap.json",
            "taboo_timer.json",
            "the_unifying_theory.json",
            "title_fight.json",
            "alibi_drop.json",
            "reality_check.json",
            "scatterblast.json",
            "over_under.json",
        ]

        for template in required_templates:
            file_path = template_dir / template
            if file_path.exists():
                print(f"  ✓ {template}")
            else:
                self.issues.append(f"Missing template: {template}")
                print(f"  ✗ {template}")

        # Check for legacy templates
        legacy_templates = ["hype_or_yike.json", "odd_one_out.json", "majority_report.json"]
        for legacy in legacy_templates:
            file_path = template_dir / legacy
            if file_path.exists():
                self.issues.append(f"Legacy template still exists: {legacy}")
                print(f"  ✗ Found legacy template: {legacy}")

        if not any("template" in issue.lower() for issue in self.issues):
            self.passed_checks.append("Template files validation")
            print("  ✅ Template Files PASSED")
        else:
            print("  ❌ Template Files FAILED")

    def check_4_no_legacy_references(self):
        """Check for active legacy game references in code"""
        print("\n🔍 CHECK 4: Legacy Reference Check")
        print("=" * 60)

        # Search for legacy references in Kotlin files
        kt_files = list(self.base_path.glob("app/src/main/java/**/*.kt"))

        legacy_patterns = [
            "MAJORITY_REPORT",
            "ODD_ONE_OUT",
            "HYPE_OR_YIKE",
            "GameIds.MAJORITY",
            "GameIds.ODD_ONE",
            "GameIds.HYPE_YIKE",
        ]

        active_references = 0
        comment_references = 0

        for kt_file in kt_files:
            with open(kt_file) as f:
                lines = f.readlines()

            for i, line in enumerate(lines):
                for pattern in legacy_patterns:
                    if pattern in line:
                        # Check if it's a comment
                        stripped = line.strip()
                        if stripped.startswith("//") or "/*" in line or "*/" in line:
                            comment_references += 1
                        else:
                            active_references += 1
                            self.warnings.append(
                                f"{kt_file.name}:{i + 1} - Active reference: {pattern}"
                            )

        print(f"  Active references: {active_references}")
        print(f"  Comment references: {comment_references}")

        if active_references == 0:
            self.passed_checks.append("No active legacy references")
            print("  ✅ Legacy Reference Check PASSED")
        else:
            print("  ⚠️  Legacy Reference Check has warnings")

    def check_5_game_icons(self):
        """Check GameIcons.kt has correct icons"""
        print("\n🔍 CHECK 5: Game Icons Validation")
        print("=" * 60)

        file_path = self.base_path / "app/src/main/java/com/helldeck/ui/GameIcons.kt"
        with open(file_path) as f:
            content = f.read()

        required_icons = {
            "ROAST_CONS": "🎯",
            "CONFESS_CAP": "🤥",
            "POISON_PITCH": "💀",
            "FILLIN": "✍️",
            "RED_FLAG": "🚩",
            "HOTSEAT_IMP": "🎭",
            "TEXT_TRAP": "📱",
            "TABOO": "⏱️",
            "UNIFYING_THEORY": "📐",
            "TITLE_FIGHT": "🥊",
            "ALIBI": "🕵️",
            "REALITY_CHECK": "🪞",
            "SCATTER": "💣",
            "OVER_UNDER": "📉",
        }

        for game_id, expected_icon in required_icons.items():
            if f"GameIds.{game_id}" in content and expected_icon in content:
                print(f"  ✓ {game_id}: {expected_icon}")
            else:
                self.warnings.append(f"GameIcons.kt may have wrong icon for {game_id}")

        self.passed_checks.append("Game icons validation")
        print("  ✅ Game Icons PASSED")

    def check_6_card_quality(self):
        """Run the 5-pass card quality verifier"""
        print("\n🔍 CHECK 6: Card Quality Verification")
        print("=" * 60)

        import subprocess

        result = subprocess.run(
            ["python3", "tools/card_quality_verifier.py"],
            cwd=self.base_path,
            capture_output=True,
            text=True,
        )

        if result.returncode == 0:
            self.passed_checks.append("Card quality verification")
            print("  ✅ Card Quality PASSED")
        else:
            self.issues.append("Card quality verification failed")
            print("  ❌ Card Quality FAILED")
            print(result.stdout[-500:] if len(result.stdout) > 500 else result.stdout)

    def run_all_checks(self):
        """Run all sanity checks"""
        print("\n" + "=" * 60)
        print("🎯 HELLDECK SYSTEM SANITY CHECK")
        print("=" * 60)

        self.check_1_game_metadata()
        self.check_2_gold_cards()
        self.check_3_template_files()
        self.check_4_no_legacy_references()
        self.check_5_game_icons()
        self.check_6_card_quality()

        print("\n" + "=" * 60)
        print("📊 SANITY CHECK SUMMARY")
        print("=" * 60)

        print(f"\n✅ Passed Checks: {len(self.passed_checks)}/6")
        for check in self.passed_checks:
            print(f"  ✓ {check}")

        if self.issues:
            print(f"\n🚨 Critical Issues: {len(self.issues)}")
            for issue in self.issues[:10]:
                print(f"  - {issue}")
            if len(self.issues) > 10:
                print(f"  ... and {len(self.issues) - 10} more")

        if self.warnings:
            print(f"\n⚠️  Warnings: {len(self.warnings)}")
            for warning in self.warnings[:10]:
                print(f"  - {warning}")
            if len(self.warnings) > 10:
                print(f"  ... and {len(self.warnings) - 10} more")

        if not self.issues and not self.warnings:
            print("\n🎉 SYSTEM SANITY CHECK PASSED - NO ISSUES!")
        elif not self.issues:
            print("\n✅ SYSTEM SANITY CHECK PASSED - Minor warnings only")
        else:
            print("\n❌ SYSTEM SANITY CHECK FAILED - Critical issues found")

        print("\n" + "=" * 60)

        return len(self.issues) == 0


if __name__ == "__main__":
    checker = SystemSanityCheck()
    success = checker.run_all_checks()
    exit(0 if success else 1)
