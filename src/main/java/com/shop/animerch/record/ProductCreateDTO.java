package com.shop.animerch.record;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record ProductCreateDTO(
        @NotBlank(message = "Name is required")
        @Size(max = 255, message = "Name must be at most 255 characters")
        String name,

        @NotBlank(message = "Description is required")
        @Size(max = 5000, message = "Description must be at most 5000 characters")
        String description,

        String model,
        String weight,

        @PositiveOrZero(message = "Quantity cannot be negative")
        int quantity,

        @NotNull(message = "Price is required")
        @Positive(message = "Price must be greater than zero")
        BigDecimal price,

        @NotNull(message = "Seller ID is required")
        @Positive(message = "Seller ID must be positive")
        Long sellerId
) {
}
