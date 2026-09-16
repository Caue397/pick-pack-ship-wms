package com.pickpackship.order.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record PartyRequest(
        @NotBlank(message = "Name of sender and recipient are needed") String name,
        @NotBlank(message = "Document of sender and recipient are needed") String document,
        @Valid() @NotNull(message = "Address of sender and recipient are needed") AddressRequest address
) {
}
