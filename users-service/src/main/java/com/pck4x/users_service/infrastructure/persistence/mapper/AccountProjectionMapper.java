package com.pck4x.users_service.infrastructure.persistence.mapper;

import com.pck4x.users_service.domain.AccountProjection;
import com.pck4x.users_service.infrastructure.persistence.jpa.entities.AccountProjectionEntity;

public class AccountProjectionMapper {

    public static final AccountProjectionMapper INSTANCE = new AccountProjectionMapper();

    private AccountProjectionMapper() {}

    public AccountProjectionEntity toEntity(AccountProjection domain) {
        AccountProjectionEntity entity = new AccountProjectionEntity();
        entity.setId(domain.getId());
        entity.setAccountNumber(domain.getAccountNumber());
        entity.setBalance(domain.getBalance());
        entity.setCurrency(domain.getCurrency());
        entity.setStatus(domain.getStatus());
        entity.setUserId(domain.getUserId());
        entity.setCreatedAt(domain.getCreatedAt());
        return entity;
    }

    public AccountProjection toDomain(AccountProjectionEntity entity) {
        return new AccountProjection(
                entity.getId(),
                entity.getAccountNumber(),
                entity.getBalance(),
                entity.getCurrency(),
                entity.getStatus(),
                entity.getUserId(),
                entity.getCreatedAt()
        );
    }
}
