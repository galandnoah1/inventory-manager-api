package com.galandnoah.inventory_manager.product.dto;

import lombok.*;

import java.util.Date;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class ProductResponse {
    private String designation;
    private String purchasePrice;
    private String sellingPrice;
    private Integer sellingQty;
    private Double totalSelling;
    private Double margin;
    private Double totalMargin;
    private Double value;
    private Integer stock;
    private Integer threshold;
    private Date created;
}
