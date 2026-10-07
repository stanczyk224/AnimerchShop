package com.shop.animerch.record;

import com.shop.animerch.model.Product;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.ArrayList;
import java.util.List;

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
