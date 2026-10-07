package com.shop.animerch.mapper;

import com.shop.animerch.model.Seller;
import com.shop.animerch.record.SellerDTO;
import com.shop.animerch.record.SellerSummaryDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface SellerMapper {
    SellerDTO toDTO(Seller seller);

    @Named("toSummaryDTO")
    SellerSummaryDTO toSummaryDTO(Seller seller);

    List<SellerDTO> toDTOList(List<Seller> sellers);
}
