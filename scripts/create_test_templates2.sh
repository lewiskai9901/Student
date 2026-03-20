#!/bin/bash
set -e
BASE="http://localhost:8080/api"
TOKEN=$(curl -s -X POST "$BASE/auth/login" -H "Content-Type: application/json" -d "{\"username\":\"admin\",\"password\":\"admin123\"}" | grep -o '"accessToken":"[^"]*"' | cut -d'"' -f4)
AUTH="Authorization: Bearer $TOKEN"
CT="Content-Type: application/json"

gid() { grep -o '"id":"[^"]*"' | head -1 | cut -d'"' -f4; }

echo "=== L1: 宿舍卫生检查 ==="
T1_ID=$(curl -s -X POST "$BASE/v7/insp/templates" -H "$AUTH" -H "$CT" -d "{\"templateName\":\"L1-宿舍卫生检查\",\"description\":\"覆盖全部22种字段类型\",\"targetType\":\"PLACE\",\"catalogId\":\"1\"}" | gid)
echo "T1=$T1_ID"

S1_ID=$(curl -s -X POST "$BASE/v7/insp/templates/$T1_ID/sections" -H "$AUTH" -H "$CT" -d "{\"sectionCode\":\"HYG\",\"sectionName\":\"房间卫生\",\"weight\":40,\"sortOrder\":1}" | gid)
for TYPE in TEXT TEXTAREA RICH_TEXT NUMBER SLIDER CALCULATED SELECT MULTI_SELECT CHECKBOX RADIO DATE TIME DATETIME PHOTO VIDEO SIGNATURE FILE_UPLOAD GPS BARCODE RATING PASS_FAIL CHECKLIST; do
  curl -s -X POST "$BASE/v7/insp/sections/$S1_ID/items" -H "$AUTH" -H "$CT" -d "{\"itemCode\":\"$TYPE\",\"itemName\":\"$TYPE\",\"itemType\":\"$TYPE\",\"isRequired\":false,\"sortOrder\":0}" > /dev/null
done
echo "  HYG: 22 items (all types)"

S2_ID=$(curl -s -X POST "$BASE/v7/insp/templates/$T1_ID/sections" -H "$AUTH" -H "$CT" -d "{\"sectionCode\":\"SAFE\",\"sectionName\":\"安全检查\",\"weight\":35,\"sortOrder\":2}" | gid)
curl -s -X POST "$BASE/v7/insp/sections/$S2_ID/items" -H "$AUTH" -H "$CT" -d "{\"itemCode\":\"S1\",\"itemName\":\"消防通道\",\"itemType\":\"PASS_FAIL\",\"isRequired\":true,\"isScored\":true,\"sortOrder\":1}" > /dev/null
curl -s -X POST "$BASE/v7/insp/sections/$S2_ID/items" -H "$AUTH" -H "$CT" -d "{\"itemCode\":\"S2\",\"itemName\":\"安全评分\",\"itemType\":\"RATING\",\"isScored\":true,\"sortOrder\":2}" > /dev/null
curl -s -X POST "$BASE/v7/insp/sections/$S2_ID/items" -H "$AUTH" -H "$CT" -d "{\"itemCode\":\"S3\",\"itemName\":\"隐患照片\",\"itemType\":\"PHOTO\",\"requireEvidence\":true,\"sortOrder\":3}" > /dev/null
echo "  SAFE: 3 items"

S3_ID=$(curl -s -X POST "$BASE/v7/insp/templates/$T1_ID/sections" -H "$AUTH" -H "$CT" -d "{\"sectionCode\":\"FAC\",\"sectionName\":\"设施检查\",\"weight\":15,\"isRepeatable\":true,\"sortOrder\":3}" | gid)
curl -s -X POST "$BASE/v7/insp/sections/$S3_ID/items" -H "$AUTH" -H "$CT" -d "{\"itemCode\":\"F1\",\"itemName\":\"完好度\",\"itemType\":\"NUMBER\",\"isScored\":true,\"sortOrder\":1}" > /dev/null
curl -s -X POST "$BASE/v7/insp/sections/$S3_ID/items" -H "$AUTH" -H "$CT" -d "{\"itemCode\":\"F2\",\"itemName\":\"设施照片\",\"itemType\":\"PHOTO\",\"sortOrder\":2}" > /dev/null
echo "  FAC: 2 items (repeatable)"

