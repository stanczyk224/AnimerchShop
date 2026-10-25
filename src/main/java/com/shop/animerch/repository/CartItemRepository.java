package com.shop.animerch.repository;

import com.shop.animerch.model.Cart;
import com.shop.animerch.model.CartItem;
import com.shop.animerch.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Long> {
    Optional<CartItem> findByCartAndProduct(Cart cart, Product product);
    List<CartItem> findAllByCart_Id(Long cartId);

    boolean existsByCart_IdAndProduct_Id(Long id, Long id1);
}
