package com.galandnoah.inventory_manager.sale.entity;

import com.galandnoah.inventory_manager.product.entity.Product;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.Objects;

@Entity
@Table(name = "sales")
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
public class Sale {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;

    private Integer qty;

    private Double amount;

    private Boolean paid;

    private LocalDate created;

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Sale sale = (Sale) o;
        return Objects.equals(id, sale.id) && Objects.equals(product, sale.product) && Objects.equals(qty, sale.qty) && Objects.equals(amount, sale.amount) && Objects.equals(paid, sale.paid) && Objects.equals(created, sale.created);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, product, qty, amount, paid, created);
    }
}
