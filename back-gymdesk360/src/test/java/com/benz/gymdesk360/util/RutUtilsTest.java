package com.benz.gymdesk360.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RutUtilsTest {

    @ParameterizedTest
    @CsvSource({
            "'12.345.678-5', 12345678-5",
            "'12345678-5',   12345678-5",
            "'123456785',    12345678-5",
            "' 7.654.321-6', 7654321-6",
            "'11.111.111-1', 11111111-1",
            "'15.672.842-k', 15672842-K",   // dígito K en minúscula
            "'20.000.008-0', 20000008-0"    // dígito 0 (resto 11)
    })
    void normalizaRutsValidos(String raw, String expected) {
        assertThat(RutUtils.normalize(raw)).isEqualTo(expected);
    }

    @ParameterizedTest
    @ValueSource(strings = {"12.345.678-9", "11.111.111-2", "15.672.842-1"})
    void rechazaDigitoVerificadorIncorrecto(String raw) {
        assertThatThrownBy(() -> RutUtils.normalize(raw)).hasMessageContaining("dígito verificador");
    }

    @ParameterizedTest
    @ValueSource(strings = {"abc", "123-4", "12.345.678-X", "1234567890123"})
    void rechazaFormatoInvalido(String raw) {
        assertThatThrownBy(() -> RutUtils.normalize(raw)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void vacioEsNull() {
        assertThat(RutUtils.normalize(null)).isNull();
        assertThat(RutUtils.normalize("   ")).isNull();
    }
}
