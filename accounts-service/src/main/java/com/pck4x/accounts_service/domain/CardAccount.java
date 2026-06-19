package com.pck4x.accounts_service.domain;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class CardAccount {
    private UUID cardId;
    private UUID accountId;
    private boolean primary;
    private String currency;

    public CardAccount() {}

    public CardAccount(UUID cardId, UUID accountId, boolean primary, String currency) {
        this.cardId = cardId;
        this.accountId = accountId;
        this.primary = primary;
        this.currency = currency;
    }
}
