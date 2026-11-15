package com.benz.gymdesk360.security;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

/**
 * @param secret     clave HMAC en Base64 (mínimo 32 bytes decodificados). Generar con: openssl rand -base64 48
 * @param issuer     valor del claim "iss"
 * @param accessTtl  duración del access token (corto)
 * @param refreshTtl duración del refresh token (largo)
 */
@Validated
@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(
        @NotBlank String secret,
        @NotBlank String issuer,
        @NotNull Duration accessTtl,
        @NotNull Duration refreshTtl
) {
}
