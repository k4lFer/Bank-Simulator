package com.pck4x.ledger_service.application.mapper;

import com.pck4x.ledger_service.domain.LedgerEntries;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Fase 2.6: un asiento REV devuelve saldo y debe sumar al balance del ledger. */
class LedgerDtoMapperTest {

    @Test
    void reversionSeCuentaComoCreditoEnElBalance() {
        var transferId = UUID.randomUUID();
        var entries = List.of(
                new LedgerEntries(transferId, "111", "CR", new BigDecimal("500.00"), "USD"),
                new LedgerEntries(transferId, "111", "DR", new BigDecimal("200.00"), "USD"),
                new LedgerEntries(transferId, "111", "REV", new BigDecimal("200.00"), "USD")
        );

        assertEquals(new BigDecimal("500.00"), LedgerDtoMapper.INSTANCE.calculateBalance(entries));
    }
}
