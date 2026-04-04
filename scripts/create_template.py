#!/usr/bin/env python3
"""创建系部全面检查模板 - 覆盖所有评分方式"""
import json
import requests
import sys

BASE = "http://localhost:8080/api"

NO_PROXY = {"http": None, "https": None}

def api(method, path, data=None, token=None):
    url = f"{BASE}{path}"
    headers = {}
    if token:
        headers["Authorization"] = f"Bearer {token}"
    resp = requests.request(method, url, json=data, headers=headers, proxies=NO_PROXY)
    if resp.status_code >= 400:
        print(f"ERROR {resp.status_code}: {resp.text}", file=sys.stderr)
        sys.exit(1)
    return resp.json()

# 1. Login
print("=== 登录 ===")
login_resp = api("POST", "/auth/login", {"username": "admin", "password": "admin123"})
token = login_resp["data"]["accessToken"]
print(f"Token: {token[:30]}...")

# 2. Create root template
print("\n=== 创建根模板: 系部全面检查模板 ===")
root = api("POST", "/v7/insp/templates", {
    "name": "系部全面检查模板",
    "description": "覆盖安全、纪律、宿舍、卫生四大维度，涵盖所有评分方式（通过/不通过、扣分制、加分制、直接打分、等级制、评分量表等）的综合检查模板",
    "tags": json.dumps(["全面检查", "系部", "综合评估"])
}, token)
ROOT_ID = root["data"]["id"]
print(f"Root ID: {ROOT_ID}")

# ============================================================
# 3. Create 4 sections
# ============================================================

# --- Section 1: 安全检查 (targets ORG - departments) ---
print("\n=== 创建分区1: 安全检查 ===")
s1 = api("POST", "/v7/insp/sections", {
    "parentSectionId": ROOT_ID,
    "sectionCode": "SAFETY",
    "sectionName": "安全检查",
    "targetType": "ORG",
    "weight": 30,
    "sortOrder": 1
}, token)
S1_ID = s1["data"]["id"]
print(f"Section ID: {S1_ID}")

# Update targetTypeFilter for departments
api("PUT", f"/v7/insp/sections/{S1_ID}", {
    "sectionName": "安全检查",
    "targetType": "ORG",
    "targetSourceMode": "INDEPENDENT",
    "targetTypeFilter": "unit_type=DEPARTMENT",
    "weight": 30
}, token)
print("  -> targetTypeFilter set: unit_type=DEPARTMENT")

# Set scoring config: deduction mode, max 100
api("PUT", f"/v7/insp/sections/{S1_ID}/scoring-config", {
    "scoringConfig": json.dumps({
        "aggregation": "WEIGHTED_SUM",
        "maxScore": 100,
        "minScore": 0
    })
}, token)
print("  -> scoringConfig set")

# --- Section 2: 纪律检查 (targets ORG - classes) ---
print("\n=== 创建分区2: 纪律检查 ===")
s2 = api("POST", "/v7/insp/sections", {
    "parentSectionId": ROOT_ID,
    "sectionCode": "DISCIPLINE",
    "sectionName": "纪律检查",
    "targetType": "ORG",
    "weight": 25,
    "sortOrder": 2
}, token)
S2_ID = s2["data"]["id"]
print(f"Section ID: {S2_ID}")

api("PUT", f"/v7/insp/sections/{S2_ID}", {
    "sectionName": "纪律检查",
    "targetType": "ORG",
    "targetSourceMode": "PARENT_ASSOCIATED",
    "targetTypeFilter": "unit_type=CLASS",
    "weight": 25
}, token)
print("  -> targetTypeFilter set: unit_type=CLASS")

api("PUT", f"/v7/insp/sections/{S2_ID}/scoring-config", {
    "scoringConfig": json.dumps({
        "aggregation": "WEIGHTED_SUM",
        "maxScore": 100,
        "minScore": 0
    })
}, token)

# --- Section 3: 宿舍检查 (targets PLACE - dormitories) ---
print("\n=== 创建分区3: 宿舍检查 ===")
s3 = api("POST", "/v7/insp/sections", {
    "parentSectionId": ROOT_ID,
    "sectionCode": "DORMITORY",
    "sectionName": "宿舍检查",
    "targetType": "PLACE",
    "weight": 25,
    "sortOrder": 3
}, token)
S3_ID = s3["data"]["id"]
print(f"Section ID: {S3_ID}")

