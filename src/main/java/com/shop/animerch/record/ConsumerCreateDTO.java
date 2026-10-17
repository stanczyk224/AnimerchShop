package com.shop.animerch.record;

import jakarta.validation.constraints.NotBlank;

public record ConsumerCreateDTO(
        @NotBlank
        String name,
        @NotBlank
        String phone,
        @NotBlank
        String email

) {
}
