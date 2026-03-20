# -*- coding: utf-8 -*-
"""V7 检查平台中文测试数据创建脚本 (修正版)"""

import json
import subprocess
import tempfile
import os
import sys

BASE = "http://localhost:8080/api"
TOKEN = None
TEMP_DIR = os.environ.get('TEMP', 'C:/Users/21579/AppData/Local/Temp')

def api(method, path, body=None):
    """发送 API 请求 - 通过 curl 子进程，用临时文件传 JSON 确保 UTF-8"""
    url = BASE + path
    cmd = ["curl", "-s", "-X", method, url,
           "-H", "Content-Type: application/json; charset=utf-8"]
    if TOKEN:
        cmd += ["-H", f"Authorization: Bearer {TOKEN}"]

    tmp_path = None
    if body:
        tmp_path = os.path.join(TEMP_DIR, 'api_body.json')
        with open(tmp_path, 'w', encoding='utf-8') as f:
            json.dump(body, f, ensure_ascii=False)
        cmd += ["-d", f"@{tmp_path}"]

    try:
        result = subprocess.run(cmd, capture_output=True, timeout=30)
        text = result.stdout.decode('utf-8', errors='replace')
        if not text:
            return None
        resp = json.loads(text)
        # Check for error codes
        if isinstance(resp, dict) and resp.get("code") and resp["code"] != 200:
            msg = resp.get('message', '')
            print(f"  ERR {resp['code']}: {method} {path} => {msg}")
        return resp
    except json.JSONDecodeError:
        # Some endpoints return a list or raw object
        try:
            return json.loads(text)
        except:
            print(f"  ERR JSON: {method} {path} => {text[:100]}")
            return None
    except Exception as e:
        print(f"  ERR: {method} {path} => {e}")
        return None
    finally:
        if tmp_path and os.path.exists(tmp_path):
            os.unlink(tmp_path)

def get(path):
    return api("GET", path)

def post(path, body=None):
    return api("POST", path, body)

def put(path, body=None):
    return api("PUT", path, body)

def xid(resp):
    """从响应中提取 ID - 支持 Result<T> 和直接返回两种格式"""
    if not resp:
        return None
    # Result<T> 格式: {code, data: {id: ...}}
    if isinstance(resp, dict) and "data" in resp:
        d = resp["data"]
        if isinstance(d, dict):
            v = d.get("id")
            return int(v) if v else None
        if isinstance(d, (int, str)):
            return int(d) if d else None
    # 直接返回对象: {id: ...}
    if isinstance(resp, dict) and "id" in resp:
        return int(resp["id"]) if resp["id"] else None
    return None

# ==================== LOGIN ====================
print("=" * 60)
print("Phase A: Login")
print("=" * 60)

resp = post("/auth/login", {"username": "admin", "password": "admin123"})
if not resp or not resp.get("data"):
    print("LOGIN FAILED!")
    sys.exit(1)
TOKEN = resp["data"]["accessToken"]
print(f"  OK: {TOKEN[:40]}...")

# ==================== B1: Platform BC ====================
print("\n" + "=" * 60)
print("Phase B1: Platform BC")
print("=" * 60)

# --- Issue Categories (ResponseEntity, not Result<T>) ---
print("\n--- Issue Categories ---")
r = post("/v7/insp/issue-categories", {
    "categoryCode": "ROOT", "categoryName": "检查问题分类",
    "description": "所有检查问题的根分类", "sortOrder": 1
})
root_cat = xid(r)
print(f"  Root: {root_cat}")

r = post("/v7/insp/issue-categories", {
    "categoryCode": "HYGIENE", "categoryName": "卫生问题",
    "description": "卫生清洁相关问题", "parentId": root_cat, "sortOrder": 1
})
hyg_cat = xid(r)
print(f"  Hygiene: {hyg_cat}")

r = post("/v7/insp/issue-categories", {
    "categoryCode": "SAFETY", "categoryName": "安全问题",
    "description": "安全隐患相关问题", "parentId": root_cat, "sortOrder": 2
})
saf_cat = xid(r)
print(f"  Safety: {saf_cat}")

r = post("/v7/insp/issue-categories", {
    "categoryCode": "FACILITY", "categoryName": "设施问题",
    "description": "设施损坏相关问题", "parentId": root_cat, "sortOrder": 3
})
fac_cat = xid(r)
print(f"  Facility: {fac_cat}")