api("PUT", f"/v7/insp/sections/{S3_ID}", {
    "sectionName": "宿舍检查",
    "targetType": "PLACE",
    "targetSourceMode": "INDEPENDENT",
    "targetTypeFilter": "type_code=ROOM",
    "weight": 25
}, token)
print("  -> targetTypeFilter set: type_code=ROOM (dormitory rooms)")

api("PUT", f"/v7/insp/sections/{S3_ID}/scoring-config", {
    "scoringConfig": json.dumps({
        "aggregation": "WEIGHTED_SUM",
        "maxScore": 100,
        "minScore": 0
    })
}, token)

# --- Section 4: 卫生检查 (targets PLACE - classrooms) ---
print("\n=== 创建分区4: 卫生检查 ===")
s4 = api("POST", "/v7/insp/sections", {
    "parentSectionId": ROOT_ID,
    "sectionCode": "HYGIENE",
    "sectionName": "卫生检查",
    "targetType": "PLACE",
    "weight": 20,
    "sortOrder": 4
}, token)
S4_ID = s4["data"]["id"]
print(f"Section ID: {S4_ID}")

api("PUT", f"/v7/insp/sections/{S4_ID}", {
    "sectionName": "卫生检查",
    "targetType": "PLACE",
    "targetSourceMode": "INDEPENDENT",
    "targetTypeFilter": "type_code=ROOM",
    "weight": 20
}, token)
print("  -> targetTypeFilter set: type_code=ROOM (classrooms)")

api("PUT", f"/v7/insp/sections/{S4_ID}/scoring-config", {
    "scoringConfig": json.dumps({
        "aggregation": "WEIGHTED_SUM",
        "maxScore": 100,
        "minScore": 0
    })
}, token)

# ============================================================
# 4. Create items for each section - covering ALL scoring modes
# ============================================================

def create_item(section_id, item_code, item_name, item_type, is_scored=True,
                scoring_config=None, config=None, is_required=True,
                require_evidence=False, item_weight=1.0, sort_order=1, description=None):
    payload = {
        "itemCode": item_code,
        "itemName": item_name,
        "itemType": item_type,
        "isScored": is_scored,
        "isRequired": is_required,
        "requireEvidence": require_evidence,
        "itemWeight": item_weight,
        "sortOrder": sort_order
    }
    if scoring_config:
        payload["scoringConfig"] = json.dumps(scoring_config)
    if config:
        payload["config"] = json.dumps(config)
    if description:
        payload["description"] = description

    result = api("POST", f"/v7/insp/sections/{section_id}/items", payload, token)
    item_id = result["data"]["id"]
    print(f"  [{item_type:12s}] {item_name} (ID: {item_id})")
    return item_id


# ============================================================
# Section 1: 安全检查 - items
# ============================================================
print(f"\n=== 安全检查 (Section {S1_ID}) - 检查项 ===")

# 1) PASS_FAIL - 通过/不通过
create_item(S1_ID, "SAFETY-01", "消防设施是否完好", "PASS_FAIL",
    scoring_config={"mode": "PASS_FAIL", "passScore": 10, "failScore": 0},
    description="检查灭火器、消防栓、烟感器等是否正常工作",
    require_evidence=True, item_weight=0.2, sort_order=1)

# 2) DEDUCTION - 扣分制
create_item(S1_ID, "SAFETY-02", "安全隐患排查", "NUMBER",
    scoring_config={"mode": "DEDUCTION", "maxScore": 20, "deductPerViolation": 5, "minScore": 0},
    description="每发现一处安全隐患扣5分，满分20分",
    item_weight=0.2, sort_order=2)

# 3) CHECKLIST - 清单式（多项检查）
create_item(S1_ID, "SAFETY-03", "安全制度落实情况", "CHECKLIST",
    scoring_config={"mode": "CUMULATIVE", "scorePerItem": 4, "maxScore": 20},
    config={"options": [
        {"label": "安全责任制度上墙", "value": "posted"},
        {"label": "安全培训记录完整", "value": "training"},
        {"label": "应急预案已制定", "value": "plan"},
        {"label": "安全巡查记录齐全", "value": "patrol"},
        {"label": "危险品管理规范", "value": "hazmat"}
    ]},
    description="每达标一项得4分，共5项满分20分",
    item_weight=0.2, sort_order=3)

