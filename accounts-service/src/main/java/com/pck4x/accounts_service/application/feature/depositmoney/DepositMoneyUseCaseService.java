package com.pck4x.accounts_service.application.feature.depositmoney;

import com.pck4x.accounts_service.application.dto.command.DepositCommand;
import com.pck4x.accounts_service.application.dto.response.DepositResponse;
import com.pck4x.accounts_service.application.mapper.AccountDtoMapper;
import com.pck4x.accounts_service.application.port.output.AccountMovementRepository;
import com.pck4x.accounts_service.application.port.output.AccountRepository;
import com.pck4x.accounts_service.application.port.output.EventPublisher;
import com.pck4x.accounts_service.application.port.output.MovementNumberGenerator;
import com.pck4x.accounts_service.domain.Account;
import com.pck4x.accounts_service.domain.AccountMovement;
import com.pck4x.accounts_service.domain.enums.MovementType;
import com.pck4x.sharedcontracts.enums.AccountStatus;
import com.pck4x.sharedcontracts.event.AccountDepositedEvent;
import com.pck4x.sharedcontracts.result.OutputPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class DepositMoneyUseCaseService implements DepositMoneyUseCase {

    private final AccountRepository accountRepository;
    private final AccountMovementRepository movementRepository;
    private final MovementNumberGenerator movementNumberGenerator;
    private final EventPublisher eventPublisher;

    public DepositMoneyUseCaseService(AccountRepository accountRepository,
                                       AccountMovementRepository movementRepository,
                                       MovementNumberGenerator movementNumberGenerator,
                                       EventPublisher eventPublisher) {
        this.accountRepository = accountRepository;
        this.movementRepository = movementRepository;
        this.movementNumberGenerator = movementNumberGenerator;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional
    public OutputPort<DepositResponse> execute(DepositCommand command, UUID userId) {
        Account account = accountRepository.findByAccountNumber(command.getAccountNumber())
                .orElse(null);

        if (account == null) {
            return OutputPort.notFound("Account not found");
        }

        if (account.getStatus() != AccountStatus.ACTIVE) {
            return OutputPort.badRequest("Account is not active");
        }

        if (!account.getCurrency().equalsIgnoreCase(command.getCurrency())) {
            return OutputPort.badRequest("Currency does not match account currency");
        }

        if (!account.getPin4().equals(command.getPin4())) {
            return OutputPort.badRequest("Invalid PIN");
        }

        account.setBalance(account.getBalance().add(command.getAmount()));
        account = accountRepository.save(account);

        eventPublisher.publish("bank.account.events",
                new AccountDepositedEvent(account.getId(), account.getAccountNumber(),
                        command.getAmount(), account.getBalance(), account.getCurrency()));

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
