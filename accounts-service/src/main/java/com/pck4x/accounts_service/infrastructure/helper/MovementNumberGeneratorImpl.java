package com.pck4x.accounts_service.infrastructure.helper;

import com.pck4x.accounts_service.application.port.output.MovementNumberGenerator;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Component
public class MovementNumberGeneratorImpl implements MovementNumberGenerator {

    private static final SecureRandom RANDOM = new SecureRandom();

    @Override
    public String generate(String accountNumber) {
        String last4 = accountNumber.length() >= 4
                ? accountNumber.substring(accountNumber.length() - 4)
                : accountNumber;

        String datePart = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyMMddHHmm"));

        String randomPart = String.format("%02X", RANDOM.nextInt(256));

        return last4 + datePart + randomPart;
    }
}
