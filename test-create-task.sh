#!/bin/bash
TOKEN="eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiIxIiwiaWF0IjoxNzY3MDE2ODAxLCJleHAiOjE3NjcwMjQwMDEsInVzZXJuYW1lIjoiYWRtaW4iLCJyb2xlcyI6WyJTVVBFUl9BRE1JTiIsIlNDSE9PTF9BRE1JTiJdLCJ0b2tlblR5cGUiOiJhY2Nlc3NfdG9rZW4ifQ.EXal8knruzNdczEbgXvKASH4r75HXFuwN8OhgZ0D6m8x-MuPb1fUGrL3q-fhN3lYpJbnmUWJWdO9TkXj9RjZrw"

curl -X POST "http://localhost:8080/api/task/tasks" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
  "title": "Test Card Display Task",
  "description": "Test batch task for card-style display",
  "priority": 2,
  "assignType": 2,
  "targetIds": [1101, 1102, 1103, 1104, 2000001462, 2000001463, 2000001464, 2000001465],
  "workflowTemplateId": 2,
  "approvalConfigs": [
    {
      "departmentId": 100,
      "departmentName": "Info Engineering",
      "levels": [
        {"level": 1, "approverId": 2001, "approverName": "Director Liu", "approverRole": "Director"},
        {"level": 2, "approverId": 3001, "approverName": "VP Li", "approverRole": "VP"}
      ]
    },
    {
      "departmentId": 200,
      "departmentName": "Business School",
      "levels": [
        {"level": 1, "approverId": 1001, "approverName": "Director Zhang", "approverRole": "Director"},
        {"level": 2, "approverId": 3001, "approverName": "VP Li", "approverRole": "VP"}
      ]
    }
  ]
}'
