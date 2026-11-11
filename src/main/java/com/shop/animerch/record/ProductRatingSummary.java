package com.shop.animerch.record;

/** One row of calculated review statistics, matched to a product by its ID. */
public record ProductRatingSummary(Long productId, Double averageRating, long reviewCount) {
}
