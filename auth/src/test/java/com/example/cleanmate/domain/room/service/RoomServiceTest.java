package com.example.cleanmate.domain.room.service;

import com.example.cleanmate.domain.member.model.Member;
import com.example.cleanmate.domain.member.model.MemberRole;
import com.example.cleanmate.domain.member.repository.InMemoryMemberRepository;
import com.example.cleanmate.domain.room.dto.CreateRoomRequest;
import com.example.cleanmate.domain.room.exception.RoomAlreadyActiveException;
import com.example.cleanmate.domain.room.model.Room;
import com.example.cleanmate.domain.room.model.RoomStatus;
import com.example.cleanmate.domain.room.repository.InMemoryRoomRepository;
import com.example.cleanmate.domain.zone.model.Zone;
import com.example.cleanmate.domain.zone.repository.InMemoryZoneRepository;
import com.example.cleanmate.global.exception.CleanmateException;
import com.example.cleanmate.global.security.PasswordHasher;
import com.example.cleanmate.global.session.SessionData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RoomServiceTest {

    private RoomService service;
    private InMemoryRoomRepository roomRepository;
    private InMemoryMemberRepository memberRepository;
    private InMemoryZoneRepository zoneRepository;
    private PasswordHasher passwordHasher;

    @BeforeEach
    void setUp() {
        passwordHasher = new PasswordHasher();
        roomRepository = new InMemoryRoomRepository();
        memberRepository = new InMemoryMemberRepository();
        zoneRepository = new InMemoryZoneRepository();

        service = new RoomService(
                roomRepository,
                memberRepository,
                zoneRepository,
                passwordHasher,
                Clock.systemDefaultZone()
        );
    }

    private CreateRoomRequest defaultRequest() {
        return new CreateRoomRequest(
                "2026-2학기 525호",
                "예손",
                List.of(
                        new CreateRoomRequest.MemberInput("민정", "1234"),
                        new CreateRoomRequest.MemberInput("지현", "1234"),
                        new CreateRoomRequest.MemberInput("서현", "1234"),
                        new CreateRoomRequest.MemberInput("예손", "1234")
                ),
                List.of("샤워실", "화장실", "세면대", "바닥")
        );
    }

    // ── 1. 정상 생성 ──────────────────────────────────────────

    @Test
    void createRoom_정상_생성_rooms_members_zones_저장() {
        SessionData sessionData = service.createRoom(defaultRequest());

        assertNotNull(sessionData);

        // rooms 저장 확인
        List<Room> rooms = roomRepository.findAll();
        assertEquals(1, rooms.size());
        assertEquals("2026-2학기 525호", rooms.get(0).getName());
        assertEquals(RoomStatus.ACTIVE, rooms.get(0).getStatus());

        // start_week = 오늘의 주 월요일
        LocalDate expectedMonday = LocalDate.now().with(DayOfWeek.MONDAY);
        assertEquals(expectedMonday, rooms.get(0).getStartWeek());

        // members 저장 확인
        List<Member> members = memberRepository.findAll();
        assertEquals(4, members.size());

        // zones 저장 확인
        List<Zone> zones = zoneRepository.findAll();
        assertEquals(4, zones.size());
    }

    @Test
    void createRoom_정상_생성_관리자_세션_데이터_확인() {
        SessionData data = service.createRoom(defaultRequest());
        assertEquals("ADMIN", data.role());
        assertEquals("예손", data.name());
        assertNotNull(data.memberId());
        assertNotNull(data.roomId());
    }

    // ── 2. 진행 중인 방 있을 때 → 409 ───────────────────────

    @Test
    void createRoom_진행중인방_있으면_409() {
        // 이미 ACTIVE 방을 심어둔다
        roomRepository.seedActiveRoom(
                new Room(99L, "기존 방", LocalDate.now(), RoomStatus.ACTIVE)
        );

        assertThrows(RoomAlreadyActiveException.class,
                () -> service.createRoom(defaultRequest()));
    }

    // ── 3. adminName이 목록에 없을 때 → 400 ─────────────────

    @Test
    void createRoom_adminName_목록에_없으면_400() {
        CreateRoomRequest request = new CreateRoomRequest(
                "525호",
                "없는사람",
                List.of(
                        new CreateRoomRequest.MemberInput("민정", "1234"),
                        new CreateRoomRequest.MemberInput("지현", "1234")
                ),
                List.of("샤워실")
        );

        CleanmateException ex = assertThrows(CleanmateException.class,
                () -> service.createRoom(request));
        assertEquals(400, ex.getStatus());
    }

    // ── 4. 이름 중복 → 400 ──────────────────────────────────

    @Test
    void createRoom_이름_중복_400() {
        CreateRoomRequest request = new CreateRoomRequest(
                "525호",
                "민정",
                List.of(
                        new CreateRoomRequest.MemberInput("민정", "1234"),
                        new CreateRoomRequest.MemberInput("민정", "5678")
                ),
                List.of("샤워실")
        );

        CleanmateException ex = assertThrows(CleanmateException.class,
                () -> service.createRoom(request));
        assertEquals(400, ex.getStatus());
    }

    // ── 5. 비밀번호 4자 미만 → 400 ──────────────────────────

    @Test
    void createRoom_비밀번호_4자미만_400() {
        CreateRoomRequest request = new CreateRoomRequest(
                "525호",
                "민정",
                List.of(
                        new CreateRoomRequest.MemberInput("민정", "123"),
                        new CreateRoomRequest.MemberInput("지현", "1234")
                ),
                List.of("샤워실")
        );

        CleanmateException ex = assertThrows(CleanmateException.class,
                () -> service.createRoom(request));
        assertEquals(400, ex.getStatus());
    }

    // ── 6. 관리자만 ADMIN, 나머지 MEMBER ────────────────────

    @Test
    void createRoom_관리자만_ADMIN_나머지_MEMBER() {
        service.createRoom(defaultRequest());

        List<Member> members = memberRepository.findAll()
                .stream()
                .sorted((a, b) -> a.getTurnOrder() - b.getTurnOrder())
                .toList();

        assertEquals(4, members.size());

        for (Member m : members) {
            if (m.getName().equals("예손")) {
                assertEquals(MemberRole.ADMIN, m.getRole());
            } else {
                assertEquals(MemberRole.MEMBER, m.getRole());
            }
        }
    }

    // ── 7. must_change_pw: 관리자 false, 나머지 true ─────────

    @Test
    void createRoom_mustChangePw_관리자_false_나머지_true() {
        service.createRoom(defaultRequest());

        List<Member> members = memberRepository.findAll();

        for (Member m : members) {
            if (m.getName().equals("예손")) {
                assertFalse(m.isMustChangePw(), "관리자는 must_change_pw=FALSE여야 한다");
            } else {
                assertTrue(m.isMustChangePw(), m.getName() + "은 must_change_pw=TRUE여야 한다");
            }
        }
    }

    // ── 8. 구역 이름 검증 ────────────────────────────────────

    @Test
    void createRoom_구역_공백_이름_400() {
        CreateRoomRequest request = new CreateRoomRequest(
                "525호",
                "민정",
                List.of(new CreateRoomRequest.MemberInput("민정", "1234")),
                List.of("샤워실", " ")
        );

        CleanmateException ex = assertThrows(CleanmateException.class,
                () -> service.createRoom(request));
        assertEquals(400, ex.getStatus());
    }

    @Test
    void createRoom_구역_이름_중복_400() {
        CreateRoomRequest request = new CreateRoomRequest(
                "525호",
                "민정",
                List.of(new CreateRoomRequest.MemberInput("민정", "1234")),
                List.of("샤워실", "샤워실")
        );

        CleanmateException ex = assertThrows(CleanmateException.class,
                () -> service.createRoom(request));
        assertEquals(400, ex.getStatus());
    }
}
