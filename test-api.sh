#!/bin/bash

# Login and get token
echo "Logging in..."
LOGIN_RESPONSE=$(curl -s -X POST "http://localhost:8080/api/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}')

# Extract token
TOKEN=$(echo "$LOGIN_RESPONSE" | grep -o '"accessToken":"[^"]*' | cut -d'"' -f4)

if [ -z "$TOKEN" ]; then
  echo "Failed to get token"
  echo "$LOGIN_RESPONSE"
  exit 1
fi

echo "Token: ${TOKEN:0:50}..."

# Create batch task
echo ""
echo "Creating batch task..."
TASK_RESPONSE=$(curl -s -X POST "http://localhost:8080/api/task/tasks" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Test Card Display Task",
    "description": "Test batch task for card-style display",
    "priority": 2,
    "assignType": 2,
    "targetIds": [1101, 1102, 1103, 1104],
    "workflowTemplateId": 2,
    "approvalConfigs": [
      {
        "departmentId": 100,
        "departmentName": "Info Engineering",
        "levels": [
          {"level": 1, "approverId": 2001, "approverName": "Director Liu", "approverRole": "Director"},
          {"level": 2, "approverId": 3001, "approverName": "VP Li", "approverRole": "VP"}
        ]
      }
    ]
  }')

echo "$TASK_RESPONSE"

# Extract task ID
TASK_ID=$(echo "$TASK_RESPONSE" | grep -o '"id":"[^"]*' | head -1 | cut -d'"' -f4)

if [ -n "$TASK_ID" ]; then
  echo ""
  echo "Task created with ID: $TASK_ID"
  echo ""
  echo "Getting task detail..."

  # Get task detail using the card-style API
  DETAIL_RESPONSE=$(curl -s -X GET "http://localhost:8080/api/task/tasks/$TASK_ID/detail" \
    -H "Authorization: Bearer $TOKEN")

  echo "$DETAIL_RESPONSE"
fi
