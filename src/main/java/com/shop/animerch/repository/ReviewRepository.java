package com.shop.animerch.repository;

import com.shop.animerch.model.Review;
import com.shop.animerch.record.ProductRatingSummary;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    @Query("""
            select new com.shop.animerch.record.ProductRatingSummary(
                r.product.id, avg(r.rating), count(r))
            from Review r
            where r.product.id in :productIds
            group by r.product.id
            """)
    List<ProductRatingSummary> summarizeByProductIds(@Param("productIds") List<Long> productIds);

    boolean existsByConsumerIdAndProductId(Long consumerId, Long productId);

    @EntityGraph(attributePaths = {"consumer", "product"})
    Page<Review> findAllByProductId(Long productId, Pageable pageable);
}
