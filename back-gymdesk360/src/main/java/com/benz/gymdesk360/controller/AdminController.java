package com.benz.gymdesk360.controller;

import com.benz.gymdesk360.dto.request.AdminUserRequest;
import com.benz.gymdesk360.dto.request.ResetPasswordRequest;
import com.benz.gymdesk360.dto.request.StatusRequest;
import com.benz.gymdesk360.dto.response.StatsResponse;
import com.benz.gymdesk360.dto.response.UserResponse;
import com.benz.gymdesk360.security.AuthUser;
import com.benz.gymdesk360.service.UserService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SUPER_ADMIN')")
@Tag(name = "Administración", description = "Panel del SUPER_ADMIN: usuarios de todos los gimnasios y métricas")
public class AdminController {

    private final UserService userService;

    @GetMapping("/stats")
    @Operation(summary = "Métricas generales de la plataforma")
    public StatsResponse stats() {
        return userService.stats();
    }

    @GetMapping("/users")
    @Operation(summary = "Lista usuarios (opcionalmente filtrados por gimnasio)")
    public List<UserResponse> listUsers(@RequestParam(required = false) Long gymId) {
        return userService.adminList(gymId);
    }

    @PostMapping("/users")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Crea un usuario en cualquier gimnasio")
    public UserResponse createUser(@Valid @RequestBody AdminUserRequest req) {
        return userService.adminCreate(req);
    }

    @PutMapping("/users/{id}")
    @Operation(summary = "Edita email, nombre, rol y gimnasio")
    public UserResponse updateUser(@AuthenticationPrincipal AuthUser actor, @PathVariable Long id,
                                   @Valid @RequestBody AdminUserRequest req) {
        return userService.adminUpdate(actor, id, req);
    }

    @PatchMapping("/users/{id}/status")
    @Operation(summary = "Activa o desactiva un usuario")
    public UserResponse setUserStatus(@AuthenticationPrincipal AuthUser actor, @PathVariable Long id,
                                      @Valid @RequestBody StatusRequest req) {
        return userService.adminSetEnabled(actor, id, req.active());
    }

    @PutMapping("/users/{id}/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Restablece la contraseña (cierra sus sesiones)")
    public void resetPassword(@PathVariable Long id, @Valid @RequestBody ResetPasswordRequest req) {
        userService.adminResetPassword(id, req.password());
    }

    @DeleteMapping("/users/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Elimina un usuario")
    public void deleteUser(@AuthenticationPrincipal AuthUser actor, @PathVariable Long id) {
        userService.adminDelete(actor, id);
    }
}
