package com.pck4x.notifications_service.infrastructure.persistence.jpa.adapters;

import com.pck4x.notifications_service.application.port.output.NotificationRepository;
import com.pck4x.notifications_service.domain.Notification;
import com.pck4x.notifications_service.infrastructure.persistence.jpa.mapper.NotificationEntityMapper;
import com.pck4x.notifications_service.infrastructure.persistence.jpa.repositories.NotificationJpaRepository;
import com.pck4x.sharedcontracts.objects.QueryResult;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class NotificationJpaRepositoryAdapter implements NotificationRepository {

    private final NotificationJpaRepository jpaRepository;

    public NotificationJpaRepositoryAdapter(NotificationJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Notification save(Notification notification) {
        var entity = NotificationEntityMapper.INSTANCE.toEntity(notification);
        entity = jpaRepository.save(entity);
        return NotificationEntityMapper.INSTANCE.toDomain(entity);
    }

    @Override
    public Optional<Notification> findByIdAndUserId(UUID id, UUID userId) {
        return jpaRepository.findById(id)
                .filter(e -> e.getUserId().equals(userId))
                .map(NotificationEntityMapper.INSTANCE::toDomain);
    }

    @Override
    public QueryResult<List<Notification>> findByUserIdOrderByCreatedAtDesc(Pageable pageable, UUID userId) {
        var page = jpaRepository.findByUserIdOrderByCreatedAtDesc(pageable, userId);
        var notifications = page.getContent().stream()
                .map(NotificationEntityMapper.INSTANCE::toDomain)
                .toList();
        return QueryResult.of(notifications, (int) page.getTotalElements(), page.getTotalPages(), page.getNumber(), page.getSize());
    }

    @Override
    public long countUnreadByUserId(UUID userId) {
        return jpaRepository.countByUserIdAndReadFalse(userId);
    }
}
