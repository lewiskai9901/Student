#!/bin/bash
BASE="http://localhost:8080/api"
TOKEN=$(curl -s -X POST "$BASE/auth/login" -H 'Content-Type: application/json' -d '{"username":"admin","password":"admin123"}' | grep -o '"accessToken":"[^"]*"' | cut -d'"' -f4)
H1="Authorization: Bearer $TOKEN"
H2="Content-Type: application/json"

post() { curl -s -X POST "$BASE$1" -H "$H1" -H "$H2" -d "$2"; }
get_id() { echo "$1" | grep -o '"id":"[^"]*"' | head -1 | cut -d'"' -f4; }

echo "=== 1. 宿舍卫生检查 (PLACE) ==="
T1=$(post "/v7/insp/templates" '{"templateName":"宿舍卫生检查","description":"场所级检查,覆盖卫生安全设施","targetType":"PLACE","catalogId":"1"}')
T1_ID=$(get_id "$T1"); echo "T1=$T1_ID"

S1=$(post "/v7/insp/templates/$T1_ID/sections" '{"sectionCode":"ROOM_HYG","sectionName":"房间卫生","weight":40,"sortOrder":1}')
S1_ID=$(get_id "$S1")
post "/v7/insp/sections/$S1_ID/items" '{"itemCode":"FLOOR","itemName":"地面清洁度","itemType":"PASS_FAIL","isRequired":true,"isScored":true,"itemWeight":1.0,"sortOrder":1}' > /dev/null
post "/v7/insp/sections/$S1_ID/items" '{"itemCode":"DESK","itemName":"桌面整理","itemType":"RATING","isRequired":true,"isScored":true,"itemWeight":1.0,"sortOrder":2}' > /dev/null
post "/v7/insp/sections/$S1_ID/items" '{"itemCode":"BED","itemName":"床铺整理","itemType":"RADIO","isRequired":true,"isScored":true,"sortOrder":3}' > /dev/null
post "/v7/insp/sections/$S1_ID/items" '{"itemCode":"TRASH","itemName":"垃圾桶清理","itemType":"CHECKBOX","sortOrder":4}' > /dev/null
post "/v7/insp/sections/$S1_ID/items" '{"itemCode":"HYG_SCORE","itemName":"卫生评分","itemType":"SLIDER","isRequired":true,"isScored":true,"itemWeight":2.0,"sortOrder":5}' > /dev/null
post "/v7/insp/sections/$S1_ID/items" '{"itemCode":"HYG_NOTE","itemName":"卫生备注","itemType":"TEXTAREA","sortOrder":6}' > /dev/null
post "/v7/insp/sections/$S1_ID/items" '{"itemCode":"HYG_PHOTO","itemName":"现场照片","itemType":"PHOTO","requireEvidence":true,"sortOrder":7}' > /dev/null
echo "  房间卫生: 7 items"

S2=$(post "/v7/insp/templates/$T1_ID/sections" '{"sectionCode":"SAFETY","sectionName":"安全检查","weight":35,"sortOrder":2}')
S2_ID=$(get_id "$S2")
post "/v7/insp/sections/$S2_ID/items" '{"itemCode":"FIRE_EXT","itemName":"灭火器有效期","itemType":"DATE","isRequired":true,"sortOrder":1}' > /dev/null
post "/v7/insp/sections/$S2_ID/items" '{"itemCode":"WIRE","itemName":"电线安全","itemType":"SELECT","isRequired":true,"isScored":true,"sortOrder":2}' > /dev/null
post "/v7/insp/sections/$S2_ID/items" '{"itemCode":"EXIT","itemName":"消防通道","itemType":"PASS_FAIL","isRequired":true,"isScored":true,"sortOrder":3}' > /dev/null
post "/v7/insp/sections/$S2_ID/items" '{"itemCode":"HAZARD","itemName":"安全隐患","itemType":"MULTI_SELECT","sortOrder":4}' > /dev/null
post "/v7/insp/sections/$S2_ID/items" '{"itemCode":"GPS_LOC","itemName":"检查位置","itemType":"GPS","isRequired":true,"sortOrder":5}' > /dev/null
post "/v7/insp/sections/$S2_ID/items" '{"itemCode":"SAFE_VIDEO","itemName":"隐患视频","itemType":"VIDEO","requireEvidence":true,"sortOrder":6}' > /dev/null
echo "  安全检查: 6 items"

