package com.benz.gymdesk360.dto.response;

import java.util.List;

/**
 * @param activeMembers      socios con membresía vigente hoy
 * @param totalMembers       socios registrados (activos en la ficha)
 * @param expiringSoon       membresías vigentes que vencen en los próximos 7 días
 * @param expiredLast30Days  socios cuya última membresía venció en los últimos 30 días y no renovaron
 * @param newMembersThisMonth socios registrados este mes
 * @param salesThisMonth     membresías vendidas este mes (no anuladas)
 * @param revenueThisMonth   total vendido este mes en pesos
 */
public record GymDashboardResponse(
        String gymName,
        long activeMembers,
        long totalMembers,
        long expiredLast30Days,
        long newMembersThisMonth,
        long salesThisMonth,
        long revenueThisMonth,
        long activePlans,
        List<MemberResponse> expiringSoon
) {
}
