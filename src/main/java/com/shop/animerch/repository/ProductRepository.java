
package com.shop.animerch.repository;

import com.shop.animerch.model.Product;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository
        extends JpaRepository<Product, Long>,
        JpaSpecificationExecutor<Product> {

    @Query("SELECT p FROM Product p WHERE p.name = ?1")
    Optional<Product> findProductByName(String name);

    @EntityGraph(attributePaths = {
            "photos",
            "seller",
            "seller.photo"
    })
    @Query("SELECT DISTINCT p FROM Product p")
    List<Product> findAllWithCategories();

    @EntityGraph(attributePaths = {
            "photos",
            "seller",
            "seller.photo"
    })
    @Query("SELECT DISTINCT p FROM Product p WHERE p.id = :id")
    Optional<Product> findByIdWithCategories(@Param("id") Long id);

    @EntityGraph(attributePaths = {
            "photos",
            "seller",
            "seller.photo"
    })
    @Query("""
        SELECT DISTINCT p
        FROM Product p
        WHERE p.id IN :ids
        """)
    List<Product> findAllWithPhotosByIdIn(@Param("ids") List<Long> ids);


    @EntityGraph(attributePaths = "categories")
    @Query("""
        SELECT DISTINCT p
        FROM Product p
        WHERE p.id IN :ids
        """)
    List<Product> findAllWithCategoriesByIdIn(@Param("ids") List<Long> ids);

    // Pessimistic lock for stock-sensitive operations
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Product p WHERE p.id = :id")
    Optional<Product> findByIdForUpdate(@Param("id") Long id);

    // Product filtering and pagination
    // No EntityGraph here: pagination must happen in SQL
    @Override
    Page<Product> findAll(
            Specification<Product> spec,
            Pageable pageable
    );

    // Name uniqueness check
    boolean existsByNameIgnoreCase(String name);
}
