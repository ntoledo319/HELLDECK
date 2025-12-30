# Card Audit Baselines (2025-11-03)

Frozen CSV outputs from `./gradlew :app:cardAudit` on key seeds. Regenerate by running the same commands and diffing against these snapshots.

**Note:** Legacy game baselines (ODD_ONE_OUT) have been archived to `legacy_archived/` directory as these games are no longer part of the official 14-game lineup.

| Game | Command | Baseline |
|------|---------|----------|
| Poison Pitch | `./gradlew :app:cardAudit -Pgame=POISON_PITCH -Pcount=10 -Pseed=123` | `audit_POISON_PITCH_123_10_2025-11-03.csv` |
| Poison Pitch | `./gradlew :app:cardAudit -Pgame=POISON_PITCH -Pcount=5 -Pseed=12` | `audit_POISON_PITCH_12_5_2025-11-03.csv` |
| Poison Pitch | `./gradlew :app:cardAudit -Pgame=POISON_PITCH -Pcount=3 -Pseed=7` | `audit_POISON_PITCH_7_3_2025-11-03.csv` |
| Roast Consensus | `./gradlew :app:cardAudit -Pgame=ROAST_CONSENSUS -Pcount=10 -Pseed=456` | `audit_ROAST_CONSENSUS_456_10_2025-11-03.csv` |
| Roast Consensus | `./gradlew :app:cardAudit -Pgame=ROAST_CONSENSUS -Pcount=5 -Pseed=456` | `audit_ROAST_CONSENSUS_456_5_2025-11-03.csv` |
| Roast Consensus | `./gradlew :app:cardAudit -Pgame=ROAST_CONSENSUS -Pcount=3 -Pseed=44` | `audit_ROAST_CONSENSUS_44_3_2025-11-03.csv` |

Copy new CSVs here when quality changes are approved to keep history deterministic.

### Regression check helper

Run `python tools/card_audit_diff.py` after generating fresh audits to compare them against these baselines. The script exits with status 1 if any CSV diverges or is missing, making it easy to wire into CI.
