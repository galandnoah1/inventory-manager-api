package com.galandnoah.inventory_manager.supply.controller;

import com.galandnoah.inventory_manager.supply.dto.SupplyRequest;
import com.galandnoah.inventory_manager.supply.dto.SupplyResponse;
import com.galandnoah.inventory_manager.supply.service.SupplyServiceImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/supplies")
@Slf4j
@RequiredArgsConstructor
@Tag(name = "Supplies management API")
public class SupplyController {
    private final SupplyServiceImpl supplyService;

    @PostMapping
    @Operation(description = "Add supply")
    public ResponseEntity<SupplyResponse> create(
            @Valid @RequestBody SupplyRequest supplyRequest
            ){

        log.info("POST /api/v1/supplies");

        SupplyResponse supplyResponse = supplyService.createSupply(supplyRequest);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(supplyResponse);
    }

    @GetMapping
    @Operation(description = "Find all supplies")
    public ResponseEntity<Page<SupplyResponse>> findAll(
            @RequestParam("size") int size, @RequestParam("page") int page){

        log.info("GET /api/v1/supplies?size={}&page={}", size, page);

        Page<SupplyResponse> supplies = supplyService.findAll(size, page);

        return ResponseEntity.ok(supplies);
    }

    @GetMapping("/{date}")
    @Operation(description = "Find by date")
    public ResponseEntity<List<SupplyResponse>> findByDate(
            @PathVariable String date
            )
    {

        log.info("GET /api/v1/supplies/{}", date);

        List<SupplyResponse> supplies = supplyService.findByDate(LocalDate.parse(date));

        return ResponseEntity.ok(supplies);
    }

}
