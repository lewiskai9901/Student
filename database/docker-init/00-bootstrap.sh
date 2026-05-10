#!/bin/bash
# Docker MySQL 容器初始化脚本 — 在 mysqld 第一次启动时运行
# 等价 database/scripts/init-all.sh 但适配容器环境 (无需 mysql 客户端外部参数)
#
# 挂载约定:
#   /sql                                → 项目 database/ (containing schema/, migrations/, init/)
#   /docker-entrypoint-initdb.d/00-bootstrap.sh → 本文件
#
# 由 mysql 官方镜像的 docker-entrypoint.sh 在首次启动时通过 root socket 调起 mysql.
set -e

DB="${MYSQL_DATABASE:-student_management}"
SQLDIR="/sql"

echo "[docker-init] 1. baseline: complete_schema_v2.sql"
mysql "$DB" < "$SQLDIR/schema/complete_schema_v2.sql"

echo "[docker-init] 2. migrations schema/V*.sql"
for f in $(ls "$SQLDIR/schema/V"*.sql 2>/dev/null | sort -V); do
  [[ "$f" == *complete_schema_v2.sql ]] && continue
  echo "   apply $(basename "$f")"
  mysql "$DB" < "$f" || { echo "[docker-init] FAILED at $f"; exit 1; }
done

echo "[docker-init] 3. migrations migrations/V*.sql"
for f in $(ls "$SQLDIR/migrations/V"*.sql 2>/dev/null | sort); do
  echo "   apply $(basename "$f")"
  mysql "$DB" < "$f" || { echo "[docker-init] FAILED at $f"; exit 1; }
done

echo "[docker-init] 4. seed init/*.sql (skip verify_*)"
for f in $(ls "$SQLDIR/init/"*.sql 2>/dev/null | sort); do
  base=$(basename "$f")
  [[ "$base" == verify_* ]] && { echo "   skip $base"; continue; }
  echo "   apply $base"
  mysql "$DB" < "$f" || { echo "[docker-init] FAILED at $base"; exit 1; }
done

echo "[docker-init] DONE"
