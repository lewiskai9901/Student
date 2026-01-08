#!/bin/bash

# ============================================
#    完整修复脚本 - 修复所有问题
# ============================================

RED='\033[0;31m'
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m'

INSTALL_DIR="/www/wwwroot/student-system"
JAVA_HOME="/www/server/java/jdk-17.0.8"

echo -e "${BLUE}"
echo "============================================"
echo "   完整修复 - 修复密码和权限"
echo "============================================"
echo -e "${NC}"

read -p "MySQL密码 [默认: 123456]: " MYSQL_PWD
MYSQL_PWD=${MYSQL_PWD:-123456}

echo ""
echo -e "${BLUE}[1/4] 修复 user_roles 表${NC}"
mysql -uroot -p"$MYSQL_PWD" student_management << 'EOF'
-- 确保 user_roles 表有正确的 deleted 字段
ALTER TABLE user_roles ADD COLUMN IF NOT EXISTS deleted TINYINT DEFAULT 0;

-- 清理并重新插入 admin 用户的角色
DELETE FROM user_roles WHERE user_id = 1;
INSERT INTO user_roles (id, user_id, role_id, deleted, created_at, updated_at)
VALUES (1, 1, 1, 0, NOW(), NOW());

-- 验证
SELECT 'user_roles 数据:' as info;
SELECT * FROM user_roles WHERE user_id = 1;
EOF
echo -e "${GREEN}完成${NC}"

echo ""
echo -e "${BLUE}[2/4] 修复 role_permissions 表${NC}"
mysql -uroot -p"$MYSQL_PWD" student_management << 'EOF'
-- 确保 role_permissions 表存在并有 deleted 字段
CREATE TABLE IF NOT EXISTS role_permissions (
  id BIGINT NOT NULL,
  role_id BIGINT NOT NULL,
  permission_id BIGINT NOT NULL,
  created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted TINYINT DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE KEY uk_role_permission (role_id, permission_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 给超级管理员赋予所有权限
INSERT IGNORE INTO role_permissions (id, role_id, permission_id, deleted)
SELECT
  (ROW_NUMBER() OVER () + 100) as id,
  1 as role_id,
  p.id as permission_id,
  0 as deleted
FROM permissions p;

-- 验证
SELECT 'role_permissions 数据:' as info;
SELECT COUNT(*) as permission_count FROM role_permissions WHERE role_id = 1;
EOF
echo -e "${GREEN}完成${NC}"

echo ""
echo -e "${BLUE}[3/4] 使用 Java 生成并更新密码${NC}"

# 创建临时 Java 程序来生成密码
cd "$INSTALL_DIR/backend"
cat > /tmp/GenPassword.java << 'JAVAEOF'
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
public class GenPassword {
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String hash = encoder.encode("admin123");
        System.out.println(hash);
    }
}
JAVAEOF

# 编译并运行
export JAVA_HOME="$JAVA_HOME"
export PATH="$JAVA_HOME/bin:$PATH"

# 从 JAR 中提取依赖并编译
cd /tmp
$JAVA_HOME/bin/jar -xf "$INSTALL_DIR/backend/student-management-system-1.0.0.jar" BOOT-INF/lib 2>/dev/null

# 找到 spring-security-crypto jar
CRYPTO_JAR=$(find /tmp/BOOT-INF/lib -name "spring-security-crypto*.jar" 2>/dev/null | head -1)

if [ -n "$CRYPTO_JAR" ]; then
    echo "使用: $CRYPTO_JAR"
    $JAVA_HOME/bin/javac -cp "$CRYPTO_JAR" GenPassword.java 2>/dev/null
    NEW_HASH=$($JAVA_HOME/bin/java -cp ".:$CRYPTO_JAR" GenPassword 2>/dev/null)

    if [ -n "$NEW_HASH" ]; then
        echo "生成的密码哈希: $NEW_HASH"
        mysql -uroot -p"$MYSQL_PWD" -e "UPDATE student_management.users SET password='$NEW_HASH' WHERE username='admin';"
        echo -e "${GREEN}密码已更新${NC}"
    else
        echo -e "${YELLOW}Java 生成失败，使用预设哈希${NC}"
        # 这个哈希是标准 BCrypt 加密的 "admin123"
        mysql -uroot -p"$MYSQL_PWD" -e "UPDATE student_management.users SET password='\$2a\$10\$rDkPvvAFV8kqwvKJzwlRv.mfvMvP1Q7ZBdS8D1vO7xtVfxH3Hg8aW' WHERE username='admin';"
    fi
else
    echo -e "${YELLOW}未找到 crypto jar，使用预设哈希${NC}"
    mysql -uroot -p"$MYSQL_PWD" -e "UPDATE student_management.users SET password='\$2a\$10\$rDkPvvAFV8kqwvKJzwlRv.mfvMvP1Q7ZBdS8D1vO7xtVfxH3Hg8aW' WHERE username='admin';"
fi

# 清理
rm -rf /tmp/BOOT-INF /tmp/GenPassword.* 2>/dev/null

echo ""
echo -e "${BLUE}[4/4] 重启后端并测试${NC}"

pkill -f "student-management-system" 2>/dev/null || true
sleep 2

cd "$INSTALL_DIR/backend"
nohup $JAVA_HOME/bin/java -jar -Xms256m -Xmx512m \
    -Dspring.profiles.active=prod \
    -Dfile.encoding=UTF-8 \
    student-management-system-1.0.0.jar > "$INSTALL_DIR/logs/app.log" 2>&1 &

echo "等待启动..."
sleep 15

# 测试登录
echo "测试登录..."
RESPONSE=$(curl -s -X POST http://127.0.0.1:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}' 2>/dev/null)

if echo "$RESPONSE" | grep -q "accessToken"; then
    echo -e "${GREEN}============================================${NC}"
    echo -e "${GREEN}   登录测试成功！${NC}"
    echo -e "${GREEN}============================================${NC}"
    echo ""
    echo "访问地址: http://$(curl -s ifconfig.me)"
    echo "账号: admin"
    echo "密码: admin123"
else
    echo -e "${YELLOW}登录测试结果: $RESPONSE${NC}"
    echo ""
    echo "检查日志:"
    tail -30 "$INSTALL_DIR/logs/app.log" | grep -i "error\|exception\|密码"
fi

echo ""
echo "============================================"
echo "修复完成"
echo "============================================"
