package com.pck4x.sharedcontracts.event;

import java.io.Serializable;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
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
}
