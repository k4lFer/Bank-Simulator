package com.pck4x.transfers_service.application.port.output;

import org.springframework.data.domain.Pageable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.pck4x.sharedcontracts.objects.QueryResult;
import com.pck4x.transfers_service.domain.Transfer;

public interface TransferRepository {
    Transfer save(Transfer transfer);
    Optional<Transfer> findById(Long id);
    Optional<Transfer> findByTransferId(UUID transferId);
    QueryResult<List<Transfer>> findByAccountNumber(Pageable pageable, String accountNumber);
    /** Fase 2.9: transferencias en estados no terminales mas antiguas que el corte. */
    List<Transfer> findStale(LocalDateTime cutoff);
}