# 4) RATING - 评分量表 (1-5星)
create_item(S1_ID, "SAFETY-04", "安全文化氛围", "RATING",
    scoring_config={"mode": "RATING_SCALE", "minRating": 1, "maxRating": 5, "scorePerStar": 4, "maxScore": 20},
    config={"stars": 5, "labels": ["很差", "较差", "一般", "良好", "优秀"]},
    description="对安全文化建设进行1-5星评价",
    item_weight=0.2, sort_order=4)

# 5) RADIO - 等级制选择 (A/B/C/D)
create_item(S1_ID, "SAFETY-05", "安全管理综合评级", "RADIO",
    scoring_config={"mode": "LEVEL", "levels": [
        {"label": "A-优秀", "score": 20},
        {"label": "B-良好", "score": 15},
        {"label": "C-合格", "score": 10},
        {"label": "D-不合格", "score": 0}
    ], "maxScore": 20},
    config={"options": [
        {"label": "A-优秀", "value": "A"},
        {"label": "B-良好", "value": "B"},
        {"label": "C-合格", "value": "C"},
        {"label": "D-不合格", "value": "D"}
    ]},
    description="综合评定安全管理等级",
    item_weight=0.2, sort_order=5)


# ============================================================
# Section 2: 纪律检查 - items
# ============================================================
print(f"\n=== 纪律检查 (Section {S2_ID}) - 检查项 ===")

# 1) PASS_FAIL - 出勤达标
create_item(S2_ID, "DISC-01", "班级出勤率达标", "PASS_FAIL",
    scoring_config={"mode": "PASS_FAIL", "passScore": 15, "failScore": 0},
    description="出勤率>=95%为通过",
    item_weight=0.15, sort_order=1)

# 2) NUMBER + DIRECT - 直接打分
create_item(S2_ID, "DISC-02", "课堂纪律评分", "NUMBER",
    scoring_config={"mode": "DIRECT", "maxScore": 20, "minScore": 0},
    config={"min": 0, "max": 20, "step": 1, "unit": "分"},
    description="检查人直接对课堂纪律打0-20分",
    item_weight=0.2, sort_order=2)

# 3) ADDITION - 加分制
create_item(S2_ID, "DISC-03", "纪律表现加分项", "CHECKLIST",
    scoring_config={"mode": "ADDITION", "maxScore": 20, "items": [
        {"label": "班干部带头示范", "score": 5},
        {"label": "全班无违纪记录", "score": 5},
        {"label": "积极参与学校活动", "score": 5},
        {"label": "获得纪律标兵班级", "score": 5}
    ]},
    config={"options": [
        {"label": "班干部带头示范", "value": "leader"},
        {"label": "全班无违纪记录", "value": "no_violation"},
        {"label": "积极参与学校活动", "value": "active"},
        {"label": "获得纪律标兵班级", "value": "model"}
    ]},
    description="每达标一项加5分，满分20分",
    item_weight=0.2, sort_order=3)

# 4) SLIDER - 滑动打分 (TIERED_DEDUCTION)
create_item(S2_ID, "DISC-04", "迟到早退情况", "SLIDER",
    scoring_config={"mode": "TIERED_DEDUCTION", "maxScore": 20, "tiers": [
        {"threshold": 0, "deduction": 0, "label": "无迟到"},
        {"threshold": 3, "deduction": 5, "label": "1-3人次"},
        {"threshold": 6, "deduction": 10, "label": "4-6人次"},
        {"threshold": 10, "deduction": 20, "label": "7人次以上"}
    ]},
    config={"min": 0, "max": 20, "step": 1},
    description="根据迟到早退人次数分档扣分",
    item_weight=0.2, sort_order=4)

# 5) VIOLATION_RECORD - 违纪记录
create_item(S2_ID, "DISC-05", "违纪事件记录", "VIOLATION_RECORD",
    scoring_config={"mode": "DEDUCTION", "maxScore": 25, "deductPerViolation": 5, "minScore": 0},
    description="记录违纪事件，每条扣5分",
    require_evidence=True, item_weight=0.25, sort_order=5)


# ============================================================
# Section 3: 宿舍检查 - items
# ============================================================
print(f"\n=== 宿舍检查 (Section {S3_ID}) - 检查项 ===")

