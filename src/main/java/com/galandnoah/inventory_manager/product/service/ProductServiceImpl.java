package com.galandnoah.inventory_manager.product.service;

import com.galandnoah.inventory_manager.exception.DuplicateResourceException;
import com.galandnoah.inventory_manager.exception.InsufficientStockException;
import com.galandnoah.inventory_manager.exception.InvalidRequestException;
import com.galandnoah.inventory_manager.product.dto.ProductRequest;
import com.galandnoah.inventory_manager.product.dto.ProductResponse;
import com.galandnoah.inventory_manager.product.dto.UpdateProductRequest;
import com.galandnoah.inventory_manager.product.entity.Product;
import com.galandnoah.inventory_manager.product.mapper.ProductMapper;
import com.galandnoah.inventory_manager.product.repository.ProductRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;


/**
 * Business logic related to catalog management
 * Author: Galand Noah
 * */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final ProductMapper productMapper;

    @Transactional
    @CacheEvict(cacheNames = {"products", "allProducts"}, allEntries = true)
    public ProductResponse createProduct(ProductRequest productRequest)
    {
        log.info("Add product in catalog : {}", productRequest.designation());

        if (productRepository.existsByDesignationIgnoreCase(productRequest.designation()))
        {
            log.warn("Product already exists: {}", productRequest.designation());
            throw new DuplicateResourceException(
                    "Product already exists: " + productRequest.designation());
        }

        Product product = productMapper.toEntity(productRequest);
        product.setCreated(LocalDate.now());
        product.setUpdated(LocalDate.now());
        product.setSellingQty(0);

        Product saved = productRepository.save(product);

        log.info("Product added with success");

        ProductResponse response = productMapper.toResponse(saved);
        response.setId(saved.getId());
        accounting(response);

        return response;
    }

    @Override
    @Cacheable(value = "products", key = "#productId")
    public ProductResponse findProductById(Integer productId) {

        log.info("Find product : {}", productId);

        Product product = getProductOrThrow(productId);

        ProductResponse productResponse = productMapper.toResponse(product);

        accounting(productResponse);

        return productResponse;
    }

    @Override
    @Cacheable(value = "allProducts", key = "#page + '-' + #size")
    public List<ProductResponse> findAllProduct(int size, int page) {

        log.info("Find all product. page: {}, size: {}", page, size);

        if (page < 1)
        {
            throw new InvalidRequestException("Page must be greater than or equal to 1");
        }

        if (size < 1)
        {
            throw new InvalidRequestException("Size must be greater than or equal to 1");
        }

        int offset = (page - 1) * size;

        List<Product> products = productRepository.findProducts(size, offset);

        return accounting(products);
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = {"products", "allProducts"}, allEntries = true)
    public ProductResponse updateProduct(Integer productId, UpdateProductRequest updateProductRequest) {

        log.info("Update product : {}", productId);

        Product product = getProductOrThrow(productId);

        if (updateProductRequest.getStock() != null)
        {
            if (updateProductRequest.getStock() < 0)
            {
                throw new InvalidRequestException("Stock cannot be negative");
            }

            log.info("Update stock");
            product.setStock(updateProductRequest.getStock());
        }

        if (updateProductRequest.getDesignation() != null)
        {
            String designation = updateProductRequest.getDesignation().trim();

            if (designation.isEmpty())
            {
                throw new InvalidRequestException("Designation cannot be blank");
            }

            boolean designationChanged = !designation.equalsIgnoreCase(product.getDesignation());

            if (designationChanged && productRepository.existsByDesignationIgnoreCase(designation))
            {
                log.warn("Product already exists: {}", designation);
                throw new DuplicateResourceException("Product already exists: " + designation);
            }

            log.info("Change designation");
            product.setDesignation(designation);
        }

        product.setUpdated(LocalDate.now());

        Product saved = productRepository.save(product);

        ProductResponse response = productMapper.toResponse(saved);
        accounting(response);

        return response;
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = {"products", "allProducts"}, allEntries = true)
    public void incrementStock(Integer productId, int qty) {

        log.info("Increment stock. product: {}, qty: +{}", productId, qty);

        validateQuantity(qty);

        Product product = getProductOrThrow(productId);

        product.setStock(product.getStock() + qty);
        product.setUpdated(LocalDate.now());

        Product saved = productRepository.save(product);

        log.info("Stock incremented with success. stock: {}", saved.getStock());
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = {"products", "allProducts"}, allEntries = true)
    public void decrementStock(Integer productId, int qty) {

        log.info("Decrement stock. product: {}, qty: -{}", productId, qty);

        validateQuantity(qty);

        Product product = getProductOrThrow(productId);

        if (qty > product.getStock())
        {
            log.warn("Insufficient stock. product: {}, available: {}, requested: {}",
                    productId, product.getStock(), qty);
            throw new InsufficientStockException(
                    "Insufficient stock for product '" + product.getDesignation()
                            + "': available " + product.getStock() + ", requested " + qty);
        }

        product.setStock(product.getStock() - qty);
        product.setUpdated(LocalDate.now());

        Product saved = productRepository.save(product);

        log.info("Stock decremented with success. stock: {}", saved.getStock());
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = {"products", "allProducts"}, allEntries = true)
    public void setPurchasePrice(Integer productId, Double price) {

        if (price == null || price < 0)
        {
            log.warn("Invalid price: {}", price);
            throw new InvalidRequestException("Price cannot be null or negative");
        }

        Product product = getProductOrThrow(productId);

        log.info("Update product purchase price: {}", product.getDesignation());

        product.setPurchasePrice(price);
        product.setUpdated(LocalDate.now());

        Product saved = productRepository.save(product);

        log.info("Purchase price updated with success: {}", saved.getPurchasePrice());
    }

    @Transactional
    @CacheEvict(cacheNames = {"products", "allProducts"}, allEntries = true)
    public void incrementSellingQty(Integer productId, int qty)
    {
        log.info("Increment selling qty. product: {}, qty: +{}", productId, qty);

        validateQuantity(qty);

        Product product = getProductOrThrow(productId);

        int oldSellingQty = product.getSellingQty() != null ? product.getSellingQty() : 0;

        product.setSellingQty(oldSellingQty + qty);
        product.setUpdated(LocalDate.now());

        productRepository.save(product);
    }


    public List<ProductResponse> accounting(List<Product> products)
    {
        List<ProductResponse> productResponses = products
                .stream()
                .map(productMapper::toResponse)
                .toList();

        productResponses.forEach(this::accounting);

        return productResponses;
    }

    public void accounting(ProductResponse productResponse)
    {
        productResponse.setMargin(productResponse.getSellingPrice() - productResponse.getPurchasePrice());
        productResponse.setTotalMargin(productResponse.getMargin() * productResponse.getSellingQty());
        productResponse.setTotalSelling(productResponse.getSellingPrice() * productResponse.getSellingQty());
        productResponse.setValue(productResponse.getStock() * productResponse.getPurchasePrice());
    }

    // ---------- helpers ----------

    private Product getProductOrThrow(Integer productId)
    {
        return productRepository.findById(productId)
                .orElseThrow(() -> {
                    log.warn("Product not found: {}", productId);
                    return new EntityNotFoundException("Product not found with id: " + productId);
                });
    }

    private void validateQuantity(int qty)
    {
        if (qty <= 0)
        {
            log.warn("Invalid quantity: {}", qty);
            throw new InvalidRequestException("Quantity must be positive");
        }
    }
}