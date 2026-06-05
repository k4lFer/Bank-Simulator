package com.pck4x.transfers_service.interfaces.rest;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.pck4x.sharedcontracts.helper.ResponseHelper;
import com.pck4x.sharedcontracts.objects.ApiResponse;
import com.pck4x.sharedcontracts.objects.QueryResult;
import com.pck4x.transfers_service.application.dto.command.TransferCommand;
import com.pck4x.transfers_service.application.dto.response.TransferResponse;
import com.pck4x.transfers_service.application.feature.gettransfer.GetTransferUseCase;
import com.pck4x.transfers_service.application.feature.gettransferbyaccount.GetTransfersByAccountUseCase;
import com.pck4x.transfers_service.application.feature.transfer.TransferUseCase;

import io.swagger.v3.oas.annotations.Parameter;

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
    public ResponseEntity<ApiResponse<QueryResult<List<TransferResponse>>>> getTransfersByAccount(
            @PathVariable String accountNumber,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        var result = getTransfersByAccountUseCase.execute(accountNumber, page, size);
        return ResponseHelper.toResponse(result);
    }
}
