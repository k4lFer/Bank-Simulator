package com.pck4x.transfers_service.application.feature.gettransferbyaccount;

import java.util.List;

import com.pck4x.sharedcontracts.objects.QueryResult;
import com.pck4x.sharedcontracts.result.OutputPort;
import com.pck4x.transfers_service.application.dto.response.TransferResponse;

public interface GetTransfersByAccountUseCase {
    OutputPort<QueryResult<List<TransferResponse>>> execute(String accountNumber, int page, int size);
}
