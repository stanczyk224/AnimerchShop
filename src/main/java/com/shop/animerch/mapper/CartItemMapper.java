package com.shop.animerch.mapper;

import com.shop.animerch.model.CartItem;
import com.shop.animerch.record.CartItemDTO;
import com.shop.animerch.record.ProductPhotoDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import com.shop.animerch.model.ProductPhoto;
import java.util.List;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface CartItemMapper {
    @Mapping(target = "cartId", source = "cart.id")
    @Mapping(target = "productId", source = "product.id")
    @Mapping(target = "photos", source = "product.photos")
    CartItemDTO toDTO(CartItem cartItem);
    List<CartItemDTO> toDTOList(List<CartItem> cartItems);
    ProductPhotoDTO mapPhoto(ProductPhoto photo);
}
