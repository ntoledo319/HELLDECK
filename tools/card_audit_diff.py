#!/usr/bin/env python3
"""
Quick diff tool for offline card audit CSVs.

This script compares the frozen baselines under `docs/card_audit_baselines/`
against freshly generated reports in `app/build/reports/cardlab/`.

Usage:
    python tools/card_audit_diff.py
    python tools/card_audit_diff.py --baseline-dir path/to/baselines --report-dir path/to/new
"""

from __future__ import annotations

import argparse
import re
import sys
from dataclasses import dataclass
from pathlib import Path
from typing import Iterable

try:
    from difflib import unified_diff
except ImportError:  # pragma: no cover
    print("Python difflib module unavailable; cannot continue.", file=sys.stderr)
    sys.exit(2)

BASELINE_SUFFIX_RE = re.compile(r"_(\d{4}-\d{2}-\d{2})(?=\.csv$)")


@dataclass
class DiffResult:
    baseline: Path
    candidate: Path | None
    diff_lines: tuple[str, ...]

    @property
    def is_missing(self) -> bool:
        return self.candidate is None

    @property
    def is_different(self) -> bool:
        return bool(self.diff_lines)


def parse_args(argv: Iterable[str] | None = None) -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Compare card audit baselines against the latest reports."
    )
    parser.add_argument(
        "--baseline-dir",
        default="docs/card_audit_baselines",
        help="Directory containing frozen baseline CSVs (default: %(default)s).",
    )
    parser.add_argument(
        "--report-dir",
        default="app/build/reports/cardlab",
        help="Directory containing freshly generated CSVs (default: %(default)s).",
    )
    parser.add_argument(
        "--max-diff-lines",
        type=int,
        default=120,
        help="Maximum number of diff lines to print per file (default: %(default)s).",
    )
    return parser.parse_args(argv)


def target_name(baseline: Path) -> str:
    return BASELINE_SUFFIX_RE.sub("", baseline.name)


def load_lines(path: Path) -> list[str]:
    with path.open("r", encoding="utf-8") as handle:
        return [line.rstrip("\n") for line in handle]


def compare_files(baseline: Path, candidate: Path | None, max_lines: int) -> DiffResult:
    if candidate is None or not candidate.exists():
        return DiffResult(baseline=baseline, candidate=None, diff_lines=())

    base_lines = load_lines(baseline)
    cand_lines = load_lines(candidate)
    diff = list(
        unified_diff(
            base_lines,
            cand_lines,
            fromfile=str(baseline),
            tofile=str(candidate),
            lineterm="",
        )
    )
    if max_lines > 0 and len(diff) > max_lines:
        diff = diff[: max_lines - 1] + ["… (diff truncated)"]
    return DiffResult(baseline=baseline, candidate=candidate, diff_lines=tuple(diff))


def main(argv: Iterable[str] | None = None) -> int:
    args = parse_args(argv)
    baseline_dir = Path(args.baseline_dir)
    report_dir = Path(args.report_dir)

    if not baseline_dir.is_dir():
        print(f"[ERROR] Baseline directory not found: {baseline_dir}", file=sys.stderr)
        return 2
    if not report_dir.is_dir():
        print(f"[ERROR] Report directory not found: {report_dir}", file=sys.stderr)
        return 2

    baselines = sorted(baseline_dir.glob("audit_*.csv"))
    if not baselines:
        print(f"[WARN] No baseline CSVs found in {baseline_dir}")
        return 0

    missing = []
    diffs = []

    for baseline in baselines:
        candidate_name = target_name(baseline)
        candidate = report_dir / candidate_name
        result = compare_files(
            baseline, candidate if candidate.exists() else None, args.max_diff_lines
        )
        if result.is_missing:
            missing.append(result)
        elif result.is_different:
            diffs.append(result)

    if not missing and not diffs:
        print(f"[OK] All {len(baselines)} audit reports match their baselines.")
        return 0

    if missing:
        print("[MISSING] Baselines without matching reports:")
        for entry in missing:
            print(f"  - {entry.baseline.name}")

    if diffs:
        print("[DIFF] Reports changed:")
        for entry in diffs:
            print(f"\n--- {entry.baseline.name} vs {entry.candidate.name}")
            for line in entry.diff_lines:
                print(line)

    return 1


if __name__ == "__main__":
    sys.exit(main())
