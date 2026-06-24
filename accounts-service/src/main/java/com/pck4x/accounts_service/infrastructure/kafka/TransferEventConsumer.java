package com.pck4x.accounts_service.infrastructure.kafka;

import com.pck4x.accounts_service.application.port.output.AccountMovementRepository;
import com.pck4x.accounts_service.application.port.output.AccountRepository;
import com.pck4x.accounts_service.application.port.output.CardAccountRepository;
import com.pck4x.accounts_service.application.port.output.CardRepository;
import com.pck4x.accounts_service.application.port.output.EventPublisher;
import com.pck4x.accounts_service.application.port.output.MovementNumberGenerator;
import com.pck4x.accounts_service.domain.AccountMovement;
import com.pck4x.accounts_service.domain.CardAccount;
import com.pck4x.accounts_service.domain.enums.CardStatus;
import com.pck4x.accounts_service.domain.enums.MovementType;
import com.pck4x.sharedcontracts.event.AccountCreditedEvent;
import com.pck4x.sharedcontracts.event.AccountDebitedEvent;
import com.pck4x.sharedcontracts.event.AccountRejectedEvent;
import com.pck4x.sharedcontracts.enums.AccountStatus;
import com.pck4x.sharedcontracts.event.TransferRequestedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.YearMonth;

