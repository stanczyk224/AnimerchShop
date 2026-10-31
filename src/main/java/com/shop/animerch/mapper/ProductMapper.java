package com.shop.animerch.mapper;

import com.shop.animerch.model.Product;
import com.shop.animerch.record.ProductDTO;
import com.shop.animerch.record.ProductUpdateDTO;
import org.mapstruct.*;

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
    @BeanMapping(
            nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
    )
    void updateProductFromDto(
            ProductUpdateDTO dto,
            @MappingTarget Product product
    );
}