# --- Holiday Calendar ---
print("\n--- Holiday Calendar ---")
# Note: field is 'year' not 'calendarYear', holidays is a JSON string
r = post("/v7/insp/holiday-calendars", {
    "calendarName": "2026年校历",
    "year": 2026,
    "holidays": json.dumps([
        "2026-01-01", "2026-02-17", "2026-04-05",
        "2026-05-01", "2026-05-31", "2026-09-27", "2026-10-01"
    ]),
    "isDefault": True
})
cal_id = xid(r)
print(f"  Calendar: {cal_id}")

# --- Notification Rules ---
print("\n--- Notification Rules ---")
r = post("/v7/insp/notification-rules", {
    "ruleName": "任务分配通知", "eventType": "TASK_ASSIGNED",
    "channel": "IN_APP", "templateContent": "您有新的检查任务已分配，请及时处理。",
    "recipientType": "INSPECTOR", "isEnabled": True, "priority": 1
})
print(f"  Rule1: {xid(r)}")

r = post("/v7/insp/notification-rules", {
    "ruleName": "整改到期提醒", "eventType": "CASE_DEADLINE_APPROACHING",
    "channel": "IN_APP", "templateContent": "您的整改案例即将到期，请尽快完成整改。",
    "recipientType": "ASSIGNEE", "isEnabled": True, "priority": 2
})
print(f"  Rule2: {xid(r)}")

# --- Report Template ---
print("\n--- Report Template ---")
# Note: createdBy is Long, not String
r = post("/v7/insp/report-templates", {
    "tenantId": 0, "templateName": "周检查汇总报告",
    "templateCode": "WEEKLY_SUMMARY", "reportType": "SUMMARY",
    "formatConfig": json.dumps({"format": "PDF", "orientation": "portrait"}),
    "headerConfig": json.dumps({"showLogo": True, "title": "周检查汇总报告"}),
    "isDefault": True, "createdBy": 1
})
rpt_id = xid(r)
print(f"  Report: {rpt_id}")

# --- Webhook ---
print("\n--- Webhook ---")
r = post("/v7/insp/webhooks", {
    "subscriptionName": "检查结果推送",
    "targetUrl": "https://example.com/webhook/inspection",
    "secret": "webhook_secret_2026",
    "eventTypes": "TASK_COMPLETED,SUBMISSION_PUBLISHED",
    "retryCount": 3
})
wh_id = xid(r)
print(f"  Webhook: {wh_id}")

# ==================== B2: Template BC ====================
print("\n" + "=" * 60)
print("Phase B2: Template BC")
print("=" * 60)

# --- Catalogs ---
print("\n--- Catalogs ---")
r = post("/v7/insp/catalogs", {
    "catalogCode": "DORM_CHECK", "catalogName": "宿舍检查",
    "description": "宿舍相关检查模板目录", "sortOrder": 1
})
cat1_id = xid(r)
print(f"  宿舍检查: {cat1_id}")

r = post("/v7/insp/catalogs", {
    "catalogCode": "CLASS_CHECK", "catalogName": "教室检查",
    "description": "教室环境检查模板目录", "sortOrder": 2
})
print(f"  教室检查: {xid(r)}")

r = post("/v7/insp/catalogs", {
    "catalogCode": "CAMPUS_SAFETY", "catalogName": "校园安全",
    "description": "校园安全检查模板目录", "sortOrder": 3
})
print(f"  校园安全: {xid(r)}")

# --- Response Sets ---
print("\n--- Response Sets ---")
# RS1: 合格/不合格
r = post("/v7/insp/response-sets", {
    "setCode": "PASS_FAIL", "setName": "合格/不合格",
    "description": "二选一评判", "setType": "SINGLE_CHOICE"
})
rs1 = xid(r)
print(f"  合格/不合格: {rs1}")
if rs1:
    for o in [
        {"optionCode": "PASS", "optionLabel": "合格", "optionValue": "合格", "score": 100, "sortOrder": 1},
        {"optionCode": "FAIL", "optionLabel": "不合格", "optionValue": "不合格", "score": 0, "sortOrder": 2},
    ]:
        post(f"/v7/insp/response-sets/{rs1}/options", o)

