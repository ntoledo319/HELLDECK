#!/usr/bin/env python3
"""
Offline lexicon linter for HELLDECK Lexicon V2 files.

Scans app/src/main/assets/lexicons_v2/*.json and reports:
 - leading/trailing punctuation
 - non-ASCII characters (suggest higher locality)
 - emoji presence
 - article collisions (text begins with a/an/the/some but needs_article != none)

Usage:
  python tools/lexicon_lint.py
"""

from __future__ import annotations

import json
import re
import sys
from pathlib import Path
from typing import Iterable

LEX_DIR = Path("app/src/main/assets/lexicons_v2")
ARTICLES = ("a ", "an ", "the ", "some ")
EMOJI_RE = re.compile(r"[\U0001F300-\U0001FAFF\U00002600-\U000026FF\U00002700-\U000027BF]")


def is_ascii(s: str) -> bool:
    try:
        s.encode("ascii")
        return True
    except UnicodeEncodeError:
        return False


def lint_file(path: Path) -> list[str]:
    msgs: list[str] = []
    try:
        data = json.loads(path.read_text(encoding="utf-8"))
    except Exception as ex:
        return [f"[ERROR] {path.name}: cannot parse JSON ({ex})"]

    entries = data.get("entries", [])
    for i, e in enumerate(entries):
        txt = (e.get("text") or "").strip()
        if not txt:
            msgs.append(f"[ERROR] {path.name}#{i}: empty text")
            continue
        if txt[0] in ",.;:!?" or txt.endswith((",", ".", ";", ":", "!", "?")):
            msgs.append(f"[WARN] {path.name}#{i}: suspicious punctuation at edges -> '{txt}'")
        if not is_ascii(txt):
            msgs.append(
                f"[INFO] {path.name}#{i}: non-ASCII characters; ensure locality is set appropriately"
            )
        if EMOJI_RE.search(txt):
            msgs.append(f"[INFO] {path.name}#{i}: emoji present; double-check tone/locality")

        needs = (e.get("needs_article") or "none").lower()
        lower = txt.lower()
        if lower.startswith(ARTICLES) and needs != "none":
            msgs.append(
                f"[WARN] {path.name}#{i}: article collision (needs_article={needs}) -> '{txt}'"
            )

    return msgs


def main(argv: Iterable[str] | None = None) -> int:
    if not LEX_DIR.is_dir():
        print(f"[ERROR] Lexicon dir not found: {LEX_DIR}")
        return 2
    files = sorted(LEX_DIR.glob("*.json"))
    total = 0
    warn_count = 0
    error_count = 0
    for f in files:
        total += 1
        msgs = lint_file(f)
        for m in msgs:
            print(m)
        warn_count += sum(m.startswith("[WARN]") for m in msgs)
        error_count += sum(m.startswith("[ERROR]") for m in msgs)
    if warn_count == 0 and error_count == 0:
        print(f"[OK] {total} lexicons checked, no issues found.")
        return 0
    print(f"[DONE] {total} lexicons checked, {warn_count} warnings, {error_count} errors.")
    return 1 if error_count else 0


if __name__ == "__main__":
    sys.exit(main())
