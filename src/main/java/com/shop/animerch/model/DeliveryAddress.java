package com.shop.animerch.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class DeliveryAddress {
    @Column(name = "delivery_recipient_name", length = 255, updatable = false)
    private String recipientName;
    @Column(name = "delivery_phone", length = 30, updatable = false)
    private String phone;
    @Column(name = "delivery_address_line1", length = 255, updatable = false)
    private String addressLine1;
    @Column(name = "delivery_address_line2", length = 255, updatable = false)
    private String addressLine2;
    @Column(name = "delivery_city", length = 100, updatable = false)
    private String city;
    @Column(name = "delivery_postal_code", length = 20, updatable = false)
    private String postalCode;
    @Column(name = "delivery_country", length = 100, updatable = false)
    private String country;
}
