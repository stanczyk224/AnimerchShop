package com.shop.animerch.record;

import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ReviewCreateDTOTest {
    @Test
    void rejectsInvalidRatingsAndComments() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var validator = factory.getValidator();
            for (Integer rating : new Integer[]{null, 0, 6}) {
                assertFalse(validator.validate(new ReviewCreateDTO(rating, "Review")).isEmpty());
            }
            for (String comment : new String[]{null, "", "   ", "x".repeat(5001)}) {
                assertFalse(validator.validate(new ReviewCreateDTO(5, comment)).isEmpty());
            }
            assertTrue(validator.validate(new ReviewCreateDTO(1, "Review")).isEmpty());
            assertTrue(validator.validate(new ReviewCreateDTO(5, "x".repeat(5000))).isEmpty());
        }
    }
}
