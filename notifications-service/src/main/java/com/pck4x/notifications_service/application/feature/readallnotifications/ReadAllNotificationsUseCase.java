package com.pck4x.notifications_service.application.feature.readallnotifications;

import com.pck4x.sharedcontracts.result.OutputPort;

import java.util.UUID;

public interface ReadAllNotificationsUseCase {
    OutputPort<Void> execute(UUID userId);
}
