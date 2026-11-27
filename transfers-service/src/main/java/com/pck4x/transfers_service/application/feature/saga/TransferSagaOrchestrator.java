package com.pck4x.transfers_service.application.feature.saga;

import com.pck4x.sharedcontracts.command.TransferCompensateCommand;
import com.pck4x.sharedcontracts.command.TransferCreditCommand;
import com.pck4x.sharedcontracts.event.AccountCreditedEvent;
import com.pck4x.sharedcontracts.event.AccountCreditFailedEvent;
import com.pck4x.sharedcontracts.event.AccountDebitCompensatedEvent;
import com.pck4x.sharedcontracts.event.AccountDebitedEvent;
import com.pck4x.sharedcontracts.event.AccountRejectedEvent;
import com.pck4x.sharedcontracts.event.TransferNotificationEvent;
import com.pck4x.sharedcontracts.security.CorrelationContext;
import com.pck4x.transfers_service.application.event.TransferStatusEvent;
import com.pck4x.transfers_service.application.port.output.EventPublisher;
import com.pck4x.transfers_service.application.port.output.ProcessedEventRepository;
import com.pck4x.transfers_service.application.port.output.TransferRepository;
import com.pck4x.transfers_service.domain.Transfer;
import com.pck4x.transfers_service.domain.TransferSaga;
import com.pck4x.transfers_service.domain.enums.TransferStatus;
import com.pck4x.transfers_service.infrastructure.kafka.TransferCommandPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Fase 2.2: decide el siguiente comando tras cada evento de accounts.
 * Fase 2.7: es el UNICO emisor de notificaciones, y solo en estado terminal.
 */
