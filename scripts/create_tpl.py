import subprocess, json, sys

BASE = "http://localhost:8080/api"
TOKEN = ""

def api(method, path, data=None):
    cmd = ["curl", "-s", "-X", method, f"{BASE}{path}", "-H", f"Authorization: Bearer {TOKEN}", "-H", "Content-Type: application/json"]
    if data: cmd += ["-d", json.dumps(data)]
    r = subprocess.run(cmd, capture_output=True, text=True, encoding="utf-8")
    try:
        j = json.loads(r.stdout)
        if j.get("code") != 200:
            print(f"  ERROR {path}: {j.get('message')}", file=sys.stderr)
            return None
        return j.get("data")
    except:
        print(f"  PARSE ERROR {path}: {r.stdout[:200]}", file=sys.stderr)
        return None

# Login
r = subprocess.run(["curl","-s","-X","POST",f"{BASE}/auth/login","-H","Content-Type: application/json","-d",'{"username":"admin","password":"admin123"}'], capture_output=True, text=True, encoding="utf-8")
TOKEN = json.loads(r.stdout)["data"]["accessToken"]
print("Logged in")

TYPES = ["TEXT","TEXTAREA","RICH_TEXT","NUMBER","SLIDER","CALCULATED","SELECT","MULTI_SELECT","CHECKBOX","RADIO","DATE","TIME","DATETIME","PHOTO","VIDEO","SIGNATURE","FILE_UPLOAD","GPS","BARCODE","RATING","PASS_FAIL","CHECKLIST"]
NAMES = ["文本","多行文本","富文本","数值","滑块","计算字段","下拉选择","多选","复选框","单选","日期","时间","日期时间","拍照","录像","签名","文件上传","GPS定位","条码扫描","评分","通过否","检查清单"]

# T1: 宿舍卫生检查
print("=== L1: 宿舍卫生检查 ===")
t = api("POST","/v7/insp/templates",{"templateName":"L1-宿舍卫生检查","description":"场所级,覆盖22种字段","targetType":"PLACE","catalogId":"1"})
T1 = t["id"]; print(f"T1={T1}")

s = api("POST",f"/v7/insp/templates/{T1}/sections",{"sectionCode":"HYG","sectionName":"房间卫生(全字段)","weight":40,"sortOrder":1})
for i,(tp,nm) in enumerate(zip(TYPES,NAMES)):
    api("POST",f"/v7/insp/sections/{s['id']}/items",{"itemCode":tp,"itemName":nm,"itemType":tp,"sortOrder":i+1})
print(f"  HYG: 22 items (all field types)")

s = api("POST",f"/v7/insp/templates/{T1}/sections",{"sectionCode":"SAFE","sectionName":"安全检查","weight":35,"sortOrder":2})
for item in [
    {"itemCode":"S1","itemName":"消防通道","itemType":"PASS_FAIL","isScored":True,"sortOrder":1},
    {"itemCode":"S2","itemName":"安全评分","itemType":"RATING","isScored":True,"sortOrder":2},
    {"itemCode":"S3","itemName":"隐患照片","itemType":"PHOTO","requireEvidence":True,"sortOrder":3},
]: api("POST",f"/v7/insp/sections/{s['id']}/items",item)
print(f"  SAFE: 3 items")

s = api("POST",f"/v7/insp/templates/{T1}/sections",{"sectionCode":"FAC","sectionName":"设施(可重复)","weight":15,"isRepeatable":True,"sortOrder":3})
api("POST",f"/v7/insp/sections/{s['id']}/items",{"itemCode":"F1","itemName":"完好度","itemType":"NUMBER","isScored":True,"sortOrder":1})
api("POST",f"/v7/insp/sections/{s['id']}/items",{"itemCode":"F2","itemName":"照片","itemType":"PHOTO","sortOrder":2})
print(f"  FAC: 2 items (repeatable)")

