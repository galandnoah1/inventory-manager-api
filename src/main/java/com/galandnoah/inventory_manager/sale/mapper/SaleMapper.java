package com.galandnoah.inventory_manager.sale.mapper;

import com.galandnoah.inventory_manager.sale.dto.SaleRequest;
import com.galandnoah.inventory_manager.sale.dto.SaleResponse;
import com.galandnoah.inventory_manager.sale.entity.Sale;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface SaleMapper {

    // From Request DTO to Entity
    @Mapping(target = "product", ignore = true)
    Sale toEntity(SaleRequest saleRequest);

    // From Entity to Response DTO
    @Mapping(target = "designation", source = "product.designation")
    SaleResponse toResponse(Sale sale);
}
