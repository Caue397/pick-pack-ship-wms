package com.pickpackship.stock.domain;

import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.Positive;

@Embeddable
public record Dimensions(
        @Positive(message = "Height must be positive") Integer heightMm,
        @Positive(message = "Width must be positive") Integer widthMm,
        @Positive(message = "Depth must be positive") Integer depthMm
) {}
