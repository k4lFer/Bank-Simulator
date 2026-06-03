package com.pck4x.accounts_service.application.feature.createaccount;

import com.pck4x.sharedcontracts.result.OutputPort;
import com.pck4x.accounts_service.application.dto.command.CreateAccountCommand;
import com.pck4x.accounts_service.application.dto.response.AccountCreatedResponse;

import java.util.UUID;

public interface CreateAccountUseCase {
    OutputPort<AccountCreatedResponse> execute(CreateAccountCommand command, UUID userId);
}
