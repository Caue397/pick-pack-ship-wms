package com.pickpackship.order.api.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateSellerRequest(
        @NotBlank(message = "Name is required") String name,
        @NotBlank(message = "Document is required") String document
) {}
