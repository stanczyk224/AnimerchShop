package com.shop.animerch.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "wishlist_item",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_wishlist_consumer_product",
                columnNames = {"consumer_id", "product_id"}
        )
)
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class WishlistItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne
    @JoinColumn(name = "consumer_id", nullable = false)
    private Consumer consumer;

    @Column(name = "created_at", updatable = false, nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now().truncatedTo(java.time.temporal.ChronoUnit.MICROS);
    }
}
