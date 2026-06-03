package com.pck4x.accounts_service.domain;

import com.pck4x.accounts_service.domain.enums.MovementType;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
public class AccountMovement {
    private Long id;
    private UUID accountId;
    private String movementNumber;
    private MovementType type;
    private BigDecimal amount;
    private BigDecimal balanceAfter;
    private LocalDateTime createdAt;

    public AccountMovement() {}

    public AccountMovement(UUID accountId, String movementNumber, MovementType type, BigDecimal amount, BigDecimal balanceAfter) {
        this.accountId = accountId;
        this.movementNumber = movementNumber;
        this.type = type;
        this.amount = amount;
        this.balanceAfter = balanceAfter;
        this.createdAt = LocalDateTime.now();
    }
}
