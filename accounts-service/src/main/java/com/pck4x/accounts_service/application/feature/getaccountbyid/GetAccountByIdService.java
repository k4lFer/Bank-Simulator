package com.pck4x.accounts_service.application.feature.getaccountbyid;

import com.pck4x.accounts_service.application.dto.response.AccountDetailResponse;
import com.pck4x.accounts_service.application.mapper.AccountDtoMapper;
import com.pck4x.accounts_service.application.port.output.AccountMovementRepository;
import com.pck4x.accounts_service.application.port.output.AccountRepository;
import com.pck4x.sharedcontracts.result.OutputPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class GetAccountByIdService implements GetAccountByIdUseCase {

    private final AccountRepository accountRepository;
    private final AccountMovementRepository movementRepository;

    public GetAccountByIdService(AccountRepository accountRepository,
                                  AccountMovementRepository movementRepository) {
        this.accountRepository = accountRepository;
        this.movementRepository = movementRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public OutputPort<AccountDetailResponse> execute(UUID accountId, UUID userId) {
        var account = accountRepository.findByIdAndUserId(accountId, userId);

        if (account.isEmpty()) {
            return OutputPort.notFound("Account not found");
        }

        int movementCount = movementRepository.countByAccountId(accountId);

        return OutputPort.ok(
                AccountDtoMapper.INSTANCE.toDetailResponse(account.get(), movementCount)
        );
    }
}
