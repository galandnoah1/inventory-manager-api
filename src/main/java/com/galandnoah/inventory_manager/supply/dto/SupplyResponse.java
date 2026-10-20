package com.galandnoah.inventory_manager.supply.dto;

import java.util.Date;

public record SupplyResponse(
        String designation,
        Integer productId,
        Integer qty,
        Double cost,
        String madeBy,
        Date created
) {
}
