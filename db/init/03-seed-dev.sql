-- =====================================================================
-- 개발·시험용 예시 데이터
-- 비밀번호는 모두 "1234"
-- 구역 id는 1/2/3/4, check.http와 맞춤
-- 실제 사용 시에는 이 파일을 빼고 방 생성(POST /api/rooms)으로 시작한다.
-- =====================================================================
SET NAMES utf8mb4;
USE cleanmate_auth;

INSERT INTO rooms (id, name, start_week, end_date, status)
VALUES (1, '2026-2학기 525호', '2026-11-02', NULL, 'ACTIVE');

-- must_change_pw = FALSE: 시험 편의를 위해 첫 로그인 비밀번호 변경을 건너뜀
INSERT INTO members (id, room_id, name, role, password_hash, must_change_pw, turn_order, active) VALUES
  (1, 1, '민정', 'MEMBER', '$2a$10$oprepZYc5/zzCnuR8nQ2r.emTEQRKAK05NDw3SjsVgjOUn19ciahK', FALSE, 1, TRUE),
  (2, 1, '지현', 'MEMBER', '$2a$10$HCgQd0no2se78mi1DlCeRumA74Ca5I4NhxWuJZ/687euHE53AeXy.', FALSE, 2, TRUE),
  (3, 1, '서현', 'MEMBER', '$2a$10$UiEx9kz.cucx76tciUhk6OlhzbNAJuUw0dlgsOG0jjFD/mFiVSJxG', FALSE, 3, TRUE),
  (4, 1, '예손', 'ADMIN', '$2a$10$kiB.pFL1/WeLyzZXirDQQuuAXa7hf/56VcZ0.7ubaq/kAyNpmTrH2', FALSE, 4, TRUE);

INSERT INTO zones (id, room_id, name, zone_order, active) VALUES
  (1, 1, '샤워실', 1, TRUE),
  (2, 1, '화장실', 2, TRUE),
  (3, 1, '세면대', 3, TRUE),
  (4, 1, '바닥',   4, TRUE);

-- duties는 넣지 않는다: 당번 서버가 순환 규칙으로 직접 만든다.
