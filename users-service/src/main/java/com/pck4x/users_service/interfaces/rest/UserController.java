package com.pck4x.users_service.interfaces.rest;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.pck4x.sharedcontracts.helper.ResponseHelper;
import com.pck4x.sharedcontracts.objects.ApiResponse;
import com.pck4x.sharedcontracts.objects.QueryResult;
import com.pck4x.users_service.application.dto.command.UpdateProfileCommand;
import com.pck4x.users_service.application.dto.response.AccountResponse;
import com.pck4x.users_service.application.dto.response.UserAccountsResponse;
import com.pck4x.users_service.application.dto.response.UserResponse;
import com.pck4x.users_service.application.feature.getaccounts.GetAccountsUseCase;
import com.pck4x.users_service.application.feature.getprofile.GetProfileUseCase;
import com.pck4x.users_service.application.feature.getuseraccounts.GetUserAccountsUseCase;
import com.pck4x.users_service.application.feature.updateprofile.UpdateProfileUseCase;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;

@RestController
@RequestMapping("/api/users")
@SecurityRequirement(name = "bearerAuth")
public class UserController {

    private final GetProfileUseCase getProfileUseCase;
    private final UpdateProfileUseCase updateProfileUseCase;
    private final GetAccountsUseCase getAccountsUseCase;
    private final GetUserAccountsUseCase getUserAccountsUseCase;

    public UserController(GetProfileUseCase getProfileUseCase,
                          UpdateProfileUseCase updateProfileUseCase,
                          GetAccountsUseCase getAccountsUseCase,
                          GetUserAccountsUseCase getUserAccountsUseCase) {
        this.getProfileUseCase = getProfileUseCase;
        this.updateProfileUseCase = updateProfileUseCase;
        this.getAccountsUseCase = getAccountsUseCase;
        this.getUserAccountsUseCase = getUserAccountsUseCase;
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> getProfile(Authentication authentication) {
        UUID userId = (UUID) authentication.getPrincipal();
        var result = getProfileUseCase.execute(userId);
        return ResponseHelper.toResponse(result);
    }

    @PutMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> updateProfile(Authentication authentication,
                                                                    @RequestBody UpdateProfileCommand command) {
        UUID userId = (UUID) authentication.getPrincipal();
        var result = updateProfileUseCase.execute(userId, command);
        return ResponseHelper.toResponse(result);
    }

    @GetMapping("/me/accounts")
    public ResponseEntity<ApiResponse<QueryResult<List<AccountResponse>>>> getMyAccounts(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        UUID userId = (UUID) authentication.getPrincipal();
        var result = getAccountsUseCase.execute(userId, page, size);
        return ResponseHelper.toResponse(result);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> getUserById(@PathVariable UUID id) {
        var result = getProfileUseCase.execute(id);
        return ResponseHelper.toResponse(result);
    }

    @GetMapping("/{id}/accounts")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserAccountsResponse>> getUserAccounts(@PathVariable UUID id) {
        var result = getUserAccountsUseCase.execute(id);
        return ResponseHelper.toResponse(result);
    }
}
