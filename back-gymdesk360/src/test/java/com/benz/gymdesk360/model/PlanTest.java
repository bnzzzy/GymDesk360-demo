package com.benz.gymdesk360.model;

import com.benz.gymdesk360.model.enums.DurationUnit;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class PlanTest {

    @ParameterizedTest(name = "{0} {1} desde {2} -> hasta {3}")
    @CsvSource({
            "1,  MONTHS, 2026-11-15, 2026-12-14",
            "1,  MONTHS, 2026-01-31, 2026-02-27",   // fin de mes: 28/02 - 1
            "1,  MONTHS, 2028-01-31, 2028-02-28",   // bisiesto: 29/02 - 1
            "3,  MONTHS, 2026-11-01, 2027-01-31",
            "12, MONTHS, 2026-03-01, 2027-02-28",
            "1,  DAYS,   2026-11-15, 2026-11-15",   // pase diario: mismo día
            "7,  DAYS,   2026-11-15, 2026-11-21",
            "30, DAYS,   2026-12-20, 2027-01-18"
    })
    void calculaFechaDeTerminoInclusive(int value, DurationUnit unit, LocalDate start, LocalDate expectedEnd) {
        Plan plan = new Plan();
        plan.setDurationValue(value);
        plan.setDurationUnit(unit);
        assertThat(plan.endDateFor(start)).isEqualTo(expectedEnd);
    }
}
