#!/usr/bin/env python3
"""Create complete test data: school -> departments -> grades -> classes"""
import os
os.environ['no_proxy'] = 'localhost,127.0.0.1'
import urllib.request, json, ssl

ssl._create_default_https_context = ssl._create_unverified_context
base = 'http://localhost:8080/api'

def login():
    data = json.dumps({'username': 'admin', 'password': 'admin123'}).encode()
    req = urllib.request.Request(f'{base}/auth/login', data, {'Content-Type': 'application/json'}, method='POST')
    resp = urllib.request.urlopen(req)
    return json.loads(resp.read())['data']['accessToken']

token = login()
headers = {'Authorization': f'Bearer {token}', 'Content-Type': 'application/json; charset=utf-8'}

def get(path):
    req = urllib.request.Request(f'{base}{path}', None, headers)
    resp = urllib.request.urlopen(req)
    return json.loads(resp.read())

def post(path, data):
    body = json.dumps(data).encode('utf-8')
    req = urllib.request.Request(f'{base}{path}', body, headers, method='POST')
    try:
        resp = urllib.request.urlopen(req)
        return json.loads(resp.read())
    except urllib.error.HTTPError as e:
        return json.loads(e.read().decode())

# Get existing tree
tree = get('/org-units/tree').get('data', [])
def find_all(nodes, tc):
    r = []
    for n in nodes:
        if n.get('typeCode') == tc or n.get('unitType') == tc: r.append(n)
        r.extend(find_all(n.get('children', []), tc))
    return r

school = tree[0] if tree else None
sid = int(school['id'])
print(f"School: {school['unitName']} ({sid})")

# Departments
existing = {d['unitName']: int(d['id']) for d in find_all(tree, 'DEPARTMENT')}
print(f"Existing depts: {existing}")

needed = {'康养系': 'DEPT-KY', '汽车工程系': 'DEPT-QC', '艺术教育系': 'DEPT-YS'}
for name, code in needed.items():
    if name not in existing:
        r = post('/org-units', {'unitName': name, 'unitCode': code, 'unitType': 'DEPARTMENT', 'parentId': sid})
        did = r.get('data', {}).get('id')
        if did: existing[name] = int(did); print(f"  Created {name}: {did}")
        else: print(f"  FAILED {name}: {r.get('message','')}")

print(f"\nDepts: {len(existing)}")

# Grades
grades = {}
for dname, did in existing.items():
    for year in [2024, 2025]:
        gcode = f'GRD-{hash(dname) % 10000}-{year}'
        r = post('/org-units', {
            'unitName': f'{year}级', 'unitCode': gcode, 'unitType': 'GRADE',
            'parentId': did, 'attributes': {'enrollmentYear': year}
        })
        gid = r.get('data', {}).get('id')
        if gid:
            grades[f'{dname}_{year}'] = int(gid)
            print(f"  {dname}/{year}: {gid}")
        else:
            print(f"  FAILED {dname}/{year}: {r.get('message','')}")

# Classes
classes = {}
n = 0
for key, gid in grades.items():
    dname, year = key.rsplit('_', 1)
    ncls = 3 if '经济' in dname else 2
    for i in range(1, ncls + 1):
        n += 1
        cname = f'{dname[:2]}{year}-{i}班'
        r = post('/org-units', {
            'unitName': cname, 'unitCode': f'CLS-{n:04d}', 'unitType': 'CLASS',
            'parentId': gid, 'attributes': {'enrollmentYear': int(year), 'classType': 1}
        })
        cid = r.get('data', {}).get('id')
        if cid:
            classes[cname] = int(cid)
            print(f"    {cname}: {cid}")
        else:
            print(f"    FAILED {cname}: {r.get('message','')}")

print(f"\n=== SUMMARY ===")
print(f"Departments: {len(existing)}")
print(f"Grades: {len(grades)}")
print(f"Classes: {len(classes)}")
