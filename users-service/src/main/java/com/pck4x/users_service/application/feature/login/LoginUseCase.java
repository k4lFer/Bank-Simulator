package com.pck4x.users_service.application.feature.login;

import com.pck4x.sharedcontracts.result.OutputPort;
import com.pck4x.users_service.application.dto.command.LoginCommand;
import com.pck4x.users_service.application.dto.response.AuthResponse;

public interface LoginUseCase {
    OutputPort<AuthResponse> execute(LoginCommand command);
}
