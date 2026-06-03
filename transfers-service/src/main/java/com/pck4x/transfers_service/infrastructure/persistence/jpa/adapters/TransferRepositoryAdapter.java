package com.pck4x.transfers_service.infrastructure.persistence.jpa.adapters;

import com.pck4x.transfers_service.application.port.output.TransferRepository;
import com.pck4x.transfers_service.domain.Transfer;
import com.pck4x.transfers_service.infrastructure.persistence.jpa.entities.TransferEntity;
import com.pck4x.transfers_service.infrastructure.persistence.jpa.repositories.JpaTransferRepository;
import com.pck4x.transfers_service.infrastructure.persistence.mapper.TransferMapper;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class TransferRepositoryAdapter implements TransferRepository {

    private final JpaTransferRepository jpaTransferRepository;

    public TransferRepositoryAdapter(JpaTransferRepository jpaTransferRepository) {
        this.jpaTransferRepository = jpaTransferRepository;
    }

    @Override
    public Transfer save(Transfer transfer) {
        TransferEntity entity = TransferMapper.INSTANCE.toEntity(transfer);
        entity = jpaTransferRepository.save(entity);
        return TransferMapper.INSTANCE.toDomain(entity);
    }

    @Override
    public Optional<Transfer> findById(Long id) {
        return jpaTransferRepository.findById(id)
                .map(TransferMapper.INSTANCE::toDomain);
    }

    @Override
    public Optional<Transfer> findByTransferId(UUID transferId) {
        return jpaTransferRepository.findByTransferId(transferId)
                .map(TransferMapper.INSTANCE::toDomain);
    }

    @Override
    public List<Transfer> findByAccountNumber(String accountNumber) {
        return jpaTransferRepository.findByAccountNumber(accountNumber)
                .stream()
                .map(TransferMapper.INSTANCE::toDomain)
                .toList();
    }
}
