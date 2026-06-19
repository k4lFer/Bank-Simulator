package com.pck4x.accounts_service.interfaces.rest;

import com.pck4x.accounts_service.application.dto.command.ChangePinCommand;
import com.pck4x.accounts_service.application.dto.command.DepositCommand;
import com.pck4x.accounts_service.application.dto.command.IssueCardCommand;
import com.pck4x.accounts_service.application.dto.command.LinkAccountCommand;
import com.pck4x.accounts_service.application.dto.response.CardDetailResponse;
import com.pck4x.accounts_service.application.dto.response.CardResponse;
import com.pck4x.accounts_service.application.dto.response.DepositResponse;
import com.pck4x.accounts_service.application.feature.blockcard.BlockCardUseCase;
import com.pck4x.accounts_service.application.feature.changepin.ChangeCardPinUseCase;
import com.pck4x.accounts_service.application.feature.depositmoney.DepositMoneyUseCase;
import com.pck4x.accounts_service.application.feature.getcard.GetCardUseCase;
import com.pck4x.accounts_service.application.feature.getcardsbyuser.GetCardsByUserUseCase;
import com.pck4x.accounts_service.application.feature.issuecard.IssueCardUseCase;
import com.pck4x.accounts_service.application.feature.linkaccount.LinkAccountUseCase;
import com.pck4x.sharedcontracts.helper.ResponseHelper;
import com.pck4x.sharedcontracts.objects.ApiResponse;
import io.swagger.v3.oas.annotations.Parameter;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/cards")
public class CardController {

    private final IssueCardUseCase issueCardUseCase;
    private final GetCardsByUserUseCase getCardsByUserUseCase;
    private final GetCardUseCase getCardUseCase;
    private final BlockCardUseCase blockCardUseCase;
    private final ChangeCardPinUseCase changeCardPinUseCase;
    private final LinkAccountUseCase linkAccountUseCase;
    private final DepositMoneyUseCase depositMoneyUseCase;

    public CardController(IssueCardUseCase issueCardUseCase,
                          GetCardsByUserUseCase getCardsByUserUseCase,
                          GetCardUseCase getCardUseCase,
                          BlockCardUseCase blockCardUseCase,
                          ChangeCardPinUseCase changeCardPinUseCase,
                          LinkAccountUseCase linkAccountUseCase,
                          DepositMoneyUseCase depositMoneyUseCase) {
        this.issueCardUseCase = issueCardUseCase;
        this.getCardsByUserUseCase = getCardsByUserUseCase;
        this.getCardUseCase = getCardUseCase;
        this.blockCardUseCase = blockCardUseCase;
        this.changeCardPinUseCase = changeCardPinUseCase;
        this.linkAccountUseCase = linkAccountUseCase;
        this.depositMoneyUseCase = depositMoneyUseCase;
    }

    @PostMapping("/issue")
    public ResponseEntity<ApiResponse<CardResponse>> issueCard(
            @RequestBody IssueCardCommand command,
            @Parameter(hidden = true) @RequestHeader("X-User-Id") UUID userId) {
        var result = issueCardUseCase.execute(command, userId);
        return ResponseHelper.toResponse(result);
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<List<CardResponse>>> getMyCards(
            @Parameter(hidden = true) @RequestHeader("X-User-Id") UUID userId) {
        var result = getCardsByUserUseCase.execute(userId);
        return ResponseHelper.toResponse(result);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CardDetailResponse>> getCard(
            @PathVariable UUID id,
            @Parameter(hidden = true) @RequestHeader("X-User-Id") UUID userId) {
        var result = getCardUseCase.execute(id, userId);
        return ResponseHelper.toResponse(result);
    }

    @PatchMapping("/{id}/block")
    public ResponseEntity<ApiResponse<CardResponse>> blockCard(
            @PathVariable UUID id,
            @Parameter(hidden = true) @RequestHeader("X-User-Id") UUID userId) {
        var result = blockCardUseCase.execute(id, userId);
        return ResponseHelper.toResponse(result);
    }

    @PutMapping("/{id}/pin")
    public ResponseEntity<ApiResponse<CardResponse>> changePin(
            @PathVariable UUID id,
            @RequestBody ChangePinCommand command,
            @Parameter(hidden = true) @RequestHeader("X-User-Id") UUID userId) {
        var result = changeCardPinUseCase.execute(id, command, userId);
        return ResponseHelper.toResponse(result);
    }

    @PostMapping("/{id}/accounts")
    public ResponseEntity<ApiResponse<CardDetailResponse>> linkAccount(
            @PathVariable UUID id,
            @RequestBody LinkAccountCommand command,
            @Parameter(hidden = true) @RequestHeader("X-User-Id") UUID userId) {
        var result = linkAccountUseCase.execute(id, command, userId);
        return ResponseHelper.toResponse(result);
    }

    @PostMapping("/{id}/deposit")
    public ResponseEntity<ApiResponse<DepositResponse>> cardDeposit(
            @PathVariable UUID id,
            @RequestBody DepositCommand command,
            @Parameter(hidden = true) @RequestHeader("X-User-Id") UUID userId) {
        command.setCardId(id);
        var result = depositMoneyUseCase.execute(command, userId);
        return ResponseHelper.toResponse(result);
    }
}
