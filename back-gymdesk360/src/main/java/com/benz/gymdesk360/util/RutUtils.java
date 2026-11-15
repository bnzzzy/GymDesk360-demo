package com.benz.gymdesk360.util;

import java.util.Locale;

/** RUT chileno: normalización a "12345678-K" y validación del dígito verificador (módulo 11). */
public final class RutUtils {

    private RutUtils() {
    }

    /**
     * Devuelve el RUT normalizado, o null si viene vacío.
     *
     * @throws IllegalArgumentException si el formato o el dígito verificador no son válidos
     */
    public static String normalize(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String clean = raw.replaceAll("[.\\s-]", "").toUpperCase(Locale.ROOT);
        if (!clean.matches("^\\d{7,8}[0-9K]$")) {
            throw new IllegalArgumentException("RUT con formato inválido");
        }
        String body = clean.substring(0, clean.length() - 1);
        char dv = clean.charAt(clean.length() - 1);
        if (computeDv(body) != dv) {
            throw new IllegalArgumentException("RUT inválido: el dígito verificador no coincide");
        }
        return body + "-" + dv;
    }

    public static char computeDv(String body) {
        int sum = 0;
        int factor = 2;
        for (int i = body.length() - 1; i >= 0; i--) {
            sum += (body.charAt(i) - '0') * factor;
            factor = factor == 7 ? 2 : factor + 1;
        }
        int r = 11 - (sum % 11);
        return r == 11 ? '0' : r == 10 ? 'K' : (char) ('0' + r);
    }
}
