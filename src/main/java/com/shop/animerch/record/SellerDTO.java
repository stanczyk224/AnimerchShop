package com.shop.animerch.record;

import java.util.List;

/**
 * DTO for {@link com.shop.animerch.model.Seller}
 */
public record SellerDTO(Long id, String name, List<ProductSummaryDTO> products) {
}