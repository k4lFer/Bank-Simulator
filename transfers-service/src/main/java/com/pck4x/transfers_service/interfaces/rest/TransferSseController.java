package com.pck4x.transfers_service.interfaces.rest;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.pck4x.sharedcontracts.security.JwtTokenValidator;
import com.pck4x.transfers_service.application.event.TransferStatusEvent;
import com.pck4x.transfers_service.application.mapper.TransferMapper;

@RestController
public class TransferSseController {

    private static final Logger log = LoggerFactory.getLogger(TransferSseController.class);

    private final JwtTokenValidator jwtTokenValidator;
    private final Map<UUID, List<SseEmitter>> emittersByUser = new ConcurrentHashMap<>();

    public TransferSseController(JwtTokenValidator jwtTokenValidator) {
        this.jwtTokenValidator = jwtTokenValidator;
    }

    @GetMapping(path = "/api/transfers/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@RequestParam(required = false) String token) {
        if (token == null || token.isBlank() || !jwtTokenValidator.validate(token)) {
            var errorEmitter = new SseEmitter(0L);
            try {
                errorEmitter.send(SseEmitter.event()
                        .name("error")
                        .data("Unauthorized"));
            } catch (IOException ignored) {}
            errorEmitter.complete();
            return errorEmitter;
        }

        var userId = UUID.fromString(jwtTokenValidator.getUserId(token));
        var emitter = new SseEmitter(Long.MAX_VALUE);
        var userEmitters = emittersByUser.computeIfAbsent(userId, k -> new CopyOnWriteArrayList<>());
        userEmitters.add(emitter);

        emitter.onCompletion(() -> removeEmitter(userId, emitter));
        emitter.onTimeout(() -> removeEmitter(userId, emitter));
        emitter.onError(e -> removeEmitter(userId, emitter));

        return emitter;
    }

    private void removeEmitter(UUID userId, SseEmitter emitter) {
        var userEmitters = emittersByUser.get(userId);
        if (userEmitters != null) {
            userEmitters.remove(emitter);
            if (userEmitters.isEmpty()) {
                emittersByUser.remove(userId);
            }
        }
    }

    @EventListener
    public void onTransferStatus(TransferStatusEvent event) {
        var dto = TransferMapper.INSTANCE.toResponse(event.transfer());
        var transfer = event.transfer();
        var fromUserId = transfer.getUserId();
        if (fromUserId == null) return;

        // Always send to sender (userId)
        sendToUser(fromUserId, dto);

        // If COMPLETED, also notify the recipient (toUserId)
        if (transfer.getToUserId() != null) {
            sendToUser(transfer.getToUserId(), dto);
        }
    }

    private void sendToUser(UUID userId, Object data) {
        var userEmitters = emittersByUser.get(userId);
        if (userEmitters == null) return;

        userEmitters.forEach(emitter -> {
            try {
                emitter.send(SseEmitter.event()
                        .name("transfer-status")
                        .data(data));
            } catch (IOException e) {
                emitter.completeWithError(e);
                userEmitters.remove(emitter);
            }
        });
    }
}
