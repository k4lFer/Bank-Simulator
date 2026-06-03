package com.pck4x.transfers_service.interfaces.rest;

import com.pck4x.sharedcontracts.helper.ResponseHelper;
import com.pck4x.sharedcontracts.objects.ApiResponse;
import com.pck4x.transfers_service.application.dto.response.TransferResponse;
import com.pck4x.transfers_service.application.feature.transfer.GetTransferUseCase;
import com.pck4x.transfers_service.application.feature.transfer.GetTransfersByAccountUseCase;
import com.pck4x.transfers_service.application.feature.transfer.TransferUseCase;
import com.pck4x.transfers_service.application.dto.command.TransferCommand;
import io.swagger.v3.oas.annotations.Parameter;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/transfers")
public class TransferController {

    private final TransferUseCase transferUseCase;
    private final GetTransferUseCase getTransferUseCase;
    private final GetTransfersByAccountUseCase getTransfersByAccountUseCase;

    public TransferController(TransferUseCase transferUseCase,
                              GetTransferUseCase getTransferUseCase,
                              GetTransfersByAccountUseCase getTransfersByAccountUseCase) {
        this.transferUseCase = transferUseCase;
        this.getTransferUseCase = getTransferUseCase;
        this.getTransfersByAccountUseCase = getTransfersByAccountUseCase;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<TransferResponse>> createTransfer(
            @RequestBody TransferCommand command,
            @Parameter(hidden = true) @RequestHeader("X-User-Id") UUID userId) {
        var result = transferUseCase.execute(command, userId);
        return ResponseHelper.toResponse(result);
    }

    @GetMapping("/{transferId}")
    public ResponseEntity<ApiResponse<TransferResponse>> getTransfer(
            @PathVariable UUID transferId) {
        var result = getTransferUseCase.execute(transferId);
        return ResponseHelper.toResponse(result);
    }

    @GetMapping("/by-account/{accountNumber}")
    public ResponseEntity<ApiResponse<List<TransferResponse>>> getTransfersByAccount(
            @PathVariable String accountNumber) {
        var result = getTransfersByAccountUseCase.execute(accountNumber);
        return ResponseHelper.toResponse(result);
    }
}
