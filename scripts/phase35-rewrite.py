#!/usr/bin/env python3
"""Phase 3.5 literal-string rewriter - 非 sed 非 regex, 纯 str.replace.

用法:
    python scripts/phase35-rewrite.py <pkg>
    <pkg> 是 calendar / teaching / academic / student 之一。

脚本搜索 backend/src 下所有 *.java 文件,
做 4 条 literal 字符串替换 (无 regex, 无通配, 无回溯):
    com.school.management.domain.<pkg>                          -> plugins.education.domain.<pkg>
    com.school.management.application.<pkg>                     -> plugins.education.application.<pkg>
    com.school.management.infrastructure.persistence.<pkg>      -> plugins.education.infrastructure.persistence.<pkg>
    com.school.management.interfaces.rest.<pkg>                 -> plugins.education.interfaces.rest.<pkg>

与 IntelliJ Move Package 的字符串改写效果一致, 可用 git diff 完整审查。
"""
from __future__ import annotations
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1] / "backend" / "src"
NEW_BASE = "com.school.management.infrastructure.extension.plugins.education"

PAIRS_TEMPLATE = [
    ("com.school.management.domain.{pkg}",                      NEW_BASE + ".domain.{pkg}"),
    ("com.school.management.application.{pkg}",                 NEW_BASE + ".application.{pkg}"),
    ("com.school.management.infrastructure.persistence.{pkg}",  NEW_BASE + ".infrastructure.persistence.{pkg}"),
    ("com.school.management.interfaces.rest.{pkg}",             NEW_BASE + ".interfaces.rest.{pkg}"),
]


def main(pkg: str) -> None:
    pairs = [(old.format(pkg=pkg), new.format(pkg=pkg)) for old, new in PAIRS_TEMPLATE]

    files_changed = 0
    hits_per_pair = {old: 0 for old, _ in pairs}

    for path in ROOT.rglob("*.java"):
        try:
            original = path.read_text(encoding="utf-8")
        except UnicodeDecodeError:
            continue

        new_src = original
        for old, replacement in pairs:
            if old in new_src:
                hits_per_pair[old] += new_src.count(old)
                new_src = new_src.replace(old, replacement)

        if new_src != original:
            path.write_text(new_src, encoding="utf-8")
            files_changed += 1

    print(f"[phase35] package={pkg!r}")
    for old, count in hits_per_pair.items():
        print(f"  {count:>5}  {old}")
    print(f"  files touched: {files_changed}")


if __name__ == "__main__":
    if len(sys.argv) != 2 or sys.argv[1] not in {"calendar", "teaching", "academic", "student", "myclass"}:
        print("usage: python scripts/phase35-rewrite.py {calendar|teaching|academic|student|myclass}")
        sys.exit(2)
    main(sys.argv[1])
