package com.pck4x.accounts_service.infrastructure.kafka;

import com.pck4x.sharedcontracts.event.AccountCreditedEvent;
import com.pck4x.sharedcontracts.event.AccountDebitedEvent;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Fase 1.8: resolucion tipada de eventos del outbox (sin Class.forName). */
class AccountEventTypeTest {

    @Test
    void nameOFuncionaParaInstanciasConocidas() {
        assertEquals("ACCOUNT_DEBITED", AccountEventType.nameOf(new AccountDebitedEvent()));
        assertEquals("ACCOUNT_CREDITED", AccountEventType.nameOf(new AccountCreditedEvent()));
    }

    @Test
    void resolveAceptaEnumSimpleYFullyQualifiedName() {
        assertEquals(AccountDebitedEvent.class, AccountEventType.resolve("ACCOUNT_DEBITED"));
        assertEquals(AccountDebitedEvent.class, AccountEventType.resolve("AccountDebitedEvent"));
        assertEquals(AccountDebitedEvent.class,
                AccountEventType.resolve(AccountDebitedEvent.class.getName()));
    }

    @Test
    void tiposDesconocidosSeRechazanSinCargarClasesArbitrarias() {
        assertThrows(IllegalArgumentException.class, () -> AccountEventType.resolve("java.lang.Runtime"));
        assertThrows(IllegalArgumentException.class, () -> AccountEventType.resolve(null));
    }
}
