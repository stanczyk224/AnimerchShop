package com.shop.animerch.record;

import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CheckoutRequestTest {
    private CheckoutRequest request(String[] fields) {
        return new CheckoutRequest(new DeliveryAddressDTO(fields[0], fields[1], fields[2],
                fields[3], fields[4], fields[5], fields[6]));
    }

    @Test
    void requiredFieldsRejectNullAndBlankAndAllFieldsEnforceLengths() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var validator = factory.getValidator();
            String[] valid = {"Recipient", "+998901234567", "Street", null, "Tashkent", "100000", "Uzbekistan"};
            int[] limits = {255, 30, 255, 255, 100, 20, 100};
            assertTrue(validator.validate(request(valid)).isEmpty());
            assertFalse(validator.validate(new CheckoutRequest(null)).isEmpty());
            for (int i = 0; i < valid.length; i++) {
                String[] fields = valid.clone();
                fields[i] = "x".repeat(limits[i]);
                assertTrue(validator.validate(request(fields)).isEmpty(), "Boundary field " + i);
                fields[i] += "x";
                assertFalse(validator.validate(request(fields)).isEmpty(), "Length field " + i);
                if (i != 3) {
                    fields[i] = null;
                    assertFalse(validator.validate(request(fields)).isEmpty(), "Null field " + i);
                    fields[i] = " \t";
                    assertFalse(validator.validate(request(fields)).isEmpty(), "Blank field " + i);
                }
            }
        }
    }
}
