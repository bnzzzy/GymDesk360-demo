package com.benz.gymdesk360.dto.response;

import com.benz.gymdesk360.model.Plan;
import com.benz.gymdesk360.model.enums.DurationUnit;

import java.time.Instant;

public record PlanResponse(Long id, String name, String description, long price, int durationValue,
                           DurationUnit durationUnit, boolean active, Instant createdAt) {
    public static PlanResponse from(Plan p) {
        return new PlanResponse(p.getId(), p.getName(), p.getDescription(), p.getPrice(), p.getDurationValue(),
                p.getDurationUnit(), p.isActive(), p.getCreatedAt());
    }
}
