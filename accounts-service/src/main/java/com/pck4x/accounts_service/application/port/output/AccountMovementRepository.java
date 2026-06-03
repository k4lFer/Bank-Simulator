package com.pck4x.accounts_service.application.port.output;

import com.pck4x.accounts_service.domain.AccountMovement;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AccountMovementRepository {
    AccountMovement save(AccountMovement movement);
    Optional<AccountMovement> findById(Long id);
    List<AccountMovement> findByAccountId(UUID accountId);
    int countByAccountId(UUID accountId);
}
