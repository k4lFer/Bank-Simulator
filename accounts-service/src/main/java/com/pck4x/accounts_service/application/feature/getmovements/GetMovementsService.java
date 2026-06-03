package com.pck4x.accounts_service.application.feature.getmovements;

import com.pck4x.accounts_service.application.dto.response.MovementItemResponse;
import com.pck4x.accounts_service.application.mapper.AccountDtoMapper;
import com.pck4x.accounts_service.application.port.output.AccountMovementRepository;
import com.pck4x.accounts_service.application.port.output.AccountRepository;
import com.pck4x.sharedcontracts.result.OutputPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class GetMovementsService implements GetMovementsUseCase {

    private final AccountRepository accountRepository;
    private final AccountMovementRepository movementRepository;

    public GetMovementsService(AccountRepository accountRepository,
                                AccountMovementRepository movementRepository) {
        this.accountRepository = accountRepository;
        this.movementRepository = movementRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public OutputPort<List<MovementItemResponse>> execute(UUID accountId, UUID userId) {
        var account = accountRepository.findByIdAndUserId(accountId, userId);

        if (account.isEmpty()) {
            return OutputPort.notFound("Account not found");
        }

        var movements = movementRepository.findByAccountId(accountId);

        return OutputPort.ok(
                AccountDtoMapper.INSTANCE.toMovementItemResponseList(movements)
        );
    }
}
