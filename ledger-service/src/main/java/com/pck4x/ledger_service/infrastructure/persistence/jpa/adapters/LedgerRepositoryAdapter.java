package com.pck4x.ledger_service.infrastructure.persistence.jpa.adapters;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.pck4x.ledger_service.application.port.output.LedgerRepository;
import com.pck4x.ledger_service.domain.LedgerEntries;
import com.pck4x.ledger_service.infrastructure.persistence.jpa.repositories.JpaLedgerRepository;
import com.pck4x.ledger_service.infrastructure.persistence.mapper.LedgerMapper;

import lombok.AllArgsConstructor;

@Component
@AllArgsConstructor
public class LedgerRepositoryAdapter implements LedgerRepository {

    private final JpaLedgerRepository jpaRepository;

    @Override
    public LedgerEntries save(LedgerEntries ledgerEntries) {
        var entity = LedgerMapper.INSTANCE.toEntity(ledgerEntries);
        entity = jpaRepository.save(entity);
        return LedgerMapper.INSTANCE.toDomain(entity);
    }

    @Override
    public List<LedgerEntries> findByAccountNumber(String accountNumber) {
        return jpaRepository.findByAccountNumberOrderByCreatedAtDesc(accountNumber)
                .stream()
                .map(LedgerMapper.INSTANCE::toDomain)
                .toList();
    }

    @Override
    public List<LedgerEntries> findByTransferId(UUID transferId) {
        return jpaRepository.findByTransferIdOrderByCreatedAtDesc(transferId)
                .stream()
                .map(LedgerMapper.INSTANCE::toDomain)
                .toList();
    }

    @Override
    public List<LedgerEntries> findAll() {
        return jpaRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(LedgerMapper.INSTANCE::toDomain)
                .toList();
    }

    @Override
    public Optional<LedgerEntries> findById(Long id) {
        return jpaRepository.findById(id)
                .map(LedgerMapper.INSTANCE::toDomain);
    }
    
}
