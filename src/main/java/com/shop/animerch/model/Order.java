package com.shop.animerch.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "customer_order")
@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "consumer_id", nullable = false)
    private Consumer consumer;
    @Column(name = "created_at", updatable = false, nullable = false)
    private LocalDateTime createdAt;
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
    @Column(name = "total_price", nullable = false)
    private BigDecimal totalPrice;
    @Column(name = "checkout_key", length = 128, updatable = false)
    private String checkoutKey;
    @Embedded
    private DeliveryAddress deliveryAddress;
    @OneToMany(mappedBy = "order", cascade = CascadeType.PERSIST)
    @OrderBy("id ASC")
    private List<OrderItem> orderItems = new ArrayList<>();

    public void addItem(OrderItem item) {
        this.orderItems.add(item);
        item.setOrder(this);
    }

    public BigDecimal calculateTotalPrice() {
        return orderItems.stream()
                .map(OrderItem::calculateSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public void recalculateTotalPrice() {
        this.totalPrice = calculateTotalPrice();
    }

    public BigDecimal getOriginalTotal() {
        return calculateTotalPrice();
    }

    public BigDecimal getCancelledTotal() {
        return orderItems.stream()
                .filter(item -> item.getStatus() == OrderStatus.CANCELLED)
                .map(OrderItem::calculateSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal getActiveTotal() {
        return getOriginalTotal().subtract(getCancelledTotal());
    }

}
