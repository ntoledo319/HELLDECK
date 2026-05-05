---
name: taste-engineering
description: >
  Activates when the user asks to audit taste, perform a taste facelift,
  build something from scratch with taste constraints, run a
  generate-then-destroy loop, or apply taste standards to any element.
  Triggers on: "taste audit", "taste facelift", "make this tasteful",
  "this looks generic", "escape the average", "too default", "AI slop",
  "looks like a template", "specificity test", "rejection vocabulary",
  "generate and destroy".
triggers:
  - taste audit
  - taste facelift
  - make this tasteful
  - this looks generic
  - too AI
  - AI slop
  - looks like a template
  - specificity test
  - feels default
---

# Taste Engineering Skill

## Overview
This skill enforces taste at every layer of a project by orchestrating
the Taste Crew subagents and applying the project's taste standards.

## When Activated

### For Taste Audits:
1. Read `references/anti-patterns.md` for the current ban list.
2. Invoke `@agent-taste-auditor` on the specified files/components.
3. Compile the audit into a structured report with Taste Score.
4. If score < 60%, recommend a full facelift with `@agent-taste-destroyer`
   and `@agent-taste-rebuilder` in sequence.

### For Taste Facelifts:
1. Run `@agent-taste-auditor` first to identify all GENERIC/SAFE-GOOD.
2. For each flagged element, run `@agent-taste-destroyer` to find the
   underlying taste principle through the generate-destroy loop.
3. Hand principles + flagged elements to `@agent-taste-rebuilder`.
4. Final pass through `@agent-taste-critic` before presenting results.
5. Run `scripts/specificity-test.sh` on all modified files.

### For From-Scratch Builds:
1. Check if CLAUDE.md has Project Identity fields filled in.
   If empty -> STOP. Run the Taste Brief process first (see below).
2. Read `references/taste-brief.md` for the project's identity anchors.
3. Build with taste-rebuilder principles from the start.
4. Run `@agent-taste-critic` on every component before moving to the next.

### For Taste Brief Generation:
1. Interview the user with these questions:
   - What does this project believe that competitors don't?
   - Name 3 products in this space you think are generic. What do they share?
   - Name 3 things from OUTSIDE this space that capture the right feeling.
   - What should someone feel after 30 seconds with this project?
   - What is the one thing this project will NEVER do?
2. Synthesize into a Taste Brief and save to `references/taste-brief.md`.
3. Update CLAUDE.md Project Identity fields.

## Supporting Files
- `references/anti-patterns.md` — Banned patterns registry (update per project)
- `references/taste-brief.md` — Project identity anchors (generated per project)
- `scripts/specificity-test.sh` — Automated swap-test on copy and component names
- `scripts/audit.sh` — Quick grep-based pre-audit for known anti-patterns
