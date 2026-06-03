package com.pck4x.accounts_service.application.feature.getaccountsbyuser;

import com.pck4x.sharedcontracts.result.OutputPort;
import com.pck4x.accounts_service.application.dto.response.AccountInfoResponse;

import java.util.List;
import java.util.UUID;

public interface GetAccountsByUserUseCase {
    OutputPort<List<AccountInfoResponse>> execute(UUID userId);
}
