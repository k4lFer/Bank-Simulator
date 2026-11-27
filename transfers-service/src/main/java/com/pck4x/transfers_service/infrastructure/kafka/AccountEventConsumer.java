package com.pck4x.transfers_service.infrastructure.kafka;

import com.pck4x.sharedcontracts.event.AccountCreditedEvent;
import com.pck4x.sharedcontracts.event.AccountDebitedEvent;
import com.pck4x.sharedcontracts.event.AccountRejectedEvent;
import com.pck4x.sharedcontracts.event.TransferNotificationEvent;
import com.pck4x.sharedcontracts.security.CorrelationContext;
import com.pck4x.transfers_service.application.event.TransferStatusEvent;
import com.pck4x.transfers_service.application.feature.saga.TransferSagaOrchestrator;
import com.pck4x.transfers_service.application.port.output.EventPublisher;
import com.pck4x.transfers_service.application.port.output.ProcessedEventRepository;
import com.pck4x.transfers_service.application.port.output.TransferRepository;
import com.pck4x.transfers_service.domain.enums.TransferStatus;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class AccountEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(AccountEventConsumer.class);

    private final TransferRepository transferRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final EventPublisher kafkaPublisher;
    private final ProcessedEventRepository processedEventRepository;
    private final TransferSagaOrchestrator orchestrator;
    private final boolean orchestrated;

    public AccountEventConsumer(TransferRepository transferRepository,
                                 ApplicationEventPublisher eventPublisher,
                                 EventPublisher kafkaPublisher,
                                 ProcessedEventRepository processedEventRepository,
                                 TransferSagaOrchestrator orchestrator,
                                 @Value("${app.saga.orchestrated:true}") boolean orchestrated) {
        this.transferRepository = transferRepository;
        this.eventPublisher = eventPublisher;
        this.kafkaPublisher = kafkaPublisher;
        this.processedEventRepository = processedEventRepository;
        this.orchestrator = orchestrator;
        this.orchestrated = orchestrated;
    }

    @Transactional
    @KafkaListener(topics = "bank.account.events", groupId = "transfers-service")
    public void handleAccountEvent(ConsumerRecord<String, Object> record) {
        Object event = record.value();

        // Fase 2: la saga orquestada centraliza estado, comandos y notificaciones.
        // El codigo inferior se conserva solo para el dual-run (flag=false).
        if (orchestrated) {
            orchestrator.handle(event);
            return;
        }

        // Fase 1.4: dedupe por eventId antes de cualquier cambio de estado.
        String eventId = resolveEventId(event);
        if (processedEventRepository.isProcessed(eventId, "transfers-account")) {
            log.info("Duplicate event ignored by inbox: eventId={}", eventId);
            return;
        }
        processedEventRepository.markProcessed(eventId, "transfers-account");

        if (event instanceof AccountDebitedEvent e) {
            log.info("Received AccountDebitedEvent: transferId={}, account={}",
                    e.getTransferId(), e.getAccountNumber());
            transferRepository.findByTransferId(e.getTransferId()).ifPresent(transfer -> {
                if (transfer.getStatus() != TransferStatus.PENDING) {
                    log.info("Ignored duplicate AccountDebitedEvent: transferId={}, currentStatus={}",
                            e.getTransferId(), transfer.getStatus());
                    return;
                }
                transfer.setStatus(TransferStatus.DEBITED);
                transfer = transferRepository.save(transfer);
                eventPublisher.publishEvent(new TransferStatusEvent(transfer));

                publishNotification(e,
                        new TransferNotificationEvent(transfer.getUserId(), null,
                                "TRANSFER_SENT", "Transferencia enviada",
                                "Transferencia de " + e.getAmount() + " " + e.getCurrency()
                                        + " desde tu cuenta " + e.getAccountNumber() + " está en proceso",
                                e.getAmount(), e.getCurrency(), e.getAccountNumber()));

                log.info("Transfer {} updated to DEBITED", e.getTransferId());
            });
        } else if (event instanceof AccountCreditedEvent e) {
            log.info("Received AccountCreditedEvent: transferId={}, account={}",
                    e.getTransferId(), e.getAccountNumber());
            transferRepository.findByTransferId(e.getTransferId()).ifPresent(transfer -> {
                if (transfer.getStatus() == TransferStatus.COMPLETED) {
                    log.info("Ignored duplicate AccountCreditedEvent: transferId={}", e.getTransferId());
                    return;
                }
                transfer.setStatus(TransferStatus.COMPLETED);
                transfer.setToUserId(e.getToUserId());
                transfer.setUpdatedAt(java.time.LocalDateTime.now());
                transfer = transferRepository.save(transfer);
                eventPublisher.publishEvent(new TransferStatusEvent(transfer));

                // Notify recipient
                if (e.getToUserId() != null) {
                    publishNotification(e,
                            new TransferNotificationEvent(e.getToUserId(), null,
                                    "TRANSFER_RECEIVED", "Transferencia recibida",
                                    "Recibiste " + e.getAmount() + " " + e.getCurrency()
                                            + " en tu cuenta " + e.getAccountNumber(),
                                    e.getAmount(), e.getCurrency(), e.getAccountNumber()));
                }

                log.info("Transfer {} updated to COMPLETED", e.getTransferId());
            });
        } else if (event instanceof AccountRejectedEvent e) {
            log.warn("Received AccountRejectedEvent: transferId={}, account={}, reason={}",
                    e.getTransferId(), e.getAccountNumber(), e.getReason());
            transferRepository.findByTransferId(e.getTransferId()).ifPresent(transfer -> {
                TransferStatus current = transfer.getStatus();

                // Fase 0.2: un rechazo despues del debito NO se descarta.
                // PENDING  -> REJECTED   (nunca se debito)
                // DEBITED  -> COMPENSATED (el debito ya fue revertido por accounts-service)
                if (current != TransferStatus.PENDING && current != TransferStatus.DEBITED) {
                    log.info("Ignored duplicate AccountRejectedEvent: transferId={}, currentStatus={}",
                            e.getTransferId(), current);
                    return;
                }

                boolean wasDebited = current == TransferStatus.DEBITED;
                transfer.setStatus(wasDebited ? TransferStatus.COMPENSATED : TransferStatus.REJECTED);
                transfer.setRejectionReason(wasDebited
                        ? e.getReason() + " (debito revertido)"
                        : e.getReason());
                transfer.setUpdatedAt(java.time.LocalDateTime.now());
                transfer = transferRepository.save(transfer);
                eventPublisher.publishEvent(new TransferStatusEvent(transfer));

                publishNotification(e,
                        new TransferNotificationEvent(transfer.getUserId(), null,
                                wasDebited ? "TRANSFER_COMPENSATED" : "TRANSFER_REJECTED",
                                wasDebited ? "Transferencia revertida" : "Transferencia rechazada",
                                "Tu transferencia de " + e.getAmount() + " " + e.getCurrency()
                                        + (wasDebited ? " fue cancelada y el debito revertido: "
                                                      : " fue rechazada: ") + e.getReason(),
                                e.getAmount(), e.getCurrency(), e.getAccountNumber()));

                log.info("Transfer {} updated to {}", e.getTransferId(), transfer.getStatus());
            });
        } else {
            log.debug("Ignored event type: {}", event.getClass().getSimpleName());
        }
    }

    private void publishNotification(Object source, TransferNotificationEvent notification) {
        CorrelationContext.copy(source, notification);
        kafkaPublisher.publish("bank.notification.events", notification);
    }

    private String resolveEventId(Object event) {
        if (event == null) {
            return "unknown:" + java.util.UUID.randomUUID();
        }
        java.util.UUID eventId = null;
        if (event instanceof AccountDebitedEvent e) eventId = e.getEventId();
        else if (event instanceof AccountCreditedEvent e) eventId = e.getEventId();
        else if (event instanceof AccountRejectedEvent e) eventId = e.getEventId();
        return eventId != null
                ? eventId.toString()
                : event.getClass().getSimpleName() + ":" + event.hashCode();
    }
}
