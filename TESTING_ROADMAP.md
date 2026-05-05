# HELLDECK: Android Testing Modernization Roadmap

This roadmap defines the multi-agent strategy for upgrading the testing infrastructure of the HELLDECK project using **Claude, Gemini, Windsurf, and Codex.**

---

## 🎯 Objective
Enable high-performance device mirroring and reliable UI automation to reduce manual testing overhead by 60%+.

---

## 🛠️ Phase 1: High-Performance Mirroring
**Persona:** `startup-cto` (Claude Code)
**Skills:** `senior-devops`, `google-workspace-cli`
**Action:** Install and configure **Scrcpy**.

1.  **Installation:** `brew install scrcpy`
2.  **Configuration:**
    *   Set up `adb` over TCP for wireless mirroring.
    *   Create a shortcut script for 60FPS mirroring: `scrcpy --max-fps=60 --bit-rate=8M`.
3.  **Verification:** Successfully mirror a physical device with <50ms latency.

---

## 🤖 Phase 2: Automated UI Testing
**Persona:** `senior-qa` (Gemini CLI / Claude Code)
**Skills:** `maestro-qa`, `playwright-pro` (adapted)
**Action:** Initialize **Maestro**.

1.  **Installation:** `curl -Ls "https://get.maestro.mobile.dev" | bash`
2.  **Setup:**
    *   Create `HELLDECK/testing/flows/` directory.
    *   Draft the first YAML flow for the Login/Main Screen.
3.  **Verification:** Run `maestro test testing/flows/login.yaml`.

---

## 🎨 Phase 3: Visual & IDE Iteration
**Persona:** `senior-frontend` (Windsurf)
**Skills:** `a11y-audit`, `ui-design-system`
**Action:** Real-time test authoring.

1.  **Context:** Windsurf uses its `.mdc` rules to provide real-time suggestions for Maestro YAML syntax.
2.  **Action:** Use Windsurf to refine flows while looking at the source code in the `app/` and `heimdall-frontend/` directories.
3.  **Verification:** Ensure UI components have proper `content-description` for stable testing.

---

## 🚀 Phase 4: Continuous Quality
**Persona:** `devops-engineer` (Codex)
**Skills:** `ci-cd-pipeline-builder`, `release-manager`
**Action:** Background Automation.

1.  **Action:** Create a GitHub Action or local script that triggers the Maestro suite on every build.
2.  **Notification:** Set up an alert if any flow in `testing/flows/` fails.

---

## 📝 Rules for Handoff

*   **Claude Code:** Execute the heavy installations and core config changes.
*   **Windsurf:** Use for refining the actual YAML test files within the IDE.
*   **Gemini CLI:** Use for high-level strategy and troubleshooting any SDK conflicts.
*   **Codex:** Use for the final CI/CD integration.
