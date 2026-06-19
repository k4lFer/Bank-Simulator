package com.pck4x.accounts_service.infrastructure.persistence.jpa.adapters;

import com.pck4x.accounts_service.application.port.output.CardAccountRepository;
import com.pck4x.accounts_service.domain.CardAccount;
import com.pck4x.accounts_service.infrastructure.persistence.jpa.entities.CardAccountId;
import com.pck4x.accounts_service.infrastructure.persistence.jpa.repositories.JpaCardAccountRepository;
import com.pck4x.accounts_service.infrastructure.persistence.mapper.CardAccountMapper;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class CardAccountRepositoryAdapter implements CardAccountRepository {

    private final JpaCardAccountRepository jpaCardAccountRepository;

    public CardAccountRepositoryAdapter(JpaCardAccountRepository jpaCardAccountRepository) {
        this.jpaCardAccountRepository = jpaCardAccountRepository;
    }

    @Override
    public CardAccount save(CardAccount cardAccount) {
        var entity = CardAccountMapper.INSTANCE.toEntity(cardAccount);
        entity = jpaCardAccountRepository.save(entity);
        return CardAccountMapper.INSTANCE.toDomain(entity);
    }

    @Override
    public Optional<CardAccount> findByCardIdAndAccountId(UUID cardId, UUID accountId) {
        return jpaCardAccountRepository.findById(new CardAccountId(cardId, accountId))
                .map(CardAccountMapper.INSTANCE::toDomain);
    }

    @Override
    public List<CardAccount> findByCardId(UUID cardId) {
        return jpaCardAccountRepository.findByCardId(cardId).stream()
                .map(CardAccountMapper.INSTANCE::toDomain)
                .toList();
    }

    @Override
    public List<CardAccount> findByAccountId(UUID accountId) {
        return jpaCardAccountRepository.findByAccountId(accountId).stream()
                .map(CardAccountMapper.INSTANCE::toDomain)
                .toList();
    }

    @Override
    public void deleteByCardIdAndAccountId(UUID cardId, UUID accountId) {
        jpaCardAccountRepository.deleteByCardIdAndAccountId(cardId, accountId);
    }
}
