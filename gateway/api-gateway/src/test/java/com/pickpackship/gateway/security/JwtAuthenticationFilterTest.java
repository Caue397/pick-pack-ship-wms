package com.pickpackship.gateway.security;

import com.pickpackship.gateway.config.JwtProperties;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class JwtAuthenticationFilterTest {

    private static final String SECRET = "test-secret-key-with-at-least-32-bytes!!";

    private JwtAuthenticationFilter filter;
    private SecretKey secretKey;

    @BeforeEach
    void setUp() {
        JwtProperties properties = new JwtProperties(SECRET);
        secretKey = properties.secretKey();
        filter = new JwtAuthenticationFilter(properties);
    }

    @Test
    void allowsPublicSignupWithoutToken() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.post("/auth/signup"));

        AtomicReference<ServerWebExchange> forwarded = new AtomicReference<>();
        filter.filter(exchange, ex -> {
            forwarded.set(ex);
            return Mono.empty();
        }).block();

        assertNotNull(forwarded.get());
    }

    @Test
    void rejectsProtectedRouteWithoutToken() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/order/123"));

        filter.filter(exchange, ex -> Mono.empty()).block();

        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
    }

    @Test
    void rejectsExpiredToken() {
        String expired = Jwts.builder()
                .subject("user-1")
                .claim("operatorId", "op-1")
                .claim("workspaceId", "ws-1")
                .claim("role", "ADMIN")
                .expiration(new Date(System.currentTimeMillis() - 1000))
                .signWith(secretKey)
                .compact();

        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/order/123")
                        .header("Authorization", "Bearer " + expired));

        filter.filter(exchange, ex -> Mono.empty()).block();

        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
    }

    @Test
    void rejectsTokenSignedWithDifferentKey() {
        SecretKey otherKey = new JwtProperties("a-completely-different-secret-key-32-bytes").secretKey();
        String token = Jwts.builder()
                .subject("user-1")
                .expiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(otherKey)
                .compact();

        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/order/123")
                        .header("Authorization", "Bearer " + token));

        filter.filter(exchange, ex -> Mono.empty()).block();

        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
    }

    @Test
    void forwardsRequestForValidToken() {
        String token = Jwts.builder()
                .subject("user-1")
                .claim("operatorId", "op-1")
                .claim("workspaceId", "ws-1")
                .claim("role", "ADMIN")
                .expiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(secretKey)
                .compact();

        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/order/123")
                        .header("Authorization", "Bearer " + token));

        AtomicReference<ServerHttpRequest> forwardedRequest = new AtomicReference<>();
        filter.filter(exchange, ex -> {
            forwardedRequest.set(ex.getRequest());
            return Mono.empty();
        }).block();

        assertNotNull(forwardedRequest.get());
    }

    @Test
    void forwardsRequestForValidTokenInCookie() {
        String token = Jwts.builder()
                .subject("user-1")
                .claim("operatorId", "op-1")
                .claim("workspaceId", "ws-1")
                .claim("role", "ADMIN")
                .expiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(secretKey)
                .compact();

        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/order/123")
                        .cookie(new org.springframework.http.HttpCookie("access_token", token)));

        AtomicReference<ServerHttpRequest> forwardedRequest = new AtomicReference<>();
        filter.filter(exchange, ex -> {
            forwardedRequest.set(ex.getRequest());
            return Mono.empty();
        }).block();

        assertNotNull(forwardedRequest.get());
    }

    @Test
    void rejectsMissingAuthorizationHeaderFormat() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/order/123")
                        .header("Authorization", "not-a-bearer-token"));

        filter.filter(exchange, ex -> Mono.empty()).block();

        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
    }
}
