package com.pck4x.accounts_service.application.mapper;

import com.pck4x.accounts_service.application.dto.response.CardDetailResponse;
import com.pck4x.accounts_service.application.dto.response.CardResponse;
import com.pck4x.accounts_service.domain.Account;
import com.pck4x.accounts_service.domain.Card;
import com.pck4x.accounts_service.domain.CardAccount;

import java.util.List;

public class CardDtoMapper {

    public static final CardDtoMapper INSTANCE = new CardDtoMapper();

    private CardDtoMapper() {}

    public CardResponse toResponse(Card card) {
        return new CardResponse(
                card.getId(),
                card.getUserId(),
                maskPan(card.getPan()),
                card.getExpiryDate(),
                card.getStatus(),
                card.getCreatedAt()
        );
    }

    public List<CardResponse> toResponseList(List<Card> cards) {
        return cards.stream().map(this::toResponse).toList();
    }

    public CardDetailResponse toDetailResponse(Card card, List<CardAccount> linkedAccounts) {
        var accounts = linkedAccounts.stream()
                .map(ca -> new CardDetailResponse.LinkedAccountInfo(
                        ca.getAccountId(), null, ca.getCurrency(), ca.isPrimary()))
                .toList();

        return new CardDetailResponse(
                card.getId(),
                card.getUserId(),
                maskPan(card.getPan()),
                card.getExpiryDate(),
                card.getStatus(),
                accounts,
                card.getCreatedAt(),
                card.getUpdatedAt()
        );
    }

    public CardDetailResponse toDetailResponse(Card card, List<CardAccount> linkedAccounts, List<Account> accounts) {
        var accountMap = accounts.stream()
                .collect(java.util.stream.Collectors.toMap(Account::getId, a -> a));

        var linked = linkedAccounts.stream()
                .map(ca -> {
                    var acc = accountMap.get(ca.getAccountId());
                    return new CardDetailResponse.LinkedAccountInfo(
                            ca.getAccountId(),
                            acc != null ? acc.getAccountNumber() : null,
                            ca.getCurrency(),
                            ca.isPrimary()
                    );
                })
                .toList();

        return new CardDetailResponse(
                card.getId(),
                card.getUserId(),
                maskPan(card.getPan()),
                card.getExpiryDate(),
                card.getStatus(),
                linked,
                card.getCreatedAt(),
                card.getUpdatedAt()
        );
    }

    private String maskPan(String pan) {
        if (pan == null || pan.length() < 8) return "****";
        return pan.substring(0, 6) + "******" + pan.substring(pan.length() - 4);
    }
}
