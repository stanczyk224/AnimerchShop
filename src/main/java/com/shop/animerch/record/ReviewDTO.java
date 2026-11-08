package com.shop.animerch.record;

import java.time.LocalDateTime;

public record ReviewDTO(Long id, Long productId, String consumerName,
                        int rating, String comment, LocalDateTime createdAt,
                        LocalDateTime editedAt, boolean edited) {}
