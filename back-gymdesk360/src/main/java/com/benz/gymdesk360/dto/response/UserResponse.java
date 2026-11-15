package com.benz.gymdesk360.dto.response;

import com.benz.gymdesk360.model.User;
import com.benz.gymdesk360.model.enums.Role;

import java.time.Instant;

/** gymName requiere que el gimnasio esté cargado (llamar dentro de una transacción). */
public record UserResponse(Long id, Long gymId, String gymName, String email, String fullName, Role role,
                           boolean enabled, Instant createdAt) {
    public static UserResponse from(User u) {
        return new UserResponse(u.getId(), u.getGymId(), u.getGym() == null ? null : u.getGym().getName(),
                u.getEmail(), u.getFullName(), u.getRole(), u.isEnabled(), u.getCreatedAt());
    }
}
