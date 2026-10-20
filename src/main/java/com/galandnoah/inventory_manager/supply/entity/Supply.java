package com.galandnoah.inventory_manager.supply.entity;

import com.galandnoah.inventory_manager.product.entity.Product;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.Objects;

@Entity
@Table(name = "supplies")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class Supply {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "productId")
    private Product product;

    private Double cost;

    private Integer qty;

    private String madeBy;

    private LocalDate created;

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Supply supply = (Supply) o;
        return Objects.equals(id, supply.id) && Objects.equals(product, supply.product) && Objects.equals(cost, supply.cost) && Objects.equals(qty, supply.qty) && Objects.equals(madeBy, supply.madeBy);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, product, cost, qty, madeBy);
    }
}
