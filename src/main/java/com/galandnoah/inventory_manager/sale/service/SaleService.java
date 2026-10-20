package com.galandnoah.inventory_manager.sale.service;

import com.galandnoah.inventory_manager.sale.dto.SaleRequest;
import com.galandnoah.inventory_manager.sale.dto.SaleResponse;

import java.time.LocalDate;
import java.util.List;

public interface SaleService {

    SaleResponse createSale(SaleRequest saleRequest);

    List<SaleResponse> findSales(int size, int page);

    List<SaleResponse> findSalesByDate(LocalDate date);

}
