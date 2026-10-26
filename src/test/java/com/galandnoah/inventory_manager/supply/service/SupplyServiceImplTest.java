package com.galandnoah.inventory_manager.supply.service;

import com.galandnoah.inventory_manager.exception.DuplicateResourceException;
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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SupplyServiceImplTest {

    @Mock
    private SupplyRepository supplyRepository;

    @Mock
    private ProductServiceImpl productService;

    @Mock
    private SupplyMapper supplyMapper;

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private SupplyServiceImpl supplyService;

    /**
     * Single place to adapt if the component order of SupplyRequest differs
     * (assumed: productId, designation, sellingPrice, cost, qty).
     */
    private static SupplyRequest supplyRequest(Integer productId, String designation,
                                               Double sellingPrice, double cost, int qty) {
        return new SupplyRequest(designation, productId, qty, cost, sellingPrice, "");
    }

    private static Product product() {
        return Product.builder().id(1).designation("Savon").stock(10).build();
    }


    @Test
    void createSupply_shouldRejectRequestWithoutProductNorDesignation() {
        SupplyRequest request = supplyRequest(null, null, null, 7000.0, 20);

        assertThatThrownBy(() -> supplyService.createSupply(request))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("Either select product or enter designation");

        verifyNoInteractions(productService, productRepository, supplyRepository);
    }

    @Test
    void createSupply_shouldTreatBlankDesignationAsMissing() {
        SupplyRequest request = supplyRequest(null, "   ", 1500.0, 7000.0, 20);

        assertThatThrownBy(() -> supplyService.createSupply(request))
                .isInstanceOf(InvalidRequestException.class);

        verifyNoInteractions(productService, productRepository, supplyRepository);
    }

    @Test
    void createSupply_shouldRejectRequestWithBothProductAndDesignation() {
        SupplyRequest request = supplyRequest(1, "Savon", 1500.0, 7000.0, 20);

        assertThatThrownBy(() -> supplyService.createSupply(request))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("not both");

        verifyNoInteractions(productService, productRepository, supplyRepository);
    }

    @Test
    void createSupply_shouldRejectNonPositiveQuantity() {
        SupplyRequest request = supplyRequest(1, null, null, 7000.0, 0);

        assertThatThrownBy(() -> supplyService.createSupply(request))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("Quantity");

        verifyNoInteractions(productService, productRepository, supplyRepository);
    }

    @Test
    void createSupply_shouldRejectNegativeCost() {
        SupplyRequest request = supplyRequest(1, null, null, -1.0, 5);

        assertThatThrownBy(() -> supplyService.createSupply(request))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("Cost");

        verifyNoInteractions(productService, productRepository, supplyRepository);
    }

    @Test
    void createSupply_shouldRequireSellingPrice_forNewProduct() {
        SupplyRequest request = supplyRequest(null, "Huile 1L", null, 24000.0, 24);

        assertThatThrownBy(() -> supplyService.createSupply(request))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("Selling price");

        verifyNoInteractions(productService, productRepository, supplyRepository);
    }



    @Test
    void createSupply_shouldThrowNotFound_whenProductDoesNotExist() {
        SupplyRequest request = supplyRequest(99, null, null, 7000.0, 20);
        when(supplyMapper.toEntity(request)).thenReturn(new Supply());
        when(productRepository.findById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> supplyService.createSupply(request))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("99");

        verify(productService, never()).incrementStock(anyInt(), anyInt());
        verify(supplyRepository, never()).save(any());
    }

    @Test
    void createSupply_shouldIncrementStockAndUpdateRoundedPurchasePrice_forExistingProduct() {
        SupplyRequest request = supplyRequest(1, null, null, 1000.0, 3);
        Product product = product();
        SupplyResponse expected = mock(SupplyResponse.class);
        when(supplyMapper.toEntity(request)).thenReturn(new Supply());
        when(productRepository.findById(1)).thenReturn(Optional.of(product));
        when(supplyRepository.save(any(Supply.class))).thenAnswer(inv -> inv.getArgument(0));
        when(supplyMapper.toResponse(any(Supply.class))).thenReturn(expected);

        SupplyResponse result = supplyService.createSupply(request);

        ArgumentCaptor<Supply> captor = ArgumentCaptor.forClass(Supply.class);
        verify(supplyRepository).save(captor.capture());
        Supply saved = captor.getValue();

        assertThat(result).isSameAs(expected);
        assertThat(saved.getProduct()).isSameAs(product);
        assertThat(saved.getMadeBy()).isEqualTo("Maman");
        assertThat(saved.getCreated()).isEqualTo(LocalDate.now());
        verify(productService).incrementStock(1, 3);
        // 1000 / 3 = 333.33 -> rounded to 333
        verify(productService).setPurchasePrice(1, 333.0);
        verify(productService, never()).createProduct(any());
    }



    @Test
    void createSupply_shouldCreateProductWithRoundedPurchasePrice_forNewDesignation() {
        SupplyRequest request = supplyRequest(null, " Huile 1L ", 1500.0, 24000.0, 24);
        Product created = Product.builder().id(5).designation("Huile 1L").stock(24).build();
        ProductResponse productResponse = new ProductResponse();
        productResponse.setId(5);
        SupplyResponse expected = mock(SupplyResponse.class);

        when(supplyMapper.toEntity(request)).thenReturn(new Supply());
        when(productService.createProduct(any(ProductRequest.class))).thenReturn(productResponse);
        when(productRepository.findById(5)).thenReturn(Optional.of(created));
        when(supplyRepository.save(any(Supply.class))).thenAnswer(inv -> inv.getArgument(0));
        when(supplyMapper.toResponse(any(Supply.class))).thenReturn(expected);

        SupplyResponse result = supplyService.createSupply(request);

        // designation trimmed, purchase price = 24000 / 24 = 1000, stock = qty, threshold = 10
        verify(productService).createProduct(new ProductRequest("Huile 1L", 1500.0, 1000.0, 24, 10));
        verify(productService, never()).incrementStock(anyInt(), anyInt());
        verify(productService, never()).setPurchasePrice(anyInt(), anyDouble());

        ArgumentCaptor<Supply> captor = ArgumentCaptor.forClass(Supply.class);
        verify(supplyRepository).save(captor.capture());

        assertThat(result).isSameAs(expected);
        assertThat(captor.getValue().getProduct()).isSameAs(created);
    }

    @Test
    void createSupply_shouldPropagateDuplicate_whenDesignationAlreadyExists() {
        SupplyRequest request = supplyRequest(null, "Savon", 500.0, 7000.0, 20);
        when(supplyMapper.toEntity(request)).thenReturn(new Supply());
        when(productService.createProduct(any(ProductRequest.class)))
                .thenThrow(new DuplicateResourceException("Product already exists: Savon"));

        assertThatThrownBy(() -> supplyService.createSupply(request))
                .isInstanceOf(DuplicateResourceException.class);

        verify(supplyRepository, never()).save(any());
    }


    @Test
    void findAll_shouldRejectPageLowerThanOne() {
        assertThatThrownBy(() -> supplyService.findAll(10, 0))
                .isInstanceOf(InvalidRequestException.class);

        verifyNoInteractions(supplyRepository);
    }

    @Test
    void findAll_shouldRejectSizeLowerThanOne() {
        assertThatThrownBy(() -> supplyService.findAll(0, 1))
                .isInstanceOf(InvalidRequestException.class);

        verifyNoInteractions(supplyRepository);
    }

    @Test
    void findAll_shouldUseZeroBasedPageIndexAndSortByCreation() {
        Supply supply = new Supply();
        SupplyResponse response = mock(SupplyResponse.class);
        when(supplyRepository.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(supply)));
        when(supplyMapper.toResponse(supply)).thenReturn(response);

        Page<SupplyResponse> result = supplyService.findAll(5, 2);

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(supplyRepository).findAll(captor.capture());
        Pageable pageable = captor.getValue();

        assertThat(pageable.getPageNumber()).isEqualTo(1);
        assertThat(pageable.getPageSize()).isEqualTo(5);
        assertThat(pageable.getSort()).isEqualTo(Sort.by("created").ascending());
        assertThat(result.getContent()).containsExactly(response);
    }


    @Test
    void findByDate_shouldMapSuppliesOfTheDay() {
        LocalDate date = LocalDate.of(2026, 10, 23);
        Supply supply = new Supply();
        SupplyResponse response = mock(SupplyResponse.class);
        when(supplyRepository.findByCreated(date)).thenReturn(List.of(supply));
        when(supplyMapper.toResponse(supply)).thenReturn(response);

        assertThat(supplyService.findByDate(date)).containsExactly(response);
    }
}