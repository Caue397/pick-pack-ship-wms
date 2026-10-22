package com.pickpackship.stock.domain;

import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.NotBlank;

/** Physical position in the warehouse. Columns are prefixed with {@code location_} by {@link StockItem}. */
@Embeddable
public record Location(
        @NotBlank(message = "Block is required") String block,
        @NotBlank(message = "Aisle is required") String aisle,
        @NotBlank(message = "Column is required") String column,
        @NotBlank(message = "Level is required") String level
) {}
