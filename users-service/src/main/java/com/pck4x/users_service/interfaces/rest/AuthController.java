package com.pck4x.users_service.interfaces.rest;

import com.pck4x.sharedcontracts.helper.ResponseHelper;
import com.pck4x.sharedcontracts.objects.ApiResponse;
import com.pck4x.sharedcontracts.result.OutputPort;
import com.pck4x.users_service.application.dto.command.LoginCommand;
import com.pck4x.users_service.application.dto.command.RefreshTokenCommand;
import com.pck4x.users_service.application.dto.command.RegisterAdminCommand;
import com.pck4x.users_service.application.dto.command.RegisterUserCommand;
import com.pck4x.users_service.application.dto.response.AuthResponse;
import com.pck4x.users_service.application.feature.login.LoginUseCase;
import com.pck4x.users_service.application.feature.refresh.RefreshTokenUseCase;
import com.pck4x.users_service.application.feature.register.RegisterAdminUseCase;
import com.pck4x.users_service.application.feature.register.RegisterUseCase;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final RegisterUseCase registerUseCase;
    private final RegisterAdminUseCase registerAdminUseCase;
    private final LoginUseCase loginUseCase;
    private final RefreshTokenUseCase refreshTokenUseCase;
    private final String bootstrapSecret;

    public AuthController(RegisterUseCase registerUseCase,
                          RegisterAdminUseCase registerAdminUseCase,
                          LoginUseCase loginUseCase,
                          RefreshTokenUseCase refreshTokenUseCase,
                          @Value("${app.admin-bootstrap-secret}") String bootstrapSecret) {
        this.registerUseCase = registerUseCase;
        this.registerAdminUseCase = registerAdminUseCase;
        this.loginUseCase = loginUseCase;
        this.refreshTokenUseCase = refreshTokenUseCase;
        this.bootstrapSecret = bootstrapSecret;
    }

    @GetMapping("/test")
    public String test() {
        return "Hello World!";
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<UUID>> register(@RequestBody RegisterUserCommand command) {
        var result = registerUseCase.execute(command);
        return ResponseHelper.toResponse(result);
    }

    @PostMapping("/register-admin")
    public ResponseEntity<ApiResponse<UUID>> registerAdmin(
            @RequestBody RegisterAdminCommand command,
            @RequestHeader("X-Bootstrap-Secret") String secret) {
        if (!bootstrapSecret.equals(secret)) {
            return ResponseHelper.toResponse(OutputPort.failure(
                    org.springframework.http.HttpStatus.UNAUTHORIZED, "Invalid bootstrap secret"));
        }
        var result = registerAdminUseCase.execute(command);
        return ResponseHelper.toResponse(result);
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@RequestBody LoginCommand command) {
        var result = loginUseCase.execute(command);
        return ResponseHelper.toResponse(result);
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(@RequestBody RefreshTokenCommand command) {
        var result = refreshTokenUseCase.execute(command);
        return ResponseHelper.toResponse(result);
    }
}
