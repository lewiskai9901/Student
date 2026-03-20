# -*- coding: utf-8 -*-
"""Continue setup from where create_chinese_template.py stopped."""
import requests
import json
import sys
import os

os.environ['NO_PROXY'] = 'localhost,127.0.0.1'
session = requests.Session()
session.trust_env = False

BASE = "http://localhost:8080/api"

r = session.post(f"{BASE}/auth/login", json={"username": "admin", "password": "admin123"})
token = r.json()["data"]["accessToken"]
H = {"Authorization": f"Bearer {token}", "Content-Type": "application/json"}

def api(method, path, data=None):
    url = f"{BASE}{path}"
    resp = getattr(session, method)(url, json=data, headers=H)
    if resp.status_code != 200:
        print(f"FAIL {method.upper()} {path}: {resp.status_code} {resp.text[:300]}")
        sys.exit(1)
    body = resp.json()
    if body.get("code") != 200:
        print(f"FAIL {method.upper()} {path}: code={body.get('code')} msg={body.get('message','')[:200]}")
        sys.exit(1)
    return body.get("data")

# Known IDs from previous run
tpl_id = 13
version_id = 4
proj_id = 4
section_ids = [11, 12, 13]

# Section and item metadata for creating details
sections_meta = [
    {"sectionName": "\u536b\u751f\u68c0\u67e5", "items": [  # 卫生检查
        {"itemCode": "WS-01", "itemName": "\u6559\u5ba4\u5730\u9762\u6e05\u6d01", "itemType": "PASS_FAIL", "scoringMode": "PASS_FAIL"},
        {"itemCode": "WS-02", "itemName": "\u9ed1\u677f\u64e6\u62ed\u60c5\u51b5", "itemType": "PASS_FAIL", "scoringMode": "PASS_FAIL"},
        {"itemCode": "WS-03", "itemName": "\u5783\u573e\u672a\u53ca\u65f6\u6e05\u7406", "itemType": "NUMBER", "scoringMode": "DEDUCTION"},
        {"itemCode": "WS-04", "itemName": "\u7a97\u53f0\u684c\u9762\u79ef\u7070", "itemType": "NUMBER", "scoringMode": "DEDUCTION"},
        {"itemCode": "WS-05", "itemName": "\u7eff\u5316\u89d2\u7ef4\u62a4\u60c5\u51b5", "itemType": "RATING", "scoringMode": "DIRECT"},
    ]},
    {"sectionName": "\u5bbf\u820d\u68c0\u67e5", "items": [  # 宿舍检查
        {"itemCode": "SS-01", "itemName": "\u5e8a\u94fa\u6574\u7406\u89c4\u8303", "itemType": "PASS_FAIL", "scoringMode": "PASS_FAIL"},
        {"itemCode": "SS-02", "itemName": "\u5730\u9762\u536b\u751f\u72b6\u51b5", "itemType": "NUMBER", "scoringMode": "DEDUCTION"},
        {"itemCode": "SS-03", "itemName": "\u7269\u54c1\u6446\u653e\u6574\u9f50\u5ea6", "itemType": "RATING", "scoringMode": "DIRECT"},
        {"itemCode": "SS-04", "itemName": "\u8282\u80fd\u73af\u4fdd\u8868\u73b0", "itemType": "NUMBER", "scoringMode": "ADDITION"},
        {"itemCode": "SS-05", "itemName": "\u95e8\u7a97\u8bbe\u65bd\u5b8c\u597d", "itemType": "PASS_FAIL", "scoringMode": "PASS_FAIL"},
    ]},
    {"sectionName": "\u7eaa\u5f8b\u68c0\u67e5", "items": [  # 纪律检查
        {"itemCode": "JL-01", "itemName": "\u6309\u65f6\u5230\u6821\u51fa\u52e4", "itemType": "PASS_FAIL", "scoringMode": "PASS_FAIL"},
        {"itemCode": "JL-02", "itemName": "\u8bfe\u5802\u8fdd\u7eaa\u884c\u4e3a", "itemType": "NUMBER", "scoringMode": "DEDUCTION"},
        {"itemCode": "JL-03", "itemName": "\u7a7f\u7740\u6821\u670d\u89c4\u8303", "itemType": "PASS_FAIL", "scoringMode": "PASS_FAIL"},
        {"itemCode": "JL-04", "itemName": "\u89c1\u4e49\u52c7\u4e3a/\u52a9\u4eba\u4e3a\u4e50", "itemType": "NUMBER", "scoringMode": "ADDITION"},
        {"itemCode": "JL-05", "itemName": "\u8bfe\u95f4\u64cd\u8868\u73b0\u8bc4\u5206", "itemType": "RATING", "scoringMode": "DIRECT"},
    ]},
]

# 1. Publish project
print("=== Publish project ===")
api("post", f"/v7/insp/projects/{proj_id}/publish", {"templateVersionId": version_id})
print("Project published")

# 2. Create task
print("\n=== Create task ===")
task = api("post", "/v7/insp/tasks", {
    "projectId": proj_id,
    "taskDate": "2026-02-26",
    "timeSlotCode": "AM",
    "timeSlotStart": "08:00",
    "timeSlotEnd": "12:00"
})
task_id = task["id"]
print(f"Task ID: {task_id}, Code: {task['taskCode']}")

# 3. Claim & start task
print("\n=== Claim & start ===")
api("post", f"/v7/insp/tasks/{task_id}/claim", {"inspectorName": "\u7ba1\u7406\u5458"})  # 管理员
print("Claimed")
api("post", f"/v7/insp/tasks/{task_id}/start")
print("Started")

# 4. Create submissions
print("\n=== Create submissions ===")
targets = [
    {"targetName": "\u9ad8\u4e00(1)\u73ed", "targetType": "ORG", "targetId": 1, "orgUnitName": "\u9ad8\u4e00\u5e74\u7ea7"},  # 高一(1)班 / 高一年级
    {"targetName": "\u9ad8\u4e00(2)\u73ed", "targetType": "ORG", "targetId": 2, "orgUnitName": "\u9ad8\u4e00\u5e74\u7ea7"},  # 高一(2)班
    {"targetName": "\u9ad8\u4e8c(1)\u73ed", "targetType": "ORG", "targetId": 3, "orgUnitName": "\u9ad8\u4e8c\u5e74\u7ea7"},  # 高二(1)班 / 高二年级
]

sub_ids = []
for t in targets:
    sub = api("post", f"/v7/insp/tasks/{task_id}/submissions", t)
    sub_ids.append(sub["id"])
    print(f"  Submission ID={sub['id']}")

# 5. Create details for each submission
print("\n=== Create details ===")
for sub_id in sub_ids:
    count = 0
    for sec_idx, sec_meta in enumerate(sections_meta):
        sec_name = sec_meta["sectionName"]
        sec_id = section_ids[sec_idx]
        for item in sec_meta["items"]:
            api("post", f"/v7/insp/submissions/{sub_id}/details", {
                "templateItemId": 0,
                "itemCode": item["itemCode"],
                "itemName": item["itemName"],
                "sectionId": sec_id,
                "sectionName": sec_name,
                "itemType": item["itemType"],
                "scoringMode": item["scoringMode"],
                "isScored": True
            })
            count += 1
    print(f"  Sub {sub_id}: {count} details created")

print(f"\n=== DONE ===")
print(f"Task ID: {task_id}")
print(f"Submissions: {sub_ids}")
print(f"URL: http://localhost:3000/inspection/v7/tasks/{task_id}/execute")
