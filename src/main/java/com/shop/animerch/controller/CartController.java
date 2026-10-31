package com.shop.animerch.controller;

import com.shop.animerch.record.CartDTO;
import com.shop.animerch.service.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path =  "/api/cart")
@RequiredArgsConstructor
public class CartController {
    private final CartService cartService;

    @GetMapping("{consumerId}")
    public CartDTO getCartByConsumerId(
            @PathVariable("consumerId") Long consumerId
    ) {
        return cartService.getCart(consumerId);
    }
    
}
