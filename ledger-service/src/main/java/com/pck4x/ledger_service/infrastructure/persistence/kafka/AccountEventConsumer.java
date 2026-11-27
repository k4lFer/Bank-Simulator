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
import com.pck4x.sharedcontracts.money.Money;
import com.pck4x.ledger_service.application.port.output.ProcessedEventRepository;
import com.pck4x.ledger_service.domain.LedgerEntries;
import com.pck4x.sharedcontracts.event.AccountCreditedEvent;
import com.pck4x.sharedcontracts.event.AccountCreditFailedEvent;
import com.pck4x.sharedcontracts.event.AccountDebitCompensatedEvent;
import com.pck4x.sharedcontracts.event.AccountDebitedEvent;
import com.pck4x.sharedcontracts.event.AccountDepositedEvent;
import com.pck4x.sharedcontracts.event.AccountRejectedEvent;

import jakarta.transaction.Transactional;

@Component
public class AccountEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(AccountEventConsumer.class);

    private final LedgerRepository ledgerRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final ProcessedEventRepository processedEventRepository;

    public AccountEventConsumer(LedgerRepository ledgerRepository,
                                 ApplicationEventPublisher eventPublisher,
                                 ProcessedEventRepository processedEventRepository) {
        this.ledgerRepository = ledgerRepository;
        this.eventPublisher = eventPublisher;
        this.processedEventRepository = processedEventRepository;
    }

    @Transactional
    @KafkaListener(topics = "bank.account.events", groupId = "ledger-service")
    public void handlerAccountEvent(ConsumerRecord<String, Object> record) {
        Object event = record.value();

        String eventId = resolveEventId(event);
        if (processedEventRepository.isProcessed(eventId, "ledger-account")) {
            log.info("Duplicate event ignored by inbox: eventId={}", eventId);
            return;
        }
        processedEventRepository.markProcessed(eventId, "ledger-account");

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

        } else if (event instanceof AccountDebitCompensatedEvent e) {
            // Fase 2.6: asiento invertido con tipo propio (varchar(3) => REV).
            log.info("Reversal event: transferId={}, account={}, reversalOf={}",
                    e.getTransferId(), e.getAccountNumber(), e.getReversalOf());
            saveEntry(e.getTransferId(), e.getAccountNumber(), "REV", e.getAmount(), e.getCurrency());

        } else if (event instanceof AccountCreditFailedEvent e) {
            // Fase 2.5: el credito fallo sin tocar saldos, por lo que no hay asiento.
            log.warn("Credit failed event ignored by ledger: transferId={}, reason={}",
                    e.getTransferId(), e.getReason());

        } else if (event instanceof AccountRejectedEvent e) {
            log.warn("Rejected event: transferId={}, account={}, reason={}",
                    e.getTransferId(), e.getAccountNumber(), e.getReason());

            // Fase 0.2: solo la compensacion de un debito genera asiento.
            // Un rechazo previo al debito (fondos, cuenta, PIN) nunca genera CR.
            boolean isReversal = e.getReason() != null
                    && (e.getReason().contains("reversed") || e.getReason().contains("revertido"));
            if (isReversal && e.getAmount() != null && e.getCurrency() != null) {
                saveEntry(e.getTransferId(), e.getAccountNumber(), "CR", e.getAmount(), e.getCurrency());
                log.info("Reversal entry saved for compensated transfer: account={}, amount={}",
                        e.getAccountNumber(), e.getAmount());
            }

        } else {
            log.debug("Ignored event type: {}", event.getClass().getSimpleName());
        }
    }

    private String resolveEventId(Object event) {
        if (event == null) {
            return "unknown:" + UUID.randomUUID();
        }
        UUID eventId = null;
        if (event instanceof AccountDebitedEvent e) eventId = e.getEventId();
        else if (event instanceof AccountCreditedEvent e) eventId = e.getEventId();
        else if (event instanceof AccountDebitCompensatedEvent e) eventId = e.getEventId();
        else if (event instanceof AccountCreditFailedEvent e) eventId = e.getEventId();
        else if (event instanceof AccountDepositedEvent e) eventId = e.getEventId();
        else if (event instanceof AccountRejectedEvent e) eventId = e.getEventId();
        return eventId != null ? eventId.toString() : event.getClass().getSimpleName() + ":" + event.hashCode();
    }

    private void saveEntry(UUID transferId, String accountNumber, String entryType, BigDecimal amount, String currency) {
        // Fase 1.6: el ledger nunca persiste importes invalidos.
        try {
            Money.of(amount, currency);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid ledger entry ignored: transferId={}, account={}, reason={}",
                    transferId, accountNumber, e.getMessage());
            return;
        }

        if (transferId != null) {
            var existing = ledgerRepository.findByTransferIdAndEntryTypeAndAccountNumber(transferId, entryType, accountNumber);
            if (existing.isPresent()) {
                log.info("Duplicate ledger entry ignored: transferId={}, type={}, account={}", transferId, entryType, accountNumber);
                return;
            }
        }

        var entry = transferId != null
                ? new LedgerEntries(transferId, accountNumber, entryType, amount, currency)
                : new LedgerEntries(accountNumber, entryType, amount, currency);
        var saved = ledgerRepository.save(entry);
        eventPublisher.publishEvent(new LedgerEntryEvent(saved));
        log.info("Ledger entry saved: {} {} {} {} {}", entryType, accountNumber, amount, currency, transferId);
    }
}
