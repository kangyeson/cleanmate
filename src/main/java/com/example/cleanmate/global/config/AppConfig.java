package com.example.cleanmate.global.config;

import com.example.cleanmate.domain.duty.model.RotationPolicy;
import com.example.cleanmate.domain.duty.model.WeekCalendar;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AppConfig {

    /** 프로덕션: 현재 시각 기준 오늘(Seoul) */
    @Bean
    public WeekCalendar weekCalendar() {
        return new WeekCalendar();
    }

    @Bean
    public RotationPolicy rotationPolicy() {
        return new RotationPolicy();
    }
}
