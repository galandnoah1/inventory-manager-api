package com.galandnoah.inventory_manager.product.service;

import com.galandnoah.inventory_manager.exception.DuplicateResourceException;
import com.galandnoah.inventory_manager.exception.InsufficientStockException;
import com.galandnoah.inventory_manager.exception.InvalidRequestException;
import com.galandnoah.inventory_manager.product.dto.ProductRequest;
import com.galandnoah.inventory_manager.product.dto.ProductResponse;
import com.galandnoah.inventory_manager.product.entity.Product;
import com.galandnoah.inventory_manager.product.mapper.ProductMapper;
import com.galandnoah.inventory_manager.product.repository.ProductRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceImplTest {


    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductMapper productMapper;

    @InjectMocks
    private ProductServiceImpl productService;

    //-----helpers-----
    private static Product product(Integer id, String designation, double sellingPrice,
                                   double purchasePrice, int stock, Integer sellingQty) {
        return Product.builder()
                .id(id)
                .designation(designation)
                .sellingPrice(sellingPrice)
                .purchasePrice(purchasePrice)
                .stock(stock)
                .sellingQty(sellingQty)
                .threshold(5)
                .build();
    }

    private static ProductResponse toResponse(Product p) {
        ProductResponse response = new ProductResponse();
        response.setId(p.getId());
        response.setSellingPrice(p.getSellingPrice());
        response.setPurchasePrice(p.getPurchasePrice());
        response.setSellingQty(p.getSellingQty());
        response.setStock(p.getStock());
        return response;
    }

    @Test
    void createProduct_shouldThrowDuplicate_whenDesignationAlreadyExists() {
        ProductRequest request = new ProductRequest("Savon", 500.0, 350.0, 20, 5);
        when(productRepository.existsByDesignationIgnoreCase("Savon")).thenReturn(true);

        assertThatThrownBy(() -> productService.createProduct(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("Savon");

        verify(productRepository, never()).save(any());
    }

    @Test
    void createProduct_shouldSaveProductAndComputeAccounting() {
        ProductRequest request = new ProductRequest("Savon", 500.0, 350.0, 20, 5);
        Product entity = Product.builder()
                .designation("Savon").sellingPrice(500.0).purchasePrice(350.0).stock(20).build();

        when(productRepository.existsByDesignationIgnoreCase("Savon")).thenReturn(false);
        when(productMapper.toEntity(request)).thenReturn(entity);
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> {
            Product p = inv.getArgument(0);
            p.setId(1);
            return p;
        });
        when(productMapper.toResponse(any(Product.class))).thenAnswer(inv -> toResponse(inv.getArgument(0)));

        ProductResponse result = productService.createProduct(request);

        assertThat(entity.getSellingQty()).isZero();
        assertThat(entity.getCreated()).isEqualTo(LocalDate.now());
        assertThat(entity.getUpdated()).isEqualTo(LocalDate.now());
        assertThat(result.getId()).isEqualTo(1);
        assertThat(result.getMargin()).isEqualTo(150.0);
        assertThat(result.getTotalMargin()).isEqualTo(0.0);
        assertThat(result.getTotalSelling()).isEqualTo(0.0);
        assertThat(result.getValue()).isEqualTo(7000.0);
    }

    @Test
    void findProductById_shouldThrowNotFound_whenProductDoesNotExist() {
        when(productRepository.findById(42)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.findProductById(42))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("42");
    }

    @Test
    void findProductById_shouldComputeAccounting() {
        Product product = product(1, "Savon", 500.0, 350.0, 20, 4);
        when(productRepository.findById(1)).thenReturn(Optional.of(product));
        when(productMapper.toResponse(product)).thenReturn(toResponse(product));

        ProductResponse result = productService.findProductById(1);

        assertThat(result.getMargin()).isEqualTo(150.0);
        assertThat(result.getTotalMargin()).isEqualTo(600.0);
        assertThat(result.getTotalSelling()).isEqualTo(2000.0);
        assertThat(result.getValue()).isEqualTo(7000.0);
    }

    @Test
    void findAllProduct_shouldRejectPageLowerThanOne() {
        assertThatThrownBy(() -> productService.findAllProduct(10, 0))
                .isInstanceOf(InvalidRequestException.class);

        verifyNoInteractions(productRepository);
    }

    @Test
    void findAllProduct_shouldRejectSizeLowerThanOne() {
        assertThatThrownBy(() -> productService.findAllProduct(0, 1))
                .isInstanceOf(InvalidRequestException.class);

        verifyNoInteractions(productRepository);
    }

    @Test
    void findAllProduct_shouldComputeOffsetFromPage() {
        Product product = product(1, "Savon", 500.0, 350.0, 20, 0);
        when(productRepository.findProducts(10, 10)).thenReturn(List.of(product));
        when(productMapper.toResponse(product)).thenReturn(toResponse(product));

        List<ProductResponse> result = productService.findAllProduct(10, 2);

        assertThat(result).hasSize(1);
        verify(productRepository).findProducts(10, 10);
    }


    @Test
    void incrementStock_shouldRejectNonPositiveQuantity() {
        assertThatThrownBy(() -> productService.incrementStock(1, 0))
                .isInstanceOf(InvalidRequestException.class);

        verifyNoInteractions(productRepository);
    }

    @Test
    void incrementStock_shouldThrowNotFound_whenProductDoesNotExist() {
        when(productRepository.findById(1)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.incrementStock(1, 5))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void incrementStock_shouldAddQuantityToStock() {
        Product product = product(1, "Savon", 500.0, 350.0, 10, 0);
        when(productRepository.findById(1)).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

        productService.incrementStock(1, 5);

        assertThat(product.getStock()).isEqualTo(15);
        verify(productRepository).save(product);
    }


    @Test
    void decrementStock_shouldRejectNonPositiveQuantity() {
        assertThatThrownBy(() -> productService.decrementStock(1, -2))
                .isInstanceOf(InvalidRequestException.class);

        verifyNoInteractions(productRepository);
    }

    @Test
    void decrementStock_shouldThrowInsufficientStock_whenQuantityExceedsStock() {
        Product product = product(1, "Savon", 500.0, 350.0, 3, 0);
        when(productRepository.findById(1)).thenReturn(Optional.of(product));

        assertThatThrownBy(() -> productService.decrementStock(1, 5))
                .isInstanceOf(InsufficientStockException.class)
                .hasMessageContaining("available 3")
                .hasMessageContaining("requested 5");

        assertThat(product.getStock()).isEqualTo(3);
        verify(productRepository, never()).save(any());
    }

    @Test
    void decrementStock_shouldAllowSellingTheWholeStock() {
        Product product = product(1, "Savon", 500.0, 350.0, 5, 0);
        when(productRepository.findById(1)).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

        productService.decrementStock(1, 5);

        assertThat(product.getStock()).isZero();
    }


    @Test
    void setPurchasePrice_shouldRejectNullPrice() {
        assertThatThrownBy(() -> productService.setPurchasePrice(1, null))
                .isInstanceOf(InvalidRequestException.class);

        verifyNoInteractions(productRepository);
    }

    @Test
    void setPurchasePrice_shouldRejectNegativePrice() {
        assertThatThrownBy(() -> productService.setPurchasePrice(1, -10.0))
                .isInstanceOf(InvalidRequestException.class);

        verifyNoInteractions(productRepository);
    }

    @Test
    void setPurchasePrice_shouldUpdatePrice() {
        Product product = product(1, "Savon", 500.0, 350.0, 5, 0);
        when(productRepository.findById(1)).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

        productService.setPurchasePrice(1, 400.0);

        assertThat(product.getPurchasePrice()).isEqualTo(400.0);
        assertThat(product.getUpdated()).isEqualTo(LocalDate.now());
    }

    @Test
    void incrementSellingQty_shouldRejectNonPositiveQuantity() {
        assertThatThrownBy(() -> productService.incrementSellingQty(1, 0))
                .isInstanceOf(InvalidRequestException.class);

        verifyNoInteractions(productRepository);
    }

    @Test
    void incrementSellingQty_shouldTreatNullSellingQtyAsZero() {
        Product product = product(1, "Savon", 500.0, 350.0, 5, null);
        when(productRepository.findById(1)).thenReturn(Optional.of(product));

        productService.incrementSellingQty(1, 3);

        assertThat(product.getSellingQty()).isEqualTo(3);
        verify(productRepository).save(product);
    }

    @Test
    void incrementSellingQty_shouldAddToExistingSellingQty() {
        Product product = product(1, "Savon", 500.0, 350.0, 5, 4);
        when(productRepository.findById(1)).thenReturn(Optional.of(product));

        productService.incrementSellingQty(1, 3);

        assertThat(product.getSellingQty()).isEqualTo(7);
    }

}