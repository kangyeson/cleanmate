#!/bin/bash
# =====================================================================
# 서버별 DB 계정 생성 (스키마마다 계정 분리)
#   auth_user : cleanmate_auth 만 사용
#   duty_user : cleanmate_duty 만 사용
#   notify_user : cleanmate_notify 만 사용 (알림 서버)
#   docker run ... --env-file env/db.env  (AUTH_/DUTY_/NOTIFY_DB_PASSWORD)
# =====================================================================
set -e

: "${AUTH_DB_PASSWORD:?AUTH_DB_PASSWORD 환경 변수가 필요합니다}"
: "${DUTY_DB_PASSWORD:?DUTY_DB_PASSWORD 환경 변수가 필요합니다}"
: "${NOTIFY_DB_PASSWORD:?NOTIFY_DB_PASSWORD 환경 변수가 필요합니다}"

mysql -uroot -p"${MYSQL_ROOT_PASSWORD}" <<SQL
CREATE USER IF NOT EXISTS 'auth_user'@'%' IDENTIFIED BY '${AUTH_DB_PASSWORD}';
CREATE USER IF NOT EXISTS 'duty_user'@'%' IDENTIFIED BY '${DUTY_DB_PASSWORD}';
CREATE USER IF NOT EXISTS 'notify_user'@'%' IDENTIFIED BY '${NOTIFY_DB_PASSWORD}';

GRANT SELECT, INSERT, UPDATE, DELETE ON cleanmate_auth.* TO 'auth_user'@'%';
GRANT SELECT, INSERT, UPDATE, DELETE ON cleanmate_duty.* TO 'duty_user'@'%';
GRANT SELECT, INSERT, UPDATE, DELETE ON cleanmate_notify.* TO 'notify_user'@'%';

FLUSH PRIVILEGES;
SQL
