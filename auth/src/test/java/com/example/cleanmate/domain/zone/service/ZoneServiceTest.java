package com.example.cleanmate.domain.zone.service;

import com.example.cleanmate.domain.room.model.Room;
import com.example.cleanmate.domain.room.model.RoomStatus;
import com.example.cleanmate.domain.room.repository.InMemoryRoomRepository;
import com.example.cleanmate.domain.zone.dto.ZoneResponse;
import com.example.cleanmate.domain.zone.model.Zone;
import com.example.cleanmate.domain.zone.repository.InMemoryZoneRepository;
import com.example.cleanmate.global.exception.AdminOnlyException;
import com.example.cleanmate.global.exception.CleanmateException;
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

class ZoneServiceTest {

    private static final Long ROOM_ID = 1L;
    private static final String ADMIN_SESSION = "admin-session";
    private static final String MEMBER_SESSION = "member-session";

    private ZoneService service;
    private InMemoryZoneRepository zoneRepository;
    private InMemoryRoomRepository roomRepository;

    @BeforeEach
    void setUp() {
        zoneRepository = new InMemoryZoneRepository();
        roomRepository = new InMemoryRoomRepository();
        Map<String, SessionData> sessions = new HashMap<>();

        roomRepository.seedActiveRoom(new Room(ROOM_ID, "525호", LocalDate.now(), RoomStatus.ACTIVE));
        sessions.put(ADMIN_SESSION,  new SessionData(10L, ROOM_ID, "ADMIN", "예손", false));
        sessions.put(MEMBER_SESSION, new SessionData(11L, ROOM_ID, "MEMBER", "민정", false));

        // Seed zones
        zoneRepository.save(new Zone(null, ROOM_ID, "샤워실", 1, true));
        zoneRepository.save(new Zone(null, ROOM_ID, "화장실", 2, true));

        SessionStore sessionStore = new SessionStore() {
            public Optional<SessionData> find(String s) { return Optional.ofNullable(sessions.get(s)); }
            public String create(SessionData d) { return "new-id"; }
            public void delete(String s) {}
            public void update(String s, SessionData d) {}
        };

        AdminGuard adminGuard = new AdminGuard(sessionStore, roomRepository);
        com.example.cleanmate.domain.client.DutyClient noopDuty = new com.example.cleanmate.domain.client.DutyClient() {
            public void finalizeRoom(Long roomId) {}
            public void ensureCurrentWeek(Long roomId) {}
        };
        service = new ZoneService(zoneRepository, adminGuard, noopDuty);
    }

    @Test
    void getZones_관리자는_목록_조회() {
        List<ZoneResponse> result = service.getZones(ADMIN_SESSION);
        assertEquals(2, result.size());
    }

    @Test
    void getZones_비관리자_403() {
        assertThrows(AdminOnlyException.class, () -> service.getZones(MEMBER_SESSION));
    }

    @Test
    void addZone_정상_추가() {
        service.addZone(ADMIN_SESSION, "세면대");
        assertEquals(3, zoneRepository.findAllByRoomId(ROOM_ID).size());
    }

    @Test
    void addZone_빈_이름_400() {
        CleanmateException ex = assertThrows(CleanmateException.class,
                () -> service.addZone(ADMIN_SESSION, " "));
        assertEquals(400, ex.getStatus());
    }

    @Test
    void addZone_비관리자_403() {
        assertThrows(AdminOnlyException.class,
                () -> service.addZone(MEMBER_SESSION, "세면대"));
    }

    @Test
    void deleteZone_성공() {
        Long zoneId = zoneRepository.findAllByRoomId(ROOM_ID).get(0).getId();
        service.deleteZone(ADMIN_SESSION, zoneId);
        assertFalse(zoneRepository.findById(zoneId).get().isActive());
    }

    @Test
    void deleteZone_마지막_1개_삭제_불가_400() {
        Long id1 = zoneRepository.findAllByRoomId(ROOM_ID).get(0).getId();
        service.deleteZone(ADMIN_SESSION, id1);

        Long id2 = zoneRepository.findAllByRoomId(ROOM_ID).stream()
                .filter(Zone::isActive).findFirst().get().getId();
        CleanmateException ex = assertThrows(CleanmateException.class,
                () -> service.deleteZone(ADMIN_SESSION, id2));
        assertEquals(400, ex.getStatus());
    }

    @Test
    void reorderZones_목록_불일치_400() {
        CleanmateException ex = assertThrows(CleanmateException.class,
                () -> service.reorderZones(ADMIN_SESSION, List.of(999L)));
        assertEquals(400, ex.getStatus());
    }

    @Test
    void addZone_duty_실패_시_503_구역_추가_안됨() {
        Map<String, SessionData> sessions = new HashMap<>();
        sessions.put(ADMIN_SESSION, new SessionData(10L, ROOM_ID, "ADMIN", "예손", false));
        SessionStore sessionStore = new SessionStore() {
            public Optional<SessionData> find(String s) { return Optional.ofNullable(sessions.get(s)); }
            public String create(SessionData d) { return "new-id"; }
            public void delete(String s) {}
            public void update(String s, SessionData d) {}
        };
        AdminGuard adminGuard = new AdminGuard(sessionStore, roomRepository);
        com.example.cleanmate.domain.client.DutyClient failDuty = new com.example.cleanmate.domain.client.DutyClient() {
            public void finalizeRoom(Long roomId) {}
            public void ensureCurrentWeek(Long roomId) { throw new com.example.cleanmate.global.exception.CleanmateException("duty 오류", 503); }
        };
        ZoneService failSvc = new ZoneService(
                zoneRepository, adminGuard, failDuty
        );
        int before = zoneRepository.findAllByRoomId(ROOM_ID).size();
        assertThrows(com.example.cleanmate.global.exception.CleanmateException.class,
                () -> failSvc.addZone(ADMIN_SESSION, "세면대"));
        assertEquals(before, zoneRepository.findAllByRoomId(ROOM_ID).size());
    }
}
