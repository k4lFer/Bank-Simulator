package com.pck4x.accounts_service.application.feature.blockaccount;

import com.pck4x.accounts_service.application.dto.response.AccountStatusResponse;
import com.pck4x.sharedcontracts.result.OutputPort;

import java.util.UUID;

public interface BlockAccountUseCase {
    OutputPort<AccountStatusResponse> execute(UUID accountId, UUID userId);
}
