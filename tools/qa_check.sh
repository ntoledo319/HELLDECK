#!/usr/bin/env bash
set -euo pipefail

# Offline QA gate for generator quality and assets
# - Lints lexicons
# - Generates a small set of audits
# - Diffs against frozen baselines

ROOT_DIR=$(cd "$(dirname "$0")/.." && pwd)
cd "$ROOT_DIR"

echo "[QA] Linting lexicons..."
python3 tools/lexicon_lint.py || LINT_STATUS=$? || true
LINT_STATUS=${LINT_STATUS:-0}

echo "[QA] Running quick audits..."
declare -a GAMES=(
  POISON_PITCH
  RED_FLAG_RALLY
  ROAST_CONSENSUS
  ODD_ONE_OUT
  SCATTERBLAST
)

COUNT=${COUNT:-10}
SEED=${SEED:-123}
SPICE=${SPICE:-2}

for game in "${GAMES[@]}"; do
  echo " - Auditing ${game} (count=${COUNT}, seed=${SEED})"
  ./gradlew :app:cardAudit -Pgame="$game" -Pcount="$COUNT" -Pseed="$SEED" -Pspice="$SPICE" >/dev/null
done

echo "[QA] Diffing audits against baselines..."
python3 tools/card_audit_diff.py || DIFF_STATUS=$? || true
DIFF_STATUS=${DIFF_STATUS:-0}

echo "[QA] Summary: lint=${LINT_STATUS} diff=${DIFF_STATUS}"
if [[ $LINT_STATUS -ne 0 || $DIFF_STATUS -ne 0 ]]; then
  echo "[QA] FAIL — see output above for details." >&2
  exit 1
fi

echo "[QA] PASS"
