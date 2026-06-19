package com.pck4x.notifications_service.application.feature.readnotification;

import com.pck4x.notifications_service.application.dto.response.NotificationResponse;
import com.pck4x.notifications_service.application.mapper.NotificationMapper;
import com.pck4x.notifications_service.application.port.output.NotificationRepository;
import com.pck4x.sharedcontracts.result.OutputPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class ReadNotificationService implements ReadNotificationUseCase {

    private final NotificationRepository notificationRepository;

    public ReadNotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @Override
    @Transactional
    public OutputPort<NotificationResponse> execute(UUID notificationId, UUID userId) {
        var opt = notificationRepository.findByIdAndUserId(notificationId, userId);
        if (opt.isEmpty()) {
            return OutputPort.notFound("Notification not found");
        }

        var notification = opt.get();
        notification.setRead(true);
        notification = notificationRepository.save(notification);

        return OutputPort.ok(NotificationMapper.INSTANCE.toResponse(notification), "Marked as read");
    }
}
