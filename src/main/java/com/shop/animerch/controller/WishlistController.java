package com.shop.animerch.controller;

import com.shop.animerch.record.WishlistItemDTO;
import com.shop.animerch.service.WishlistService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/wishlist")
@SecurityRequirement(name = "Bearer Authentication")
@RequiredArgsConstructor
public class WishlistController {
    private final WishlistService wishlistService;

    @PostMapping("/items/{productId}")
    public WishlistItemDTO add(@PathVariable Long productId) {
        return wishlistService.add(productId);
    }

    @GetMapping
    public Page<WishlistItemDTO> getMyWishlist(@RequestParam(defaultValue = "0") int page,
                                               @RequestParam(defaultValue = "20") int size) {
        return wishlistService.getMyWishlist(page, size);
    }

    @DeleteMapping("/items/{productId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remove(@PathVariable Long productId) {
        wishlistService.remove(productId);
    }
}
