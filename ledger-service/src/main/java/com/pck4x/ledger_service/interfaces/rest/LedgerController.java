package com.pck4x.ledger_service.interfaces.rest;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.pck4x.ledger_service.application.dto.response.AccountBalanceResponse;
import com.pck4x.ledger_service.application.dto.response.DailyReportResponse;
import com.pck4x.ledger_service.application.dto.response.LedgerEntryResponse;
import com.pck4x.ledger_service.application.feature.getaccountbalance.GetAccountBalanceUseCase;
import com.pck4x.ledger_service.application.feature.getallentries.GetAllEntriesUseCase;
import com.pck4x.ledger_service.application.feature.getdailyreport.GetDailyReportUseCase;
import com.pck4x.ledger_service.application.feature.getentriesbyaccount.GetEntriesByAccountUseCase;
import com.pck4x.ledger_service.application.feature.getentriesbytransfer.GetEntriesByTransferUseCase;
import com.pck4x.ledger_service.application.feature.getentrybyid.GetEntryByIdUseCase;
import com.pck4x.sharedcontracts.helper.ResponseHelper;
import com.pck4x.sharedcontracts.objects.ApiResponse;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;

@RestController
@RequestMapping("/api/ledger")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ADMIN')")
public class LedgerController {

    private final GetAllEntriesUseCase getAllEntriesUseCase;
    private final GetEntryByIdUseCase getEntryByIdUseCase;
    private final GetEntriesByAccountUseCase getEntriesByAccountUseCase;
    private final GetEntriesByTransferUseCase getEntriesByTransferUseCase;
    private final GetDailyReportUseCase getDailyReportUseCase;
    private final GetAccountBalanceUseCase getAccountBalanceUseCase;

    public LedgerController(GetAllEntriesUseCase getAllEntriesUseCase,
                            GetEntryByIdUseCase getEntryByIdUseCase,
                            GetEntriesByAccountUseCase getEntriesByAccountUseCase,
                            GetEntriesByTransferUseCase getEntriesByTransferUseCase,
                            GetDailyReportUseCase getDailyReportUseCase,
                            GetAccountBalanceUseCase getAccountBalanceUseCase) {
        this.getAllEntriesUseCase = getAllEntriesUseCase;
        this.getEntryByIdUseCase = getEntryByIdUseCase;
        this.getEntriesByAccountUseCase = getEntriesByAccountUseCase;
        this.getEntriesByTransferUseCase = getEntriesByTransferUseCase;
        this.getDailyReportUseCase = getDailyReportUseCase;
        this.getAccountBalanceUseCase = getAccountBalanceUseCase;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<LedgerEntryResponse>>> getAll() {
        var result = getAllEntriesUseCase.execute();
        return ResponseHelper.toResponse(result);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<LedgerEntryResponse>> getById(@PathVariable Long id) {
        var result = getEntryByIdUseCase.execute(id);
        return ResponseHelper.toResponse(result);
    }

    @GetMapping("/by-account")
    public ResponseEntity<ApiResponse<List<LedgerEntryResponse>>> getByAccount(
            @RequestParam String accountNumber) {
        var result = getEntriesByAccountUseCase.execute(accountNumber);
        return ResponseHelper.toResponse(result);
    }

    @GetMapping("/by-transfer")
    public ResponseEntity<ApiResponse<List<LedgerEntryResponse>>> getByTransfer(
            @RequestParam UUID transferId) {
        var result = getEntriesByTransferUseCase.execute(transferId);
        return ResponseHelper.toResponse(result);
    }

    @GetMapping("/daily-report")
    public ResponseEntity<ApiResponse<DailyReportResponse>> getDailyReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        var result = getDailyReportUseCase.execute(date);
        return ResponseHelper.toResponse(result);
    }

    @GetMapping("/balance")
    public ResponseEntity<ApiResponse<AccountBalanceResponse>> getBalance(
            @RequestParam String accountNumber) {
        var result = getAccountBalanceUseCase.execute(accountNumber);
        return ResponseHelper.toResponse(result);
    }
}
