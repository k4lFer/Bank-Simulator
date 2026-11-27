package com.pck4x.transfers_service.application.feature.saga;

import com.pck4x.sharedcontracts.command.TransferCompensateCommand;
import com.pck4x.sharedcontracts.command.TransferCreditCommand;
import com.pck4x.sharedcontracts.event.AccountCreditedEvent;
import com.pck4x.sharedcontracts.event.AccountCreditFailedEvent;
import com.pck4x.sharedcontracts.event.AccountDebitCompensatedEvent;
import com.pck4x.sharedcontracts.event.AccountDebitedEvent;
import com.pck4x.sharedcontracts.event.AccountRejectedEvent;
import com.pck4x.sharedcontracts.event.TransferNotificationEvent;
import com.pck4x.transfers_service.application.event.TransferStatusEvent;
import com.pck4x.transfers_service.application.port.output.EventPublisher;
import com.pck4x.transfers_service.application.port.output.ProcessedEventRepository;
import com.pck4x.transfers_service.application.port.output.TransferRepository;
import com.pck4x.transfers_service.domain.Transfer;
import com.pck4x.transfers_service.domain.enums.TransferStatus;
import com.pck4x.transfers_service.infrastructure.kafka.TransferCommandPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Fase 2.2 / 2.5 / 2.7 / 2.8: orquestador, compensacion y notificaciones terminales. */
class TransferSagaOrchestratorTest {

    private TransferRepository transferRepository;
    private ProcessedEventRepository processedEvents;
    private TransferCommandPublisher commandPublisher;
    private EventPublisher kafkaPublisher;
    private ApplicationEventPublisher statusPublisher;
    private TransferSagaOrchestrator orchestrator;

