package com.pck4x.accounts_service.infrastructure.kafka;

import com.pck4x.accounts_service.application.port.output.AccountRepository;
import com.pck4x.accounts_service.application.port.output.AccountMovementRepository;
import com.pck4x.accounts_service.application.port.output.EventPublisher;
import com.pck4x.accounts_service.application.port.output.MovementNumberGenerator;
import com.pck4x.accounts_service.domain.AccountMovement;
import com.pck4x.accounts_service.domain.enums.MovementType;
import com.pck4x.sharedcontracts.event.AccountCreditedEvent;
import com.pck4x.sharedcontracts.event.AccountDebitedEvent;
import com.pck4x.sharedcontracts.event.AccountRejectedEvent;
import com.pck4x.sharedcontracts.event.TransferRequestedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Component
public class TransferEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(TransferEventConsumer.class);

    private final AccountRepository accountRepository;
    private final AccountMovementRepository movementRepository;
    private final MovementNumberGenerator movementNumberGenerator;
    private final EventPublisher eventPublisher;

    public TransferEventConsumer(AccountRepository accountRepository,
                                 AccountMovementRepository movementRepository,
                                 MovementNumberGenerator movementNumberGenerator,
                                 EventPublisher eventPublisher) {
        this.accountRepository = accountRepository;
        this.movementRepository = movementRepository;
        this.movementNumberGenerator = movementNumberGenerator;
        this.eventPublisher = eventPublisher;
    }

    // Recibe TransferRequestedEvent, debita la origen y acredita la destino
    @Transactional
    @KafkaListener(topics = "bank.transfer.events", groupId = "accounts-service")
    public void handleTransferRequested(TransferRequestedEvent event) {
        log.info("Received TransferRequestedEvent: transferId={}, from={}, to={}, amount={}",
                event.getTransferId(), event.getFromAccount(), event.getToAccount(), event.getAmount());

        var optFromAccount = accountRepository.findByAccountNumber(event.getFromAccount());

        if (optFromAccount.isEmpty()) {
            eventPublisher.publish("bank.account.events",
                    new AccountRejectedEvent(event.getTransferId(), event.getFromAccount(), "Source account not found"));
            return;
        }

        var fromAccount = optFromAccount.get();

        if (fromAccount.getBalance().compareTo(event.getAmount()) < 0) {
            eventPublisher.publish("bank.account.events",
                    new AccountRejectedEvent(event.getTransferId(), event.getFromAccount(), "Insufficient funds"));
            return;
        }

        // Debitar origen
        fromAccount.setBalance(fromAccount.getBalance().subtract(event.getAmount()));
        fromAccount.setUpdatedAt(LocalDateTime.now());
        accountRepository.save(fromAccount);

        var debitMovement = new AccountMovement(
                fromAccount.getId(),
                movementNumberGenerator.generate(event.getFromAccount()),
                MovementType.DEBIT,
                event.getAmount(),
                fromAccount.getBalance()
        );
        movementRepository.save(debitMovement);

        eventPublisher.publish("bank.account.events",
                new AccountDebitedEvent(event.getTransferId(), fromAccount.getId(), event.getFromAccount(),
                        event.getAmount(), fromAccount.getBalance(), event.getCurrency()));

        log.info("Account {} debited by {}", event.getFromAccount(), event.getAmount());

        // Simular demora en el proceso de transferencia
        try { Thread.sleep(2000); } catch (InterruptedException ignored) {}

        // Acreditar destino
        var optToAccount = accountRepository.findByAccountNumber(event.getToAccount());

        if (optToAccount.isEmpty()) {
            eventPublisher.publish("bank.account.events",
                    new AccountRejectedEvent(event.getTransferId(), event.getToAccount(), "Destination account not found"));
            return;
        }

        var toAccount = optToAccount.get();
        toAccount.setBalance(toAccount.getBalance().add(event.getAmount()));
        toAccount.setUpdatedAt(LocalDateTime.now());
        accountRepository.save(toAccount);

        var creditMovement = new AccountMovement(
                toAccount.getId(),
                movementNumberGenerator.generate(event.getToAccount()),
                MovementType.CREDIT,
                event.getAmount(),
                toAccount.getBalance()
        );
        movementRepository.save(creditMovement);

        eventPublisher.publish("bank.account.events",
                new AccountCreditedEvent(event.getTransferId(), toAccount.getId(), event.getToAccount(),
                        event.getAmount(), toAccount.getBalance(), event.getCurrency()));

        log.info("Account {} credited by {}", event.getToAccount(), event.getAmount());
    }
}
