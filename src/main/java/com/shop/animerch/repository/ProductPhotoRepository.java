package com.shop.animerch.repository;

import com.shop.animerch.model.ProductPhoto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProductPhotoRepository
        extends JpaRepository<ProductPhoto, Long> {

    List<ProductPhoto> findAllByProductId(Long productId);

    long countByProductId(Long productId);

    Optional<ProductPhoto> findByIdAndProductId(Long id, Long productId);
}