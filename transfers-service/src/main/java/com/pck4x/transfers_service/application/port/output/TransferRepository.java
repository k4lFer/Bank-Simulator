package com.pck4x.transfers_service.application.port.output;

import com.pck4x.transfers_service.domain.Transfer;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TransferRepository {
    Transfer save(Transfer transfer);
    Optional<Transfer> findById(Long id);
    Optional<Transfer> findByTransferId(UUID transferId);
    List<Transfer> findByAccountNumber(String accountNumber);
}
