package com.benz.gymdesk360.controller;

import com.benz.gymdesk360.dto.request.CreateUserRequest;
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
@RequestMapping("/api/users")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
@Tag(name = "Usuarios", description = "Usuarios del gimnasio del usuario autenticado")
public class UserController {

    private final UserService userService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Crea un usuario en mi gimnasio")
    public UserResponse create(@AuthenticationPrincipal AuthUser actor, @Valid @RequestBody CreateUserRequest req) {
        return userService.createInOwnGym(actor, req);
    }

    @GetMapping
    @Operation(summary = "Lista los usuarios de mi gimnasio")
    public List<UserResponse> list(@AuthenticationPrincipal AuthUser actor) {
        return userService.listOwnGym(actor);
    }
}
