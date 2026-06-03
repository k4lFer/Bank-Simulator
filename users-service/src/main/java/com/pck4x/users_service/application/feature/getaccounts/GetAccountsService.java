package com.pck4x.users_service.application.feature.getaccounts;

import com.pck4x.sharedcontracts.objects.QueryResult;
import com.pck4x.sharedcontracts.result.OutputPort;
import com.pck4x.users_service.application.dto.response.AccountResponse;
import com.pck4x.users_service.application.port.output.AccountProjectionRepository;
import com.pck4x.users_service.application.port.output.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class GetAccountsService implements GetAccountsUseCase {

    private final UserRepository userRepository;
    private final AccountProjectionRepository projectionRepository;

    public GetAccountsService(UserRepository userRepository,
                              AccountProjectionRepository projectionRepository) {
        this.userRepository = userRepository;
        this.projectionRepository = projectionRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public OutputPort<QueryResult<List<AccountResponse>>> execute(UUID userId, int page, int size) {
        if (userRepository.findById(userId).isEmpty()) {
            return OutputPort.notFound("User not found");
        }

        return OutputPort.ok(projectionRepository.findAccountsByUserId(userId, page, size));
    }
}
