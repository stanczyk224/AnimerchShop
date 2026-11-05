package com.shop.animerch.repository;

import com.shop.animerch.model.Cart;
import com.shop.animerch.model.CartItem;
import com.shop.animerch.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.EntityGraph;
import java.util.List;
import java.util.Optional;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Long> {
    Optional<CartItem> findByCartAndProduct(Cart cart, Product product);
    List<CartItem> findAllByCart_Id(Long cartId);
    @EntityGraph(attributePaths = {"product", "product.photos"})
    List<CartItem> findAllByCart(Cart cart);

    boolean existsByCart_IdAndProduct_Id(Long cartId, Long productId);
    void deleteAllByCart(Cart cart);
}
