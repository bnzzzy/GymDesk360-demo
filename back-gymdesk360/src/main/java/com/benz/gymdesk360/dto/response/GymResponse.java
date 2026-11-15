package com.benz.gymdesk360.dto.response;

import com.benz.gymdesk360.model.Gym;

import java.time.Instant;

public record GymResponse(Long id, String name, String slug, boolean active, long usersCount, Instant createdAt) {
    public static GymResponse from(Gym g, long usersCount) {
        return new GymResponse(g.getId(), g.getName(), g.getSlug(), g.isActive(), usersCount, g.getCreatedAt());
    }
}
