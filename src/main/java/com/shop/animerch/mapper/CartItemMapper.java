package com.shop.animerch.mapper;

import com.shop.animerch.model.CartItem;
import com.shop.animerch.record.CartItemDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface CartItemMapper {
    @Mapping(target = "cartId", source = "cart.id")
    @Mapping(target = "productId", source = "product.id")
    CartItemDTO toDTO(CartItem cartItem);
    List<CartItemDTO> toDTOList(List<CartItem> cartItems);
}
