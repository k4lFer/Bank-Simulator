package com.pck4x.sharedcontracts.event;

import java.io.Serializable;
import java.util.UUID;

public class AccountRejectedEvent implements Serializable {
    private UUID transferId;
    private String accountNumber;
    private String reason;

    public AccountRejectedEvent() {}

    public AccountRejectedEvent(UUID transferId, String accountNumber, String reason) {
        this.transferId = transferId;
        this.accountNumber = accountNumber;
        this.reason = reason;
    }

    public UUID getTransferId() {
        return transferId;
    }

    public void setTransferId(UUID transferId) {
        this.transferId = transferId;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
