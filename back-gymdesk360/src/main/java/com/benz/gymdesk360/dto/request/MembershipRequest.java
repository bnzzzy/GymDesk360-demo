package com.benz.gymdesk360.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * Venta de un plan a un socio.
 *
 * @param startDate opcional: si no viene, empieza hoy o, si el socio tiene una membresía vigente/futura,
 *                  el día siguiente a que termine (renovación)
 * @param pricePaid opcional: si no viene se cobra el precio del plan (permite descuentos)
 */
public record MembershipRequest(
        @NotNull Long planId,
        LocalDate startDate,
        @Min(0) @Max(100_000_000) Long pricePaid,
        @Size(max = 500) String notes
) {
}
