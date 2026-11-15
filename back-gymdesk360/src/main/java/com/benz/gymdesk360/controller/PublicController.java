package com.benz.gymdesk360.controller;

import com.benz.gymdesk360.demo.DemoGuard;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public")
@RequiredArgsConstructor
@Tag(name = "Público")
public class PublicController {

    private final DemoGuard demoGuard;

    /** @param demo true si esta instancia es la demo pública (el frontend muestra el login con un clic) */
    public record PublicConfig(boolean demo) {
    }

    @GetMapping("/config")
    @SecurityRequirements
    @Operation(summary = "Configuración pública de la instancia")
    public PublicConfig config() {
        return new PublicConfig(demoGuard.enabled());
    }
}
