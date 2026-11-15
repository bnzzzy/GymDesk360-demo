package com.benz.gymdesk360.model;

import com.benz.gymdesk360.model.enums.MembershipStatus;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;

/** Venta de un plan a un socio. Guarda nombre y precio del plan tal como estaban al venderlo. */
@Entity
@Table(name = "memberships")
@Getter
@Setter
@NoArgsConstructor
public class Membership {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "gym_id", nullable = false, updatable = false)
    private Long gymId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false, updatable = false)
    private Member member;

    @Column(name = "plan_id", nullable = false, updatable = false)
    private Long planId;

    @Column(name = "plan_name", nullable = false, length = 100)
    private String planName;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "price_paid", nullable = false)
    private Long pricePaid;

    @Column(length = 500)
    private String notes;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public boolean isCancelled() {
        return cancelledAt != null;
    }

    public MembershipStatus statusOn(LocalDate today) {
        if (isCancelled()) return MembershipStatus.CANCELLED;
        if (today.isBefore(startDate)) return MembershipStatus.UPCOMING;
        if (today.isAfter(endDate)) return MembershipStatus.EXPIRED;
        return MembershipStatus.ACTIVE;
    }
}
