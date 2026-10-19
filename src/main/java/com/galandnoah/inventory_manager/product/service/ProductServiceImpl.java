package com.galandnoah.inventory_manager.product.service;

import com.galandnoah.inventory_manager.product.dto.ProductRequest;
import com.galandnoah.inventory_manager.product.dto.ProductResponse;
import com.galandnoah.inventory_manager.product.dto.UpdateProductRequest;
import com.galandnoah.inventory_manager.product.entity.Product;
import com.galandnoah.inventory_manager.product.mapper.ProductMapper;
import com.galandnoah.inventory_manager.product.repository.ProductRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;


/**
 * Business logic related to catalog management
 * Author: Galand Noah
 * */
@Service
@AllArgsConstructor
@Slf4j
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final ProductMapper productMapper;

    @Transactional
    public ProductResponse createProduct(ProductRequest productRequest)
    {
        log.info("Add  product in catalog : {}", productRequest.designation());

        if (productRepository.existsByDesignationIgnoreCase(productRequest.designation()))
        {
            log.warn("Product already exists");
            throw new RuntimeException("Product already exists");
        }

        Product product = productMapper.toEntity(productRequest);

        Product saved = productRepository.save(product);

        log.info("Product added with success");

        return productMapper.toResponse(saved);
    }

    @Override
    public ProductResponse findProductById(Integer productId) {

        log.info("Find product : {}", productId);

        Product product = productRepository.findById(productId)
                .orElseThrow(()-> new EntityNotFoundException("Product not found"));

        return productMapper.toResponse(product);
    }

    @Override
    public Page<ProductResponse> findAllProduct(int size, int page) {

        log.info("Find all product");
        Sort sort = Sort.by("designation").ascending();
        Pageable pageable = PageRequest.of(size, page, sort);
        Page<Product> products = productRepository.findAll(pageable);

        return products.map(productMapper::toResponse);
    }

    @Override
    @Transactional
    public ProductResponse updateProduct(Integer productId, UpdateProductRequest updateProductRequest) {

        log.info("Update product");

        Product product = productRepository.findById(productId)
                .orElseThrow(()-> new EntityNotFoundException("Product not found"));

        if (updateProductRequest.getStock() != null)
        {
            log.info("Update stock");
            product.setStock(updateProductRequest.getStock());
        }

        if (updateProductRequest.getDesignation() != null)
        {
            log.info("Change designation");
            product.setDesignation(updateProductRequest.getDesignation());
        }

        Product saved = productRepository.save(product);

        return productMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public void incrementStock(Integer productId, int qty) {
        Product product = productRepository.findById(productId)
                .orElseThrow(()-> new EntityNotFoundException("Product not found"));

        log.info("Increment stock : +{}, qty : {}", product.getStock(), qty);


        if (qty <= 0)
        {
            throw new IllegalArgumentException("Quantity must be positive");
        }


        product.setStock(product.getStock() + qty);

        Product saved = productRepository.save(product);

        log.info("Stock incremented with success. stock: {}", saved.getStock() );
    }

    @Override
    @Transactional
    public void decrementStock(Integer productId, int qty) {

        Product product = productRepository.findById(productId)
                .orElseThrow(()-> new EntityNotFoundException("Product not found"));

        log.info("Decrement stock : +{}, qty : {}", product.getStock(), qty);

        if (qty <= 0)
        {
            throw new IllegalArgumentException("Quantity must be positive");
        }


        if(qty > product.getStock())
        {
            log.warn("Insufficient stock");
            throw new RuntimeException("Insufficient stock");
        }

        product.setStock(product.getStock() + qty);

        Product saved = productRepository.save(product);

        log.info("Stock decremented with success. stock: {}", saved.getStock() );
    }

    @Override
    @Transactional
    public void updateProductPurchasePrice(Integer productId, Double price) {
        Product product = productRepository.findById(productId)
                .orElseThrow(()-> new EntityNotFoundException("Product not found"));

        log.info("Update product purchase price: {}", product.getDesignation());

        if (price < 0)
        {
            log.warn("Negative price");
            throw new IllegalArgumentException("Price must be positive");
        }

        product.setPurchasePrice(price);

        Product saved = productRepository.save(product);

        log.info("Purchase price updated with success: {}", saved.getPurchasePrice());
    }


}
