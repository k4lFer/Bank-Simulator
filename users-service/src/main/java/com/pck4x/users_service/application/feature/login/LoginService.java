package com.pck4x.users_service.application.feature.login;

import com.pck4x.sharedcontracts.result.OutputPort;
import com.pck4x.users_service.application.dto.command.LoginCommand;
import com.pck4x.users_service.application.dto.response.AuthResponse;
import com.pck4x.users_service.application.port.output.JwtTokenProvider;
import com.pck4x.users_service.application.port.output.UserRepository;
import com.pck4x.users_service.domain.User;
import com.pck4x.users_service.application.mapper.UserMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LoginService implements LoginUseCase {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;

    public LoginService(UserRepository userRepository,
                        PasswordEncoder passwordEncoder,
                        JwtTokenProvider tokenProvider) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
    }

    @Override
    @Transactional(readOnly = true)
    public OutputPort<AuthResponse> execute(LoginCommand command) {

        User user = userRepository.findByEmail(command.getEmail())
                .orElse(null);

        if (user == null || !passwordEncoder.matches(command.getPassword(), user.getPassword())) {
            return OutputPort.badRequest("Invalid email or password");
        }

        String accessToken = tokenProvider.generateAccessToken(user);
        String refreshToken = tokenProvider.generateRefreshToken(user);

        return OutputPort.ok(new AuthResponse(
                accessToken,
                refreshToken,
                UserMapper.INSTANCE.toResponse(user)
        ));
    }
}
