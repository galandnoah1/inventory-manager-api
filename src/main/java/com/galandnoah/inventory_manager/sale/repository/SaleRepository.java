package com.galandnoah.inventory_manager.sale.repository;

import com.galandnoah.inventory_manager.sale.entity.Sale;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;


public interface SaleRepository extends JpaRepository<Sale, Integer> {

    @Query(
            value = """
                SELECT  * FROM sales ORDER BY created DESC 
                                LIMIT :size
                                OFFSET :offset
                """,
            nativeQuery = true
    )
    List<Sale> findSales(@Param("size")int size, @Param("offset")int offset);

    List<Sale> findByCreated(LocalDate date);
}



