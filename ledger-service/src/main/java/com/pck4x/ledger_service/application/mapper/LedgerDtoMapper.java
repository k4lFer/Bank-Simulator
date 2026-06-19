package com.pck4x.ledger_service.application.mapper;

import com.pck4x.ledger_service.application.dto.response.AccountBalanceResponse;
import com.pck4x.ledger_service.application.dto.response.DailyReportItemResponse;
import com.pck4x.ledger_service.application.dto.response.DailyReportResponse;
import com.pck4x.ledger_service.application.dto.response.LedgerEntryResponse;
import com.pck4x.ledger_service.domain.LedgerEntries;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class LedgerDtoMapper {

    public static final LedgerDtoMapper INSTANCE = new LedgerDtoMapper();

    private LedgerDtoMapper() {}

    public LedgerEntryResponse toResponse(LedgerEntries entry) {
        return new LedgerEntryResponse(
                entry.getId(),
                entry.getTransferId(),
                entry.getAccountNumber(),
                entry.getEntryType(),
                entry.getAmount(),
                entry.getCurrency(),
                entry.getCreatedAt()
        );
    }

    public List<LedgerEntryResponse> toResponseList(List<LedgerEntries> entries) {
        return entries.stream().map(this::toResponse).toList();
    }

    public AccountBalanceResponse toBalanceResponse(String accountNumber, String currency, BigDecimal balance) {
        return new AccountBalanceResponse(
                accountNumber,
                currency,
                balance,
                LocalDateTime.now()
        );
    }

    public DailyReportResponse toDailyReport(
            java.time.LocalDate date,
            List<LedgerEntries> allEntries,
            List<LedgerEntries> beforeEntries) {

        Map<String, List<LedgerEntries>> entriesByAccount = allEntries.stream()
                .collect(Collectors.groupingBy(LedgerEntries::getAccountNumber));

        Map<String, List<LedgerEntries>> beforeByAccount = beforeEntries.stream()
                .collect(Collectors.groupingBy(LedgerEntries::getAccountNumber));

        var allAccounts = entriesByAccount.keySet();
        var beforeAccounts = beforeByAccount.keySet();

        var reportItems = java.util.stream.Stream.concat(allAccounts.stream(), beforeAccounts.stream())
                .distinct()
                .map(account -> buildReportItem(account, allEntries, beforeEntries))
                .toList();

        return new DailyReportResponse(
                date,
                reportItems.size(),
                allEntries.size(),
                reportItems
        );
    }

    private DailyReportItemResponse buildReportItem(
            String accountNumber,
            List<LedgerEntries> dayEntries,
            List<LedgerEntries> beforeEntries) {

        var dayForAccount = dayEntries.stream()
                .filter(e -> e.getAccountNumber().equals(accountNumber))
                .toList();

        String currency = dayForAccount.isEmpty()
                ? beforeEntries.stream()
                        .filter(e -> e.getAccountNumber().equals(accountNumber))
                        .findFirst()
                        .map(LedgerEntries::getCurrency)
                        .orElse("N/A")
                : dayForAccount.getFirst().getCurrency();

        BigDecimal opening = calculateBalance(
                beforeEntries.stream()
                        .filter(e -> e.getAccountNumber().equals(accountNumber))
                        .toList()
        );

        BigDecimal totalDebits = dayForAccount.stream()
                .filter(e -> "DR".equals(e.getEntryType()))
                .map(LedgerEntries::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalCredits = dayForAccount.stream()
                .filter(e -> "CR".equals(e.getEntryType()))
                .map(LedgerEntries::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal closing = opening.add(totalCredits).subtract(totalDebits);

        return new DailyReportItemResponse(
                accountNumber,
                currency,
                opening,
                totalDebits,
                totalCredits,
                closing
        );
    }

    public BigDecimal calculateBalance(List<LedgerEntries> entries) {
        BigDecimal credits = entries.stream()
                .filter(e -> "CR".equals(e.getEntryType()))
                .map(LedgerEntries::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal debits = entries.stream()
                .filter(e -> "DR".equals(e.getEntryType()))
                .map(LedgerEntries::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return credits.subtract(debits);
    }
}
