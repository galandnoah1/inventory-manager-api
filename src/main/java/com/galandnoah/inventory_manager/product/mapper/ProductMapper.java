package com.galandnoah.inventory_manager.product.mapper;

import com.galandnoah.inventory_manager.product.dto.ProductRequest;
import com.galandnoah.inventory_manager.product.dto.ProductResponse;
import com.galandnoah.inventory_manager.product.entity.Product;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.stereotype.Component;

@Mapper(componentModel = "spring")
@Component
public interface ProductMapper {

    //  From ProductRequest to Product Entity
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "purchasePrice", ignore = true)
    @Mapping(target = "sellingQty", ignore = true)
    @Mapping(target = "created", ignore = true)
    @Mapping(target = "updated", ignore = true)
    Product toEntity(ProductRequest productRequest);

    // From Product Entity to ProductResponse

    @Mapping(target = "sellingQty", ignore = true)
    @Mapping(target = "totalSelling", ignore = true)
    @Mapping(target = "margin", ignore = true)
    @Mapping(target = "totalMargin", ignore = true)
    @Mapping(target = "value", ignore = true)
    ProductResponse toResponse(Product product);
}
