package com.benz.gymdesk360.controller;

import com.benz.gymdesk360.dto.request.PlanRequest;
import com.benz.gymdesk360.dto.request.StatusRequest;
import com.benz.gymdesk360.dto.response.PlanResponse;
import com.benz.gymdesk360.security.AuthUser;
import com.benz.gymdesk360.service.PlanService;

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
@RequestMapping("/api/plans")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
@Tag(name = "Planes", description = "Planes del gimnasio (STAFF solo lectura)")
public class PlanController {

    private final PlanService planService;

    @GetMapping
    @Operation(summary = "Lista los planes del gimnasio (activos primero)")
    public List<PlanResponse> list(@AuthenticationPrincipal AuthUser actor) {
        return planService.list(actor);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Crea un plan")
    public PlanResponse create(@AuthenticationPrincipal AuthUser actor, @Valid @RequestBody PlanRequest req) {
        return planService.create(actor, req);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Edita un plan (no afecta membresías ya vendidas)")
    public PlanResponse update(@AuthenticationPrincipal AuthUser actor, @PathVariable Long id,
                               @Valid @RequestBody PlanRequest req) {
        return planService.update(actor, id, req);
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Activa o desactiva un plan (inactivo = no se puede vender)")
    public PlanResponse setStatus(@AuthenticationPrincipal AuthUser actor, @PathVariable Long id,
                                  @Valid @RequestBody StatusRequest req) {
        return planService.setActive(actor, id, req.active());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Elimina un plan sin ventas")
    public void delete(@AuthenticationPrincipal AuthUser actor, @PathVariable Long id) {
        planService.delete(actor, id);
    }
}
