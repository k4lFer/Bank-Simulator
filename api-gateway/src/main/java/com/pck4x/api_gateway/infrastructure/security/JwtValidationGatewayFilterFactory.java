package com.pck4x.api_gateway.infrastructure.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Component
public class JwtValidationGatewayFilterFactory
        extends AbstractGatewayFilterFactory<JwtValidationGatewayFilterFactory.Config> {

    private final SecretKey key;

    public JwtValidationGatewayFilterFactory(@Value("${app.jwt.secret}") String secret) {
        super(Config.class);
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            // Fase 1.5: correlation ID disponible incluso en respuestas 401.
            String correlationId = exchange.getRequest().getHeaders().getFirst("X-Correlation-Id");
            if (correlationId == null || correlationId.isBlank()) {
                correlationId = UUID.randomUUID().toString();
            }
            exchange.getResponse().getHeaders().set("X-Correlation-Id", correlationId);

            String authHeader = exchange.getRequest().getHeaders()
                    .getFirst("Authorization");

            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return unauthorized(exchange, "Missing or invalid Authorization header");
            }

            String token = authHeader.substring(7);

            try {
                Claims claims = Jwts.parser()
                        .verifyWith(key)
                        .build()
                        .parseSignedClaims(token)
                        .getPayload();

                var mutatedRequest = exchange.getRequest().mutate()
                        .header("X-User-Id", claims.getSubject())
                        .header("X-User-Role", claims.get("role", String.class))
                        .header("X-Correlation-Id", correlationId)
                        .build();

                return chain.filter(exchange.mutate().request(mutatedRequest).build());
            } catch (Exception e) {
                return unauthorized(exchange, "Invalid or expired token");
            }
        };
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange, String message) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        return exchange.getResponse().setComplete();
    }

    public static class Config {}
}
