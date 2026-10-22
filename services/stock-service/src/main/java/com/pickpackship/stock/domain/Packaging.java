package com.pickpackship.stock.domain;

import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.Positive;

/**
 * Inner/master box data set by the manufacturer. Every field is optional so a product can be
 * registered before its boxes; the unit itself is identified by {@link Product#getEanCode()}.
 */
@Embeddable
public record Packaging(
        String innerBoxBarcode,
        @Positive(message = "Units per inner box must be positive") Integer unitsPerInnerBox,
        String masterBoxBarcode,
        @Positive(message = "Units per master box must be positive") Integer unitsPerMasterBox
) {}
