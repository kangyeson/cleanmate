package com.example.cleanmate.domain.schedule;

import com.example.cleanmate.domain.client.NoActiveRoomException;
import com.example.cleanmate.domain.client.RosterUnavailableException;
import com.example.cleanmate.domain.duty.service.DutyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class WeeklyJob {
    private static final Logger log = LoggerFactory.getLogger(WeeklyJob.class);
    private final DutyService dutyService;

    public WeeklyJob(DutyService dutyService) { this.dutyService = dutyService; }

    @Scheduled(cron = "0 0 0 * * MON", zone = "Asia/Seoul")
    public void scheduled() { execute("scheduled"); }

    @EventListener(ApplicationReadyEvent.class)
    public void onReady() { execute("startup"); }

    private void execute(String trigger) {
        try {
            dutyService.closePastWeeks();
            log.info("정기 작업 완료 ({})", trigger);
        } catch (NoActiveRoomException e) {
            log.info("정기 작업 건너뜀 [{}]: 진행 중인 방 없음", trigger);
        } catch (RosterUnavailableException e) {
            log.warn("정기 작업 건너뜀 [{}]: auth 서버 응답 없음", trigger);
        } catch (Exception e) {
            log.error("정기 작업 실패 [{}]", trigger, e);
        }
    }
}
