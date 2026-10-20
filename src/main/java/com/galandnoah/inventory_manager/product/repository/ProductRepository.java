package com.galandnoah.inventory_manager.product.repository;

import com.galandnoah.inventory_manager.product.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;


public interface ProductRepository extends JpaRepository<Product, Integer> {

    boolean existsByDesignationIgnoreCase(String designation);

    @Query(
            value = """
                SELECT  * FROM  products
                                          ORDER BY designation ASC 
                                LIMIT :size
                                OFFSET :offset
                                
                """,
            nativeQuery = true
    )
    List<Product> findProducts(@Param("size")int size, @Param("offset") int offset);
}
