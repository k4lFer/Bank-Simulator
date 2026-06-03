package com.pck4x.transfers_service.infrastructure.kafka;

import com.pck4x.sharedcontracts.event.AccountCreditedEvent;
import com.pck4x.sharedcontracts.event.AccountDebitedEvent;
import com.pck4x.sharedcontracts.event.AccountRejectedEvent;
import com.pck4x.transfers_service.domain.enums.TransferStatus;
import com.pck4x.transfers_service.application.port.output.TransferRepository;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class AccountEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(AccountEventConsumer.class);

    private final TransferRepository transferRepository;

    public AccountEventConsumer(TransferRepository transferRepository) {
        this.transferRepository = transferRepository;
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
                transferRepository.save(transfer);
                log.info("Transfer {} updated to DEBITED", e.getTransferId());
            });
        } else if (event instanceof AccountCreditedEvent e) {
            log.info("Received AccountCreditedEvent: transferId={}, account={}",
                    e.getTransferId(), e.getAccountNumber());
            transferRepository.findByTransferId(e.getTransferId()).ifPresent(transfer -> {
                transfer.setStatus(TransferStatus.COMPLETED);
                transfer.setUpdatedAt(java.time.LocalDateTime.now());
                transferRepository.save(transfer);
                log.info("Transfer {} updated to COMPLETED", e.getTransferId());
            });
        } else if (event instanceof AccountRejectedEvent e) {
            log.warn("Received AccountRejectedEvent: transferId={}, account={}, reason={}",
                    e.getTransferId(), e.getAccountNumber(), e.getReason());
            transferRepository.findByTransferId(e.getTransferId()).ifPresent(transfer -> {
                transfer.setStatus(TransferStatus.REJECTED);
                transfer.setUpdatedAt(java.time.LocalDateTime.now());
                transferRepository.save(transfer);
                log.info("Transfer {} updated to REJECTED", e.getTransferId());
            });
        } else {
            log.debug("Ignored event type: {}", event.getClass().getSimpleName());
        }
    }
}
