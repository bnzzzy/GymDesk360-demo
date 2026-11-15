package com.benz.gymdesk360.dto.response;

import com.benz.gymdesk360.model.Membership;
import com.benz.gymdesk360.model.enums.MembershipStatus;

import java.time.Instant;
import java.time.LocalDate;

public record MembershipResponse(Long id, Long memberId, Long planId, String planName, LocalDate startDate,
                                 LocalDate endDate, long pricePaid, MembershipStatus status, String notes,
                                 Instant cancelledAt, Instant createdAt) {
    public static MembershipResponse from(Membership m, LocalDate today) {
        return new MembershipResponse(m.getId(), m.getMember().getId(), m.getPlanId(), m.getPlanName(),
                m.getStartDate(), m.getEndDate(), m.getPricePaid(), m.statusOn(today), m.getNotes(),
                m.getCancelledAt(), m.getCreatedAt());
    }
}
