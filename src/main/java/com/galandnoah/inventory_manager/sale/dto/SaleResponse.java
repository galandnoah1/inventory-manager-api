package com.galandnoah.inventory_manager.sale.dto;

import java.time.LocalDate;

public record SaleResponse(
        Integer id,
        String designation,
        Double amount,
        Integer qty,
        Boolean paid,
        LocalDate created
) {
}
