package com.pck4x.notifications_service.application.feature.listnotifications;

import com.pck4x.notifications_service.application.dto.response.NotificationResponse;
import com.pck4x.sharedcontracts.objects.QueryResult;
import com.pck4x.sharedcontracts.result.OutputPort;

import java.util.List;
import java.util.UUID;

public interface ListNotificationsUseCase {
    OutputPort<QueryResult<List<NotificationResponse>>> execute(UUID userId, int page, int size);
}