S3=$(post "/v7/insp/templates/$T1_ID/sections" '{"sectionCode":"FACILITY","sectionName":"设施完好","weight":15,"isRepeatable":true,"sortOrder":3}')
S3_ID=$(get_id "$S3")
post "/v7/insp/sections/$S3_ID/items" '{"itemCode":"DOOR","itemName":"门窗完好度","itemType":"NUMBER","isRequired":true,"isScored":true,"sortOrder":1}' > /dev/null
post "/v7/insp/sections/$S3_ID/items" '{"itemCode":"FURN","itemName":"家具状况","itemType":"CHECKLIST","isRequired":true,"sortOrder":2}' > /dev/null
post "/v7/insp/sections/$S3_ID/items" '{"itemCode":"LIGHT","itemName":"照明设备","itemType":"PASS_FAIL","isScored":true,"sortOrder":3}' > /dev/null
post "/v7/insp/sections/$S3_ID/items" '{"itemCode":"DMG_DESC","itemName":"损坏描述","itemType":"RICH_TEXT","sortOrder":4}' > /dev/null
post "/v7/insp/sections/$S3_ID/items" '{"itemCode":"DMG_FILE","itemName":"维修工单","itemType":"FILE_UPLOAD","sortOrder":5}' > /dev/null
echo "  设施完好(可重复): 5 items"

S4=$(post "/v7/insp/templates/$T1_ID/sections" '{"sectionCode":"OVERALL","sectionName":"综合评价","weight":10,"sortOrder":4}')
S4_ID=$(get_id "$S4")
post "/v7/insp/sections/$S4_ID/items" '{"itemCode":"OVR_SCORE","itemName":"总体评价","itemType":"RATING","isRequired":true,"isScored":true,"sortOrder":1}' > /dev/null
post "/v7/insp/sections/$S4_ID/items" '{"itemCode":"OVR_TIME","itemName":"检查时间","itemType":"DATETIME","isRequired":true,"sortOrder":2}' > /dev/null
post "/v7/insp/sections/$S4_ID/items" '{"itemCode":"OVR_SIGN","itemName":"检查员签名","itemType":"SIGNATURE","isRequired":true,"sortOrder":3}' > /dev/null
post "/v7/insp/sections/$S4_ID/items" '{"itemCode":"BARCODE_SCAN","itemName":"房间条码","itemType":"BARCODE","sortOrder":4}' > /dev/null
post "/v7/insp/sections/$S4_ID/items" '{"itemCode":"CALC_TOTAL","itemName":"自动汇总","itemType":"CALCULATED","sortOrder":5}' > /dev/null
post "/v7/insp/sections/$S4_ID/items" '{"itemCode":"CHECK_DURATION","itemName":"检查用时","itemType":"TIME","sortOrder":6}' > /dev/null
echo "  综合评价: 6 items"
echo "  Total: 4 sections, 24 items, 22 field types covered"

echo ""
echo "=== 2. 纪律检查 (ORG) ==="
T2=$(post "/v7/insp/templates" '{"templateName":"纪律检查","description":"组织级纪律检查","targetType":"ORG","catalogId":"4"}')
T2_ID=$(get_id "$T2"); echo "T2=$T2_ID"

S5=$(post "/v7/insp/templates/$T2_ID/sections" '{"sectionCode":"ATTEND","sectionName":"出勤纪律","weight":50,"sortOrder":1}')
S5_ID=$(get_id "$S5")
post "/v7/insp/sections/$S5_ID/items" '{"itemCode":"LATE","itemName":"迟到人数","itemType":"NUMBER","isRequired":true,"isScored":true,"sortOrder":1}' > /dev/null
post "/v7/insp/sections/$S5_ID/items" '{"itemCode":"ABSENT","itemName":"缺勤人数","itemType":"NUMBER","isRequired":true,"isScored":true,"sortOrder":2}' > /dev/null
post "/v7/insp/sections/$S5_ID/items" '{"itemCode":"UNIFORM","itemName":"着装规范","itemType":"PASS_FAIL","isRequired":true,"isScored":true,"sortOrder":3}' > /dev/null
post "/v7/insp/sections/$S5_ID/items" '{"itemCode":"ATT_PHOTO","itemName":"出勤照片","itemType":"PHOTO","requireEvidence":true,"sortOrder":4}' > /dev/null

S6=$(post "/v7/insp/templates/$T2_ID/sections" '{"sectionCode":"BEHAVIOR","sectionName":"行为规范","weight":50,"sortOrder":2}')
S6_ID=$(get_id "$S6")
post "/v7/insp/sections/$S6_ID/items" '{"itemCode":"ORDER","itemName":"秩序评分","itemType":"RATING","isRequired":true,"isScored":true,"sortOrder":1}' > /dev/null
post "/v7/insp/sections/$S6_ID/items" '{"itemCode":"CLEAN","itemName":"环境整洁","itemType":"RATING","isRequired":true,"isScored":true,"sortOrder":2}' > /dev/null
post "/v7/insp/sections/$S6_ID/items" '{"itemCode":"INCIDENT","itemName":"违规事项","itemType":"TEXTAREA","sortOrder":3}' > /dev/null
post "/v7/insp/sections/$S6_ID/items" '{"itemCode":"VID_EVIDENCE","itemName":"违规证据","itemType":"VIDEO","requireEvidence":true,"sortOrder":4}' > /dev/null
echo "  2 sections, 8 items"

