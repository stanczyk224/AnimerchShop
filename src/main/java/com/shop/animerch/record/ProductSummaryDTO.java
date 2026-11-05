package com.shop.animerch.record;

import java.math.BigDecimal;

public record ProductSummaryDTO(
        Long id,
        String name,
        BigDecimal price
) {}
