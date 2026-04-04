#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""E2E Test Script for V7 Inspection Platform - Complete Flow"""

import requests
import json
import os
import time
from datetime import date, timedelta

os.environ['NO_PROXY'] = 'localhost,127.0.0.1'
os.environ['no_proxy'] = 'localhost,127.0.0.1'

BASE = 'http://localhost:8080/api'
session = requests.Session()
session.trust_env = False

def api(method, path, data=None, params=None):
    url = f'{BASE}{path}'
    resp = getattr(session, method)(url, json=data, params=params)
    result = resp.json()
    if result.get('code') != 200:
        print(f'  ERROR [{resp.status_code}] {path}: {result.get("message", resp.text[:200])}')
        return None
    return result.get('data')

def login():
    resp = session.post(f'{BASE}/auth/login', json={'username': 'admin', 'password': 'admin123'})
    token = resp.json()['data']['accessToken']
    session.headers.update({'Authorization': f'Bearer {token}', 'Content-Type': 'application/json'})
    print(f'[OK] Logged in as admin')
    return token

def step1_grade_scheme():
    print('\n=== Step 1: Create Grade Scheme ===')
    data = api('post', '/v7/insp/grade-schemes', {
        'displayName': 'E2E五级评分方案',
        'description': 'E2E测试用等级方案',
        'schemeType': 'SCORE_BASED',
        'grades': [
            {'code': 'A', 'name': '优秀', 'minValue': 90, 'maxValue': 100, 'color': '#22c55e', 'icon': 'star', 'sortOrder': 1},
            {'code': 'B', 'name': '良好', 'minValue': 80, 'maxValue': 89.99, 'color': '#3b82f6', 'icon': 'thumbs-up', 'sortOrder': 2},
            {'code': 'C', 'name': '中等', 'minValue': 70, 'maxValue': 79.99, 'color': '#f59e0b', 'icon': 'minus', 'sortOrder': 3},
            {'code': 'D', 'name': '及格', 'minValue': 60, 'maxValue': 69.99, 'color': '#f97316', 'icon': 'alert-triangle', 'sortOrder': 4},
            {'code': 'F', 'name': '不及格', 'minValue': 0, 'maxValue': 59.99, 'color': '#ef4444', 'icon': 'x-circle', 'sortOrder': 5}
        ]
    })
    if not data:
        raise Exception('Failed to create grade scheme')
    print(f'  Grade Scheme ID: {data["id"]}')
    return data

def step2_template():
    print('\n=== Step 2: Create Template (Root + 4 Layers) ===')
    # Create root section (template) using the correct API: name/description/catalogId/tags
    root = api('post', '/v7/insp/templates', {
        'name': 'E2E四层综合检查模板',
        'description': '自动化测试：教学、卫生、安全三大类四层检查',
        'catalogId': None,
        'tags': 'E2E,测试,四层'
    })
    if not root:
        raise Exception('Failed to create template root')
    root_id = root['id']
    print(f'  Root ID: {root_id}')

    # Level 1: 3 major categories
    l1_defs = [('教学管理', 'ORG'), ('卫生环境', 'ORG'), ('安全纪律', 'ORG')]
    l1 = []
    for i, (name, tt) in enumerate(l1_defs):
        code = f'L1-{chr(65+i)}-{str(int(time.time()))[-4:]}'
        sec = api('post', '/v7/insp/sections', {
            'parentSectionId': int(root_id),
            'sectionCode': code,
            'sectionName': name,
            'targetType': tt,
            'sortOrder': i
        })
        if not sec:
            raise Exception(f'Failed to create L1 section: {name}')
        l1.append(sec)
        print(f'  L1[{i}]: {sec["id"]} - {name}')

    # Level 2: 2 sub-categories each
    l2_names = [
        ['课堂教学', '作业批改'],
        ['教室卫生', '公共区域'],
        ['出勤考勤', '行为规范']
    ]
    l2 = []
    for l1_idx, names in enumerate(l2_names):
        for j, name in enumerate(names):
            code = f'L2-{l1_idx}{j}-{str(int(time.time()))[-4:]}'
            sec = api('post', '/v7/insp/sections', {
                'parentSectionId': int(l1[l1_idx]['id']),
                'sectionCode': code,
                'sectionName': name,
                'targetType': 'ORG',
                'sortOrder': j
            })
            if not sec:
                raise Exception(f'Failed to create L2 section: {name}')
            l2.append(sec)
            print(f'    L2[{len(l2)-1}]: {sec["id"]} - {name}')

    # Level 3: 2 detail sections each
    l3_names = [
        ['教学内容', '教学方法'],     # under 课堂教学
        ['批改及时性', '批改质量'],     # under 作业批改
        ['地面清洁', '桌面整理'],       # under 教室卫生
        ['走廊卫生', '厕所卫生'],       # under 公共区域
        ['到班情况', '请假管理'],       # under 出勤考勤
        ['课堂纪律', '文明礼仪']       # under 行为规范
    ]
    l3 = []
    for l2_idx, names in enumerate(l3_names):
        for k, name in enumerate(names):
            code = f'L3-{l2_idx}{k}-{str(int(time.time()))[-4:]}'
            sec = api('post', '/v7/insp/sections', {
                'parentSectionId': int(l2[l2_idx]['id']),
                'sectionCode': code,
                'sectionName': name,
                'targetType': 'ORG',
                'sortOrder': k
            })
            if not sec:
                raise Exception(f'Failed to create L3 section: {name}')
            l3.append(sec)
            print(f'      L3[{len(l3)-1}]: {sec["id"]} - {name}')

    print(f'  Total sections: 1 root + {len(l1)} L1 + {len(l2)} L2 + {len(l3)} L3 = {1+len(l1)+len(l2)+len(l3)}')
    return root, l1, l2, l3

