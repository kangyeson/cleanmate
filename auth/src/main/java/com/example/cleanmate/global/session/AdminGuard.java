package com.example.cleanmate.global.session;

import com.example.cleanmate.domain.room.model.Room;
import com.example.cleanmate.domain.room.repository.RoomRepository;
import com.example.cleanmate.global.exception.AdminOnlyException;
import com.example.cleanmate.global.exception.CleanmateException;
import com.example.cleanmate.global.exception.PasswordChangeRequiredException;
import org.springframework.stereotype.Component;

@Component
public class AdminGuard {

    private final SessionStore sessionStore;
    private final RoomRepository roomRepository;

    public AdminGuard(SessionStore sessionStore, RoomRepository roomRepository) {
        this.sessionStore = sessionStore;
        this.roomRepository = roomRepository;
    }

    /**
     * 세션이 유효하고, ADMIN 역할이며, 현재 활성 방의 관리자임을 확인한다.
     *
     * @return 검증된 SessionData (session.roomId()가 현재 활성 방 ID와 일치함이 보장됨)
     * @throws CleanmateException 401 세션 없음 / 다른 방의 세션
     * @throws AdminOnlyException 403 ADMIN 역할 아님
     * @throws CleanmateException 404 진행 중인 방 없음
     */
    public SessionData requireAdmin(String sessionId) {
        SessionData session = sessionStore.find(sessionId)
            .orElseThrow(() -> new CleanmateException("로그인이 필요합니다.", 401));
        if (!"ADMIN".equals(session.role())) throw new AdminOnlyException();
        if (Boolean.TRUE.equals(session.mustChangePw())) {
            throw new PasswordChangeRequiredException();
        }

        Room room = roomRepository.findActive()
            .orElseThrow(() -> new CleanmateException("진행 중인 방이 없습니다.", 404));
        if (!room.getId().equals(session.roomId())) {
            throw new CleanmateException("로그인이 필요합니다.", 401);
        }
        return session;
    }
}
