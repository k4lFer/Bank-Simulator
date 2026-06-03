package com.pck4x.users_service.application.feature.getuseraccounts;

import com.pck4x.sharedcontracts.result.OutputPort;
import com.pck4x.users_service.application.dto.response.UserAccountsResponse;
import com.pck4x.users_service.application.port.output.AccountProjectionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class GetUserAccountsService implements GetUserAccountsUseCase {

    private final AccountProjectionRepository projectionRepository;

    public GetUserAccountsService(AccountProjectionRepository projectionRepository) {
        this.projectionRepository = projectionRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public OutputPort<UserAccountsResponse> execute(UUID userId) {
        var response = projectionRepository.findUserAccountsByUserId(userId);
        if (response == null) {
            return OutputPort.notFound("User not found");
        }
        return OutputPort.ok(response);
    }
}
