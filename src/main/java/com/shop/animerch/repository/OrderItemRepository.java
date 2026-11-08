package com.shop.animerch.repository;

import com.shop.animerch.model.OrderItem;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT item FROM OrderItem item WHERE item.id = :id AND item.order.id = :orderId")
    Optional<OrderItem> findByIdAndOrderIdForUpdate(@Param("id") Long id, @Param("orderId") Long orderId);
}
