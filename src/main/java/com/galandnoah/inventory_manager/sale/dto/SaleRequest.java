package com.galandnoah.inventory_manager.sale.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record SaleRequest(

        @NotNull(message = "Veuillez choisir le produit")
        Integer productId,

        @Positive(message = "La quantité doit être positive")
        Integer qty,

        @Positive(message = "Le montant doit être positif")
        Double amount,

        Boolean paid
) {
}
