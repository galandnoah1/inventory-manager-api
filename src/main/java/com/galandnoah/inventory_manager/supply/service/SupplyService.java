package com.galandnoah.inventory_manager.supply.service;

import com.galandnoah.inventory_manager.supply.dto.SupplyRequest;
import com.galandnoah.inventory_manager.supply.dto.SupplyResponse;
import org.springframework.data.domain.Page;

import java.time.LocalDate;
import java.util.List;

public interface SupplyService {

    SupplyResponse createSupply(SupplyRequest supplyRequest);

    Page<SupplyResponse> findAll(int size, int page);

    List<SupplyResponse> findByDate(LocalDate date);
}
