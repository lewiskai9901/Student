#!/bin/bash
# V6量化系统角色测试脚本

BASE_URL="http://localhost:8080/api"

# 颜色输出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

# 获取token函数
get_token() {
    local username=$1
    local password=$2
    curl -s -X POST "$BASE_URL/auth/login" \
        -H "Content-Type: application/json" \
        -d "{\"username\":\"$username\",\"password\":\"$password\"}" | jq -r '.data.accessToken'
}

# 测试API函数
test_api() {
    local method=$1
    local url=$2
    local token=$3
    local data=$4
    local desc=$5
    
    echo -e "${YELLOW}测试: $desc${NC}"
    
    if [ -n "$data" ]; then
        response=$(curl -s -w "\n%{http_code}" -X "$method" "$BASE_URL$url" \
            -H "Content-Type: application/json" \
            -H "Authorization: Bearer $token" \
            -d "$data")
    else
        response=$(curl -s -w "\n%{http_code}" -X "$method" "$BASE_URL$url" \
            -H "Content-Type: application/json" \
            -H "Authorization: Bearer $token")
    fi
    
    http_code=$(echo "$response" | tail -n1)
    body=$(echo "$response" | sed '$d')
    
    if [ "$http_code" == "200" ] || [ "$http_code" == "201" ] || [ "$http_code" == "204" ]; then
        echo -e "${GREEN}✓ 成功 (HTTP $http_code)${NC}"
        echo "$body" | jq . 2>/dev/null || echo "$body"
    else
        echo -e "${RED}✗ 失败 (HTTP $http_code)${NC}"
        echo "$body" | jq . 2>/dev/null || echo "$body"
    fi
    echo ""
}

echo "======================================"
echo "V6量化系统角色测试"
echo "======================================"
echo ""

# 1. 超级管理员测试
echo -e "${GREEN}=== 1. 超级管理员 (admin) ===${NC}"
ADMIN_TOKEN=$(get_token "admin" "admin123")
if [ -z "$ADMIN_TOKEN" ] || [ "$ADMIN_TOKEN" == "null" ]; then
    echo -e "${RED}admin登录失败${NC}"
    exit 1
fi
echo "Token获取成功"
echo ""

# 创建项目
echo "--- 1.1 创建V6检查项目 ---"
CREATE_RESPONSE=$(curl -s -X POST "$BASE_URL/v6/inspection-projects" \
    -H "Content-Type: application/json" \
    -H "Authorization: Bearer $ADMIN_TOKEN" \
    -d '{
        "projectName": "2024年秋季宿舍检查项目",
        "description": "每日宿舍卫生和安全检查"
    }')
echo "$CREATE_RESPONSE" | jq .
PROJECT_ID=$(echo "$CREATE_RESPONSE" | jq -r '.id // .data.id // empty')
echo "项目ID: $PROJECT_ID"
echo ""

if [ -n "$PROJECT_ID" ] && [ "$PROJECT_ID" != "null" ]; then
    # 更新项目配置
    echo "--- 1.2 更新项目配置 ---"
    test_api "PUT" "/v6/inspection-projects/$PROJECT_ID/config" "$ADMIN_TOKEN" '{
        "scopeType": "DORMITORY",
        "startDate": "2024-09-01",
        "endDate": "2024-12-31",
        "cycleType": "DAILY",
        "skipHolidays": true,
        "sharedSpaceStrategy": "ROTATE",
        "inspectorAssignmentMode": "MANUAL"
    }' "更新项目配置"

    # 获取项目详情
    echo "--- 1.3 获取项目详情 ---"
    test_api "GET" "/v6/inspection-projects/$PROJECT_ID" "$ADMIN_TOKEN" "" "获取项目详情"

    # 发布项目
    echo "--- 1.4 发布项目 ---"
    test_api "POST" "/v6/inspection-projects/$PROJECT_ID/publish" "$ADMIN_TOKEN" '{}' "发布项目"
fi

# 获取任务列表
echo "--- 1.5 获取任务列表 ---"
test_api "GET" "/v6/inspection-tasks" "$ADMIN_TOKEN" "" "获取任务列表"

# 获取汇总列表
echo "--- 1.6 获取汇总列表 ---"
test_api "GET" "/v6/inspection-summaries" "$ADMIN_TOKEN" "" "获取汇总列表"

echo ""
echo "======================================"
echo "超级管理员测试完成"
echo "======================================"
