package com.shop.animerch.repository;

import com.shop.animerch.model.SellerPhoto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SellerPhotoRepository extends JpaRepository<SellerPhoto, Long> {

    List<SellerPhoto> findAllBySellerId(Long sellerId);

    Optional<SellerPhoto> findByIdAndSellerId(Long id, Long sellerId);

    long countBySellerId(Long sellerId);

    Optional<SellerPhoto> findBySellerId(Long sellerId);
}