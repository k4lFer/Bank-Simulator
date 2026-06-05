package com.pck4x.ledger_service.interfaces.rest;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.pck4x.ledger_service.application.port.output.LedgerRepository;
import com.pck4x.ledger_service.domain.LedgerEntries;
import com.pck4x.sharedcontracts.objects.ApiResponse;
import com.pck4x.sharedcontracts.objects.MessageDto;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.security.access.prepost.PreAuthorize;

@RestController
@RequestMapping("/api/ledger")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ADMIN')")
public class LedgerController {

    private final LedgerRepository ledgerRepository;

    public LedgerController(LedgerRepository ledgerRepository) {
        this.ledgerRepository = ledgerRepository;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<LedgerEntries>>> getAll() {
        var entries = ledgerRepository.findAll();
        return ResponseEntity.ok(new ApiResponse<>(true, entries, List.of()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<LedgerEntries>> getById(@PathVariable Long id) {
        return ledgerRepository.findById(id)
                .map(e -> ResponseEntity.ok(new ApiResponse<>(true, e, List.of())))
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ApiResponse<>(false, null,
                                List.of(new MessageDto("error", "Ledger entry not found: " + id)))));
    }

    @GetMapping("/by-account")
    public ResponseEntity<ApiResponse<List<LedgerEntries>>> getByAccount(
            @RequestParam String accountNumber) {
        var entries = ledgerRepository.findByAccountNumber(accountNumber);
        return ResponseEntity.ok(new ApiResponse<>(true, entries, List.of()));
    }

    @GetMapping("/by-transfer")
    public ResponseEntity<ApiResponse<List<LedgerEntries>>> getByTransfer(
            @RequestParam UUID transferId) {
        var entries = ledgerRepository.findByTransferId(transferId);
        return ResponseEntity.ok(new ApiResponse<>(true, entries, List.of()));
    }
}
