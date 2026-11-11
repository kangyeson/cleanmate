package com.example.cleanmate.domain.room.service;

import com.example.cleanmate.domain.member.model.Member;
import com.example.cleanmate.domain.member.model.MemberRole;
import com.example.cleanmate.domain.member.repository.MemberRepository;
import com.example.cleanmate.domain.room.dto.CreateRoomRequest;
import com.example.cleanmate.domain.room.exception.RoomAlreadyActiveException;
import com.example.cleanmate.domain.room.model.Room;
import com.example.cleanmate.domain.room.model.RoomStatus;
import com.example.cleanmate.domain.room.repository.RoomRepository;
import com.example.cleanmate.domain.roster.dto.RosterResponse;
import com.example.cleanmate.domain.zone.model.Zone;
import com.example.cleanmate.domain.zone.repository.ZoneRepository;
import com.example.cleanmate.global.exception.CleanmateException;
import com.example.cleanmate.global.security.PasswordHasher;
import com.example.cleanmate.global.session.SessionData;
import com.example.cleanmate.global.validation.InputValidator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class RoomService {

    private final RoomRepository roomRepository;
    private final MemberRepository memberRepository;
    private final ZoneRepository zoneRepository;
    private final PasswordHasher passwordHasher;
    private final Clock clock;

    public RoomService(RoomRepository roomRepository,
                       MemberRepository memberRepository,
                       ZoneRepository zoneRepository,
                       PasswordHasher passwordHasher,
                       Clock clock) {
        this.roomRepository = roomRepository;
        this.memberRepository = memberRepository;
        this.zoneRepository = zoneRepository;
        this.passwordHasher = passwordHasher;
        this.clock = clock;
    }

    /**
     * 현재 진행 중인 방의 명단(방·룸메이트·구역)을 조립해 반환한다.
     *
     * @throws CleanmateException 404 진행 중인 방이 없을 때
     */
    public RosterResponse getRoster() {
        Room room = roomRepository.findActive()
                .orElseThrow(() -> new CleanmateException("진행 중인 방이 없습니다.", 404));

        List<RosterResponse.MemberInfo> members = memberRepository
                .findAllByRoomIdOrderByTurnOrder(room.getId()).stream()
                .map(m -> new RosterResponse.MemberInfo(m.getId(), m.getName()))
                .toList();

        List<RosterResponse.ZoneInfo> zones = zoneRepository
                .findAllByRoomIdOrderByZoneOrder(room.getId()).stream()
                .map(z -> new RosterResponse.ZoneInfo(z.getId(), z.getName()))
                .toList();

        return new RosterResponse(room.getId(), room.getName(), room.getStartWeek(), members, zones);
    }

    /**
     * 방을 생성하고 관리자 세션 데이터를 반환한다.
     * 세션 생성(Redis)은 호출자(컨트롤러)가 트랜잭션 커밋 후에 수행한다.
     *
     * @throws CleanmateException          400 입력 검증 실패
     * @throws RoomAlreadyActiveException  409 이미 진행 중인 방이 있을 때
     */
    @Transactional
    public SessionData createRoom(CreateRoomRequest request) {
        // 1. 입력 검증
        validateCreateRoomRequest(request);

        // 2. 진행 중인 방 존재 여부 확인
        if (roomRepository.findActive().isPresent()) {
            throw new RoomAlreadyActiveException();
        }

        // 3. rooms INSERT (start_week = 오늘의 주 월요일)
        LocalDate monday = LocalDate.now(clock)
                .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        Room room = roomRepository.save(
                new Room(null, request.roomName().trim(), monday, RoomStatus.ACTIVE)
        );

        // 4. members INSERT
        String adminName = request.adminName().trim();
        Member adminMember = null;
        for (int i = 0; i < request.members().size(); i++) {
            CreateRoomRequest.MemberInput input = request.members().get(i);
            String name = input.name().trim();
            MemberRole role = name.equals(adminName) ? MemberRole.ADMIN : MemberRole.MEMBER;
            boolean mustChangePw = !name.equals(adminName);
            String passwordHash = passwordHasher.hash(input.password());
            int turnOrder = i + 1;

            Member saved = memberRepository.save(
                    new Member(null, room.getId(), name, role, passwordHash, mustChangePw, turnOrder, true)
            );
            if (name.equals(adminName)) {
                adminMember = saved;
            }
        }

        // 5. zones INSERT
        List<String> zones = request.zones();
        for (int i = 0; i < zones.size(); i++) {
            zoneRepository.save(
                    new Zone(null, room.getId(), zones.get(i).trim(), i + 1, true)
            );
        }

        // 6. 관리자 세션 데이터 반환 (세션 생성은 컨트롤러에서 커밋 후 수행)
        return new SessionData(
                adminMember.getId(), room.getId(),
                MemberRole.ADMIN.name(), adminMember.getName(), false
        );
    }

    // ── 검증 ─────────────────────────────────────────────────────────────────

    private void validateCreateRoomRequest(CreateRoomRequest request) {
        // 방 이름 빈 값
        if (request.roomName() == null || request.roomName().isBlank()) {
            throw new CleanmateException("방 이름은 필수입니다.", 400);
        }
        // 룸메이트 0명
        if (request.members() == null || request.members().isEmpty()) {
            throw new CleanmateException("룸메이트는 1명 이상이어야 합니다.", 400);
        }
        // 구역 0개
        if (request.zones() == null || request.zones().isEmpty()) {
            throw new CleanmateException("구역은 1개 이상이어야 합니다.", 400);
        }

        // 각 멤버 이름·비밀번호 검증 + 이름 중복 확인
        Set<String> names = new HashSet<>();
        for (CreateRoomRequest.MemberInput m : request.members()) {
            String name = InputValidator.validateName(m.name());
            InputValidator.validatePassword(m.password());
            if (!names.add(name)) {
                throw new CleanmateException("멤버 이름이 중복됩니다: " + name, 400);
            }
        }

        // 각 구역 이름 검증 + 중복 확인
        Set<String> zoneNames = new HashSet<>();
        for (String zone : request.zones()) {
            String name = InputValidator.validateName(zone);
            if (!zoneNames.add(name)) {
                throw new CleanmateException("구역 이름이 중복됩니다: " + name, 400);
            }
        }

        // adminName이 members 목록에 있는지 확인
        String adminName = request.adminName() == null ? null : request.adminName().trim();
        if (adminName == null || adminName.isBlank()) {
            throw new CleanmateException("관리자 이름은 필수입니다.", 400);
        }
        boolean adminFound = request.members().stream()
                .anyMatch(m -> m.name() != null && m.name().trim().equals(adminName));
        if (!adminFound) {
            throw new CleanmateException("관리자 이름이 멤버 목록에 없습니다.", 400);
        }
    }
}
