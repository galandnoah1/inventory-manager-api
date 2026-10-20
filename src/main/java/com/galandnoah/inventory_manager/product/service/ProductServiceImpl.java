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
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;


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
    @CacheEvict(
            cacheNames = {"products", "allProducts"},
            allEntries = true
    )
    public ProductResponse createProduct(ProductRequest productRequest)
    {
        log.info("Add  product in catalog : {}", productRequest.designation());

        if (productRepository.existsByDesignationIgnoreCase(productRequest.designation()))
        {
            log.warn("Product already exists");
            throw new RuntimeException("Product already exists");
        }

        Product product = productMapper.toEntity(productRequest);
        product.setCreated(LocalDate.now());
        product.setSellingQty(0);
        product.setUpdated(LocalDate.now());

        Product saved = productRepository.save(product);

        log.info("Product added with success");

        ProductResponse response =  productMapper.toResponse(saved);
        response.setId(saved.getId());
        Map<String , Double> margins = calculateMargin(saved);

        response.setMargin(margins.get("margin"));
        response.setTotalMargin(margins.get("totalMargin"));
        response.setTotalSelling(response.getSellingQty() * response.getSellingPrice());
        response.setValue(response.getStock() * response.getPurchasePrice());

        return response;
    }

    private Map<String, Double> calculateMargin(Product product)
    {
        Double sellingPrice = product.getSellingPrice();
        Double purchasePrice = product.getPurchasePrice();
        Double sellingQty = product.getSellingQty() != null ? product.getSellingQty() : 0.0;
        Double margin = sellingPrice - purchasePrice;
        Double totalMargin = sellingQty * margin;

        return Map.of(
                "margin", margin,
                "totalMargin", totalMargin
        );
    }

    @Override
    @Cacheable(value = "products", key = "#productId")
    public ProductResponse findProductById(Integer productId) {

        log.info("Find product : {}", productId);

        Product product = productRepository.findById(productId)
                .orElseThrow(()-> new EntityNotFoundException("Product not found"));

        ProductResponse productResponse = productMapper.toResponse(product);

        productResponse.setMargin(productResponse.getSellingPrice() - productResponse.getPurchasePrice());
        productResponse.setTotalMargin(productResponse.getMargin() * productResponse.getSellingQty());
        productResponse.setTotalSelling(productResponse.getSellingPrice() * productResponse.getSellingQty());
        productResponse.setValue(productResponse.getStock() * productResponse.getPurchasePrice());

        return productResponse;
    }

    @Override
    @Cacheable(value = "allProducts", key = "#page")
    public List<ProductResponse> findAllProduct(int size, int page) {

        log.info("Find all product");

        int offset = (page - 1 ) * size;

        List<Product> products = productRepository.findProducts(size, offset);

        List<ProductResponse> productResponses = products
                .stream()
                .map(productMapper::toResponse)
                .toList();

        productResponses.forEach(p -> {
            p.setMargin(p.getSellingPrice() - p.getPurchasePrice());
            p.setTotalMargin(p.getMargin()*p.getSellingQty());
            p.setTotalSelling(p.getSellingQty()*p.getSellingPrice());
            p.setValue(p.getStock()*p.getPurchasePrice());
        });
        return productResponses;
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = {"products", "allProducts"}, allEntries = true)
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

        product.setStock(product.getStock() - qty);

        Product saved = productRepository.save(product);

        log.info("Stock decremented with success. stock: {}", saved.getStock() );
    }

    @Override
    @Transactional
    public void setPurchasePrice(Integer productId, Double price) {
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

    @Transactional
    public void incrementSellingQty(Integer productId, int qty)
    {
        log.info("Increment selling qty");

        Product product = productRepository.findById(productId)
                .orElseThrow(()-> new EntityNotFoundException("Product not found"));

        int oldSellingQty = product.getSellingQty();

        int newSellingQty = oldSellingQty + qty;

        product.setSellingQty(newSellingQty);

        productRepository.save(product);
    }


}
