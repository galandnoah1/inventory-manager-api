package com.galandnoah.inventory_manager.sale.service;

import com.galandnoah.inventory_manager.exception.InvalidRequestException;
import com.galandnoah.inventory_manager.product.entity.Product;
import com.galandnoah.inventory_manager.product.repository.ProductRepository;
import com.galandnoah.inventory_manager.product.service.ProductServiceImpl;
import com.galandnoah.inventory_manager.sale.dto.SaleRequest;
import com.galandnoah.inventory_manager.sale.dto.SaleResponse;
import com.galandnoah.inventory_manager.sale.entity.Sale;
import com.galandnoah.inventory_manager.sale.mapper.SaleMapper;
import com.galandnoah.inventory_manager.sale.repository.SaleRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class SaleServiceImpl implements SaleService {

    private final SaleRepository saleRepository;
    private final ProductServiceImpl productService;
    private final ProductRepository productRepository;
    private final SaleMapper saleMapper;


    @Override
    @Transactional
    @CacheEvict(cacheNames = {"products", "allProducts", "sales", "allSales"}, allEntries = true)
    public SaleResponse createSale(SaleRequest saleRequest) {

        log.info("Create sale. product: {}, qty: {}", saleRequest.productId(), saleRequest.qty());

        if (saleRequest.productId() == null)
        {
            throw new InvalidRequestException("Product is required");
        }

        if (saleRequest.amount() != null && saleRequest.amount() < 0)
        {
            throw new InvalidRequestException("Amount cannot be negative");
        }

        Product product = productRepository.findById(saleRequest.productId())
                .orElseThrow(() -> {
                    log.warn("Product not found: {}", saleRequest.productId());
                    return new EntityNotFoundException(
                            "Product not found with id: " + saleRequest.productId());
                });

        // Throws InvalidRequestException if qty <= 0
        // and InsufficientStockException if qty > available stock
        productService.decrementStock(product.getId(), saleRequest.qty());
        productService.incrementSellingQty(product.getId(), saleRequest.qty());

        Sale sale = saleMapper.toEntity(saleRequest);
        sale.setProduct(product);

        // Custom amount if provided, otherwise qty x selling price
        double amount = saleRequest.amount() != null
                ? saleRequest.amount()
                : saleRequest.qty() * product.getSellingPrice();

        sale.setAmount(amount);
        sale.setPaid(Boolean.TRUE.equals(saleRequest.paid()));
        sale.setCreated(LocalDate.now());

        Sale saved = saleRepository.save(sale);

        log.info("Sale created with success");

        return saleMapper.toResponse(saved);
    }

    @Override
    @Cacheable(value = "allSales", key = "#page + '-' + #size")
    @Transactional
    public List<SaleResponse> findSales(int size, int page) {

        log.info("Find sales with pagination. page: {}, size: {}", page, size);

        if (page < 1)
        {
            throw new InvalidRequestException("Page must be greater than or equal to 1");
        }

        if (size < 1)
        {
            throw new InvalidRequestException("Size must be greater than or equal to 1");
        }

        int offset = (page - 1) * size;

        return saleRepository.findSales(size, offset)
                .stream()
                .map(saleMapper::toResponse)
                .toList();
    }

    @Override
    @Cacheable(value = "sales", key = "#date")
    @Transactional
    public List<SaleResponse> findSalesByDate(LocalDate date) {

        log.info("Find sales by date: {}", date);

        return saleRepository.findByCreated(date)
                .stream()
                .map(saleMapper::toResponse)
                .toList();
    }
}