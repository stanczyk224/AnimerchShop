package com.shop.animerch.repository;

import com.shop.animerch.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findAllByConsumerIdOrderByCreatedAtDesc(Long consumerId);

    Optional<Order> findByIdAndConsumerId(Long orderId, Long consumerId);

}
