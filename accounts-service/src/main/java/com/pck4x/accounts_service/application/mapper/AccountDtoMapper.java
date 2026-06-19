package com.pck4x.accounts_service.application.mapper;

import com.pck4x.accounts_service.application.dto.response.AccountCreatedResponse;
import com.pck4x.accounts_service.application.dto.response.AccountDetailResponse;
import com.pck4x.accounts_service.application.dto.response.AccountInfoResponse;
import com.pck4x.accounts_service.application.dto.response.AccountStatusResponse;
import com.pck4x.accounts_service.application.dto.response.DepositResponse;
import com.pck4x.accounts_service.application.dto.response.MovementItemResponse;
import com.pck4x.accounts_service.domain.Account;
import com.pck4x.accounts_service.domain.AccountMovement;

import java.time.LocalDateTime;
import java.util.List;

public class AccountDtoMapper {

    public static final AccountDtoMapper INSTANCE = new AccountDtoMapper();

    private AccountDtoMapper() {}

    public AccountCreatedResponse toCreatedResponse(Account account) {
        return new AccountCreatedResponse(
                account.getId(),
                account.getAccountNumber(),
                account.getBalance(),
                account.getCurrency(),
                account.getStatus(),
                account.getCreatedAt()
        );
    }

    public AccountInfoResponse toInfoResponse(Account account) {
        return new AccountInfoResponse(
                account.getId(),
                account.getAccountNumber(),
                account.getBalance(),
                account.getCurrency(),
                account.getStatus(),
                account.getCreatedAt()
        );
    }

    public List<AccountInfoResponse> toInfoResponseList(List<Account> accounts) {
        return accounts.stream().map(this::toInfoResponse).toList();
    }

    public AccountDetailResponse toDetailResponse(Account account, int movementCount) {
        return new AccountDetailResponse(
                account.getId(),
                account.getAccountNumber(),
                account.getBalance(),
                account.getCurrency(),
                account.getStatus(),
                account.getUserId(),
                movementCount,
                account.getCreatedAt(),
                account.getUpdatedAt()
        );
    }

    public DepositResponse toDepositResponse(AccountMovement movement, String accountNumber) {
        return new DepositResponse(
                movement.getMovementNumber(),
                movement.getAccountId(),
                accountNumber,
                movement.getType(),
                movement.getAmount(),
                movement.getBalanceAfter(),
                movement.getCreatedAt()
        );
    }

    public MovementItemResponse toMovementItemResponse(AccountMovement movement) {
        return new MovementItemResponse(
                movement.getId(),
                movement.getMovementNumber(),
                movement.getType(),
                movement.getAmount(),
                movement.getBalanceAfter(),
                movement.getCreatedAt()
        );
    }

    public List<MovementItemResponse> toMovementItemResponseList(List<AccountMovement> movements) {
        return movements.stream().map(this::toMovementItemResponse).toList();
    }

    public AccountStatusResponse toStatusResponse(Account account) {
        return new AccountStatusResponse(
                account.getId(),
                account.getAccountNumber(),
                account.getStatus(),
                account.getUpdatedAt()
        );
    }

}
