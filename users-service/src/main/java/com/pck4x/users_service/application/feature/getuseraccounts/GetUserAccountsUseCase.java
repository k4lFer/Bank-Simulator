package com.pck4x.users_service.application.feature.getuseraccounts;

import com.pck4x.sharedcontracts.result.OutputPort;
import com.pck4x.users_service.application.dto.response.UserAccountsResponse;

import java.util.UUID;

public interface GetUserAccountsUseCase {
    OutputPort<UserAccountsResponse> execute(UUID userId);
}
