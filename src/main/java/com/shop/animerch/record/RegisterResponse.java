package com.shop.animerch.record;

import com.shop.animerch.model.UserRole;

public record RegisterResponse(
        Long userId,
        String email,
        UserRole role
) {
}
