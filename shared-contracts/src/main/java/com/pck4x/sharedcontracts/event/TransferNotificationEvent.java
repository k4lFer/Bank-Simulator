package com.pck4x.sharedcontracts.event;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
public class TransferNotificationEvent implements Serializable {
    private UUID userId;
    private UUID relatedUserId;
    private String type;
    private String title;
    private String message;
    private BigDecimal amount;
    private String currency;
    private String relatedAccount;

    public TransferNotificationEvent() {}

    public TransferNotificationEvent(UUID userId, UUID relatedUserId, String type,
                                      String title, String message, BigDecimal amount,
                                      String currency, String relatedAccount) {
        this.userId = userId;
        this.relatedUserId = relatedUserId;
        this.type = type;
        this.title = title;
        this.message = message;
        this.amount = amount;
        this.currency = currency;
        this.relatedAccount = relatedAccount;
    }
}
