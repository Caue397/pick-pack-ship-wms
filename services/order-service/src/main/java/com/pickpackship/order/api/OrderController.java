package com.pickpackship.order.api;

import com.pickpackship.order.api.dto.*;
import com.pickpackship.order.domain.Order;
import com.pickpackship.order.domain.OrderStatus;
import com.pickpackship.order.security.AuthenticatedUser;
import com.pickpackship.order.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.apache.coyote.Response;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/order")
@RequiredArgsConstructor
public class OrderController {
    private final OrderService orderService;

    @GetMapping
    public ResponseEntity<Page<SummaryOrderResponse>> listOrders(
            @AuthenticationPrincipal Jwt jwt,
            Pageable pageable,
            @ModelAttribute OrderFilter filter
            ) {
        AuthenticatedUser caller = AuthenticatedUser.from(jwt);
        Page<SummaryOrderResponse> orders = orderService.listOrders(pageable, filter, caller);
        return ResponseEntity.ok(orders);
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<OrderResponse> getOrder(
            @PathVariable UUID orderId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        AuthenticatedUser caller = AuthenticatedUser.from(jwt);
        OrderResponse response = orderService.getOrder(orderId, caller);
        return ResponseEntity.ok(response);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
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

    @PostMapping("/cancel/{orderId}")
    @PreAuthorize("hasRole('ADMIN')")
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
