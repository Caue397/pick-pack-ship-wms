package com.pickpackship.gateway.security;

import com.pickpackship.gateway.config.JwtProperties;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * First line of JWT validation at the edge (defense in depth — each downstream
 * service validates the signature again, independently and statelessly).
 * Rejects unauthenticated/invalid requests before they reach any service.
 */
@Component
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    private static final String ACCESS_TOKEN_COOKIE = "access_token";

    private static final List<PublicEndpoint> PUBLIC_ENDPOINTS = List.of(
            new PublicEndpoint(HttpMethod.POST, "/auth/signup"),
            new PublicEndpoint(HttpMethod.POST, "/auth/login")
    );

    private final JwtProperties jwtProperties;

    public JwtAuthenticationFilter(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();

        if (isPublicEndpoint(request)) {
            return chain.filter(exchange);
        }

        String token = extractToken(request);
        if (token == null) {
            return reject(exchange, "Missing bearer token");
        }

        try {
            Jwts.parser()
                    .verifyWith(jwtProperties.secretKey())
                    .build()
                    .parseSignedClaims(token);

            return chain.filter(exchange);
        } catch (JwtException | IllegalArgumentException ex) {
            return reject(exchange, "Invalid or expired token");
        }
    }

    @Override
    public int getOrder() {
        return -1;
    }

    private boolean isPublicEndpoint(ServerHttpRequest request) {
        String path = request.getURI().getPath();
        HttpMethod method = request.getMethod();
        return PUBLIC_ENDPOINTS.stream()
                .anyMatch(endpoint -> endpoint.method().equals(method) && endpoint.path().equals(path));
    }

    private String extractToken(ServerHttpRequest request) {
        String header = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7);
        }

        HttpCookie cookie = request.getCookies().getFirst(ACCESS_TOKEN_COOKIE);
        if (cookie != null && !cookie.getValue().isBlank()) {
            return cookie.getValue();
        }

        return null;
    }

    private Mono<Void> reject(ServerWebExchange exchange, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        byte[] body = ("{\"error\":\"" + message + "\"}").getBytes(StandardCharsets.UTF_8);
        DataBuffer buffer = response.bufferFactory().wrap(body);
        return response.writeWith(Mono.just(buffer));
    }
}
