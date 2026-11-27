package com.pck4x.sharedcontracts.command;

import com.pck4x.sharedcontracts.event.AccountDebitedEvent;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/** Fase 2.3: los comandos se identifican de forma unica para dedupe por Inbox. */
class SagaCommandContractTest {

    private static final List<SagaCommand> COMMANDS = List.of(
            new TransferDebitCommand(),
            new TransferCreditCommand(),
            new TransferCompensateCommand()
    );

    @Test
    void todosLosComandosTienenCommandIdUnico() {
        assertEquals(COMMANDS.size(), COMMANDS.stream().map(SagaCommand::getCommandId).distinct().count());
        for (SagaCommand command : COMMANDS) {
            assertNotNull(command.getCommandId(), command.getClass().getSimpleName());
        }
    }

    @Test
    void losComandosLlevanTransferIdYCorrelationId() {
        var transferId = UUID.randomUUID();
        var correlationId = UUID.randomUUID();
        var command = new TransferDebitCommand(transferId, "111", "222", null, "PEN", "test");
        command.setCorrelationId(correlationId);

        assertEquals(transferId, command.getTransferId());
        assertEquals(correlationId, command.getCorrelationId());
        assertEquals("INTERNAL", command.getTransferType());

        // Un comando nunca es un evento: no comparte el contrato DomainEvent.
        assertFalse(command instanceof com.pck4x.sharedcontracts.event.DomainEvent);
        assertTrue(new AccountDebitedEvent() instanceof com.pck4x.sharedcontracts.event.DomainEvent);
    }
}
