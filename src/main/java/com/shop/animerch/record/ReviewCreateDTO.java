package com.shop.animerch.record;

import jakarta.validation.constraints.*;

public record ReviewCreateDTO(
        @NotNull @Min(1) @Max(5) Integer rating,
        @NotBlank @Size(max = 5000) String comment
) {}
