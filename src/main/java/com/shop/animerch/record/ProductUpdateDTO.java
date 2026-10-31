package com.shop.animerch.record;

import com.shop.animerch.model.Category;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.List;

public record ProductUpdateDTO(

        @Size(max = 255, message = "Name must be at most 255 characters")
        String name,

        @Size(max = 5000, message = "Description must be at most 5000 characters")
        String description,

        String model,
        String weight,

        @PositiveOrZero(message = "Quantity cannot be negative")
        Integer quantity,

        @Positive(message = "Price must be greater than zero")
        BigDecimal price,

        List<Long> categoryIds
) {
}
