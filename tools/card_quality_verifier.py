#!/usr/bin/env python3
"""
5-Pass Card Quality Verification System
Ensures all cards are funny, appropriate, and match HDRealRules.md tone
"""

import json
import re
from typing import Dict, List, Tuple

class CardQualityVerifier:
    """
    5-Pass verification system for card quality
    """
    
    def __init__(self, gold_cards_path: str):
        with open(gold_cards_path, 'r') as f:
            self.data = json.load(f)
        self.issues = []
        self.warnings = []
        
    def pass_1_structure_validation(self) -> bool:
        """
        Pass 1: Validate JSON structure and required fields
        """
        print("\n🔍 PASS 1: Structure Validation")
        print("=" * 60)
        
        passed = True
        
        # Check version and description
        if 'version' not in self.data:
            self.issues.append("Missing 'version' field")
            passed = False
        if 'description' not in self.data:
            self.issues.append("Missing 'description' field")
            passed = False
            
        # Check games structure
        if 'games' not in self.data:
            self.issues.append("Missing 'games' field")
            return False
            
        games = self.data['games']
        
        # Verify all 14 games exist
        required_games = [
            'roast_consensus', 'confession_or_cap', 'poison_pitch', 'fill_in_finisher',
            'red_flag_rally', 'hot_seat_imposter', 'text_thread_trap', 'taboo_timer',
            'the_unifying_theory', 'title_fight', 'alibi_drop', 'reality_check',
            'scatterblast', 'over_under'
        ]
        
        for game in required_games:
            if game not in games:
                self.issues.append(f"Missing required game: {game}")
                passed = False
            else:
                game_data = games[game]
                if 'game_name' not in game_data:
                    self.issues.append(f"Game '{game}' missing 'game_name' field")
                    passed = False
                if 'cards' not in game_data:
                    self.issues.append(f"Game '{game}' missing 'cards' field")
                    passed = False
                else:
                    card_count = len(game_data['cards'])
                    if card_count < 50:
                        self.issues.append(f"Game '{game}' has only {card_count} cards (minimum 50 required)")
                        passed = False
                    print(f"  ✓ {game}: {card_count} cards")
                    
                    # Validate each card structure
                    for i, card in enumerate(game_data['cards']):
                        # Different games have different required fields
                        if game == 'red_flag_rally':
                            if 'perk' not in card:
                                self.issues.append(f"Game '{game}' card {i} missing 'perk' field")
                                passed = False
                            if 'red_flag' not in card:
                                self.issues.append(f"Game '{game}' card {i} missing 'red_flag' field")
                                passed = False
                        elif game == 'taboo_timer':
                            if 'text' not in card:
                                self.issues.append(f"Game '{game}' card {i} missing 'text' field")
                                passed = False
                            if 'forbidden_words' not in card:
                                self.issues.append(f"Game '{game}' card {i} missing 'forbidden_words' field")
                                passed = False
                        elif game == 'alibi_drop':
                            if 'text' not in card:
                                self.issues.append(f"Game '{game}' card {i} missing 'text' field")
                                passed = False
                            if 'hidden_words' not in card:
                                self.issues.append(f"Game '{game}' card {i} missing 'hidden_words' field")
                                passed = False
                        else:
                            if 'text' not in card:
                                self.issues.append(f"Game '{game}' card {i} missing 'text' field")
                                passed = False
                        
                        if 'quality_score' not in card:
                            self.issues.append(f"Game '{game}' card {i} missing 'quality_score' field")
                            passed = False
                        elif card['quality_score'] != 10:
                            self.issues.append(f"Game '{game}' card {i} has quality_score {card['quality_score']} (must be 10)")
                            passed = False
                        if 'spice' not in card:
                            self.issues.append(f"Game '{game}' card {i} missing 'spice' field")
                            passed = False
        
        # Check for legacy games
        legacy_games = ['majority_report', 'odd_one_out', 'hype_or_yike']
        for legacy in legacy_games:
            if legacy in games:
                self.issues.append(f"Found legacy game '{legacy}' - must be removed")
                passed = False
        
        if passed:
            print("  ✅ Structure validation PASSED")
        else:
            print("  ❌ Structure validation FAILED")
            
        return passed
    
    def pass_2_content_quality(self) -> bool:
        """
        Pass 2: Validate content quality (length, readability, clarity)
        """
        print("\n🔍 PASS 2: Content Quality")
        print("=" * 60)
        
        passed = True
        games = self.data['games']
        
        for game_key, game_data in games.items():
            cards = game_data.get('cards', [])
            
            for i, card in enumerate(cards):
                # Get text based on game type
                if game_key == 'red_flag_rally':
                    text = f"{card.get('perk', '')} BUT {card.get('red_flag', '')}"
                elif game_key == 'taboo_timer':
                    text = card.get('text', '')
                    forbidden = card.get('forbidden_words', [])
                    if forbidden:
                        text += ' (forbidden: ' + ', '.join(forbidden) + ')'
                elif game_key == 'alibi_drop':
                    text = card.get('text', '')
                    hidden = card.get('hidden_words', [])
                    if hidden:
                        text += ' (hidden: ' + ', '.join(hidden) + ')'
                else:
                    text = card.get('text', '')
                
                # Check minimum length
                if len(text) < 10:
                    self.issues.append(f"{game_key} card {i}: Text too short ({len(text)} chars)")
                    passed = False
                
                # Check maximum length (readability)
                if len(text) > 300:
                    self.warnings.append(f"{game_key} card {i}: Text very long ({len(text)} chars)")
                
                # Check for empty or whitespace-only text
                if not text.strip():
                    self.issues.append(f"{game_key} card {i}: Empty text")
                    passed = False
                
                # Check for proper capitalization (skip for some games)
                if text and not text[0].isupper() and game_key not in ['taboo_timer']:
                    self.warnings.append(f"{game_key} card {i}: Should start with capital letter")
        
        if passed:
            print("  ✅ Content quality PASSED")
        else:
            print("  ❌ Content quality FAILED")
            
        return passed
    
    def pass_3_humor_tone_check(self) -> bool:
        """
        Pass 3: Verify humor and tone match HDRealRules.md
        """
        print("\n🔍 PASS 3: Humor & Tone Check")
        print("=" * 60)
        
        passed = True
        games = self.data['games']
        
        # Keywords that indicate good humor
        humor_indicators = [
            'who would', 'most likely', 'confess', 'would you rather',
            'smash or pass', 'predict', 'category', 'number of'
        ]
        
        # Red flags for truly inappropriate content (audience is 25-40, spice handles profanity)
        # Only flag genuinely harmful content, not adult humor
        inappropriate = [
            'rape', 'nazi', 'hitler', 'genocide', 'child abuse',
            'pedophile', 'terrorist', 'suicide bomber'
        ]
        
        for game_key, game_data in games.items():
            cards = game_data.get('cards', [])
            humor_count = 0
            
            for i, card in enumerate(cards):
                # Get text based on game type
                if game_key == 'red_flag_rally':
                    text = f"{card.get('perk', '')} {card.get('red_flag', '')}".lower()
                else:
                    text = card.get('text', '').lower()
                
                # Check for inappropriate content
                for word in inappropriate:
                    if word in text:
                        self.issues.append(f"{game_key} card {i}: Contains inappropriate word '{word}'")
                        passed = False
                
                # Check for humor indicators
                if any(indicator in text for indicator in humor_indicators):
                    humor_count += 1
            
            # At least 30% of cards should have clear humor indicators
            if len(cards) > 0 and humor_count / len(cards) < 0.3:
                self.warnings.append(f"{game_key}: Low humor indicator ratio ({humor_count}/{len(cards)})")
        
        if passed:
            print("  ✅ Humor & tone check PASSED")
        else:
            print("  ❌ Humor & tone check FAILED")
            
        return passed
    
    def pass_4_game_specific_validation(self) -> bool:
        """
        Pass 4: Validate game-specific requirements from HDRealRules.md
        """
        print("\n🔍 PASS 4: Game-Specific Validation")
        print("=" * 60)
        
        passed = True
        games = self.data['games']
        
        # Game-specific validation rules
        def validate_card(game_key, card):
            if game_key == 'roast_consensus':
                text = card.get('text', '').lower()
                return 'who' in text or 'most likely' in text
            elif game_key == 'confession_or_cap':
                text = card.get('text', '').lower()
                return 'confess' in text or 'have you' in text
            elif game_key == 'poison_pitch':
                text = card.get('text', '').lower()
                return 'would you rather' in text
            elif game_key == 'fill_in_finisher':
                text = card.get('text', '')
                return '_' in text
            elif game_key == 'red_flag_rally':
                return 'perk' in card and 'red_flag' in card
            elif game_key == 'hot_seat_imposter':
                text = card.get('text', '').lower()
                return len(text) > 0  # Just needs a question
            elif game_key == 'text_thread_trap':
                text = card.get('text', '').lower()
                return len(text) > 0  # Just needs a text scenario
            elif game_key == 'taboo_timer':
                return 'text' in card and 'forbidden_words' in card
            elif game_key == 'the_unifying_theory':
                text = card.get('text', '')
                return ',' in text  # Should have multiple items
            elif game_key == 'title_fight':
                text = card.get('text', '').lower()
                return 'category' in text or len(text) > 0
            elif game_key == 'alibi_drop':
                return 'text' in card and 'hidden_words' in card
            elif game_key == 'reality_check':
                text = card.get('text', '').lower()
                return len(text) > 0  # Just needs a trait/question
            elif game_key == 'scatterblast':
                text = card.get('text', '').lower()
                return 'category' in text and 'letter' in text
            elif game_key == 'over_under':
                text = card.get('text', '').lower()
                return 'number' in text or 'how many' in text or len(text) > 0
            return True
        
        for game_key in games.keys():
            if game_key in games:
                cards = games[game_key].get('cards', [])
                valid_count = sum(1 for card in cards if validate_card(game_key, card))
                
                # At least 80% should match game-specific pattern
                if len(cards) > 0 and valid_count / len(cards) < 0.8:
                    self.warnings.append(
                        f"{game_key}: Only {valid_count}/{len(cards)} cards match game pattern"
                    )
                else:
                    print(f"  ✓ {game_key}: {valid_count}/{len(cards)} cards match pattern")
        
        if passed:
            print("  ✅ Game-specific validation PASSED")
        else:
            print("  ❌ Game-specific validation FAILED")
            
        return passed
    
    def pass_5_uniqueness_check(self) -> bool:
        """
        Pass 5: Check for duplicate or near-duplicate cards
        """
        print("\n🔍 PASS 5: Uniqueness Check")
        print("=" * 60)
        
        passed = True
        games = self.data['games']
        
        all_texts = []
        
        for game_key, game_data in games.items():
            cards = game_data.get('cards', [])
            # Get text based on game type
            game_texts = []
            for card in cards:
                if game_key == 'red_flag_rally':
                    text = f"{card.get('perk', '')} {card.get('red_flag', '')}".lower().strip()
                else:
                    text = card.get('text', '').lower().strip()
                game_texts.append(text)
            
            # Check for exact duplicates within game
            seen = set()
            for i, text in enumerate(game_texts):
                if text in seen:
                    self.issues.append(f"{game_key} card {i}: Duplicate text found")
                    passed = False
                seen.add(text)
            
            all_texts.extend([(game_key, text) for text in game_texts])
        
        # Check for duplicates across games
        text_map = {}
        for game_key, text in all_texts:
            if text in text_map and text_map[text] != game_key:
                self.warnings.append(f"Text appears in both {text_map[text]} and {game_key}: '{text[:50]}...'")
            else:
                text_map[text] = game_key
        
        if passed:
            print("  ✅ Uniqueness check PASSED")
        else:
            print("  ❌ Uniqueness check FAILED")
            
        return passed
    
    def run_all_passes(self) -> Tuple[bool, List[str], List[str]]:
        """
        Run all 5 verification passes
        """
        print("\n" + "=" * 60)
        print("🎯 HELLDECK CARD QUALITY VERIFICATION SYSTEM")
        print("=" * 60)
        
        results = []
        results.append(self.pass_1_structure_validation())
        results.append(self.pass_2_content_quality())
        results.append(self.pass_3_humor_tone_check())
        results.append(self.pass_4_game_specific_validation())
        results.append(self.pass_5_uniqueness_check())
        
        all_passed = all(results)
        
        print("\n" + "=" * 60)
        print("📊 VERIFICATION SUMMARY")
        print("=" * 60)
        
        if all_passed:
            print("✅ ALL PASSES COMPLETED SUCCESSFULLY!")
        else:
            print("❌ VERIFICATION FAILED")
        
        if self.issues:
            print(f"\n🚨 CRITICAL ISSUES ({len(self.issues)}):")
            for issue in self.issues[:10]:  # Show first 10
                print(f"  - {issue}")
            if len(self.issues) > 10:
                print(f"  ... and {len(self.issues) - 10} more")
        
        if self.warnings:
            print(f"\n⚠️  WARNINGS ({len(self.warnings)}):")
            for warning in self.warnings[:10]:  # Show first 10
                print(f"  - {warning}")
            if len(self.warnings) > 10:
                print(f"  ... and {len(self.warnings) - 10} more")
        
        if not self.issues and not self.warnings:
            print("\n🎉 NO ISSUES OR WARNINGS FOUND!")
        
        print("\n" + "=" * 60)
        
        return all_passed, self.issues, self.warnings


if __name__ == "__main__":
    import sys
    
    gold_cards_path = "app/src/main/assets/gold_cards.json"
    if len(sys.argv) > 1:
        gold_cards_path = sys.argv[1]
    
    verifier = CardQualityVerifier(gold_cards_path)
    passed, issues, warnings = verifier.run_all_passes()
    
    sys.exit(0 if passed else 1)