#!/bin/bash
DB="smart_agriculture"
SQL_DIR="/init-sql"

mysql --protocol=socket -uroot -proot "${DB}" < "${SQL_DIR}/init_field.sql"
mysql --protocol=socket -uroot -proot "${DB}" < "${SQL_DIR}/init_crop.sql"
mysql --protocol=socket -uroot -proot "${DB}" < "${SQL_DIR}/init_device.sql"
mysql --protocol=socket -uroot -proot "${DB}" < "${SQL_DIR}/init_environment_data.sql"
mysql --protocol=socket -uroot -proot "${DB}" < "${SQL_DIR}/init_warning.sql"
mysql --protocol=socket -uroot -proot "${DB}" < "${SQL_DIR}/migrate_warning_closure_center.sql"
mysql --protocol=socket -uroot -proot "${DB}" < "${SQL_DIR}/init_warning_rule_config.sql"
mysql --protocol=socket -uroot -proot "${DB}" < "${SQL_DIR}/init_inspection_task_log.sql"
mysql --protocol=socket -uroot -proot "${DB}" < "${SQL_DIR}/init_ai_chat_log.sql"
mysql --protocol=socket -uroot -proot "${DB}" < "${SQL_DIR}/init_real_china_competition_data.sql"

echo "[mysql-init] all done"
