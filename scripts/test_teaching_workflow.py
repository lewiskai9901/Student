#!/usr/bin/env python3
"""Test full teaching workflow: 校历→开课→排课→考试→成绩"""
import os
os.environ['no_proxy'] = 'localhost,127.0.0.1'
import urllib.request, json, sys, io, random

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

def put(path, data):
    body = json.dumps(data).encode('utf-8')
    req = urllib.request.Request(f'{base}{path}', body, h, method='PUT')
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

# Academic year & semester
years = get('/calendar/academic-years').get('data', [])
year = years[0]
year_id = int(year['id'])
print(f"Academic Year: {year['yearName']} (id={year_id})")

semesters = get('/calendar/semesters').get('data', [])
semester = semesters[0]
semester_id = int(semester['id'])
print(f"Semester: {semester['semesterName']} (id={semester_id})")

# Courses
courses_data = get('/academic/courses?pageNum=1&pageSize=10').get('data', {}).get('records', [])
courses = {c['courseCode']: c for c in courses_data}
print(f"Courses: {len(courses)}")
for code, c in courses.items():
    print(f"  {code}: {c['courseName']} (id={c['id']})")

# Classes (first 6 only for testing)
tree = get('/org-units/tree').get('data', [])
def find_all(nodes, tc):
    r = []
    for n in nodes:
        if n.get('unitType') == tc: r.append(n)
        r.extend(find_all(n.get('children', []), tc))
    return r

all_classes = find_all(tree, 'CLASS')
test_classes = all_classes[:6]  # Just test with first 6 classes
print(f"Test classes: {len(test_classes)}")
for c in test_classes:
    print(f"  {c['unitName']} (id={c['id']})")

# Teachers
users = get('/users?pageNum=1&pageSize=50').get('data', [])
teachers = sorted([u for u in users if u.get('username', '').startswith('teacher')], key=lambda x: x['username'])[:6]
print(f"Teachers for testing: {len(teachers)}")

# Classrooms
import subprocess
classroom_rows = subprocess.run(
    ['mysql', '-u', 'root', '-p123456', 'student_management', '-N', '-e',
     "SELECT id, place_name FROM places WHERE type_code='TYPE_CLASSROOM' AND deleted=0 ORDER BY id LIMIT 6"],
    capture_output=True, text=True
).stdout.strip().split('\n')
classrooms = [r.split('\t') for r in classroom_rows if r]
print(f"Classrooms: {len(classrooms)}")

# ==========================================
# 1. Generate teaching weeks
# ==========================================
print("\n" + "=" * 60)
print("STEP 1: Generate teaching weeks for semester")
print("=" * 60)

r = post(f'/calendar/semesters/{semester_id}/generate-weeks', {})
print(f"Generate weeks: {r.get('message', '')[:60]}")

weeks = get(f'/calendar/semesters/{semester_id}/weeks').get('data', [])
print(f"Teaching weeks: {len(weeks)}")

# ==========================================
# 2. Create course offerings (开课)
# ==========================================
print("\n" + "=" * 60)
print("STEP 2: Create course offerings (开课)")
print("=" * 60)

# Offer 3 courses to our test classes
test_courses = [
    ('MATH101', '高等数学', 4),
    ('ENG101', '大学英语', 2),
    ('CS101', '计算机基础', 2),
]

offering_ids = []
for code, name, weekly_hours in test_courses:
    course = courses.get(code)
    if not course:
        print(f"  SKIP {code}: course not found")
        continue

    r = post('/teaching/offerings', {
        'semesterId': semester_id,
        'courseId': int(course['id']),
        'weeklyHours': weekly_hours,
        'startWeek': 1,
        'endWeek': 16,
        'courseCategory': 1,
        'courseType': 1,
    })
    oid = r.get('data', {}).get('id') if isinstance(r.get('data'), dict) else r.get('data')
    msg = r.get('message', '')[:50]
    print(f"  {code} ({name}): {oid or 'FAILED'} ({msg})")
    if oid:
        offering_ids.append(int(oid))

# ==========================================
# 3. Create schedule entries (排课)
# ==========================================
print("\n" + "=" * 60)
print("STEP 3: Create schedule entries (排课)")
print("=" * 60)

schedule_ids = []
weekday = 1  # Start from Monday
slot = 1     # Start from first slot

