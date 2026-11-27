package com.pck4x.accounts_service.infrastructure.kafka;

import com.pck4x.accounts_service.application.port.output.AccountMovementRepository;
import com.pck4x.accounts_service.application.port.output.AccountRepository;
import com.pck4x.accounts_service.application.port.output.CardAccountRepository;
import com.pck4x.accounts_service.application.port.output.CardRepository;
import com.pck4x.accounts_service.application.port.output.EventPublisher;
import com.pck4x.accounts_service.application.port.output.MovementNumberGenerator;
import com.pck4x.accounts_service.application.port.output.ProcessedEventRepository;
import com.pck4x.accounts_service.domain.Account;
import com.pck4x.accounts_service.domain.AccountMovement;
import com.pck4x.accounts_service.domain.CardAccount;
import com.pck4x.accounts_service.domain.enums.CardStatus;
import com.pck4x.accounts_service.domain.enums.MovementType;
import com.pck4x.sharedcontracts.command.SagaCommand;
import com.pck4x.sharedcontracts.command.TransferCompensateCommand;
import com.pck4x.sharedcontracts.command.TransferCreditCommand;
import com.pck4x.sharedcontracts.command.TransferDebitCommand;
import com.pck4x.sharedcontracts.enums.AccountStatus;
import com.pck4x.sharedcontracts.event.AccountCreditFailedEvent;
import com.pck4x.sharedcontracts.event.AccountCreditedEvent;
import com.pck4x.sharedcontracts.event.AccountDebitCompensatedEvent;
import com.pck4x.sharedcontracts.event.AccountDebitedEvent;
import com.pck4x.sharedcontracts.event.AccountRejectedEvent;
import com.pck4x.sharedcontracts.money.Money;
import com.pck4x.sharedcontracts.security.CorrelationContext;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;

/**
 * Fase 2.4: accounts ejecuta comandos del orquestador, cada uno en su propia
 * transaccion local. El debito y el credito dejan de estar en la misma tx.
 */
@Component
@ConditionalOnProperty(prefix = "app.saga", name = "orchestrated", havingValue = "true", matchIfMissing = true)
public class TransferCommandConsumer {

    private static final Logger log = LoggerFactory.getLogger(TransferCommandConsumer.class);
    private static final String ACCOUNT_EVENTS = "bank.account.events";
    private static final String CONSUMER = "accounts-commands";

    private final AccountRepository accountRepository;
    private final AccountMovementRepository movementRepository;
    private final MovementNumberGenerator movementNumberGenerator;
    private final EventPublisher eventPublisher;
    private final CardRepository cardRepository;
    private final CardAccountRepository cardAccountRepository;
    private final ProcessedEventRepository processedEventRepository;

    public TransferCommandConsumer(AccountRepository accountRepository,
                                   AccountMovementRepository movementRepository,
                                   MovementNumberGenerator movementNumberGenerator,
                                   EventPublisher eventPublisher,
                                   CardRepository cardRepository,
                                   CardAccountRepository cardAccountRepository,
                                   ProcessedEventRepository processedEventRepository) {
        this.accountRepository = accountRepository;
        this.movementRepository = movementRepository;
        this.movementNumberGenerator = movementNumberGenerator;
        this.eventPublisher = eventPublisher;
        this.cardRepository = cardRepository;
        this.cardAccountRepository = cardAccountRepository;
        this.processedEventRepository = processedEventRepository;
    }

    @Transactional
    @KafkaListener(topics = "bank.transfer.commands", groupId = "accounts-service")
    public void handleCommand(ConsumerRecord<String, Object> record) {
        Object command = record.value();
        if (!(command instanceof SagaCommand sagaCommand)) {
            log.debug("Ignored command type: {}", command == null ? "null" : command.getClass().getSimpleName());
            return;
        }
        if (!markProcessed(sagaCommand)) {
            return;
        }

        if (command instanceof TransferDebitCommand debit) {
            handleDebit(debit);
        } else if (command instanceof TransferCreditCommand credit) {
            handleCredit(credit);
        } else if (command instanceof TransferCompensateCommand compensate) {
            handleCompensate(compensate);
        } else {
            log.debug("Ignored saga command: {}", command.getClass().getSimpleName());
        }
    }

    private boolean markProcessed(SagaCommand command) {
        String eventId = command.getCommandId() != null
                ? command.getCommandId().toString()
                : "command:" + command.getClass().getSimpleName() + ":" + command.getTransferId();
        if (processedEventRepository.isProcessed(eventId, CONSUMER)) {
            log.info("Duplicate command ignored by inbox: commandId={}", eventId);
            return false;
        }
        processedEventRepository.markProcessed(eventId, CONSUMER);
        return true;
    }

