#!/usr/bin/env python3
"""Assign classrooms and dorms via direct SQL."""
import os
os.environ['no_proxy'] = 'localhost,127.0.0.1'
import urllib.request, json, sys, io, subprocess

sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8')

def sql(query, fetch=True):
    result = subprocess.run(
        ['mysql', '-u', 'root', '-p123456', 'student_management', '-N', '-e', query],
        capture_output=True, text=True
    )
    if result.returncode != 0:
        print(f'  SQL Error: {result.stderr[:200]}')
        return []
    if not fetch:
        return []
    rows = []
    for line in result.stdout.strip().split('\n'):
        if line:
            rows.append(line.split('\t'))
    return rows

def sql_exec(query):
    result = subprocess.run(
        ['mysql', '-u', 'root', '-p123456', 'student_management', '-e', query],
        capture_output=True, text=True
    )
    if result.returncode != 0:
        print(f'  SQL Error: {result.stderr[:200]}')
        return False
    return True

# Get classes
classes = sql("SELECT id, unit_name FROM org_units WHERE unit_type='CLASS' AND type_code='CLASS' AND deleted=0 ORDER BY id")
print(f'Classes: {len(classes)}')

# Get classrooms
classrooms = sql("SELECT id, place_name FROM places WHERE type_code='TYPE_CLASSROOM' AND deleted=0 ORDER BY id")
print(f'Classrooms: {len(classrooms)}')

# ==========================================
# 1. Assign classrooms to classes via place_class_assignment
# ==========================================
print('\n=== CLASSROOM ASSIGNMENTS ===')
values = []
for i, cls in enumerate(classes):
    if i >= len(classrooms):
        break
    cid, cname = cls
    rid, rname = classrooms[i]
    values.append(f"({rid}, {cid}, 1, 1)")
    print(f'  {cname} -> {rname}')

if values:
    insert_sql = "INSERT INTO place_class_assignment (place_id, org_unit_id, status, tenant_id) VALUES\n"
    insert_sql += ",\n".join(values) + ";"
    sql_exec(insert_sql)
    print(f'Assigned {len(values)} classrooms')

# ==========================================
# 2. Assign students to dorm rooms
# ==========================================
print('\n=== DORM ASSIGNMENTS ===')

# Get students with gender
students = sql("""
    SELECT s.id, s.student_no, u.gender, u.real_name, s.org_unit_id
    FROM students s JOIN users u ON s.user_id = u.id
    WHERE s.deleted = 0 ORDER BY s.org_unit_id, s.id
""")
male_students = [s for s in students if s[2] == '1']
female_students = [s for s in students if s[2] == '2']
print(f'Male students: {len(male_students)}, Female: {len(female_students)}')

# Get dorm rooms
male_dorms = sql("SELECT id, place_name FROM places WHERE type_code='DORM_ROOM' AND gender='1' AND deleted=0 ORDER BY id")
female_dorms = sql("SELECT id, place_name FROM places WHERE type_code='DORM_ROOM' AND gender='2' AND deleted=0 ORDER BY id")
print(f'Male dorm rooms: {len(male_dorms)}, Female: {len(female_dorms)}')

def assign_dorms(students_list, dorms, label):
    room_idx = 0
    bed_no = 1
    occupant_values = []
    student_updates = []

    for s in students_list:
        if room_idx >= len(dorms):
            print(f'  WARNING: out of {label} dorm rooms!')
            break
        sid, sno, gender, name, org_unit_id = s
        room_id = dorms[room_idx][0]

        occupant_values.append(
            f"({room_id}, 'STUDENT', {sid}, '{name}', NULL, NULL, {gender}, '{bed_no}', NOW(), NULL, 1, NULL, NULL, 0, 1)"
        )
        student_updates.append(
            f"UPDATE students SET dormitory_id = {room_id}, bed_number = '{bed_no}' WHERE id = {sid};"
        )

        bed_no += 1
        if bed_no > 6:
            bed_no = 1
            room_idx += 1

    # Insert occupants
    batch_size = 100
    total_inserted = 0
    for i in range(0, len(occupant_values), batch_size):
        batch = occupant_values[i:i+batch_size]
        insert_sql = "INSERT INTO place_occupants (place_id, occupant_type, occupant_id, occupant_name, username, org_unit_name, gender, position_no, check_in_time, check_out_time, status, remark, created_by, deleted, tenant_id) VALUES\n"
        insert_sql += ",\n".join(batch) + ";"
        if sql_exec(insert_sql):
            total_inserted += len(batch)

    # Update students
    for i in range(0, len(student_updates), batch_size):
        batch = student_updates[i:i+batch_size]
        sql_exec("\n".join(batch))

    rooms_used = (len(students_list) + 5) // 6
    print(f'  {label}: {total_inserted} students -> {rooms_used} rooms')

assign_dorms(male_students, male_dorms, 'Male')
assign_dorms(female_students, female_dorms, 'Female')

# Update room current_occupancy
sql_exec("""
    UPDATE places p SET current_occupancy = (
        SELECT COUNT(*) FROM place_occupants r
        WHERE r.place_id = p.id AND r.status = 1 AND r.deleted = 0
    ) WHERE p.type_code = 'DORM_ROOM' AND p.deleted = 0;
""")

print('\n=== SUMMARY ===')
print(f'Classrooms assigned: {min(len(classes), len(classrooms))}')
print(f'Students assigned to dorms: {len(male_students) + len(female_students)}')
