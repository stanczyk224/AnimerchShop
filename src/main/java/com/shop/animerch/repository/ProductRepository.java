package com.shop.animerch.repository;

import com.shop.animerch.model.Product;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.domain.Specification;
@Repository
public interface ProductRepository
        extends JpaRepository<Product, Long>,
        JpaSpecificationExecutor<Product> {

    @Query("SELECT p FROM Product p WHERE p.name = ?1")
    Optional<Product> findProductByName(String name);

    @EntityGraph(attributePaths = {"categories", "seller"})
    @Query("SELECT p FROM Product p")
    List<Product> findAllWithCategories();

    @EntityGraph(attributePaths = {"categories", "seller"})
    @Query("SELECT p FROM Product p")
    Page<Product> findAllWithCategories(Pageable pageable);

    @EntityGraph(attributePaths = {"categories", "seller"})
    @Query("SELECT p FROM Product p WHERE p.id = :id")
    Optional<Product> findByIdWithCategories(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Product p WHERE p.id = :id")
    Optional<Product> findByIdForUpdate(@Param("id") Long id);

    Page<Product> findAll(Pageable pageable);

    @EntityGraph(attributePaths = {"categories", "seller"})
    @Query("""
        SELECT p FROM Product p
        WHERE LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%'))
        """)

    Page<Product> searchProducts(
            @Param("search") String search,
            Pageable pageable
    );
    @EntityGraph(attributePaths = {"categories", "seller"})
    @Override
    Page<Product> findAll(Specification<Product> spec, Pageable pageable);

}