# RS2: 五级评分
r = post("/v7/insp/response-sets", {
    "setCode": "FIVE_LEVEL", "setName": "五级评分",
    "description": "优秀/良好/一般/较差/不合格", "setType": "SINGLE_CHOICE"
})
rs2 = xid(r)
print(f"  五级评分: {rs2}")
if rs2:
    for o in [
        {"optionCode": "EXCELLENT", "optionLabel": "优秀", "optionValue": "优秀", "score": 100, "sortOrder": 1},
        {"optionCode": "GOOD", "optionLabel": "良好", "optionValue": "良好", "score": 80, "sortOrder": 2},
        {"optionCode": "AVERAGE", "optionLabel": "一般", "optionValue": "一般", "score": 60, "sortOrder": 3},
        {"optionCode": "POOR", "optionLabel": "较差", "optionValue": "较差", "score": 40, "sortOrder": 4},
        {"optionCode": "FAIL", "optionLabel": "不合格", "optionValue": "不合格", "score": 0, "sortOrder": 5},
    ]:
        post(f"/v7/insp/response-sets/{rs2}/options", o)

# RS3: 达标评判
r = post("/v7/insp/response-sets", {
    "setCode": "THREE_LEVEL", "setName": "达标评判",
    "description": "达标/轻微问题/不达标", "setType": "SINGLE_CHOICE"
})
rs3 = xid(r)
print(f"  达标评判: {rs3}")
if rs3:
    for o in [
        {"optionCode": "MET", "optionLabel": "达标", "optionValue": "达标", "score": 100, "sortOrder": 1},
        {"optionCode": "MINOR", "optionLabel": "轻微问题", "optionValue": "轻微问题", "score": 60, "sortOrder": 2},
        {"optionCode": "NOT_MET", "optionLabel": "不达标", "optionValue": "不达标", "score": 0, "sortOrder": 3},
    ]:
        post(f"/v7/insp/response-sets/{rs3}/options", o)

# --- Template ---
print("\n--- Template ---")
r = post("/v7/insp/templates", {
    "templateName": "宿舍卫生检查模板",
    "description": "用于日常宿舍卫生与安全检查的标准模板",
    "catalogId": cat1_id,
    "tags": json.dumps(["宿舍", "卫生", "日常检查"]),
    "applicableTargetTypes": json.dumps(["PLACE", "ORG"])
})
tmpl = xid(r)
print(f"  Template: {tmpl}")

# --- Sections ---
print("\n--- Sections ---")
sec_defs = [
    ("SEC_HYGIENE", "房间卫生", 40, 1),
    ("SEC_SAFETY", "安全检查", 30, 2),
    ("SEC_FACILITY", "设施完好", 20, 3),
    ("SEC_OTHER", "综合评价", 10, 4),
]
secs = {}
for code, name, weight, order in sec_defs:
    r = post(f"/v7/insp/templates/{tmpl}/sections", {
        "sectionCode": code, "sectionName": name, "weight": weight, "sortOrder": order
    })
    secs[code] = xid(r)
    print(f"  {name}: {secs[code]}")

# --- Items (URL: /v7/insp/sections/{sectionId}/items, type: RADIO/TEXTAREA/PHOTO etc.) ---
print("\n--- Items ---")
item_defs = [
    # (sectionKey, itemCode, itemName, itemType, responseSetId, isScored, isRequired, sortOrder, extra)
    ("SEC_HYGIENE", "HYG_FLOOR", "地面清洁", "RADIO", rs2, True, True, 1, {}),
    ("SEC_HYGIENE", "HYG_DESK", "桌面整理", "RADIO", rs2, True, True, 2, {}),
    ("SEC_HYGIENE", "HYG_BATH", "卫生间清洁", "RADIO", rs2, True, True, 3, {}),
    ("SEC_HYGIENE", "HYG_BED", "床铺整理", "RADIO", rs2, True, True, 4, {}),
    ("SEC_HYGIENE", "HYG_NOTE", "卫生备注", "TEXTAREA", None, False, False, 5, {}),
    ("SEC_SAFETY", "SAF_ELEC", "电器安全", "RADIO", rs1, True, True, 1, {}),
    ("SEC_SAFETY", "SAF_FIRE", "消防设施", "RADIO", rs1, True, True, 2, {}),
    ("SEC_SAFETY", "SAF_EXIT", "安全通道", "RADIO", rs1, True, True, 3, {}),
    ("SEC_SAFETY", "SAF_HAZARD", "违禁品检查", "RADIO", rs3, True, True, 4, {}),
    ("SEC_FACILITY", "FAC_DOOR", "门窗完好", "RADIO", rs1, True, True, 1, {}),
    ("SEC_FACILITY", "FAC_FURN", "家具状况", "RADIO", rs2, True, True, 2, {}),
    ("SEC_FACILITY", "FAC_LIGHT", "照明设备", "RADIO", rs1, True, True, 3, {}),
    ("SEC_FACILITY", "FAC_DMG", "损坏描述", "TEXTAREA", None, False, False, 4, {}),
    ("SEC_FACILITY", "FAC_PHOTO", "现场照片", "PHOTO", None, False, False, 5, {"requireEvidence": True}),
    ("SEC_OTHER", "OVR_RATING", "总体评价", "RADIO", rs2, True, True, 1, {}),
    ("SEC_OTHER", "OVR_COMMENT", "检查员评语", "TEXTAREA", None, False, False, 2, {}),
    ("SEC_OTHER", "OVR_SUGGEST", "改进建议", "TEXTAREA", None, False, False, 3, {}),
]
items = {}
for sec_key, code, name, itype, rsid, scored, req, order, extra in item_defs:
    sid = secs.get(sec_key)
    if not sid:
        continue
    body = {"itemCode": code, "itemName": name, "itemType": itype,
            "isScored": scored, "isRequired": req, "sortOrder": order}
    if rsid:
        body["responseSetId"] = rsid
    body.update(extra)
    r = post(f"/v7/insp/sections/{sid}/items", body)
    items[code] = xid(r)
    print(f"  {name}: {items[code]}")

