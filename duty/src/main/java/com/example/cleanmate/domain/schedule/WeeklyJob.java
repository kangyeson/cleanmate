package com.example.cleanmate.domain.schedule;

import com.example.cleanmate.domain.client.NoActiveRoomException;
import com.example.cleanmate.domain.client.RosterUnavailableException;
import com.example.cleanmate.domain.duty.service.DutyService;
import com.example.cleanmate.global.lock.JobLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 정기 작업: 빠진 주 배정 채우기 + 지난 주 미완료 확정 (DutyService.closePastWeeks).
 *
 * 실행 시점: 매주 월요일 0시(Asia/Seoul)와 서버가 시작될 때.
 *   시험할 때는 환경 변수 WEEKLY_CRON으로 주기를 바꾼다(예: "0 * * * * *" = 매분 0초).
 *
 * 당번 서버가 2대면 두 대가 동시에 실행하려 하므로, JobLock을 얻은 한 대만 실행한다.
 * 잠금은 "동시에" 실행되는 것만 막는다. 서버가 시작될 때마다 다시 돌아도 안전하도록
 * 작업 자체도 여러 번 실행해도 결과가 같게(멱등) 만들어 두었다.
 */
@Component
public class WeeklyJob {
    private static final Logger log = LoggerFactory.getLogger(WeeklyJob.class);

    private final DutyService dutyService;
    private final JobLock jobLock;
    private final String instanceId;

    public WeeklyJob(DutyService dutyService, JobLock jobLock,
                     @Value("${INSTANCE_ID:local}") String instanceId) {
        this.dutyService = dutyService;
        this.jobLock = jobLock;
        this.instanceId = instanceId;
    }

    @Scheduled(cron = "${WEEKLY_CRON:0 0 0 * * MON}", zone = "Asia/Seoul")
    public void scheduled() { execute("scheduled"); }

    @EventListener(ApplicationReadyEvent.class)
    public void onReady() { execute("startup"); }

    /** 테스트에서도 부를 수 있게 package-private */
    void execute(String trigger) {
        boolean locked;
        try {
            locked = jobLock.tryLock();
        } catch (Exception e) {
            // Redis에 연결할 수 없으면 잠금을 확인할 수 없으므로 이번 회차는 건너뛴다
            log.warn("정기 작업 건너뜀 [{}] {}: 잠금 확인 실패 ({})", trigger, instanceId, e.getMessage());
            return;
        }
        if (!locked) {
            log.info("정기 작업 건너뜀 [{}] {}: 다른 인스턴스가 실행 중", trigger, instanceId);
            return;
        }

        try {
            dutyService.closePastWeeks();
            log.info("정기 작업 실행 완료 [{}] {}", trigger, instanceId);
        } catch (NoActiveRoomException e) {
            log.info("정기 작업 건너뜀 [{}]: 진행 중인 방 없음", trigger);
        } catch (RosterUnavailableException e) {
            log.warn("정기 작업 건너뜀 [{}]: auth 서버 응답 없음", trigger);
        } catch (Exception e) {
            log.error("정기 작업 실패 [{}]", trigger, e);
        } finally {
            try {
                jobLock.unlock();
            } catch (Exception e) {
                // 풀지 못해도 120초 뒤 자동으로 사라진다
                log.warn("잠금 해제 실패 (120초 뒤 자동 만료): {}", e.getMessage());
            }
        }
    }
}
