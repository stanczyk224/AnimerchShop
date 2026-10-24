package com.shop.animerch.record;

import com.shop.animerch.model.Product;

import java.util.List;

/**
 * DTO for {@link Product}
 */
public record ProductDTO(
        Long id,
        String name,
        String description,
        String model,
        String weight,
        int quantity,
        double price,
        SellerSummaryDTO seller,
        List<CategoryDTO> categories
) {}