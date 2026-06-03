package com.pck4x.users_service.application.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

public record UserResponse (
        UUID id,
        String firstName,
        String lastName,
        String email,
        String phone,
        String role,
        boolean active,
        UserProfileResponse profile,
        LocalDateTime createdAt
) { }
