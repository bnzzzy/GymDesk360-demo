package com.benz.gymdesk360.security;

import com.benz.gymdesk360.exception.ApiException;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Freno a la fuerza bruta en el login (en memoria, suficiente para una sola instancia):
 * - por IP: máximo {@value #MAX_PER_IP} intentos cada 10 minutos
 * - por email: máximo {@value #MAX_FAILS_PER_EMAIL} contraseñas incorrectas cada 15 minutos
 */
@Component
public class LoginRateLimiter {

    private static final int MAX_PER_IP = 20;
    private static final Duration IP_WINDOW = Duration.ofMinutes(10);
    private static final int MAX_FAILS_PER_EMAIL = 5;
    private static final Duration EMAIL_WINDOW = Duration.ofMinutes(15);

    private final Map<String, Deque<Long>> hits = new ConcurrentHashMap<>();

    /** Registra un intento desde la IP y lanza 429 si se superó algún límite. */
    public void checkAndRecord(HttpServletRequest request, String email) {
        long now = System.currentTimeMillis();
        String ipKey = "ip:" + clientIp(request);
        if (count(ipKey, IP_WINDOW, now) >= MAX_PER_IP) {
            throw tooMany(IP_WINDOW);
        }
        if (email != null && count(emailKey(email), EMAIL_WINDOW, now) >= MAX_FAILS_PER_EMAIL) {
            throw tooMany(EMAIL_WINDOW);
        }
        add(ipKey, now);
    }

    public void recordFailure(String email) {
        add(emailKey(email), System.currentTimeMillis());
    }

    public void recordSuccess(String email) {
        hits.remove(emailKey(email));
    }

    /** Detrás de Cloudflare Tunnel la IP real llega en CF-Connecting-IP (la API solo escucha en 127.0.0.1). */
    public static String clientIp(HttpServletRequest request) {
        String cf = request.getHeader("CF-Connecting-IP");
        if (cf != null && !cf.isBlank()) return cf.trim();
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) return xff.split(",")[0].trim();
        return request.getRemoteAddr();
    }

    @Scheduled(fixedDelay = 10 * 60 * 1000)
    void cleanup() {
        long now = System.currentTimeMillis();
        long maxWindow = Math.max(IP_WINDOW.toMillis(), EMAIL_WINDOW.toMillis());
        hits.entrySet().removeIf(e -> {
            synchronized (e.getValue()) {
                return e.getValue().isEmpty() || now - e.getValue().peekLast() > maxWindow;
            }
        });
    }

    private int count(String key, Duration window, long now) {
        Deque<Long> q = hits.get(key);
        if (q == null) return 0;
        synchronized (q) {
            while (!q.isEmpty() && now - q.peekFirst() > window.toMillis()) q.pollFirst();
            return q.size();
        }
    }

    private void add(String key, long now) {
        Deque<Long> q = hits.computeIfAbsent(key, k -> new ArrayDeque<>());
        synchronized (q) {
            q.addLast(now);
        }
    }

    private static String emailKey(String email) {
        return "fail:" + email.trim().toLowerCase(Locale.ROOT);
    }

    private static ApiException tooMany(Duration window) {
        return new ApiException(HttpStatus.TOO_MANY_REQUESTS,
                "Demasiados intentos. Espera " + window.toMinutes() + " minutos e inténtalo de nuevo.");
    }
}
