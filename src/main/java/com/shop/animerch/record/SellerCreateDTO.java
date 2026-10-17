package com.shop.animerch.record;

import jakarta.validation.constraints.NotBlank;


public record SellerCreateDTO(
        @NotBlank
        String name,
        @NotBlank
        String description,
        @NotBlank
        String status,
        @NotBlank
        String phone,
        @NotBlank
        String email,
        @NotBlank
        String address

) {

}
