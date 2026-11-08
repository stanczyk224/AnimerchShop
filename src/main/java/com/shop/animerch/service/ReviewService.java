package com.shop.animerch.service;

import com.shop.animerch.exception.ConflictException;
import com.shop.animerch.exception.ResourceNotFoundException;
import com.shop.animerch.model.Consumer;
import com.shop.animerch.model.Product;
import com.shop.animerch.model.Review;
import com.shop.animerch.model.OrderStatus;
import com.shop.animerch.record.ReviewCreateDTO;
import com.shop.animerch.record.ReviewDTO;
import com.shop.animerch.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ReviewService {
    private final ReviewRepository reviewRepository;
    private final ConsumerRepository consumerRepository;
    private final ProductRepository productRepository;
    private final OrderItemRepository orderItemRepository;

    @Transactional
    public ReviewDTO create(Long productId, ReviewCreateDTO dto) {
        Consumer consumer = getCurrentConsumerForUpdate();
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
        if (!orderItemRepository.existsByOrderConsumerIdAndProductIdAndStatus(
                consumer.getId(), productId, OrderStatus.DELIVERED)) {
            throw new AccessDeniedException("You can only review products delivered to you");
        }
        if (reviewRepository.existsByConsumerIdAndProductId(consumer.getId(), productId)) {
            throw new ConflictException("You have already reviewed this product");
        }
        Review review = new Review();
        review.setConsumer(consumer);
        review.setProduct(product);
        review.setRating(dto.rating());
        review.setComment(dto.comment().strip());
        return toDTO(reviewRepository.save(review));
    }

    private Consumer getCurrentConsumerForUpdate() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            throw new AccessDeniedException("User is not authenticated");
        }
        // Serialize submissions by the same customer so concurrent duplicates return 409.
        return consumerRepository.findByUserEmailForUpdate(authentication.getName())
                .orElseThrow(() -> new AccessDeniedException("A customer profile is required"));
    }

    @Transactional
    public ReviewDTO update(Long productId, Long reviewId, ReviewCreateDTO dto) {
        Consumer consumer = getCurrentConsumerForUpdate();
        Review review = getOwnedReview(productId, reviewId, consumer);
        String comment = dto.comment().strip();
        if (review.getRating() != dto.rating() || !review.getComment().equals(comment)) {
            review.setRating(dto.rating());
            review.setComment(comment);
            review.setEditedAt(LocalDateTime.now());
        }
        return toDTO(review);
    }

    @Transactional
    public void delete(Long productId, Long reviewId) {
        Consumer consumer = getCurrentConsumerForUpdate();
        reviewRepository.delete(getOwnedReview(productId, reviewId, consumer));
    }

    private Review getOwnedReview(Long productId, Long reviewId, Consumer consumer) {
        Review review = reviewRepository.findById(reviewId)
                .filter(existing -> existing.getProduct().getId().equals(productId))
                .orElseThrow(() -> new ResourceNotFoundException("Review not found for this product"));
        if (!review.getConsumer().getId().equals(consumer.getId())) {
            throw new AccessDeniedException("You can only modify your own reviews");
        }
        return review;
    }

    @Transactional(readOnly = true)
    public Page<ReviewDTO> getByProduct(Long productId, Pageable pageable) {
        if (!productRepository.existsById(productId)) {
            throw new ResourceNotFoundException("Product not found");
        }
        return reviewRepository.findAllByProductId(productId, pageable).map(this::toDTO);
    }

    private ReviewDTO toDTO(Review review) {
        return new ReviewDTO(review.getId(), review.getProduct().getId(),
                review.getConsumer().getName(), review.getRating(), review.getComment(), review.getCreatedAt(),
                review.getEditedAt(), review.getEditedAt() != null);
    }
}
