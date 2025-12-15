#!/usr/bin/env python3
"""
HELLDECK 2.0 Duplicate Detection Tool

Scans for duplicate class names, screen routes, and core components.
Fails build if duplicates are found.
"""

import re
import sys
import glob
import os
from collections import defaultdict

ROOT = "app/src/main/java"

if not os.path.exists(ROOT):
    print(f"Error: {ROOT} does not exist")
    sys.exit(1)

files = glob.glob(ROOT + "/**/*.kt", recursive=True)

# Regex patterns
class_re = re.compile(r'^\s*(data\s+class|class|object|interface)\s+([A-Za-z0-9_]+)\b', re.M)
fun_re = re.compile(r'^\s*(?:@Composable\s+)?fun\s+([A-Za-z0-9_]+)\s*\(', re.M)

# Core names that MUST have exactly one instance
core_names = {
    "GameNightViewModel", "InteractionRenderer", "HelldeckDb",
    "GameEngine", "RouteAudit"
}

# Screen functions that MUST have exactly one instance
core_screens = {
    "HouseRulesScreen", "GroupDnaScreen", "PacksScreen", "RolesScreen",
    "HighlightsScreen", "DebugHarnessScreen",
    "HomeScreen", "LobbyScreen", "RoundScreen", "FeedbackScreen",
    "StatsScreen", "SettingsScreen", "CardLabScreen"
}

classes = defaultdict(list)
funs = defaultdict(list)

for fp in files:
    try:
        with open(fp, "r", encoding="utf-8") as f:
            text = f.read()
    except Exception as e:
        print(f"Warning: Could not read {fp}: {e}")
        continue

    for _, name in class_re.findall(text):
        if name in core_names:
            classes[name].append(fp)

    for name in fun_re.findall(text):
        if name in core_screens:
            funs[name].append(fp)

errors = []

# Check for duplicate core types
for name, paths in classes.items():
    if len(paths) > 1:
        errors.append(f"Duplicate core type '{name}':\n  " + "\n  ".join(paths))

# Check for duplicate core functions
for name, paths in funs.items():
    if len(paths) > 1:
        errors.append(f"Duplicate core function '{name}':\n  " + "\n  ".join(paths))

# Check route duplicates by parsing Screen.kt if exists
screen_files = [f for f in files if f.endswith("Screen.kt") and "ui/nav" in f.replace("\\", "/")]
if screen_files:
    try:
        with open(screen_files[0], "r", encoding="utf-8") as f:
            text = f.read()
        routes = re.findall(r'Screen\("([^"]+)"\)', text)
        route_counts = defaultdict(int)
        for r in routes:
            route_counts[r] += 1
        dup_routes = [r for r, c in route_counts.items() if c > 1]
        if dup_routes:
            errors.append(f"Duplicate route strings in Screen.kt: {dup_routes}")
    except Exception as e:
        print(f"Warning: Could not parse Screen.kt: {e}")

if errors:
    print("\n\n".join(errors))
    sys.exit(1)

print("dupcheck: OK - No duplicates found")
sys.exit(0)
