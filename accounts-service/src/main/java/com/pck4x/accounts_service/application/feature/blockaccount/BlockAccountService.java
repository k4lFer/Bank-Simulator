package com.pck4x.accounts_service.application.feature.blockaccount;

import com.pck4x.accounts_service.application.dto.response.AccountStatusResponse;
import com.pck4x.accounts_service.application.mapper.AccountDtoMapper;
import com.pck4x.accounts_service.application.port.output.AccountRepository;
import com.pck4x.sharedcontracts.enums.AccountStatus;
import com.pck4x.sharedcontracts.result.OutputPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class BlockAccountService implements BlockAccountUseCase {

    private final AccountRepository accountRepository;

    public BlockAccountService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    @Override
    @Transactional
    public OutputPort<AccountStatusResponse> execute(UUID accountId, UUID userId) {
        var account = accountRepository.findByIdAndUserId(accountId, userId);

        if (account.isEmpty()) {
            return OutputPort.notFound("Account not found");
        }

        var acc = account.get();

        if (acc.getStatus() == AccountStatus.BLOCKED) {
            return OutputPort.badRequest("Account is already blocked");
        }

        acc.setStatus(AccountStatus.BLOCKED);
        acc.setUpdatedAt(LocalDateTime.now());
        acc = accountRepository.save(acc);

        return OutputPort.ok(
                AccountDtoMapper.INSTANCE.toStatusResponse(acc),
                "Account blocked successfully"
        );
    }
}
