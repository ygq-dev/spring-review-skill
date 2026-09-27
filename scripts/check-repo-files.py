# -*- coding: utf-8 -*-
"""检查开源仓库基础文件是否落盘。"""
from pathlib import Path

FILES = [
    "README.md",
    "LICENSE",
    ".gitignore",
    ".gitattributes",
    ".editorconfig",
    "reports/README.md",
    "tools/README.md",
]

ROOT = Path.cwd()

print(f"{'file':<40} {'exists':<8} {'bytes':>8}")
print("-" * 60)
missing = []
for f in FILES:
    p = ROOT / f
    if p.exists():
        size = p.stat().st_size
        print(f"{f:<40} {'YES':<8} {size:>8}")
        if size == 0:
            missing.append(f + " (empty)")
    else:
        print(f"{f:<40} {'NO':<8} {'-':>8}")
        missing.append(f)

print()
if missing:
    print("MISSING / EMPTY:")
    for m in missing:
        print(f"  - {m}")
else:
    print("ALL OK")