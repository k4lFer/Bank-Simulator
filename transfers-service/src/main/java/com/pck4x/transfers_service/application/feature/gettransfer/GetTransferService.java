package com.pck4x.transfers_service.application.feature.gettransfer;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pck4x.sharedcontracts.result.OutputPort;
import com.pck4x.transfers_service.application.dto.response.TransferResponse;
import com.pck4x.transfers_service.application.mapper.TransferMapper;
import com.pck4x.transfers_service.application.port.output.TransferRepository;

@Service
public class GetTransferService implements GetTransferUseCase {

    private final TransferRepository transferRepository;

    public GetTransferService(TransferRepository transferRepository) {
        this.transferRepository = transferRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public OutputPort<TransferResponse> execute(UUID transferId) {
        return transferRepository.findByTransferId(transferId)
                .map(transfer -> OutputPort.ok(TransferMapper.INSTANCE.toResponse(transfer)))
                .orElseGet(() -> OutputPort.notFound("Transfer not found"));
    }
}
