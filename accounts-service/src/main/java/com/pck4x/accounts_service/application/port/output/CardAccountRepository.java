package com.pck4x.accounts_service.application.port.output;

import com.pck4x.accounts_service.domain.CardAccount;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CardAccountRepository {
    CardAccount save(CardAccount cardAccount);
    Optional<CardAccount> findByCardIdAndAccountId(UUID cardId, UUID accountId);
    List<CardAccount> findByCardId(UUID cardId);
    List<CardAccount> findByAccountId(UUID accountId);
    void deleteByCardIdAndAccountId(UUID cardId, UUID accountId);
}
