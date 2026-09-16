package com.pickpackship.order.domain;

import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Embeddable
public record Party(
        @NotBlank(message = "Name is required") String name,
        @NotBlank(message = "Document is required") String document,
        @NotNull(message = "Address is required") @Valid @Embedded Address address
) {}
