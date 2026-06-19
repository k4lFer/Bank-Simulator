package com.pck4x.accounts_service.application.port.output;

import com.pck4x.accounts_service.domain.Card;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CardRepository {
    Card save(Card card);
    Optional<Card> findById(UUID id);
    Optional<Card> findByIdAndUserId(UUID id, UUID userId);
    List<Card> findByUserId(UUID userId);
}
