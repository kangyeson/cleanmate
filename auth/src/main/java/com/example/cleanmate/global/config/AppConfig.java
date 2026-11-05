package com.example.cleanmate.global.config;

import com.example.cleanmate.global.security.PasswordHasher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AppConfig {

    @Bean
    public PasswordHasher passwordHasher() {
        return new PasswordHasher();
    }
}
