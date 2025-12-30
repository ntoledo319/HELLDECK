#!/usr/bin/env python3
"""
HELLDECK 2.0 Dead Click Detection Tool

Scans for empty onClick handlers, TODO placeholders, and unhandled navigation.
Fails build if dead interactions are found.
"""

import glob
import os
import re
import sys

ROOT = "app/src/main/java"

if not os.path.exists(ROOT):
    print(f"Error: {ROOT} does not exist")
    sys.exit(1)

files = glob.glob(ROOT + "/**/*.kt", recursive=True)

# Patterns to detect
empty_onclick_re = re.compile(r"onClick\s*=\s*\{\s*\}")
todo_comment_re = re.compile(r"//\s*TODO(?!\()", re.I)
todo_function_re = re.compile(r"TODO\(\)")

issues = []

for fp in files:
    # Skip test files
    if "/test/" in fp or "/androidTest/" in fp:
        continue

    try:
        with open(fp, encoding="utf-8") as f:
            text = f.read()
            lines = text.split("\n")
    except Exception as e:
        print(f"Warning: Could not read {fp}: {e}")
        continue

    # Check for empty onClick
    for match in empty_onclick_re.finditer(text):
        line_num = text[: match.start()].count("\n") + 1
        issues.append(f"{fp}:{line_num} - Empty onClick handler")

    # Check for TODO comments
    for i, line in enumerate(lines, 1):
        if todo_comment_re.search(line):
            issues.append(f"{fp}:{i} - TODO comment found")

    # Check for TODO() function calls
    for match in todo_function_re.finditer(text):
        line_num = text[: match.start()].count("\n") + 1
        issues.append(f"{fp}:{line_num} - TODO() function call")

if issues:
    print(f"Found {len(issues)} dead click/TODO issues:\n")
    for issue in issues[:50]:  # Limit output
        print(f"  {issue}")
    if len(issues) > 50:
        print(f"\n  ... and {len(issues) - 50} more")
    sys.exit(1)

print("deadclick_check: OK - No dead clicks or TODOs found")
sys.exit(0)
