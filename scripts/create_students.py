#!/usr/bin/env python3
"""Create student users and student records for all 18 classes."""
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

def post(path, data):
    body = json.dumps(data).encode('utf-8')
    req = urllib.request.Request(f'{base}{path}', body, h, method='POST')
    try:
        resp = urllib.request.urlopen(req)
        return json.loads(resp.read())
    except urllib.error.HTTPError as e:
        return json.loads(e.read().decode())

# Get classes from org tree
tree = get('/org-units/tree').get('data', [])

def find_all(nodes, tc):
    r = []
    for n in nodes:
        if n.get('unitType') == tc:
            r.append(n)
        r.extend(find_all(n.get('children', []), tc))
    return r

classes = find_all(tree, 'CLASS')
print(f'Found {len(classes)} classes')

# Build class -> grade mapping
class_grade_map = {}
for dept in find_all(tree, 'DEPARTMENT'):
    for g in dept.get('children', []):
        if g.get('unitType') == 'GRADE':
            for c in g.get('children', []):
                if c.get('unitType') == 'CLASS':
                    class_grade_map[str(c['id'])] = int(g['id'])

# Name pools
surnames = ['王','李','张','刘','陈','杨','赵','黄','周','吴','徐','孙','胡','朱','高',
            '林','何','郭','马','罗','梁','宋','郑','谢','韩','唐','冯','于','董','萧',
            '程','曹','袁','邓','许','傅','沈','曾','彭','吕','苏','卢','蒋','蔡','贾',
            '丁','魏','薛','叶','阎','余','潘','杜','戴','夏','钟','汪','田','任','姜',
            '范','方','石','姚','谭','廖','邹','熊','金','陆','郝','孔','白','崔','康',
            '毛','邱','秦','江','史','顾','侯','邵','孟','龙','万','段','雷','钱']
male_names = ['伟','强','磊','军','勇','杰','涛','明','超','刚','辉','鹏','飞','鑫','波',
              '斌','亮','建','文','博','浩','宇','翔','昊','睿','旭','天','龙','志','毅',
              '成','凯','俊','健','坤','威','彬','峰','林','武','岩','松','栋','庆','豪']
female_names = ['芳','丽','静','敏','燕','艳','红','梅','玲','娟','莉','萍','雪','婷','慧',
                '琳','颖','欣','佳','倩','琪','薇','蕾','晨','瑶','璐','怡','悦','梦','诗',
                '蓉','菲','洁','馨','妍','淑','珍','秀','兰','月','霞','彤','嘉','柳','蝶']

random.seed(42)
STUDENT_ROLE_ID = 2021993207935557634

# Generate student data
all_students = []
total = 0
for ci, cls in enumerate(classes):
    cid = int(cls['id'])
    cname = cls['unitName']
    year = 2025 if '2025' in cname else 2024
    grade_id = class_grade_map.get(str(cid))

    num_students = random.randint(25, 40)
    for si in range(num_students):
        total += 1
        # Art dept has more females
        if '艺术' in cname:
            gender = random.choice([1, 2, 2, 2, 2])
        elif '康养' in cname:
            gender = random.choice([1, 2, 2, 2])
        else:
            gender = random.choice([1, 1, 1, 2, 2])

        surname = random.choice(surnames)
        given = random.choice(male_names if gender == 1 else female_names)
        if random.random() < 0.3:
            given += random.choice(male_names[:15] if gender == 1 else female_names[:15])
        name = surname + given

        sno = f'S{year}{ci+1:02d}{si+1:03d}'
        username = f'stu{total:04d}'
        phone = f'139{random.randint(10000000, 99999999):08d}'

        all_students.append({
            'username': username,
            'realName': name,
            'gender': gender,
            'phone': phone,
            'orgUnitId': cid,
            'sno': sno,
            'gradeId': grade_id,
            'year': year,
        })

print(f'Total students to create: {total}')

# Step 1: Create user accounts via API
created_users = 0
failed_users = 0
for i, s in enumerate(all_students):
    r = post('/users', {
        'username': s['username'],
        'password': 'Student123!',
        'realName': s['realName'],
        'phone': s['phone'],
        'gender': s['gender'],
        'orgUnitId': s['orgUnitId'],
        'userTypeCode': 'STUDENT',
        'roleIds': [STUDENT_ROLE_ID]
    })
    uid = None
    if isinstance(r.get('data'), dict):
        uid = r['data'].get('id')
    elif r.get('data'):
        uid = r['data']

    if uid:
        s['userId'] = int(uid)
        created_users += 1
    else:
        failed_users += 1
        if (failed_users <= 5):
            print(f'  FAIL {s["username"]}: {r.get("message", "")[:60]}')

    if (i + 1) % 50 == 0:
        print(f'  Progress: {i+1}/{total} users (ok={created_users}, fail={failed_users})')

print(f'\nUsers created: {created_users}, failed: {failed_users}')

# Step 2: Insert student records via SQL
valid = [s for s in all_students if 'userId' in s]
print(f'Inserting {len(valid)} student records via SQL...')

sql_values = []
for s in valid:
    gid = s['gradeId'] if s['gradeId'] else 'NULL'
    sql_values.append(
        f"({s['userId']}, '{s['sno']}', {s['userId']}, {s['orgUnitId']}, {gid}, 1, 0, 1)"
    )

# Insert in batches
batch_size = 100
inserted = 0
for i in range(0, len(sql_values), batch_size):
    batch = sql_values[i:i+batch_size]
    sql = "INSERT INTO students (id, student_no, user_id, org_unit_id, grade_id, student_status, deleted, tenant_id) VALUES\n"
    sql += ",\n".join(batch) + ";"
    result = subprocess.run(
        ['mysql', '-u', 'root', '-p123456', 'student_management', '-e', sql],
        capture_output=True, text=True
    )
    if result.returncode == 0:
        inserted += len(batch)
    else:
        print(f'  SQL Error batch {i//batch_size}: {result.stderr[:200]}')

print(f'Students inserted via SQL: {inserted}')
print(f'\n=== SUMMARY ===')
print(f'Classes: {len(classes)}')
print(f'User accounts: {created_users}')
print(f'Student records: {inserted}')
