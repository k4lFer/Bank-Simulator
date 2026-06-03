package com.pck4x.accounts_service.interfaces.rest;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Parameter;

import com.pck4x.accounts_service.application.dto.command.CreateAccountCommand;
import com.pck4x.accounts_service.application.dto.command.DepositCommand;
import com.pck4x.accounts_service.application.dto.command.UpdatePinCommand;
import com.pck4x.accounts_service.application.dto.response.AccountCreatedResponse;
import com.pck4x.accounts_service.application.dto.response.AccountDetailResponse;
import com.pck4x.accounts_service.application.dto.response.AccountInfoResponse;
import com.pck4x.accounts_service.application.dto.response.AccountStatusResponse;
import com.pck4x.accounts_service.application.dto.response.DepositResponse;
import com.pck4x.accounts_service.application.dto.response.MovementItemResponse;
import com.pck4x.accounts_service.application.dto.response.PinChangeResponse;
import com.pck4x.accounts_service.application.feature.blockaccount.BlockAccountUseCase;
import com.pck4x.accounts_service.application.feature.createaccount.CreateAccountUseCase;
import com.pck4x.accounts_service.application.feature.depositmoney.DepositMoneyUseCase;
import com.pck4x.accounts_service.application.feature.getaccountbyid.GetAccountByIdUseCase;
import com.pck4x.accounts_service.application.feature.getaccountsbyuser.GetAccountsByUserUseCase;
import com.pck4x.accounts_service.application.feature.getmovements.GetMovementsUseCase;
import com.pck4x.accounts_service.application.feature.updatepin.UpdatePinUseCase;
import com.pck4x.sharedcontracts.helper.ResponseHelper;
import com.pck4x.sharedcontracts.objects.ApiResponse;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    private final CreateAccountUseCase createAccountUseCase;
    private final GetAccountsByUserUseCase getAccountsByUserUseCase;
    private final DepositMoneyUseCase depositMoneyUseCase;
    private final GetAccountByIdUseCase getAccountByIdUseCase;
    private final GetMovementsUseCase getMovementsUseCase;
    private final BlockAccountUseCase blockAccountUseCase;
    private final UpdatePinUseCase updatePinUseCase;

    public AccountController(CreateAccountUseCase createAccountUseCase,
                             GetAccountsByUserUseCase getAccountsByUserUseCase,
                             DepositMoneyUseCase depositMoneyUseCase,
                             GetAccountByIdUseCase getAccountByIdUseCase,
                             GetMovementsUseCase getMovementsUseCase,
                             BlockAccountUseCase blockAccountUseCase,
                             UpdatePinUseCase updatePinUseCase) {
        this.createAccountUseCase = createAccountUseCase;
        this.getAccountsByUserUseCase = getAccountsByUserUseCase;
        this.depositMoneyUseCase = depositMoneyUseCase;
        this.getAccountByIdUseCase = getAccountByIdUseCase;
        this.getMovementsUseCase = getMovementsUseCase;
        this.blockAccountUseCase = blockAccountUseCase;
        this.updatePinUseCase = updatePinUseCase;
    }

    @PostMapping("/create")
    public ResponseEntity<ApiResponse<AccountCreatedResponse>> createAccount(
            @RequestBody CreateAccountCommand command,
            @Parameter(hidden = true) @RequestHeader("X-User-Id") UUID userId) {
        var result = createAccountUseCase.execute(command, userId);
        return ResponseHelper.toResponse(result);
    }

    @PostMapping("/deposit")
    public ResponseEntity<ApiResponse<DepositResponse>> depositMoney(
            @RequestBody DepositCommand command,
            @Parameter(hidden = true) @RequestHeader("X-User-Id") UUID userId) {
        var result = depositMoneyUseCase.execute(command, userId);
        return ResponseHelper.toResponse(result);
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<List<AccountInfoResponse>>> getMyAccounts(
            @Parameter(hidden = true) @RequestHeader("X-User-Id") UUID userId) {
        var result = getAccountsByUserUseCase.execute(userId);
        return ResponseHelper.toResponse(result);
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<ApiResponse<List<AccountInfoResponse>>> getAccountsByUser(
            @PathVariable UUID userId) {
        var result = getAccountsByUserUseCase.execute(userId);
        return ResponseHelper.toResponse(result);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<AccountDetailResponse>> getAccountById(
            @PathVariable UUID id,
            @Parameter(hidden = true) @RequestHeader("X-User-Id") UUID userId) {
        var result = getAccountByIdUseCase.execute(id, userId);
        return ResponseHelper.toResponse(result);
    }

    @GetMapping("/{id}/movements")
    public ResponseEntity<ApiResponse<List<MovementItemResponse>>> getMovements(
            @PathVariable UUID id,
            @Parameter(hidden = true) @RequestHeader("X-User-Id") UUID userId) {
        var result = getMovementsUseCase.execute(id, userId);
        return ResponseHelper.toResponse(result);
    }

    @PatchMapping("/{id}/block")
    public ResponseEntity<ApiResponse<AccountStatusResponse>> blockAccount(
            @PathVariable UUID id,
            @Parameter(hidden = true) @RequestHeader("X-User-Id") UUID userId) {
        var result = blockAccountUseCase.execute(id, userId);
        return ResponseHelper.toResponse(result);
    }

    @PutMapping("/{id}/pin")
    public ResponseEntity<ApiResponse<PinChangeResponse>> updateMyPin(
            @PathVariable UUID id,
            @RequestBody UpdatePinCommand command,
            @Parameter(hidden = true) @RequestHeader("X-User-Id") UUID userId) {
        var result = updatePinUseCase.execute(id, command, userId);
        return ResponseHelper.toResponse(result);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}/pin/admin")
    public ResponseEntity<ApiResponse<PinChangeResponse>> updatePinAsAdmin(
            @PathVariable UUID id,
            @RequestBody UpdatePinCommand command) {
        var result = updatePinUseCase.execute(id, command, null);
        return ResponseHelper.toResponse(result);
    }
}
