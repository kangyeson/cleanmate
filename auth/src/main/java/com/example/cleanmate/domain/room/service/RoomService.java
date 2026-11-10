package com.example.cleanmate.domain.room.service;

import com.example.cleanmate.domain.member.repository.MemberRepository;
import com.example.cleanmate.domain.room.model.Room;
import com.example.cleanmate.domain.room.repository.RoomRepository;
import com.example.cleanmate.domain.roster.dto.RosterResponse;
import com.example.cleanmate.domain.zone.repository.ZoneRepository;
import com.example.cleanmate.global.exception.CleanmateException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RoomService {

    private final RoomRepository roomRepository;
    private final MemberRepository memberRepository;
    private final ZoneRepository zoneRepository;

    public RoomService(RoomRepository roomRepository,
                       MemberRepository memberRepository,
                       ZoneRepository zoneRepository) {
        this.roomRepository = roomRepository;
        this.memberRepository = memberRepository;
        this.zoneRepository = zoneRepository;
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
}
