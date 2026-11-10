#!/bin/bash
# =====================================================================
# 서버별 DB 계정 생성 (스키마마다 계정 분리)
#   auth_user : cleanmate_auth 만 사용
#   duty_user : cleanmate_duty 만 사용
# 비밀번호는 컨테이너 환경 변수로 받는다 (Git에 올리지 않음).
#   docker run ... -e AUTH_DB_PASSWORD=... -e DUTY_DB_PASSWORD=...
# =====================================================================
set -e

: "${AUTH_DB_PASSWORD:?AUTH_DB_PASSWORD 환경 변수가 필요합니다}"
: "${DUTY_DB_PASSWORD:?DUTY_DB_PASSWORD 환경 변수가 필요합니다}"

mysql -uroot -p"${MYSQL_ROOT_PASSWORD}" <<SQL
CREATE USER IF NOT EXISTS 'auth_user'@'%' IDENTIFIED BY '${AUTH_DB_PASSWORD}';
CREATE USER IF NOT EXISTS 'duty_user'@'%' IDENTIFIED BY '${DUTY_DB_PASSWORD}';

GRANT SELECT, INSERT, UPDATE, DELETE ON cleanmate_auth.* TO 'auth_user'@'%';
GRANT SELECT, INSERT, UPDATE, DELETE ON cleanmate_duty.* TO 'duty_user'@'%';

FLUSH PRIVILEGES;
SQL
