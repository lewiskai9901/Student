#!/bin/bash

# 多角色测试脚本
# 测试流程: 执行人接收 -> 提交 -> 系主任审批 -> 校领导终审

BASE_URL="http://localhost:8080/api"
TASK_ID="2005644246895316993"

# 颜色输出
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}   多角色任务流程测试${NC}"
echo -e "${BLUE}========================================${NC}"

# 函数: 登录获取token
login() {
    local username=$1
    local password=${2:-"123456"}
    local response=$(curl -s -X POST "$BASE_URL/auth/login" \
        -H "Content-Type: application/json" \
        -d "{\"username\":\"$username\",\"password\":\"$password\"}")
    echo "$response" | grep -o '"accessToken":"[^"]*' | cut -d'"' -f4
}

# 1. 执行人(王老师)登录并查看我的任务
echo -e "\n${YELLOW}【1】执行人(王老师)登录${NC}"
TEACHER_TOKEN=$(login "teacher_wang")
if [ -z "$TEACHER_TOKEN" ]; then
    echo "王老师登录失败，尝试使用admin123密码"
    TEACHER_TOKEN=$(login "teacher_wang" "admin123")
fi
echo "Token: ${TEACHER_TOKEN:0:30}..."

echo -e "\n${YELLOW}【2】王老师查看我的任务${NC}"
curl -s -X GET "$BASE_URL/task/tasks/my?pageNum=1&pageSize=10" \
    -H "Authorization: Bearer $TEACHER_TOKEN" | head -c 500
echo ""

# 2. 接收任务
echo -e "\n${YELLOW}【3】王老师接收任务${NC}"
ACCEPT_RESULT=$(curl -s -X POST "$BASE_URL/task/tasks/$TASK_ID/accept" \
    -H "Authorization: Bearer $TEACHER_TOKEN")
echo "$ACCEPT_RESULT"

# 3. 提交任务 - 使用正确的API路径
echo -e "\n${YELLOW}【4】王老师提交任务${NC}"
SUBMIT_RESULT=$(curl -s -X POST "$BASE_URL/task/tasks/submit" \
    -H "Authorization: Bearer $TEACHER_TOKEN" \
    -H "Content-Type: application/json" \
    -d "{\"taskId\":\"$TASK_ID\",\"content\":\"已完成班会召开，共有30名学生参加\"}")
echo "$SUBMIT_RESULT"

# 4. 查看任务详情(卡片式)
echo -e "\n${YELLOW}【5】查看任务详情(卡片式数据)${NC}"
DETAIL=$(curl -s -X GET "$BASE_URL/task/tasks/$TASK_ID/detail" \
    -H "Authorization: Bearer $TEACHER_TOKEN")
echo "$DETAIL" | head -c 1000
echo ""

# 从详情中提取submissionId
SUBMISSION_ID=$(echo "$DETAIL" | grep -o '"submissionId":[0-9]*' | head -1 | cut -d':' -f2)
echo -e "\n提取到的submissionId: $SUBMISSION_ID"

# 5. 系主任登录
echo -e "\n${YELLOW}【6】系主任(刘主任)登录${NC}"
DIRECTOR_TOKEN=$(login "dept_director" "admin123")
echo "Token: ${DIRECTOR_TOKEN:0:30}..."

# 6. 系主任查看待审批
echo -e "\n${YELLOW}【7】系主任查看待审批任务${NC}"
curl -s -X GET "$BASE_URL/task/tasks/pending-approval?pageNum=1&pageSize=10" \
    -H "Authorization: Bearer $DIRECTOR_TOKEN" | head -c 500
echo ""

# 7. 系主任审批通过 - 使用正确的API路径
echo -e "\n${YELLOW}【8】系主任审批通过${NC}"
if [ -n "$SUBMISSION_ID" ]; then
    APPROVE_RESULT=$(curl -s -X POST "$BASE_URL/task/tasks/approve" \
        -H "Authorization: Bearer $DIRECTOR_TOKEN" \
        -H "Content-Type: application/json" \
        -d "{\"taskId\":\"$TASK_ID\",\"submissionId\":$SUBMISSION_ID,\"action\":1,\"comment\":\"材料齐全，同意通过\"}")
    echo "$APPROVE_RESULT"
else
    echo -e "${RED}无法获取submissionId，跳过审批${NC}"
fi

# 8. 校领导登录
echo -e "\n${YELLOW}【9】校领导(李副校长)登录${NC}"
LEADER_TOKEN=$(login "school_leader" "admin123")
echo "Token: ${LEADER_TOKEN:0:30}..."

# 9. 校领导查看待审批
echo -e "\n${YELLOW}【10】校领导查看待审批任务${NC}"
curl -s -X GET "$BASE_URL/task/tasks/pending-approval?pageNum=1&pageSize=10" \
    -H "Authorization: Bearer $LEADER_TOKEN" | head -c 500
echo ""

# 10. 校领导终审通过
echo -e "\n${YELLOW}【11】校领导终审通过${NC}"
if [ -n "$SUBMISSION_ID" ]; then
    FINAL_APPROVE=$(curl -s -X POST "$BASE_URL/task/tasks/approve" \
        -H "Authorization: Bearer $LEADER_TOKEN" \
        -H "Content-Type: application/json" \
        -d "{\"taskId\":\"$TASK_ID\",\"submissionId\":$SUBMISSION_ID,\"action\":1,\"comment\":\"审批通过，工作完成良好\"}")
    echo "$FINAL_APPROVE"
else
    echo -e "${RED}无法获取submissionId，跳过终审${NC}"
fi

# 11. 最终查看任务详情
echo -e "\n${YELLOW}【12】最终任务详情${NC}"
curl -s -X GET "$BASE_URL/task/tasks/$TASK_ID/detail" \
    -H "Authorization: Bearer $TEACHER_TOKEN"
echo ""

echo -e "\n${GREEN}========================================${NC}"
echo -e "${GREEN}   测试完成${NC}"
echo -e "${GREEN}========================================${NC}"
