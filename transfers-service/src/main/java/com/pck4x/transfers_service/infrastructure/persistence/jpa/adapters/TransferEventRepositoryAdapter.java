package com.pck4x.transfers_service.infrastructure.persistence.jpa.adapters;

import com.pck4x.transfers_service.application.port.output.TransferEventRepository;
import com.pck4x.transfers_service.domain.TransferEvent;
import com.pck4x.transfers_service.domain.enums.OutboxStatus;
import com.pck4x.transfers_service.infrastructure.persistence.jpa.repositories.JpaTransferEventRepository;
import com.pck4x.transfers_service.infrastructure.persistence.jpa.repositories.JpaTransferRepository;
import com.pck4x.transfers_service.infrastructure.persistence.mapper.TransferEventMapper;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
public class TransferEventRepositoryAdapter implements TransferEventRepository {

    private final JpaTransferEventRepository jpaTransferEventRepository;
    private final JpaTransferRepository jpaTransferRepository;

    public TransferEventRepositoryAdapter(JpaTransferEventRepository jpaTransferEventRepository,
                                          JpaTransferRepository jpaTransferRepository) {
        this.jpaTransferEventRepository = jpaTransferEventRepository;
        this.jpaTransferRepository = jpaTransferRepository;
    }

    @Override
    @Transactional
    public TransferEvent save(TransferEvent event) {
        var entity = TransferEventMapper.INSTANCE.toEntity(event);
        var transferEntity = jpaTransferRepository.findByTransferId(event.getTransferId())
                .orElseThrow(() -> new IllegalArgumentException("Transfer not found: " + event.getTransferId()));
        entity.setTransfer(transferEntity);
        entity = jpaTransferEventRepository.save(entity);
        return TransferEventMapper.INSTANCE.toDomain(entity);
    }

    @Override
    public List<TransferEvent> findByStatus(OutboxStatus status) {
        return jpaTransferEventRepository.findByStatus(status)
                .stream()
                .map(TransferEventMapper.INSTANCE::toDomain)
                .toList();
    }
}
