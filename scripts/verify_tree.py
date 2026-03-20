# -*- coding: utf-8 -*-
import requests
import json
import sys

TOKEN = sys.argv[1]
session = requests.Session()
session.trust_env = False

resp = session.get(
    "http://localhost:8080/api/v9/places/tree",
    headers={"Authorization": f"Bearer {TOKEN}"}
)
data = resp.json()["data"]

def show(nodes, indent=0):
    for n in nodes:
        name = n.get("placeName", "?")
        tc = n.get("typeCode", "?")
        cap = n.get("capacity")
        cap_str = f" cap={cap}" if cap else ""
        print(f"{'  ' * indent}{name} ({tc}){cap_str}")
        if n.get("children"):
            show(n["children"], indent + 1)

print(f"Root nodes: {len(data)}")
show(data)
