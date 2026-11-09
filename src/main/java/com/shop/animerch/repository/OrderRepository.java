package com.shop.animerch.repository;

import com.shop.animerch.model.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {
    Page<Order> findAllByConsumerId(Long consumerId, Pageable pageable);
    Optional<Order> findByConsumerIdAndCheckoutKey(Long consumerId, String checkoutKey);
}
