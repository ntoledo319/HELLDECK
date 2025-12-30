#!/usr/bin/env python3
"""
Summarize AI judge metrics (humor, sense, understandable) per game from quality JSONs.

Usage:
  python tools/quality_ai_summary.py [--seed 12345] [--count 80]

Writes docs/quality_ai_summary.md
"""

from __future__ import annotations

import argparse
import json
from collections import defaultdict
from pathlib import Path

IN_DIR = Path("app/app/build/reports/cardlab/quality")
OUT_MD = Path("docs/quality_ai_summary.md")


def parse_args():
    p = argparse.ArgumentParser()
    p.add_argument("--seed", type=str, default=None, help="Filter runs by seed")
    p.add_argument("--count", type=str, default=None, help="Filter runs by count")
    return p.parse_args()


def to_float(x):
    try:
        return float(x)
    except Exception:
        return None


def main() -> int:
    args = parse_args()
    if not IN_DIR.is_dir():
        print(f"[WARN] Input directory not found: {IN_DIR}")
        return 0

    per_game = defaultdict(
        lambda: {"humor": [], "sense": [], "understand": [], "n_rows": 0, "n_ai": 0}
    )

    for p in sorted(IN_DIR.glob("quality_*.json")):
        name = p.stem  # quality_<GAME>_<SEED>_<COUNT>
        parts = name.split("_")
        if len(parts) < 4:
            continue
        # Last two segments are seed and count; game may contain underscores
        seed = parts[-2]
        count = parts[-1]
        game = "_".join(parts[1:-2])
        if args.seed and seed != args.seed:
            continue
        if args.count and count != args.count:
            continue
        try:
            data = json.loads(p.read_text(encoding="utf-8"))
        except Exception:
            continue
        rows = data.get("rows") or []
        for r in rows:
            per_game[game]["n_rows"] += 1
            m = r.get("metrics") or {}
            h = to_float(m.get("aiHumor"))
            s = to_float(m.get("aiSense"))
            u = to_float(m.get("aiUnderstandable"))
            if h is None and s is None and u is None:
                continue
            per_game[game]["n_ai"] += 1
            if h is not None:
                per_game[game]["humor"].append(h)
            if s is not None:
                per_game[game]["sense"].append(s)
            if u is not None:
                per_game[game]["understand"].append(u)

    lines = [
        "# AI Judge Summary (Humor & Sense)\n",
        "Aggregated from per-game quality JSONs. Values are 0..1 averages; only rows with AI judgments are included.\n",
    ]

    for game in sorted(per_game.keys()):
        g = per_game[game]
        n_rows = g["n_rows"]
        n_ai = g["n_ai"]
        if n_rows == 0:
            continue
        avg_h = sum(g["humor"]) / len(g["humor"]) if g["humor"] else None
        avg_s = sum(g["sense"]) / len(g["sense"]) if g["sense"] else None
        avg_u = sum(g["understand"]) / len(g["understand"]) if g["understand"] else None
        lines.append(f"## {game}\n")
        lines.append(f"- Rows: {n_rows} | With AI: {n_ai}\n")
        lines.append(
            f"- Avg humor01: {avg_h:.3f}\n"
            if avg_h is not None
            else "- Avg humor01: (no AI data)\n"
        )
        lines.append(
            f"- Avg makesSense01: {avg_s:.3f}\n"
            if avg_s is not None
            else "- Avg makesSense01: (no AI data)\n"
        )
        lines.append(
            f"- Avg understandable01: {avg_u:.3f}\n\n"
            if avg_u is not None
            else "- Avg understandable01: (no AI data)\n\n"
        )

    OUT_MD.write_text("\n".join(lines), encoding="utf-8")
    print(f"[OK] Wrote {OUT_MD}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
