package com.galandnoah.inventory_manager.sale.service;

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
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class SaleServiceImpl implements SaleService{
    private final SaleRepository saleRepository;
    private final ProductServiceImpl productService;
    private final ProductRepository productRepository;
    private final SaleMapper saleMapper;


    @Override
    @Transactional
    @CacheEvict(cacheNames = {"products", "allProducts", "sales", "allSales"}, allEntries = true)
    public SaleResponse createSale(SaleRequest saleRequest) {

        log.info("Create sale");

        Sale sale = saleMapper.toEntity(saleRequest);

        Product product = productRepository.findById(saleRequest.productId())
                .orElseThrow(()-> new EntityNotFoundException("Product not found"));

        sale.setProduct(product);

        if (product.getStock() <= saleRequest.qty() || product.getStock() == 0)
        {
            log.warn("Stock insuffisant");
            throw new RuntimeException("Insufficient stock");
        }

        if (saleRequest.amount() == null){

            productService.decrementStock(product.getId(), saleRequest.qty());
            productService.incrementSellingQty(product.getId(), saleRequest.qty());
            sale.setAmount(saleRequest.qty() * product.getSellingPrice());
        }

        sale.setPaid(saleRequest.paid());
        sale.setCreated(LocalDate.now());

        Sale saved = saleRepository.save(sale);

        log.info("Sale created with success");

        return saleMapper.toResponse(saved);
    }

    @Override
    @Cacheable(value = "allSales", key = "#page")
    @Transactional
    public List<SaleResponse> findSales(int size, int page) {

        log.info("Find sales with pagination");
        int offset = (page - 1)*size;

        List<Sale> sales = saleRepository.findSales(size, offset);


        return sales
                .stream()
                .map(saleMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Cacheable(value = "sales", key = "#date")
    @Transactional
    public List<SaleResponse> findSalesByDate(LocalDate date) {

        log.info("Find sales by date");

        List<Sale> sales = saleRepository.findByCreated(date);

        return sales
                .stream()
                .map(saleMapper::toResponse)
                .collect(Collectors.toList());
    }
}
