# LLM & Card Quality Guide

This guide explains how HELLDECK ships on‑device LLMs by default and how to run full game quality checks.

## LLM: Default & Automatic

- Models are bundled in the APK under `app/src/main/assets/models/*.gguf` and copied to app storage on first run.
- The native bridge `libhelldeck_llama.so` is built with the app. If `third_party/llama.cpp` is present at build time, it links to the full runtime; otherwise a safe stub is shipped and the engine falls back.
- Paraphrasing & classification are automatic when the model is ready. No settings toggle is required.

## Build Notes

```
# Debug APK (bundles models & native lib)
./gradlew :app:assembleDebug

# Release APK
./gradlew :app:assembleRelease
```

- `.gguf` files are stored uncompressed for faster copy on first launch (`androidResources.noCompress 'gguf'`).
- JNI names are preserved via `app/proguard-rules.pro`.

## Full Game Quality Checks

Use the dedicated Gradle task to generate per‑game reports.

```
# Generate reports for all games (single seed)
./gradlew :app:cardQuality -Pcount=100 -Pseed=42 -Pspice=2

# Multiple seeds in one run (faster aggregation)
./gradlew :app:cardQuality -Pcount=80 -Pseeds=701,702,703,704,705,706,707,708 -Pspice=2

# Aggregate into docs/quality_summary.md
python3 tools/quality_summarize.py
```

- Per‑game reports: `app/app/build/reports/cardlab/quality/quality_<GAME>_<SEED>_<COUNT>.(json|csv|html)`
- Aggregate summary: `docs/quality_summary.md`

### One‑shot iterative pipeline (batch → calibrate → batch)

```
chmod +x tools/quality_pipeline.sh
COUNT=60 SPICE=2 TARGET=0.90 STEP=0.05 ./tools/quality_pipeline.sh
```

- Edits per‑game `minHumor` thresholds after each batch to move toward the target pass rate.
- Re-run the pipeline if you significantly change templates or lexicons.

## Tuning Guidance

- If `LOW_HUMOR` dominates: enrich lexicons/blueprints for that game, or nudge `minHumor` in `GameQualityProfiles.kt` by +/‑ 0.05 until pass ≥ 80%.
- If `NOT_TARGETED` in Roast: ensure templates include a “because ___” clause and add “evidence” lexicons. The evaluator also checks for targeting cues.
- If `OPTIONS_BAD` (Taboo/Scatter/AB): review blueprint `option_provider` wiring or lexicon coverage.
- If runtime falls back often: ensure `third_party/llama.cpp` is present for production builds to enable the full LLM runtime.

## Targets

- Pass rate per game: ~90%
- Average score: ≥ 0.68 (structure + humor + AI)