echo ""
echo "=== 3. 实训室检查 (PLACE) ==="
T3=$(post "/v7/insp/templates" '{"templateName":"实训室检查","description":"实训室设备与安全","targetType":"PLACE","catalogId":"1"}')
T3_ID=$(get_id "$T3"); echo "T3=$T3_ID"

S7=$(post "/v7/insp/templates/$T3_ID/sections" '{"sectionCode":"EQUIP","sectionName":"设备检查","weight":60,"sortOrder":1}')
S7_ID=$(get_id "$S7")
post "/v7/insp/sections/$S7_ID/items" '{"itemCode":"EQUIP_ID","itemName":"设备编号","itemType":"BARCODE","isRequired":true,"sortOrder":1}' > /dev/null
post "/v7/insp/sections/$S7_ID/items" '{"itemCode":"EQUIP_STATUS","itemName":"设备状态","itemType":"SELECT","isRequired":true,"isScored":true,"sortOrder":2}' > /dev/null
post "/v7/insp/sections/$S7_ID/items" '{"itemCode":"EQUIP_TEMP","itemName":"设备温度","itemType":"NUMBER","sortOrder":3}' > /dev/null
post "/v7/insp/sections/$S7_ID/items" '{"itemCode":"EQUIP_PHOTO","itemName":"设备照片","itemType":"PHOTO","requireEvidence":true,"sortOrder":4}' > /dev/null

S8=$(post "/v7/insp/templates/$T3_ID/sections" '{"sectionCode":"ENV","sectionName":"环境安全","weight":40,"sortOrder":2}')
S8_ID=$(get_id "$S8")
post "/v7/insp/sections/$S8_ID/items" '{"itemCode":"VENT","itemName":"通风设施","itemType":"PASS_FAIL","isRequired":true,"isScored":true,"sortOrder":1}' > /dev/null
post "/v7/insp/sections/$S8_ID/items" '{"itemCode":"DANGER_SIGN","itemName":"警示标识","itemType":"CHECKLIST","isRequired":true,"sortOrder":2}' > /dev/null
post "/v7/insp/sections/$S8_ID/items" '{"itemCode":"WASTE","itemName":"废弃物处理","itemType":"PASS_FAIL","isScored":true,"sortOrder":3}' > /dev/null
echo "  2 sections, 7 items"

echo ""
echo "=== 4. 消防安全专项 (PLACE) ==="
T5=$(post "/v7/insp/templates" '{"templateName":"消防安全专项","description":"消防安全专项检查","targetType":"PLACE","catalogId":"1"}')
T5_ID=$(get_id "$T5"); echo "T5=$T5_ID"

S10=$(post "/v7/insp/templates/$T5_ID/sections" '{"sectionCode":"FIRE","sectionName":"消防设施","weight":60,"sortOrder":1}')
S10_ID=$(get_id "$S10")
post "/v7/insp/sections/$S10_ID/items" '{"itemCode":"SPRINKLER","itemName":"喷淋系统","itemType":"PASS_FAIL","isRequired":true,"isScored":true,"sortOrder":1}' > /dev/null
post "/v7/insp/sections/$S10_ID/items" '{"itemCode":"ALARM","itemName":"报警器","itemType":"PASS_FAIL","isRequired":true,"isScored":true,"sortOrder":2}' > /dev/null
post "/v7/insp/sections/$S10_ID/items" '{"itemCode":"ESCAPE","itemName":"逃生路线","itemType":"PHOTO","requireEvidence":true,"sortOrder":3}' > /dev/null
post "/v7/insp/sections/$S10_ID/items" '{"itemCode":"DRILL_DATE","itemName":"上次演练","itemType":"DATE","isRequired":true,"sortOrder":4}' > /dev/null

S11=$(post "/v7/insp/templates/$T5_ID/sections" '{"sectionCode":"EMERGENCY","sectionName":"应急准备","weight":40,"sortOrder":2}')
S11_ID=$(get_id "$S11")
post "/v7/insp/sections/$S11_ID/items" '{"itemCode":"FIRST_AID","itemName":"急救箱","itemType":"CHECKLIST","isRequired":true,"sortOrder":1}' > /dev/null
post "/v7/insp/sections/$S11_ID/items" '{"itemCode":"CONTACTS","itemName":"紧急联系表","itemType":"FILE_UPLOAD","isRequired":true,"sortOrder":2}' > /dev/null
echo "  2 sections, 6 items"

