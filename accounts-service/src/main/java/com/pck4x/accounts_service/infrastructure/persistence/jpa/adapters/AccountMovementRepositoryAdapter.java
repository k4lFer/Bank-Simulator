package com.pck4x.accounts_service.infrastructure.persistence.jpa.adapters;

import com.pck4x.accounts_service.application.port.output.AccountMovementRepository;
import com.pck4x.accounts_service.domain.AccountMovement;
import com.pck4x.accounts_service.infrastructure.persistence.jpa.repositories.JpaAccountMovementRepository;
import com.pck4x.accounts_service.infrastructure.persistence.mapper.AccountMovementMapper;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class AccountMovementRepositoryAdapter implements AccountMovementRepository {

    private final JpaAccountMovementRepository jpaRepository;

    public AccountMovementRepositoryAdapter(JpaAccountMovementRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public AccountMovement save(AccountMovement movement) {
        var entity = AccountMovementMapper.INSTANCE.toEntity(movement);
        entity = jpaRepository.save(entity);
        return AccountMovementMapper.INSTANCE.toDomain(entity);
    }

    @Override
    public Optional<AccountMovement> findById(Long id) {
        return jpaRepository.findById(id)
                .map(AccountMovementMapper.INSTANCE::toDomain);
    }

    @Override
    public List<AccountMovement> findByAccountId(UUID accountId) {
        return jpaRepository.findByAccountIdOrderByCreatedAtDesc(accountId)
                .stream()
                .map(AccountMovementMapper.INSTANCE::toDomain)
                .toList();
    }

    @Override
    public int countByAccountId(UUID accountId) {
        return jpaRepository.countByAccountId(accountId);
    }
}
