package com.pck4x.notifications_service.domain;

import com.pck4x.notifications_service.domain.enums.NotificationType;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
public class Notification {
    private UUID id;
    private UUID userId;
    private NotificationType type;
    private String title;
    private String message;
    private BigDecimal amount;
    private String currency;
    private String relatedAccount;
    private boolean read;
    private LocalDateTime createdAt;

    public Notification() {}

    public Notification(UUID id, UUID userId, NotificationType type, String title,
                        String message, BigDecimal amount, String currency,
                        String relatedAccount) {
        this.id = id;
        this.userId = userId;
        this.type = type;
        this.title = title;
        this.message = message;
        this.amount = amount;
        this.currency = currency;
        this.relatedAccount = relatedAccount;
        this.read = false;
        this.createdAt = LocalDateTime.now();
    }
}