# ==================== B3: Scoring BC ====================
print("\n" + "=" * 60)
print("Phase B3: Scoring BC")
print("=" * 60)

r = post("/v7/insp/scoring-profiles", {"templateId": tmpl})
profile = xid(r)
print(f"  Profile: {profile}")

# Dimensions
print("\n--- Dimensions ---")
for dc, dn, w, o in [
    ("DIM_HYG", "卫生维度", 40, 1),
    ("DIM_SAF", "安全维度", 30, 2),
    ("DIM_FAC", "设施维度", 20, 3),
    ("DIM_OVR", "综合维度", 10, 4),
]:
    r = post(f"/v7/insp/scoring-profiles/{profile}/dimensions", {
        "dimensionCode": dc, "dimensionName": dn, "weight": w, "sortOrder": o
    })
    print(f"  {dn}: {xid(r)}")

# Grade Bands
print("\n--- Grade Bands ---")
for gc, gn, mn, mx, clr, o in [
    ("A", "优秀", 90, 100, "#4CAF50", 1),
    ("B", "良好", 80, 89, "#2196F3", 2),
    ("C", "及格", 70, 79, "#FF9800", 3),
    ("D", "不及格", 0, 69, "#F44336", 4),
]:
    r = post(f"/v7/insp/scoring-profiles/{profile}/grade-bands", {
        "gradeCode": gc, "gradeName": gn, "minScore": mn, "maxScore": mx,
        "color": clr, "sortOrder": o
    })
    print(f"  {gn}({gc}): {xid(r)}")

# Calculation Rule (URL: /calculation-rules not /rules)
print("\n--- Calculation Rules ---")
r = post(f"/v7/insp/scoring-profiles/{profile}/calculation-rules", {
    "ruleCode": "RULE_VETO", "ruleName": "安全一票否决", "priority": 1,
    "ruleType": "VETO",
    "config": json.dumps({"dimension": "DIM_SAF", "threshold": 60, "vetoGrade": "D"}),
    "isEnabled": True
})
print(f"  安全一票否决: {xid(r)}")

# ==================== B4: Publish Template ====================
print("\n" + "=" * 60)
print("Phase B4: Publish Template")
print("=" * 60)

r = post(f"/v7/insp/templates/{tmpl}/publish")
ver = xid(r)
print(f"  Version: {ver}")

# ==================== B5: Execution BC - Project ====================
print("\n" + "=" * 60)
print("Phase B5: Project")
print("=" * 60)

r = post("/v7/insp/projects", {
    "projectName": "2026年2月宿舍卫生检查",
    "templateId": tmpl,
    "startDate": "2026-02-01"
})
proj = xid(r)
print(f"  Project: {proj}")

