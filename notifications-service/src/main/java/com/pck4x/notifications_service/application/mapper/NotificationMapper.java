package com.pck4x.notifications_service.application.mapper;

import com.pck4x.notifications_service.application.dto.response.NotificationResponse;
import com.pck4x.notifications_service.application.dto.response.UnreadCountResponse;
import com.pck4x.notifications_service.domain.Notification;

import java.util.List;

public class NotificationMapper {

    public static final NotificationMapper INSTANCE = new NotificationMapper();

    private NotificationMapper() {}

    public NotificationResponse toResponse(Notification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getUserId(),
                notification.getType(),
                notification.getTitle(),
                notification.getMessage(),
                notification.getAmount(),
                notification.getCurrency(),
                notification.getRelatedAccount(),
                notification.isRead(),
                notification.getCreatedAt()
        );
    }

    public List<NotificationResponse> toResponseList(List<Notification> notifications) {
        return notifications.stream().map(this::toResponse).toList();
    }

    public UnreadCountResponse toUnreadCountResponse(long count) {
        return new UnreadCountResponse(count);
    }
}
