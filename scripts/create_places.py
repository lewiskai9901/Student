#!/usr/bin/env python3
"""Create place hierarchy: 2 teaching buildings + 2 dorm buildings with rooms."""
import os
os.environ['no_proxy'] = 'localhost,127.0.0.1'
import urllib.request, json, sys, io, time

sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8')

base = 'http://localhost:8080/api'

def login():
    data = json.dumps({'username': 'admin', 'password': 'admin123'}).encode()
    req = urllib.request.Request(f'{base}/auth/login', data, {'Content-Type': 'application/json'}, method='POST')
    resp = urllib.request.urlopen(req)
    return json.loads(resp.read())['data']['accessToken']

token = login()
h = {'Authorization': f'Bearer {token}', 'Content-Type': 'application/json; charset=utf-8'}

def post(path, data):
    body = json.dumps(data).encode('utf-8')
    req = urllib.request.Request(f'{base}{path}', body, h, method='POST')
    try:
        resp = urllib.request.urlopen(req)
        return json.loads(resp.read())
    except urllib.error.HTTPError as e:
        return json.loads(e.read().decode())

CAMPUS_ID = 1

# ==========================================
# 1. Teaching Buildings
# ==========================================
print("=== TEACHING BUILDINGS ===")

for bldg_num, bldg_name in [(1, '第一教学楼'), (2, '第二教学楼')]:
    r = post('/v9/places', {
        'placeName': bldg_name,
        'placeCode': f'BLDG-T{bldg_num}',
        'typeCode': 'TYPE_TEACH_BLDG',
        'parentId': CAMPUS_ID,
        'status': 1,
    })
    bldg_id = r.get('data', {}).get('id') if isinstance(r.get('data'), dict) else None
    print(f'{bldg_name}: {bldg_id} ({r.get("message", "")[:40]})')

    if not bldg_id:
        continue
    bldg_id = int(bldg_id)

    # Create 4 floors per building
    for floor_num in range(1, 5):
        r = post('/v9/places', {
            'placeName': f'{bldg_name}{floor_num}楼',
            'placeCode': f'T{bldg_num}-F{floor_num}',
            'typeCode': 'TYPE_TEACH_FLOOR',
            'parentId': bldg_id,
            'status': 1,
        })
        floor_id = r.get('data', {}).get('id') if isinstance(r.get('data'), dict) else None
        print(f'  {floor_num}楼: {floor_id}')

        if not floor_id:
            continue
        floor_id = int(floor_id)

        # 3 classrooms per floor
        for room_num in range(1, 4):
            room_no = f'{floor_num}0{room_num}'
            r = post('/v9/places', {
                'placeName': f'{room_no}教室',
                'placeCode': f'T{bldg_num}-{room_no}',
                'typeCode': 'TYPE_CLASSROOM',
                'parentId': floor_id,
                'status': 1,
                'capacity': 50,
            })
            room_id = r.get('data', {}).get('id') if isinstance(r.get('data'), dict) else None
            print(f'    {room_no}教室: {room_id}')

# ==========================================
# 2. Dorm Buildings
# ==========================================
print("\n=== DORM BUILDINGS ===")

dorm_buildings = [
    (1, '男生宿舍1号楼', 1),  # gender=1 (male)
    (2, '女生宿舍2号楼', 2),  # gender=2 (female)
]

for bldg_num, bldg_name, gender in dorm_buildings:
    r = post('/v9/places', {
        'placeName': bldg_name,
        'placeCode': f'BLDG-D{bldg_num}',
        'typeCode': 'DORM_BUILDING',
        'parentId': CAMPUS_ID,
        'status': 1,
        'gender': gender,
    })
    bldg_id = r.get('data', {}).get('id') if isinstance(r.get('data'), dict) else None
    print(f'{bldg_name}: {bldg_id} ({r.get("message", "")[:40]})')

    if not bldg_id:
        continue
    bldg_id = int(bldg_id)

    # Create 6 floors per dorm building
    for floor_num in range(1, 7):
        r = post('/v9/places', {
            'placeName': f'{bldg_name}{floor_num}层',
            'placeCode': f'D{bldg_num}-F{floor_num}',
            'typeCode': 'DORM_FLOOR',
            'parentId': bldg_id,
            'status': 1,
            'gender': gender,
        })
        floor_id = r.get('data', {}).get('id') if isinstance(r.get('data'), dict) else None
        print(f'  {floor_num}层: {floor_id}')

        if not floor_id:
            continue
        floor_id = int(floor_id)

        # 10 rooms per floor, 6 beds each
        for room_num in range(1, 11):
            room_no = f'{floor_num}{room_num:02d}'
            r = post('/v9/places', {
                'placeName': f'{room_no}宿舍',
                'placeCode': f'D{bldg_num}-{room_no}',
                'typeCode': 'DORM_ROOM',
                'parentId': floor_id,
                'status': 1,
                'capacity': 6,
                'gender': gender,
            })
            room_id = r.get('data', {}).get('id') if isinstance(r.get('data'), dict) else None
            if room_num <= 2:  # Only print first 2 per floor
                print(f'    {room_no}宿舍: {room_id}')

    print(f'  ({bldg_name}: 6 floors x 10 rooms = 60 rooms)')

print("\n=== DONE ===")
