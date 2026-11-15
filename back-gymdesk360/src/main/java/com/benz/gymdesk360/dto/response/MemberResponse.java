package com.benz.gymdesk360.dto.response;

import com.benz.gymdesk360.model.Member;
import com.benz.gymdesk360.model.Membership;
import com.benz.gymdesk360.model.enums.MembershipStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Socio con el resumen de su membresía más relevante (vigente, si no la próxima, si no la última).
 *
 * @param daysLeft  días que le quedan a la membresía vigente (0 = vence hoy); null si no está activo
 * @param paidUntil último día cubierto por membresías vigentes o ya pagadas a futuro (null si no hay)
 */
public record MemberResponse(
        Long id,
        String rut,
        String firstName,
        String lastName,
        String fullName,
        String email,
        String phone,
        LocalDate birthDate,
        String emergencyContactName,
        String emergencyContactPhone,
        String notes,
        boolean active,
        Instant createdAt,
        MembershipStatus status,
        String currentPlanName,
        LocalDate currentStartDate,
        LocalDate currentEndDate,
        Long daysLeft,
        LocalDate paidUntil
) {
    public static MemberResponse from(Member m, Membership current, LocalDate paidUntil, LocalDate today) {
        MembershipStatus status = current == null ? MembershipStatus.NONE : current.statusOn(today);
        Long daysLeft = status == MembershipStatus.ACTIVE ? ChronoUnit.DAYS.between(today, current.getEndDate()) : null;
        return new MemberResponse(m.getId(), m.getRut(), m.getFirstName(), m.getLastName(), m.getFullName(),
                m.getEmail(), m.getPhone(), m.getBirthDate(), m.getEmergencyContactName(),
                m.getEmergencyContactPhone(), m.getNotes(), m.isActive(), m.getCreatedAt(), status,
                current == null ? null : current.getPlanName(),
                current == null ? null : current.getStartDate(),
                current == null ? null : current.getEndDate(),
                daysLeft, paidUntil);
    }
}
