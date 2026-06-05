package com.pck4x.transfers_service.infrastructure.persistence.jpa.repositories;

import com.pck4x.transfers_service.infrastructure.persistence.jpa.entities.TransferEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface JpaTransferRepository extends JpaRepository<TransferEntity, Long> {
    Optional<TransferEntity> findByTransferId(UUID transferId);

    @Query("SELECT t FROM TransferEntity t WHERE t.fromAccount = :accountNumber OR t.toAccount = :accountNumber ORDER BY t.createdAt DESC")
    Page<TransferEntity> findByAccountNumber(@Param("accountNumber") String accountNumber, Pageable pageable);
}
