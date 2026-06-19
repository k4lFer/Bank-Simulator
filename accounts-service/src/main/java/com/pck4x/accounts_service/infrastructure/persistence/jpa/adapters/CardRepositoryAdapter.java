package com.pck4x.accounts_service.infrastructure.persistence.jpa.adapters;

import com.pck4x.accounts_service.application.port.output.CardRepository;
import com.pck4x.accounts_service.domain.Card;
import com.pck4x.accounts_service.infrastructure.persistence.jpa.repositories.JpaCardRepository;
import com.pck4x.accounts_service.infrastructure.persistence.mapper.CardMapper;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class CardRepositoryAdapter implements CardRepository {

    private final JpaCardRepository jpaCardRepository;

    public CardRepositoryAdapter(JpaCardRepository jpaCardRepository) {
        this.jpaCardRepository = jpaCardRepository;
    }

    @Override
    public Card save(Card card) {
        var entity = CardMapper.INSTANCE.toEntity(card);
        entity = jpaCardRepository.save(entity);
        return CardMapper.INSTANCE.toDomain(entity);
    }

    @Override
    public Optional<Card> findById(UUID id) {
        return jpaCardRepository.findById(id).map(CardMapper.INSTANCE::toDomain);
    }

    @Override
    public Optional<Card> findByIdAndUserId(UUID id, UUID userId) {
        return jpaCardRepository.findById(id)
                .filter(e -> e.getUserId().equals(userId))
                .map(CardMapper.INSTANCE::toDomain);
    }

    @Override
    public List<Card> findByUserId(UUID userId) {
        return jpaCardRepository.findByUserId(userId).stream()
                .map(CardMapper.INSTANCE::toDomain)
                .toList();
    }
}
