package com.benz.gymdesk360.controller;

import com.benz.gymdesk360.dto.request.MemberRequest;
import com.benz.gymdesk360.dto.request.MembershipRequest;
import com.benz.gymdesk360.dto.request.StatusRequest;
import com.benz.gymdesk360.dto.response.MemberDetailResponse;
import com.benz.gymdesk360.dto.response.MemberResponse;
import com.benz.gymdesk360.dto.response.MembershipResponse;
import com.benz.gymdesk360.model.enums.MembershipStatus;
import com.benz.gymdesk360.security.AuthUser;
import com.benz.gymdesk360.service.MemberService;
import com.benz.gymdesk360.service.MembershipService;

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
@RequestMapping("/api/members")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
@Tag(name = "Socios", description = "Socios del gimnasio y venta de membresías")
public class MemberController {

    private final MemberService memberService;
    private final MembershipService membershipService;

    @GetMapping
    @Operation(summary = "Lista socios con el estado de su membresía")
    public List<MemberResponse> list(@AuthenticationPrincipal AuthUser actor,
                                     @RequestParam(required = false) String q,
                                     @RequestParam(required = false) MembershipStatus status) {
        return memberService.list(actor, q, status);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Ficha del socio con su historial de membresías")
    public MemberDetailResponse detail(@AuthenticationPrincipal AuthUser actor, @PathVariable Long id) {
        return memberService.detail(actor, id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Registra un socio")
    public MemberResponse create(@AuthenticationPrincipal AuthUser actor, @Valid @RequestBody MemberRequest req) {
        return memberService.create(actor, req);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Edita los datos del socio")
    public MemberResponse update(@AuthenticationPrincipal AuthUser actor, @PathVariable Long id,
                                 @Valid @RequestBody MemberRequest req) {
        return memberService.update(actor, id, req);
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Activa o desactiva la ficha del socio")
    public MemberResponse setStatus(@AuthenticationPrincipal AuthUser actor, @PathVariable Long id,
                                    @Valid @RequestBody StatusRequest req) {
        return memberService.setActive(actor, id, req.active());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Elimina un socio sin membresías")
    public void delete(@AuthenticationPrincipal AuthUser actor, @PathVariable Long id) {
        memberService.delete(actor, id);
    }

    @PostMapping("/{id}/memberships")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Vende un plan al socio (sin fecha: hoy o renovación tras la vigente)")
    public MembershipResponse sell(@AuthenticationPrincipal AuthUser actor, @PathVariable Long id,
                                   @Valid @RequestBody MembershipRequest req) {
        return membershipService.create(actor, id, req);
    }
}
