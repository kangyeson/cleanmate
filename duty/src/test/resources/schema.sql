-- @JdbcTest 용 H2 인메모리 스키마 (duties 테이블만)
CREATE TABLE IF NOT EXISTS duties (
  id          BIGINT       AUTO_INCREMENT PRIMARY KEY,
  room_id     BIGINT       NOT NULL,
  week_start  DATE         NOT NULL,
  zone_id     BIGINT       NOT NULL,
  assignee_id BIGINT       NOT NULL,
  status      VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
  done_by_id  BIGINT       NULL,
  done_date   DATE         NULL,
  CONSTRAINT uq_duties_week_zone UNIQUE (room_id, week_start, zone_id),
  CONSTRAINT ck_duties_status    CHECK (status IN ('PENDING', 'DONE', 'MISSED')),
  CONSTRAINT ck_duties_done      CHECK (
    (status = 'DONE') = (done_by_id IS NOT NULL AND done_date IS NOT NULL)
  )
);
