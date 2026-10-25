package com.shop.animerch.record;

public record CartItemDTO(
        Long id,
        Long cartId,
        Long productId,
        int quantity) {
}
