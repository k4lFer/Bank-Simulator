package com.pck4x.notifications_service.application.port.output;

import com.pck4x.notifications_service.domain.Notification;
import com.pck4x.sharedcontracts.objects.QueryResult;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NotificationRepository {
    Notification save(Notification notification);
    Optional<Notification> findByIdAndUserId(UUID id, UUID userId);
    QueryResult<List<Notification>> findByUserIdOrderByCreatedAtDesc(Pageable pageable, UUID userId);
    long countUnreadByUserId(UUID userId);
}