# Update project
r = put(f"/v7/insp/projects/{proj}", {
    "projectName": "2026年2月宿舍卫生检查",
    "templateId": tmpl,
    "scoringProfileId": profile,
    "scopeType": "ORG",
    "scopeConfig": json.dumps({"orgIds": [1, 2, 3]}),
    "targetType": "PLACE",
    "startDate": "2026-02-01",
    "endDate": "2026-02-28",
    "cycleType": "DAILY",
    "timeSlots": json.dumps([
        {"code": "MORNING", "name": "上午", "start": "08:00", "end": "12:00"},
        {"code": "AFTERNOON", "name": "下午", "start": "14:00", "end": "18:00"}
    ]),
    "skipHolidays": True,
    "holidayCalendarId": cal_id,
    "assignmentMode": "FREE",
    "reviewRequired": True,
    "autoPublish": False
})
print(f"  Update: {'OK' if r and r.get('code') == 200 else 'FAIL'}")

# Add inspectors
r = post(f"/v7/insp/projects/{proj}/inspectors", {
    "userId": 1, "userName": "张明", "role": "INSPECTOR"
})
insp1 = xid(r)
print(f"  Inspector1: {insp1}")

r = post(f"/v7/insp/projects/{proj}/inspectors", {
    "userId": 1, "userName": "李华", "role": "REVIEWER"
})
print(f"  Reviewer: {xid(r)}")

# Publish project
r = post(f"/v7/insp/projects/{proj}/publish", {"templateVersionId": ver})
print(f"  Publish: {'OK' if r and r.get('code') == 200 else 'FAIL'}")

# ==================== B6: Tasks & Submissions ====================
print("\n" + "=" * 60)
print("Phase B6: Tasks & Submissions")
print("=" * 60)

# Create 3 tasks
task_defs = [
    ("2026-02-25", "MORNING", "08:00", "12:00"),
    ("2026-02-26", "AFTERNOON", "14:00", "18:00"),
    ("2026-02-27", "MORNING", "08:00", "12:00"),
]
tasks = []
for td, ts_code, ts_start, ts_end in task_defs:
    r = post("/v7/insp/tasks", {
        "projectId": proj, "taskDate": td,
        "timeSlotCode": ts_code, "timeSlotStart": ts_start, "timeSlotEnd": ts_end
    })
    tid = xid(r)
    tasks.append(tid)
    print(f"  Task {td} {ts_code}: {tid}")

# === Task 1: Full lifecycle ===
t1 = tasks[0]
print(f"\n--- Task 1 (ID={t1}): Full lifecycle ---")
post(f"/v7/insp/tasks/{t1}/claim", {"inspectorName": "张明"})
post(f"/v7/insp/tasks/{t1}/start")
print("  Claimed & Started")

# Create 3 submissions
targets = [
    ("PLACE", 1, "男生宿舍101"),
    ("PLACE", 2, "男生宿舍102"),
    ("PLACE", 3, "女生宿舍201"),
]
subs = []
for tt, tid_val, tn in targets:
    r = post("/v7/insp/submissions", {
        "taskId": t1, "targetType": tt, "targetId": tid_val, "targetName": tn
    })
    sid = xid(r)
    subs.append(sid)
    print(f"  Submission {tn}: {sid}")

# Scored items definition
scored_items = [
    ("HYG_FLOOR", "地面清洁", "RADIO", "SEC_HYGIENE", "房间卫生"),
    ("HYG_DESK", "桌面整理", "RADIO", "SEC_HYGIENE", "房间卫生"),
    ("HYG_BATH", "卫生间清洁", "RADIO", "SEC_HYGIENE", "房间卫生"),
    ("HYG_BED", "床铺整理", "RADIO", "SEC_HYGIENE", "房间卫生"),
    ("SAF_ELEC", "电器安全", "RADIO", "SEC_SAFETY", "安全检查"),
    ("SAF_FIRE", "消防设施", "RADIO", "SEC_SAFETY", "安全检查"),
    ("SAF_EXIT", "安全通道", "RADIO", "SEC_SAFETY", "安全检查"),
    ("SAF_HAZARD", "违禁品检查", "RADIO", "SEC_SAFETY", "安全检查"),
    ("FAC_DOOR", "门窗完好", "RADIO", "SEC_FACILITY", "设施完好"),
    ("FAC_FURN", "家具状况", "RADIO", "SEC_FACILITY", "设施完好"),
    ("FAC_LIGHT", "照明设备", "RADIO", "SEC_FACILITY", "设施完好"),
    ("OVR_RATING", "总体评价", "RADIO", "SEC_OTHER", "综合评价"),
]
scores_matrix = [
    [100, 80, 100, 80, 100, 100, 100, 100, 100, 80, 100, 100],  # 101: excellent
    [80, 60, 80, 60, 100, 100, 100, 60, 100, 60, 100, 80],      # 102: good
    [60, 40, 60, 40, 100, 0, 100, 60, 0, 40, 100, 40],          # 201: poor
]
values_matrix = [
    ["优秀", "良好", "优秀", "良好", "合格", "合格", "合格", "达标", "合格", "良好", "合格", "优秀"],
    ["良好", "一般", "良好", "一般", "合格", "合格", "合格", "轻微问题", "合格", "一般", "合格", "良好"],
    ["一般", "较差", "一般", "较差", "合格", "不合格", "合格", "轻微问题", "不合格", "较差", "合格", "较差"],
]

