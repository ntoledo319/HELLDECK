# HELLDECK: Testing Guide

## 📱 Mirroring (Scrcpy)
To launch a high-performance mirror:
```bash
scrcpy --max-fps=60 --bit-rate=8M --always-on-top
```

## 🤖 UI Automation (Maestro)
To run the full test suite:
```bash
maestro test testing/flows/
```

### Active Flows:
- `main_launch.yaml`: Verifies app launch and initial view state.
