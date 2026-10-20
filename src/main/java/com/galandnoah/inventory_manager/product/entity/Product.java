package com.galandnoah.inventory_manager.product.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.Objects;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "products")
@Builder
@Getter
@Setter
public class Product {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    private String designation;
    private Double sellingPrice;
    private Double purchasePrice;
    private Integer sellingQty;
    private Integer stock;
    private Integer threshold;
    private LocalDate created;
    private LocalDate updated;

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Product product = (Product) o;
        return Objects.equals(id, product.id) && Objects.equals(designation, product.designation);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, designation);
    }
}
