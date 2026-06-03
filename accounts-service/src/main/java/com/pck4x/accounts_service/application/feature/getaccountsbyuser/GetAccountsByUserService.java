package com.pck4x.accounts_service.application.feature.getaccountsbyuser;

import com.pck4x.sharedcontracts.result.OutputPort;
import com.pck4x.accounts_service.application.dto.response.AccountInfoResponse;
import com.pck4x.accounts_service.application.port.output.AccountRepository;
import com.pck4x.accounts_service.application.mapper.AccountDtoMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class GetAccountsByUserService implements GetAccountsByUserUseCase {

    private final AccountRepository accountRepository;

    public GetAccountsByUserService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public OutputPort<List<AccountInfoResponse>> execute(UUID userId) {
        return OutputPort.ok(
                AccountDtoMapper.INSTANCE.toInfoResponseList(
                        accountRepository.findByUserId(userId)
                )
        );
    }
}
