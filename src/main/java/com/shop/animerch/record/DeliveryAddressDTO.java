package com.shop.animerch.record;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;
import com.shop.animerch.validation.InputRules;

public record DeliveryAddressDTO(
        @NotBlank @Size(max = 255)
        @Pattern(regexp = InputRules.NAME, message = "Enter a name using letters, spaces, apostrophes, periods or hyphens") String recipientName,
        @NotBlank @Size(max = 30)
        @Pattern(regexp = InputRules.PHONE, message = "Enter 7 to 15 digits with an optional +; letters are not allowed") String phone,
        @NotBlank @Size(max = 255)
        @Pattern(regexp = InputRules.ADDRESS, message = "Enter a street address containing letters or numbers without control characters") String addressLine1,
        @Size(max = 255)
        @Pattern(regexp = "(?:|" + InputRules.ADDRESS + ")", message = "Use letters or numbers without control characters for the apartment or unit") String addressLine2,
        @NotBlank @Size(max = 100)
        @Pattern(regexp = InputRules.NAME, message = "Enter a city using letters and normal punctuation") String city,
        @NotBlank @Size(max = 20)
        @Pattern(regexp = InputRules.POSTAL_CODE, message = "Use letters, numbers, spaces or hyphens for the postal code") String postalCode,
        @NotBlank @Size(max = 100)
        @Pattern(regexp = InputRules.NAME, message = "Enter a country using letters and normal punctuation") String country
) {}
