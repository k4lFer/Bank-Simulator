package com.pck4x.users_service.application.feature.refresh;

import com.pck4x.sharedcontracts.result.OutputPort;
import com.pck4x.users_service.application.dto.command.RefreshTokenCommand;
import com.pck4x.users_service.application.dto.response.AuthResponse;

public interface RefreshTokenUseCase {
    OutputPort<AuthResponse> execute(RefreshTokenCommand command);
}
