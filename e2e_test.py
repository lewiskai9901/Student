#!/usr/bin/env python3
"""
V7 Inspection System - Full E2E Test
完整闭环: 模板→评分方案→项目→任务→提交→打分→出结果
"""

import requests
import json
import sys
from datetime import date

BASE = "http://localhost:8080/api"
session = requests.Session()
session.trust_env = False

def login():
    r = session.post(f"{BASE}/auth/login", json={"username": "admin", "password": "admin123"})
    token = r.json()["data"]["accessToken"]
    session.headers["Authorization"] = f"Bearer {token}"
    print("[OK] Login success")

def api(method, path, payload=None, label=""):
    url = f"{BASE}{path}"
    r = session.request(method, url, json=payload)
    try:
        body = r.json()
    except:
        print(f"[FAIL] {label}: HTTP {r.status_code}, non-JSON: {r.text[:200]}")
        return None
    code = body.get("code", r.status_code)
    if code != 200:
        print(f"[FAIL] {label}: code={code}, msg={body.get('message','?')}")
        if payload:
            print(f"  Payload: {json.dumps(payload, ensure_ascii=False)[:300]}")
        return None
    data = body.get("data")
    print(f"[OK] {label}")
    return data

def to_int(val):
    """Safely convert string IDs to int for JSON payloads"""
    if val is None:
        return None
    return int(val)

# ═══════════ Phase 1: Template ═══════════
def phase1():
    print("\n" + "="*60)
    print("Phase 1: Template + Sections + Items")
    print("="*60)

    tpl = api("POST", "/v7/insp/templates", {
        "templateName": "E2E综合测试模板",
        "description": "覆盖DEDUCTION/ADDITION评分"
    }, "Create template")
    if not tpl: return None
    T = tpl["id"]
    print(f"  templateId={T}")

    sec1 = api("POST", f"/v7/insp/templates/{T}/sections", {
        "sectionCode": "SEC-HYG",
        "sectionName": "卫生检查",
        "sortOrder": 1
    }, "Section 1: 卫生检查")
    if not sec1: return None

    sec2 = api("POST", f"/v7/insp/templates/{T}/sections", {
        "sectionCode": "SEC-BNS",
        "sectionName": "表现加分",
        "sortOrder": 2
    }, "Section 2: 表现加分")
    if not sec2: return None

    items_def = [
        (sec1["id"], "ITEM-01", "地面不干净",     "CHECKLIST", {"scoreType":"DEDUCTION","configScore":5,"maxScore":10}),
        (sec1["id"], "ITEM-02", "物品摆放不整齐", "CHECKLIST", {"scoreType":"DEDUCTION","configScore":3,"maxScore":9}),
        (sec1["id"], "ITEM-03", "垃圾未清理",     "CHECKLIST", {"scoreType":"DEDUCTION","configScore":8,"maxScore":16}),
        (sec2["id"], "ITEM-04", "主动帮助打扫",   "CHECKLIST", {"scoreType":"ADDITION","configScore":5,"maxScore":15}),
        (sec2["id"], "ITEM-05", "创意装饰",       "CHECKLIST", {"scoreType":"ADDITION","configScore":10,"maxScore":10}),
    ]
    items = []
    for sid, code, name, itype, sc in items_def:
        item = api("POST", f"/v7/insp/sections/{sid}/items", {
            "itemCode": code,
            "itemName": name,
            "itemType": itype,
            "isScored": True,
            "isRequired": True,
            "sortOrder": len(items)+1,
            "scoringConfig": json.dumps(sc)
        }, f"Item: {name} ({sc['scoreType']} {sc['configScore']})")
        if not item: return None
        items.append(item)

    api("POST", f"/v7/insp/templates/{T}/publish", label="Publish template")

    return {"tpl_id": T, "items": items, "item_ids": [i["id"] for i in items]}