s = api("POST",f"/v7/insp/templates/{T1}/sections",{"sectionCode":"OVR","sectionName":"综合评价","weight":10,"sortOrder":4})
api("POST",f"/v7/insp/sections/{s['id']}/items",{"itemCode":"O1","itemName":"总评","itemType":"RATING","isScored":True,"sortOrder":1})
print(f"  OVR: 1 item | T1 total: 4 sections, 28 items")

# T2: 纪律检查
print("\n=== L1: 纪律检查 ===")
t = api("POST","/v7/insp/templates",{"templateName":"L1-纪律检查","description":"组织级纪律","targetType":"ORG","catalogId":"4"})
T2 = t["id"]; print(f"T2={T2}")
s = api("POST",f"/v7/insp/templates/{T2}/sections",{"sectionCode":"ATT","sectionName":"出勤纪律","weight":50,"sortOrder":1})
api("POST",f"/v7/insp/sections/{s['id']}/items",{"itemCode":"A1","itemName":"迟到人数","itemType":"NUMBER","isScored":True,"sortOrder":1})
api("POST",f"/v7/insp/sections/{s['id']}/items",{"itemCode":"A2","itemName":"着装规范","itemType":"PASS_FAIL","isScored":True,"sortOrder":2})
s = api("POST",f"/v7/insp/templates/{T2}/sections",{"sectionCode":"BHV","sectionName":"行为规范","weight":50,"sortOrder":2})
api("POST",f"/v7/insp/sections/{s['id']}/items",{"itemCode":"B1","itemName":"秩序评分","itemType":"RATING","isScored":True,"sortOrder":1})
api("POST",f"/v7/insp/sections/{s['id']}/items",{"itemCode":"B2","itemName":"违规事项","itemType":"TEXTAREA","sortOrder":2})
print(f"  2 sections, 4 items")

# T3: 实训室
print("\n=== L1: 实训室检查 ===")
t = api("POST","/v7/insp/templates",{"templateName":"L1-实训室检查","description":"设备安全","targetType":"PLACE","catalogId":"1"})
T3 = t["id"]; print(f"T3={T3}")
s = api("POST",f"/v7/insp/templates/{T3}/sections",{"sectionCode":"EQ","sectionName":"设备检查","weight":60,"sortOrder":1})
api("POST",f"/v7/insp/sections/{s['id']}/items",{"itemCode":"E1","itemName":"设备编号","itemType":"BARCODE","sortOrder":1})
api("POST",f"/v7/insp/sections/{s['id']}/items",{"itemCode":"E2","itemName":"运行状态","itemType":"SELECT","isScored":True,"sortOrder":2})
s = api("POST",f"/v7/insp/templates/{T3}/sections",{"sectionCode":"EN","sectionName":"环境安全","weight":40,"sortOrder":2})
api("POST",f"/v7/insp/sections/{s['id']}/items",{"itemCode":"N1","itemName":"通风设施","itemType":"PASS_FAIL","isScored":True,"sortOrder":1})
print(f"  2 sections, 3 items")

# T5: 消防
print("\n=== L1: 消防安全 ===")
t = api("POST","/v7/insp/templates",{"templateName":"L1-消防安全","description":"消防安全","targetType":"PLACE","catalogId":"1"})
T5 = t["id"]; print(f"T5={T5}")
s = api("POST",f"/v7/insp/templates/{T5}/sections",{"sectionCode":"FI","sectionName":"消防设施","weight":60,"sortOrder":1})
api("POST",f"/v7/insp/sections/{s['id']}/items",{"itemCode":"FI1","itemName":"喷淋系统","itemType":"PASS_FAIL","isScored":True,"sortOrder":1})
api("POST",f"/v7/insp/sections/{s['id']}/items",{"itemCode":"FI2","itemName":"报警器","itemType":"PASS_FAIL","isScored":True,"sortOrder":2})
s = api("POST",f"/v7/insp/templates/{T5}/sections",{"sectionCode":"EM","sectionName":"应急准备","weight":40,"sortOrder":2})
api("POST",f"/v7/insp/sections/{s['id']}/items",{"itemCode":"EM1","itemName":"急救箱","itemType":"CHECKLIST","sortOrder":1})
print(f"  2 sections, 3 items")

