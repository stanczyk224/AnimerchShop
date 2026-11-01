package com.shop.animerch.record;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ConsumerCreateDTO(
        @NotBlank(message = "Name is required")
        @Size(max = 255, message = "Name must be at most 255 characters")
        String name,

        @NotBlank(message = "Phone is required")
        @Size(max = 30, message = "Phone must be at most 255 characters")
        String phone

) {}