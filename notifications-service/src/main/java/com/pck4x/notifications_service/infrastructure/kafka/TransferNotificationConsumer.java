package com.pck4x.notifications_service.infrastructure.kafka;

import com.pck4x.notifications_service.application.port.output.NotificationRepository;
import com.pck4x.notifications_service.domain.Notification;
import com.pck4x.notifications_service.domain.enums.NotificationType;
import com.pck4x.notifications_service.interfaces.rest.NotificationSseController;
import com.pck4x.sharedcontracts.event.TransferNotificationEvent;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
public class TransferNotificationConsumer {

    private static final Logger log = LoggerFactory.getLogger(TransferNotificationConsumer.class);

    private final NotificationRepository notificationRepository;
    private final ApplicationEventPublisher eventPublisher;

    public TransferNotificationConsumer(NotificationRepository notificationRepository,
                                         ApplicationEventPublisher eventPublisher) {
        this.notificationRepository = notificationRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    @KafkaListener(topics = "bank.notification.events", groupId = "notifications-service")
    public void handleNotificationEvent(ConsumerRecord<String, Object> record) {
        if (!(record.value() instanceof TransferNotificationEvent event)) {
            log.debug("Ignored event type: {}", record.value().getClass().getSimpleName());
            return;
        }

        log.info("Received notification event for user={}, type={}", event.getUserId(), event.getType());

        NotificationType type;
        try {
            type = NotificationType.valueOf(event.getType());
        } catch (IllegalArgumentException e) {
            log.warn("Unknown notification type: {}", event.getType());
            return;
        }

        var notification = new Notification(
                UUID.randomUUID(),
                event.getUserId(),
                type,
                event.getTitle(),
                event.getMessage(),
                event.getAmount(),
                event.getCurrency(),
                event.getRelatedAccount()
        );

        notification = notificationRepository.save(notification);

        eventPublisher.publishEvent(new NotificationSseController.NotificationEvent(notification));
    }
}
