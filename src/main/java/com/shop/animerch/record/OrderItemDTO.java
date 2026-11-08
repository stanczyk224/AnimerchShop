package com.shop.animerch.record;

import com.shop.animerch.model.OrderStatus;

import java.math.BigDecimal;

public record OrderItemDTO(
        Long id,
        Long productId,
        String productName,
        Long quantity,
        BigDecimal unitPrice,
        OrderStatus status
) {
}
