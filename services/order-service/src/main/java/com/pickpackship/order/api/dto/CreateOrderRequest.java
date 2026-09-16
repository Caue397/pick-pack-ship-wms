package com.pickpackship.order.api.dto;

import com.pickpackship.order.domain.Party;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.Map;
import java.util.UUID;

public record CreateOrderRequest(
        @NotBlank(message = "Customer name is required") String customerName,
        @NotBlank(message = "Seller is required") UUID seller,
        @NotBlank(message = "Order number is required") Long orderNumber,
        @NotNull(message = "Sender is required") @Valid Party sender,
        @NotNull(message = "Recipient is required") @Valid Party recipient,
        @NotEmpty(message = "The items cannot be empty") Map<String, Integer> items
) {}
