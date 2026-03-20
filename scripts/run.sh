#!/bin/bash
BASE="http://localhost:8080/api"
TOKEN=$(curl -s -X POST "$BASE/auth/login" -H "Content-Type: application/json" -d '{"username":"admin","password":"admin123"}' | grep -o '"accessToken":"[^"]*"' | cut -d'"' -f4)
A="Authorization: Bearer $TOKEN"
C="Content-Type: application/json"
gid() { echo "$1" | grep -o '"id":"[^"]*"' | head -1 | cut -d'"' -f4; }

R=$(curl -s -X POST "$BASE/v7/insp/templates" -H "$A" -H "$C" -d '{"templateName":"L1-宿舍卫生检查","description":"22种字段","targetType":"PLACE","catalogId":"1"}')
T1=$(gid "$R"); echo "T1=$T1"
R=$(curl -s -X POST "$BASE/v7/insp/templates/$T1/sections" -H "$A" -H "$C" -d '{"sectionCode":"HYG","sectionName":"房间卫生(全字段)","weight":40,"sortOrder":1}')
S1=$(gid "$R")
for TYPE in TEXT TEXTAREA RICH_TEXT NUMBER SLIDER CALCULATED SELECT MULTI_SELECT CHECKBOX RADIO DATE TIME DATETIME PHOTO VIDEO SIGNATURE FILE_UPLOAD GPS BARCODE RATING PASS_FAIL CHECKLIST; do
  curl -s -X POST "$BASE/v7/insp/sections/$S1/items" -H "$A" -H "$C" -d "{\"itemCode\":\"$TYPE\",\"itemName\":\"$TYPE\",\"itemType\":\"$TYPE\",\"sortOrder\":0}" > /dev/null
done
echo "  22 items"
R=$(curl -s -X POST "$BASE/v7/insp/templates/$T1/sections" -H "$A" -H "$C" -d '{"sectionCode":"SAFE","sectionName":"安全检查","weight":35,"sortOrder":2}')
S2=$(gid "$R")
curl -s -X POST "$BASE/v7/insp/sections/$S2/items" -H "$A" -H "$C" -d '{"itemCode":"S1","itemName":"消防通道","itemType":"PASS_FAIL","isScored":true,"sortOrder":1}' > /dev/null
curl -s -X POST "$BASE/v7/insp/sections/$S2/items" -H "$A" -H "$C" -d '{"itemCode":"S2","itemName":"评分","itemType":"RATING","isScored":true,"sortOrder":2}' > /dev/null
R=$(curl -s -X POST "$BASE/v7/insp/templates/$T1/sections" -H "$A" -H "$C" -d '{"sectionCode":"FAC","sectionName":"设施(可重复)","weight":15,"isRepeatable":true,"sortOrder":3}')
S3=$(gid "$R")
curl -s -X POST "$BASE/v7/insp/sections/$S3/items" -H "$A" -H "$C" -d '{"itemCode":"F1","itemName":"完好度","itemType":"NUMBER","isScored":true,"sortOrder":1}' > /dev/null
R=$(curl -s -X POST "$BASE/v7/insp/templates/$T1/sections" -H "$A" -H "$C" -d '{"sectionCode":"OVR","sectionName":"综合","weight":10,"sortOrder":4}')
S4=$(gid "$R")
curl -s -X POST "$BASE/v7/insp/sections/$S4/items" -H "$A" -H "$C" -d '{"itemCode":"O1","itemName":"总评","itemType":"RATING","isScored":true,"sortOrder":1}' > /dev/null

R=$(curl -s -X POST "$BASE/v7/insp/templates" -H "$A" -H "$C" -d '{"templateName":"L1-纪律检查","targetType":"ORG","catalogId":"4"}')
T2=$(gid "$R"); echo "T2=$T2"
R=$(curl -s -X POST "$BASE/v7/insp/templates/$T2/sections" -H "$A" -H "$C" -d '{"sectionCode":"ATT","sectionName":"出勤","weight":50,"sortOrder":1}')
SA=$(gid "$R")
curl -s -X POST "$BASE/v7/insp/sections/$SA/items" -H "$A" -H "$C" -d '{"itemCode":"A1","itemName":"迟到","itemType":"NUMBER","isScored":true,"sortOrder":1}' > /dev/null
curl -s -X POST "$BASE/v7/insp/sections/$SA/items" -H "$A" -H "$C" -d '{"itemCode":"A2","itemName":"着装","itemType":"PASS_FAIL","isScored":true,"sortOrder":2}' > /dev/null
R=$(curl -s -X POST "$BASE/v7/insp/templates/$T2/sections" -H "$A" -H "$C" -d '{"sectionCode":"BHV","sectionName":"行为","weight":50,"sortOrder":2}')
SB=$(gid "$R")
curl -s -X POST "$BASE/v7/insp/sections/$SB/items" -H "$A" -H "$C" -d '{"itemCode":"B1","itemName":"秩序","itemType":"RATING","isScored":true,"sortOrder":1}' > /dev/null

R=$(curl -s -X POST "$BASE/v7/insp/templates" -H "$A" -H "$C" -d '{"templateName":"L1-实训室检查","targetType":"PLACE","catalogId":"1"}')
T3=$(gid "$R"); echo "T3=$T3"
R=$(curl -s -X POST "$BASE/v7/insp/templates/$T3/sections" -H "$A" -H "$C" -d '{"sectionCode":"EQ","sectionName":"设备","weight":60,"sortOrder":1}')
SE=$(gid "$R")
curl -s -X POST "$BASE/v7/insp/sections/$SE/items" -H "$A" -H "$C" -d '{"itemCode":"E1","itemName":"编号","itemType":"BARCODE","sortOrder":1}' > /dev/null
curl -s -X POST "$BASE/v7/insp/sections/$SE/items" -H "$A" -H "$C" -d '{"itemCode":"E2","itemName":"状态","itemType":"SELECT","isScored":true,"sortOrder":2}' > /dev/null
R=$(curl -s -X POST "$BASE/v7/insp/templates/$T3/sections" -H "$A" -H "$C" -d '{"sectionCode":"EN","sectionName":"环境","weight":40,"sortOrder":2}')
SN=$(gid "$R")
curl -s -X POST "$BASE/v7/insp/sections/$SN/items" -H "$A" -H "$C" -d '{"itemCode":"N1","itemName":"通风","itemType":"PASS_FAIL","isScored":true,"sortOrder":1}' > /dev/null

