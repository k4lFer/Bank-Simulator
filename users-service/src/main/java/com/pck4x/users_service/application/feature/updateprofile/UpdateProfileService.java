package com.pck4x.users_service.application.feature.updateprofile;

import com.pck4x.sharedcontracts.result.OutputPort;
import com.pck4x.users_service.application.dto.command.UpdateProfileCommand;
import com.pck4x.users_service.application.dto.response.UserResponse;
import com.pck4x.users_service.application.port.output.UserRepository;
import com.pck4x.users_service.domain.User;
import com.pck4x.users_service.domain.UserProfile;
import com.pck4x.users_service.application.mapper.UserMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class UpdateProfileService implements UpdateProfileUseCase {

    private final UserRepository userRepository;

    public UpdateProfileService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public OutputPort<UserResponse> execute(UUID userId, UpdateProfileCommand command) {
        User user = userRepository.findById(userId)
                .orElse(null);

        if (user == null) {
            return OutputPort.notFound("User not found");
        }

        UserProfile profile = user.getProfile();
        if (profile == null) {
            profile = new UserProfile();
            user.setProfile(profile);
        }

        if (command.getDateOfBirth() != null) {
            profile.setDateOfBirth(command.getDateOfBirth());
        }
        if (command.getAddress() != null) {
            profile.setAddress(command.getAddress());
        }
        if (command.getIdDocument() != null) {
            profile.setIdDocument(command.getIdDocument());
        }
        if (command.getOccupation() != null) {
            profile.setOccupation(command.getOccupation());
        }

        user = userRepository.save(user);

        return OutputPort.ok(UserMapper.INSTANCE.toResponse(user), "Profile updated successfully");
    }
}
