# -*- coding: utf-8 -*-
"""Create Chinese inspection template with all 4 scoring modes."""
import requests
import json
import sys
import os

# Disable proxy for localhost
os.environ['NO_PROXY'] = 'localhost,127.0.0.1'
session = requests.Session()
session.trust_env = False

BASE = "http://localhost:8080/api"

# Login
r = session.post(f"{BASE}/auth/login", json={"username": "admin", "password": "admin123"})
token = r.json()["data"]["accessToken"]
H = {"Authorization": f"Bearer {token}", "Content-Type": "application/json"}

def api(method, path, data=None):
    url = f"{BASE}{path}"
    resp = getattr(session, method)(url, json=data, headers=H)
    if resp.status_code != 200:
        print(f"FAIL {method.upper()} {path}: {resp.status_code} {resp.text[:200]}")
        sys.exit(1)
    body = resp.json()
    if body.get("code") != 200:
        print(f"FAIL {method.upper()} {path}: {body}")
        sys.exit(1)
    return body.get("data")

# 1. Create template
print("=== 创建模板 ===")
tpl = api("post", "/v7/insp/templates", {
    "templateName": "综合检查模板（全模式）",
    "description": "包含卫生、宿舍、纪律三大类检查，覆盖通过/不通过、扣分、加分、直接打分四种评分模式"
})
tpl_id = tpl["id"]
print(f"模板ID: {tpl_id}")

# 2. Create sections
print("\n=== 创建分区 ===")
sections = [
    {"sectionName": "卫生检查", "sectionCode": "WS", "sortOrder": 1},
    {"sectionName": "宿舍检查", "sectionCode": "SS", "sortOrder": 2},
    {"sectionName": "纪律检查", "sectionCode": "JL", "sortOrder": 3},
]
section_ids = []
for s in sections:
    sec = api("post", f"/v7/insp/templates/{tpl_id}/sections", s)
    section_ids.append(sec["id"])
    print(f"  分区: {s['sectionName']} -> ID={sec['id']}")

# 3. Create items (15 items, 5 per section)
print("\n=== 创建检查项 ===")

# 卫生检查 (section 0)
hygiene_items = [
    {"itemCode": "WS-01", "itemName": "教室地面清洁", "itemType": "PASS_FAIL", "scoringMode": "PASS_FAIL",
     "scoringConfig": json.dumps({"passScore": 0, "failScore": -2}), "isScored": True, "isRequired": True, "sortOrder": 1},
    {"itemCode": "WS-02", "itemName": "黑板擦拭情况", "itemType": "PASS_FAIL", "scoringMode": "PASS_FAIL",
     "scoringConfig": json.dumps({"passScore": 0, "failScore": -2}), "isScored": True, "isRequired": True, "sortOrder": 2},
    {"itemCode": "WS-03", "itemName": "垃圾未及时清理", "itemType": "NUMBER", "scoringMode": "DEDUCTION",
     "scoringConfig": json.dumps({"minDeduction": -5, "maxDeduction": 0}), "isScored": True, "isRequired": True, "sortOrder": 3},
    {"itemCode": "WS-04", "itemName": "窗台桌面积灰", "itemType": "NUMBER", "scoringMode": "DEDUCTION",
     "scoringConfig": json.dumps({"minDeduction": -3, "maxDeduction": 0}), "isScored": True, "isRequired": True, "sortOrder": 4},
    {"itemCode": "WS-05", "itemName": "绿化角维护情况", "itemType": "RATING", "scoringMode": "DIRECT",
     "scoringConfig": json.dumps({"minScore": 0, "maxScore": 10}), "isScored": True, "isRequired": True, "sortOrder": 5},
]

# 宿舍检查 (section 1)
dorm_items = [
    {"itemCode": "SS-01", "itemName": "床铺整理规范", "itemType": "PASS_FAIL", "scoringMode": "PASS_FAIL",
     "scoringConfig": json.dumps({"passScore": 0, "failScore": -2}), "isScored": True, "isRequired": True, "sortOrder": 1},
    {"itemCode": "SS-02", "itemName": "地面卫生状况", "itemType": "NUMBER", "scoringMode": "DEDUCTION",
     "scoringConfig": json.dumps({"minDeduction": -5, "maxDeduction": 0}), "isScored": True, "isRequired": True, "sortOrder": 2},
    {"itemCode": "SS-03", "itemName": "物品摆放整齐度", "itemType": "RATING", "scoringMode": "DIRECT",
     "scoringConfig": json.dumps({"minScore": 0, "maxScore": 10}), "isScored": True, "isRequired": True, "sortOrder": 3},
    {"itemCode": "SS-04", "itemName": "节能环保表现", "itemType": "NUMBER", "scoringMode": "ADDITION",
     "scoringConfig": json.dumps({"minBonus": 0, "maxBonus": 3}), "isScored": True, "isRequired": True, "sortOrder": 4},
    {"itemCode": "SS-05", "itemName": "门窗设施完好", "itemType": "PASS_FAIL", "scoringMode": "PASS_FAIL",
     "scoringConfig": json.dumps({"passScore": 0, "failScore": -2}), "isScored": True, "isRequired": True, "sortOrder": 5},
]

