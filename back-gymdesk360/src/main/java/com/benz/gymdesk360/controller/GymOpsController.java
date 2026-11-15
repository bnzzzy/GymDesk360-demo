package com.benz.gymdesk360.controller;

import com.benz.gymdesk360.dto.response.GymDashboardResponse;
import com.benz.gymdesk360.dto.response.MembershipResponse;
import com.benz.gymdesk360.security.AuthUser;
import com.benz.gymdesk360.service.GymDashboardService;
import com.benz.gymdesk360.service.MembershipService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
@Tag(name = "Gimnasio", description = "Panel y operaciones del gimnasio")
public class GymOpsController {

    private final GymDashboardService dashboardService;
    private final MembershipService membershipService;

    @GetMapping("/gym/dashboard")
    @Operation(summary = "Métricas del gimnasio: socios activos, por vencer, ventas del mes")
    public GymDashboardResponse dashboard(@AuthenticationPrincipal AuthUser actor) {
        return dashboardService.get(actor);
    }

    @PatchMapping("/memberships/{id}/cancel")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Anula una membresía vendida")
    public MembershipResponse cancel(@AuthenticationPrincipal AuthUser actor, @PathVariable Long id) {
        return membershipService.cancel(actor, id);
    }
}
