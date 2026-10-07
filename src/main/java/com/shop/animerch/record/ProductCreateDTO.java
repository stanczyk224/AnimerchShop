package com.shop.animerch.record;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record ProductCreateDTO(
        @NotBlank
        String name,

        @NotBlank
        String description,

        String model,
        String weight,
        int quantity,

        @Positive
        BigDecimal price,

        @NotNull
        Long sellerId

) {
}
