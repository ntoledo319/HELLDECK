#!/usr/bin/env python3
"""
Auto-calibrate per-game minHumor thresholds in GameQualityProfiles.kt based on quality reports.

Usage:
  python tools/calibrate_quality.py --target 0.85

Reads app/app/build/reports/cardlab/quality/*.json, computes average pass rate per game,
and updates minHumor to move pass rate toward the target band (default 0.85) with a small step.
"""

from __future__ import annotations

import argparse
import json
import re
from pathlib import Path
from statistics import mean

QUALITY_DIR = Path("app/app/build/reports/cardlab/quality")
KOTLIN_FILE = Path("app/src/main/java/com/helldeck/content/validation/GameQualityProfiles.kt")
GAMEIDS_FILE = Path("app/src/main/java/com/helldeck/engine/GamesRegistry.kt")

GAME_IDS = [
    "ROAST_CONSENSUS",
    "CONFESSION_OR_CAP",
    "POISON_PITCH",
    "FILL_IN_FINISHER",
    "RED_FLAG_RALLY",
    "HOT_SEAT_IMPOSTER",
    "TEXT_THREAD_TRAP",
    "TABOO_TIMER",
    "ODD_ONE_OUT",
    "TITLE_FIGHT",
    "ALIBI_DROP",
    "HYPE_OR_YIKE",
    "SCATTERBLAST",
    "MAJORITY_REPORT",
]


def parse_args():
    p = argparse.ArgumentParser()
    p.add_argument("--target", type=float, default=0.85, help="Target pass rate (0..1)")
    p.add_argument("--step", type=float, default=0.05, help="Adjustment step for minHumor")
    return p.parse_args()


def load_pass_rates():
    rates = {gid: [] for gid in GAME_IDS}
    for path in QUALITY_DIR.glob("quality_*.json"):
        try:
            data = json.loads(path.read_text(encoding="utf-8"))
        except Exception:
            continue
        s = data.get("summary") or {}
        gid = s.get("game")
        if gid in rates:
            rates[gid].append((float(s.get("passRate") or 0.0)) / 100.0)
    return {k: (mean(v) if v else None) for k, v in rates.items()}


def parse_gameids_constants() -> dict[str, str]:
    """Return map of GAME_ID (value) -> constant name (e.g., 'ROAST_CONSENSUS' -> 'ROAST_CONS')."""
    text = GAMEIDS_FILE.read_text(encoding="utf-8")
    mapping: dict[str, str] = {}
    for m in re.finditer(r"const val\s+(\w+)\s*=\s*\"([A-Z0-9_]+)\"", text):
        const, value = m.group(1), m.group(2)
        mapping[value] = const
    return mapping


def update_min_humor(target: float, step: float) -> bool:
    pass_rates = load_pass_rates()
    content = KOTLIN_FILE.read_text(encoding="utf-8")
    id_to_const = parse_gameids_constants()
    changed = False
    for gid, pr in pass_rates.items():
        if pr is None:
            continue
        # Find the GameIds constant name for this game id value
        const = id_to_const.get(gid)
        if not const:
            continue
        # Regex to find minHumor assignment in QualityProfile for this constant
        block_re = re.compile(
            rf"(GameIds\.{re.escape(const)}\s*to\s*QualityProfile\([^\)]*?minHumor\s*=\s*)([0-9.]+)"
        )
        m = block_re.search(content)
        if not m:
            continue
        current = float(m.group(2))
        new = current
        if pr < target - 0.03:
            # Too strict → lower threshold a bit
            new = max(0.20, current - step)
        elif pr > target + 0.05:
            # Too lenient → raise threshold a bit
            new = min(0.60, current + step)
        if abs(new - current) >= 1e-6:
            content = content[: m.start(2)] + f"{new:.2f}" + content[m.end(2) :]
            changed = True
    if changed:
        KOTLIN_FILE.write_text(content, encoding="utf-8")
    return changed


def main():
    args = parse_args()
    changed = update_min_humor(args.target, args.step)
    print("[CALIBRATE] thresholds", "updated" if changed else "no-change")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
