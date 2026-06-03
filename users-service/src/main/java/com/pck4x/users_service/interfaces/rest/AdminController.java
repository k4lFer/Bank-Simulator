package com.pck4x.users_service.interfaces.rest;

import com.pck4x.sharedcontracts.helper.ResponseHelper;
import com.pck4x.sharedcontracts.objects.ApiResponse;
import com.pck4x.users_service.application.dto.command.UpdateUserStatusCommand;
import com.pck4x.users_service.application.dto.response.UserResponse;
import com.pck4x.users_service.application.feature.admin.AdminUseCase;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
@SecurityRequirement(name = "bearerAuth")
public class AdminController {

    private final AdminUseCase adminUseCase;

    public AdminController(AdminUseCase adminUseCase) {
        this.adminUseCase = adminUseCase;
    }

    @GetMapping("/users")
    public ResponseEntity<ApiResponse<List<UserResponse>>> getAllUsers() {
        var result = adminUseCase.getAllUsers();
        return ResponseHelper.toResponse(result);
    }

    @GetMapping("/users/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> getUserById(@PathVariable UUID id) {
        var result = adminUseCase.getUserById(id);
        return ResponseHelper.toResponse(result);
    }

    @PutMapping("/users/{id}/status")
    public ResponseEntity<ApiResponse<UserResponse>> toggleUserStatus(
            @PathVariable UUID id,
            @RequestBody UpdateUserStatusCommand command) {
        var result = adminUseCase.toggleUserStatus(id, command);
        return ResponseHelper.toResponse(result);
    }

    @PutMapping("/users/{id}/role")
    public ResponseEntity<ApiResponse<UserResponse>> updateUserRole(
            @PathVariable UUID id,
            @RequestBody String role) {
        var result = adminUseCase.updateUserRole(id, role);
        return ResponseHelper.toResponse(result);
    }
}
