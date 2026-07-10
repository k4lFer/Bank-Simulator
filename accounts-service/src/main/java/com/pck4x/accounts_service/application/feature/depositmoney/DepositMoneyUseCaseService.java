package com.pck4x.accounts_service.application.feature.depositmoney;

import com.pck4x.accounts_service.application.dto.command.DepositCommand;
import com.pck4x.accounts_service.application.dto.response.DepositResponse;
import com.pck4x.accounts_service.application.mapper.AccountDtoMapper;
import com.pck4x.accounts_service.application.port.output.AccountMovementRepository;
import com.pck4x.accounts_service.application.port.output.AccountRepository;
import com.pck4x.accounts_service.application.port.output.CardAccountRepository;
import com.pck4x.accounts_service.application.port.output.CardRepository;
import com.pck4x.accounts_service.application.port.output.EventPublisher;
import com.pck4x.accounts_service.application.port.output.MovementNumberGenerator;
import com.pck4x.accounts_service.domain.Account;
import com.pck4x.accounts_service.domain.AccountMovement;
import com.pck4x.accounts_service.domain.enums.CardStatus;
import com.pck4x.accounts_service.domain.enums.MovementType;
import com.pck4x.sharedcontracts.enums.AccountStatus;
import com.pck4x.sharedcontracts.event.AccountDepositedEvent;
import com.pck4x.sharedcontracts.event.TransferNotificationEvent;
import com.pck4x.sharedcontracts.result.OutputPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.YearMonth;
import java.util.UUID;

@Service
public class DepositMoneyUseCaseService implements DepositMoneyUseCase {

    private final AccountRepository accountRepository;
    private final AccountMovementRepository movementRepository;
    private final MovementNumberGenerator movementNumberGenerator;
    private final EventPublisher eventPublisher;
    private final CardRepository cardRepository;
    private final CardAccountRepository cardAccountRepository;

    public DepositMoneyUseCaseService(AccountRepository accountRepository,
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

    @Override
    @Transactional
    public OutputPort<DepositResponse> execute(DepositCommand command, UUID userId) {
        Account account;

        if (command.getCardId() != null) {
            // Card deposit: validate card, resolve linked account, skip account PIN
            var optCard = cardRepository.findByIdAndUserId(command.getCardId(), userId);
            if (optCard.isEmpty()) {
                return OutputPort.notFound("Card not found");
            }
            var card = optCard.get();

            if (card.getStatus() != CardStatus.ACTIVE) {
                return OutputPort.badRequest("Card is not active");
            }
            if (card.getExpiryDate().isBefore(YearMonth.now())) {
                return OutputPort.badRequest("Card has expired");
            }
            if (!card.getPin4().equals(command.getPin4())) {
                return OutputPort.badRequest("Invalid PIN");
            }
            if (command.getPin6() == null || command.getPin6().length() != 6) {
                return OutputPort.badRequest("6-digit PIN is required");
            }
            if (!card.getPin6().equals(command.getPin6())) {
                return OutputPort.badRequest("Invalid 6-digit PIN");
            }

            if (command.getAccountNumber() != null) {
                account = accountRepository.findByAccountNumber(command.getAccountNumber()).orElse(null);
            } else {
                return OutputPort.badRequest("Account number is required");
            }
            if (account == null) {
                return OutputPort.notFound("Account not found");
            }
            if (account.getStatus() != AccountStatus.ACTIVE) {
                return OutputPort.badRequest("Account is not active");
            }

            var link = cardAccountRepository.findByCardIdAndAccountId(command.getCardId(), account.getId());
            if (link.isEmpty()) {
                return OutputPort.badRequest("Account is not linked to this card");
            }
        } else {
            // Normal deposit: no PIN required
            account = accountRepository.findByAccountNumber(command.getAccountNumber()).orElse(null);
            if (account == null) {
                return OutputPort.notFound("Account not found");
            }
            if (account.getStatus() != AccountStatus.ACTIVE) {
                return OutputPort.badRequest("Account is not active");
            }
            if (!account.getCurrency().equalsIgnoreCase(command.getCurrency())) {
                return OutputPort.badRequest("Currency does not match account currency");
            }
        }

        account.setBalance(account.getBalance().add(command.getAmount()));
        account = accountRepository.save(account);

        eventPublisher.publish("bank.account.events",
                new AccountDepositedEvent(account.getId(), account.getAccountNumber(),
                        command.getAmount(), account.getBalance(), account.getCurrency()));

        String type = command.getCardId() != null ? "CARD_DEPOSIT" : "DEPOSIT";
        String title = type.equals("CARD_DEPOSIT") ? "Depósito con tarjeta" : "Depósito recibido";
        eventPublisher.publish("bank.notification.events",
                new TransferNotificationEvent(userId, null, type, title,
                        "Se depositó " + command.getAmount() + " " + account.getCurrency()
                                + " en tu cuenta " + account.getAccountNumber(),
                        command.getAmount(), account.getCurrency(), account.getAccountNumber()));

        String movementNumber = movementNumberGenerator.generate(account.getAccountNumber());

        AccountMovement movement = new AccountMovement(
                account.getId(),
                movementNumber,
                MovementType.CREDIT,
                command.getAmount(),
                account.getBalance()
        );
        movement = movementRepository.save(movement);

        return OutputPort.ok(
                AccountDtoMapper.INSTANCE.toDepositResponse(movement, account.getAccountNumber()),
                "Deposit successful"
        );
    }
}
