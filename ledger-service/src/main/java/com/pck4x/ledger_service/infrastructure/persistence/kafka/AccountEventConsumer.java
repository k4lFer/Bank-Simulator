package com.pck4x.ledger_service.infrastructure.persistence.kafka;

import java.math.BigDecimal;
import java.util.UUID;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.pck4x.ledger_service.application.event.LedgerEntryEvent;
import com.pck4x.ledger_service.application.port.output.LedgerRepository;
import com.pck4x.ledger_service.domain.LedgerEntries;
import com.pck4x.sharedcontracts.event.AccountCreditedEvent;
import com.pck4x.sharedcontracts.event.AccountDebitedEvent;
import com.pck4x.sharedcontracts.event.AccountDepositedEvent;
import com.pck4x.sharedcontracts.event.AccountRejectedEvent;

import jakarta.transaction.Transactional;

@Component
public class AccountEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(AccountEventConsumer.class);

    private final LedgerRepository ledgerRepository;
    private final ApplicationEventPublisher eventPublisher;

    public AccountEventConsumer(LedgerRepository ledgerRepository,
                                 ApplicationEventPublisher eventPublisher) {
        this.ledgerRepository = ledgerRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    @KafkaListener(topics = "bank.account.events", groupId = "ledger-service")
    public void handlerAccountEvent(ConsumerRecord<String, Object> record) {
        Object event = record.value();

        if (event instanceof AccountDebitedEvent e) {
            log.info("Debit event: transferId={}, account={}, amount={}",
                    e.getTransferId(), e.getAccountNumber(), e.getAmount());
            saveEntry(e.getTransferId(), e.getAccountNumber(), "DR", e.getAmount(), e.getCurrency());

        } else if (event instanceof AccountCreditedEvent e) {
            log.info("Credit event: transferId={}, account={}, amount={}",
                    e.getTransferId(), e.getAccountNumber(), e.getAmount());
            saveEntry(e.getTransferId(), e.getAccountNumber(), "CR", e.getAmount(), e.getCurrency());

        } else if (event instanceof AccountDepositedEvent e) {
            log.info("Deposit event: account={}, amount={}",
                    e.getAccountNumber(), e.getAmount());
            saveEntry(null, e.getAccountNumber(), "CR", e.getAmount(), e.getCurrency());

        } else if (event instanceof AccountRejectedEvent e) {
            log.warn("Rejected event: transferId={}, account={}, reason={}",
                    e.getTransferId(), e.getAccountNumber(), e.getReason());

            if (e.getAmount() != null && e.getCurrency() != null) {
                saveEntry(e.getTransferId(), e.getAccountNumber(), "CR", e.getAmount(), e.getCurrency());
                log.info("Reversal entry saved for rejected transfer: account={}, amount={}",
                        e.getAccountNumber(), e.getAmount());
            }

        } else {
            log.debug("Ignored event type: {}", event.getClass().getSimpleName());
        }
    }

    private void saveEntry(UUID transferId, String accountNumber, String entryType, BigDecimal amount, String currency) {
        var entry = transferId != null
                ? new LedgerEntries(transferId, accountNumber, entryType, amount, currency)
                : new LedgerEntries(accountNumber, entryType, amount, currency);
        var saved = ledgerRepository.save(entry);
        eventPublisher.publishEvent(new LedgerEntryEvent(saved));
        log.info("Ledger entry saved: {} {} {} {} {}", entryType, accountNumber, amount, currency, transferId);
    }
}
