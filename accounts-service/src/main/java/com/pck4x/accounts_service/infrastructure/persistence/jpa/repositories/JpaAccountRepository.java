package com.pck4x.accounts_service.infrastructure.persistence.jpa.repositories;

import com.pck4x.accounts_service.infrastructure.persistence.jpa.entities.AccountEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JpaAccountRepository extends JpaRepository<AccountEntity, UUID> {
    Optional<AccountEntity> findByAccountNumber(String accountNumber);
    List<AccountEntity> findByUserId(UUID userId);
    Optional<AccountEntity> findByIdAndUserId(UUID id, UUID userId);
    boolean existsByAccountNumber(String accountNumber);
}
