package com.galandnoah.inventory_manager.supply.service;

import com.galandnoah.inventory_manager.exception.InvalidRequestException;
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
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.List;


@Service
@RequiredArgsConstructor
@Slf4j
public class SupplyServiceImpl implements SupplyService {

    // TODO: replace with the authenticated user once security is added
    private static final String DEFAULT_MADE_BY = "Maman";
    private static final int DEFAULT_THRESHOLD = 10;

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
    }, allEntries = true)
    public SupplyResponse createSupply(SupplyRequest supplyRequest) {

        log.info("Create supply. productId: {}, designation: {}",
                supplyRequest.productId(), supplyRequest.designation());

        boolean hasProduct = supplyRequest.productId() != null;
        boolean hasDesignation = StringUtils.hasText(supplyRequest.designation());

        if (!hasProduct && !hasDesignation)
        {
            log.warn("productId and designation are both missing");
            throw new InvalidRequestException("Either select product or enter designation");
        }

        if (hasProduct && hasDesignation)
        {
            log.warn("productId and designation are both provided");
            throw new InvalidRequestException("Either select product or enter designation, not both");
        }

        if (supplyRequest.qty() <= 0)
        {
            throw new InvalidRequestException("Quantity must be positive");
        }

        if (supplyRequest.cost() < 0)
        {
            throw new InvalidRequestException("Cost cannot be negative");
        }

        if (hasDesignation && (supplyRequest.sellingPrice() == null || supplyRequest.sellingPrice() < 0))
        {
            throw new InvalidRequestException("Selling price is required for a new product and cannot be negative");
        }

        double purchasePrice = (double) Math.round(supplyRequest.cost() / (double) supplyRequest.qty());

        Supply supply = supplyMapper.toEntity(supplyRequest);

        if (hasProduct)
        {
            Integer productId = supplyRequest.productId();

            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> {
                        log.warn("Product not found: {}", productId);
                        return new EntityNotFoundException("Product not found with id: " + productId);
                    });

            productService.incrementStock(productId, supplyRequest.qty());
            productService.setPurchasePrice(productId, purchasePrice);

            supply.setProduct(product);
        }
        else
        {
            ProductRequest productRequest = new ProductRequest(
                    supplyRequest.designation().trim(),
                    supplyRequest.sellingPrice(),
                    purchasePrice,
                    supplyRequest.qty(),
                    DEFAULT_THRESHOLD
            );

            // Throws DuplicateResourceException if the designation already exists
            ProductResponse productResponse = productService.createProduct(productRequest);

            Product product = productRepository.findById(productResponse.getId())
                    .orElseThrow(() -> new EntityNotFoundException(
                            "Product not found with id: " + productResponse.getId()));

            supply.setProduct(product);
        }

        supply.setCreated(LocalDate.now());
        supply.setMadeBy(DEFAULT_MADE_BY);

        Supply saved = supplyRepository.save(supply);

        log.info("Supply created with success");

        return supplyMapper.toResponse(saved);
    }


    @Override
    @Cacheable(value = "allSupplies", key = "#page + '-' + #size")
    public Page<SupplyResponse> findAll(int size, int page) {

        log.info("Find all supplies. page: {}, size: {}", page, size);

        if (page < 1)
        {
            throw new InvalidRequestException("Page must be greater than or equal to 1");
        }

        if (size < 1)
        {
            throw new InvalidRequestException("Size must be greater than or equal to 1");
        }

        Sort sort = Sort.by("created").ascending();

        // Spring Data pages are 0-based, the API is 1-based like products and sales
        Pageable pageable = PageRequest.of(page - 1, size, sort);

        return supplyRepository.findAll(pageable)
                .map(supplyMapper::toResponse);
    }

    @Override
    @Cacheable(value = "supplies", key = "#date")
    public List<SupplyResponse> findByDate(LocalDate date) {

        log.info("Find supplies by date: {}", date);

        return supplyRepository.findByCreated(date)
                .stream()
                .map(supplyMapper::toResponse)
                .toList();
    }
}