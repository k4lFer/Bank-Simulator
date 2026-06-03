package com.pck4x.accounts_service.infrastructure.persistence.mapper;

import com.pck4x.accounts_service.domain.Account;
import com.pck4x.accounts_service.infrastructure.persistence.jpa.entities.AccountEntity;

import java.time.LocalDateTime;

public class AccountMapper {

    public static final AccountMapper INSTANCE = new AccountMapper();

    private AccountMapper() {}

    public AccountEntity toEntity(Account account) {
        AccountEntity entity = new AccountEntity();
        entity.setId(account.getId());
        entity.setAccountNumber(account.getAccountNumber());
        entity.setBalance(account.getBalance());
        entity.setCurrency(account.getCurrency());
        entity.setStatus(account.getStatus());
        entity.setUserId(account.getUserId());
        entity.setPin6(account.getPin6());
        entity.setPin4(account.getPin4());
        entity.setCreatedAt(account.getCreatedAt() != null ? account.getCreatedAt() : LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
        return entity;
    }

    public Account toDomain(AccountEntity entity) {
        Account account = new Account();
        account.setId(entity.getId());
        account.setAccountNumber(entity.getAccountNumber());
        account.setBalance(entity.getBalance());
        account.setCurrency(entity.getCurrency());
        account.setStatus(entity.getStatus());
        account.setUserId(entity.getUserId());
        account.setPin6(entity.getPin6());
        account.setPin4(entity.getPin4());
        account.setCreatedAt(entity.getCreatedAt());
        account.setUpdatedAt(entity.getUpdatedAt());
        return account;
    }
}
