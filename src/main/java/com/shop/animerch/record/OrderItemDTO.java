package com.shop.animerch.record;

import java.math.BigDecimal;

public record OrderItemDTO(
        Long id,
        Long productId,
        String productName,
        Long quantity,
        BigDecimal unitPrice

) {
}
