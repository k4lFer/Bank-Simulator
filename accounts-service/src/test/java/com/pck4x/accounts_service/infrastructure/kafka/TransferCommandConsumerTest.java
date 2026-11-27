package com.pck4x.accounts_service.infrastructure.kafka;

import com.pck4x.accounts_service.application.port.output.AccountMovementRepository;
import com.pck4x.accounts_service.application.port.output.AccountRepository;
import com.pck4x.accounts_service.application.port.output.CardAccountRepository;
import com.pck4x.accounts_service.application.port.output.CardRepository;
import com.pck4x.accounts_service.application.port.output.EventPublisher;
import com.pck4x.accounts_service.application.port.output.MovementNumberGenerator;
import com.pck4x.accounts_service.application.port.output.ProcessedEventRepository;
import com.pck4x.accounts_service.domain.Account;
import com.pck4x.accounts_service.domain.AccountMovement;
import com.pck4x.accounts_service.domain.enums.MovementType;
import com.pck4x.sharedcontracts.command.TransferCompensateCommand;
import com.pck4x.sharedcontracts.command.TransferCreditCommand;
import com.pck4x.sharedcontracts.command.TransferDebitCommand;
import com.pck4x.sharedcontracts.enums.AccountStatus;
import com.pck4x.sharedcontracts.event.AccountCreditFailedEvent;
import com.pck4x.sharedcontracts.event.AccountCreditedEvent;
import com.pck4x.sharedcontracts.event.AccountDebitCompensatedEvent;
import com.pck4x.sharedcontracts.event.AccountDebitedEvent;
import com.pck4x.sharedcontracts.event.AccountRejectedEvent;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Fase 2.4 / 2.5 / 2.6: comandos de debito, credito y compensacion. */
class TransferCommandConsumerTest {

    private AccountRepository accounts;
    private AccountMovementRepository movements;
    private EventPublisher events;
    private ProcessedEventRepository inbox;
    private TransferCommandConsumer consumer;

