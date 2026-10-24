package com.shop.animerch.repository;

import com.shop.animerch.model.Seller;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SellerRepository extends JpaRepository<Seller, Long> {
    @EntityGraph(attributePaths = "products")
    @Query("SELECT s FROM Seller s")
    List<Seller> findAllWithProducts();

    @EntityGraph(attributePaths = "products")
    @Query("SELECT s FROM Seller s WHERE s.id = :id")
    Optional<Seller> findByIdWithProducts(@Param("id") Long id);
}