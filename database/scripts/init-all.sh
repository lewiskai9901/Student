#!/usr/bin/env bash
#
# 新库完整初始化 — baseline + 全部 migrations + seed.
#
# 用法:
#   DB_PASSWORD=your-pw ./database/scripts/init-all.sh
#   或省略密码, 脚本会提示 (mysql -p 交互)
#
# 流程:
#   1. CREATE DATABASE student_management
#   2. baseline: database/schema/complete_schema_v2.sql (V2 baseline, 88 张历史表)
#   3. migrations: database/schema/V*.sql (V3-V104, 语义版本号)
#   4. migrations: database/migrations/V*.sql (V20260419+, 日期号, 插件化基础设施 + V3 基础模块)
#   5. init/init_data.sql seed (admin 账号/默认角色)
#
# 幂等性: baseline 不幂等 (首次跑才对), migrations 已全部 information_schema 条件化, 可重复执行.
#
# 注: 项目未用 Flyway, migration 顺序靠文件名自然排序.
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

echo "[init-all] 2. baseline: database/schema/complete_schema_v2.sql"
$MYSQL_CMD "${DB_NAME}" < "${SCRIPT_DIR}/database/schema/complete_schema_v2.sql"

echo "[init-all] 3. migrations: database/schema/V*.sql (语义版本)"
for f in $(ls "${SCRIPT_DIR}/database/schema/V"*.sql 2>/dev/null | sort -V); do
  # 跳过 baseline (已处理)
  [[ "$f" == *complete_schema_v2.sql ]] && continue
  echo "   apply $(basename "$f")"
  $MYSQL_CMD "${DB_NAME}" < "$f" || { echo "[init-all] FAILED at $f"; exit 1; }
done

echo "[init-all] 4. migrations: database/migrations/V*.sql (日期版本)"
# 必须 sort -V (语义版本排序): 纯 sort 会把 V10/V20260* 排到 V4/V8/V9 之前 (字典序 '1'<'4'),
# 导致依赖错位 (如 V20260419 在其依赖 V8.2.0 之前跑)。
for f in $(ls "${SCRIPT_DIR}/database/migrations/V"*.sql 2>/dev/null | sort -V); do
  echo "   apply $(basename "$f")"
  $MYSQL_CMD "${DB_NAME}" < "$f" || { echo "[init-all] FAILED at $f"; exit 1; }
done

echo "[init-all] 5. seed: database/init/*.sql (按文件名顺序, 跳过 verify_*)"
for f in $(ls "${SCRIPT_DIR}/database/init/"*.sql 2>/dev/null | sort); do
  base=$(basename "$f")
  [[ "$base" == verify_* ]] && { echo "   skip $base"; continue; }
  echo "   apply $base"
  $MYSQL_CMD "${DB_NAME}" < "$f" || { echo "[init-all] FAILED at $base"; exit 1; }
done

echo "[init-all] DONE"
