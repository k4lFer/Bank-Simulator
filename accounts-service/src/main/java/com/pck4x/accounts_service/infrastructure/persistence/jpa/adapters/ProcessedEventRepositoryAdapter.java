package com.pck4x.accounts_service.infrastructure.persistence.jpa.adapters;

import com.pck4x.accounts_service.application.port.output.ProcessedEventRepository;
import com.pck4x.accounts_service.infrastructure.persistence.jpa.entities.ProcessedEventEntity;
import com.pck4x.accounts_service.infrastructure.persistence.jpa.repositories.JpaProcessedEventRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
public class ProcessedEventRepositoryAdapter implements ProcessedEventRepository {

    private final JpaProcessedEventRepository repository;

    public ProcessedEventRepositoryAdapter(JpaProcessedEventRepository repository) {
        this.repository = repository;
    }

    @Override
    public boolean isProcessed(String eventId, String consumer) {
        return repository.existsByEventIdAndConsumer(eventId, consumer);
    }

    @Override
    @Transactional
    public void markProcessed(String eventId, String consumer) {
        if (isProcessed(eventId, consumer)) {
            return;
        }
        var entity = new ProcessedEventEntity();
        entity.setEventId(eventId);
        entity.setConsumer(consumer);
        entity.setProcessedAt(LocalDateTime.now());
        repository.save(entity);
    }
}