# 1) PASS_FAIL - 违禁物品检查
create_item(S3_ID, "DORM-01", "有无违禁物品", "PASS_FAIL",
    scoring_config={"mode": "PASS_FAIL", "passScore": 15, "failScore": 0},
    description="检查是否存在违禁电器、管制刀具等",
    require_evidence=True, item_weight=0.2, sort_order=1)

# 2) RATING - 内务整理评分
create_item(S3_ID, "DORM-02", "内务整理评分", "RATING",
    scoring_config={"mode": "RATING_SCALE", "minRating": 1, "maxRating": 5, "scorePerStar": 4, "maxScore": 20},
    config={"stars": 5, "labels": ["极差", "较差", "一般", "良好", "优秀"]},
    description="被褥叠放、物品摆放、地面整洁等",
    item_weight=0.2, sort_order=2)

# 3) SCORE_TABLE - 查表打分
create_item(S3_ID, "DORM-03", "宿舍卫生等级", "SELECT",
    scoring_config={"mode": "SCORE_TABLE", "table": [
        {"value": "excellent", "score": 25, "label": "优秀-整洁无死角"},
        {"value": "good", "score": 20, "label": "良好-基本干净"},
        {"value": "fair", "score": 12, "label": "一般-有待改进"},
        {"value": "poor", "score": 5, "label": "较差-明显脏乱"},
        {"value": "fail", "score": 0, "label": "不合格-严重脏乱差"}
    ], "maxScore": 25},
    config={"options": [
        {"label": "优秀-整洁无死角", "value": "excellent"},
        {"label": "良好-基本干净", "value": "good"},
        {"label": "一般-有待改进", "value": "fair"},
        {"label": "较差-明显脏乱", "value": "poor"},
        {"label": "不合格-严重脏乱差", "value": "fail"}
    ]},
    description="根据卫生等级对应分值",
    item_weight=0.25, sort_order=3)

# 4) PHOTO + THRESHOLD - 需拍照 + 阈值评分
create_item(S3_ID, "DORM-04", "设施损坏情况", "NUMBER",
    scoring_config={"mode": "THRESHOLD", "maxScore": 20, "thresholds": [
        {"min": 0, "max": 0, "score": 20, "label": "无损坏"},
        {"min": 1, "max": 2, "score": 15, "label": "轻微损坏"},
        {"min": 3, "max": 5, "score": 8, "label": "中度损坏"},
        {"min": 6, "max": 999, "score": 0, "label": "严重损坏"}
    ]},
    config={"min": 0, "max": 99, "step": 1, "unit": "处"},
    description="记录设施损坏处数，按阈值评分",
    require_evidence=True, item_weight=0.2, sort_order=4)

# 5) PERSON_SCORE - 按人评分
create_item(S3_ID, "DORM-05", "宿舍成员个人表现", "PERSON_SCORE",
    scoring_config={"mode": "DIRECT", "maxScore": 20, "minScore": 0},
    description="对宿舍每位成员的个人卫生和纪律逐一评分",
    item_weight=0.15, sort_order=5)


# ============================================================
# Section 4: 卫生检查 - items
# ============================================================
print(f"\n=== 卫生检查 (Section {S4_ID}) - 检查项 ===")

# 1) RADIO + LEVEL - 地面卫生等级
create_item(S4_ID, "HYG-01", "地面卫生状况", "RADIO",
    scoring_config={"mode": "LEVEL", "levels": [
        {"label": "A-一尘不染", "score": 20},
        {"label": "B-干净整洁", "score": 15},
        {"label": "C-基本干净", "score": 10},
        {"label": "D-有明显垃圾", "score": 5},
        {"label": "E-严重脏乱", "score": 0}
    ], "maxScore": 20},
    config={"options": [
        {"label": "A-一尘不染", "value": "A"},
        {"label": "B-干净整洁", "value": "B"},
        {"label": "C-基本干净", "value": "C"},
        {"label": "D-有明显垃圾", "value": "D"},
        {"label": "E-严重脏乱", "value": "E"}
    ]},
    description="教室地面、走廊卫生整体评级",
    item_weight=0.2, sort_order=1)

