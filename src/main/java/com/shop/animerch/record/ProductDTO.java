package com.shop.animerch.record;

import com.shop.animerch.model.Product;

import java.util.List;
import java.math.BigDecimal;

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
        BigDecimal price,
        SellerSummaryDTO seller,
        Double averageRating,
        long reviewCount,
        List<CategoryDTO> categories,
        List<ProductPhotoDTO> photos
) {}