last_detail_ids = []
for idx, sub_id in enumerate(subs):
    if not sub_id:
        continue
    print(f"  Filling {targets[idx][2]}...")
    det_ids = []
    for j, (code, name, itype, sec_key, sec_name) in enumerate(scored_items):
        # Create detail (URL: /v7/insp/submissions/{id}/details)
        d = post(f"/v7/insp/submissions/{sub_id}/details", {
            "templateItemId": items.get(code, 1),
            "itemCode": code, "itemName": name, "itemType": itype,
            "sectionId": secs.get(sec_key, 1), "sectionName": sec_name,
            "scoringMode": "DIRECT"
        })
        did = xid(d)
        det_ids.append(did)
        # Update response (URL: /v7/insp/submissions/details/{detailId}/response)
        if did:
            put(f"/v7/insp/submissions/details/{did}/response", {
                "responseValue": values_matrix[idx][j],
                "scoringMode": "DIRECT",
                "score": scores_matrix[idx][j]
            })
    last_detail_ids = det_ids

    total = sum(scores_matrix[idx]) / len(scores_matrix[idx])
    grade = "A" if total >= 90 else "B" if total >= 80 else "C" if total >= 70 else "D"
    post(f"/v7/insp/submissions/{sub_id}/complete", {
        "baseScore": 100, "finalScore": round(total, 1),
        "deductionTotal": round(100 - total, 1), "bonusTotal": 0,
        "grade": grade, "passed": total >= 70
    })
    print(f"    Score={round(total,1)} Grade={grade}")

# Submit for review, review, publish
post(f"/v7/insp/tasks/{t1}/submit")
post(f"/v7/insp/tasks/{t1}/review", {
    "reviewerName": "李华", "comment": "检查结果准确，同意发布。"
})
post(f"/v7/insp/tasks/{t1}/publish")
print("  Task 1: Submitted -> Reviewed -> Published")

# === Task 2: Partial ===
t2 = tasks[1]
print(f"\n--- Task 2 (ID={t2}): Partial ---")
if t2:
    post(f"/v7/insp/tasks/{t2}/claim", {"inspectorName": "张明"})
    post(f"/v7/insp/tasks/{t2}/start")
    r = post("/v7/insp/submissions", {
        "taskId": t2, "targetType": "PLACE", "targetId": 4, "targetName": "男生宿舍103"
    })
    print(f"  Partial submission: {xid(r)}")

# === Task 3: Pending ===
print(f"\n--- Task 3 (ID={tasks[2]}): Pending ---")

# ==================== B7: Corrective BC ====================
print("\n" + "=" * 60)
print("Phase B7: Corrective Cases")
print("=" * 60)

# Note: deadline must be ISO datetime format, lifecycle URLs differ
# Case 1: Critical - full lifecycle
print("\n--- Case 1: Critical (full lifecycle) ---")
r = post("/v7/insp/corrective-cases", {
    "caseCode": "CASE-2026-001",
    "issueDescription": "女生宿舍201消防灭火器过期，存在重大安全隐患",
    "priority": "CRITICAL",
    "submissionId": subs[2] if len(subs) > 2 and subs[2] else 1,
    "detailId": last_detail_ids[5] if len(last_detail_ids) > 5 and last_detail_ids[5] else 1,
    "projectId": proj,
    "taskId": t1,
    "targetType": "PLACE",
    "targetId": 3,
    "targetName": "女生宿舍201",
    "requiredAction": "立即更换过期灭火器，全面检查消防设施",
    "deadline": "2026-03-01T18:00:00"
})
c1 = xid(r)
print(f"  Case1: {c1}")
if c1:
    post(f"/v7/insp/corrective-cases/{c1}/assign", {"assigneeName": "王建国"})
    post(f"/v7/insp/corrective-cases/{c1}/start-work")
    post(f"/v7/insp/corrective-cases/{c1}/submit-correction", {
        "resolution": "已更换全部过期灭火器4个，检查消防栓2个均正常。"
    })
    post(f"/v7/insp/corrective-cases/{c1}/verify", {
        "verifierName": "李华", "comment": "灭火器已更换，消防设施正常。"
    })
    post(f"/v7/insp/corrective-cases/{c1}/close", {"comment": "整改完成，问题已解决。"})
    print("  OPEN -> ASSIGNED -> IN_PROGRESS -> SUBMITTED -> VERIFIED -> CLOSED")

