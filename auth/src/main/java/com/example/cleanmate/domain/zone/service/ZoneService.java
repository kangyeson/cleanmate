package com.example.cleanmate.domain.zone.service;

import com.example.cleanmate.domain.client.DutyClient;
import com.example.cleanmate.domain.zone.dto.ZoneResponse;
import com.example.cleanmate.domain.zone.model.Zone;
import com.example.cleanmate.domain.zone.repository.ZoneRepository;
import com.example.cleanmate.global.exception.CleanmateException;
import com.example.cleanmate.global.session.AdminGuard;
import com.example.cleanmate.global.session.SessionData;
import com.example.cleanmate.global.validation.InputValidator;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class ZoneService {

    private final ZoneRepository zoneRepository;
    private final AdminGuard adminGuard;
    private final DutyClient dutyClient;

    public ZoneService(ZoneRepository zoneRepository, AdminGuard adminGuard, DutyClient dutyClient) {
        this.zoneRepository = zoneRepository;
        this.adminGuard = adminGuard;
        this.dutyClient = dutyClient;
    }

    public List<ZoneResponse> getZones(String sessionId) {
        SessionData session = adminGuard.requireAdmin(sessionId);
        return zoneRepository.findAllByRoomId(session.roomId()).stream()
            .map(z -> new ZoneResponse(z.getId(), z.getName(), z.getZoneOrder(), z.isActive()))
            .toList();
    }

    @Transactional
    public void addZone(String sessionId, String name) {
        SessionData session = adminGuard.requireAdmin(sessionId);
        Long roomId = session.roomId();
        dutyClient.ensureCurrentWeek(roomId);  // 배정 먼저 확정
        String validatedName = InputValidator.validateName(name);

        List<Zone> all = zoneRepository.findAllByRoomId(roomId);
        int maxOrder = all.stream().mapToInt(Zone::getZoneOrder).max().orElse(0);

        try {
            zoneRepository.save(new Zone(null, roomId, validatedName, maxOrder + 1, true));
        } catch (DuplicateKeyException e) {
            throw new CleanmateException("이미 사용된 구역 이름입니다.", 400);
        }
    }

    @Transactional
    public void renameZone(String sessionId, Long zoneId, String newName) {
        SessionData session = adminGuard.requireAdmin(sessionId);
        Long roomId = session.roomId();
        String validatedName = InputValidator.validateName(newName);

        Zone zone = zoneRepository.findById(zoneId)
            .orElseThrow(() -> new CleanmateException("구역을 찾을 수 없습니다.", 404));
        if (!zone.getRoomId().equals(roomId)) {
            throw new CleanmateException("구역을 찾을 수 없습니다.", 404);
        }

        try {
            zoneRepository.updateName(zoneId, validatedName);
        } catch (DuplicateKeyException e) {
            throw new CleanmateException("이미 사용된 구역 이름입니다.", 400);
        }
    }

    @Transactional
    public void deleteZone(String sessionId, Long zoneId) {
        SessionData session = adminGuard.requireAdmin(sessionId);
        Long roomId = session.roomId();
        dutyClient.ensureCurrentWeek(roomId);  // 배정 먼저 확정

        Zone zone = zoneRepository.findById(zoneId)
            .orElseThrow(() -> new CleanmateException("구역을 찾을 수 없습니다.", 404));
        if (!zone.getRoomId().equals(roomId)) {
            throw new CleanmateException("구역을 찾을 수 없습니다.", 404);
        }
        if (!zone.isActive()) {
            throw new CleanmateException("이미 비활성화된 구역입니다.", 400);
        }

        long activeCount = zoneRepository.findAllByRoomId(roomId).stream()
            .filter(Zone::isActive).count();
        if (activeCount <= 1) {
            throw new CleanmateException("마지막 활성 구역은 삭제할 수 없습니다.", 400);
        }

        zoneRepository.deactivate(zoneId);
    }

    @Transactional
    public void reorderZones(String sessionId, List<Long> zoneIds) {
        SessionData session = adminGuard.requireAdmin(sessionId);
        Long roomId = session.roomId();
        dutyClient.ensureCurrentWeek(roomId);  // 배정 먼저 확정

        List<Long> activeIds = zoneRepository.findAllByRoomId(roomId).stream()
            .filter(Zone::isActive)
            .map(Zone::getId)
            .toList();

        if (zoneIds.size() != activeIds.size()) {
            throw new CleanmateException("활성 구역 목록과 ID 수가 일치하지 않습니다.", 400);
        }
        Set<Long> activeSet = new HashSet<>(activeIds);
        for (Long id : zoneIds) {
            if (!activeSet.contains(id)) {
                throw new CleanmateException("유효하지 않은 구역 ID가 포함되어 있습니다.", 400);
            }
        }
        if (new HashSet<>(zoneIds).size() != zoneIds.size()) {
            throw new CleanmateException("중복된 구역 ID가 있습니다.", 400);
        }

        zoneRepository.updateZoneOrders(roomId, zoneIds);
    }
}
