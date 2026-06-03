package com.pck4x.users_service.application.port.output;

import com.pck4x.users_service.domain.User;

public interface JwtTokenProvider {
    String generateAccessToken(User user);
    String generateRefreshToken(User user);
    String getUserIdFromToken(String token);
    String getRoleFromToken(String token);
    boolean validateToken(String token);
}
