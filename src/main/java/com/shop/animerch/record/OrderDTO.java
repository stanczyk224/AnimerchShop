package com.shop.animerch.record;

import java.math.BigDecimal;
import java.util.List;

public record OrderDTO(
        Long id,
        Long consumerId,
        BigDecimal totalPrice,
        List<OrderItemDTO> items
        ) {
}
