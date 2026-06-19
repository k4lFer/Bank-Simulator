package com.pck4x.transfers_service.infrastructure.kafka;

import com.pck4x.sharedcontracts.event.AccountCreditedEvent;
import com.pck4x.sharedcontracts.event.AccountDebitedEvent;
import com.pck4x.sharedcontracts.event.AccountRejectedEvent;
import com.pck4x.sharedcontracts.event.TransferNotificationEvent;
import com.pck4x.transfers_service.application.event.TransferStatusEvent;
import com.pck4x.transfers_service.application.port.output.EventPublisher;
import com.pck4x.transfers_service.application.port.output.TransferRepository;
import com.pck4x.transfers_service.domain.enums.TransferStatus;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    public AccountEventConsumer(TransferRepository transferRepository,
                                 ApplicationEventPublisher eventPublisher,
                                 EventPublisher kafkaPublisher) {
        this.transferRepository = transferRepository;
        this.eventPublisher = eventPublisher;
        this.kafkaPublisher = kafkaPublisher;
    }

    @Transactional
    @KafkaListener(topics = "bank.account.events", groupId = "transfers-service")
    public void handleAccountEvent(ConsumerRecord<String, Object> record) {
        Object event = record.value();
        if (event instanceof AccountDebitedEvent e) {
            log.info("Received AccountDebitedEvent: transferId={}, account={}",
                    e.getTransferId(), e.getAccountNumber());
            transferRepository.findByTransferId(e.getTransferId()).ifPresent(transfer -> {
                transfer.setStatus(TransferStatus.DEBITED);
                transfer = transferRepository.save(transfer);
                eventPublisher.publishEvent(new TransferStatusEvent(transfer));

                kafkaPublisher.publish("bank.notification.events",
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
                transfer.setStatus(TransferStatus.COMPLETED);
                transfer.setToUserId(e.getToUserId());
                transfer.setUpdatedAt(java.time.LocalDateTime.now());
                transfer = transferRepository.save(transfer);
                eventPublisher.publishEvent(new TransferStatusEvent(transfer));

                // Notify recipient
                if (e.getToUserId() != null) {
                    kafkaPublisher.publish("bank.notification.events",
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
                transfer.setStatus(TransferStatus.REJECTED);
                transfer.setRejectionReason(e.getReason());
                transfer.setUpdatedAt(java.time.LocalDateTime.now());
                transfer = transferRepository.save(transfer);
                eventPublisher.publishEvent(new TransferStatusEvent(transfer));

                // Notify sender about rejection
                kafkaPublisher.publish("bank.notification.events",
                        new TransferNotificationEvent(transfer.getUserId(), null,
                                "TRANSFER_REJECTED", "Transferencia rechazada",
                                "Tu transferencia de " + e.getAmount() + " " + e.getCurrency()
                                        + " fue rechazada: " + e.getReason(),
                                e.getAmount(), e.getCurrency(), e.getAccountNumber()));

                log.info("Transfer {} updated to REJECTED", e.getTransferId());
            });
        } else {
            log.debug("Ignored event type: {}", event.getClass().getSimpleName());
        }
    }
}
