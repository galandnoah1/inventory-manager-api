package com.galandnoah.inventory_manager.product.service;

import com.galandnoah.inventory_manager.product.dto.ProductRequest;
import com.galandnoah.inventory_manager.product.dto.ProductResponse;
import com.galandnoah.inventory_manager.product.dto.UpdateProductRequest;


import java.util.List;

public interface ProductService {

    ProductResponse createProduct(ProductRequest productRequest);

    ProductResponse findProductById(Integer productId);

    List<ProductResponse> findAllProduct(int size, int page);

    ProductResponse updateProduct(Integer productId, UpdateProductRequest updateProductRequest);

    void incrementStock(Integer productId, int qty);

    void decrementStock(Integer productId, int qty);

    void setPurchasePrice(Integer productId, Double price);
}
