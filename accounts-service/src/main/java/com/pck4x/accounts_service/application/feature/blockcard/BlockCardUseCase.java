package com.pck4x.accounts_service.application.feature.blockcard;

import com.pck4x.accounts_service.application.dto.response.CardResponse;
import com.pck4x.sharedcontracts.result.OutputPort;

import java.util.UUID;

public interface BlockCardUseCase {
    OutputPort<CardResponse> execute(UUID cardId, UUID userId);
}
