package com.example.cleanmate.domain.member.service;

import com.example.cleanmate.domain.member.dto.MemberResponse;
import com.example.cleanmate.domain.member.model.Member;
import com.example.cleanmate.domain.member.model.MemberRole;
import com.example.cleanmate.domain.member.repository.InMemoryMemberRepository;
import com.example.cleanmate.domain.room.model.Room;
import com.example.cleanmate.domain.room.model.RoomStatus;
import com.example.cleanmate.domain.room.repository.InMemoryRoomRepository;
import com.example.cleanmate.global.exception.AdminOnlyException;
import com.example.cleanmate.global.exception.CleanmateException;
import com.example.cleanmate.global.exception.PasswordChangeRequiredException;
import com.example.cleanmate.global.security.PasswordHasher;
import com.example.cleanmate.global.session.AdminGuard;
import com.example.cleanmate.global.session.SessionData;
import com.example.cleanmate.global.session.SessionStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class MemberServiceTest {

    private static final Long ROOM_ID = 1L;
    private static final Long ADMIN_MEMBER_ID = 10L;
    private static final Long MEMBER_ID = 11L;
    private static final String ADMIN_SESSION = "admin-session";
    private static final String MEMBER_SESSION = "member-session";
    private static final String OTHER_ROOM_SESSION = "other-room-session";

    private MemberService service;
    private InMemoryMemberRepository memberRepository;
    private InMemoryRoomRepository roomRepository;
    private Map<String, SessionData> sessions;

    @BeforeEach
    void setUp() {
        memberRepository = new InMemoryMemberRepository();
        roomRepository = new InMemoryRoomRepository();
        sessions = new HashMap<>();

        // Seed active room
        roomRepository.seedActiveRoom(new Room(ROOM_ID, "525호", LocalDate.now(), RoomStatus.ACTIVE));

        // Seed sessions
        sessions.put(ADMIN_SESSION,  new SessionData(ADMIN_MEMBER_ID, ROOM_ID, "ADMIN", "예손", false));
        sessions.put(MEMBER_SESSION, new SessionData(MEMBER_ID, ROOM_ID, "MEMBER", "민정", false));
        sessions.put(OTHER_ROOM_SESSION, new SessionData(99L, 999L, "ADMIN", "다른방관리자", false));

        // Seed members in the room
        memberRepository.save(new Member(null, ROOM_ID, "민정", MemberRole.MEMBER,
                new PasswordHasher().hash("1234"), false, 1, true));
        memberRepository.save(new Member(null, ROOM_ID, "지현", MemberRole.MEMBER,
                new PasswordHasher().hash("1234"), false, 2, true));

        SessionStore sessionStore = new SessionStore() {
            public Optional<SessionData> find(String s) { return Optional.ofNullable(sessions.get(s)); }
            public String create(SessionData d) { return "new-id"; }
            public void delete(String s) { sessions.remove(s); }
            public void update(String s, SessionData d) { sessions.put(s, d); }
        };

        AdminGuard adminGuard = new AdminGuard(sessionStore, roomRepository);
        com.example.cleanmate.domain.client.DutyClient noopDuty = new com.example.cleanmate.domain.client.DutyClient() {
            public void finalizeRoom(Long roomId) {}
            public void ensureCurrentWeek(Long roomId) {}
        };
        service = new MemberService(memberRepository, new PasswordHasher(), adminGuard, noopDuty);
    }

    // ── getMembers ─────────────────────────────────────────

    @Test
    void getMembers_관리자는_목록_조회() {
        List<MemberResponse> result = service.getMembers(ADMIN_SESSION);
        assertEquals(2, result.size());
    }

    @Test
    void getMembers_비관리자_403() {
        assertThrows(AdminOnlyException.class, () -> service.getMembers(MEMBER_SESSION));
    }

    @Test
    void getMembers_다른방세션_401() {
        CleanmateException ex = assertThrows(CleanmateException.class,
                () -> service.getMembers(OTHER_ROOM_SESSION));
        assertEquals(401, ex.getStatus());
    }

    // ── addMember ─────────────────────────────────────────

    @Test
    void addMember_정상_추가() {
        service.addMember(ADMIN_SESSION, "서현", "1234");
        assertEquals(3, memberRepository.findAllByRoomId(ROOM_ID).size());
    }

    @Test
    void addMember_빈_이름_400() {
        CleanmateException ex = assertThrows(CleanmateException.class,
                () -> service.addMember(ADMIN_SESSION, " ", "1234"));
        assertEquals(400, ex.getStatus());
    }

    @Test
    void addMember_비관리자_403() {
        assertThrows(AdminOnlyException.class,
                () -> service.addMember(MEMBER_SESSION, "서현", "1234"));
    }

    // ── deleteMember ───────────────────────────────────────

    @Test
    void deleteMember_성공() {
        Long memberId = memberRepository.findAllByRoomId(ROOM_ID).get(0).getId();
        service.deleteMember(ADMIN_SESSION, memberId);
        assertFalse(memberRepository.findById(memberId).get().isActive());
    }

    @Test
    void deleteMember_마지막_1명은_삭제_불가_400() {
        // 첫 번째 삭제 — 성공
        Long id1 = memberRepository.findAllByRoomId(ROOM_ID).get(0).getId();
        service.deleteMember(ADMIN_SESSION, id1);

        // 두 번째 삭제 — 마지막 1명
        Long id2 = memberRepository.findAllByRoomId(ROOM_ID).stream()
                .filter(Member::isActive).findFirst().get().getId();
        CleanmateException ex = assertThrows(CleanmateException.class,
                () -> service.deleteMember(ADMIN_SESSION, id2));
        assertEquals(400, ex.getStatus());
    }

    @Test
    void deleteMember_본인_삭제_불가_400() {
        // admin의 멤버 ID는 ADMIN_MEMBER_ID인데 아직 저장소에 없다 — 먼저 추가
        memberRepository.save(new Member(ADMIN_MEMBER_ID, ROOM_ID, "예손", MemberRole.ADMIN,
                new PasswordHasher().hash("1234"), false, 3, true));
        CleanmateException ex = assertThrows(CleanmateException.class,
                () -> service.deleteMember(ADMIN_SESSION, ADMIN_MEMBER_ID));
        assertEquals(400, ex.getStatus());
    }

    @Test
    void deleteMember_비관리자_403() {
        Long memberId = memberRepository.findAllByRoomId(ROOM_ID).get(0).getId();
        assertThrows(AdminOnlyException.class,
                () -> service.deleteMember(MEMBER_SESSION, memberId));
    }

    // ── reorderMembers ─────────────────────────────────────

    @Test
    void reorderMembers_정상_순번_변경() {
        List<Long> ids = memberRepository.findAllByRoomId(ROOM_ID).stream()
                .filter(Member::isActive).map(Member::getId).toList();
        // 순번 반전
        List<Long> reversed = List.of(ids.get(1), ids.get(0));
        assertDoesNotThrow(() -> service.reorderMembers(ADMIN_SESSION, reversed));
    }

    @Test
    void reorderMembers_목록_불일치_400() {
        CleanmateException ex = assertThrows(CleanmateException.class,
                () -> service.reorderMembers(ADMIN_SESSION, List.of(999L)));
        assertEquals(400, ex.getStatus());
    }

    @Test
    void reorderMembers_중복_id_400() {
        List<Long> ids = memberRepository.findAllByRoomId(ROOM_ID).stream()
                .filter(Member::isActive).map(Member::getId).toList();
        Long first = ids.get(0);
        CleanmateException ex = assertThrows(CleanmateException.class,
                () -> service.reorderMembers(ADMIN_SESSION, List.of(first, first)));
        assertEquals(400, ex.getStatus());
    }

    @Test
    void getMembers_mustChangePw_true이면_403() {
        sessions.put("mcp-session", new SessionData(ADMIN_MEMBER_ID, ROOM_ID, "ADMIN", "예손", true));
        assertThrows(PasswordChangeRequiredException.class,
                () -> service.getMembers("mcp-session"));
    }

    @Test
    void addMember_duty_실패_시_503_멤버_추가_안됨() {
        // duty 실패하는 서비스
        SessionStore sessionStore = new SessionStore() {
            public Optional<SessionData> find(String s) { return Optional.ofNullable(sessions.get(s)); }
            public String create(SessionData d) { return "new-id"; }
            public void delete(String s) { sessions.remove(s); }
            public void update(String s, SessionData d) { sessions.put(s, d); }
        };
        AdminGuard adminGuard = new AdminGuard(sessionStore, roomRepository);
        com.example.cleanmate.domain.client.DutyClient failDuty = new com.example.cleanmate.domain.client.DutyClient() {
            public void finalizeRoom(Long roomId) {}
            public void ensureCurrentWeek(Long roomId) { throw new CleanmateException("duty 오류", 503); }
        };
        MemberService failSvc = new MemberService(
                memberRepository, new PasswordHasher(), adminGuard, failDuty
        );
        int before = memberRepository.findAllByRoomId(ROOM_ID).size();
        assertThrows(CleanmateException.class,
                () -> failSvc.addMember(ADMIN_SESSION, "서현", "1234"));
        assertEquals(before, memberRepository.findAllByRoomId(ROOM_ID).size());
    }
}
