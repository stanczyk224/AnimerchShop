package com.shop.animerch.record;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record CheckoutRequest(@NotNull @Valid DeliveryAddressDTO deliveryAddress) {}