def step3_items(l3_sections):
    print('\n=== Step 3: Add Items to Leaf Sections ===')
    # Different item types for variety (using correct ItemType enum values)
    item_templates = [
        {'itemType': 'NUMBER', 'scoringConfig': '{"maxScore":100,"minScore":0,"step":1}'},
        {'itemType': 'RATING', 'scoringConfig': '{"maxScore":100,"minScore":0,"step":5}'},
        {'itemType': 'PASS_FAIL', 'scoringConfig': '{"passScore":100,"failScore":0}'},
    ]

    all_items = []
    for sec_idx, sec in enumerate(l3_sections):
        sec_id = int(sec['id'])
        for item_idx in range(2):
            tpl = item_templates[(sec_idx * 2 + item_idx) % len(item_templates)]
            code = f'ITM-{sec_idx:02d}-{item_idx}-{str(int(time.time()))[-4:]}'
            name = f'{sec["sectionName"]}检查项{item_idx+1}'
            item = api('post', f'/v7/insp/sections/{sec_id}/items', {
                'itemCode': code,
                'itemName': name,
                'description': f'{name}描述',
                'itemType': tpl['itemType'],
                'scoringConfig': tpl['scoringConfig'],
                'isRequired': True,
                'isScored': True,
                'itemWeight': 1.0,
                'sortOrder': item_idx
            })
            if item:
                all_items.append(item)
            else:
                print(f'  WARN: Failed to create item {code}')
    print(f'  Created {len(all_items)} items across {len(l3_sections)} leaf sections')
    return all_items

def step4_publish(root_id):
    print('\n=== Step 4: Publish Template ===')
    result = api('post', f'/v7/insp/templates/{root_id}/publish')
    if result:
        print(f'  Template published: version={result.get("versionNumber", "?")}')
    else:
        print(f'  WARN: Publish failed, proceeding anyway')
    return result

def step5_project(root_id):
    print('\n=== Step 5: Create Project ===')
    today = date.today()
    project = api('post', '/v7/insp/projects', {
        'projectName': 'E2E四层检查项目',
        'rootSectionId': int(root_id),
        'startDate': str(today)
    })
    if not project:
        raise Exception('Failed to create project')
    print(f'  Project ID: {project["id"]}, Status: {project.get("status")}')
    return project

