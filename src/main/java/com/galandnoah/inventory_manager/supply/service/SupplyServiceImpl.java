package com.galandnoah.inventory_manager.supply.service;

import com.galandnoah.inventory_manager.product.dto.ProductRequest;
import com.galandnoah.inventory_manager.product.dto.ProductResponse;
import com.galandnoah.inventory_manager.product.entity.Product;
import com.galandnoah.inventory_manager.product.repository.ProductRepository;
import com.galandnoah.inventory_manager.product.service.ProductServiceImpl;
import com.galandnoah.inventory_manager.supply.dto.SupplyRequest;
import com.galandnoah.inventory_manager.supply.dto.SupplyResponse;
import com.galandnoah.inventory_manager.supply.entity.Supply;
import com.galandnoah.inventory_manager.supply.mapper.SupplyMapper;
import com.galandnoah.inventory_manager.supply.repository.SupplyRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;


@Service
@RequiredArgsConstructor
@Slf4j
public class SupplyServiceImpl implements SupplyService{
    private final SupplyRepository supplyRepository;
    private final ProductServiceImpl productService;
    private final SupplyMapper supplyMapper;
    private final ProductRepository productRepository;


    @Override
    @Transactional
    @CacheEvict(cacheNames = {
            "allProducts",
            "products",
            "supplies",
            "allSupplies"
    }, allEntries = true
    )
    public SupplyResponse createSupply(SupplyRequest supplyRequest) {

        log.info("Create supply. Product: {}", supplyRequest.designation());

        if (supplyRequest.productId() == null && supplyRequest.designation() == null)
        {
            log.warn("productId and designation are nullable");
            throw new RuntimeException("Either select product or enter designation");
        }

        if(supplyRequest.productId() != null && supplyRequest.designation() != null)
        {
            log.warn("productId and designation are both not null value");
            throw new RuntimeException("Either select product or enter designation, not both");
        }

        Supply supply = supplyMapper.toEntity(supplyRequest);

        double purchasePrice = supplyRequest.cost() / (double) supplyRequest.qty();

        purchasePrice = Double.parseDouble(String.valueOf(Math.round(purchasePrice)));

        if (supplyRequest.productId() != null){

            Integer productId = supplyRequest.productId();

            productService.incrementStock(productId, supplyRequest.qty());

            productService.setPurchasePrice(productId, purchasePrice);


            Product product = productRepository.findById(productId)
                    .orElseThrow(()-> new EntityNotFoundException("Product not found"));

            supply.setProduct(product);

        }

        if (supplyRequest.designation() != null)
        {
            ProductRequest productRequest = new ProductRequest(
                    supplyRequest.designation(),
                    supplyRequest.sellingPrice(),
                    purchasePrice,
                    supplyRequest.qty(),
                    10
            );

            ProductResponse productResponse = productService.createProduct(productRequest);

            Product product = productRepository.findById(productResponse.getId())
                    .orElseThrow(()-> new EntityNotFoundException("Product not found"));


            supply.setProduct(product);

        }

        supply.setCreated(LocalDate.now());
        supply.setMadeBy("Maman");
        Supply saved = supplyRepository.save(supply);

        log.info("Supply created with success");

        return supplyMapper.toResponse(saved);
    }


    @Override
    @Cacheable(value = "allSupplies", key = "#page")
    public Page<SupplyResponse> findAll(int size, int page) {
        log.info("Find all supplies");

        Sort sort = Sort.by("created").ascending();

        Pageable pageable = PageRequest.of(size, page, sort);

        Page<Supply> supplies = supplyRepository.findAll(pageable);

        return supplies.map(supplyMapper::toResponse);
    }

    @Override
    @Cacheable(value = "supplies", key = "#date")
    public List<SupplyResponse> findByDate(LocalDate date) {

        log.info("Find supplies by date");

        return supplyRepository.findByCreated(date)
                .stream()
                .map(supplyMapper::toResponse)
                .toList();
    }
}