    private final UUID userId = UUID.randomUUID();
    private final UUID transferId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        accounts = mock(AccountRepository.class);
        movements = mock(AccountMovementRepository.class);
        events = mock(EventPublisher.class);
        inbox = mock(ProcessedEventRepository.class);
        var cards = mock(CardRepository.class);
        var cardAccounts = mock(CardAccountRepository.class);
        var numbers = mock(MovementNumberGenerator.class);
        when(numbers.generate(anyString())).thenReturn("1234567890");
        when(movements.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(accounts.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        consumer = new TransferCommandConsumer(accounts, movements, numbers, events,
                cards, cardAccounts, inbox);
    }

    private Account account(String number, String balance, UUID owner) {
        var account = new Account(UUID.randomUUID(), number, "USD", owner);
        account.setBalance(new BigDecimal(balance));
        account.setStatus(AccountStatus.ACTIVE);
        return account;
    }

    private void deliver(Object command) {
        consumer.handleCommand(new ConsumerRecord<>("bank.transfer.commands", 0, 0L, "key", command));
    }

    @Test
    void debitoInsuficienteRechazaAntesDeTocarSaldo() {
        var source = account("111", "10.00", userId);
        when(accounts.findByAccountNumberForUpdate("111")).thenReturn(Optional.of(source));
        when(accounts.findByAccountNumber("222")).thenReturn(Optional.of(account("222", "0.00", userId)));
        var command = debit("100.00");

        deliver(command);

        assertEquals(new BigDecimal("10.00"), source.getBalance());
        var rejected = ArgumentCaptor.forClass(AccountRejectedEvent.class);
        verify(events).publish(eq("bank.account.events"), rejected.capture());
        assertEquals("Insufficient funds", rejected.getValue().getReason());
        verify(movements, never()).save(any());
    }

    @Test
    void debitoValidoPublicaAccountDebitedYMovimientoDebit() {
        var source = account("111", "500.00", userId);
        when(accounts.findByAccountNumberForUpdate("111")).thenReturn(Optional.of(source));
        when(accounts.findByAccountNumber("222")).thenReturn(Optional.of(account("222", "0.00", userId)));

        deliver(debit("200.00"));

        assertEquals(new BigDecimal("300.00"), source.getBalance());
        var movement = ArgumentCaptor.forClass(AccountMovement.class);
        verify(movements).save(movement.capture());
        assertEquals(MovementType.DEBIT, movement.getValue().getType());
        var event = ArgumentCaptor.forClass(AccountDebitedEvent.class);
        verify(events).publish(eq("bank.account.events"), event.capture());
        assertEquals(new BigDecimal("300.00"), event.getValue().getBalanceAfter());
    }

    @Test
    void creditoFallidoPublicaCreditFailedSinModificarSaldo() {
        when(accounts.findByAccountNumberForUpdate("222")).thenReturn(Optional.empty());

        deliver(credit("200.00"));

        var failed = ArgumentCaptor.forClass(AccountCreditFailedEvent.class);
        verify(events).publish(eq("bank.account.events"), failed.capture());
        assertEquals("Destination account not found", failed.getValue().getReason());
        verify(movements, never()).save(any());
    }

    @Test
    void creditoValidoPublicaAccountCreditedYMovimientoCredit() {
        var destination = account("222", "100.00", userId);
        when(accounts.findByAccountNumberForUpdate("222")).thenReturn(Optional.of(destination));

        deliver(credit("200.00"));

        assertEquals(new BigDecimal("300.00"), destination.getBalance());
        var event = ArgumentCaptor.forClass(AccountCreditedEvent.class);
        verify(events).publish(eq("bank.account.events"), event.capture());
        assertEquals(userId, event.getValue().getToUserId());
    }

    @Test
    void compensacionCreaMovimientoReversalConReferenciaAlDebito() {
        var source = account("111", "300.00", userId);
        when(accounts.findByAccountNumberForUpdate("111")).thenReturn(Optional.of(source));
        var original = new AccountMovement(source.getId(), "999", MovementType.DEBIT,
                new BigDecimal("200.00"), new BigDecimal("300.00"));
        when(movements.findByTransferIdAndType(transferId, MovementType.DEBIT))
                .thenReturn(Optional.of(original));

        var command = new TransferCompensateCommand(transferId, "111",
                new BigDecimal("200.00"), "USD", "Destination account not found");
        deliver(command);

        assertEquals(new BigDecimal("500.00"), source.getBalance());
        var movement = ArgumentCaptor.forClass(AccountMovement.class);
        verify(movements).save(movement.capture());
        assertEquals(MovementType.REVERSAL, movement.getValue().getType());
        assertEquals("999", movement.getValue().getReversalOf());
        var event = ArgumentCaptor.forClass(AccountDebitCompensatedEvent.class);
        verify(events).publish(eq("bank.account.events"), event.capture());
        assertEquals("999", event.getValue().getReversalOf());
    }

    @Test
    void comandoDuplicadoEsIgnoradoPorInbox() {
        var command = debit("200.00");
        when(inbox.isProcessed(command.getCommandId().toString(), "accounts-commands"))
                .thenReturn(false, true);
        var source = account("111", "500.00", userId);
        when(accounts.findByAccountNumberForUpdate("111")).thenReturn(Optional.of(source));
        when(accounts.findByAccountNumber("222")).thenReturn(Optional.of(account("222", "0.00", userId)));

        deliver(command);
        deliver(command);

        verify(inbox, times(1)).markProcessed(command.getCommandId().toString(), "accounts-commands");
        verify(movements, times(1)).save(any());
    }

    private TransferDebitCommand debit(String amount) {
        var command = new TransferDebitCommand(transferId, "111", "222",
                new BigDecimal(amount), "USD", "test");
        command.setUserId(userId);
        return command;
    }

    private TransferCreditCommand credit(String amount) {
        return new TransferCreditCommand(transferId, "111", "222",
                new BigDecimal(amount), "USD", "test");
    }
}
