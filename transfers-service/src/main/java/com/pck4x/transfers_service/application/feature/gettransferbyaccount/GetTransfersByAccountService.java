package com.pck4x.transfers_service.application.feature.gettransferbyaccount;

import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pck4x.sharedcontracts.objects.QueryResult;
import com.pck4x.sharedcontracts.result.OutputPort;
import com.pck4x.transfers_service.application.dto.response.TransferResponse;
import com.pck4x.transfers_service.application.mapper.TransferMapper;
import com.pck4x.transfers_service.application.port.output.TransferRepository;

@Service
public class GetTransfersByAccountService implements GetTransfersByAccountUseCase {

    private final TransferRepository transferRepository;

    public GetTransfersByAccountService(TransferRepository transferRepository) {
        this.transferRepository = transferRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public OutputPort<QueryResult<List<TransferResponse>>> execute(String accountNumber, int page, int size) {
        var result = transferRepository.findByAccountNumber(PageRequest.of(page, size), accountNumber);
        var responses = result.getResults().stream()
                .map(TransferMapper.INSTANCE::toResponse)
                .toList();
        return OutputPort.ok(QueryResult.of(responses, result.getTotalCount(), result.getTotalPages(), result.getPageNumber(), result.getPageSize()));
    }
}
