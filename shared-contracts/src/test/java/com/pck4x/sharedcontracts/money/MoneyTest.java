package com.pck4x.sharedcontracts.money;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/** Fase 1.6: el value object es la unica fuente de validacion de importe + moneda. */
class MoneyTest {

    @Test
    void aceptaImportePositivoConDosDecimales() {
        var money = Money.of(new BigDecimal("123.45"), "USD");

        assertEquals(0, new BigDecimal("123.45").compareTo(money.amount()));
        assertEquals("USD", money.currency());
    }

    @Test
    void rechazaImporteNuloOCeroONegativo() {
        assertThrows(IllegalArgumentException.class, () -> Money.of(null, "USD"));
        assertThrows(IllegalArgumentException.class, () -> Money.of(BigDecimal.ZERO, "USD"));
        assertThrows(IllegalArgumentException.class, () -> Money.of(new BigDecimal("-1.00"), "USD"));
    }

    @Test
    void rechazaMasDeDosDecimales() {
        assertThrows(IllegalArgumentException.class,
                () -> Money.of(new BigDecimal("1.001"), "USD"));
    }

    @Test
    void rechazaMonedaInvalida() {
        assertThrows(IllegalArgumentException.class, () -> Money.of(BigDecimal.ONE, null));
        assertThrows(IllegalArgumentException.class, () -> Money.of(BigDecimal.ONE, "usd"));
        assertThrows(IllegalArgumentException.class, () -> Money.of(BigDecimal.ONE, "US"));
        assertThrows(IllegalArgumentException.class, () -> Money.of(BigDecimal.ONE, "USDX"));
    }
}
