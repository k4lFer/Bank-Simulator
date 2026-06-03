package com.pck4x.accounts_service.application.feature.updatepin;

import com.pck4x.accounts_service.application.dto.command.UpdatePinCommand;
import com.pck4x.accounts_service.application.dto.response.PinChangeResponse;
import com.pck4x.accounts_service.application.mapper.AccountDtoMapper;
import com.pck4x.accounts_service.application.port.output.AccountRepository;
import com.pck4x.sharedcontracts.result.OutputPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class UpdatePinService implements UpdatePinUseCase {

    private final AccountRepository accountRepository;

    public UpdatePinService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    @Override
    @Transactional
    public OutputPort<PinChangeResponse> execute(UUID accountId, UpdatePinCommand command, UUID userId) {
        var account = userId != null
                ? accountRepository.findByIdAndUserId(accountId, userId)
                : accountRepository.findById(accountId);

        if (account.isEmpty()) {
            return OutputPort.notFound("Account not found");
        }

        var acc = account.get();

        if (userId != null) {
            if (!acc.getPin6().equals(command.getCurrentPin6())) {
                return OutputPort.badRequest("Current PIN6 is incorrect");
            }
            if (!acc.getPin4().equals(command.getCurrentPin4())) {
                return OutputPort.badRequest("Current PIN4 is incorrect");
            }
        }

        if (command.getNewPin6() != null) {
            acc.setPin6(command.getNewPin6());
        }
        if (command.getNewPin4() != null) {
            acc.setPin4(command.getNewPin4());
        }
        acc.setUpdatedAt(LocalDateTime.now());
        accountRepository.save(acc);

        return OutputPort.ok(
                AccountDtoMapper.INSTANCE.toPinChangeResponse(accountId),
                "PIN changed successfully"
        );
    }
}
