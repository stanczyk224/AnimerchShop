package com.shop.animerch.record;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CategoryCreateDTO (
        @NotBlank(message = "name is required!")
        @Size(max = 50, message = "name must be less then 50 characters")
        String name
) {
}
