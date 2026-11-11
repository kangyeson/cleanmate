package com.example.cleanmate.domain.history.service;

import com.example.cleanmate.domain.client.NoActiveRoomException;
import com.example.cleanmate.domain.client.RosterClient;
import com.example.cleanmate.domain.client.RosterResponse;
import com.example.cleanmate.domain.client.StubRosterClient;
import com.example.cleanmate.domain.duty.model.Duty;
import com.example.cleanmate.domain.duty.model.DutyStatus;
import com.example.cleanmate.domain.duty.repository.InMemoryDutyRepository;
import com.example.cleanmate.domain.history.dto.HistoryResponse;
import com.example.cleanmate.global.exception.CleanmateException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class HistoryServiceTest {

    private InMemoryDutyRepository repository;
    private HistoryService service;

    @BeforeEach
    void setUp() {
        repository = new InMemoryDutyRepository();
        service = new HistoryService(repository, new StubRosterClient());
    }

    @Test
    void getHistory_최신주부터_내림차순() {
        LocalDate week1 = LocalDate.of(2026, 11, 2);
        LocalDate week2 = LocalDate.of(2026, 11, 9);

        repository.save(Duty.newAssignment(1L, week1, 1L, 1L));
        repository.save(Duty.newAssignment(1L, week2, 2L, 2L));

        HistoryResponse response = service.getHistory(null);

        assertEquals(2, response.weeks().size());
        // 최신 주(11/9)가 첫 번째여야 한다
        assertEquals(week2, response.weeks().get(0).weekStart());
        assertEquals(week1, response.weeks().get(1).weekStart());
    }

    @Test
    void getHistory_weekStart_필터_없는주면_404() {
        LocalDate week1 = LocalDate.of(2026, 11, 2);
        repository.save(Duty.newAssignment(1L, week1, 1L, 1L));

        LocalDate nonExistent = LocalDate.of(2026, 10, 26);
        CleanmateException ex = assertThrows(CleanmateException.class,
                () -> service.getHistory(nonExistent));
        assertEquals(404, ex.getStatus());
    }

    @Test
    void getHistory_방없으면_404() {
        // NoActiveRoomException을 던지는 RosterClient로 교체
        RosterClient noRoom = new RosterClient() {
            @Override
            public RosterResponse getRoster() {
                throw new NoActiveRoomException();
            }
        };
        HistoryService noRoomService = new HistoryService(repository, noRoom);

        assertThrows(NoActiveRoomException.class, () -> noRoomService.getHistory(null));
    }
}
