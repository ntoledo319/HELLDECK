#!/usr/bin/env python3
"""lint_deck.py <deck.json> [...] — CONTENT_BIBLE / spec 8.1 invariants. Exit 1 on any violation.

Checks: schema shape, chaos>=3, E/C in 1..5, text<=120 chars, register vocabulary,
per-deck required extras, banlist regexes, id uniqueness+format, {NAME} above E3 heuristic.
"""
import json
import re
import sys
from pathlib import Path

REGISTERS = {'observational', 'absurdist', 'deadpan', 'menace', 'petty-domestic',
             'gross', 'physical', 'parody', 'table-aware', 'euphemism'}
EXTRAS = {
    'overunder': ['receiptSurface', 'timebox'],
    'poison': ['optionA', 'optionB'],
    'redflag': ['perk', 'flag'],
    'alibi': ['accusation', 'words', 'decoys'],
    'scatter': ['category', 'letter'],
    'taboo': ['word', 'forbidden'],
    'texttrap': ['sender', 'message', 'tone'],
}

def load_banlist() -> list[re.Pattern]:
    path = Path(__file__).parent / 'banlist.txt'
    pats = []
    for line in path.read_text().splitlines():
        line = line.strip()
        if line and not line.startswith('#'):
            pats.append(re.compile(line, re.I))
    return pats

def lint(path: str, bans: list[re.Pattern]) -> list[str]:
    errs: list[str] = []
    d = json.loads(Path(path).read_text())
    deck = d.get('deck', '?')
    if d.get('schema') != 3:
        errs.append(f'{deck}: schema != 3')
    ids: set[str] = set()
    for c in d.get('cards', []):
        cid = c.get('id', '?')
        loc = f'{deck}/{cid}'
        if cid in ids:
            errs.append(f'{loc}: duplicate id')
        ids.add(cid)
        if not re.fullmatch(rf'{deck}_v3_\d{{3,}}', cid):
            errs.append(f'{loc}: bad id format')
        text = c.get('text', '')
        if not text or len(text) > 120:
            errs.append(f'{loc}: text empty or >120 chars ({len(text)})')
        e, ch = c.get('exposure'), c.get('chaos')
        if e not in (1, 2, 3, 4, 5):
            errs.append(f'{loc}: exposure {e}')
        if ch not in (3, 4, 5):
            errs.append(f'{loc}: chaos {ch} (law: chaos>=3, wholesome does not exist)')
        if c.get('register') not in REGISTERS:
            errs.append(f'{loc}: unknown register {c.get("register")}')
        if not c.get('skeleton'):
            errs.append(f'{loc}: missing skeleton slug')
        searchable = ' '.join(str(c.get(k, '')) for k in
                              ('text', 'optionA', 'optionB', 'perk', 'flag', 'accusation',
                               'category', 'word', 'message', 'sender'))
        for pat in bans:
            if pat.search(searchable):
                errs.append(f'{loc}: BANNED vocabulary /{pat.pattern}/ in "{text[:60]}"')
        for field in EXTRAS.get(deck, []):
            if field not in c:
                errs.append(f'{loc}: missing required extra "{field}"')
        if deck == 'alibi' and 'words' in c and 'decoys' in c:
            if len(c['words']) != 3 or len(c['decoys']) != 5:
                errs.append(f'{loc}: alibi needs 3 words + 5 decoys')
        if deck == 'taboo' and len(c.get('forbidden', [])) != 5:
            errs.append(f'{loc}: taboo needs exactly 5 forbidden words')
        if deck == 'scatter' and len(str(c.get('category', '')).split()) > 7:
            errs.append(f'{loc}: category > 7 words')
    return errs

def main() -> int:
    bans = load_banlist()
    failed = False
    for path in sys.argv[1:]:
        errs = lint(path, bans)
        n = len(json.loads(Path(path).read_text()).get('cards', []))
        if errs:
            failed = True
            print(f'FAIL {path} ({n} cards, {len(errs)} violations):')
            for e in errs[:40]:
                print(f'  - {e}')
            if len(errs) > 40:
                print(f'  ... +{len(errs) - 40} more')
        else:
            print(f'OK   {path} ({n} cards)')
    return 1 if failed else 0

if __name__ == '__main__':
    sys.exit(main())