@Component
public class TransferSagaOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(TransferSagaOrchestrator.class);
    private static final String NOTIFICATION_TOPIC = "bank.notification.events";
    private static final String INBOX_CONSUMER = "transfers-saga";

    private final TransferRepository transferRepository;
    private final ProcessedEventRepository processedEventRepository;
    private final TransferCommandPublisher commandPublisher;
    private final EventPublisher kafkaPublisher;
    private final ApplicationEventPublisher statusPublisher;

    public TransferSagaOrchestrator(TransferRepository transferRepository,
                                    ProcessedEventRepository processedEventRepository,
                                    TransferCommandPublisher commandPublisher,
                                    EventPublisher kafkaPublisher,
                                    ApplicationEventPublisher statusPublisher) {
        this.transferRepository = transferRepository;
        this.processedEventRepository = processedEventRepository;
        this.commandPublisher = commandPublisher;
        this.kafkaPublisher = kafkaPublisher;
        this.statusPublisher = statusPublisher;
    }

    @Transactional
    public void handle(Object reply) {
        UUID transferId = transferIdOf(reply);
        if (transferId == null) {
            log.debug("Ignored reply type: {}", reply == null ? "null" : reply.getClass().getSimpleName());
            return;
        }

        String eventId = eventIdOf(reply, transferId);
        if (processedEventRepository.isProcessed(eventId, INBOX_CONSUMER)) {
            log.info("Duplicate saga reply ignored by inbox: eventId={}", eventId);
            return;
        }
        processedEventRepository.markProcessed(eventId, INBOX_CONSUMER);

        var transfer = transferRepository.findByTransferId(transferId).orElse(null);
        if (transfer == null) {
            log.warn("Saga reply for unknown transfer: {}", transferId);
            return;
        }
        var saga = new TransferSaga(transfer);
        if (saga.isTerminal() && !(reply instanceof AccountDebitCompensatedEvent)) {
            log.info("Ignored reply for terminal transfer {}: {}", transferId, saga.current());
            return;
        }

        if (reply instanceof AccountDebitedEvent event) {
            onDebited(saga, transfer, event);
        } else if (reply instanceof AccountCreditedEvent event) {
            onCredited(saga, transfer, event);
        } else if (reply instanceof AccountRejectedEvent event) {
            onRejected(saga, transfer, event);
        } else if (reply instanceof AccountCreditFailedEvent event) {
            onCreditFailed(saga, transfer, event);
        } else if (reply instanceof AccountDebitCompensatedEvent event) {
            onCompensated(saga, transfer, event);
        } else {
            log.debug("Ignored saga reply: {}", reply.getClass().getSimpleName());
        }
    }

    /** Fase 2.9: cierra o compensa una saga abandonada por el scheduler. */
    @Transactional
    public void handleTimeout(Transfer transfer) {
        if (transfer == null || new TransferSaga(transfer).isTerminal()) {
            return;
        }
        if (transfer.getStatus() == TransferStatus.PENDING) {
            transfer.setStatus(TransferStatus.FAILED);
            transfer.setRejectionReason("Saga timeout before debit");
            transfer.setUpdatedAt(LocalDateTime.now());
            transferRepository.save(transfer);
            statusPublisher.publishEvent(new TransferStatusEvent(transfer));
            notify(transfer, null, "TRANSFER_REJECTED", "Transferencia fallida",
                    "Tu transferencia de " + transfer.getAmount() + " " + transfer.getCurrency()
                            + " no pudo completarse: Saga timeout antes del debito",
                    transfer.getFromAccount(), transfer.getUserId());
            log.warn("Transfer {} timed out before debit -> FAILED", transfer.getTransferId());
            return;
        }

        // Ya hubo debito: renovar updatedAt evitaria reintentar si la compensacion se pierde.
        transfer.setRejectionReason("Saga timeout after debit (compensating)");
        transferRepository.save(transfer);
        statusPublisher.publishEvent(new TransferStatusEvent(transfer));
        var command = new TransferCompensateCommand(transfer.getTransferId(), transfer.getFromAccount(),
                transfer.getAmount(), transfer.getCurrency(), "Saga timeout");
        commandPublisher.enqueue(transfer.getTransferId(), command);
        log.warn("Transfer {} timed out after debit -> compensate command enqueued", transfer.getTransferId());
    }

    private void onDebited(TransferSaga saga, Transfer transfer, AccountDebitedEvent event) {
        if (!saga.can(TransferSaga.State.CREATED, TransferSaga.State.DEBITED)) {
            log.info("Ignored debited reply: transferId={}, state={}", transfer.getTransferId(), saga.current());
            return;
        }
        saga.transition(TransferSaga.State.CREATED, TransferSaga.State.DEBITED);
        transfer.setStatus(TransferStatus.DEBITED);
        transfer.setUpdatedAt(LocalDateTime.now());
        transferRepository.save(transfer);
        statusPublisher.publishEvent(new TransferStatusEvent(transfer));

        var command = new TransferCreditCommand(transfer.getTransferId(), transfer.getFromAccount(),
                transfer.getToAccount(), transfer.getAmount(), transfer.getCurrency(), transfer.getDescription());
        command.setToUserId(transfer.getToUserId());
        commandPublisher.enqueue(transfer.getTransferId(), command);
        log.info("Transfer {} debited -> credit command enqueued", transfer.getTransferId());
    }

    private void onCredited(TransferSaga saga, Transfer transfer, AccountCreditedEvent event) {
        String reason = transfer.getRejectionReason();
        if (reason != null && (reason.contains("compensating") || reason.contains("Saga timeout"))) {
            log.warn("Ignored credited reply for transfer {} already compensating", transfer.getTransferId());
            return;
        }
        if (!saga.can(TransferSaga.State.DEBITED, TransferSaga.State.COMPLETED)) {
            log.info("Ignored credited reply: transferId={}, state={}", transfer.getTransferId(), saga.current());
            return;
        }
        saga.transition(TransferSaga.State.DEBITED, TransferSaga.State.COMPLETED);
        transfer.setStatus(TransferStatus.COMPLETED);
        transfer.setToUserId(event.getToUserId());
        transfer.setUpdatedAt(LocalDateTime.now());
        transferRepository.save(transfer);
        statusPublisher.publishEvent(new TransferStatusEvent(transfer));

        notify(transfer, event, "TRANSFER_SENT", "Transferencia enviada",
                "Transferencia de " + transfer.getAmount() + " " + transfer.getCurrency()
                        + " desde tu cuenta " + transfer.getFromAccount() + " completada",
                transfer.getFromAccount(), transfer.getUserId());
        if (event.getToUserId() != null) {
            notify(transfer, event, "TRANSFER_RECEIVED", "Transferencia recibida",
                    "Recibiste " + transfer.getAmount() + " " + transfer.getCurrency()
                            + " en tu cuenta " + event.getAccountNumber(),
                    event.getAccountNumber(), event.getToUserId());
        }
        log.info("Transfer {} COMPLETED", transfer.getTransferId());
    }

    private void onRejected(TransferSaga saga, Transfer transfer, AccountRejectedEvent event) {
        // Compatibilidad con el flujo anterior: un rechazo en DEBITED implica debito revertido.
        if (saga.current() == TransferSaga.State.DEBITED) {
            transfer.setStatus(TransferStatus.COMPENSATED);
            transfer.setRejectionReason(event.getReason() + " (debito revertido)");
        } else if (saga.can(TransferSaga.State.CREATED, TransferSaga.State.REJECTED)) {
            saga.transition(TransferSaga.State.CREATED, TransferSaga.State.REJECTED);
            transfer.setStatus(TransferStatus.REJECTED);
            transfer.setRejectionReason(event.getReason());
        } else {
            log.info("Ignored rejected reply: transferId={}, state={}", transfer.getTransferId(), saga.current());
            return;
        }
        transfer.setUpdatedAt(LocalDateTime.now());
        transferRepository.save(transfer);
        statusPublisher.publishEvent(new TransferStatusEvent(transfer));

        boolean compensated = transfer.getStatus() == TransferStatus.COMPENSATED;
        notify(transfer, event, compensated ? "TRANSFER_COMPENSATED" : "TRANSFER_REJECTED",
                compensated ? "Transferencia revertida" : "Transferencia rechazada",
                "Tu transferencia de " + transfer.getAmount() + " " + transfer.getCurrency()
                        + (compensated ? " fue cancelada y el debito revertido: " : " fue rechazada: ")
                        + event.getReason(),
                transfer.getFromAccount(), transfer.getUserId());
        log.info("Transfer {} -> {}", transfer.getTransferId(), transfer.getStatus());
    }

    private void onCreditFailed(TransferSaga saga, Transfer transfer, AccountCreditFailedEvent event) {
        if (!saga.can(TransferSaga.State.DEBITED, TransferSaga.State.COMPENSATING)) {
            log.info("Ignored credit-failed reply: transferId={}, state={}", transfer.getTransferId(), saga.current());
            return;
        }
        saga.transition(TransferSaga.State.DEBITED, TransferSaga.State.COMPENSATING);
        transfer.setRejectionReason("Credit failed: " + event.getReason() + " (compensating)");
        transfer.setUpdatedAt(LocalDateTime.now());
        transferRepository.save(transfer);
        statusPublisher.publishEvent(new TransferStatusEvent(transfer));

        var command = new TransferCompensateCommand(transfer.getTransferId(), transfer.getFromAccount(),
                transfer.getAmount(), transfer.getCurrency(), event.getReason());
        commandPublisher.enqueue(transfer.getTransferId(), command);
        log.warn("Transfer {} credit failed -> compensate command enqueued", transfer.getTransferId());
    }

    private void onCompensated(TransferSaga saga, Transfer transfer, AccountDebitCompensatedEvent event) {
        // COMPENSATING no se persiste: mientras tanto la transferencia sigue en DEBITED.
        if (saga.current() != TransferSaga.State.COMPENSATING
                && saga.current() != TransferSaga.State.DEBITED
                && saga.current() != TransferSaga.State.FAILED) {
            log.info("Ignored compensated reply: transferId={}, state={}", transfer.getTransferId(), saga.current());
            return;
        }
        transfer.setStatus(TransferStatus.COMPENSATED);
        transfer.setRejectionReason(event.getReason() + " (debito revertido)");
        transfer.setUpdatedAt(LocalDateTime.now());
        transferRepository.save(transfer);
        statusPublisher.publishEvent(new TransferStatusEvent(transfer));

        notify(transfer, event, "TRANSFER_COMPENSATED", "Transferencia revertida",
                "Tu transferencia de " + transfer.getAmount() + " " + transfer.getCurrency()
                        + " fue cancelada y el debito revertido: " + event.getReason(),
                transfer.getFromAccount(), transfer.getUserId());
        log.info("Transfer {} COMPENSATED", transfer.getTransferId());
    }

    /** Fase 2.8: eventId determinista para que un replay no cree otra notificacion. */
    private void notify(Transfer transfer, Object source, String type, String title, String message,
                        String account, UUID userId) {
        if (userId == null) {
            return;
        }
        var notification = new TransferNotificationEvent(userId, null, type, title, message,
                transfer.getAmount(), transfer.getCurrency(), account);
        String seed = transfer.getTransferId() + ":" + type + ":" + userId;
        notification.setEventId(UUID.nameUUIDFromBytes(seed.getBytes(StandardCharsets.UTF_8)));
        CorrelationContext.copy(source, notification);
        kafkaPublisher.publish(NOTIFICATION_TOPIC, notification);
    }

    private UUID transferIdOf(Object event) {
        if (event instanceof AccountDebitedEvent e) return e.getTransferId();
        if (event instanceof AccountCreditedEvent e) return e.getTransferId();
        if (event instanceof AccountRejectedEvent e) return e.getTransferId();
        if (event instanceof AccountCreditFailedEvent e) return e.getTransferId();
        if (event instanceof AccountDebitCompensatedEvent e) return e.getTransferId();
        return null;
    }

    private String eventIdOf(Object event, UUID transferId) {
        UUID eventId = null;
        if (event instanceof AccountDebitedEvent e) eventId = e.getEventId();
        else if (event instanceof AccountCreditedEvent e) eventId = e.getEventId();
        else if (event instanceof AccountRejectedEvent e) eventId = e.getEventId();
        else if (event instanceof AccountCreditFailedEvent e) eventId = e.getEventId();
        else if (event instanceof AccountDebitCompensatedEvent e) eventId = e.getEventId();
        return eventId != null ? eventId.toString() : event.getClass().getSimpleName() + ":" + transferId;
    }
}