    @BeforeEach
    void setUp() {
        transferRepository = mock(TransferRepository.class);
        processedEvents = mock(ProcessedEventRepository.class);
        commandPublisher = mock(TransferCommandPublisher.class);
        kafkaPublisher = mock(EventPublisher.class);
        statusPublisher = mock(ApplicationEventPublisher.class);
        orchestrator = new TransferSagaOrchestrator(transferRepository, processedEvents,
                commandPublisher, kafkaPublisher, statusPublisher);
        when(transferRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    private Transfer transfer(TransferStatus status) {
        var transfer = new Transfer(UUID.randomUUID(), UUID.randomUUID(), "111", "222",
                new BigDecimal("50.00"), "USD", "test");
        transfer.setStatus(status);
        when(transferRepository.findByTransferId(transfer.getTransferId())).thenReturn(Optional.of(transfer));
        return transfer;
    }

    @Test
    void debitoPublicaComandoDeCreditoPeroNoNotifica() {
        var transfer = transfer(TransferStatus.PENDING);
        var event = new AccountDebitedEvent(transfer.getTransferId(), UUID.randomUUID(), "111",
                transfer.getAmount(), new BigDecimal("450.00"), "USD");

        orchestrator.handle(event);

        assertEquals(TransferStatus.DEBITED, transfer.getStatus());
        var command = ArgumentCaptor.forClass(TransferCreditCommand.class);
        verify(commandPublisher).enqueue(eq(transfer.getTransferId()), command.capture());
        assertEquals("222", command.getValue().getToAccount());
        verifyNoInteractions(kafkaPublisher);
        verify(statusPublisher).publishEvent(any(TransferStatusEvent.class));
    }

    @Test
    void creditoTerminaEnCompletedYNotificaASenderYRecipient() {
        var transfer = transfer(TransferStatus.DEBITED);
        var recipient = UUID.randomUUID();
        var event = new AccountCreditedEvent(transfer.getTransferId(), UUID.randomUUID(), "222",
                transfer.getAmount(), new BigDecimal("250.00"), "USD", recipient);

        orchestrator.handle(event);

        assertEquals(TransferStatus.COMPLETED, transfer.getStatus());
        assertEquals(recipient, transfer.getToUserId());
        var notifications = ArgumentCaptor.forClass(TransferNotificationEvent.class);
        verify(kafkaPublisher, times(2)).publish(eq("bank.notification.events"), notifications.capture());
        assertEquals("TRANSFER_SENT", notifications.getAllValues().get(0).getType());
        assertEquals("TRANSFER_RECEIVED", notifications.getAllValues().get(1).getType());
    }

    @Test
    void rechazoAntesDeDebitoTerminaEnRejectedSinCompensar() {
        var transfer = transfer(TransferStatus.PENDING);
        var event = new AccountRejectedEvent(transfer.getTransferId(), "111", "Insufficient funds",
                transfer.getAmount(), "USD");

        orchestrator.handle(event);

        assertEquals(TransferStatus.REJECTED, transfer.getStatus());
        assertEquals("Insufficient funds", transfer.getRejectionReason());
        verifyNoInteractions(commandPublisher);
        verifyNotification("TRANSFER_REJECTED");
    }

    @Test
    void rechazoDespuesDeDebitoTerminaEnCompensatedYNotificaReversion() {
        var transfer = transfer(TransferStatus.DEBITED);
        // Compatibilidad con el flujo anterior (accounts ya revertio).
        var event = new AccountRejectedEvent(transfer.getTransferId(), "111",
                "Destination account not found — debit reversed", transfer.getAmount(), "USD");
        event.setCorrelationId(UUID.randomUUID());

        orchestrator.handle(event);

        assertEquals(TransferStatus.COMPENSATED, transfer.getStatus());
        assertTrue(transfer.getRejectionReason().contains("debito revertido"));
        var notification = verifyNotification("TRANSFER_COMPENSATED");
        assertEquals(event.getCorrelationId(), notification.getCorrelationId());
    }

    @Test
    void falloDeCreditoPublicaComandoDeCompensacionYNoNotifica() {
        var transfer = transfer(TransferStatus.DEBITED);
        var event = new AccountCreditFailedEvent(transfer.getTransferId(), "222",
                "Destination account is blocked", transfer.getAmount(), "USD");

        orchestrator.handle(event);

        assertEquals(TransferStatus.DEBITED, transfer.getStatus());
        assertTrue(transfer.getRejectionReason().contains("compensating"));
        var command = ArgumentCaptor.forClass(TransferCompensateCommand.class);
        verify(commandPublisher).enqueue(eq(transfer.getTransferId()), command.capture());
        assertEquals("111", command.getValue().getFromAccount());
        verifyNoInteractions(kafkaPublisher);
    }

    @Test
    void compensacionConfirmadaTerminaEnCompensatedConNotificacionUnica() {
        var transfer = transfer(TransferStatus.DEBITED);
        transfer.setRejectionReason("Credit failed (compensating)");
        var event = new AccountDebitCompensatedEvent(transfer.getTransferId(), "111",
                transfer.getAmount(), "USD", "Destination account not found", "123456");

        orchestrator.handle(event);

        assertEquals(TransferStatus.COMPENSATED, transfer.getStatus());
        verifyNotification("TRANSFER_COMPENSATED");
    }

    @Test
    void eventoDuplicadoEsIgnoradoPorInbox() {
        var transfer = transfer(TransferStatus.PENDING);
        var event = new AccountRejectedEvent(transfer.getTransferId(), "111", "Insufficient funds",
                transfer.getAmount(), "USD");
        when(processedEvents.isProcessed(event.getEventId().toString(), "transfers-saga"))
                .thenReturn(false, true);

        orchestrator.handle(event);
        orchestrator.handle(event);

        verify(processedEvents, times(1)).markProcessed(event.getEventId().toString(), "transfers-saga");
        verify(transferRepository, times(1)).save(any());
        verify(kafkaPublisher, times(1)).publish(anyString(), any());
    }

    @Test
    void eventIdDeNotificacionEsDeterministaParaEvitarDuplicados() {
        var sagaTransfer = transfer(TransferStatus.DEBITED);
        var recipient = UUID.randomUUID();
        var firstNotification = captureNotification(sagaTransfer, recipient);
        clearInvocations(kafkaPublisher);
        sagaTransfer.setStatus(TransferStatus.DEBITED);
        var secondNotification = captureNotification(sagaTransfer, recipient);

        assertEquals(firstNotification.getEventId(), secondNotification.getEventId());
    }

    @Test
    void timeoutAntesDelDebitoTerminaEnFailedYNotifica() {
        var transfer = transfer(TransferStatus.PENDING);

        orchestrator.handleTimeout(transfer);

        assertEquals(TransferStatus.FAILED, transfer.getStatus());
        assertEquals("Saga timeout before debit", transfer.getRejectionReason());
        verifyNotification("TRANSFER_REJECTED");
        verifyNoInteractions(commandPublisher);
    }

    @Test
    void timeoutDespuesDelDebitoEncolaCompensacionSinCerrarLaSaga() {
        var transfer = transfer(TransferStatus.DEBITED);

        orchestrator.handleTimeout(transfer);

        assertEquals(TransferStatus.DEBITED, transfer.getStatus());
        assertTrue(transfer.getRejectionReason().contains("compensating"));
        var command = ArgumentCaptor.forClass(TransferCompensateCommand.class);
        verify(commandPublisher).enqueue(eq(transfer.getTransferId()), command.capture());
        assertEquals("111", command.getValue().getFromAccount());
        verifyNoInteractions(kafkaPublisher);
    }

    private TransferNotificationEvent captureNotification(Transfer transfer, UUID recipient) {
        var event = new AccountCreditedEvent(transfer.getTransferId(), UUID.randomUUID(), "222",
                transfer.getAmount(), new BigDecimal("100.00"), "USD", recipient);
        orchestrator.handle(event);
        var notifications = ArgumentCaptor.forClass(TransferNotificationEvent.class);
        verify(kafkaPublisher, times(2)).publish(eq("bank.notification.events"), notifications.capture());
        return notifications.getAllValues().get(0);
    }

    private TransferNotificationEvent verifyNotification(String type) {
        var notification = ArgumentCaptor.forClass(TransferNotificationEvent.class);
        verify(kafkaPublisher).publish(eq("bank.notification.events"), notification.capture());
        assertEquals(type, notification.getValue().getType());
        return notification.getValue();
    }
}
