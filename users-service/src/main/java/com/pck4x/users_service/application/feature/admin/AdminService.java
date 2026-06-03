package com.pck4x.users_service.application.feature.admin;

import com.pck4x.sharedcontracts.result.OutputPort;
import com.pck4x.users_service.application.dto.command.UpdateUserStatusCommand;
import com.pck4x.users_service.application.dto.response.UserResponse;
import com.pck4x.users_service.application.mapper.UserMapper;
import com.pck4x.users_service.application.port.output.UserRepository;
import com.pck4x.users_service.domain.Role;
import com.pck4x.users_service.domain.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class AdminService implements AdminUseCase {

    private final UserRepository userRepository;

    public AdminService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public OutputPort<List<UserResponse>> getAllUsers() {
        List<User> users = userRepository.findAll();
        return OutputPort.ok(UserMapper.INSTANCE.toResponseList(users));
    }

    @Override
    @Transactional(readOnly = true)
    public OutputPort<UserResponse> getUserById(UUID userId) {
        return userRepository.findById(userId)
                .map(user -> OutputPort.ok(UserMapper.INSTANCE.toResponse(user)))
                .orElseGet(() -> OutputPort.notFound("User not found"));
    }

    @Override
    @Transactional
    public OutputPort<UserResponse> toggleUserStatus(UUID userId, UpdateUserStatusCommand command) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return OutputPort.notFound("User not found");
        }
        user.setActive(command.isActive());
        user = userRepository.save(user);
        return OutputPort.ok(UserMapper.INSTANCE.toResponse(user),
                "User status updated successfully");
    }

    @Override
    @Transactional
    public OutputPort<UserResponse> updateUserRole(UUID userId, String role) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return OutputPort.notFound("User not found");
        }
        try {
            user.setRole(Role.valueOf(role.toUpperCase()));
        } catch (IllegalArgumentException e) {
            return OutputPort.badRequest("Invalid role: " + role);
        }
        user = userRepository.save(user);
        return OutputPort.ok(UserMapper.INSTANCE.toResponse(user),
                "User role updated successfully");
    }
}