# ═══════════ Phase 2: Scoring Profile ═══════════
def phase2(tpl_id):
    print("\n" + "="*60)
    print("Phase 2: Scoring Profile + Dimensions + Rules + Bands")
    print("="*60)

    p = api("POST", "/v7/insp/scoring-profiles", {"templateId": tpl_id}, "Create profile")
    if not p: return None
    P = p["id"]
    print(f"  profileId={P}")

    api("PUT", f"/v7/insp/scoring-profiles/{P}", {
        "baseScore": 100, "maxScore": 120, "minScore": 0,
        "allowNegative": False, "precisionDigits": 2,
        "aggregationMethod": "WEIGHTED_AVG"
    }, "Configure profile")

    d1 = api("POST", f"/v7/insp/scoring-profiles/{P}/dimensions", {
        "dimensionCode": "DIM-HYG",
        "dimensionName": "卫生质量",
        "weight": 60,
        "sortOrder": 1
    }, "Dimension 1: 卫生质量 (60%)")
    D1 = d1["id"] if d1 else None

    d2 = api("POST", f"/v7/insp/scoring-profiles/{P}/dimensions", {
        "dimensionCode": "DIM-BHV",
        "dimensionName": "文明行为",
        "weight": 40,
        "sortOrder": 2
    }, "Dimension 2: 文明行为 (40%)")
    D2 = d2["id"] if d2 else None

    # Grade bands
    for code, name, mn, mx, color, order in [
        ("A", "优秀",   90, 120, "#22c55e", 1),
        ("B", "良好",   75, 89.99, "#3b82f6", 2),
        ("C", "合格",   60, 74.99, "#f59e0b", 3),
        ("D", "不合格", 0,  59.99, "#ef4444", 4),
    ]:
        api("POST", f"/v7/insp/scoring-profiles/{P}/grade-bands", {
            "dimensionId": to_int(D1),
            "gradeCode": code,
            "gradeName": name,
            "minScore": mn, "maxScore": mx,
            "color": color, "sortOrder": order
        }, f"Band: {name} ({mn}-{mx})")

    # Calc rules
    rules = [
        ("RULE-CEIL", "上限封顶", "CEILING", 1, {"ceiling": 120}),
        ("RULE-FLR",  "下限保底", "FLOOR",   2, {"floor": 0}),
        ("RULE-PROG", "累进扣分", "PROGRESSIVE", 3, {"thresholds":[{"count":3,"multiplier":1.5},{"count":5,"multiplier":2.0}]}),
        ("RULE-BNS",  "高分奖励", "BONUS",   4, {"bonusPoints": 5, "condition": "score >= 95"}),
        ("RULE-VETO", "一票否决", "VETO",    5, {"vetoThreshold": 30, "vetoScore": 0}),
    ]
    for code, name, rtype, pri, cfg in rules:
        api("POST", f"/v7/insp/scoring-profiles/{P}/calculation-rules", {
            "ruleCode": code,
            "ruleName": name,
            "ruleType": rtype,
            "priority": pri,
            "isEnabled": True,
            "config": json.dumps(cfg)
        }, f"Rule: {name} ({rtype})")

    return {"profile_id": P, "dim1_id": D1, "dim2_id": D2}

# ═══════════ Phase 3: Project ═══════════
def phase3(tpl_id, profile_id):
    print("\n" + "="*60)
    print("Phase 3: Project + Inspector + Publish")
    print("="*60)

    proj = api("POST", "/v7/insp/projects", {
        "projectName": "E2E测试检查项目",
        "templateId": to_int(tpl_id),
        "startDate": str(date.today())
    }, "Create project")
    if not proj: return None
    PJ = proj["id"]
    print(f"  projectId={PJ}")

    api("PUT", f"/v7/insp/projects/{PJ}", {
        "projectName": "E2E测试检查项目",
        "templateId": to_int(tpl_id),
        "scoringProfileId": to_int(profile_id),
        "scopeType": "ORG",
        "targetType": "ORG",
        "startDate": str(date.today()),
        "assignmentMode": "FREE"
    }, "Configure project (bind scoring profile)")

    api("POST", f"/v7/insp/projects/{PJ}/inspectors", {
        "userId": 1, "userName": "admin", "role": "INSPECTOR"
    }, "Assign inspector")

    # Get template detail for version
    tpl = api("GET", f"/v7/insp/templates/{tpl_id}", label="Get template")
    vid = tpl.get("latestVersionId") if tpl else None

    api("POST", f"/v7/insp/projects/{PJ}/publish",
        {"templateVersionId": to_int(vid)} if vid else {},
        "Publish project")

    return {"proj_id": PJ}

