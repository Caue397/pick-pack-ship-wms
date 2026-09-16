package com.pickpackship.order.domain;

import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.NotBlank;

@Embeddable
public record Address(
        @NotBlank(message = "Street is required") String street,
        @NotBlank(message = "Number is required") String number,
        @NotBlank(message = "Neighborhood is required") String neighborhood,
        @NotBlank(message = "City is required") String city,
        @NotBlank(message = "State is required") String state,
        @NotBlank(message = "Zip code is required") String zipCode
) {}
