package com.pck4x.accounts_service.infrastructure.kafka;

import com.pck4x.sharedcontracts.event.AccountCreatedEvent;
import com.pck4x.sharedcontracts.event.AccountCreditedEvent;
import com.pck4x.sharedcontracts.event.AccountDebitedEvent;
import com.pck4x.sharedcontracts.event.AccountCreditFailedEvent;
import com.pck4x.sharedcontracts.event.AccountDebitCompensatedEvent;
import com.pck4x.sharedcontracts.event.AccountDepositedEvent;
import com.pck4x.sharedcontracts.event.AccountRejectedEvent;
import com.pck4x.sharedcontracts.event.TransferNotificationEvent;

/**
 * Fase 1.8: sustituye a Class.forName(eventType) en el outbox.
 * Acepta el nombre del enum, el nombre simple de la clase y el nombre completo
 * (para poder leer eventos ya persistidos con el formato anterior).
 */
public enum AccountEventType {
    ACCOUNT_CREATED(AccountCreatedEvent.class),
    ACCOUNT_DEBITED(AccountDebitedEvent.class),
    ACCOUNT_CREDITED(AccountCreditedEvent.class),
    ACCOUNT_REJECTED(AccountRejectedEvent.class),
    ACCOUNT_CREDIT_FAILED(AccountCreditFailedEvent.class),
    ACCOUNT_DEBIT_COMPENSATED(AccountDebitCompensatedEvent.class),
    ACCOUNT_DEPOSITED(AccountDepositedEvent.class),
    TRANSFER_NOTIFICATION(TransferNotificationEvent.class);

    private final Class<?> eventClass;

    AccountEventType(Class<?> eventClass) {
        this.eventClass = eventClass;
    }

    public Class<?> eventClass() {
        return eventClass;
    }

    public static String nameOf(Object event) {
        if (event == null) {
            throw new IllegalArgumentException("Event must not be null");
        }
        for (AccountEventType type : values()) {
            if (type.eventClass.isInstance(event)) {
                return type.name();
            }
        }
        throw new IllegalArgumentException("Unknown account event type: " + event.getClass().getName());
    }

    public static Class<?> resolve(String typeName) {
        if (typeName == null) {
            throw new IllegalArgumentException("Event type must not be null");
        }
        for (AccountEventType type : values()) {
            if (type.name().equals(typeName)
                    || type.eventClass.getSimpleName().equals(typeName)
                    || type.eventClass.getName().equals(typeName)) {
                return type.eventClass;
            }
        }
        throw new IllegalArgumentException("Unknown account event type: " + typeName);
    }
}
