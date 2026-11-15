package com.benz.gymdesk360.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateGymRequest(
        @NotBlank @Size(max = 150) String name,
        @NotBlank @Size(max = 80)
        @Pattern(regexp = "^[a-z0-9]+(-[a-z0-9]+)*$", message = "solo minúsculas, números y guiones")
        String slug
) {
}
