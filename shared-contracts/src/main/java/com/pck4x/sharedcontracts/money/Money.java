package com.pck4x.sharedcontracts.money;

import java.math.BigDecimal;

/**
 * Fase 1.6: value object para importe + moneda.
 * Centraliza la validacion que antes estaba repetida en transfers, accounts y ledger.
 */
public record Money(BigDecimal amount, String currency) {

    public Money {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero");
        }
        if (amount.scale() > 2) {
            throw new IllegalArgumentException("Amount supports at most 2 decimal places");
        }
        if (currency == null || !currency.matches("[A-Z]{3}")) {
            throw new IllegalArgumentException("Currency must be a 3-letter uppercase code");
        }
    }

    public static Money of(BigDecimal amount, String currency) {
        return new Money(amount, currency);
    }
}
