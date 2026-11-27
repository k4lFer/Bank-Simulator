package com.pck4x.transfers_service.domain;

import com.pck4x.transfers_service.domain.enums.TransferStatus;

/**
 * Fase 2.1: maquina de estados explicita de la Saga.
 * El estado persistido es Transfer.status; los estados intermedios DEBITING,
 * CREDITING y COMPENSATING se derivan del paso actual del orquestador.
 */
public final class TransferSaga {

    public enum State {
        CREATED, DEBITED, COMPLETED, REJECTED, COMPENSATING, COMPENSATED, FAILED
    }

    private final Transfer transfer;

    public TransferSaga(Transfer transfer) {
        this.transfer = transfer;
    }

    public State current() {
        return fromStatus(transfer.getStatus());
    }

    public boolean isTerminal() {
        State state = current();
        return state == State.COMPLETED || state == State.REJECTED
                || state == State.COMPENSATED || state == State.FAILED;
    }

    public boolean can(State from, State to) {
        return current() == from;
    }

    public void transition(State from, State to) {
        if (!can(from, to)) {
            throw new IllegalStateException("Invalid saga transition " + from + " -> " + to
                    + " (current=" + current() + ", transferId=" + transfer.getTransferId() + ")");
        }
    }

    public static State fromStatus(TransferStatus status) {
        if (status == null) {
            return State.CREATED;
        }
        return switch (status) {
            case PENDING -> State.CREATED;
            case DEBITED -> State.DEBITED;
            case COMPLETED -> State.COMPLETED;
            case REJECTED -> State.REJECTED;
            case COMPENSATED -> State.COMPENSATED;
            case FAILED -> State.FAILED;
        };
    }
}
