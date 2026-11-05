package com.example.cleanmate.global.config;

import com.example.cleanmate.domain.duty.model.RotationPolicy;
import com.example.cleanmate.domain.duty.model.WeekCalendar;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

@Configuration
public class AppConfig {

    /** 프로덕션: 호출 시점의 Asia/Seoul 현재 시각 기준 */
    @Bean
    public WeekCalendar weekCalendar() {
        return new WeekCalendar(Clock.system(ZoneId.of("Asia/Seoul")));
    }

    @Bean
    public RotationPolicy rotationPolicy() {
        return new RotationPolicy();
    }
}