# ═══════════ Phase 4: Task ═══════════
def phase4(proj_id):
    print("\n" + "="*60)
    print("Phase 4: Task Create + Claim + Start")
    print("="*60)

    task = api("POST", "/v7/insp/tasks", {
        "projectId": to_int(proj_id),
        "taskDate": str(date.today()),
        "timeSlotCode": "MORNING",
        "timeSlotStart": "08:00",
        "timeSlotEnd": "12:00"
    }, "Create task")
    if not task: return None
    TK = task["id"]
    print(f"  taskId={TK}")

    api("POST", f"/v7/insp/tasks/{TK}/claim", {"inspectorName": "admin"}, "Claim task")
    api("POST", f"/v7/insp/tasks/{TK}/start", label="Start task")

    return {"task_id": TK}

# ═══════════ Phase 5: Submission 1 (Normal) ═══════════
def phase5(task_id, items):
    print("\n" + "="*60)
    print("Phase 5: Submission 1 (Normal case)")
    print("  base=100, -10 -3 +15 +10 = 112")
    print("="*60)

    sub = api("POST", "/v7/insp/submissions", {
        "taskId": to_int(task_id),
        "targetType": "ORG",
        "targetId": 1,
        "targetName": "一年级一班"
    }, "Create submission")
    if not sub: return None
    S = sub["id"]
    print(f"  submissionId={S}")

    api("POST", f"/v7/insp/submissions/{S}/start-filling", label="Start filling")

    # Create details
    details = []
    for item in items:
        d = api("POST", f"/v7/insp/submissions/{S}/details", {
            "templateItemId": to_int(item["id"]),
            "itemCode": item.get("itemCode", "ITEM-X"),
            "itemName": item.get("itemName", "unknown"),
            "itemType": item.get("itemType", "CHECKLIST"),
            "sectionId": to_int(item.get("sectionId")),
            "scoringConfig": item.get("scoringConfig", ""),
            "isScored": True
        }, f"Detail: {item.get('itemName')}")
        if d:
            details.append(d)

    # Fill responses
    quantities = [2, 1, 0, 3, 1]  # matching 5 items
    for i, qty in enumerate(quantities):
        if i < len(details):
            api("PUT", f"/v7/insp/submissions/details/{details[i]['id']}/response", {
                "responseValue": json.dumps({"quantity": qty}),
                "score": None
            }, f"Response: {items[i].get('itemName')} x{qty}")

    # Complete
    api("POST", f"/v7/insp/submissions/{S}/complete", {
        "baseScore": 100,
        "deductionTotal": -13,
        "bonusTotal": 25,
        "finalScore": 112,
        "scoreBreakdown": json.dumps({"base":100,"deductions":-13,"additions":25,"final":112}),
        "grade": "A",
        "passed": True
    }, "Complete (finalScore=112, grade=A/优秀)")

    v = api("GET", f"/v7/insp/submissions/{S}", label="Verify submission")
    if v:
        print(f"  finalScore={v.get('finalScore')}, grade={v.get('grade')}, status={v.get('status')}")

    return {"sub_id": S}

