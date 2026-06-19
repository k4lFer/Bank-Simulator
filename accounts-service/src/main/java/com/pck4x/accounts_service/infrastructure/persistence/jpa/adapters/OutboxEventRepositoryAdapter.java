package com.pck4x.accounts_service.infrastructure.persistence.jpa.adapters;

import com.pck4x.accounts_service.application.port.output.OutboxEventRepository;
import com.pck4x.accounts_service.domain.OutboxEvent;
import com.pck4x.accounts_service.domain.enums.OutboxStatus;
import com.pck4x.accounts_service.infrastructure.persistence.jpa.entities.OutboxEventEntity;
import com.pck4x.accounts_service.infrastructure.persistence.jpa.repositories.JpaOutboxEventRepository;
import com.pck4x.accounts_service.infrastructure.persistence.mapper.OutboxEventMapper;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
public class OutboxEventRepositoryAdapter implements OutboxEventRepository {

    private final JpaOutboxEventRepository jpaOutboxEventRepository;

    public OutboxEventRepositoryAdapter(JpaOutboxEventRepository jpaOutboxEventRepository) {
        this.jpaOutboxEventRepository = jpaOutboxEventRepository;
    }

    @Override
    @Transactional
    public OutboxEvent save(OutboxEvent event) {
        OutboxEventEntity entity = OutboxEventMapper.INSTANCE.toEntity(event);
        entity = jpaOutboxEventRepository.save(entity);
        return OutboxEventMapper.INSTANCE.toDomain(entity);
    }

    @Override
    public List<OutboxEvent> findByStatus(OutboxStatus status) {
        return jpaOutboxEventRepository.findByStatusOrderByIdAsc(status)
                .stream()
                .map(OutboxEventMapper.INSTANCE::toDomain)
                .toList();
    }
}