echo ""
echo "=== 5. 常规检查 (L2: refs 宿舍+纪律+实训室) ==="
T4=$(post "/v7/insp/templates" '{"templateName":"常规检查","description":"二级组合: 引用宿舍+纪律+实训室","targetType":"ORG","catalogId":"4"}')
T4_ID=$(get_id "$T4"); echo "T4=$T4_ID"

S9=$(post "/v7/insp/templates/$T4_ID/sections" '{"sectionCode":"OVERVIEW","sectionName":"检查概况","weight":10,"sortOrder":1}')
S9_ID=$(get_id "$S9")
post "/v7/insp/sections/$S9_ID/items" '{"itemCode":"CHECK_DATE","itemName":"检查日期","itemType":"DATE","isRequired":true,"sortOrder":1}' > /dev/null
post "/v7/insp/sections/$S9_ID/items" '{"itemCode":"CHECK_PERIOD","itemName":"检查时段","itemType":"SELECT","isRequired":true,"sortOrder":2}' > /dev/null
post "/v7/insp/sections/$S9_ID/items" '{"itemCode":"NOTE","itemName":"备注","itemType":"TEXT","sortOrder":3}' > /dev/null

echo "  Adding module refs..."
post "/v7/insp/templates/$T4_ID/module-refs" "{\"moduleTemplateId\":\"$T1_ID\",\"sortOrder\":1,\"weight\":40}" > /dev/null
post "/v7/insp/templates/$T4_ID/module-refs" "{\"moduleTemplateId\":\"$T2_ID\",\"sortOrder\":2,\"weight\":35}" > /dev/null
post "/v7/insp/templates/$T4_ID/module-refs" "{\"moduleTemplateId\":\"$T3_ID\",\"sortOrder\":3,\"weight\":25}" > /dev/null
echo "  1 section + 3 module refs"

echo ""
echo "=== 6. 全面检查 (L3: refs 常规+消防) ==="
T6=$(post "/v7/insp/templates" '{"templateName":"全面检查","description":"三级组合: 引用常规检查+消防安全","targetType":"ORG","catalogId":"4"}')
T6_ID=$(get_id "$T6"); echo "T6=$T6_ID"

S12=$(post "/v7/insp/templates/$T6_ID/sections" '{"sectionCode":"META","sectionName":"检查信息","weight":5,"sortOrder":1}')
S12_ID=$(get_id "$S12")
post "/v7/insp/sections/$S12_ID/items" '{"itemCode":"SEMESTER","itemName":"学期","itemType":"TEXT","isRequired":true,"sortOrder":1}' > /dev/null
post "/v7/insp/sections/$S12_ID/items" '{"itemCode":"TEAM_LEAD","itemName":"带队人签名","itemType":"SIGNATURE","isRequired":true,"sortOrder":2}' > /dev/null

echo "  Adding module refs (3-level)..."
post "/v7/insp/templates/$T6_ID/module-refs" "{\"moduleTemplateId\":\"$T4_ID\",\"sortOrder\":1,\"weight\":70}" > /dev/null
post "/v7/insp/templates/$T6_ID/module-refs" "{\"moduleTemplateId\":\"$T5_ID\",\"sortOrder\":2,\"weight\":30}" > /dev/null
echo "  1 section + 2 module refs"

echo ""
echo "=== 7. 测试复制 ==="
COPY=$(post "/v7/insp/templates/$T1_ID/duplicate")
COPY_ID=$(get_id "$COPY"); echo "Duplicated T1 -> $COPY_ID"

echo ""
echo "=== 8. 发布所有模板 ==="
for TID in $T1_ID $T2_ID $T3_ID $T5_ID $T4_ID $T6_ID; do
  R=$(post "/v7/insp/templates/$TID/publish")
  STATUS=$(echo "$R" | grep -o '"code":[0-9]*' | cut -d: -f2)
  echo "  $TID -> code=$STATUS"
done

echo ""
echo "========================================="
echo "宿舍卫生检查 (L1,PLACE):  $T1_ID"
echo "纪律检查 (L1,ORG):        $T2_ID"
echo "实训室检查 (L1,PLACE):     $T3_ID"
echo "消防安全专项 (L1,PLACE):   $T5_ID"
echo "常规检查 (L2,组合):        $T4_ID"
echo "全面检查 (L3,组合):        $T6_ID"
echo "宿舍检查副本:              $COPY_ID"
echo "========================================="
