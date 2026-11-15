package com.benz.gymdesk360.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/** Orígenes del frontend autorizados (CORS_ALLOWED_ORIGINS separado por comas). */
@ConfigurationProperties(prefix = "app.cors")
public record CorsProperties(List<String> allowedOrigins) {
    public CorsProperties {
        allowedOrigins = allowedOrigins == null ? List.of() : allowedOrigins;
    }
}
