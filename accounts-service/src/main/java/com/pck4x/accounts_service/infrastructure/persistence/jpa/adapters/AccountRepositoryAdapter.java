package com.pck4x.accounts_service.infrastructure.persistence.jpa.adapters;

import com.pck4x.accounts_service.application.port.output.AccountRepository;
import com.pck4x.accounts_service.domain.Account;
import com.pck4x.accounts_service.infrastructure.persistence.jpa.entities.AccountEntity;
import com.pck4x.accounts_service.infrastructure.persistence.jpa.repositories.JpaAccountRepository;
import com.pck4x.accounts_service.infrastructure.persistence.mapper.AccountMapper;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class AccountRepositoryAdapter implements AccountRepository {

    private final JpaAccountRepository jpaAccountRepository;

    public AccountRepositoryAdapter(JpaAccountRepository jpaAccountRepository) {
        this.jpaAccountRepository = jpaAccountRepository;
    }

    @Override
    public Account save(Account account) {
        AccountEntity entity = AccountMapper.INSTANCE.toEntity(account);
        entity = jpaAccountRepository.save(entity);
        return AccountMapper.INSTANCE.toDomain(entity);
    }

    @Override
    public Optional<Account> findById(UUID id) {
        return jpaAccountRepository.findById(id)
                .map(AccountMapper.INSTANCE::toDomain);
    }

    @Override
    public Optional<Account> findByAccountNumber(String accountNumber) {
        return jpaAccountRepository.findByAccountNumber(accountNumber)
                .map(AccountMapper.INSTANCE::toDomain);
    }

    @Override
    public List<Account> findByUserId(UUID userId) {
        return jpaAccountRepository.findByUserId(userId)
                .stream()
                .map(AccountMapper.INSTANCE::toDomain)
                .toList();
    }

    @Override
    public Optional<Account> findByIdAndUserId(UUID id, UUID userId) {
        return jpaAccountRepository.findByIdAndUserId(id, userId)
                .map(AccountMapper.INSTANCE::toDomain);
    }

    @Override
    public boolean existsByAccountNumber(String accountNumber) {
        return jpaAccountRepository.existsByAccountNumber(accountNumber);
    }
}
