package com.benz.gymdesk360.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateGymRequest(
        @NotBlank @Size(max = 150) String name,
        @NotBlank @Size(max = 80)
        @Pattern(regexp = "^[a-z0-9]+(-[a-z0-9]+)*$", message = "solo minúsculas, números y guiones")
        String slug,
        @NotBlank @Email @Size(max = 255) String adminEmail,
        @NotBlank @Size(min = 8, max = 72) String adminPassword,
        @NotBlank @Size(max = 150) String adminFullName
) {
}
