"""One-time script: clean old positions, recreate from org type templates."""
import json
import subprocess


def curl(method, path, data=None):
    cmd = ["curl", "-s", "-X", method, f"http://localhost:8080/api{path}",
           "-H", f"Authorization: Bearer {TOKEN}",
           "-H", "Content-Type: application/json"]
    if data:
        cmd += ["-d", json.dumps(data, ensure_ascii=False)]
    result = subprocess.run(cmd, capture_output=True, text=True, encoding="utf-8")
    return json.loads(result.stdout)


# Login
login_cmd = ["curl", "-s", "-X", "POST", "http://localhost:8080/api/auth/login",
             "-H", "Content-Type: application/json",
             "-d", json.dumps({"username": "admin", "password": "admin123"})]
login_resp = json.loads(subprocess.run(login_cmd, capture_output=True, text=True, encoding="utf-8").stdout)
TOKEN = login_resp["data"]["accessToken"]
print("Logged in.")


# 1. Collect all org units
def walk(nodes):
    result = []
    for n in nodes:
        result.append(n)
        if n.get("children"):
            result.extend(walk(n["children"]))
    return result


tree = curl("GET", "/org-units/tree")["data"]
all_orgs = walk(tree)
print(f"Found {len(all_orgs)} org units\n")

# 2. For each org unit, delete old positions, create from template
for org in all_orgs:
    org_id = org["id"]
    unit_type = org.get("unitType", "")
    unit_name = org["unitName"]

    # Get current positions
    positions = curl("GET", f"/positions?orgUnitId={org_id}")["data"]

    # Delete all existing positions
    for p in positions:
        curl("DELETE", f"/positions/{p['id']}")
        print(f"  Deleted: {p['positionCode']} ({p['positionName']})")

    # Get org type template
    try:
        org_type = curl("GET", f"/org-types/code/{unit_type}")["data"]
        templates = org_type.get("defaultPositions") or []
    except Exception:
        templates = []

    if not templates:
        print(f"[{unit_name}] ({unit_type}) - no template, skipped")
        continue

    # Create from template
    for tpl in templates:
        data = {
            "positionCode": f"{org_id}_{tpl['positionCode']}",
            "positionName": tpl["positionName"],
            "orgUnitId": org_id,
            "headcount": tpl.get("headcount", 1),
            "keyPosition": tpl.get("keyPosition", False),
            "jobLevel": tpl.get("jobLevel"),
        }
        result = curl("POST", "/positions", data)
        status = "OK" if result.get("code") == 200 else f"FAIL: {result.get('message','?')}"
        print(f"  Created: {tpl['positionName']} - {status}")

    print(f"[{unit_name}] ({unit_type}) - synced {len(templates)} positions\n")

print("Done!")
