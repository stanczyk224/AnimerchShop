package com.shop.animerch.mapper;

import com.shop.animerch.model.Category;
import com.shop.animerch.record.CategoryDTO;
import com.shop.animerch.record.CategorySummaryDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface CategoryMapper {
    CategoryDTO toDTO(Category category);
    @Named("toSummaryDTO")
    CategorySummaryDTO toSummaryDTO(Category category);

    List<CategoryDTO> toDTOList(List<Category> categories);
}