for ci, cls in enumerate(test_classes):
    for cj, (code, name, _) in enumerate(test_courses):
        course = courses.get(code)
        if not course:
            continue
        teacher = teachers[ci % len(teachers)]
        classroom = classrooms[ci % len(classrooms)]

        day = ((ci * 3 + cj) // 4) + 1  # Spread across weekdays
        start_slot = ((ci * 3 + cj) % 4) * 2 + 1  # 2-slot classes

        r = post('/teaching/schedules', {
            'semesterId': semester_id,
            'courseId': int(course['id']),
            'orgUnitId': int(cls['id']),
            'teacherId': int(teacher['id']),
            'classroomId': int(classroom[0]),
            'weekday': min(day, 5),
            'startSlot': start_slot,
            'endSlot': start_slot + 1,
            'startWeek': 1,
            'endWeek': 16,
            'weekType': 'EVERY',
        })
        sid = r.get('data', {}).get('id') if isinstance(r.get('data'), dict) else r.get('data')
        msg = r.get('message', '')[:50]
        if sid:
            schedule_ids.append(int(sid))
        status = 'OK' if sid else 'FAIL'
        print(f"  {cls['unitName']} + {code}: {status} ({msg})")

print(f"Scheduled: {len(schedule_ids)} entries")

# ==========================================
# 4. Create exam batch and arrangements (考试)
# ==========================================
print("\n" + "=" * 60)
print("STEP 4: Create exam batch and arrangements (考试)")
print("=" * 60)

r = post('/teaching/examinations/batches', {
    'batchName': '2025-2026第二学期期中考试',
    'semesterId': semester_id,
    'examType': 1,
    'startDate': '2026-04-20',
    'endDate': '2026-04-24',
    'status': 0,
    'remark': '期中考试'
})
batch_id = r.get('data', {}).get('id') if isinstance(r.get('data'), dict) else r.get('data')
print(f"Exam batch: {batch_id} ({r.get('message', '')[:50]})")

if batch_id:
    batch_id = int(batch_id)

    # Create arrangements for each course
    for cj, (code, name, _) in enumerate(test_courses):
        course = courses.get(code)
        if not course:
            continue

        exam_date = f'2026-04-{20 + cj}'
        r = post(f'/teaching/examinations/batches/{batch_id}/arrangements', {
            'courseId': int(course['id']),
            'examDate': exam_date,
            'startTime': '09:00',
            'endTime': '11:00',
            'duration': 120,
            'examForm': 1,
            'totalStudents': 180,
            'remark': f'{name} 期中考试'
        })
        arr_id = r.get('data', {}).get('id') if isinstance(r.get('data'), dict) else r.get('data')
        print(f"  {name} exam: {arr_id or 'FAILED'} ({r.get('message', '')[:40]})")

    # Publish the batch
    r = post(f'/teaching/examinations/batches/{batch_id}/publish', {})
    print(f"Publish exam batch: {r.get('message', '')[:50]}")

# ==========================================
# 5. Create grade batch and record grades (成绩)
# ==========================================
print("\n" + "=" * 60)
print("STEP 5: Create grade batch and record grades (成绩)")
print("=" * 60)

r = post('/teaching/grades/batches', {
    'batchName': '2025-2026第二学期期中成绩',
    'semesterId': semester_id,
    'gradeType': 1,
    'startTime': '2026-04-25T08:00:00',
    'endTime': '2026-05-01T23:59:59',
    'status': 0,
})
grade_batch_id = r.get('data', {}).get('id') if isinstance(r.get('data'), dict) else r.get('data')
print(f"Grade batch: {grade_batch_id} ({r.get('message', '')[:50]})")

if grade_batch_id:
    grade_batch_id = int(grade_batch_id)

    # Get students from first test class
    students = get(f'/students?orgUnitId={test_classes[0]["id"]}&pageNum=1&pageSize=50')
    student_records = students.get('data', {}).get('records', [])
    print(f"Students in {test_classes[0]['unitName']}: {len(student_records)}")

    # Record grades for first course, first class
    course = courses.get('MATH101')
    if course and student_records:
        random.seed(100)
        grades_payload = []
        for s in student_records[:10]:  # Grade first 10 students
            score = round(random.uniform(55, 100), 1)
            gp = round(min((score - 50) / 10, 5.0), 1) if score >= 60 else 0.0
            grades_payload.append({
                'studentId': int(s['id']),
                'courseId': int(course['id']),
                'score': score,
                'gradePoint': gp,
            })

        r = post(f'/teaching/grades/batches/{grade_batch_id}/batch-record', {
            'grades': grades_payload
        })
        print(f"Batch record grades ({len(grades_payload)} students): {r.get('message', '')[:50]}")

        # Submit and publish
        r = post(f'/teaching/grades/batches/{grade_batch_id}/submit', {})
        print(f"Submit grade batch: {r.get('message', '')[:50]}")

        r = post(f'/teaching/grades/batches/{grade_batch_id}/publish', {})
        print(f"Publish grade batch: {r.get('message', '')[:50]}")

# ==========================================
# Summary
# ==========================================
print("\n" + "=" * 60)
print("WORKFLOW SUMMARY")
print("=" * 60)
print(f"Academic Year: {year['yearName']}")
print(f"Semester: {semester['semesterName']}")
print(f"Teaching weeks: {len(weeks)}")
print(f"Course offerings: {len(offering_ids)}")
print(f"Schedule entries: {len(schedule_ids)}")
print(f"Exam batch: {'Created' if batch_id else 'Failed'}")
print(f"Grade batch: {'Created' if grade_batch_id else 'Failed'}")
print("=" * 60)
