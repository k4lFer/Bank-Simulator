package com.pck4x.notifications_service.infrastructure.persistence.jpa.mapper;

import com.pck4x.notifications_service.domain.Notification;
import com.pck4x.notifications_service.infrastructure.persistence.jpa.entities.NotificationEntity;

public class NotificationEntityMapper {

    public static final NotificationEntityMapper INSTANCE = new NotificationEntityMapper();

    private NotificationEntityMapper() {}

    public NotificationEntity toEntity(Notification notification) {
        var entity = new NotificationEntity();
        entity.setId(notification.getId());
        entity.setUserId(notification.getUserId());
        entity.setType(notification.getType());
        entity.setTitle(notification.getTitle());
        entity.setMessage(notification.getMessage());
        entity.setAmount(notification.getAmount());
        entity.setCurrency(notification.getCurrency());
        entity.setRelatedAccount(notification.getRelatedAccount());
        entity.setRead(notification.isRead());
        entity.setCreatedAt(notification.getCreatedAt());
        return entity;
    }

    public Notification toDomain(NotificationEntity entity) {
        var notification = new Notification();
        notification.setId(entity.getId());
        notification.setUserId(entity.getUserId());
        notification.setType(entity.getType());
        notification.setTitle(entity.getTitle());
        notification.setMessage(entity.getMessage());
        notification.setAmount(entity.getAmount());
        notification.setCurrency(entity.getCurrency());
        notification.setRelatedAccount(entity.getRelatedAccount());
        notification.setRead(entity.isRead());
        notification.setCreatedAt(entity.getCreatedAt());
        return notification;
    }
}
