package com.pck4x.sharedcontracts.security;

import com.pck4x.sharedcontracts.event.AccountRejectedEvent;
import com.pck4x.sharedcontracts.event.TransferNotificationEvent;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/** Fase 1.5: propagacion del correlation ID entre eventos. */
class CorrelationContextTest {

    @Test
    void copyTrasladaElCorrelationIdDelEventoOrigenAlDestino() {
        var origin = new AccountRejectedEvent();
        var id = UUID.randomUUID();
        origin.setCorrelationId(id);
        var target = new TransferNotificationEvent();

        CorrelationContext.copy(origin, target);

        assertEquals(id, target.getCorrelationId());
    }

    @Test
    void copyConOrigenSinCorrelationIdNoModificaElDestino() {
        var origin = new AccountRejectedEvent();
        var target = new TransferNotificationEvent();

        CorrelationContext.copy(origin, target);

        assertNull(target.getCorrelationId());
    }

    @Test
    void copyNoSobrescribeUnCorrelationIdYaPresente() {
        var origin = new AccountRejectedEvent();
        origin.setCorrelationId(UUID.randomUUID());
        var target = new TransferNotificationEvent();
        var existing = UUID.randomUUID();
        target.setCorrelationId(existing);

        CorrelationContext.copy(origin, target);

        assertEquals(existing, target.getCorrelationId());
    }
}
