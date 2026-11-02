
package com.shop.animerch.controller;

import com.shop.animerch.record.CartItemDTO;
import com.shop.animerch.service.CartItemService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cart/items")
@SecurityRequirement(name = "Bearer Authentication")
@RequiredArgsConstructor
public class CartItemController {

    private final CartItemService cartItemService;

    @GetMapping
    public List<CartItemDTO> getCartItems() {
        return cartItemService.getCartItems();
    }

    @GetMapping("/{productId}")
    public CartItemDTO getCartItem(
            @PathVariable Long productId
    ) {
        return cartItemService.getCartItem(productId);
    }

    @PostMapping("/{productId}")
    public CartItemDTO addProductToCart(
            @PathVariable Long productId,
            @RequestParam(defaultValue = "1") int quantity
    ) {
        return cartItemService.addProductToCart(productId, quantity);
    }

    @PutMapping("/{productId}")
    public CartItemDTO editCartItemQuantity(
            @PathVariable Long productId,
            @RequestParam("quantity") int quantity
    ) {
        return cartItemService.editQuantityCartItem(productId, quantity);
    }

    @DeleteMapping("/{productId}")
    public void deleteCartItem(
            @PathVariable Long productId
    ) {
        cartItemService.deleteCartItem(productId);
    }

}
