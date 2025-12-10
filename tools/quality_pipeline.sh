#!/usr/bin/env bash
set -euo pipefail

# Iterative quality pipeline: run batches across seeds, calibrate, and summarize.

COUNT=${COUNT:-60}
SPICE=${SPICE:-2}
TARGET=${TARGET:-0.85}
STEP=${STEP:-0.05}

SEED_BATCHES=(
  "101,102,103,104"
  "105,106,107,108"
  "301,302,303,304"
  "401,402"
)

for batch in "${SEED_BATCHES[@]}"; do
  echo "[PIPELINE] Running seeds: $batch"
  ./gradlew :app:cardQuality -Pcount=${COUNT} -Pseeds=${batch} -Pspice=${SPICE}
  python3 tools/quality_summarize.py || true
  echo "[PIPELINE] Calibrating thresholds toward ${TARGET} (step ${STEP})"
  python3 tools/calibrate_quality.py --target ${TARGET} --step ${STEP}
done

python3 tools/quality_summarize.py
echo "[PIPELINE] Done. See docs/quality_summary.md"

