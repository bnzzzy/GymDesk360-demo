package com.benz.gymdesk360.security;

import com.benz.gymdesk360.model.enums.Role;

/**
 * Usuario autenticado, reconstruido a partir del access token (sin ir a la base de datos).
 * Se obtiene en los controllers con {@code @AuthenticationPrincipal AuthUser user}.
 *
 * @param gymId null solo para SUPER_ADMIN
 */
public record AuthUser(Long id, Long gymId, String email, Role role) {
}
