package com.shop.animerch.repository;

import com.shop.animerch.model.WishlistItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WishlistItemRepository extends JpaRepository<WishlistItem, Long> {
    Optional<WishlistItem> findByConsumerIdAndProductId(Long consumerId, Long productId);

    @EntityGraph(attributePaths = "product")
    Page<WishlistItem> findAllByConsumerId(Long consumerId, Pageable pageable);
}
