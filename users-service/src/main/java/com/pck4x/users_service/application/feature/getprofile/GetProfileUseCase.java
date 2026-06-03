package com.pck4x.users_service.application.feature.getprofile;

import com.pck4x.sharedcontracts.result.OutputPort;
import com.pck4x.users_service.application.dto.response.UserResponse;

import java.util.UUID;

public interface GetProfileUseCase {
    OutputPort<UserResponse> execute(UUID userId);
}
