package com.shop.animerch.mapper;

import com.shop.animerch.model.Product;
import com.shop.animerch.record.ProductDTO;
import com.shop.animerch.record.ProductUpdateDTO;
import com.shop.animerch.record.ProductRatingSummary;
import org.mapstruct.*;

@Mapper(
        componentModel = "spring",
        uses = {
                SellerMapper.class,
                CategoryMapper.class
        },
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface ProductMapper {

    @Mapping(target = "seller", source = "product.seller", qualifiedByName = "toSummaryDTO")
    @Mapping(target = "averageRating", source = "summary.averageRating")
    @Mapping(target = "reviewCount", source = "summary.reviewCount")
    ProductDTO toDTO(Product product, ProductRatingSummary summary);
    @BeanMapping(
            nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
    )
    void updateProductFromDto(
            ProductUpdateDTO dto,
            @MappingTarget Product product
    );
}
