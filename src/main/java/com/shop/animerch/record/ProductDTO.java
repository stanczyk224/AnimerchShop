package com.shop.animerch.record;

import com.shop.animerch.model.Product;

/**
 * DTO for {@link Product}
 */
public record ProductDTO(
        Long id,
        String name,
        double price,
        String description,
        SellerSummaryDTO seller
) {}