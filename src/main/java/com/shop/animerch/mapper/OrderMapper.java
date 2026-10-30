package com.shop.animerch.mapper;

import com.shop.animerch.model.Order;
import com.shop.animerch.model.OrderItem;
import com.shop.animerch.record.OrderDTO;
import com.shop.animerch.record.OrderItemDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface OrderMapper {
    @Mapping(target = "consumerId", source = "consumer.id")
    @Mapping(target = "items", source = "orderItems")
    OrderDTO toDTO(Order order);
    @Mapping(target = "productId", source = "product.id")
    OrderItemDTO toDTO(OrderItem orderItem);
}