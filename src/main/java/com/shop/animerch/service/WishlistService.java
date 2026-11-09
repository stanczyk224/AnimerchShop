package com.shop.animerch.service;

import com.shop.animerch.exception.BadRequestException;
import com.shop.animerch.exception.ResourceNotFoundException;
import com.shop.animerch.model.Consumer;
import com.shop.animerch.model.WishlistItem;
import com.shop.animerch.record.ProductSummaryDTO;
import com.shop.animerch.record.WishlistItemDTO;
import com.shop.animerch.repository.ConsumerRepository;
import com.shop.animerch.repository.ProductRepository;
import com.shop.animerch.repository.WishlistItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class WishlistService {
    private final WishlistItemRepository wishlistRepository;
    private final ConsumerRepository consumerRepository;
    private final ProductRepository productRepository;

    private Consumer currentConsumer(boolean lock) {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            throw new AccessDeniedException("Authentication required");
        }
        var consumer = lock
                ? consumerRepository.findByUserEmailForUpdate(authentication.getName())
                : consumerRepository.findByUserEmail(authentication.getName());
        return consumer.orElseThrow(() -> new AccessDeniedException("A customer profile is required"));
    }

    @Transactional
    public WishlistItemDTO add(Long productId) {
        // Serialize changes for one customer, including concurrent duplicate saves.
        Consumer consumer = currentConsumer(true);
        var existing = wishlistRepository.findByConsumerIdAndProductId(consumer.getId(), productId);
        if (existing.isPresent()) {
            return toDTO(existing.get());
        }
        var product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
        WishlistItem item = new WishlistItem();
        item.setConsumer(consumer);
        item.setProduct(product);
        return toDTO(wishlistRepository.save(item));
    }

    @Transactional(readOnly = true)
    public Page<WishlistItemDTO> getMyWishlist(int page, int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw new BadRequestException("Page must be non-negative and size must be between 1 and 100");
        }
        Consumer consumer = currentConsumer(false);
        var pageable = PageRequest.of(page, size, Sort.by(
                Sort.Order.desc("createdAt"), Sort.Order.desc("id")));
        return wishlistRepository.findAllByConsumerId(consumer.getId(), pageable).map(this::toDTO);
    }

    @Transactional
    public void remove(Long productId) {
        Consumer consumer = currentConsumer(true);
        wishlistRepository.findByConsumerIdAndProductId(consumer.getId(), productId)
                .ifPresent(wishlistRepository::delete);
    }

    private WishlistItemDTO toDTO(WishlistItem item) {
        var product = item.getProduct();
        return new WishlistItemDTO(item.getId(),
                new ProductSummaryDTO(product.getId(), product.getName(), product.getPrice()), item.getCreatedAt());
    }
}
