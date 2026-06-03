package com.pck4x.accounts_service.application.feature.getaccountbyid;

import com.pck4x.accounts_service.application.dto.response.AccountDetailResponse;
import com.pck4x.sharedcontracts.result.OutputPort;

import java.util.UUID;

public interface GetAccountByIdUseCase {
    OutputPort<AccountDetailResponse> execute(UUID accountId, UUID userId);
}
