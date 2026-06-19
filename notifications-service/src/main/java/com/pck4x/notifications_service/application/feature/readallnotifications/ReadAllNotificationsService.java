package com.pck4x.notifications_service.application.feature.readallnotifications;

import com.pck4x.notifications_service.application.port.output.NotificationRepository;
import com.pck4x.sharedcontracts.result.OutputPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class ReadAllNotificationsService implements ReadAllNotificationsUseCase {

    private final NotificationRepository notificationRepository;

    public ReadAllNotificationsService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @Override
    @Transactional
    public OutputPort<Void> execute(UUID userId) {
        var result = notificationRepository.findByUserIdOrderByCreatedAtDesc(
                org.springframework.data.domain.PageRequest.of(0, Integer.MAX_VALUE), userId);
        var notifications = result.getResults();
        for (var n : notifications) {
            n.setRead(true);
            notificationRepository.save(n);
        }
        return OutputPort.noContent();
    }
}
