package com.galandnoah.inventory_manager.supply.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record SupplyRequest(

        String designation,

        Integer productId,

        @NotNull(message = "Veuillez renseigner la quantité")
        @Positive(message = "La quantité doit être positive")
        Integer qty,

        @NotNull(message = "Veuillez renseigner le cout total")
        @Positive(message = "Le cout total doit être  positif")
        Double cost,

        @NotNull(message = "Veuillez entrer le prix de vente")
        @Positive(message = "Le prix de vente doit être positif")
        Double sellingPrice,

        String madeBy
) {
}
