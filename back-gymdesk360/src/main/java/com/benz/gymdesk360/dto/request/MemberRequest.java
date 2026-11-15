package com.benz.gymdesk360.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record MemberRequest(
        @Size(max = 14) String rut,
        @NotBlank @Size(max = 80) String firstName,
        @NotBlank @Size(max = 80) String lastName,
        @Email @Size(max = 255) String email,
        @Size(max = 30) String phone,
        @Past LocalDate birthDate,
        @Size(max = 150) String emergencyContactName,
        @Size(max = 30) String emergencyContactPhone,
        @Size(max = 1000) String notes
) {
}
