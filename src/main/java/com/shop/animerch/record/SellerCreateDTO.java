
package com.shop.animerch.record;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SellerCreateDTO(
        @NotBlank(message = "Name is required")
        @Size(max = 255, message = "Name must be at most 255 characters")
        String name,

        @NotBlank(message = "Description is required")
        String description,

        @NotBlank(message = "Status is required")
        @Size(max = 50, message = "Status must be at most 50 characters")
        String status,

        @NotBlank(message = "Phone is required")
        @Size(max = 30, message = "Phone must be at most 30 characters")
        String phone,

        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        @Size(max = 255, message = "Email must be at most 255 characters")
        String email,

        @NotBlank(message = "Address is required")
        @Size(max = 500, message = "Address must be at most 500 characters")
        String address
) {
}
