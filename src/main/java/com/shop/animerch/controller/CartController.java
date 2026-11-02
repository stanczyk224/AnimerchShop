package com.shop.animerch.controller;

import com.shop.animerch.record.CartDTO;
import com.shop.animerch.service.CartItemService;
import com.shop.animerch.service.CartService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(path =  "/api/cart")
@SecurityRequirement(name = "Bearer Authentication")
@RequiredArgsConstructor
public class CartController {
    private final CartService cartService;
    private final CartItemService cartItemService;

    @GetMapping
    public CartDTO getCart() {
        return cartService.getCart();
    }

    @DeleteMapping
    public void clearCart() {
        cartItemService.clearCart();
    }
}
