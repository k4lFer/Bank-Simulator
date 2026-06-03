package com.pck4x.users_service.application.dto.response;

public record AuthResponse (
        String accessToken,
        String refreshToken,
        UserResponse user
) {}