S4_ID=$(curl -s -X POST "$BASE/v7/insp/templates/$T1_ID/sections" -H "$AUTH" -H "$CT" -d "{\"sectionCode\":\"OVR\",\"sectionName\":\"综合评价\",\"weight\":10,\"sortOrder\":4}" | gid)
curl -s -X POST "$BASE/v7/insp/sections/$S4_ID/items" -H "$AUTH" -H "$CT" -d "{\"itemCode\":\"O1\",\"itemName\":\"总评\",\"itemType\":\"RATING\",\"isScored\":true,\"sortOrder\":1}" > /dev/null
echo "  OVR: 1 item"

echo ""
echo "=== L1: 纪律检查 ==="
T2_ID=$(curl -s -X POST "$BASE/v7/insp/templates" -H "$AUTH" -H "$CT" -d "{\"templateName\":\"L1-纪律检查\",\"description\":\"组织级纪律\",\"targetType\":\"ORG\",\"catalogId\":\"4\"}" | gid)
echo "T2=$T2_ID"

S5_ID=$(curl -s -X POST "$BASE/v7/insp/templates/$T2_ID/sections" -H "$AUTH" -H "$CT" -d "{\"sectionCode\":\"ATT\",\"sectionName\":\"出勤\",\"weight\":50,\"sortOrder\":1}" | gid)
curl -s -X POST "$BASE/v7/insp/sections/$S5_ID/items" -H "$AUTH" -H "$CT" -d "{\"itemCode\":\"A1\",\"itemName\":\"迟到\",\"itemType\":\"NUMBER\",\"isScored\":true,\"sortOrder\":1}" > /dev/null
curl -s -X POST "$BASE/v7/insp/sections/$S5_ID/items" -H "$AUTH" -H "$CT" -d "{\"itemCode\":\"A2\",\"itemName\":\"缺勤\",\"itemType\":\"NUMBER\",\"isScored\":true,\"sortOrder\":2}" > /dev/null
curl -s -X POST "$BASE/v7/insp/sections/$S5_ID/items" -H "$AUTH" -H "$CT" -d "{\"itemCode\":\"A3\",\"itemName\":\"着装\",\"itemType\":\"PASS_FAIL\",\"isScored\":true,\"sortOrder\":3}" > /dev/null

S6_ID=$(curl -s -X POST "$BASE/v7/insp/templates/$T2_ID/sections" -H "$AUTH" -H "$CT" -d "{\"sectionCode\":\"BHV\",\"sectionName\":\"行为\",\"weight\":50,\"sortOrder\":2}" | gid)
curl -s -X POST "$BASE/v7/insp/sections/$S6_ID/items" -H "$AUTH" -H "$CT" -d "{\"itemCode\":\"B1\",\"itemName\":\"秩序\",\"itemType\":\"RATING\",\"isScored\":true,\"sortOrder\":1}" > /dev/null
curl -s -X POST "$BASE/v7/insp/sections/$S6_ID/items" -H "$AUTH" -H "$CT" -d "{\"itemCode\":\"B2\",\"itemName\":\"整洁\",\"itemType\":\"RATING\",\"isScored\":true,\"sortOrder\":2}" > /dev/null
curl -s -X POST "$BASE/v7/insp/sections/$S6_ID/items" -H "$AUTH" -H "$CT" -d "{\"itemCode\":\"B3\",\"itemName\":\"违规\",\"itemType\":\"TEXTAREA\",\"sortOrder\":3}" > /dev/null
echo "  2 sections, 6 items"

echo ""
echo "=== L1: 实训室检查 ==="
T3_ID=$(curl -s -X POST "$BASE/v7/insp/templates" -H "$AUTH" -H "$CT" -d "{\"templateName\":\"L1-实训室检查\",\"description\":\"设备与安全\",\"targetType\":\"PLACE\",\"catalogId\":\"1\"}" | gid)
echo "T3=$T3_ID"

S7_ID=$(curl -s -X POST "$BASE/v7/insp/templates/$T3_ID/sections" -H "$AUTH" -H "$CT" -d "{\"sectionCode\":\"EQ\",\"sectionName\":\"设备\",\"weight\":60,\"sortOrder\":1}" | gid)
curl -s -X POST "$BASE/v7/insp/sections/$S7_ID/items" -H "$AUTH" -H "$CT" -d "{\"itemCode\":\"E1\",\"itemName\":\"设备编号\",\"itemType\":\"BARCODE\",\"sortOrder\":1}" > /dev/null
curl -s -X POST "$BASE/v7/insp/sections/$S7_ID/items" -H "$AUTH" -H "$CT" -d "{\"itemCode\":\"E2\",\"itemName\":\"运行状态\",\"itemType\":\"SELECT\",\"isScored\":true,\"sortOrder\":2}" > /dev/null
curl -s -X POST "$BASE/v7/insp/sections/$S7_ID/items" -H "$AUTH" -H "$CT" -d "{\"itemCode\":\"E3\",\"itemName\":\"照片\",\"itemType\":\"PHOTO\",\"requireEvidence\":true,\"sortOrder\":3}" > /dev/null

