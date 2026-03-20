#!/bin/bash
TOKEN=$(curl -s http://localhost:8080/api/auth/login -H "Content-Type: application/json" -d '{"username":"admin","password":"admin123"}' | grep -o '"accessToken":"[^"]*"' | cut -d'"' -f4)
API="http://localhost:8080/api/v7/insp"
H1="Authorization: Bearer $TOKEN"
H2="Content-Type: application/json"

# Clean existing template data
mysql -u root -p123456 student_management -e "SET FOREIGN_KEY_CHECKS=0; TRUNCATE insp_templates; TRUNCATE insp_template_sections; TRUNCATE insp_template_items; TRUNCATE insp_template_versions; SET FOREIGN_KEY_CHECKS=1;" 2>/dev/null

echo "=== Create Template ==="
R=$(curl -s -X POST "$API/templates" -H "$H1" -H "$H2" \
  -d '{"templateName":"Daily Dormitory Inspection","description":"Comprehensive daily check covering hygiene, discipline, safety, and facilities in student dormitories","catalogId":2,"tags":"dormitory,daily,hygiene"}')
TPL=$(echo $R | grep -o '"id":"[^"]*"' | head -1 | cut -d'"' -f4)
echo "Template ID=$TPL"

echo "=== Create Sections ==="
R=$(curl -s -X POST "$API/templates/$TPL/sections" -H "$H1" -H "$H2" -d '{"sectionName":"Hygiene and Cleanliness","description":"Room cleanliness, bathroom, and common area hygiene","sortOrder":1}')
SEC1=$(echo $R | grep -o '"id":"[^"]*"' | head -1 | cut -d'"' -f4)
echo "SEC1=$SEC1 Hygiene"

R=$(curl -s -X POST "$API/templates/$TPL/sections" -H "$H1" -H "$H2" -d '{"sectionName":"Discipline and Order","description":"Bed making, personal belongings, lights-out compliance","sortOrder":2}')
SEC2=$(echo $R | grep -o '"id":"[^"]*"' | head -1 | cut -d'"' -f4)
echo "SEC2=$SEC2 Discipline"

R=$(curl -s -X POST "$API/templates/$TPL/sections" -H "$H1" -H "$H2" -d '{"sectionName":"Safety and Security","description":"Fire safety, electrical safety, emergency exits","sortOrder":3}')
SEC3=$(echo $R | grep -o '"id":"[^"]*"' | head -1 | cut -d'"' -f4)
echo "SEC3=$SEC3 Safety"

R=$(curl -s -X POST "$API/templates/$TPL/sections" -H "$H1" -H "$H2" -d '{"sectionName":"Facilities and Maintenance","description":"Furniture, plumbing, electrical outlets, windows","sortOrder":4}')
SEC4=$(echo $R | grep -o '"id":"[^"]*"' | head -1 | cut -d'"' -f4)
echo "SEC4=$SEC4 Facilities"

echo "=== Section 1: Hygiene Items ==="
curl -s -X POST "$API/sections/$SEC1/items" -H "$H1" -H "$H2" -d '{"itemCode":"HYG-001","itemName":"Floor Cleanliness","itemType":"SELECT","description":"Check if floors are swept, mopped and free of debris","isRequired":true,"responseSetId":2,"scoringMode":"RESPONSE_SCORE","maxScore":10,"sortOrder":1}' > /dev/null && echo "  HYG-001 Floor Cleanliness"
curl -s -X POST "$API/sections/$SEC1/items" -H "$H1" -H "$H2" -d '{"itemCode":"HYG-002","itemName":"Bathroom Hygiene","itemType":"SELECT","description":"Check toilet, sink, shower area cleanliness","isRequired":true,"responseSetId":2,"scoringMode":"RESPONSE_SCORE","maxScore":10,"sortOrder":2}' > /dev/null && echo "  HYG-002 Bathroom Hygiene"
curl -s -X POST "$API/sections/$SEC1/items" -H "$H1" -H "$H2" -d '{"itemCode":"HYG-003","itemName":"Trash Disposal","itemType":"SELECT","description":"Trash bins emptied and properly sorted","isRequired":true,"responseSetId":1,"scoringMode":"RESPONSE_SCORE","maxScore":10,"sortOrder":3}' > /dev/null && echo "  HYG-003 Trash Disposal"
curl -s -X POST "$API/sections/$SEC1/items" -H "$H1" -H "$H2" -d '{"itemCode":"HYG-004","itemName":"Ventilation","itemType":"SELECT","description":"Windows opened for ventilation, no bad odor","isRequired":true,"responseSetId":1,"scoringMode":"RESPONSE_SCORE","maxScore":10,"sortOrder":4}' > /dev/null && echo "  HYG-004 Ventilation"
curl -s -X POST "$API/sections/$SEC1/items" -H "$H1" -H "$H2" -d '{"itemCode":"HYG-005","itemName":"Hygiene Photo Evidence","itemType":"PHOTO","description":"Take a photo of the overall room condition","isRequired":false,"sortOrder":5}' > /dev/null && echo "  HYG-005 Photo Evidence"

echo "=== Section 2: Discipline Items ==="
curl -s -X POST "$API/sections/$SEC2/items" -H "$H1" -H "$H2" -d '{"itemCode":"DIS-001","itemName":"Bed Making","itemType":"SELECT","description":"Beds properly made with folded blankets","isRequired":true,"responseSetId":2,"scoringMode":"RESPONSE_SCORE","maxScore":10,"sortOrder":1}' > /dev/null && echo "  DIS-001 Bed Making"
curl -s -X POST "$API/sections/$SEC2/items" -H "$H1" -H "$H2" -d '{"itemCode":"DIS-002","itemName":"Personal Belongings","itemType":"SELECT","description":"Items neatly organized on shelves and desks","isRequired":true,"responseSetId":2,"scoringMode":"RESPONSE_SCORE","maxScore":10,"sortOrder":2}' > /dev/null && echo "  DIS-002 Personal Belongings"
curl -s -X POST "$API/sections/$SEC2/items" -H "$H1" -H "$H2" -d '{"itemCode":"DIS-003","itemName":"Prohibited Items Check","itemType":"SELECT","description":"No unauthorized electrical appliances or smoking materials","isRequired":true,"responseSetId":1,"scoringMode":"RESPONSE_SCORE","maxScore":10,"sortOrder":3}' > /dev/null && echo "  DIS-003 Prohibited Items"
curl -s -X POST "$API/sections/$SEC2/items" -H "$H1" -H "$H2" -d '{"itemCode":"DIS-004","itemName":"Occupancy Count","itemType":"NUMBER","description":"Number of students present in room","isRequired":false,"sortOrder":4}' > /dev/null && echo "  DIS-004 Occupancy Count"

echo "=== Section 3: Safety Items ==="
curl -s -X POST "$API/sections/$SEC3/items" -H "$H1" -H "$H2" -d '{"itemCode":"SAF-001","itemName":"Fire Extinguisher","itemType":"SELECT","description":"Fire extinguisher present, accessible, and within expiry date","isRequired":true,"responseSetId":3,"scoringMode":"RESPONSE_SCORE","maxScore":10,"sortOrder":1}' > /dev/null && echo "  SAF-001 Fire Extinguisher"
curl -s -X POST "$API/sections/$SEC3/items" -H "$H1" -H "$H2" -d '{"itemCode":"SAF-002","itemName":"Emergency Exit","itemType":"SELECT","description":"Emergency exit signs visible, pathways unobstructed","isRequired":true,"responseSetId":3,"scoringMode":"RESPONSE_SCORE","maxScore":10,"sortOrder":2}' > /dev/null && echo "  SAF-002 Emergency Exit"
curl -s -X POST "$API/sections/$SEC3/items" -H "$H1" -H "$H2" -d '{"itemCode":"SAF-003","itemName":"Electrical Safety","itemType":"SELECT","description":"No exposed wires, overloaded outlets, or damaged cords","isRequired":true,"responseSetId":3,"scoringMode":"RESPONSE_SCORE","maxScore":10,"sortOrder":3}' > /dev/null && echo "  SAF-003 Electrical Safety"
curl -s -X POST "$API/sections/$SEC3/items" -H "$H1" -H "$H2" -d '{"itemCode":"SAF-004","itemName":"Safety Issue Notes","itemType":"TEXTAREA","description":"Describe any safety concerns found","isRequired":false,"sortOrder":4}' > /dev/null && echo "  SAF-004 Safety Notes"

echo "=== Section 4: Facilities Items ==="
curl -s -X POST "$API/sections/$SEC4/items" -H "$H1" -H "$H2" -d '{"itemCode":"FAC-001","itemName":"Furniture Condition","itemType":"SELECT","description":"Beds, desks, chairs, wardrobes in good condition","isRequired":true,"responseSetId":2,"scoringMode":"RESPONSE_SCORE","maxScore":10,"sortOrder":1}' > /dev/null && echo "  FAC-001 Furniture Condition"
curl -s -X POST "$API/sections/$SEC4/items" -H "$H1" -H "$H2" -d '{"itemCode":"FAC-002","itemName":"Plumbing","itemType":"SELECT","description":"No leaks, drains working properly, water pressure adequate","isRequired":true,"responseSetId":3,"scoringMode":"RESPONSE_SCORE","maxScore":10,"sortOrder":2}' > /dev/null && echo "  FAC-002 Plumbing"
curl -s -X POST "$API/sections/$SEC4/items" -H "$H1" -H "$H2" -d '{"itemCode":"FAC-003","itemName":"Lighting","itemType":"SELECT","description":"All lights functional, adequate brightness","isRequired":true,"responseSetId":1,"scoringMode":"RESPONSE_SCORE","maxScore":10,"sortOrder":3}' > /dev/null && echo "  FAC-003 Lighting"
curl -s -X POST "$API/sections/$SEC4/items" -H "$H1" -H "$H2" -d '{"itemCode":"FAC-004","itemName":"Maintenance Request","itemType":"TEXTAREA","description":"Note any items requiring maintenance or repair","isRequired":false,"sortOrder":4}' > /dev/null && echo "  FAC-004 Maintenance Request"

echo ""
echo "=== Publish Template ==="
R=$(curl -s -X POST "$API/templates/$TPL/publish" -H "$H1" -H "$H2")
echo "Publish result: $(echo $R | head -c 200)"

echo ""
echo "=== SUMMARY ==="
echo "Template: $TPL (Daily Dormitory Inspection)"
echo "Sections: $SEC1 (Hygiene), $SEC2 (Discipline), $SEC3 (Safety), $SEC4 (Facilities)"
echo "Items: 17 total (13 scored + 2 text + 1 number + 1 photo)"
echo "Response Sets: 1 (Pass/Fail), 2 (Condition Rating), 3 (Compliance Level)"
echo "Catalogs: 1 (School) -> 2 (Dorm), 3 (Classroom), 4 (Safety)"
