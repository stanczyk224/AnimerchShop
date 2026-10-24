package com.shop.animerch.mapper;

import com.shop.animerch.model.Product;
import com.shop.animerch.record.ProductDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import java.util.List;

@Mapper(
        componentModel = "spring",
        uses = {
                SellerMapper.class,
                CategoryMapper.class
        },
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface ProductMapper {

    @Mapping(target = "seller", qualifiedByName = "toSummaryDTO")
    ProductDTO toDTO(Product product);

    List<ProductDTO> toDTOList(List<Product> products);
}