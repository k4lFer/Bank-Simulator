package com.pck4x.accounts_service.application.feature.createaccount;

import com.pck4x.sharedcontracts.event.AccountCreatedEvent;
import com.pck4x.sharedcontracts.result.OutputPort;
import com.pck4x.accounts_service.application.dto.command.CreateAccountCommand;
import com.pck4x.accounts_service.application.dto.response.AccountCreatedResponse;
import com.pck4x.accounts_service.application.port.output.AccountNumberGenerator;
import com.pck4x.accounts_service.application.port.output.AccountRepository;
import com.pck4x.accounts_service.application.port.output.EventPublisher;
import com.pck4x.accounts_service.domain.Account;
import com.pck4x.accounts_service.application.mapper.AccountDtoMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
public class CreateAccountService implements CreateAccountUseCase {

    private final AccountRepository accountRepository;
    private final AccountNumberGenerator accountNumberGenerator;
    private final EventPublisher eventPublisher;

    public CreateAccountService(AccountRepository accountRepository,
                                AccountNumberGenerator accountNumberGenerator,
                                EventPublisher eventPublisher) {
        this.accountRepository = accountRepository;
        this.accountNumberGenerator = accountNumberGenerator;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional
    public OutputPort<AccountCreatedResponse> execute(CreateAccountCommand command, UUID userId) {
        Account account = new Account(
                UUID.randomUUID(),
                accountNumberGenerator.generate(),
                command.getCurrency(),
                userId,
                command.getPin6(),
                command.getPin4()
        );

        account = accountRepository.save(account);

        eventPublisher.publish("bank.account.events", new AccountCreatedEvent(
                account.getId(),
                account.getAccountNumber(),
                account.getBalance(),
                account.getCurrency(),
                account.getStatus().name(),
                account.getUserId(),
                account.getPin6(),
                account.getPin4(),
                account.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        ));

        return OutputPort.created(
                AccountDtoMapper.INSTANCE.toCreatedResponse(account),
                "Account created successfully"
        );
    }
}
