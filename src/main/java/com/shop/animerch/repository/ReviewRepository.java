package com.shop.animerch.repository;

import com.shop.animerch.model.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    boolean existsByConsumerIdAndProductId(Long consumerId, Long productId);

    @EntityGraph(attributePaths = {"consumer", "product"})
    Page<Review> findAllByProductId(Long productId, Pageable pageable);
}
