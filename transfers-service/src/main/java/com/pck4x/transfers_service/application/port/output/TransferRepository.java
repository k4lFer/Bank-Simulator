package com.pck4x.transfers_service.application.port.output;

import org.springframework.data.domain.Pageable;
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
}
