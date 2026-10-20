package com.galandnoah.inventory_manager.supply.mapper;

import com.galandnoah.inventory_manager.supply.dto.SupplyRequest;
import com.galandnoah.inventory_manager.supply.dto.SupplyResponse;
import com.galandnoah.inventory_manager.supply.entity.Supply;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface SupplyMapper {

    // From SupplyRequest to Supply Entity
    Supply toEntity(SupplyRequest supplyRequest);

    // From Supply Entity to SupplyResponse
    @Mapping(source = "product.id", target = "productId")
    @Mapping(source = "product.designation", target = "designation")
    SupplyResponse toResponse(Supply supply);
}
