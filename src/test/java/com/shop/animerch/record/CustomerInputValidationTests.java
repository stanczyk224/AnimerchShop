package com.shop.animerch.record;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class CustomerInputValidationTests {
    static final jakarta.validation.ValidatorFactory FACTORY = Validation.buildDefaultValidatorFactory();
    static final Validator VALIDATOR = FACTORY.getValidator();
    @AfterAll static void close() { FACTORY.close(); }

    @ParameterizedTest
    @ValueSource(strings = {"hello", "+998abc1234567", "12345", "1234567890123456", "+0000000", "123\n4567", "++998901234567"})
    void invalidPhonesCannotRegisterOrCheckout(String phone) {
        assertFalse(VALIDATOR.validate(new RegisterRequest("Test User", phone, "test@example.com", "password123")).isEmpty());
        assertFalse(VALIDATOR.validate(new CheckoutRequest(address("Test User", phone, "Tashkent", "100000"))).isEmpty());
    }

    @ParameterizedTest
    @ValueSource(strings = {"+998901234567", "+998 90 123 45 67", "1234567", "+123456789012345", "1 (234) 567-8900"})
    void internationalPhonesAndNamesAreAccepted(String phone) {
        assertTrue(VALIDATOR.validate(new RegisterRequest("José O’Neill", phone, "test@example.com", "password123")).isEmpty());
        assertTrue(VALIDATOR.validate(new CheckoutRequest(address("李 明", phone, "Ташкент", "SW1A 1AA"))).isEmpty());
    }

    @Test void malformedIdentityAndDeliveryFieldsAreRejected() {
        assertFalse(VALIDATOR.validate(new RegisterRequest("123!", "+998901234567", "invalid", "short")).isEmpty());
        assertFalse(VALIDATOR.validate(new LoginRequest("not-an-email", " ")).isEmpty());
        assertFalse(VALIDATOR.validate(new CheckoutRequest(address("User", "+998901234567", "123@", "<script>"))).isEmpty());
    }

    @Test void passwordsRespectBcryptByteLimitWithoutRejectingUnicode() {
        assertTrue(VALIDATOR.validate(new RegisterRequest("User", "+998901234567", "user@example.com", "é".repeat(36))).isEmpty());
        assertFalse(VALIDATOR.validate(new RegisterRequest("User", "+998901234567", "user@example.com", "é".repeat(37))).isEmpty());
        assertFalse(VALIDATOR.validate(new LoginRequest("user@example.com", "é".repeat(37))).isEmpty());
    }

    private DeliveryAddressDTO address(String name, String phone, String city, String postal) {
        return new DeliveryAddressDTO(name, phone, "12 Main Street", null, city, postal, "Uzbekistan");
    }
}
