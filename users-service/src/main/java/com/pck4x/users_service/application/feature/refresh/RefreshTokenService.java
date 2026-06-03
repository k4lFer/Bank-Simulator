package com.pck4x.users_service.application.feature.refresh;

import com.pck4x.sharedcontracts.result.OutputPort;
import com.pck4x.users_service.application.dto.command.RefreshTokenCommand;
import com.pck4x.users_service.application.dto.response.AuthResponse;
import com.pck4x.users_service.application.port.output.JwtTokenProvider;
import com.pck4x.users_service.application.port.output.UserRepository;
import com.pck4x.users_service.domain.User;
import com.pck4x.users_service.application.mapper.UserMapper;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class RefreshTokenService implements RefreshTokenUseCase {

    private final JwtTokenProvider tokenProvider;
    private final UserRepository userRepository;

    public RefreshTokenService(JwtTokenProvider tokenProvider,
                               UserRepository userRepository) {
        this.tokenProvider = tokenProvider;
        this.userRepository = userRepository;
    }

    @Override
    public OutputPort<AuthResponse> execute(RefreshTokenCommand command) {
        if (!tokenProvider.validateToken(command.refreshToken())) {
            return OutputPort.badRequest("Invalid or expired refresh token");
        }

        String userId = tokenProvider.getUserIdFromToken(command.refreshToken());
        User user = userRepository.findById(UUID.fromString(userId)).orElse(null);
        if (user == null) {
            return OutputPort.notFound("User not found");
        }

        String newAccessToken = tokenProvider.generateAccessToken(user);
        String newRefreshToken = tokenProvider.generateRefreshToken(user);

        return OutputPort.ok(new AuthResponse(
                newAccessToken,
                newRefreshToken,
                UserMapper.INSTANCE.toResponse(user)
        ));
    }
}
