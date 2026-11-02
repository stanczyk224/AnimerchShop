package com.shop.animerch.mapper;

import com.shop.animerch.model.Consumer;
import com.shop.animerch.record.ConsumerDTO;
import com.shop.animerch.record.ConsumerSummaryDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface ConsumerMapper {
    @Mapping(source = "user.email", target = "email")
    ConsumerDTO toDTO(Consumer consumer);
    @Named("toSummaryDTO")
    ConsumerSummaryDTO toSummaryDTO(Consumer consumer);

    List<ConsumerDTO> toDTOList(List<Consumer> consumers);

}
