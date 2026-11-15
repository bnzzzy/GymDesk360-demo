package com.benz.gymdesk360.controller;

import com.benz.gymdesk360.dto.request.ChangePasswordRequest;
import com.benz.gymdesk360.dto.request.LoginRequest;
import com.benz.gymdesk360.dto.request.RefreshRequest;
import com.benz.gymdesk360.dto.response.AuthResponse;
import com.benz.gymdesk360.dto.response.UserResponse;
import com.benz.gymdesk360.security.AuthUser;
import com.benz.gymdesk360.service.AuthService;
import com.benz.gymdesk360.service.UserService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Autenticación")
public class AuthController {

    private final AuthService authService;
    private final UserService userService;

    @PostMapping("/login")
    @SecurityRequirements
    @Operation(summary = "Inicia sesión y devuelve access + refresh token")
    public AuthResponse login(@Valid @RequestBody LoginRequest req, HttpServletRequest request) {
        return authService.login(req, request);
    }

    @PostMapping("/demo/{account}")
    @SecurityRequirements
    @Operation(summary = "Solo en modo demo: entra como super-admin, admin o staff de la demo")
    public AuthResponse demoLogin(@PathVariable String account, HttpServletRequest request) {
        return authService.demoLogin(account, request);
    }

    @PostMapping("/refresh")
    @SecurityRequirements
    @Operation(summary = "Canjea un refresh token por un par nuevo (el anterior queda invalidado)")
    public AuthResponse refresh(@Valid @RequestBody RefreshRequest req) {
        return authService.refresh(req.refreshToken());
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @SecurityRequirements
    @Operation(summary = "Revoca el refresh token")
    public void logout(@Valid @RequestBody RefreshRequest req) {
        authService.logout(req.refreshToken());
    }

    @GetMapping("/me")
    @Operation(summary = "Datos del usuario autenticado")
    public UserResponse me(@AuthenticationPrincipal AuthUser user) {
        return userService.getById(user.id());
    }

    @PutMapping("/password")
    @Operation(summary = "Cambia la contraseña propia; cierra las demás sesiones y devuelve tokens nuevos")
    public AuthResponse changePassword(@AuthenticationPrincipal AuthUser user,
                                       @Valid @RequestBody ChangePasswordRequest req) {
        return authService.changePassword(user, req);
    }
}
