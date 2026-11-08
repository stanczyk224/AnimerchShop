package com.shop.animerch.controller;

import com.shop.animerch.record.ReviewCreateDTO;
import com.shop.animerch.record.ReviewDTO;
import com.shop.animerch.service.ReviewService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/products/{productId}/reviews")
@SecurityRequirement(name = "Bearer Authentication")
@RequiredArgsConstructor
public class ReviewController {
    private final ReviewService reviewService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ReviewDTO create(@PathVariable Long productId, @Valid @RequestBody ReviewCreateDTO dto) {
        return reviewService.create(productId, dto);
    }

    @GetMapping
    public Page<ReviewDTO> getByProduct(@PathVariable Long productId,
            @ParameterObject @PageableDefault(size = 20, sort = {"createdAt", "id"},
                    direction = Sort.Direction.DESC) Pageable pageable) {
        return reviewService.getByProduct(productId, pageable);
    }

    @PutMapping("/{reviewId}")
    public ReviewDTO update(@PathVariable Long productId, @PathVariable Long reviewId,
            @Valid @RequestBody ReviewCreateDTO dto) {
        return reviewService.update(productId, reviewId, dto);
    }

    @DeleteMapping("/{reviewId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long productId, @PathVariable Long reviewId) {
        reviewService.delete(productId, reviewId);
    }
}
