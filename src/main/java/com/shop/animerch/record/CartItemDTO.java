package com.shop.animerch.record;

import java.util.List;

public record CartItemDTO(
        Long id,
        Long cartId,
        Long productId,
        int quantity,
        List<ProductPhotoDTO> photos) {
}
