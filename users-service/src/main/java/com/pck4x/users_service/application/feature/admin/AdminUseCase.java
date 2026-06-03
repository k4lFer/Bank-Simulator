package com.pck4x.users_service.application.feature.admin;

import com.pck4x.sharedcontracts.result.OutputPort;
import com.pck4x.users_service.application.dto.command.UpdateUserStatusCommand;
import com.pck4x.users_service.application.dto.response.UserResponse;

import java.util.List;
import java.util.UUID;

public interface AdminUseCase {
    OutputPort<List<UserResponse>> getAllUsers();
    OutputPort<UserResponse> getUserById(UUID userId);
    OutputPort<UserResponse> toggleUserStatus(UUID userId, UpdateUserStatusCommand command);
    OutputPort<UserResponse> updateUserRole(UUID userId, String role);
}