S8_ID=$(curl -s -X POST "$BASE/v7/insp/templates/$T3_ID/sections" -H "$AUTH" -H "$CT" -d "{\"sectionCode\":\"EN\",\"sectionName\":\"环境\",\"weight\":40,\"sortOrder\":2}" | gid)
curl -s -X POST "$BASE/v7/insp/sections/$S8_ID/items" -H "$AUTH" -H "$CT" -d "{\"itemCode\":\"N1\",\"itemName\":\"通风\",\"itemType\":\"PASS_FAIL\",\"isScored\":true,\"sortOrder\":1}" > /dev/null
curl -s -X POST "$BASE/v7/insp/sections/$S8_ID/items" -H "$AUTH" -H "$CT" -d "{\"itemCode\":\"N2\",\"itemName\":\"废弃物\",\"itemType\":\"PASS_FAIL\",\"isScored\":true,\"sortOrder\":2}" > /dev/null
echo "  2 sections, 5 items"

echo ""
echo "=== L1: 消防安全 ==="
T5_ID=$(curl -s -X POST "$BASE/v7/insp/templates" -H "$AUTH" -H "$CT" -d "{\"templateName\":\"L1-消防安全专项\",\"description\":\"消防\",\"targetType\":\"PLACE\",\"catalogId\":\"1\"}" | gid)
echo "T5=$T5_ID"

S10_ID=$(curl -s -X POST "$BASE/v7/insp/templates/$T5_ID/sections" -H "$AUTH" -H "$CT" -d "{\"sectionCode\":\"FI\",\"sectionName\":\"消防设施\",\"weight\":60,\"sortOrder\":1}" | gid)
curl -s -X POST "$BASE/v7/insp/sections/$S10_ID/items" -H "$AUTH" -H "$CT" -d "{\"itemCode\":\"FI1\",\"itemName\":\"喷淋\",\"itemType\":\"PASS_FAIL\",\"isScored\":true,\"sortOrder\":1}" > /dev/null
curl -s -X POST "$BASE/v7/insp/sections/$S10_ID/items" -H "$AUTH" -H "$CT" -d "{\"itemCode\":\"FI2\",\"itemName\":\"报警器\",\"itemType\":\"PASS_FAIL\",\"isScored\":true,\"sortOrder\":2}" > /dev/null

S11_ID=$(curl -s -X POST "$BASE/v7/insp/templates/$T5_ID/sections" -H "$AUTH" -H "$CT" -d "{\"sectionCode\":\"EM\",\"sectionName\":\"应急\",\"weight\":40,\"sortOrder\":2}" | gid)
curl -s -X POST "$BASE/v7/insp/sections/$S11_ID/items" -H "$AUTH" -H "$CT" -d "{\"itemCode\":\"EM1\",\"itemName\":\"急救箱\",\"itemType\":\"CHECKLIST\",\"sortOrder\":1}" > /dev/null
curl -s -X POST "$BASE/v7/insp/sections/$S11_ID/items" -H "$AUTH" -H "$CT" -d "{\"itemCode\":\"EM2\",\"itemName\":\"联系表\",\"itemType\":\"FILE_UPLOAD\",\"sortOrder\":2}" > /dev/null
echo "  2 sections, 4 items"

echo ""
echo "=== L2: 常规检查 (refs T1+T2+T3) ==="
T4_ID=$(curl -s -X POST "$BASE/v7/insp/templates" -H "$AUTH" -H "$CT" -d "{\"templateName\":\"L2-常规检查\",\"description\":\"二级:宿舍+纪律+实训室\",\"targetType\":\"ORG\",\"catalogId\":\"4\"}" | gid)
echo "T4=$T4_ID"

