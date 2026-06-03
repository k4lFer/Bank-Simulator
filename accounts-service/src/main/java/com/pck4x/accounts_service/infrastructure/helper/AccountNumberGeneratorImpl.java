package com.pck4x.accounts_service.infrastructure.helper;

import com.pck4x.accounts_service.application.port.output.AccountNumberGenerator;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class AccountNumberGeneratorImpl implements AccountNumberGenerator {

    private static final String PREFIX = "ES99";
    private static final int DIGITS = 12;
    private final SecureRandom random = new SecureRandom();

    @Override
    public String generate() {
        StringBuilder sb = new StringBuilder(PREFIX);
        for (int i = 0; i < DIGITS; i++) {
            sb.append(random.nextInt(10));
        }
        return sb.toString();
    }
}
