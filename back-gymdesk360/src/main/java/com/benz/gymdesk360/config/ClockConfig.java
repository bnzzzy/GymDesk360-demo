package com.benz.gymdesk360.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

/**
 * Reloj de negocio: "hoy" se calcula en la zona del gimnasio (por defecto Chile),
 * no en la del servidor (UTC). Así una membresía no "vence" a las 21:00 hora local.
 */
@Configuration
public class ClockConfig {

    @Bean
    public Clock clock(@Value("${app.timezone:America/Santiago}") String zone) {
        return Clock.system(ZoneId.of(zone));
    }
}
