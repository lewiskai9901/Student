#!/usr/bin/env python3
"""Test full teaching workflow: 校历→开课→教学任务→排课→考试→成绩"""
import os
os.environ['no_proxy'] = 'localhost,127.0.0.1'
import urllib.request, json, sys, io, random, subprocess

sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8')

base = 'http://localhost:8080/api'

def login():
    data = json.dumps({'username': 'admin', 'password': 'admin123'}).encode()
    req = urllib.request.Request(f'{base}/auth/login', data, {'Content-Type': 'application/json'}, method='POST')
    resp = urllib.request.urlopen(req)
    return json.loads(resp.read())['data']['accessToken']

token = login()
h = {'Authorization': f'Bearer {token}', 'Content-Type': 'application/json; charset=utf-8'}

def get(path):
    req = urllib.request.Request(f'{base}{path}', None, h)
    try:
        resp = urllib.request.urlopen(req)
        return json.loads(resp.read())
    except urllib.error.HTTPError as e:
        return json.loads(e.read().decode())

def post(path, data):
    body = json.dumps(data).encode('utf-8')
    req = urllib.request.Request(f'{base}{path}', body, h, method='POST')
    try:
        resp = urllib.request.urlopen(req)
        return json.loads(resp.read())
    except urllib.error.HTTPError as e:
        return json.loads(e.read().decode())

# ==========================================
# 0. Get existing data
# ==========================================
print("=" * 60)
print("STEP 0: Getting existing data")
print("=" * 60)

year_id = 2040474620729016321
semester_id = 2041362399822385154
print(f"Academic Year: 2025-2026学年 (id={year_id})")
print(f"Semester: 2025-2026-S2 (id={semester_id})")

# Courses
courses_data = get('/academic/courses?pageNum=1&pageSize=10').get('data', {}).get('records', [])
courses = {c['courseCode']: c for c in courses_data}

test_courses = [
    ('MATH101', '高等数学', 4, 64),
    ('ENG101', '大学英语', 2, 32),
    ('CS101', '计算机基础', 2, 32),
]
print(f"Test courses: {[c[0] for c in test_courses]}")

# Classes (first 6)
tree = get('/org-units/tree').get('data', [])
def find_all(nodes, tc):
    r = []
    for n in nodes:
        if n.get('unitType') == tc: r.append(n)
        r.extend(find_all(n.get('children', []), tc))
    return r

test_classes = find_all(tree, 'CLASS')[:6]
print(f"Test classes: {[c['unitName'] for c in test_classes]}")

# Teachers
users = get('/users?pageNum=1&pageSize=50').get('data', [])
teachers = sorted([u for u in users if u.get('username', '').startswith('teacher')], key=lambda x: x['username'])[:6]
print(f"Teachers: {[t['realName'] for t in teachers]}")

# Classrooms
cr = subprocess.run(
    ['mysql', '-u', 'root', '-p123456', 'student_management', '-N', '-e',
     "SELECT id, place_name FROM places WHERE type_code='TYPE_CLASSROOM' AND deleted=0 ORDER BY id LIMIT 6"],
    capture_output=True, text=True
).stdout.strip().split('\n')
classrooms = [r.split('\t') for r in cr if r]

# ==========================================
# 1. Teaching weeks already generated
# ==========================================
weeks = get(f'/calendar/semesters/{semester_id}/weeks').get('data', [])
print(f"\nTeaching weeks: {len(weeks)}")

# ==========================================
# 2. Create teaching tasks (教学任务)
# ==========================================
print("\n" + "=" * 60)
print("STEP 2: Create teaching tasks (教学任务)")
print("=" * 60)

task_map = {}  # (class_id, course_code) -> task_id
for cls in test_classes:
    for code, name, wh, th in test_courses:
        course = courses.get(code)
        if not course:
            continue
        key = (int(cls['id']), code)
        r = post('/teaching/tasks', {
            'semesterId': semester_id,
            'courseId': int(course['id']),
            'orgUnitId': int(cls['id']),
            'weeklyHours': wh,
            'totalHours': th,
            'startWeek': 1,
            'endWeek': 16,
        })
        tid = r.get('data', {}).get('id') if isinstance(r.get('data'), dict) else r.get('data')
        if tid:
            task_map[key] = int(tid)
            print(f"  OK: {cls['unitName']} + {code} -> task {tid}")
        else:
            print(f"  FAIL: {cls['unitName']} + {code}: {r.get('message', '')[:50]}")

print(f"Tasks created: {len(task_map)}")

# ==========================================
# 3. Create schedule entries (排课)
# ==========================================
print("\n" + "=" * 60)
print("STEP 3: Create schedule entries (排课)")
print("=" * 60)

