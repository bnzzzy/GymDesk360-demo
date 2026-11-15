package com.benz.gymdesk360.dto.request;

import jakarta.validation.constraints.NotNull;

/** Activa o desactiva un gimnasio o un usuario. */
public record StatusRequest(@NotNull Boolean active) {
}
