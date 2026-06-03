package com.pck4x.users_service.application.mapper;

import com.pck4x.users_service.application.dto.response.UserProfileResponse;
import com.pck4x.users_service.application.dto.response.UserResponse;
import com.pck4x.users_service.application.port.output.Mapper;
import com.pck4x.users_service.domain.User;

import java.util.List;

public class UserMapper implements Mapper<User, UserResponse> {

    public static final UserMapper INSTANCE = new UserMapper();

    private UserMapper() {}

    @Override
    public UserResponse toResponse(User user) {
        UserProfileResponse profileResponse = null;
        if (user.getProfile() != null) {
            profileResponse = new UserProfileResponse(
                    user.getProfile().getDateOfBirth(),
                    user.getProfile().getAddress(),
                    user.getProfile().getIdDocument(),
                    user.getProfile().getOccupation(),
                    user.getProfile().getCreatedAt()
            );
        }
        return new UserResponse(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getPhone(),
                user.getRole() != null ? user.getRole().name() : null,
                user.isActive(),
                profileResponse,
                user.getCreatedAt()
        );
    }

    @Override
    public List<UserResponse> toResponseList(List<User> domainList) {
        return domainList.stream().map(this::toResponse).toList();
    }
}