# Case 2: Medium - rejected
print("\n--- Case 2: Medium (rejected) ---")
r = post("/v7/insp/corrective-cases", {
    "caseCode": "CASE-2026-002",
    "issueDescription": "女生宿舍201卧室窗户把手松动，无法正常关闭",
    "priority": "MEDIUM",
    "submissionId": subs[2] if len(subs) > 2 and subs[2] else 1,
    "projectId": proj,
    "taskId": t1,
    "targetType": "PLACE", "targetId": 3, "targetName": "女生宿舍201",
    "requiredAction": "修复或更换窗户把手",
    "deadline": "2026-03-05T18:00:00"
})
c2 = xid(r)
print(f"  Case2: {c2}")
if c2:
    post(f"/v7/insp/corrective-cases/{c2}/assign", {"assigneeName": "赵伟"})
    post(f"/v7/insp/corrective-cases/{c2}/start-work")
    post(f"/v7/insp/corrective-cases/{c2}/submit-correction", {"resolution": "已上紧螺丝"})
    post(f"/v7/insp/corrective-cases/{c2}/reject", {
        "comment": "整改不充分，窗户仍无法正常闭合，需更换把手。"
    })
    print("  OPEN -> ASSIGNED -> IN_PROGRESS -> SUBMITTED -> REJECTED")

# Case 3: High - escalated
print("\n--- Case 3: High (escalated) ---")
r = post("/v7/insp/corrective-cases", {
    "caseCode": "CASE-2026-003",
    "issueDescription": "男生宿舍102发现私拉电线，存在触电和火灾隐患",
    "priority": "HIGH",
    "submissionId": subs[1] if len(subs) > 1 and subs[1] else 1,
    "projectId": proj,
    "taskId": t1,
    "targetType": "PLACE", "targetId": 2, "targetName": "男生宿舍102",
    "requiredAction": "拆除私拉电线，对学生进行安全教育",
    "deadline": "2026-03-02T18:00:00"
})
c3 = xid(r)
print(f"  Case3: {c3}")
if c3:
    post(f"/v7/insp/corrective-cases/{c3}/assign", {"assigneeName": "张明"})
    post(f"/v7/insp/corrective-cases/{c3}/escalate", {
        "reason": "涉及学生安全教育，需升级到学工部处理"
    })
    print("  OPEN -> ASSIGNED -> ESCALATED")

# ==================== B8: Analytics ====================
print("\n" + "=" * 60)
print("Phase B8: Analytics")
print("=" * 60)

# Analytics rebuild endpoints might not need a body
r = post("/v7/insp/analytics/rebuild-daily", {})
print(f"  Daily rebuild: {'OK' if r else 'FAIL'}")

r = post("/v7/insp/analytics/rebuild-period", {})
print(f"  Period rebuild: {'OK' if r else 'FAIL'}")

# Test overview
r = get(f"/v7/insp/analytics/overview?projectId={proj}")
if r and r.get("code") == 200:
    print(f"  Overview: OK")
else:
    print(f"  Overview: {r}")

# ==================== DONE ====================
print("\n" + "=" * 60)
print("ALL DONE!")
print("=" * 60)
print(f"""
  Catalogs: 3
  Response Sets: 3 (10 options)
  Template: 1 (4 sections, {len(items)} items)
  Scoring: 1 profile, 4 dims, 4 grades, 1 rule
  Project: 1 (2 inspectors)
  Tasks: 3 (published/partial/pending)
  Submissions: 4 (3 complete + 1 partial)
  Corrective: 3 (closed/rejected/escalated)
  Platform: categories:{root_cat},{hyg_cat},{saf_cat},{fac_cat}
            calendar:{cal_id} webhook:{wh_id} report:{rpt_id}
""")
