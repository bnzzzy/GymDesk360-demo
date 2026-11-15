package com.benz.gymdesk360.model;

import com.benz.gymdesk360.model.enums.DurationUnit;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "plans")
@Getter
@Setter
@NoArgsConstructor
public class Plan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "gym_id", nullable = false, updatable = false)
    private Long gymId;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 500)
    private String description;

    /** Pesos chilenos. */
    @Column(nullable = false)
    private Long price;

    @Column(name = "duration_value", nullable = false)
    private Integer durationValue;

    @Enumerated(EnumType.STRING)
    @Column(name = "duration_unit", nullable = false, length = 10)
    private DurationUnit durationUnit;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    /** Último día (inclusive) de una membresía de este plan que empieza en {@code start}. */
    public LocalDate endDateFor(LocalDate start) {
        return switch (durationUnit) {
            case MONTHS -> start.plusMonths(durationValue).minusDays(1);
            case DAYS -> start.plusDays(durationValue - 1L);
        };
    }
}
