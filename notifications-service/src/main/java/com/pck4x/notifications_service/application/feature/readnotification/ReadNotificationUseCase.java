package com.pck4x.notifications_service.application.feature.readnotification;

import com.pck4x.notifications_service.application.dto.response.NotificationResponse;
import com.pck4x.sharedcontracts.result.OutputPort;

import java.util.UUID;

public interface ReadNotificationUseCase {
    OutputPort<NotificationResponse> execute(UUID notificationId, UUID userId);
}