    // ------------------------------------------------------------------
    // PASO 1: DEBITAR (validaciones de negocio antes de tocar el saldo)
    // ------------------------------------------------------------------

    private void handleDebit(TransferDebitCommand command) {
        log.info("Debit command: transferId={}, from={}, amount={}",
                command.getTransferId(), command.getFromAccount(), command.getAmount());

        if (movementRepository.findByTransferIdAndType(command.getTransferId(), MovementType.DEBIT).isPresent()) {
            log.info("Debit already applied: transferId={}", command.getTransferId());
            return;
        }

        try {
            Money.of(command.getAmount(), command.getCurrency());
        } catch (IllegalArgumentException e) {
            reject(command, e.getMessage(), command.getFromAccount());
            return;
        }

        boolean isCardPayment = command.getCardId() != null;
        boolean isExternal = !"INTERNAL".equals(command.getTransferType());
        String fromAccountNumber = command.getFromAccount();

        if (isCardPayment) {
            var optCard = cardRepository.findById(command.getCardId());
            if (optCard.isEmpty()) { reject(command, "Card not found", null); return; }
            var card = optCard.get();
            if (card.getStatus() != CardStatus.ACTIVE) { reject(command, "Card is not active", null); return; }
            if (card.getExpiryDate().isBefore(YearMonth.now())) { reject(command, "Card has expired", null); return; }
            if (!card.getPin4().equals(command.getPin4())) { reject(command, "Invalid PIN", null); return; }

            var linkedAccounts = cardAccountRepository.findByCardId(card.getId());
            if (linkedAccounts.isEmpty()) { reject(command, "Card has no linked accounts", null); return; }
            var resolved = accountRepository.findById(resolveAccount(linkedAccounts, command.getCurrency()).getAccountId());
            if (resolved.isEmpty()) { reject(command, "Linked account not found", null); return; }
            fromAccountNumber = resolved.get().getAccountNumber();
            log.info("Card payment resolved fromAccount={}", fromAccountNumber);
        }

        var optFrom = accountRepository.findByAccountNumberForUpdate(fromAccountNumber);
        if (optFrom.isEmpty()) { reject(command, "Source account not found", fromAccountNumber); return; }
        var from = optFrom.get();

        if (from.getStatus() != AccountStatus.ACTIVE) { reject(command, "Source account is blocked", fromAccountNumber); return; }
        if (!isCardPayment && !from.getUserId().equals(command.getUserId())) {
            reject(command, "Source account does not belong to user", fromAccountNumber); return;
        }

        var optTo = accountRepository.findByAccountNumber(command.getToAccount());
        if (optTo.isPresent() && !optTo.get().getCurrency().equals(command.getCurrency())) {
            reject(command, "Currency mismatch", command.getToAccount()); return;
        }

        if (!isExternal && !isCardPayment) {
            if (optTo.isEmpty()) { reject(command, "Destination account not found", command.getToAccount()); return; }
            if (!optTo.get().getUserId().equals(command.getUserId())) {
                reject(command, "Destination account does not belong to user", command.getToAccount()); return;
            }
            if (optTo.get().getStatus() != AccountStatus.ACTIVE) {
                reject(command, "Destination account is blocked", command.getToAccount()); return;
            }
        }

        if (from.getBalance().compareTo(command.getAmount()) < 0) {
            reject(command, "Insufficient funds", fromAccountNumber); return;
        }

        from.setBalance(from.getBalance().subtract(command.getAmount()));
        from.setUpdatedAt(LocalDateTime.now());
        accountRepository.save(from);

        var movement = new AccountMovement(from.getId(), movementNumberGenerator.generate(fromAccountNumber),
                MovementType.DEBIT, command.getAmount(), from.getBalance());
        movement.setTransferId(command.getTransferId());
        movementRepository.save(movement);

        publish(command, new AccountDebitedEvent(command.getTransferId(), from.getId(), fromAccountNumber,
                command.getAmount(), from.getBalance(), command.getCurrency()));
        log.info("Account {} debited by {}", fromAccountNumber, command.getAmount());
    }

    // ------------------------------------------------------------------
    // PASO 2: ACREDITAR (fallo aqui => compensacion, nunca REJECTED)
    // ------------------------------------------------------------------

