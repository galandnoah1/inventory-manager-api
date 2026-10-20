package com.galandnoah.inventory_manager.sale.controller;

import com.galandnoah.inventory_manager.sale.dto.SaleRequest;
import com.galandnoah.inventory_manager.sale.dto.SaleResponse;
import com.galandnoah.inventory_manager.sale.service.SaleServiceImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.repository.query.Param;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/sales")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Sale's management API")
public class SaleController {
    private final SaleServiceImpl saleService;

    @PostMapping
    @Operation(description = "Save sale")
    public ResponseEntity<SaleResponse> create(
            @Valid @RequestBody SaleRequest saleRequest
            ){

        log.info("POST /api/v1/sales - create sale");

        SaleResponse saleResponse = saleService.createSale(saleRequest);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(saleResponse);
    }

    @GetMapping
    @Operation(description = "Retrieve all sales")
    public ResponseEntity<List<SaleResponse>> findAll(
            @Param("size") int size, @Param("page") int page
    )
    {
        log.info("GET /api/v1/sales?size={}&page={}", size, page);

        List<SaleResponse> sales = saleService.findSales(size, page);

        return ResponseEntity.ok(sales);
    }

    @GetMapping("/{date}")
    @Operation(description = "Retrieve all sales")
    public ResponseEntity<List<SaleResponse>> findByDate(
           @PathVariable String date
    )
    {
        log.info("GET /api/v1/sales/{}", date);

        List<SaleResponse> sales = saleService.findSalesByDate(LocalDate.parse(date));

        return ResponseEntity.ok(sales);
    }
}
