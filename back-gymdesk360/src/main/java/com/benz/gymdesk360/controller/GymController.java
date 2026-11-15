package com.benz.gymdesk360.controller;

import com.benz.gymdesk360.dto.request.CreateGymRequest;
import com.benz.gymdesk360.dto.request.StatusRequest;
import com.benz.gymdesk360.dto.request.UpdateGymRequest;
import com.benz.gymdesk360.dto.response.GymResponse;
import com.benz.gymdesk360.service.GymService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/gyms")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SUPER_ADMIN')")
@Tag(name = "Gimnasios", description = "Administración de gimnasios (solo SUPER_ADMIN)")
public class GymController {

    private final GymService gymService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Crea un gimnasio y su usuario ADMIN")
    public GymResponse create(@Valid @RequestBody CreateGymRequest req) {
        return gymService.create(req);
    }

    @GetMapping
    @Operation(summary = "Lista todos los gimnasios")
    public List<GymResponse> list() {
        return gymService.findAll();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalle de un gimnasio")
    public GymResponse get(@PathVariable Long id) {
        return gymService.findById(id);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Edita nombre y slug")
    public GymResponse update(@PathVariable Long id, @Valid @RequestBody UpdateGymRequest req) {
        return gymService.update(id, req);
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Activa o desactiva el gimnasio (al desactivar se cierran las sesiones de sus usuarios)")
    public GymResponse setStatus(@PathVariable Long id, @Valid @RequestBody StatusRequest req) {
        return gymService.setActive(id, req.active());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Elimina el gimnasio y todos sus usuarios")
    public void delete(@PathVariable Long id) {
        gymService.delete(id);
    }
}
