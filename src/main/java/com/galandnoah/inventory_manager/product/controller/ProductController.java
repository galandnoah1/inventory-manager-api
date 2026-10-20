package com.galandnoah.inventory_manager.product.controller;

import com.galandnoah.inventory_manager.product.dto.ProductRequest;
import com.galandnoah.inventory_manager.product.dto.ProductResponse;
import com.galandnoah.inventory_manager.product.dto.UpdateProductRequest;
import com.galandnoah.inventory_manager.product.service.ProductServiceImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/products")
@AllArgsConstructor
@Slf4j
@Tag(name = "Catalog management API")
public class ProductController {
    private final ProductServiceImpl productService;


    @PostMapping
    @Operation(description = "Add product in catalog")
    public ResponseEntity<ProductResponse> create(
            @Valid @RequestBody ProductRequest productRequest
            )
    {
        log.info("POST /api/v1/products");

        ProductResponse response = productService.createProduct(productRequest);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping("/{id}")
    @Operation(description = "Find product by id")
    public ResponseEntity<ProductResponse> findById(
            @PathVariable Integer id
    )
    {
        log.info("GET /api/v1/products/{}", id);

        ProductResponse response = productService.findProductById(id);

        return ResponseEntity.ok(response);
    }

    @GetMapping
    @Operation(description = "Find all product")
    public ResponseEntity<List<ProductResponse>> findAll(int size, int page)
    {
        log.info("GET /api/v1/products");

        List<ProductResponse> responses = productService.findAllProduct(size, page);

        return ResponseEntity.ok(responses);
    }

    @PutMapping("/{id}")
    @Operation(description = "Update product")
    public ResponseEntity<ProductResponse> update(
            @PathVariable Integer id,
            @Valid @RequestBody UpdateProductRequest updateProductRequest
            ){
        log.info("PUT /api/v1/products/{}", id);

        ProductResponse response = productService.updateProduct(id,updateProductRequest);

        return ResponseEntity.ok(response);
    }
}
