package com.pck4x.notifications_service.application.feature.unreadcount;

import com.pck4x.notifications_service.application.dto.response.UnreadCountResponse;
import com.pck4x.sharedcontracts.result.OutputPort;

import java.util.UUID;

public interface UnreadCountUseCase {
    OutputPort<UnreadCountResponse> execute(UUID userId);
}
