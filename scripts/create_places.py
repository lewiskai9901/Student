# -*- coding: utf-8 -*-
"""Create places via /v9/places API"""
import requests
import json
import sys

BASE = "http://localhost:8080/api"
TOKEN = sys.argv[1] if len(sys.argv) > 1 else ""

# Disable proxy
session = requests.Session()
session.trust_env = False

headers = {
    "Authorization": f"Bearer {TOKEN}",
    "Content-Type": "application/json; charset=utf-8"
}

# Department IDs
DEPTS = {
    1: {"name": "经济与信息技术系", "id": 2021489996992729089},
    2: {"name": "汽车工程系",       "id": 2021489997688983554},
    3: {"name": "康养系",           "id": 2021489998532038657},
    4: {"name": "智能制造系",       "id": 2021489999224098817},
}

def create_place(data):
    resp = session.post(f"{BASE}/v9/places", json=data, headers=headers)
    print(f"  Status: {resp.status_code}, Length: {len(resp.text)}")
    if not resp.text:
        print(f"  ERROR: Empty response for {data.get('placeName')}")
        return None
    result = resp.json()
    if result.get("code") != 200:
        print(f"  ERROR: {result.get('message')} for {data.get('placeName')}")
        return None
    pid = result["data"]["id"]
    print(f"  Created: {data['placeName']} (ID={pid})")
    return int(pid)

# 1. Create SITE
site_id = create_place({
    "placeName": "主校区",
    "typeCode": "SITE",
    "description": "技师学院主校区"
})
if not site_id:
    print("Failed to create site, aborting")
    sys.exit(1)

# 2. Create Buildings
buildings = [
    {"name": "男生宿舍楼", "desc": "男生宿舍楼", "rooms_per_floor": 8, "room_prefix": "M", "capacity": 8},
    {"name": "女生宿舍楼", "desc": "女生宿舍楼", "rooms_per_floor": 8, "room_prefix": "F", "capacity": 8},
    {"name": "教学楼",     "desc": "教学楼",     "rooms_per_floor": 4, "room_prefix": "C", "capacity": 50},
]

for bldg in buildings:
    bldg_id = create_place({
        "placeName": bldg["name"],
        "typeCode": "BUILDING",
        "parentId": site_id,
        "description": bldg["desc"]
    })
    if not bldg_id:
        continue

    # 3. Create 4 Floors per building
    for floor_num in range(1, 5):
        dept = DEPTS[floor_num]
        floor_name = f"{floor_num}F"
        floor_id = create_place({
            "placeName": floor_name,
            "typeCode": "FLOOR",
            "parentId": bldg_id,
            "orgUnitId": dept["id"],
            "description": f"{bldg['name']} {floor_name} - {dept['name']}"
        })
        if not floor_id:
            continue

        # 4. Create Rooms per floor
        for room_num in range(1, bldg["rooms_per_floor"] + 1):
            room_code = f"{floor_num}{room_num:02d}"
            if bldg["room_prefix"] == "C":
                room_name = f"教室{room_code}"
            elif bldg["room_prefix"] == "M":
                room_name = f"男{room_code}室"
            else:
                room_name = f"女{room_code}室"

            create_place({
                "placeName": room_name,
                "typeCode": "ROOM",
                "parentId": floor_id,
                "capacity": bldg["capacity"],
                "orgUnitId": dept["id"],
                "description": f"{dept['name']} - {room_name}"
            })

print("\nDone! All places created.")
