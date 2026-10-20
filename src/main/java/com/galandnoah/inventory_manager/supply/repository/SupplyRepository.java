package com.galandnoah.inventory_manager.supply.repository;

import com.galandnoah.inventory_manager.supply.entity.Supply;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface SupplyRepository extends JpaRepository<Supply, Integer> {

    List<Supply> findByCreated(LocalDate created);
}
