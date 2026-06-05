package com.pck4x.transfers_service.infrastructure.persistence.jpa.adapters;

import com.pck4x.transfers_service.application.port.output.TransferRepository;
import com.pck4x.transfers_service.domain.Transfer;
import com.pck4x.transfers_service.infrastructure.persistence.jpa.entities.TransferEntity;
import com.pck4x.transfers_service.infrastructure.persistence.jpa.repositories.JpaTransferRepository;
import com.pck4x.transfers_service.infrastructure.persistence.mapper.TransferMapper;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.pck4x.sharedcontracts.objects.QueryResult;

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
    public QueryResult<List<Transfer>> findByAccountNumber(Pageable pageable, String accountNumber) {
        Page<TransferEntity> page = jpaTransferRepository.findByAccountNumber(accountNumber, pageable);
        List<Transfer> transfers = page.getContent().stream()
                .map(TransferMapper.INSTANCE::toDomain)
                .toList();
        return QueryResult.of(transfers, (int) page.getTotalElements(), page.getTotalPages(), page.getNumber(), page.getSize());
    }
}
