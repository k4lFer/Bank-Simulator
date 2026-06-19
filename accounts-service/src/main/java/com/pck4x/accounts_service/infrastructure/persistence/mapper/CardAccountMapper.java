package com.pck4x.accounts_service.infrastructure.persistence.mapper;

import com.pck4x.accounts_service.domain.CardAccount;
import com.pck4x.accounts_service.infrastructure.persistence.jpa.entities.CardAccountEntity;

public class CardAccountMapper {

    public static final CardAccountMapper INSTANCE = new CardAccountMapper();

    private CardAccountMapper() {}

    public CardAccountEntity toEntity(CardAccount cardAccount) {
        CardAccountEntity entity = new CardAccountEntity();
        entity.setCardId(cardAccount.getCardId());
        entity.setAccountId(cardAccount.getAccountId());
        entity.setPrimary(cardAccount.isPrimary());
        entity.setCurrency(cardAccount.getCurrency());
        return entity;
    }

    public CardAccount toDomain(CardAccountEntity entity) {
        CardAccount cardAccount = new CardAccount();
        cardAccount.setCardId(entity.getCardId());
        cardAccount.setAccountId(entity.getAccountId());
        cardAccount.setPrimary(entity.isPrimary());
        cardAccount.setCurrency(entity.getCurrency());
        return cardAccount;
    }
}
