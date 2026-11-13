#!/bin/bash
set -euo pipefail
# 首次初始化库结构：执行顺序与 docs/端到端联调与测试手册 中的 SQL 依赖顺序保持一致
DB="${MYSQL_DATABASE:-smart_agriculture}"
SQL_DIR="/init-sql"

run_sql() {
  local f="$1"
  echo "[mysql-init] applying $f"
  mysql --protocol=socket -uroot -p"${MYSQL_ROOT_PASSWORD}" "${DB}" < "${SQL_DIR}/${f}"
}

run_sql init_admin.sql
run_sql init_field.sql
run_sql init_crop.sql
run_sql init_device.sql
run_sql init_environment_data.sql
run_sql init_warning.sql
run_sql migrate_warning_closure_center.sql
run_sql init_warning_rule_config.sql
run_sql init_inspection_task_log.sql
run_sql init_ai_chat_log.sql
run_sql init_real_china_competition_data.sql

echo "[mysql-init] done"