def step6_configure_project(project_id, root_id, grade_scheme_id):
    print('\n=== Step 6: Configure Project (scope, scoring, etc.) ===')
    # Get org units for scope
    org_units = api('get', '/org-units')
    if isinstance(org_units, dict) and 'records' in org_units:
        org_units = org_units['records']
    if not org_units:
        print('  WARN: No org units found, using dummy')
        org_units = [{'id': 1}]

    target_ids = [str(u['id']) for u in org_units[:5]]
    print(f'  Using {len(target_ids)} org units as targets: {target_ids}')

    # Update project with scope and config
    update_data = {
        'projectName': 'E2E四层检查项目',
        'rootSectionId': int(root_id),
        'scopeType': 'ORG',
        'scopeConfig': json.dumps(target_ids),
        'startDate': str(date.today()),
        'endDate': str(date.today() + timedelta(days=30)),
        'assignmentMode': 'FREE',
        'reviewRequired': False,
        'autoPublish': True
    }
    resp = session.put(f'{BASE}/v7/insp/projects/{project_id}', json=update_data)
    print(f'  Update response: {resp.status_code} {resp.text[:200]}')
    project = resp.json().get('data') if resp.json().get('code') == 200 else None
    if project:
        print(f'  Project updated: scope={len(target_ids)} targets')
    return project, target_ids

def step7_publish_project(project_id, template_version_id=None):
    print('\n=== Step 7: Publish Project ===')
    # Publish requires templateVersionId
    result = api('post', f'/v7/insp/projects/{project_id}/publish', {
        'templateVersionId': int(template_version_id) if template_version_id else None
    })
    if result:
        print(f'  Project published: status={result.get("status")}')
    else:
        print(f'  WARN: Publish may have failed')
    return result

def step8_create_plan(project_id, root_id):
    print('\n=== Step 8: Create Inspection Plan ===')
    plan = api('post', '/v7/insp/plans', {
        'projectId': int(project_id),
        'planName': 'E2E每日检查计划',
        'rootSectionId': int(root_id),
        'sectionIds': '[]',
        'inspectorIds': '[]',
        'scheduleMode': 'MANUAL',
        'cycleType': 'DAILY',
        'frequency': 1,
        'scheduleDays': '[]',
        'timeSlots': json.dumps([{'code': 'AM', 'start': '08:00', 'end': '12:00'}]),
        'skipHolidays': False
    })
    if not plan:
        raise Exception('Failed to create plan')
    print(f'  Plan ID: {plan["id"]}, Status: {plan.get("status")}')
    return plan

def step9_create_task(project_id):
    print('\n=== Step 9: Create Tasks ===')
    today = str(date.today())
    tasks = []
    for i in range(3):  # Create 3 tasks for different time slots
        slots = [
            {'code': 'AM', 'start': '08:00:00', 'end': '12:00:00'},
            {'code': 'PM', 'start': '13:00:00', 'end': '17:00:00'},
            {'code': 'EVE', 'start': '18:00:00', 'end': '21:00:00'}
        ]
        slot = slots[i]
        task = api('post', '/v7/insp/tasks', {
            'projectId': int(project_id),
            'taskDate': today,
            'timeSlotCode': slot['code'],
            'timeSlotStart': slot['start'],
            'timeSlotEnd': slot['end']
        })
        if task:
            tasks.append(task)
            print(f'  Task {i+1}: ID={task["id"]}, Status={task.get("status")}, Slot={slot["code"]}')
        else:
            print(f'  WARN: Failed to create task for slot {slot["code"]}')
    return tasks