# 2) MULTI_SELECT + WEIGHTED_MULTI - 加权多项
create_item(S4_ID, "HYG-02", "卫生细项检查", "MULTI_SELECT",
    scoring_config={"mode": "WEIGHTED_MULTI", "maxScore": 25, "items": [
        {"label": "黑板擦拭干净", "weight": 0.2, "score": 5},
        {"label": "桌椅摆放整齐", "weight": 0.2, "score": 5},
        {"label": "窗台无灰尘", "weight": 0.2, "score": 5},
        {"label": "垃圾桶已清理", "weight": 0.2, "score": 5},
        {"label": "门窗玻璃干净", "weight": 0.2, "score": 5}
    ]},
    config={"options": [
        {"label": "黑板擦拭干净", "value": "blackboard"},
        {"label": "桌椅摆放整齐", "value": "desks"},
        {"label": "窗台无灰尘", "value": "windowsill"},
        {"label": "垃圾桶已清理", "value": "trash"},
        {"label": "门窗玻璃干净", "value": "glass"}
    ]},
    description="每项5分，共25分",
    item_weight=0.25, sort_order=2)

# 3) NUMBER + RISK_MATRIX - 风险矩阵
create_item(S4_ID, "HYG-03", "卫生问题风险评估", "NUMBER",
    scoring_config={"mode": "RISK_MATRIX", "maxScore": 20, "dimensions": {
        "severity": {"label": "严重程度", "levels": ["低", "中", "高"]},
        "likelihood": {"label": "发生频率", "levels": ["偶尔", "经常", "持续"]}
    }, "matrix": [
        [20, 15, 10],
        [15, 10, 5],
        [10, 5, 0]
    ]},
    config={"min": 0, "max": 20, "step": 1},
    description="结合严重程度和发生频率的风险矩阵评分",
    item_weight=0.2, sort_order=3)

# 4) PASS_FAIL - 垃圾分类
create_item(S4_ID, "HYG-04", "垃圾分类是否达标", "PASS_FAIL",
    scoring_config={"mode": "PASS_FAIL", "passScore": 15, "failScore": 0},
    description="检查垃圾是否按要求分类投放",
    require_evidence=True, item_weight=0.15, sort_order=4)

# 5) TEXTAREA + FORMULA - 公式计算（文字描述+手动评分）
create_item(S4_ID, "HYG-05", "卫生综合评价", "NUMBER",
    scoring_config={"mode": "FORMULA", "maxScore": 20, "formula": "score",
                    "description": "根据整体卫生情况综合打分"},
    config={"min": 0, "max": 20, "step": 0.5, "unit": "分"},
    description="结合以上各项情况给出综合卫生分数",
    item_weight=0.2, sort_order=5)


# ============================================================
# 5. Publish template
# ============================================================
print(f"\n=== 发布模板 (Root ID: {ROOT_ID}) ===")
pub_result = api("POST", f"/v7/insp/templates/{ROOT_ID}/publish", None, token)
print(f"发布结果: {json.dumps(pub_result, ensure_ascii=False)[:200]}")

# ============================================================
# 6. Verify - get tree
# ============================================================
print(f"\n=== 验证: 获取模板树 ===")
tree = api("GET", f"/v7/insp/sections/tree/{ROOT_ID}", token=token)
sections = tree["data"]
print(f"分区总数: {len(sections)}")
for s in sections:
    indent = "  " if s.get("parentSectionId") else ""
    item_count = s.get("itemCount", "?")
    print(f"{indent}[{s['sectionCode']}] {s['sectionName']} (weight:{s.get('weight','-')}, target:{s.get('targetType','-')})")

# Get items per section
for sid, sname in [(S1_ID, "安全检查"), (S2_ID, "纪律检查"), (S3_ID, "宿舍检查"), (S4_ID, "卫生检查")]:
    items_resp = api("GET", f"/v7/insp/sections/{sid}/items", token=token)
    items = items_resp["data"]
    print(f"\n{sname} ({len(items)} items):")
    for it in items:
        sc = json.loads(it.get("scoringConfig", "{}")) if it.get("scoringConfig") else {}
        mode = sc.get("mode", "N/A")
        print(f"  [{it['itemType']:15s}] {it['itemName']} | scoring: {mode}")

print("\n=== 完成! ===")
print(f"模板ID: {ROOT_ID}")
print(f"分区: 安全检查({S1_ID}), 纪律检查({S2_ID}), 宿舍检查({S3_ID}), 卫生检查({S4_ID})")
