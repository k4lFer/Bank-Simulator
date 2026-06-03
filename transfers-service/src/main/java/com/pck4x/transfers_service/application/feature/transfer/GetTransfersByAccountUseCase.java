package com.pck4x.transfers_service.application.feature.transfer;

import com.pck4x.sharedcontracts.result.OutputPort;
import com.pck4x.transfers_service.application.dto.response.TransferResponse;

import java.util.List;

public interface GetTransfersByAccountUseCase {
    OutputPort<List<TransferResponse>> execute(String accountNumber);
}
