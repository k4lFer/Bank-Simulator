package com.pck4x.accounts_service.infrastructure.persistence.jpa.entities;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

public class CardAccountId implements Serializable {
    private UUID cardId;
    private UUID accountId;

    public CardAccountId() {}

    public CardAccountId(UUID cardId, UUID accountId) {
        this.cardId = cardId;
        this.accountId = accountId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CardAccountId that = (CardAccountId) o;
        return Objects.equals(cardId, that.cardId) && Objects.equals(accountId, that.accountId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(cardId, accountId);
    }
}
