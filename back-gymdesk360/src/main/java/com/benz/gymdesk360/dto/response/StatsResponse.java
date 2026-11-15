package com.benz.gymdesk360.dto.response;

import com.benz.gymdesk360.model.enums.Role;

import java.util.Map;

public record StatsResponse(
        long totalGyms,
        long activeGyms,
        long totalUsers,
        long totalMembers,
        Map<Role, Long> usersByRole
) {
}
