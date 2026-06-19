package com.pck4x.accounts_service.infrastructure.persistence.jpa.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "card_accounts")
@IdClass(CardAccountId.class)
@Getter
@Setter
public class CardAccountEntity {

    @Id
    @Column(name = "card_id", nullable = false)
    private UUID cardId;

    @Id
    @Column(name = "account_id", nullable = false)
    private UUID accountId;

    @Column(name = "is_primary", nullable = false)
    private boolean primary;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;
}
