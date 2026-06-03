package com.pck4x.users_service.application.feature.updateprofile;

import com.pck4x.sharedcontracts.result.OutputPort;
import com.pck4x.users_service.application.dto.command.UpdateProfileCommand;
import com.pck4x.users_service.application.dto.response.UserResponse;

import java.util.UUID;

public interface UpdateProfileUseCase {
    OutputPort<UserResponse> execute(UUID userId, UpdateProfileCommand command);
}
