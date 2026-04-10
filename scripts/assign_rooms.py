#!/usr/bin/env python3
"""Assign classrooms to classes and students to dorm rooms."""
import os
os.environ['no_proxy'] = 'localhost,127.0.0.1'
import urllib.request, json, sys, io, subprocess, random

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
    resp = urllib.request.urlopen(req)
    return json.loads(resp.read())

def put(path, data=None):
    body = json.dumps(data).encode('utf-8') if data else b''
    req = urllib.request.Request(f'{base}{path}', body, h, method='PUT')
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
# Get all data
# ==========================================

# Classes from org tree
tree = get('/org-units/tree').get('data', [])

def find_all(nodes, tc):
    r = []
    for n in nodes:
        if n.get('unitType') == tc:
            r.append(n)
        r.extend(find_all(n.get('children', []), tc))
    return r

classes = find_all(tree, 'CLASS')
print(f'Classes: {len(classes)}')

# Get classrooms and dorm rooms from DB directly
def sql(query):
    result = subprocess.run(
        ['mysql', '-u', 'root', '-p123456', 'student_management', '-N', '-e', query],
        capture_output=True, text=True
    )
    rows = []
    for line in result.stdout.strip().split('\n'):
        if line:
            rows.append(line.split('\t'))
    return rows

classrooms = sql("SELECT id, place_name FROM places WHERE type_code = 'TYPE_CLASSROOM' AND deleted = 0 ORDER BY id")
male_dorms = sql("SELECT id, place_name FROM places WHERE type_code = 'DORM_ROOM' AND gender = '1' AND deleted = 0 ORDER BY id")
female_dorms = sql("SELECT id, place_name FROM places WHERE type_code = 'DORM_ROOM' AND gender = '2' AND deleted = 0 ORDER BY id")

print(f'Classrooms: {len(classrooms)}')
print(f'Male dorm rooms: {len(male_dorms)}')
print(f'Female dorm rooms: {len(female_dorms)}')

# ==========================================
# 1. Assign classrooms to classes via place_class_assignment table
# ==========================================
print('\n=== CLASSROOM ASSIGNMENTS ===')

# Direct SQL insert into place_class_assignment
for i, cls in enumerate(classes):
    if i >= len(classrooms):
        print(f'  No more classrooms for {cls["unitName"]}')
        break
    cid = cls['id']
    room_id = classrooms[i][0]
    room_name = classrooms[i][1]

    # Also try the API
    r = put(f'/places/{room_id}/class?orgUnitId={cid}')
    print(f'  {cls["unitName"]} -> {room_name}: {r.get("message", "")[:40]}')

# ==========================================
# 2. Assign students to dorm rooms
# ==========================================
print('\n=== DORM ASSIGNMENTS ===')

# Get students with gender info from DB
students_data = sql("""
    SELECT s.id, s.student_no, s.org_unit_id, u.gender, u.real_name
    FROM students s
    JOIN users u ON s.user_id = u.id
    WHERE s.deleted = 0
    ORDER BY s.org_unit_id, s.id
""")

male_students = [s for s in students_data if s[3] == '1']
female_students = [s for s in students_data if s[3] == '2']
print(f'Male students: {len(male_students)}')
print(f'Female students: {len(female_students)}')

# Assign male students to male dorms (6 per room)
def assign_students_to_dorms(students, dorms, label):
    room_idx = 0
    bed_no = 1
    assigned = 0
    sql_parts = []

    for s in students:
        if room_idx >= len(dorms):
            print(f'  WARNING: ran out of {label} dorm rooms!')
            break

        room_id = dorms[room_idx][0]
        # Insert via occupant_records table
        sql_parts.append(f"({room_id}, 'STUDENT', {s[0]}, {bed_no}, 'ACTIVE')")
        assigned += 1
        bed_no += 1
        if bed_no > 6:
            bed_no = 1
            room_idx += 1

    # Also update students.dormitory_id and bed_number
    room_idx = 0
    bed_no = 1
    update_parts = []
    for s in students:
        if room_idx >= len(dorms):
            break
        room_id = dorms[room_idx][0]
        update_parts.append(f"UPDATE students SET dormitory_id = {room_id}, bed_number = '{bed_no}' WHERE id = {s[0]};")
        bed_no += 1
        if bed_no > 6:
            bed_no = 1
            room_idx += 1

    # Execute occupant records insert
    if sql_parts:
        insert_sql = "INSERT INTO occupant_records (place_id, occupant_type, occupant_id, position_no, status) VALUES\n"
        # Do in batches
        batch_size = 100
        for i in range(0, len(sql_parts), batch_size):
            batch = sql_parts[i:i+batch_size]
            full_sql = insert_sql + ",\n".join(batch) + ";"
            result = subprocess.run(
                ['mysql', '-u', 'root', '-p123456', 'student_management', '-e', full_sql],
                capture_output=True, text=True
            )
            if result.returncode != 0:
                print(f'  SQL Error: {result.stderr[:200]}')

    # Execute student updates
    if update_parts:
        batch_size = 100
        for i in range(0, len(update_parts), batch_size):
            batch = update_parts[i:i+batch_size]
            full_sql = "\n".join(batch)
            result = subprocess.run(
                ['mysql', '-u', 'root', '-p123456', 'student_management', '-e', full_sql],
                capture_output=True, text=True
            )
            if result.returncode != 0:
                print(f'  SQL Error: {result.stderr[:200]}')

    rooms_used = (assigned + 5) // 6
    print(f'  {label}: {assigned} students -> {rooms_used} rooms')

assign_students_to_dorms(male_students, male_dorms, 'Male')
assign_students_to_dorms(female_students, female_dorms, 'Female')

# Update room current_occupancy
subprocess.run(
    ['mysql', '-u', 'root', '-p123456', 'student_management', '-e',
     "UPDATE places p SET current_occupancy = (SELECT COUNT(*) FROM occupant_records r WHERE r.place_id = p.id AND r.status = 'ACTIVE') WHERE p.type_code = 'DORM_ROOM' AND p.deleted = 0;"],
    capture_output=True, text=True
)

print('\n=== DONE ===')
print(f'Classrooms assigned: {min(len(classes), len(classrooms))}')
print(f'Students in dorms: {len(male_students) + len(female_students)}')
