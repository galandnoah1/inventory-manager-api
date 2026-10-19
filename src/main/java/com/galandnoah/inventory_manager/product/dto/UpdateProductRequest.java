package com.galandnoah.inventory_manager.product.dto;

import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
public class UpdateProductRequest {
    private String designation;
    private Integer stock;
}
