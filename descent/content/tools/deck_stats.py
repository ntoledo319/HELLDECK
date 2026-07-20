#!/usr/bin/env python3
"""deck_stats.py <deck.json> [...] — CONTENT_BIBLE Law 10 quotas (report + exit 1 on hard breaches).

Hard: observational register <=35%; phone/app references <=20%.
Report: exposure spread vs 15/20/30/20/15 target, register mix, chaos mix.
"""
import json
import re
import sys
from collections import Counter
from pathlib import Path

PHONE_RX = re.compile(r'\b(phone|app|text|dm|screenshot|screen.?time|notification|wifi|selfie|'
                      r'group ?chat|profile|post|scroll|online|browser|search history|camera roll|'
                      r'podcast|stream|tweet|instagram|tiktok)\b', re.I)
TARGET = {1: 15, 2: 20, 3: 30, 4: 20, 5: 15}

def main() -> int:
    failed = False
    for path in sys.argv[1:]:
        d = json.loads(Path(path).read_text())
        cards = d.get('cards', [])
        n = len(cards) or 1
        regs = Counter(c.get('register') for c in cards)
        exps = Counter(c.get('exposure') for c in cards)
        chaos = Counter(c.get('chaos') for c in cards)
        phone = sum(1 for c in cards if PHONE_RX.search(c.get('text', '')))
        obs_pct = 100 * regs.get('observational', 0) / n
        phone_pct = 100 * phone / n
        print(f"== {d['deck']} ({n} cards)")
        print(f"   exposure: " + '  '.join(f'E{t}:{100*exps.get(t,0)/n:.0f}%(target {TARGET[t]}%)' for t in range(1, 6)))
        print(f"   chaos:    " + '  '.join(f'C{t}:{chaos.get(t,0)}' for t in range(3, 6)))
        print(f"   registers: {dict(regs.most_common())}")
        print(f"   observational {obs_pct:.0f}% (max 35) | phone/app refs {phone_pct:.0f}% (max 20)")
        if obs_pct > 35:
            print(f'   HARD FAIL: observational {obs_pct:.0f}% > 35%')
            failed = True
        if phone_pct > 20:
            print(f'   HARD FAIL: phone/app {phone_pct:.0f}% > 20%')
            failed = True
    return 1 if failed else 0

if __name__ == '__main__':
    sys.exit(main())
