#!/usr/bin/env python3
"""dedup_skeletons.py <deck.json> [...] — CONTENT_BIBLE Law 7.

Within-deck: no skeleton slug > 2 cards. Cross-deck + within: near-duplicate text scan
via normalized 3-word shingle Jaccard (>0.55 = same joke). Exit 1 on violations.
"""
import json
import re
import sys
from collections import Counter
from itertools import combinations
from pathlib import Path

STOP = set('a an the i my your their our of to in at on for with and or is was would has have '
           'you they we it that this'.split())

def shingles(text: str) -> frozenset:
    words = [w for w in re.sub(r'[^a-z0-9 ]', ' ', text.lower()).split() if w not in STOP]
    if len(words) < 3:
        return frozenset([tuple(words)])
    return frozenset(tuple(words[i:i + 3]) for i in range(len(words) - 2))

def main() -> int:
    cards = []  # (deck, id, text, skeleton)
    for path in sys.argv[1:]:
        d = json.loads(Path(path).read_text())
        for c in d.get('cards', []):
            cards.append((d['deck'], c['id'], c.get('text', ''), c.get('skeleton', '')))
    errs = []
    per_deck: dict[str, Counter] = {}
    for deck, _cid, _text, skel in cards:
        per_deck.setdefault(deck, Counter())[skel] += 1
    for deck, counts in per_deck.items():
        for skel, n in counts.items():
            if n > 2:
                errs.append(f'{deck}: skeleton "{skel}" used {n}x (budget 2)')
    shingled = [(deck, cid, shingles(text)) for deck, cid, text, _ in cards]
    for (d1, id1, s1), (d2, id2, s2) in combinations(shingled, 2):
        if not s1 or not s2:
            continue
        inter = len(s1 & s2)
        if inter == 0:
            continue
        j = inter / len(s1 | s2)
        if j > 0.55:
            errs.append(f'near-duplicate ({j:.2f}): {d1}/{id1} ~ {d2}/{id2}')
    if errs:
        print(f'FAIL: {len(errs)} dedup violations')
        for e in errs[:50]:
            print(f'  - {e}')
        return 1
    print(f'OK: {len(cards)} cards, skeleton budgets respected, no near-duplicates')
    return 0

if __name__ == '__main__':
    sys.exit(main())