@Component
public class TransferEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(TransferEventConsumer.class);

    private final AccountRepository accountRepository;
    private final AccountMovementRepository movementRepository;
    private final MovementNumberGenerator movementNumberGenerator;
    private final EventPublisher eventPublisher;
    private final CardRepository cardRepository;
    private final CardAccountRepository cardAccountRepository;

    public TransferEventConsumer(AccountRepository accountRepository,
                                 AccountMovementRepository movementRepository,
                                 MovementNumberGenerator movementNumberGenerator,
                                 EventPublisher eventPublisher,
                                 CardRepository cardRepository,
                                 CardAccountRepository cardAccountRepository) {
        this.accountRepository = accountRepository;
        this.movementRepository = movementRepository;
        this.movementNumberGenerator = movementNumberGenerator;
        this.eventPublisher = eventPublisher;
        this.cardRepository = cardRepository;
        this.cardAccountRepository = cardAccountRepository;
    }

    @Transactional
    @KafkaListener(topics = "bank.transfer.events", groupId = "accounts-service")
    public void handleTransferRequested(TransferRequestedEvent event) {
        log.info("Received TransferRequestedEvent: transferId={}, from={}, to={}, amount={}, type={}",
                event.getTransferId(), event.getFromAccount(), event.getToAccount(),
                event.getAmount(), event.getTransferType());

        if (movementRepository.findByTransferId(event.getTransferId()).isPresent()) {
            log.info("Duplicate TransferRequestedEvent ignored: transferId={}", event.getTransferId());
            return;
        }

        var isCardPayment = event.getCardId() != null;
        var isExternal = "EXTERNAL".equals(event.getTransferType());

        // --- CARD PAYMENT: validate card + resolve fromAccount ---
        if (isCardPayment) {
            var optCard = cardRepository.findById(event.getCardId());
            if (optCard.isEmpty()) {
                eventPublisher.publish("bank.account.events",
                        new AccountRejectedEvent(event.getTransferId(), null,
                                "Card not found", event.getAmount(), event.getCurrency()));
                return;
            }
            var card = optCard.get();

            if (card.getStatus() != CardStatus.ACTIVE) {
                eventPublisher.publish("bank.account.events",
                        new AccountRejectedEvent(event.getTransferId(), null,
                                "Card is not active", event.getAmount(), event.getCurrency()));
                return;
            }

            if (card.getExpiryDate().isBefore(YearMonth.now())) {
                eventPublisher.publish("bank.account.events",
                        new AccountRejectedEvent(event.getTransferId(), null,
                                "Card has expired", event.getAmount(), event.getCurrency()));
                return;
            }

            if (!card.getPin4().equals(event.getPin4())) {
                eventPublisher.publish("bank.account.events",
                        new AccountRejectedEvent(event.getTransferId(), null,
                                "Invalid PIN", event.getAmount(), event.getCurrency()));
                return;
            }

            // Resolve fromAccount from card's linked accounts
            var linkedAccounts = cardAccountRepository.findByCardId(card.getId());
            if (linkedAccounts.isEmpty()) {
                eventPublisher.publish("bank.account.events",
                        new AccountRejectedEvent(event.getTransferId(), null,
                                "Card has no linked accounts", event.getAmount(), event.getCurrency()));
                return;
            }

            var match = resolveAccount(linkedAccounts, event.getCurrency());
            var optAccount = accountRepository.findById(match.getAccountId());
            if (optAccount.isEmpty()) {
                eventPublisher.publish("bank.account.events",
                        new AccountRejectedEvent(event.getTransferId(), null,
                                "Linked account not found", event.getAmount(), event.getCurrency()));
                return;
            }

            var resolved = optAccount.get();
            event.setFromAccount(resolved.getAccountNumber());
            event.setUserId(card.getUserId());

            log.info("Card payment: cardId={}, resolved fromAccount={}", event.getCardId(), resolved.getAccountNumber());
        }

        // 1. Validar cuenta origen
        var optFromAccount = accountRepository.findByAccountNumber(event.getFromAccount());
        if (optFromAccount.isEmpty()) {
            eventPublisher.publish("bank.account.events",
                    new AccountRejectedEvent(event.getTransferId(), event.getFromAccount(),
                            "Source account not found", event.getAmount(), event.getCurrency()));
            return;
        }
        var fromAccount = optFromAccount.get();

        // 2. Validar que la cuenta origen esté activa
        if (fromAccount.getStatus() != AccountStatus.ACTIVE) {
            eventPublisher.publish("bank.account.events",
                    new AccountRejectedEvent(event.getTransferId(), event.getFromAccount(),
                            "Source account is blocked", event.getAmount(), event.getCurrency()));
            return;
        }

        // 3. Validar propiedad (skip para card payment — ya validó con la tarjeta)
        if (!isCardPayment && !fromAccount.getUserId().equals(event.getUserId())) {
            eventPublisher.publish("bank.account.events",
                    new AccountRejectedEvent(event.getTransferId(), event.getFromAccount(),
                            "Source account does not belong to user", event.getAmount(), event.getCurrency()));
            return;
        }

        // 4. Si es interna, validar que la cuenta destino exista, pertenezca al usuario y esté activa
        if (!isExternal && !isCardPayment) {
            var optToAccount = accountRepository.findByAccountNumber(event.getToAccount());
            if (optToAccount.isEmpty()) {
                eventPublisher.publish("bank.account.events",
                        new AccountRejectedEvent(event.getTransferId(), event.getToAccount(),
                                "Destination account not found", event.getAmount(), event.getCurrency()));
                return;
            }
            if (!optToAccount.get().getUserId().equals(event.getUserId())) {
                eventPublisher.publish("bank.account.events",
                        new AccountRejectedEvent(event.getTransferId(), event.getToAccount(),
                                "Destination account does not belong to user", event.getAmount(), event.getCurrency()));
                return;
            }
            if (optToAccount.get().getStatus() != AccountStatus.ACTIVE) {
                eventPublisher.publish("bank.account.events",
                        new AccountRejectedEvent(event.getTransferId(), event.getToAccount(),
                                "Destination account is blocked", event.getAmount(), event.getCurrency()));
                return;
            }
        }

        // 5. Validar fondos suficientes
        if (fromAccount.getBalance().compareTo(event.getAmount()) < 0) {
            eventPublisher.publish("bank.account.events",
                    new AccountRejectedEvent(event.getTransferId(), event.getFromAccount(),
                            "Insufficient funds", event.getAmount(), event.getCurrency()));
            return;
        }

        // 6. Débito de origen
        fromAccount.setBalance(fromAccount.getBalance().subtract(event.getAmount()));
        fromAccount.setUpdatedAt(java.time.LocalDateTime.now());
        accountRepository.save(fromAccount);

        var debitMovement = new AccountMovement(
                fromAccount.getId(),
                movementNumberGenerator.generate(event.getFromAccount()),
                MovementType.DEBIT,
                event.getAmount(),
                fromAccount.getBalance()
        );
        debitMovement.setTransferId(event.getTransferId());
        movementRepository.save(debitMovement);

        eventPublisher.publish("bank.account.events",
                new AccountDebitedEvent(event.getTransferId(), fromAccount.getId(), event.getFromAccount(),
                        event.getAmount(), fromAccount.getBalance(), event.getCurrency()));

        log.info("Account {} debited by {}", event.getFromAccount(), event.getAmount());

        // 7. Sleep solo para transferencias externas (simula procesamiento)
        if (isExternal || isCardPayment) {
            try { Thread.sleep(2000); } catch (InterruptedException ignored) {}
        }

        // 8. Crédito a destino
        var optToAccount = accountRepository.findByAccountNumber(event.getToAccount());

        if (optToAccount.isEmpty()) {
            // ROLLBACK: reversión del débito
            log.warn("Destination account {} not found — reversing debit", event.getToAccount());

            fromAccount.setBalance(fromAccount.getBalance().add(event.getAmount()));
            fromAccount.setUpdatedAt(java.time.LocalDateTime.now());
            accountRepository.save(fromAccount);

            var reversalMovement = new AccountMovement(
                    fromAccount.getId(),
                    movementNumberGenerator.generate(event.getFromAccount()),
                    MovementType.CREDIT,
                    event.getAmount(),
                    fromAccount.getBalance()
            );
            reversalMovement.setTransferId(event.getTransferId());
            movementRepository.save(reversalMovement);

            eventPublisher.publish("bank.account.events",
                    new AccountRejectedEvent(event.getTransferId(), event.getFromAccount(),
                            "Destination account not found — debit reversed",
                            event.getAmount(), event.getCurrency()));
            return;
        }

        var toAccount = optToAccount.get();

        // 9. Validar que la cuenta destino esté activa (solo aplica a externas/card, interna ya validó)
        if (toAccount.getStatus() != AccountStatus.ACTIVE) {
            log.warn("Destination account {} is blocked — reversing debit", event.getToAccount());

            fromAccount.setBalance(fromAccount.getBalance().add(event.getAmount()));
            fromAccount.setUpdatedAt(java.time.LocalDateTime.now());
            accountRepository.save(fromAccount);

            var reversalMovement = new AccountMovement(
                    fromAccount.getId(),
                    movementNumberGenerator.generate(event.getFromAccount()),
                    MovementType.CREDIT,
                    event.getAmount(),
                    fromAccount.getBalance()
            );
            reversalMovement.setTransferId(event.getTransferId());
            movementRepository.save(reversalMovement);

            eventPublisher.publish("bank.account.events",
                    new AccountRejectedEvent(event.getTransferId(), event.getFromAccount(),
                            "Destination account is blocked — debit reversed",
                            event.getAmount(), event.getCurrency()));
            return;
        }

        toAccount.setBalance(toAccount.getBalance().add(event.getAmount()));
        toAccount.setUpdatedAt(java.time.LocalDateTime.now());
        accountRepository.save(toAccount);

        var creditMovement = new AccountMovement(
                toAccount.getId(),
                movementNumberGenerator.generate(event.getToAccount()),
                MovementType.CREDIT,
                event.getAmount(),
                toAccount.getBalance()
        );
        creditMovement.setTransferId(event.getTransferId());
        movementRepository.save(creditMovement);

        eventPublisher.publish("bank.account.events",
                new AccountCreditedEvent(event.getTransferId(), toAccount.getId(), event.getToAccount(),
                        event.getAmount(), toAccount.getBalance(), event.getCurrency(), toAccount.getUserId()));

        log.info("Account {} credited by {}", event.getToAccount(), event.getAmount());
    }

    private CardAccount resolveAccount(java.util.List<CardAccount> linkedAccounts, String currency) {
        // Prefer account matching the currency, fall back to primary
        return linkedAccounts.stream()
                .filter(ca -> ca.getCurrency().equals(currency))
                .findFirst()
                .orElseGet(() -> linkedAccounts.stream()
                        .filter(CardAccount::isPrimary)
                        .findFirst()
                        .orElse(linkedAccounts.get(0)));
    }
}
