package com.pck4x.notifications_service.application.feature.unreadcount;

import com.pck4x.notifications_service.application.dto.response.UnreadCountResponse;
import com.pck4x.notifications_service.application.mapper.NotificationMapper;
import com.pck4x.notifications_service.application.port.output.NotificationRepository;
import com.pck4x.sharedcontracts.result.OutputPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class UnreadCountService implements UnreadCountUseCase {

    private final NotificationRepository notificationRepository;

    public UnreadCountService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public OutputPort<UnreadCountResponse> execute(UUID userId) {
        var count = notificationRepository.countUnreadByUserId(userId);
        return OutputPort.ok(NotificationMapper.INSTANCE.toUnreadCountResponse(count));
    }
}
