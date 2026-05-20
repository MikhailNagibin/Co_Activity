package com.coactivity.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Единый источник времени для приложения.
 *
 * <p>Зачем: инфраструктурные компоненты (например Redis helper) не должны вызывать Instant.now()
 * напрямую — лучше инжектить Clock, чтобы поведение было детерминированным и тестируемым.</p>
 */
@Configuration
public class TimeConfig {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
