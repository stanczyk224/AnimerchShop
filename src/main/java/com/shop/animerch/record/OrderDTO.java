package com.shop.animerch.record;

import java.math.BigDecimal;
import java.util.List;

public record OrderDTO(
        Long id,
        Long consumerId,
        BigDecimal totalPrice,
        BigDecimal originalTotal,
        BigDecimal cancelledTotal,
        BigDecimal activeTotal,
        DeliveryAddressDTO deliveryAddress,
        List<OrderItemDTO> items
        ) {
}
