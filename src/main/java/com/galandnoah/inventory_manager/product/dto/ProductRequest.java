package com.galandnoah.inventory_manager.product.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ProductRequest(
        @NotBlank(message = "Entrer le nom du produit")
        String designation,

        @NotNull(message = "Entrer le prix de vente du produit")
        @Positive(message = "Le prix de vente doit être positif")
        Double sellingPrice,

        @NotNull(message = "Entrer la quantité de produit disponible")
        @Positive(message = "La valeur du stock doit être positif")
        Integer stock,

        Integer threshold
) {
}
