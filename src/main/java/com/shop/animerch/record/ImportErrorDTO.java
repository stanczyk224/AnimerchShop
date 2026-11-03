package com.shop.animerch.record;

public record ImportErrorDTO(
        int row,
        String reason
) {
}