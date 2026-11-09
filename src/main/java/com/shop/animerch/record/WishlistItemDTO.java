package com.shop.animerch.record;

import java.time.LocalDateTime;

public record WishlistItemDTO(Long id, ProductSummaryDTO product, LocalDateTime createdAt) {
}
