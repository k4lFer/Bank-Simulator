package com.pck4x.users_service.application.feature.getprofile;

import com.pck4x.sharedcontracts.result.OutputPort;
import com.pck4x.users_service.application.dto.response.UserResponse;
import com.pck4x.users_service.application.port.output.UserRepository;
import com.pck4x.users_service.application.mapper.UserMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class GetProfileService implements GetProfileUseCase {

    private final UserRepository userRepository;

    public GetProfileService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public OutputPort<UserResponse> execute(UUID userId) {

        return userRepository.findById(userId)
                .map(user -> OutputPort.ok(UserMapper.INSTANCE.toResponse(user)))
                .orElseGet(() -> OutputPort.notFound("User not found"));
    }
}
