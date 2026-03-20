# -*- coding: utf-8 -*-
"""Create Chinese inspection template with all 4 scoring modes - v2 (uses existing template).
Now includes scoringConfig, validationRules, and conditionLogic per detail."""
import requests
import json
import sys
import os

os.environ['NO_PROXY'] = 'localhost,127.0.0.1'
s = requests.Session()
s.trust_env = False

BASE = "http://localhost:8080/api"

r = s.post(f"{BASE}/auth/login", json={"username": "admin", "password": "admin123"})
token = r.json()["data"]["accessToken"]
H = {"Authorization": f"Bearer {token}", "Content-Type": "application/json"}

def api(method, path, data=None):
    url = f"{BASE}{path}"
    resp = getattr(s, method)(url, json=data, headers=H)
    body = resp.json()
    if body.get("code") != 200:
        print(f"FAIL {method.upper()} {path}: {resp.status_code} code={body.get('code')} msg={body.get('message','')[:200]}")
        sys.exit(1)
    return body.get("data")

# Existing IDs from previous run
tpl_id = 13
version_id = 4

# 1. Create new project
print("1. Create project")
project = api("post", "/v7/insp/projects", {
    "projectName": "2026\u5e74\u6625\u5b63\u5b66\u671f\u7efc\u5408\u68c0\u67e5",
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
print(f"   Project ID: {proj_id}")

# 2. Publish project
print("2. Publish project")
api("post", f"/v7/insp/projects/{proj_id}/publish", {"templateVersionId": version_id})
print("   Published")

# 3. Create task
print("3. Create task")
task = api("post", "/v7/insp/tasks", {
    "projectId": proj_id,
    "taskDate": "2026-02-26",
    "timeSlotCode": "AM",
    "timeSlotStart": "08:00",
    "timeSlotEnd": "12:00"
})
task_id = task["id"]
print(f"   Task ID: {task_id} Code: {task['taskCode']}")

# 4. Claim & start
print("4. Claim & start task")
api("post", f"/v7/insp/tasks/{task_id}/claim", {"inspectorName": "\u7ba1\u7406\u5458"})
api("post", f"/v7/insp/tasks/{task_id}/start")
print("   Done")

# 5. Create submissions
print("5. Create submissions")
targets = [
    ("\u9ad8\u4e00(1)\u73ed", 1),  # 高一(1)班
    ("\u9ad8\u4e00(2)\u73ed", 2),  # 高一(2)班
    ("\u9ad8\u4e8c(1)\u73ed", 3),  # 高二(1)班
]
sub_ids = []
for name, tid in targets:
    sub = api("post", "/v7/insp/submissions", {
        "taskId": task_id,
        "targetType": "ORG",
        "targetId": tid,
        "targetName": name
    })
    sub_ids.append(sub["id"])
    print(f"   {name} -> Sub ID: {sub['id']}")

# 6. Create details for each submission
# Format: (item_code, item_name, item_type, scoring_mode, scoring_config, validation_rules, condition_logic)
print("6. Create details")

# Scoring configs per mode
SC_PASS_FAIL = json.dumps({"mode": "PASS_FAIL", "passScore": 0, "failScore": -2})
SC_PASS_FAIL_HEAVY = json.dumps({"mode": "PASS_FAIL", "passScore": 0, "failScore": -5})
SC_DEDUCTION = json.dumps({"mode": "DEDUCTION", "maxDeduction": -10, "deductionStep": 1})
SC_DEDUCTION_LIGHT = json.dumps({"mode": "DEDUCTION", "maxDeduction": -5, "deductionStep": 1})
SC_ADDITION = json.dumps({"mode": "ADDITION", "maxBonus": 5, "bonusStep": 1})
SC_ADDITION_BIG = json.dumps({"mode": "ADDITION", "maxBonus": 10, "bonusStep": 2})
SC_DIRECT = json.dumps({"mode": "DIRECT", "minScore": 0, "maxScore": 10})
SC_DIRECT_20 = json.dumps({"mode": "DIRECT", "minScore": 0, "maxScore": 20})

# Validation rules
VR_REQUIRED = json.dumps([{"type": "required", "message": "\u6b64\u9879\u5fc5\u987b\u586b\u5199"}])
VR_REQUIRED_IF_FAIL = json.dumps([{"type": "requiredIfFail", "message": "\u4e0d\u901a\u8fc7\u65f6\u5fc5\u987b\u586b\u5199\u539f\u56e0"}])

# Condition logic examples: WS-04 only visible if WS-03 score < 0 (deducted)
CL_SHOW_IF_WS03_DEDUCTED = json.dumps({"when": "WS-03", "operator": "lessThan", "value": "0", "action": "show"})
# SS-04 (bonus) only visible if SS-01 is PASS
CL_SHOW_IF_SS01_PASS = json.dumps({"when": "SS-01", "operator": "equals", "value": "PASS", "action": "show"})

sections = [
    (11, "\u536b\u751f\u68c0\u67e5", [  # 卫生检查
        ("WS-01", "\u6559\u5ba4\u5730\u9762\u6e05\u6d01", "PASS_FAIL", "PASS_FAIL", SC_PASS_FAIL, VR_REQUIRED, None),
        ("WS-02", "\u9ed1\u677f\u64e6\u62ed\u60c5\u51b5", "PASS_FAIL", "PASS_FAIL", SC_PASS_FAIL, VR_REQUIRED_IF_FAIL, None),
        ("WS-03", "\u5783\u573e\u672a\u53ca\u65f6\u6e05\u7406", "NUMBER", "DEDUCTION", SC_DEDUCTION, None, None),
        ("WS-04", "\u7a97\u53f0\u684c\u9762\u79ef\u7070", "NUMBER", "DEDUCTION", SC_DEDUCTION_LIGHT, None, CL_SHOW_IF_WS03_DEDUCTED),
        ("WS-05", "\u7eff\u5316\u89d2\u7ef4\u62a4\u60c5\u51b5", "RATING", "DIRECT", SC_DIRECT, None, None),
    ]),
    (12, "\u5bbf\u820d\u68c0\u67e5", [  # 宿舍检查
        ("SS-01", "\u5e8a\u94fa\u6574\u7406\u89c4\u8303", "PASS_FAIL", "PASS_FAIL", SC_PASS_FAIL_HEAVY, VR_REQUIRED, None),
        ("SS-02", "\u5730\u9762\u536b\u751f\u72b6\u51b5", "NUMBER", "DEDUCTION", SC_DEDUCTION, None, None),
        ("SS-03", "\u7269\u54c1\u6446\u653e\u6574\u9f50\u5ea6", "RATING", "DIRECT", SC_DIRECT_20, None, None),
        ("SS-04", "\u8282\u80fd\u73af\u4fdd\u8868\u73b0", "NUMBER", "ADDITION", SC_ADDITION, None, CL_SHOW_IF_SS01_PASS),
        ("SS-05", "\u95e8\u7a97\u8bbe\u65bd\u5b8c\u597d", "PASS_FAIL", "PASS_FAIL", SC_PASS_FAIL, VR_REQUIRED_IF_FAIL, None),
    ]),
    (13, "\u7eaa\u5f8b\u68c0\u67e5", [  # 纪律检查
        ("JL-01", "\u6309\u65f6\u5230\u6821\u51fa\u52e4", "PASS_FAIL", "PASS_FAIL", SC_PASS_FAIL, VR_REQUIRED, None),
        ("JL-02", "\u8bfe\u5802\u8fdd\u7eaa\u884c\u4e3a", "NUMBER", "DEDUCTION", SC_DEDUCTION, None, None),
        ("JL-03", "\u7a7f\u7740\u6821\u670d\u89c4\u8303", "PASS_FAIL", "PASS_FAIL", SC_PASS_FAIL, VR_REQUIRED_IF_FAIL, None),
        ("JL-04", "\u89c1\u4e49\u52c7\u4e3a/\u52a9\u4eba\u4e3a\u4e50", "NUMBER", "ADDITION", SC_ADDITION_BIG, None, None),
        ("JL-05", "\u8bfe\u95f4\u64cd\u8868\u73b0\u8bc4\u5206", "RATING", "DIRECT", SC_DIRECT, None, None),
    ]),
]

for sub_id in sub_ids:
    count = 0
    for sec_id, sec_name, items in sections:
        for item_code, item_name, item_type, scoring_mode, scoring_config, validation_rules, condition_logic in items:
            payload = {
                "templateItemId": 0,
                "itemCode": item_code,
                "itemName": item_name,
                "itemType": item_type,
                "sectionId": sec_id,
                "sectionName": sec_name,
                "scoringMode": scoring_mode,
                "scoringConfig": scoring_config,
            }
            if validation_rules:
                payload["validationRules"] = validation_rules
            if condition_logic:
                payload["conditionLogic"] = condition_logic
            api("post", f"/v7/insp/submissions/{sub_id}/details", payload)
            count += 1
    print(f"   Sub {sub_id}: {count} details")

print(f"\n=== DONE ===")
print(f"Task ID: {task_id}")
print(f"URL: http://localhost:3000/inspection/v7/tasks/{task_id}/execute")
