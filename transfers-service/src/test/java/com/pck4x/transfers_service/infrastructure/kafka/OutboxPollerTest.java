package com.pck4x.transfers_service.infrastructure.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pck4x.sharedcontracts.command.TransferDebitCommand;
import com.pck4x.sharedcontracts.event.TransferRequestedEvent;
import com.pck4x.transfers_service.application.port.output.EventPublisher;
import com.pck4x.transfers_service.application.port.output.TransferEventRepository;
import com.pck4x.transfers_service.domain.TransferEvent;
import com.pck4x.transfers_service.domain.enums.OutboxStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** Fase 1.3: reintentos con backoff y DLQ del outbox de transfers. */
class OutboxPollerTest {

    private TransferEventRepository repository;
    private EventPublisher publisher;
    private OutboxPoller poller;

    @BeforeEach
    void setUp() {
        repository = mock(TransferEventRepository.class);
        publisher = mock(EventPublisher.class);
        poller = new OutboxPoller(repository, publisher, new ObjectMapper());
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    private TransferEvent event(int attempts) throws Exception {
        var transferId = UUID.randomUUID();
        var payload = new ObjectMapper().writeValueAsString(
                new TransferRequestedEvent(transferId, "111", "222",
                        new BigDecimal("10.00"), "USD", "test"));
        var event = new TransferEvent(transferId, "TransferRequested", payload);
        event.setId(7L);
        event.setAttempts(attempts);
        event.setNextAttemptAt(LocalDateTime.now().minusSeconds(1));
        return event;
    }

    @Test
    void publicaCorrectamenteYMarcaSENT() throws Exception {
        var event = event(0);
        when(repository.findRetryable(any())).thenReturn(List.of(event));

        poller.processPendingEvents();

        assertEquals(OutboxStatus.SENT, event.getStatus());
        assertNotNull(event.getSentAt());
        assertNull(event.getLastError());
        verify(publisher).publish(eq("bank.transfer.events"), any(TransferRequestedEvent.class));
    }

    @Test
    void comandoDeSagaSePublicaEnElTopicDeComandos() throws Exception {
        var transferId = UUID.randomUUID();
        var command = new TransferDebitCommand(transferId, "111", "222",
                new BigDecimal("10.00"), "USD", "test");
        var payload = new ObjectMapper().writeValueAsString(command);
        var event = new TransferEvent(transferId, TransferOutboxType.nameOf(command), payload);
        event.setId(8L);
        event.setNextAttemptAt(LocalDateTime.now().minusSeconds(1));
        when(repository.findRetryable(any())).thenReturn(List.of(event));

        poller.processPendingEvents();

        assertEquals(OutboxStatus.SENT, event.getStatus());
        verify(publisher).publish(eq("bank.transfer.commands"), any(TransferDebitCommand.class));
    }

    @Test
    void falloDePublicacionProgramaReintentoConBackoff() throws Exception {
        var event = event(1);
        when(repository.findRetryable(any())).thenReturn(List.of(event));
        doThrow(new IllegalStateException("Kafka down")).when(publisher)
                .publish(anyString(), any());

        var before = LocalDateTime.now();
        poller.processPendingEvents();

        assertEquals(OutboxStatus.PENDING, event.getStatus());
        assertEquals(2, event.getAttempts());
        assertEquals("Kafka down", event.getLastError());
        assertFalse(event.getNextAttemptAt().isBefore(before.plusSeconds(3)),
                "el segundo reintento debe esperar ~4 s");
        verify(repository).save(event);
    }

    @Test
    void quintoFalloDerivaADLQYSaleDeLaColaDeReintentos() throws Exception {
        var event = event(4);
        when(repository.findRetryable(any())).thenReturn(List.of(event));
        doThrow(new IllegalStateException("Kafka down")).when(publisher)
                .publish(anyString(), any());

        poller.processPendingEvents();

        assertEquals(OutboxStatus.FAILED, event.getStatus());
        assertEquals(5, event.getAttempts());
        assertTrue(event.getNextAttemptAt().isAfter(LocalDateTime.now().plusDays(300)));
    }

    @Test
    void payloadEnvenenadoNoSePublicaPeroSiSeContabilizaElIntento() throws Exception {
        var event = new TransferEvent(UUID.randomUUID(), "TransferRequested", "{json roto");
        event.setAttempts(0);
        when(repository.findRetryable(any())).thenReturn(List.of(event));

        poller.processPendingEvents();

        assertEquals(1, event.getAttempts());
        assertNotNull(event.getLastError());
        verifyNoInteractions(publisher);
    }
}
