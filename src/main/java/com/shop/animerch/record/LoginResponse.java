package com.shop.animerch.record;

public record LoginResponse(
        String accessToken,
        String tokenType
) {
}
