package com.pck4x.users_service.application.feature.getaccounts;

import com.pck4x.sharedcontracts.objects.QueryResult;
import com.pck4x.sharedcontracts.result.OutputPort;
import com.pck4x.users_service.application.dto.response.AccountResponse;

import java.util.List;
import java.util.UUID;

public interface GetAccountsUseCase {
    OutputPort<QueryResult<List<AccountResponse>>> execute(UUID userId, int page, int size);
}
