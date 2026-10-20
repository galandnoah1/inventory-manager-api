package com.galandnoah.inventory_manager.product.dto;

import jakarta.validation.constraints.Positive;
import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
public class UpdateProductRequest {
    private String designation;
    @Positive(message = "Le stock doit être positif")
    private Integer stock;
}
