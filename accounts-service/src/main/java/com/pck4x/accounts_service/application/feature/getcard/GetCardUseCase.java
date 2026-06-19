package com.pck4x.accounts_service.application.feature.getcard;

import com.pck4x.accounts_service.application.dto.response.CardDetailResponse;
import com.pck4x.sharedcontracts.result.OutputPort;

import java.util.UUID;

public interface GetCardUseCase {
    OutputPort<CardDetailResponse> execute(UUID cardId, UUID userId);
}