R=$(curl -s -X POST "$BASE/v7/insp/templates" -H "$A" -H "$C" -d '{"templateName":"L1-消防安全","targetType":"PLACE","catalogId":"1"}')
T5=$(gid "$R"); echo "T5=$T5"
R=$(curl -s -X POST "$BASE/v7/insp/templates/$T5/sections" -H "$A" -H "$C" -d '{"sectionCode":"FI","sectionName":"消防","weight":60,"sortOrder":1}')
SF=$(gid "$R")
curl -s -X POST "$BASE/v7/insp/sections/$SF/items" -H "$A" -H "$C" -d '{"itemCode":"FI1","itemName":"喷淋","itemType":"PASS_FAIL","isScored":true,"sortOrder":1}' > /dev/null
R=$(curl -s -X POST "$BASE/v7/insp/templates/$T5/sections" -H "$A" -H "$C" -d '{"sectionCode":"EM","sectionName":"应急","weight":40,"sortOrder":2}')
SM=$(gid "$R")
curl -s -X POST "$BASE/v7/insp/sections/$SM/items" -H "$A" -H "$C" -d '{"itemCode":"EM1","itemName":"急救","itemType":"CHECKLIST","sortOrder":1}' > /dev/null

echo "=== L2 ==="
R=$(curl -s -X POST "$BASE/v7/insp/templates" -H "$A" -H "$C" -d '{"templateName":"L2-常规检查","description":"引用宿舍+纪律+实训室","targetType":"ORG","catalogId":"4"}')
T4=$(gid "$R"); echo "T4=$T4"
R=$(curl -s -X POST "$BASE/v7/insp/templates/$T4/sections" -H "$A" -H "$C" -d '{"sectionCode":"OV","sectionName":"概况","weight":10,"sortOrder":1}')
SO=$(gid "$R")
curl -s -X POST "$BASE/v7/insp/sections/$SO/items" -H "$A" -H "$C" -d '{"itemCode":"D1","itemName":"日期","itemType":"DATE","isRequired":true,"sortOrder":1}' > /dev/null
curl -s -X POST "$BASE/v7/insp/templates/$T4/module-refs" -H "$A" -H "$C" -d "{\"moduleTemplateId\":\"$T1\",\"sortOrder\":1,\"weight\":40}" > /dev/null
curl -s -X POST "$BASE/v7/insp/templates/$T4/module-refs" -H "$A" -H "$C" -d "{\"moduleTemplateId\":\"$T2\",\"sortOrder\":2,\"weight\":35}" > /dev/null
curl -s -X POST "$BASE/v7/insp/templates/$T4/module-refs" -H "$A" -H "$C" -d "{\"moduleTemplateId\":\"$T3\",\"sortOrder\":3,\"weight\":25}" > /dev/null
echo "  3 refs added"

echo "=== L3 ==="
R=$(curl -s -X POST "$BASE/v7/insp/templates" -H "$A" -H "$C" -d '{"templateName":"L3-全面检查","description":"引用常规+消防(3层)","targetType":"ORG","catalogId":"4"}')
T6=$(gid "$R"); echo "T6=$T6"
R=$(curl -s -X POST "$BASE/v7/insp/templates/$T6/sections" -H "$A" -H "$C" -d '{"sectionCode":"MT","sectionName":"信息","weight":5,"sortOrder":1}')
SM2=$(gid "$R")
curl -s -X POST "$BASE/v7/insp/sections/$SM2/items" -H "$A" -H "$C" -d '{"itemCode":"M1","itemName":"学期","itemType":"TEXT","sortOrder":1}' > /dev/null
curl -s -X POST "$BASE/v7/insp/sections/$SM2/items" -H "$A" -H "$C" -d '{"itemCode":"M2","itemName":"签名","itemType":"SIGNATURE","sortOrder":2}' > /dev/null
curl -s -X POST "$BASE/v7/insp/templates/$T6/module-refs" -H "$A" -H "$C" -d "{\"moduleTemplateId\":\"$T4\",\"sortOrder\":1,\"weight\":70}" > /dev/null
curl -s -X POST "$BASE/v7/insp/templates/$T6/module-refs" -H "$A" -H "$C" -d "{\"moduleTemplateId\":\"$T5\",\"sortOrder\":2,\"weight\":30}" > /dev/null
echo "  2 refs (3-level)"

echo "=== Duplicate ==="
R=$(curl -s -X POST "$BASE/v7/insp/templates/$T1/duplicate" -H "$A" -H "$C")
COPY=$(gid "$R"); echo "Copy=$COPY"

echo "=== Publish ==="
for TID in $T1 $T2 $T3 $T5 $T4 $T6; do
  R=$(curl -s -X POST "$BASE/v7/insp/templates/$TID/publish" -H "$A" -H "$C")
  echo "  $TID: $(echo "$R" | grep -o '"code":[0-9]*')"
done

echo ""
echo "DONE: T1=$T1 T2=$T2 T3=$T3 T5=$T5 T4=$T4 T6=$T6 COPY=$COPY"
