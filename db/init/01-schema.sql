-- =====================================================================
-- cleanmate DB 스키마
-- MySQL 컨테이너의 /docker-entrypoint-initdb.d 에서 처음 시작할 때 한 번 실행된다.
-- (볼륨에 데이터가 이미 있으면 다시 실행되지 않음)
--
-- 스키마 분리: 데이터마다 주인 서버 하나
--   cleanmate_auth : 인증 서버만 사용 (rooms, members, zones)
--   cleanmate_duty : 당번 서버만 사용 (duties)
-- 스키마를 넘는 외래 키는 걸지 않는다. duties의 room/zone/member id는
-- 인증 서버의 id 값을 그대로 저장할 뿐이고, 이름은 /internal/roster로 받는다.
-- =====================================================================

SET NAMES utf8mb4;

CREATE DATABASE IF NOT EXISTS cleanmate_auth CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS cleanmate_duty CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------
-- cleanmate_auth
-- ---------------------------------------------------------------------
USE cleanmate_auth;

-- 방: 한 거주 기간의 룸메이트·구역·기록 묶음
CREATE TABLE rooms (
  id          BIGINT       AUTO_INCREMENT PRIMARY KEY,
  name        VARCHAR(50)  NOT NULL,
  start_week  DATE         NOT NULL,              -- 생성한 주의 월요일 (주 번호 0)
  end_date    DATE         NULL,                  -- 방 종료일 (진행 중이면 NULL)
  status      VARCHAR(20)  NOT NULL,              -- ACTIVE / CLOSED
  -- 진행 중인 방은 하나만: ACTIVE일 때만 1, 나머지는 NULL(UNIQUE에서 중복 허용)
  active_flag TINYINT AS (IF(status = 'ACTIVE', 1, NULL)) STORED,
  CONSTRAINT ck_rooms_status     CHECK (status IN ('ACTIVE', 'CLOSED')),
  CONSTRAINT uq_rooms_one_active UNIQUE (active_flag)
) ENGINE = InnoDB;

-- 룸메이트: 삭제는 active = FALSE (지난 기록 유지를 위해 행을 지우지 않음)
CREATE TABLE members (
  id             BIGINT       AUTO_INCREMENT PRIMARY KEY,
  room_id        BIGINT       NOT NULL,
  name           VARCHAR(30)  NOT NULL,
  role           VARCHAR(20)  NOT NULL,           -- ADMIN / MEMBER
  password_hash  VARCHAR(60)  NOT NULL,
  must_change_pw BOOLEAN      NOT NULL DEFAULT TRUE,
  turn_order     INT          NOT NULL,           -- 순번 (순번 변경 중 잠깐 겹칠 수 있어 UNIQUE 없음)
  active         BOOLEAN      NOT NULL DEFAULT TRUE,
  CONSTRAINT fk_members_room      FOREIGN KEY (room_id) REFERENCES rooms (id) ON DELETE RESTRICT,
  CONSTRAINT uq_members_room_name UNIQUE (room_id, name),   -- 퇴실자와 같은 이름도 막힘
  CONSTRAINT ck_members_role      CHECK (role IN ('ADMIN', 'MEMBER'))
) ENGINE = InnoDB;

-- 청소 구역
CREATE TABLE zones (
  id         BIGINT       AUTO_INCREMENT PRIMARY KEY,
  room_id    BIGINT       NOT NULL,
  name       VARCHAR(30)  NOT NULL,
  zone_order INT          NOT NULL,
  active     BOOLEAN      NOT NULL DEFAULT TRUE,
  CONSTRAINT fk_zones_room      FOREIGN KEY (room_id) REFERENCES rooms (id) ON DELETE RESTRICT,
  CONSTRAINT uq_zones_room_name UNIQUE (room_id, name)
) ENGINE = InnoDB;

-- ---------------------------------------------------------------------
-- cleanmate_duty
-- ---------------------------------------------------------------------
USE cleanmate_duty;

-- 당번 기록: 한 주·한 구역에 한 행
CREATE TABLE duties (
  id          BIGINT       AUTO_INCREMENT PRIMARY KEY,
  room_id     BIGINT       NOT NULL,              -- 인증 서버 rooms.id (FK 없음)
  week_start  DATE         NOT NULL,              -- 그 주의 월요일
  zone_id     BIGINT       NOT NULL,              -- 인증 서버 zones.id (FK 없음)
  assignee_id BIGINT       NOT NULL,              -- 인증 서버 members.id (FK 없음)
  status      VARCHAR(20)  NOT NULL DEFAULT 'PENDING',  -- PENDING / DONE / MISSED
  done_by_id  BIGINT       NULL,                  -- 실제 청소한 사람 (DONE일 때만)
  done_date   DATE         NULL,                  -- 완료 날짜 (DONE일 때만)
  -- 같은 주·같은 구역 배정은 한 번만 (동시 배정 생성도 DB가 막음)
  CONSTRAINT uq_duties_week_zone UNIQUE (room_id, week_start, zone_id),
  CONSTRAINT ck_duties_status    CHECK (status IN ('PENDING', 'DONE', 'MISSED')),
  -- DONE이면 청소한 사람·날짜가 반드시 있고, DONE이 아니면 반드시 비어 있음
  CONSTRAINT ck_duties_done      CHECK ((status = 'DONE') = (done_by_id IS NOT NULL AND done_date IS NOT NULL))
) ENGINE = InnoDB;
