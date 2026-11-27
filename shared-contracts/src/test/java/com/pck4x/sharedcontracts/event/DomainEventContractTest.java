package com.pck4x.sharedcontracts.event;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Fase 1.2: todos los eventos deben poder registrarse en el outbox por eventId. */
class DomainEventContractTest {

    private static final List<DomainEvent> EVENTS = List.of(
            new AccountCreatedEvent(), new AccountCreditedEvent(), new AccountDebitedEvent(),
            new AccountCreditFailedEvent(), new AccountDebitCompensatedEvent(),
            new AccountDepositedEvent(), new AccountRejectedEvent(), new TransferCompletedEvent(),
            new TransferFailedEvent(), new TransferNotificationEvent(),
            new TransferRequestedEvent(), new UserCreatedEvent()
    );

    @Test
    void losEventosImplementanDomainEventConEventIdNoNulo() {
        assertEquals(12, EVENTS.size());
        for (DomainEvent event : EVENTS) {
            assertNotNull(event.getEventId(), event.getClass().getSimpleName());
        }
    }

    @Test
    void cadaEventoTieneUnEventIdUnico() {
        assertEquals(EVENTS.size(), EVENTS.stream().map(DomainEvent::getEventId).distinct().count());
    }
}
