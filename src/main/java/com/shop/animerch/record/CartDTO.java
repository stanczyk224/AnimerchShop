
package com.shop.animerch.record;

import java.util.List;

public record CartDTO(
        Long id,
        Long consumerId,
        List<CartItemDTO> items
) {
}
