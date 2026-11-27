package com.pck4x.transfers_service.application.feature.internaltransfer;

import com.pck4x.sharedcontracts.command.TransferDebitCommand;
import com.pck4x.sharedcontracts.event.TransferRequestedEvent;
import com.pck4x.sharedcontracts.money.Money;
import com.pck4x.sharedcontracts.result.OutputPort;
import com.pck4x.transfers_service.application.dto.command.TransferCommand;
import com.pck4x.transfers_service.application.dto.response.TransferResponse;
import com.pck4x.transfers_service.application.port.output.TransferRepository;
import com.pck4x.transfers_service.domain.Transfer;
import com.pck4x.transfers_service.infrastructure.kafka.TransferCommandPublisher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class InternalTransferService implements InternalTransferUseCase {

    private final TransferRepository transferRepository;
    private final TransferCommandPublisher commandPublisher;
    private final boolean orchestrated;

    public InternalTransferService(TransferRepository transferRepository,
                                   TransferCommandPublisher commandPublisher,
                                   @Value("${app.saga.orchestrated:true}") boolean orchestrated) {
        this.transferRepository = transferRepository;
        this.commandPublisher = commandPublisher;
        this.orchestrated = orchestrated;
    }

    @Override
    @Transactional
    public OutputPort<TransferResponse> execute(TransferCommand input, UUID idempotencyKey, UUID userId) {
        var existing = transferRepository.findByTransferId(idempotencyKey);
        if (existing.isPresent()) {
            var t = existing.get();
            return OutputPort.ok(toResponse(t), "Internal transfer (idempotent replay)");
        }

        // Fase 0.6 / 1.6: validar antes de persistir con el value object Money.
        if (input == null
                || input.getFromAccount() == null || input.getFromAccount().isBlank()
                || input.getToAccount() == null || input.getToAccount().isBlank()) {
            return OutputPort.badRequest("Account fields are required");
        }
        Money money;
        try {
            money = Money.of(input.getAmount(), input.getCurrency());
        } catch (IllegalArgumentException e) {
            return OutputPort.badRequest(e.getMessage());
        }

        var transferId = idempotencyKey;

        var transfer = new Transfer(
                transferId,
                userId,
                input.getFromAccount(),
                input.getToAccount(),
                money.amount(),
                money.currency(),
                input.getDescription()
        );
        transfer = transferRepository.save(transfer);

        // Fase 2.10: la flag permite volver al flujo de coreografia (dual-run).
        if (orchestrated) {
            var command = new TransferDebitCommand(transferId, input.getFromAccount(), input.getToAccount(),
                    money.amount(), money.currency(), input.getDescription());
            command.setUserId(userId);
            commandPublisher.enqueue(transferId, command);
        } else {
            var event = new TransferRequestedEvent(transferId, input.getFromAccount(), input.getToAccount(),
                    money.amount(), money.currency(), input.getDescription());
            event.setTransferType("INTERNAL");
            event.setUserId(userId);
            commandPublisher.enqueue(transferId, event);
        }

        return OutputPort.created(toResponse(transfer), "Internal transfer initiated");
    }

    private TransferResponse toResponse(Transfer t) {
        return new TransferResponse(
                t.getTransferId(),
                t.getUserId(),
                t.getToUserId(),
                t.getFromAccount(),
                t.getToAccount(),
                t.getAmount(),
                t.getCurrency(),
                t.getDescription(),
                t.getStatus(),
                t.getRejectionReason(),
                t.getCreatedAt()
        );
    }
}
