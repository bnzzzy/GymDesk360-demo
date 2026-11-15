package com.benz.gymdesk360.dto.request;

import com.benz.gymdesk360.model.enums.Role;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Alta / edición de usuarios por el SUPER_ADMIN.
 * password es obligatorio al crear y se ignora al editar (para eso está el reset de contraseña).
 * gymId es obligatorio salvo para SUPER_ADMIN.
 */
public record AdminUserRequest(
        @NotBlank @Email @Size(max = 255) String email,
        @Size(min = 8, max = 72) String password,
        @NotBlank @Size(max = 150) String fullName,
        @NotNull Role role,
        Long gymId
) {
}
