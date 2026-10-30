package com.shop.animerch.record;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.List;

public record OrderCreateDTO(
        @NotNull(message = "Consumer ID is required")
        @Positive(message = "Consumer ID must be positive")
        Long consumerId,
        @NotEmpty
        @Valid
        List<OrderItemCreateDTO> items
) {
}
