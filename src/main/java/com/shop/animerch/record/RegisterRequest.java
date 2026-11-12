package com.shop.animerch.record;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;
import com.shop.animerch.validation.InputRules;

public record RegisterRequest(
        @NotBlank(message = "Name is required")
        @Size(max = 100, message = "Name must not exceed 100 characters")
        @Pattern(regexp = InputRules.NAME, message = "Use letters, spaces, apostrophes, periods or hyphens for your name")
        String name,

        @NotBlank(message = "Phone is required")
        @Size(max = 30, message = "Phone must not exceed 30 characters")
        @Pattern(regexp = InputRules.PHONE, message = "Enter 7 to 15 digits, optionally starting with +; letters are not allowed")
        String phone,

        @NotBlank(message = "Email is required")
        @Email(message = "Email must be valid")
        @Size(max = 254, message = "Email must not exceed 254 characters")
        String email,

        @NotBlank(message = "Password is required")
        @com.shop.animerch.validation.Utf8Length
        @Size(min = 8, max = 72, message = "Password must contain between 8 and 72 characters")
        String password
) {}
