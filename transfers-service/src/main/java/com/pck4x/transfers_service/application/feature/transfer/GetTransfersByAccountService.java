package com.pck4x.transfers_service.application.feature.transfer;

import com.pck4x.sharedcontracts.result.OutputPort;
import com.pck4x.transfers_service.application.dto.response.TransferResponse;
import com.pck4x.transfers_service.application.mapper.TransferMapper;
import com.pck4x.transfers_service.application.port.output.TransferRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class GetTransfersByAccountService implements GetTransfersByAccountUseCase {

    private final TransferRepository transferRepository;

    public GetTransfersByAccountService(TransferRepository transferRepository) {
        this.transferRepository = transferRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public OutputPort<List<TransferResponse>> execute(String accountNumber) {
        var transfers = transferRepository.findByAccountNumber(accountNumber);
        var response = transfers.stream()
                .map(TransferMapper.INSTANCE::toResponse)
                .toList();
        return OutputPort.ok(response);
    }
}
