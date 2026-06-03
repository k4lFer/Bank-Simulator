package com.pck4x.users_service.infrastructure.kafka;

import com.pck4x.sharedcontracts.event.AccountCreatedEvent;
import com.pck4x.sharedcontracts.event.AccountCreditedEvent;
import com.pck4x.sharedcontracts.event.AccountDebitedEvent;
import com.pck4x.sharedcontracts.event.AccountDepositedEvent;
import com.pck4x.users_service.application.port.output.AccountProjectionRepository;
import com.pck4x.users_service.domain.AccountProjection;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
public class AccountEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(AccountEventConsumer.class);

    private final AccountProjectionRepository projectionRepository;

    public AccountEventConsumer(AccountProjectionRepository projectionRepository) {
        this.projectionRepository = projectionRepository;
    }

    @Transactional
    @KafkaListener(topics = "bank.account.events", groupId = "users-service")
    public void handleAccountEvent(ConsumerRecord<String, Object> record) {
        Object event = record.value();
        log.info("Received event type: {}", event.getClass().getName());

        if (event instanceof AccountCreatedEvent e) {
            log.info("AccountCreatedEvent: id={}, accountNumber={}, userId={}",
                    e.getId(), e.getAccountNumber(), e.getUserId());
            var projection = new AccountProjection(
                    e.getId(),
                    e.getAccountNumber(),
                    e.getBalance(),
                    e.getCurrency(),
                    e.getStatus(),
                    e.getUserId(),
                    e.getCreatedAt() != null
                            ? LocalDateTime.parse(e.getCreatedAt())
                            : LocalDateTime.now()
            );
            projectionRepository.save(projection);
            log.info("AccountProjection saved successfully for id={}", e.getId());
        } else if (event instanceof AccountDepositedEvent e) {
            log.info("AccountDepositedEvent: accountId={}, amount={}, balanceAfter={}",
                    e.getAccountId(), e.getAmount(), e.getBalanceAfter());
            projectionRepository.findById(e.getAccountId()).ifPresent(projection -> {
                projection.setBalance(e.getBalanceAfter());
                projectionRepository.save(projection);
                log.info("AccountProjection balance updated for id={} after deposit", e.getAccountId());
            });
        } else if (event instanceof AccountDebitedEvent e) {
            log.info("AccountDebitedEvent: accountId={}, amount={}, balanceAfter={}",
                    e.getAccountId(), e.getAmount(), e.getBalanceAfter());
            projectionRepository.findById(e.getAccountId()).ifPresent(projection -> {
                projection.setBalance(e.getBalanceAfter());
                projectionRepository.save(projection);
                log.info("AccountProjection balance updated for id={} after debit", e.getAccountId());
            });
        } else if (event instanceof AccountCreditedEvent e) {
            log.info("AccountCreditedEvent: accountId={}, amount={}, balanceAfter={}",
                    e.getAccountId(), e.getAmount(), e.getBalanceAfter());
            projectionRepository.findById(e.getAccountId()).ifPresent(projection -> {
                projection.setBalance(e.getBalanceAfter());
                projectionRepository.save(projection);
                log.info("AccountProjection balance updated for id={} after credit", e.getAccountId());
            });
        } else {
            log.warn("Unknown event type: {}", event.getClass().getName());
        }
    }
}
