#!/usr/bin/env python3
"""
Aggregate quality sweep JSONs into a compact Markdown summary per game.

Usage:
  python tools/quality_summarize.py
Writes docs/quality_summary.md
"""

from __future__ import annotations

import json
from collections import Counter, defaultdict
from pathlib import Path

IN_DIR = Path("app/app/build/reports/cardlab/quality")
OUT_MD = Path("docs/quality_summary.md")


def main() -> int:
    if not IN_DIR.is_dir():
        print(f"[WARN] Input directory not found: {IN_DIR}")
        return 0

    games = defaultdict(list)
    for p in sorted(IN_DIR.glob("quality_*.json")):
        try:
            data = json.loads(p.read_text(encoding="utf-8"))
        except Exception:
            continue
        summary = data.get("summary") or {}
        game = summary.get("game") or p.stem.split("_")[1]
        games[game].append(data)

    lines: list[str] = []
    lines.append("# HELLDECK Card Quality Summary\n")
    lines.append(
        "Aggregated across all available sweeps in app/app/build/reports/cardlab/quality.\n"
    )

    for game in sorted(games.keys()):
        runs = games[game]
        if not runs:
            continue
        total = 0
        pass_sum = 0.0
        score_sum = 0.0
        issue_counter = Counter()
        for run in runs:
            s = run.get("summary") or {}
            total += int(s.get("total") or 0)
            pass_sum += float(s.get("passRate") or 0.0)
            score_sum += float(s.get("avgScore") or 0.0)
            for k, v in (s.get("topIssues") or {}).items():
                issue_counter[k] += int(v)
        avg_pass_rate = pass_sum / max(1, len(runs))
        avg_score = score_sum / max(1, len(runs))
        top_issues = ", ".join(f"{k}×{v}" for k, v in issue_counter.most_common(5)) or "(none)"
        lines.append(f"## {game}\n")
        lines.append(f"- Average pass rate: {avg_pass_rate:.1f}% across {len(runs)} run(s)\n")
        lines.append(f"- Average score: {avg_score:.3f}\n")
        lines.append(f"- Top issues: {top_issues}\n\n")

    OUT_MD.write_text("\n".join(lines), encoding="utf-8")
    print(f"[OK] Wrote {OUT_MD}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