S9_ID=$(curl -s -X POST "$BASE/v7/insp/templates/$T4_ID/sections" -H "$AUTH" -H "$CT" -d "{\"sectionCode\":\"OV\",\"sectionName\":\"检查概况\",\"weight\":10,\"sortOrder\":1}" | gid)
curl -s -X POST "$BASE/v7/insp/sections/$S9_ID/items" -H "$AUTH" -H "$CT" -d "{\"itemCode\":\"OV1\",\"itemName\":\"检查日期\",\"itemType\":\"DATE\",\"isRequired\":true,\"sortOrder\":1}" > /dev/null
curl -s -X POST "$BASE/v7/insp/sections/$S9_ID/items" -H "$AUTH" -H "$CT" -d "{\"itemCode\":\"OV2\",\"itemName\":\"备注\",\"itemType\":\"TEXT\",\"sortOrder\":2}" > /dev/null

curl -s -X POST "$BASE/v7/insp/templates/$T4_ID/module-refs" -H "$AUTH" -H "$CT" -d "{\"moduleTemplateId\":\"$T1_ID\",\"sortOrder\":1,\"weight\":40}" > /dev/null
curl -s -X POST "$BASE/v7/insp/templates/$T4_ID/module-refs" -H "$AUTH" -H "$CT" -d "{\"moduleTemplateId\":\"$T2_ID\",\"sortOrder\":2,\"weight\":35}" > /dev/null
curl -s -X POST "$BASE/v7/insp/templates/$T4_ID/module-refs" -H "$AUTH" -H "$CT" -d "{\"moduleTemplateId\":\"$T3_ID\",\"sortOrder\":3,\"weight\":25}" > /dev/null
echo "  1 section + 3 module refs"

echo ""
echo "=== L3: 全面检查 (refs T4+T5) ==="
T6_ID=$(curl -s -X POST "$BASE/v7/insp/templates" -H "$AUTH" -H "$CT" -d "{\"templateName\":\"L3-全面检查\",\"description\":\"三级:常规+消防\",\"targetType\":\"ORG\",\"catalogId\":\"4\"}" | gid)
echo "T6=$T6_ID"

S12_ID=$(curl -s -X POST "$BASE/v7/insp/templates/$T6_ID/sections" -H "$AUTH" -H "$CT" -d "{\"sectionCode\":\"MT\",\"sectionName\":\"检查信息\",\"weight\":5,\"sortOrder\":1}" | gid)
curl -s -X POST "$BASE/v7/insp/sections/$S12_ID/items" -H "$AUTH" -H "$CT" -d "{\"itemCode\":\"MT1\",\"itemName\":\"学期\",\"itemType\":\"TEXT\",\"isRequired\":true,\"sortOrder\":1}" > /dev/null
curl -s -X POST "$BASE/v7/insp/sections/$S12_ID/items" -H "$AUTH" -H "$CT" -d "{\"itemCode\":\"MT2\",\"itemName\":\"带队签名\",\"itemType\":\"SIGNATURE\",\"isRequired\":true,\"sortOrder\":2}" > /dev/null

curl -s -X POST "$BASE/v7/insp/templates/$T6_ID/module-refs" -H "$AUTH" -H "$CT" -d "{\"moduleTemplateId\":\"$T4_ID\",\"sortOrder\":1,\"weight\":70}" > /dev/null
curl -s -X POST "$BASE/v7/insp/templates/$T6_ID/module-refs" -H "$AUTH" -H "$CT" -d "{\"moduleTemplateId\":\"$T5_ID\",\"sortOrder\":2,\"weight\":30}" > /dev/null
echo "  1 section + 2 module refs"

echo ""
echo "=== Duplicate ==="
COPY_ID=$(curl -s -X POST "$BASE/v7/insp/templates/$T1_ID/duplicate" -H "$AUTH" -H "$CT" | gid)
echo "Copy=$COPY_ID"

echo ""
echo "=== Publish ==="
for TID in $T1_ID $T2_ID $T3_ID $T5_ID $T4_ID $T6_ID; do
  CODE=$(curl -s -X POST "$BASE/v7/insp/templates/$TID/publish" -H "$AUTH" -H "$CT" | grep -o '"code":[0-9]*' | cut -d: -f2)
  echo "  $TID -> $CODE"
done

echo ""
echo "====== SUMMARY ======"
echo "L1-宿舍卫生(PLACE,22types): $T1_ID"
echo "L1-纪律检查(ORG):           $T2_ID"
echo "L1-实训室(PLACE):            $T3_ID"
echo "L1-消防安全(PLACE):          $T5_ID"
echo "L2-常规检查(T1+T2+T3):      $T4_ID"
echo "L3-全面检查(T4+T5):         $T6_ID"
echo "副本:                        $COPY_ID"
