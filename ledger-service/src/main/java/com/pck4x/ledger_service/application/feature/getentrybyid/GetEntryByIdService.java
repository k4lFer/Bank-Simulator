package com.pck4x.ledger_service.application.feature.getentrybyid;

import com.pck4x.ledger_service.application.dto.response.LedgerEntryResponse;
import com.pck4x.ledger_service.application.mapper.LedgerDtoMapper;
import com.pck4x.ledger_service.application.port.output.LedgerRepository;
import com.pck4x.sharedcontracts.result.OutputPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GetEntryByIdService implements GetEntryByIdUseCase {

    private final LedgerRepository ledgerRepository;

    public GetEntryByIdService(LedgerRepository ledgerRepository) {
        this.ledgerRepository = ledgerRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public OutputPort<LedgerEntryResponse> execute(Long id) {
        return ledgerRepository.findById(id)
                .map(entry -> OutputPort.ok(LedgerDtoMapper.INSTANCE.toResponse(entry)))
                .orElse(OutputPort.notFound("Ledger entry not found: " + id));
    }
}
