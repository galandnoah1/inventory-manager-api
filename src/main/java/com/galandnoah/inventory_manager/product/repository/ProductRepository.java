package com.galandnoah.inventory_manager.product.repository;

import com.galandnoah.inventory_manager.product.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Integer> {

    boolean existsByDesignationIgnoreCase(String designation);
}
