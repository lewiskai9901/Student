#!/usr/bin/env python3
"""Phase 4A: 从 frontend/src/router/index.ts 精确删除 5 个 EDU 段落,
   替换为单行占位注释. 仅 literal 行索引操作, 非 sed, 非 regex."""
from __future__ import annotations
from pathlib import Path

PATH = Path(__file__).resolve().parents[1] / "frontend/src/router/index.ts"

# (start_line, end_line, marker) — 1-indexed, inclusive
# 注: 按代码现状从后往前删, 否则前面删完行号会漂移
SEGMENTS = [
    (148, 315, "      // EDU routes (my-class / academic / student) loaded dynamically via bootstrap.ts"),
    (810, 915, "      // EDU routes (teaching) loaded dynamically via bootstrap.ts"),
    (1128, 1175, "      // EDU routes (dormitory) loaded dynamically via bootstrap.ts"),
]

def main():
    lines = PATH.read_text(encoding="utf-8").splitlines(keepends=True)
    # 按 start 倒序处理
    for start, end, marker in sorted(SEGMENTS, key=lambda x: -x[0]):
        # 1-indexed 转 0-indexed
        del lines[start - 1 : end]
        lines.insert(start - 1, marker + "\n")
    PATH.write_text("".join(lines), encoding="utf-8")
    print(f"[phase4a] trimmed {len(SEGMENTS)} EDU segments from {PATH.name}")

if __name__ == "__main__":
    main()