# L2: 常规检查
print("\n=== L2: 常规检查 (refs T1+T2+T3) ===")
t = api("POST","/v7/insp/templates",{"templateName":"L2-常规检查","description":"引用宿舍+纪律+实训室","targetType":"ORG","catalogId":"4"})
T4 = t["id"]; print(f"T4={T4}")
s = api("POST",f"/v7/insp/templates/{T4}/sections",{"sectionCode":"OV","sectionName":"检查概况","weight":10,"sortOrder":1})
api("POST",f"/v7/insp/sections/{s['id']}/items",{"itemCode":"D1","itemName":"检查日期","itemType":"DATE","isRequired":True,"sortOrder":1})
r1 = api("POST",f"/v7/insp/templates/{T4}/module-refs",{"moduleTemplateId":T1,"sortOrder":1,"weight":40})
r2 = api("POST",f"/v7/insp/templates/{T4}/module-refs",{"moduleTemplateId":T2,"sortOrder":2,"weight":35})
r3 = api("POST",f"/v7/insp/templates/{T4}/module-refs",{"moduleTemplateId":T3,"sortOrder":3,"weight":25})
print(f"  1 section + 3 module refs (refs: {r1 and 'OK'}, {r2 and 'OK'}, {r3 and 'OK'})")

# L3: 全面检查
print("\n=== L3: 全面检查 (refs L2+T5) ===")
t = api("POST","/v7/insp/templates",{"templateName":"L3-全面检查","description":"三级组合:常规+消防","targetType":"ORG","catalogId":"4"})
T6 = t["id"]; print(f"T6={T6}")
s = api("POST",f"/v7/insp/templates/{T6}/sections",{"sectionCode":"MT","sectionName":"检查信息","weight":5,"sortOrder":1})
api("POST",f"/v7/insp/sections/{s['id']}/items",{"itemCode":"M1","itemName":"学期","itemType":"TEXT","isRequired":True,"sortOrder":1})
api("POST",f"/v7/insp/sections/{s['id']}/items",{"itemCode":"M2","itemName":"带队签名","itemType":"SIGNATURE","isRequired":True,"sortOrder":2})
r1 = api("POST",f"/v7/insp/templates/{T6}/module-refs",{"moduleTemplateId":T4,"sortOrder":1,"weight":70})
r2 = api("POST",f"/v7/insp/templates/{T6}/module-refs",{"moduleTemplateId":T5,"sortOrder":2,"weight":30})
print(f"  1 section + 2 module refs (3-LEVEL hierarchy!)")

# Duplicate
print("\n=== Duplicate T1 ===")
copy = api("POST",f"/v7/insp/templates/{T1}/duplicate")
COPY = copy["id"] if copy else "FAILED"
print(f"Copy={COPY}")

# Publish
print("\n=== Publish All ===")
for name,tid in [("L1-宿舍",T1),("L1-纪律",T2),("L1-实训",T3),("L1-消防",T5),("L2-常规",T4),("L3-全面",T6)]:
    r = api("POST",f"/v7/insp/templates/{tid}/publish")
    print(f"  {name} ({tid}): {'OK' if r else 'FAIL'}")

print(f"\n{'='*50}")
print(f"L1 宿舍卫生(PLACE, 4sec, 28items, 22types): {T1}")
print(f"L1 纪律检查(ORG, 2sec, 4items):             {T2}")
print(f"L1 实训室(PLACE, 2sec, 3items):              {T3}")
print(f"L1 消防安全(PLACE, 2sec, 3items):            {T5}")
print(f"L2 常规检查(ORG, 1sec+3refs):                {T4}")
print(f"L3 全面检查(ORG, 1sec+2refs, 3-LEVEL):       {T6}")
print(f"副本(DRAFT):                                  {COPY}")
print(f"Total: 7 templates, 13 sections, 41+ items")