    private void handleCredit(TransferCreditCommand command) {
        log.info("Credit command: transferId={}, to={}, amount={}",
                command.getTransferId(), command.getToAccount(), command.getAmount());

        if (movementRepository.findByTransferIdAndType(command.getTransferId(), MovementType.CREDIT).isPresent()) {
            log.info("Credit already applied: transferId={}", command.getTransferId());
            return;
        }
        // Un credito que llega despues de la compensacion debe ignorarse: el saldo ya fue devuelto.
        if (movementRepository.findByTransferIdAndType(command.getTransferId(), MovementType.REVERSAL).isPresent()) {
            log.warn("Credit ignored: transfer {} was already compensated", command.getTransferId());
            return;
        }

        try {
            Money.of(command.getAmount(), command.getCurrency());
        } catch (IllegalArgumentException e) {
            creditFailed(command, e.getMessage());
            return;
        }

        var optTo = accountRepository.findByAccountNumberForUpdate(command.getToAccount());
        if (optTo.isEmpty()) {
            creditFailed(command, "Destination account not found");
            return;
        }
        var to = optTo.get();
        if (to.getStatus() != AccountStatus.ACTIVE) {
            creditFailed(command, "Destination account is blocked");
            return;
        }
        if (!to.getCurrency().equals(command.getCurrency())) {
            creditFailed(command, "Currency mismatch");
            return;
        }

        to.setBalance(to.getBalance().add(command.getAmount()));
        to.setUpdatedAt(LocalDateTime.now());
        accountRepository.save(to);

        var movement = new AccountMovement(to.getId(), movementNumberGenerator.generate(command.getToAccount()),
                MovementType.CREDIT, command.getAmount(), to.getBalance());
        movement.setTransferId(command.getTransferId());
        movementRepository.save(movement);

        publish(command, new AccountCreditedEvent(command.getTransferId(), to.getId(), command.getToAccount(),
                command.getAmount(), to.getBalance(), command.getCurrency(), to.getUserId()));
        log.info("Account {} credited by {}", command.getToAccount(), command.getAmount());
    }

    // ------------------------------------------------------------------
    // PASO 3: COMPENSAR (movimiento REVERSAL, nunca un CREDIT normal)
    // ------------------------------------------------------------------

    private void handleCompensate(TransferCompensateCommand command) {
        log.info("Compensate command: transferId={}, from={}, reason={}",
                command.getTransferId(), command.getFromAccount(), command.getReason());

        if (movementRepository.findByTransferIdAndType(command.getTransferId(), MovementType.REVERSAL).isPresent()) {
            log.info("Compensation already applied: transferId={}", command.getTransferId());
            return;
        }

        var optDebit = movementRepository.findByTransferIdAndType(command.getTransferId(), MovementType.DEBIT);
        var optFrom = accountRepository.findByAccountNumberForUpdate(command.getFromAccount());
        if (optFrom.isPresent()) {
            var from = optFrom.get();
            from.setBalance(from.getBalance().add(command.getAmount()));
            from.setUpdatedAt(LocalDateTime.now());
            accountRepository.save(from);

            var reversal = new AccountMovement(from.getId(),
                    movementNumberGenerator.generate(command.getFromAccount()),
                    MovementType.REVERSAL, command.getAmount(), from.getBalance());
            reversal.setTransferId(command.getTransferId());
            reversal.setReversalOf(optDebit.map(m -> m.getMovementNumber()).orElse(null));
            movementRepository.save(reversal);
        } else {
            log.warn("Source account not found during compensation: {}", command.getFromAccount());
        }

        publish(command, new AccountDebitCompensatedEvent(command.getTransferId(), command.getFromAccount(),
                command.getAmount(), command.getCurrency(), command.getReason(),
                optDebit.map(m -> m.getMovementNumber()).orElse(null)));
    }

    private void reject(SagaCommand command, String reason, String accountNumber) {
        log.warn("Transfer rejected before debit: transferId={}, reason={}", command.getTransferId(), reason);
        publish(command, new AccountRejectedEvent(command.getTransferId(), accountNumber, reason));
    }

    private void creditFailed(TransferCreditCommand command, String reason) {
        log.warn("Credit failed after debit: transferId={}, reason={}", command.getTransferId(), reason);
        publish(command, new AccountCreditFailedEvent(command.getTransferId(), command.getToAccount(), reason,
                command.getAmount(), command.getCurrency()));
    }

    private void publish(SagaCommand source, Object event) {
        CorrelationContext.copy(source, event);
        eventPublisher.publish(ACCOUNT_EVENTS, event);
    }

    private CardAccount resolveAccount(List<CardAccount> linkedAccounts, String currency) {
        return linkedAccounts.stream()
                .filter(ca -> ca.getCurrency().equals(currency))
                .findFirst()
                .orElseGet(() -> linkedAccounts.stream()
                        .filter(CardAccount::isPrimary)
                        .findFirst()
                        .orElse(linkedAccounts.get(0)));
    }
}
