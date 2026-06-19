package com.pck4x.ledger_service.interfaces.rest;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.pck4x.ledger_service.application.event.LedgerEntryEvent;
import com.pck4x.ledger_service.application.mapper.LedgerDtoMapper;
import com.pck4x.sharedcontracts.security.JwtTokenValidator;

@RestController
public class LedgerSseController {

    private static final Logger log = LoggerFactory.getLogger(LedgerSseController.class);

    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();
    private final JwtTokenValidator jwtTokenValidator;

    public LedgerSseController(JwtTokenValidator jwtTokenValidator) {
        this.jwtTokenValidator = jwtTokenValidator;
    }

    @GetMapping(path = "/api/ledger/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@RequestParam(required = false) String token) {
        if (token != null && !token.isBlank() && jwtTokenValidator.validate(token)) {
            String role = jwtTokenValidator.getRole(token);
            var auth = new UsernamePasswordAuthenticationToken(
                    UUID.fromString(jwtTokenValidator.getUserId(token)), null,
                    List.of(new SimpleGrantedAuthority("ROLE_" + role))
            );
            SecurityContextHolder.getContext().setAuthentication(auth);
        }

        if (SecurityContextHolder.getContext().getAuthentication() == null
                || !SecurityContextHolder.getContext().getAuthentication().getAuthorities()
                        .stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
            var emitter = new SseEmitter();
            try {
                emitter.send(SseEmitter.event().name("error").data("Unauthorized"));
                emitter.complete();
            } catch (IOException ignored) {}
            return emitter;
        }

        var emitter = new SseEmitter(Long.MAX_VALUE);
        emitters.add(emitter);

        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        emitter.onError(e -> emitters.remove(emitter));

        return emitter;
    }

    @EventListener
    public void onLedgerEntry(LedgerEntryEvent event) {
        var dto = LedgerDtoMapper.INSTANCE.toResponse(event.entry());
        emitters.forEach(emitter -> {
            try {
                emitter.send(SseEmitter.event()
                        .name("ledger-entry")
                        .data(dto));
            } catch (IOException e) {
                emitter.completeWithError(e);
                emitters.remove(emitter);
            }
        });
    }
}