schedule_ids = []
for ci, cls in enumerate(test_classes):
    for cj, (code, name, wh, th) in enumerate(test_courses):
        course = courses.get(code)
        if not course:
            continue
        key = (int(cls['id']), code)
        task_id = task_map.get(key)
        if not task_id:
            continue

        teacher = teachers[ci % len(teachers)]
        classroom = classrooms[(ci * 3 + cj) % len(classrooms)]

        day = (ci * 3 + cj) % 5 + 1  # Mon-Fri
        start_slot = ((ci * 3 + cj) // 5) * 2 + 1

        r = post('/teaching/schedules', {
            'semesterId': semester_id,
            'taskId': task_id,
            'courseId': int(course['id']),
            'orgUnitId': int(cls['id']),
            'teacherId': int(teacher['id']),
            'classroomId': int(classroom[0]),
            'weekday': day,
            'startSlot': start_slot,
            'endSlot': start_slot + 1,
            'startWeek': 1,
            'endWeek': 16,
            'weekType': 0,
        })
        sid = r.get('data', {}).get('id') if isinstance(r.get('data'), dict) else r.get('data')
        msg = r.get('message', '')[:40]
        if sid:
            schedule_ids.append(int(sid))
            status = 'OK'
        else:
            status = 'FAIL'
        print(f"  {cls['unitName']}+{code} Day{day} Slot{start_slot}: {status} ({msg})")

print(f"Schedule entries: {len(schedule_ids)}")

# ==========================================
# 4. Create exam batch and arrangements (考试)
# ==========================================
print("\n" + "=" * 60)
print("STEP 4: Create exam batch (考试)")
print("=" * 60)

r = post('/teaching/examinations/batches', {
    'batchName': '2025-2026-S2期中考试',
    'semesterId': semester_id,
    'examType': 1,
    'startDate': '2026-04-20',
    'endDate': '2026-04-24',
    'status': 0,
})
batch_id = r.get('data', {}).get('id') if isinstance(r.get('data'), dict) else r.get('data')
print(f"Exam batch: {batch_id} ({r.get('message', '')[:40]})")

arr_ids = []
if batch_id:
    batch_id = int(batch_id)
    for cj, (code, name, _, _) in enumerate(test_courses):
        course = courses.get(code)
        if not course:
            continue
        r = post(f'/teaching/examinations/batches/{batch_id}/arrangements', {
            'courseId': int(course['id']),
            'examDate': f'2026-04-{20+cj}',
            'startTime': '09:00',
            'endTime': '11:00',
            'duration': 120,
            'examForm': 1,
            'totalStudents': 200,
        })
        aid = r.get('data', {}).get('id') if isinstance(r.get('data'), dict) else r.get('data')
        if aid:
            arr_ids.append(aid)
        print(f"  {name}: {aid or 'FAIL'} ({r.get('message', '')[:40]})")

    r = post(f'/teaching/examinations/batches/{batch_id}/publish', {})
    print(f"Publish: {r.get('message', '')[:40]}")

# ==========================================
# 5. Grade recording (成绩)
# ==========================================
print("\n" + "=" * 60)
print("STEP 5: Create grade batch and record grades (成绩)")
print("=" * 60)

r = post('/teaching/grades/batches', {
    'batchName': '2025-2026-S2期中成绩',
    'semesterId': semester_id,
    'gradeType': 1,
    'startTime': '2026-04-25T08:00:00',
    'endTime': '2026-05-01T23:59:59',
    'status': 0,
})
gb_id = r.get('data', {}).get('id') if isinstance(r.get('data'), dict) else r.get('data')
print(f"Grade batch: {gb_id} ({r.get('message', '')[:40]})")

if gb_id:
    gb_id = int(gb_id)

    # Get students from first class
    cls0 = test_classes[0]
    students = get(f'/students?orgUnitId={cls0["id"]}&pageNum=1&pageSize=50')
    recs = students.get('data', {}).get('records', [])
    print(f"Students in {cls0['unitName']}: {len(recs)}")

    # Record grades for MATH101
    course = courses['MATH101']
    random.seed(42)

    # Try single record first
    if recs:
        s = recs[0]
        score = round(random.uniform(60, 100), 1)
        r = post(f'/teaching/grades/batches/{gb_id}/grades', {
            'studentId': int(s['id']),
            'courseId': int(course['id']),
            'score': score,
            'gradePoint': round(min((score - 50) / 10, 5.0), 1),
        })
        print(f"  Single grade: {r.get('message', '')[:50]}")

        # If single works, batch more
        if r.get('code') == 200:
            grades = []
            for s in recs[1:15]:
                sc = round(random.uniform(50, 100), 1)
                gp = round(min((sc - 50) / 10, 5.0), 1) if sc >= 60 else 0.0
                grades.append({
                    'studentId': int(s['id']),
                    'courseId': int(course['id']),
                    'score': sc,
                    'gradePoint': gp,
                })
            r = post(f'/teaching/grades/batches/{gb_id}/batch-record', {'grades': grades})
            print(f"  Batch grades ({len(grades)}): {r.get('message', '')[:50]}")

    # Submit & publish
    r = post(f'/teaching/grades/batches/{gb_id}/submit', {})
    print(f"  Submit: {r.get('message', '')[:50]}")
    r = post(f'/teaching/grades/batches/{gb_id}/publish', {})
    print(f"  Publish: {r.get('message', '')[:50]}")

# ==========================================
# Summary
# ==========================================
print("\n" + "=" * 60)
print("WORKFLOW COMPLETE")
print("=" * 60)
print(f"Teaching tasks: {len(task_map)}")
print(f"Schedule entries: {len(schedule_ids)}")
print(f"Exam arrangements: {len(arr_ids)}")
print(f"Grade batch: {'OK' if gb_id else 'FAIL'}")
