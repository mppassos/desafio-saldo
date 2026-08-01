package com.itau.api.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.ZoneId;

@Configuration
public class AppConfig {

    @Bean
    public ZoneId appZoneId(@Value("${app.timezone:America/Sao_Paulo}") String timezone) {
        return ZoneId.of(timezone);
    }
}
