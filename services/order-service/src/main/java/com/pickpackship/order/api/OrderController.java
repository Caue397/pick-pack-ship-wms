package com.pickpackship.order.api;

import com.pickpackship.order.api.dto.CancelOrderRequest;
import com.pickpackship.order.api.dto.CreateOrderRequest;
import com.pickpackship.order.api.dto.OrderResponse;
import com.pickpackship.order.security.AuthenticatedUser;
import com.pickpackship.order.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.apache.coyote.Response;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/order")
@RequiredArgsConstructor
public class OrderController {
    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(
            @Valid @RequestBody CreateOrderRequest request,
            @AuthenticationPrincipal Jwt jwt
            ) {
        AuthenticatedUser caller = AuthenticatedUser.from(jwt);
        OrderResponse response = orderService.createOrder(request, caller);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @PostMapping("/{orderId}")
    public ResponseEntity<Void> cancelOrder(
            @PathVariable UUID orderId,
            @RequestBody CancelOrderRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        AuthenticatedUser caller = AuthenticatedUser.from(jwt);
        orderService.cancelOrder(orderId, request, caller);
        return ResponseEntity.noContent().build();
    }
 }
