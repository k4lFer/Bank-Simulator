package com.pck4x.accounts_service.infrastructure.persistence.mapper;

import com.pck4x.accounts_service.domain.AccountMovement;
import com.pck4x.accounts_service.infrastructure.persistence.jpa.entities.AccountMovementEntity;

import java.time.LocalDateTime;

public class AccountMovementMapper {

    public static final AccountMovementMapper INSTANCE = new AccountMovementMapper();

    private AccountMovementMapper() {}

    public AccountMovementEntity toEntity(AccountMovement movement) {
        AccountMovementEntity entity = new AccountMovementEntity();
        entity.setTransferId(movement.getTransferId());
        entity.setMovementNumber(movement.getMovementNumber());
        entity.setAccountId(movement.getAccountId());
        entity.setType(movement.getType());
        entity.setAmount(movement.getAmount());
        entity.setBalanceAfter(movement.getBalanceAfter());
        entity.setCreatedAt(movement.getCreatedAt() != null ? movement.getCreatedAt() : LocalDateTime.now());
        return entity;
    }

    public AccountMovement toDomain(AccountMovementEntity entity) {
        AccountMovement movement = new AccountMovement();
        movement.setId(entity.getId());
        movement.setTransferId(entity.getTransferId());
        movement.setMovementNumber(entity.getMovementNumber());
        movement.setAccountId(entity.getAccountId());
        movement.setType(entity.getType());
        movement.setAmount(entity.getAmount());
        movement.setBalanceAfter(entity.getBalanceAfter());
        movement.setCreatedAt(entity.getCreatedAt());
        return movement;
    }
}
