package com.pck4x.notifications_service.interfaces.rest;

import com.pck4x.notifications_service.application.dto.response.NotificationResponse;
import com.pck4x.notifications_service.application.dto.response.UnreadCountResponse;
import com.pck4x.notifications_service.application.feature.listnotifications.ListNotificationsUseCase;
import com.pck4x.notifications_service.application.feature.readallnotifications.ReadAllNotificationsUseCase;
import com.pck4x.notifications_service.application.feature.readnotification.ReadNotificationUseCase;
import com.pck4x.notifications_service.application.feature.unreadcount.UnreadCountUseCase;
import com.pck4x.sharedcontracts.helper.ResponseHelper;
import com.pck4x.sharedcontracts.objects.ApiResponse;
import com.pck4x.sharedcontracts.objects.QueryResult;
import io.swagger.v3.oas.annotations.Parameter;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final ListNotificationsUseCase listNotificationsUseCase;
    private final UnreadCountUseCase unreadCountUseCase;
    private final ReadNotificationUseCase readNotificationUseCase;
    private final ReadAllNotificationsUseCase readAllNotificationsUseCase;

    public NotificationController(ListNotificationsUseCase listNotificationsUseCase,
                                  UnreadCountUseCase unreadCountUseCase,
                                  ReadNotificationUseCase readNotificationUseCase,
                                  ReadAllNotificationsUseCase readAllNotificationsUseCase) {
        this.listNotificationsUseCase = listNotificationsUseCase;
        this.unreadCountUseCase = unreadCountUseCase;
        this.readNotificationUseCase = readNotificationUseCase;
        this.readAllNotificationsUseCase = readAllNotificationsUseCase;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<QueryResult<List<NotificationResponse>>>> listNotifications(
            @Parameter(hidden = true) @RequestHeader("X-User-Id") UUID userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        var result = listNotificationsUseCase.execute(userId, page, size);
        return ResponseHelper.toResponse(result);
    }

    @GetMapping("/unread-count")
    public ResponseEntity<ApiResponse<UnreadCountResponse>> unreadCount(
            @Parameter(hidden = true) @RequestHeader("X-User-Id") UUID userId) {
        var result = unreadCountUseCase.execute(userId);
        return ResponseHelper.toResponse(result);
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<ApiResponse<NotificationResponse>> markAsRead(
            @PathVariable UUID id,
            @Parameter(hidden = true) @RequestHeader("X-User-Id") UUID userId) {
        var result = readNotificationUseCase.execute(id, userId);
        return ResponseHelper.toResponse(result);
    }

    @PatchMapping("/read-all")
    public ResponseEntity<ApiResponse<Void>> markAllAsRead(
            @Parameter(hidden = true) @RequestHeader("X-User-Id") UUID userId) {
        var result = readAllNotificationsUseCase.execute(userId);
        return ResponseHelper.toResponse(result);
    }
}
