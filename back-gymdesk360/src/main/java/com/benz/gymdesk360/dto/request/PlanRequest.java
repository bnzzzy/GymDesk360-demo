package com.benz.gymdesk360.dto.request;

import com.benz.gymdesk360.model.enums.DurationUnit;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PlanRequest(
        @NotBlank @Size(max = 100) String name,
        @Size(max = 500) String description,
        @NotNull @Min(0) @Max(100_000_000) Long price,
        @NotNull @Min(1) @Max(730) Integer durationValue,
        @NotNull DurationUnit durationUnit
) {
}