# ═══════════ Phase 6: Submission 2 (Heavy deduction) ═══════════
def phase6(task_id, items):
    print("\n" + "="*60)
    print("Phase 6: Submission 2 (Heavy deduction)")
    print("  base=100, -25 -9 -16 = 50")
    print("="*60)

    sub = api("POST", "/v7/insp/submissions", {
        "taskId": to_int(task_id),
        "targetType": "ORG",
        "targetId": 2,
        "targetName": "一年级二班"
    }, "Create submission 2")
    if not sub: return None
    S = sub["id"]

    api("POST", f"/v7/insp/submissions/{S}/start-filling", label="Start filling")

    details = []
    for item in items:
        d = api("POST", f"/v7/insp/submissions/{S}/details", {
            "templateItemId": to_int(item["id"]),
            "itemCode": item.get("itemCode", "ITEM-X"),
            "itemName": item.get("itemName", "unknown"),
            "itemType": item.get("itemType", "CHECKLIST"),
            "sectionId": to_int(item.get("sectionId")),
            "scoringConfig": item.get("scoringConfig", ""),
            "isScored": True
        }, f"Detail: {item.get('itemName')}")
        if d:
            details.append(d)

    quantities = [5, 3, 2, 0, 0]
    for i, qty in enumerate(quantities):
        if i < len(details):
            api("PUT", f"/v7/insp/submissions/details/{details[i]['id']}/response", {
                "responseValue": json.dumps({"quantity": qty}),
                "score": None
            }, f"Response: {items[i].get('itemName')} x{qty}")

    api("POST", f"/v7/insp/submissions/{S}/complete", {
        "baseScore": 100,
        "deductionTotal": -50,
        "bonusTotal": 0,
        "finalScore": 50,
        "scoreBreakdown": json.dumps({"base":100,"deductions":-50,"final":50}),
        "grade": "D",
        "passed": False
    }, "Complete (finalScore=50, grade=D/不合格)")

    v = api("GET", f"/v7/insp/submissions/{S}", label="Verify submission 2")
    if v:
        print(f"  finalScore={v.get('finalScore')}, grade={v.get('grade')}, status={v.get('status')}")

    return {"sub_id": S}

# ═══════════ Phase 7: Submit + Verify ═══════════
def phase7(task_id, proj_id):
    print("\n" + "="*60)
    print("Phase 7: Submit Task + Verify Results")
    print("="*60)

    api("POST", f"/v7/insp/tasks/{task_id}/submit", label="Submit task")

    task = api("GET", f"/v7/insp/tasks/{task_id}", label="Get task")
    if task:
        print(f"  status={task.get('status')}, completed={task.get('completedTargets')}/{task.get('totalTargets')}")

    subs = api("GET", f"/v7/insp/submissions?taskId={task_id}", label="List submissions")
    if subs:
        records = subs if isinstance(subs, list) else subs.get("records", [])
        print(f"  Total submissions: {len(records)}")
        for s in records:
            print(f"  - {s.get('targetName')}: score={s.get('finalScore')}, grade={s.get('grade')}, status={s.get('status')}")

    today = str(date.today())
    api("GET", f"/v7/insp/analytics/daily-ranking?projectId={proj_id}&date={today}", label="Daily ranking")
    api("GET", f"/v7/insp/analytics/daily-summary?projectId={proj_id}&date={today}", label="Daily summary")

    return True

# ═══════════ Main ═══════════
def main():
    print("V7 Inspection E2E Test")
    print("=" * 60)
    login()

    r = phase1()
    if not r: return 1

    s = phase2(r["tpl_id"])
    if not s: return 1

    p = phase3(r["tpl_id"], s["profile_id"])
    if not p: return 1

    t = phase4(p["proj_id"])
    if not t: return 1

    phase5(t["task_id"], r["items"])
    phase6(t["task_id"], r["items"])
    phase7(t["task_id"], p["proj_id"])

    print("\n" + "=" * 60)
    print("E2E Test Complete!")
    print("=" * 60)
    return 0

if __name__ == "__main__":
    sys.exit(main())
