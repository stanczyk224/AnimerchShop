
package com.shop.animerch.mapper;

import com.shop.animerch.model.Cart;
import com.shop.animerch.record.CartDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface CartMapper {

    @Mapping(target = "consumerId", source = "consumer.id")
    @Mapping(target = "items", source = "cartItems")
    CartDTO toDTO(Cart cart);
}
