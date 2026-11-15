package com.benz.gymdesk360.service;

import com.benz.gymdesk360.dto.request.ChangePasswordRequest;
import com.benz.gymdesk360.dto.request.LoginRequest;
import com.benz.gymdesk360.dto.response.AuthResponse;
import com.benz.gymdesk360.dto.response.UserResponse;
import com.benz.gymdesk360.exception.ApiException;
import com.benz.gymdesk360.model.RefreshToken;
import com.benz.gymdesk360.model.User;
import com.benz.gymdesk360.repository.RefreshTokenRepository;
import com.benz.gymdesk360.repository.UserRepository;
import com.benz.gymdesk360.demo.DemoGuard;
import com.benz.gymdesk360.security.AuthUser;
import com.benz.gymdesk360.security.JwtProperties;
import com.benz.gymdesk360.security.JwtService;
import com.benz.gymdesk360.security.LoginRateLimiter;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;

@Slf4j
@Service
public class AuthService {

    private static final String INVALID_CREDENTIALS = "Email o contraseña incorrectos";
    private static final String INVALID_REFRESH = "Refresh token inválido o expirado";

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;
    private final LoginRateLimiter rateLimiter;
    private final DemoGuard demoGuard;
    private final SecureRandom random = new SecureRandom();
    /** Hash ficticio para que el login tarde lo mismo exista o no el email (evita enumerar usuarios). */
    private final String dummyHash;

    public AuthService(UserRepository userRepository, RefreshTokenRepository refreshTokenRepository,
                       PasswordEncoder passwordEncoder, JwtService jwtService, JwtProperties jwtProperties,
                       LoginRateLimiter rateLimiter, DemoGuard demoGuard) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.jwtProperties = jwtProperties;
        this.rateLimiter = rateLimiter;
        this.demoGuard = demoGuard;
        this.dummyHash = passwordEncoder.encode("dummy-password-for-timing");
    }

    @Transactional
    public AuthResponse login(LoginRequest req, HttpServletRequest request) {
        String email = UserService.normalizeEmail(req.email());
        rateLimiter.checkAndRecord(request, email);
        User user = userRepository.findByEmailIgnoreCase(email).orElse(null);
        if (user == null) {
            passwordEncoder.matches(req.password(), dummyHash);
            rateLimiter.recordFailure(email);
            throw ApiException.unauthorized(INVALID_CREDENTIALS);
        }
        if (!passwordEncoder.matches(req.password(), user.getPasswordHash()) || !canLogin(user)) {
            rateLimiter.recordFailure(email);
            throw ApiException.unauthorized(INVALID_CREDENTIALS);
        }
        rateLimiter.recordSuccess(email);
        return issueTokens(user);
    }

    /** Demo: entra directamente con una de las cuentas de demostración (sin contraseña). */
    @Transactional
    public AuthResponse demoLogin(String account, HttpServletRequest request) {
        String email = demoGuard.demoAccountEmail(account);
        rateLimiter.checkAndRecord(request, null);
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "La demo se está reiniciando. Intenta en unos segundos."));
        return issueTokens(user);
    }

    /**
     * El usuario cambia su propia contraseña. Se cierran todas sus sesiones (otros dispositivos incluidos)
     * y se devuelve un par de tokens nuevo para que la sesión actual siga activa.
     */
    @Transactional
    public AuthResponse changePassword(AuthUser actor, ChangePasswordRequest req) {
        User user = userRepository.findById(actor.id())
                .orElseThrow(() -> ApiException.unauthorized("Sesión inválida"));
        demoGuard.protectUser(user);
        if (!passwordEncoder.matches(req.currentPassword(), user.getPasswordHash())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "La contraseña actual no es correcta");
        }
        if (passwordEncoder.matches(req.newPassword(), user.getPasswordHash())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "La nueva contraseña debe ser distinta a la actual");
        }
        user.setPasswordHash(passwordEncoder.encode(req.newPassword()));
        refreshTokenRepository.revokeAllForUser(user.getId());
        log.info("Usuario {} cambió su contraseña", user.getId());
        return issueTokens(user);
    }

    /**
     * Rotación: cada refresh token sirve una sola vez. Si llega uno ya usado (posible robo),
     * se revocan todas las sesiones del usuario. noRollbackFor mantiene esa revocación aunque se lance el 401.
     */
    @Transactional(noRollbackFor = ApiException.class)
    public AuthResponse refresh(String rawToken) {
        RefreshToken rt = refreshTokenRepository.findByTokenHash(hash(rawToken))
                .orElseThrow(() -> ApiException.unauthorized(INVALID_REFRESH));

        User user = rt.getUser();
        if (rt.isRotated()) {
            // Ya fue canjeado antes: alguien más tiene una copia. Se cierran todas las sesiones.
            log.warn("Refresh token reutilizado para el usuario {}: se revocan todas sus sesiones", user.getId());
            refreshTokenRepository.revokeAllForUser(user.getId());
            throw ApiException.unauthorized(INVALID_REFRESH);
        }
        if (rt.isRevoked() || rt.isExpired() || !canLogin(user)) {
            // Revocado por logout / cambio de contraseña / admin, o vencido: solo se rechaza
            rt.setRevoked(true);
            throw ApiException.unauthorized(INVALID_REFRESH);
        }
        rt.setRevoked(true);
        rt.setRotated(true);
        return issueTokens(user);
    }

    /** Idempotente: un token desconocido simplemente se ignora. */
    @Transactional
    public void logout(String rawToken) {
        refreshTokenRepository.findByTokenHash(hash(rawToken)).ifPresent(rt -> rt.setRevoked(true));
    }

    /** Limpieza diaria de refresh tokens vencidos. */
    @Scheduled(cron = "0 0 4 * * *")
    @Transactional
    public void purgeExpiredTokens() {
        int n = refreshTokenRepository.deleteExpiredBefore(Instant.now());
        if (n > 0) {
            log.info("Eliminados {} refresh tokens vencidos", n);
        }
    }

    private AuthResponse issueTokens(User user) {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String rawRefresh = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        RefreshToken rt = new RefreshToken();
        rt.setUser(user);
        rt.setTokenHash(hash(rawRefresh));
        rt.setExpiresAt(Instant.now().plus(jwtProperties.refreshTtl()));
        refreshTokenRepository.save(rt);

        return new AuthResponse(jwtService.generateAccessToken(user), rawRefresh, "Bearer",
                jwtService.accessTtlSeconds(), UserResponse.from(user));
    }

    private static boolean canLogin(User user) {
        return user.isEnabled() && (user.getGym() == null || user.getGym().isActive());
    }

    private static String hash(String raw) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(raw.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
