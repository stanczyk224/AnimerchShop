package com.shop.animerch.record;

import com.shop.animerch.model.OrderStatus;
import jakarta.validation.constraints.NotNull;

public record OrderItemStatusUpdateDTO(
        @NotNull
        OrderStatus status
) {
}
