#!/usr/bin/env python3
"""Phase 6.6 — 对 89 个 schema 迁移按领域/插件分类.

规则: 纯文件名 heuristic + 少量内容抽样. 不移动文件.
输出: Markdown 表格, 用于 database/MIGRATIONS.md.
"""
from __future__ import annotations
import re
from pathlib import Path

SCHEMA = Path(__file__).resolve().parents[1] / "database" / "schema"

# (正则, 类别) — 有序, 第一个 match 胜出
RULES = [
    (r"(complete_schema|baseline)", "baseline"),
    (r"(calendar|semester|academic_year|teaching_week)", "education/calendar"),
    (r"(teaching|offering|scheduling|exam|course|curriculum)", "education/teaching"),
    (r"(student|cohort|school_class|enrollment|attendance|academic_warning|status_change)", "education/student"),
    (r"(academic|major|direction|plan_course)", "education/academic"),
    (r"(dormitory|dorm)", "education/dormitory"),
    (r"(my[-_]?class)", "education/myclass"),
    (r"(asset)", "core/asset"),
    (r"(insp|inspection|scoring|corrective|rating|observation|response_set|v7)", "core/inspection"),
    (r"(casbin|permission|role|user_role|access_relation|relation_type)", "core/access"),
    (r"(user|users_)", "core/user"),
    (r"(place|space|occupant|universal_place|venue)", "core/place"),
    (r"(org_unit|org_type|organization)", "core/organization"),
    (r"(plugin_package|entity_type_config|tenant_plugin|extension)", "core/platform"),
    (r"(message|trigger_point|event_type|event_trigger|msg_|notification|subscription)", "core/messaging"),
    (r"(task_|workflow|approval|bpmn|flowable)", "core/task"),
    (r"(audit|activity_event|domain_event)", "core/audit"),
    (r"(tenant)", "core/tenant"),
]


def classify(name: str) -> str:
    lower = name.lower()
    for pattern, category in RULES:
        if re.search(pattern, lower):
            return category
    return "unknown"


def main() -> None:
    files = sorted(p.name for p in SCHEMA.iterdir() if p.is_file())
    buckets: dict[str, list[str]] = {}
    for f in files:
        buckets.setdefault(classify(f), []).append(f)

    # 输出: 按类别统计 + 每类展开文件
    print("# Phase 6.6 migration classification (auto-generated)\n")
    print(f"Total: {len(files)} files across {len(buckets)} categories\n")
    print("| Category | Count | Files |")
    print("|---|---:|---|")
    for cat in sorted(buckets.keys()):
        items = sorted(buckets[cat])
        shown = ", ".join(items[:3])
        if len(items) > 3:
            shown += f", ... (+{len(items)-3} more)"
        print(f"| `{cat}` | {len(items)} | {shown} |")


if __name__ == "__main__":
    main()
