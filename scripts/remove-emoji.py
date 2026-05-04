#!/usr/bin/env python3
"""移除 frontend/src 下所有 .vue/.ts/.tsx 的字体图标 (emoji + Unicode 符号).

策略:
  - 表情类 emoji (📋⚡🔴✨🤖🎲...) → 直接删除 (含前后空白合并)
  - 键盘符号 (⌘⏎⎋⎵⇥) → 改文字 (Cmd/Enter/Esc/Space/Tab)
  - 方向/几何符号 (→←↑↓◀▶▾▸▼▲) → 改文字 (>>/<</上/下/v/>/v/>/v/^)
  - 数学符号 (✓✗ ✅✕) → 改文字 (√/×/已/×)
  - 警告 (⚠) → 改文字 (!)
  - 闹钟 (⏰⏱⏎ 等) → 删除
"""
import os, re, sys

REPLACE_MAP = {
    # 表情/物品 — 删除
    '📋':'', '📊':'', '📅':'', '📍':'', '📦':'', '📝':'', '📜':'', '📷':'', '📹':'', '📆':'',
    '🔴':'', '🔵':'', '🟢':'', '🟡':'', '🟠':'', '🟣':'',
    '🔄':'', '🔧':'', '🔒':'', '🔗':'', '🔓':'', '🔔':'', '🔍':'',
    '⚡':'', '✨':'', '⚖':'', '🤖':'', '🎯':'', '🎲':'', '🎭':'',
    '🏫':'', '🏢':'', '🏖':'', '🏅':'', '🏆':'',
    '👨':'', '👥':'', '👩':'', '👧':'', '👤':'', '👁':'',
    '🤝':'', '🥇':'', '🥈':'', '🥉':'',
    '🗑':'', '💬':'',
    '☀':'', '⏰':'', '⏱':'', '🕐':'',
    # 数学/勾叉 — 改文字
    '✅':'√', '✓':'√', '✗':'×', '✕':'×', '✎':'编辑', '✏':'编辑',
    '➕':'+',
    '⚠':'!',
    # 键盘符号 — 改文字
    '⌘':'Cmd', '⏎':'Enter', '⎋':'Esc', '⎵':'Space', '⇥':'Tab',
    # 方向/三角 (Unicode arrows + geometric — 严格说不是 emoji 但用户范畴一并处理)
    '→':'>', '←':'<', '↑':'↑', '↓':'↓', '↗':'↑', '↘':'↓', '↔':'<>', '↳':'->',
    '▶':'>', '◀':'<', '▾':'v', '▸':'>', '▼':'v', '▲':'^', '⏸':'||',
    '◆':'',
    # 性别符号 — 删除 (业务场景几乎不用)
    '♂':'', '♀':'', '⚥':'',
}

# 兜底: 任何 emoji 表情范围内的字符没在 map 里也直接删
EMOJI_FALLBACK_RE = re.compile(
    '[\U0001F300-\U0001F6FF\U0001F900-\U0001F9FF\U0001FA00-\U0001FA6F'
    '\U0001F1E6-\U0001F1FF]'
)

def process_file(path):
    try:
        with open(path, 'r', encoding='utf-8') as f:
            content = f.read()
    except Exception:
        return False, 0
    original = content
    total = 0
    for ch, rep in REPLACE_MAP.items():
        if ch in content:
            cnt = content.count(ch)
            content = content.replace(ch, rep)
            total += cnt
    # 兜底删除
    fallback_count = 0
    def _del(m):
        nonlocal fallback_count
        fallback_count += 1
        return ''
    content = EMOJI_FALLBACK_RE.sub(_del, content)
    total += fallback_count
    # 清理: 多余连续空格 + 残留行前空格 (但保留缩进结构, 只清行内)
    # "  全部" 这种: 把两个连续空格合并成一个 (不影响 4-space 缩进, 因为缩进在行首)
    # 简单做法: 不处理空格, 让 prettier/lint 自动 fix
    if content != original:
        with open(path, 'w', encoding='utf-8', newline='\n') as f:
            f.write(content)
        return True, total
    return False, 0

def main():
    base = sys.argv[1] if len(sys.argv) > 1 else 'frontend/src'
    changed_files = 0
    total_replacements = 0
    for root, _, files in os.walk(base):
        if 'node_modules' in root: continue
        for f in files:
            if not (f.endswith('.vue') or f.endswith('.ts') or f.endswith('.tsx')): continue
            path = os.path.join(root, f)
            ok, cnt = process_file(path)
            if ok:
                changed_files += 1
                total_replacements += cnt
                print(f'  {path}  -{cnt}')
    print(f'\n--- DONE: {changed_files} files changed, {total_replacements} chars removed/replaced ---')

if __name__ == '__main__':
    main()
