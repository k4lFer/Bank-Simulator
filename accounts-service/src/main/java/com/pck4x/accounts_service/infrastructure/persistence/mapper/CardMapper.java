package com.pck4x.accounts_service.infrastructure.persistence.mapper;

import com.pck4x.accounts_service.domain.Card;
import com.pck4x.accounts_service.infrastructure.persistence.jpa.entities.CardEntity;

import java.time.LocalDateTime;

public class CardMapper {

    public static final CardMapper INSTANCE = new CardMapper();

    private CardMapper() {}

    public CardEntity toEntity(Card card) {
        CardEntity entity = new CardEntity();
        entity.setId(card.getId());
        entity.setUserId(card.getUserId());
        entity.setPan(card.getPan());
        entity.setExpiryDate(card.getExpiryDate() != null ? card.getExpiryDate() : java.time.YearMonth.now());
        entity.setPin4(card.getPin4());
        entity.setPin6(card.getPin6());
        entity.setStatus(card.getStatus());
        entity.setCreatedAt(card.getCreatedAt() != null ? card.getCreatedAt() : LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
        return entity;
    }

    public Card toDomain(CardEntity entity) {
        Card card = new Card();
        card.setId(entity.getId());
        card.setUserId(entity.getUserId());
        card.setPan(entity.getPan());
        card.setExpiryDate(entity.getExpiryDate());
        card.setPin4(entity.getPin4());
        card.setPin6(entity.getPin6());
        card.setStatus(entity.getStatus());
        card.setCreatedAt(entity.getCreatedAt());
        card.setUpdatedAt(entity.getUpdatedAt());
        return card;
    }
}
