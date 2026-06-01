#!/usr/bin/env bash
#
# 新库完整初始化 — 加载 squash 后的权威 baseline (V3, 2026-06-01).
#
# 用法:
#   DB_PASSWORD=your-pw ./database/scripts/init-all.sh
#   或省略密码, 脚本会提示 (mysql -p 交互)
#
# 流程:
#   1. CREATE DATABASE student_management
#   2. 加载 database/schema/baseline_v3.sql
#      —— 这是 2026-06-01 把整条历史迁移链 squash 出来的权威快照:
#         完整 schema (333 表/视图, 含归属统一重构 + 插件基础设施 + 检查平台)
#         + bootstrap 种子数据 (admin/admin123 超管 + 默认角色权限 + 类型/关系配置 + data_modules)。
#      —— 一次加载即得到"后端可直接启动并登录"的库, 经真实启动 + 冒烟 200 验证。
#   3. (将来) 应用 database/migrations/post-v3/V*.sql —— baseline_v3 之后的新增量, 目前为空。
#
# 为什么不再逐个回放 database/schema/V*.sql + database/migrations/V*.sql ?
#   那条历史链无法从 baseline 干净重放 (非时间序版本号 + 跨文件矛盾 + 大量线上 ad-hoc
#   未回写的 DDL), 是项目长期预存债。2026-06-01 已用"应用代码 115 张表为标准 + 真实启动验证"
#   重建出正确 schema 并 squash 为 baseline_v3。旧 V*.sql 文件保留作历史记录, 不再参与 init。
#   详见 database/schema/README_baseline_v3.md。
#
set -e

DB_NAME="${DB_NAME:-student_management}"
DB_USER="${DB_USER:-root}"
DB_PW="${DB_PASSWORD:-}"
SCRIPT_DIR="$(cd "$(dirname "$0")/../.." && pwd)"

# --default-character-set=utf8mb4 必须显式指定: 否则 mysql 客户端按本机默认连接字符集
# (Windows 常为 gbk/latin1) 读取 piped SQL 文件, 含中文的 INSERT 会报
# ERROR 1366 Incorrect string value / Illegal mix of collations gbk_chinese_ci。
MYSQL_CMD="mysql --default-character-set=utf8mb4 -u ${DB_USER}"
[ -n "$DB_PW" ] && MYSQL_CMD="${MYSQL_CMD} -p${DB_PW}"

echo "[init-all] 1. CREATE DATABASE ${DB_NAME}"
$MYSQL_CMD -e "CREATE DATABASE IF NOT EXISTS ${DB_NAME} CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"

echo "[init-all] 2. 加载权威 baseline: database/schema/baseline_v3.sql"
$MYSQL_CMD "${DB_NAME}" < "${SCRIPT_DIR}/database/schema/baseline_v3.sql"

echo "[init-all] 3. 应用 baseline_v3 之后的增量迁移 (database/migrations/post-v3/V*.sql)"
POST_DIR="${SCRIPT_DIR}/database/migrations/post-v3"
if compgen -G "${POST_DIR}/V"*.sql > /dev/null 2>&1; then
  for f in $(ls "${POST_DIR}/V"*.sql 2>/dev/null | sort -V); do
    echo "   apply $(basename "$f")"
    $MYSQL_CMD "${DB_NAME}" < "$f" || { echo "[init-all] FAILED at $f"; exit 1; }
  done
else
  echo "   (无增量迁移)"
fi

echo "[init-all] DONE — 默认账号 admin / admin123"
