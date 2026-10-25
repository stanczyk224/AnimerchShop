package com.shop.animerch.controller;

import com.shop.animerch.record.CartItemDTO;
import com.shop.animerch.service.CartItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(path = "/api/cart/{cartId}/items")
@RequiredArgsConstructor
public class CartItemController {
    private final CartItemService cartItemService;

    @GetMapping
    public List<CartItemDTO> getCartItems(
            @PathVariable Long cartId
    ) {
        return cartItemService.getCartItemsByCartId(cartId);
    }
    @PostMapping("/{productId}")
    public CartItemDTO addProductToCart(
            @PathVariable Long cartId,
            @PathVariable Long productId
    ) {
        return cartItemService.addProductToCart(cartId, productId);
    }
    @PutMapping("/{productId}")
    public CartItemDTO editCartItemQuantity(
            @PathVariable Long cartId,
            @PathVariable Long productId,
            @RequestParam("quantity") int quantity
    ) {
        return cartItemService.editQuantityCartItem(cartId, productId, quantity);
    }
    @DeleteMapping("/{productId}")
    public void deleteCartItem(
            @PathVariable Long cartId,
            @PathVariable Long productId
    ) {
        cartItemService.deleteCartItem(cartId, productId);
    }

}
