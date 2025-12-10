#!/usr/bin/env bash
set -euo pipefail

# Generate audit baselines for a set of games and seeds (offline)

GAMES=(
  ROAST_CONSENSUS
  CONFESSION_OR_CAP
  POISON_PITCH
  FILL_IN_FINISHER
  RED_FLAG_RALLY
  HOT_SEAT_IMPOSTER
  TEXT_THREAD_TRAP
  TABOO_TIMER
  ODD_ONE_OUT
  TITLE_FIGHT
  ALIBI_DROP
  HYPE_OR_YIKE
  SCATTERBLAST
  MAJORITY_REPORT
)

COUNTS=(10)
SEEDS=(123 321 777 42)

DATE=$(date +%F)
OUT_DIR="docs/card_audit_baselines"
mkdir -p "$OUT_DIR"

for game in "${GAMES[@]}"; do
  for count in "${COUNTS[@]}"; do
    for seed in "${SEEDS[@]}"; do
      ./gradlew :app:cardAudit -Pgame="$game" -Pcount="$count" -Pseed="$seed" >/dev/null
      name="audit_${game}_${seed}_${count}"
      src="app/build/reports/cardlab/${name}.csv"
      if [[ -f "$src" ]]; then
        cp "$src" "${OUT_DIR}/${name}_${DATE}.csv"
        echo "Saved baseline: ${OUT_DIR}/${name}_${DATE}.csv"
      fi
    done
  done
done

echo "All baselines generated." 