def step10_claim_and_score(tasks, target_ids, l3_sections, items):
    print('\n=== Step 10: Claim Tasks & Submit Scores ===')
    import random

    for task_idx, task in enumerate(tasks):
        task_id = task['id']
        print(f'\n  --- Task {task_idx+1} (ID={task_id}) ---')

        # Claim
        claimed = api('post', f'/v7/insp/tasks/{task_id}/claim', {
            'inspectorName': '王浩宇'
        })
        if claimed:
            print(f'    Claimed by 王浩宇')

        # Start
        started = api('post', f'/v7/insp/tasks/{task_id}/start')
        if started:
            print(f'    Started')

        # Get submissions for this task
        submissions = api('get', '/v7/insp/submissions', params={'taskId': int(task_id)})
        if not submissions:
            print(f'    WARN: No submissions found, creating manually')
            # Create submissions for each target
            submissions = []
            for tid in target_ids[:3]:
                sub = api('post', '/v7/insp/submissions', {
                    'taskId': int(task_id),
                    'targetType': 'ORG',
                    'targetId': int(tid)
                })
                if sub:
                    submissions.append(sub)

        if isinstance(submissions, list):
            print(f'    Found {len(submissions)} submissions')
        else:
            print(f'    Submissions response: {str(submissions)[:100]}')
            submissions = []

        # Score each submission
        for sub_idx, sub in enumerate(submissions[:3]):
            sub_id = sub['id']

            # Create details with scores
            for item in items[:6]:  # Score first 6 items
                item_type = item.get('itemType', 'SCORE')
                if item_type == 'SCORE':
                    score = random.randint(60, 100)
                    response_val = str(score)
                elif item_type == 'SINGLE_CHOICE':
                    choice = random.choice(['A', 'B', 'C', 'D'])
                    response_val = choice
                    score = {'A': 100, 'B': 80, 'C': 60, 'D': 40}.get(choice, 60)
                elif item_type == 'BOOLEAN':
                    val = random.choice([True, False])
                    response_val = str(val).lower()
                    score = 100 if val else 0
                else:
                    response_val = str(random.randint(60, 100))
                    score = int(response_val)

                detail = api('post', f'/v7/insp/submissions/{sub_id}/details', {
                    'templateItemId': int(item['id']),
                    'itemCode': item.get('itemCode', ''),
                    'itemName': item.get('itemName', ''),
                    'responseValue': response_val,
                    'scoringMode': 'DIRECT',
                    'score': score,
                    'maxScore': 100
                })

            # Complete submission
            completed = api('post', f'/v7/insp/submissions/{sub_id}/complete')
            if completed:
                print(f'    Submission {sub_idx+1} (ID={sub_id}) completed')
            else:
                print(f'    WARN: Failed to complete submission {sub_id}')

        # Submit the task
        submitted = api('post', f'/v7/insp/tasks/{task_id}/submit')
        if submitted:
            print(f'    Task submitted: status={submitted.get("status")}')
        else:
            print(f'    WARN: Failed to submit task')

def main():
    token = login()

    # 1. Grade Scheme
    grade_scheme = step1_grade_scheme()

    # 2. Template with 4 layers
    root, l1, l2, l3 = step2_template()
    root_id = root['id']

    # 3. Items
    items = step3_items(l3)

    # 4. Publish template
    version = step4_publish(root_id)
    version_id = version.get('id') if version else None

    # 5. Create project
    project = step5_project(root_id)
    project_id = project['id']

    # 6. Configure project
    project, target_ids = step6_configure_project(project_id, root_id, grade_scheme['id'])

    # 7. Publish project
    step7_publish_project(project_id, version_id)

    # 8. Create plan
    plan = step8_create_plan(project_id, root_id)

    # 9. Create tasks
    tasks = step9_create_task(project_id)

    # 10. Claim, score, submit
    step10_claim_and_score(tasks, target_ids, l3, items)

    # Save all IDs
    result = {
        'grade_scheme_id': str(grade_scheme['id']),
        'root_id': str(root_id),
        'project_id': str(project_id),
        'plan_id': str(plan['id']),
        'task_ids': [str(t['id']) for t in tasks],
        'target_ids': target_ids,
        'l1_ids': [str(s['id']) for s in l1],
        'l2_ids': [str(s['id']) for s in l2],
        'l3_ids': [str(s['id']) for s in l3],
        'item_count': len(items)
    }
    with open('/tmp/e2e_ids.json', 'w') as f:
        json.dump(result, f, ensure_ascii=False, indent=2)

    print(f'\n{"="*60}')
    print(f'E2E TEST DATA CREATION COMPLETE')
    print(f'  Grade Scheme: {grade_scheme["id"]}')
    print(f'  Template Root: {root_id}')
    print(f'  Project: {project_id}')
    print(f'  Tasks: {len(tasks)}')
    print(f'  Items: {len(items)}')
    print(f'{"="*60}')
    print(json.dumps(result, ensure_ascii=False, indent=2))

if __name__ == '__main__':
    main()