# 纪律检查 (section 2)
discipline_items = [
    {"itemCode": "JL-01", "itemName": "按时到校出勤", "itemType": "PASS_FAIL", "scoringMode": "PASS_FAIL",
     "scoringConfig": json.dumps({"passScore": 0, "failScore": -2}), "isScored": True, "isRequired": True, "sortOrder": 1},
    {"itemCode": "JL-02", "itemName": "课堂违纪行为", "itemType": "NUMBER", "scoringMode": "DEDUCTION",
     "scoringConfig": json.dumps({"minDeduction": -5, "maxDeduction": 0}), "isScored": True, "isRequired": True, "sortOrder": 2},
    {"itemCode": "JL-03", "itemName": "穿着校服规范", "itemType": "PASS_FAIL", "scoringMode": "PASS_FAIL",
     "scoringConfig": json.dumps({"passScore": 0, "failScore": -2}), "isScored": True, "isRequired": True, "sortOrder": 3},
    {"itemCode": "JL-04", "itemName": "见义勇为/助人为乐", "itemType": "NUMBER", "scoringMode": "ADDITION",
     "scoringConfig": json.dumps({"minBonus": 0, "maxBonus": 5}), "isScored": True, "isRequired": True, "sortOrder": 4},
    {"itemCode": "JL-05", "itemName": "课间操表现评分", "itemType": "RATING", "scoringMode": "DIRECT",
     "scoringConfig": json.dumps({"minScore": 0, "maxScore": 10}), "isScored": True, "isRequired": True, "sortOrder": 5},
]

all_items = [
    (section_ids[0], hygiene_items),
    (section_ids[1], dorm_items),
    (section_ids[2], discipline_items),
]

for sec_id, items in all_items:
    for item in items:
        created = api("post", f"/v7/insp/sections/{sec_id}/items", item)
        print(f"  [{item['scoringMode']:10s}] {item['itemName']} -> ID={created['id']}")

# 4. Publish template
print("\n=== 发布模板 ===")
version = api("post", f"/v7/insp/templates/{tpl_id}/publish")
version_id = version["id"]
print(f"版本ID: {version_id}")

# 5. Create project
print("\n=== 创建检查项目 ===")
project = api("post", "/v7/insp/projects", {
    "projectName": "2026年春季学期综合检查",
    "templateId": tpl_id,
    "templateVersionId": version_id,
    "scopeType": "ORG",
    "targetType": "ORG",
    "cycleType": "DAILY",
    "startDate": "2026-02-01",
    "endDate": "2026-07-01",
    "reviewRequired": False,
    "autoPublish": True
})
proj_id = project["id"]
print(f"项目ID: {proj_id}")

# 6. Publish project
print("\n=== 发布项目 ===")
api("post", f"/v7/insp/projects/{proj_id}/publish", {"templateVersionId": version_id})
print("项目已发布")

# 7. Create task
print("\n=== 创建任务 ===")
task = api("post", "/v7/insp/tasks", {
    "projectId": proj_id,
    "taskDate": "2026-02-26",
    "timeSlotCode": "AM",
    "timeSlotStart": "08:00",
    "timeSlotEnd": "12:00"
})
task_id = task["id"]
print(f"任务ID: {task_id}, 编号: {task['taskCode']}")

# 8. Claim & start task
print("\n=== 领取并开始任务 ===")
api("post", f"/v7/insp/tasks/{task_id}/claim", {"inspectorName": "管理员"})
print("任务已领取")
api("post", f"/v7/insp/tasks/{task_id}/start")
print("任务已开始")

# 9. Create submissions (3 targets with Chinese names)
print("\n=== 创建检查对象 ===")
targets = [
    {"targetName": "高一(1)班", "targetType": "ORG", "targetId": 1, "orgUnitName": "高一年级"},
    {"targetName": "高一(2)班", "targetType": "ORG", "targetId": 2, "orgUnitName": "高一年级"},
    {"targetName": "高二(1)班", "targetType": "ORG", "targetId": 3, "orgUnitName": "高二年级"},
]

sub_ids = []
for t in targets:
    sub = api("post", f"/v7/insp/tasks/{task_id}/submissions", t)
    sub_ids.append(sub["id"])
    print(f"  对象: {t['targetName']} -> 提交ID={sub['id']}")

# 10. Create details for each submission (15 items each)
print("\n=== 创建评分明细 ===")
# Collect all items from the template
all_template_items = []
for sec_id, items in all_items:
    for item in items:
        all_template_items.append(item)

# Get the section names map
section_names = {section_ids[0]: "卫生检查", section_ids[1]: "宿舍检查", section_ids[2]: "纪律检查"}

for sub_id in sub_ids:
    detail_count = 0
    for sec_idx, (sec_id, items) in enumerate(all_items):
        sec_name = sections[sec_idx]["sectionName"]
        for item in items:
            detail = api("post", f"/v7/insp/submissions/{sub_id}/details", {
                "templateItemId": 0,
                "itemCode": item["itemCode"],
                "itemName": item["itemName"],
                "sectionId": sec_id,
                "sectionName": sec_name,
                "itemType": item["itemType"],
                "scoringMode": item["scoringMode"],
                "isScored": True
            })
            detail_count += 1
    print(f"  提交ID={sub_id}: 创建了 {detail_count} 条明细")

print(f"\n=== 完成! ===")
print(f"模板ID: {tpl_id}")
print(f"项目ID: {proj_id}")
print(f"任务ID: {task_id}")
print(f"提交IDs: {sub_ids}")
print(f"\n请访问: http://localhost:3000/inspection/v7/tasks/{task_id}/execute")
