package com.galandnoah.inventory_manager.sale.service;

import com.galandnoah.inventory_manager.exception.InsufficientStockException;
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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SaleServiceImplTest {

    @Mock
    private SaleRepository saleRepository;

    @Mock
    private ProductServiceImpl productService;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private SaleMapper saleMapper;

    @InjectMocks
    private SaleServiceImpl saleService;



    /**
     * Single place to adapt if the component order of SaleRequest differs
     * (assumed: productId, qty, amount, paid).
     */
    private static SaleRequest saleRequest(Integer productId, int qty, Double amount, Boolean paid) {
        return new SaleRequest(productId, qty, amount, paid);
    }

    private static Product product() {
        return Product.builder()
                .id(1)
                .designation("Savon")
                .sellingPrice(500.0)
                .purchasePrice(350.0)
                .stock(20)
                .sellingQty(0)
                .build();
    }

    private void stubSuccessfulSave(SaleRequest request, SaleResponse response) {
        when(saleMapper.toEntity(request)).thenReturn(new Sale());
        when(saleRepository.save(any(Sale.class))).thenAnswer(inv -> inv.getArgument(0));
        when(saleMapper.toResponse(any(Sale.class))).thenReturn(response);
    }



    @Test
    void createSale_shouldRejectMissingProductId() {
        SaleRequest request = saleRequest(null, 3, null, true);

        assertThatThrownBy(() -> saleService.createSale(request))
                .isInstanceOf(InvalidRequestException.class);

        verifyNoInteractions(productRepository, productService, saleRepository);
    }

    @Test
    void createSale_shouldRejectNegativeAmount() {
        SaleRequest request = saleRequest(1, 3, -50.0, true);

        assertThatThrownBy(() -> saleService.createSale(request))
                .isInstanceOf(InvalidRequestException.class);

        verifyNoInteractions(productRepository, productService, saleRepository);
    }

    @Test
    void createSale_shouldThrowNotFound_whenProductDoesNotExist() {
        SaleRequest request = saleRequest(99, 3, null, true);
        when(productRepository.findById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> saleService.createSale(request))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("99");

        verifyNoInteractions(productService);
        verify(saleRepository, never()).save(any());
    }

    @Test
    void createSale_shouldPropagateInsufficientStock_andNotSaveSale() {
        SaleRequest request = saleRequest(1, 50, null, true);
        when(productRepository.findById(1)).thenReturn(Optional.of(product()));
        doThrow(new InsufficientStockException("Insufficient stock"))
                .when(productService).decrementStock(1, 50);

        assertThatThrownBy(() -> saleService.createSale(request))
                .isInstanceOf(InsufficientStockException.class);

        verify(productService, never()).incrementSellingQty(anyInt(), anyInt());
        verify(saleRepository, never()).save(any());
    }



    @Test
    void createSale_shouldComputeAmountFromSellingPrice_whenAmountIsNull() {
        SaleRequest request = saleRequest(1, 3, null, true);
        Product product = product();
        SaleResponse expected = mock(SaleResponse.class);
        when(productRepository.findById(1)).thenReturn(Optional.of(product));
        stubSuccessfulSave(request, expected);

        SaleResponse result = saleService.createSale(request);

        ArgumentCaptor<Sale> captor = ArgumentCaptor.forClass(Sale.class);
        verify(saleRepository).save(captor.capture());
        Sale saved = captor.getValue();

        assertThat(result).isSameAs(expected);
        assertThat(saved.getAmount()).isEqualTo(1500.0);
        assertThat(saved.getPaid()).isTrue();
        assertThat(saved.getProduct()).isSameAs(product);
        assertThat(saved.getCreated()).isEqualTo(LocalDate.now());
        verify(productService).decrementStock(1, 3);
        verify(productService).incrementSellingQty(1, 3);
    }

    @Test
    void createSale_shouldUseCustomAmount_andStillUpdateStock() {
        SaleRequest request = saleRequest(1, 3, 1200.0, false);
        SaleResponse expected = mock(SaleResponse.class);
        when(productRepository.findById(1)).thenReturn(Optional.of(product()));
        stubSuccessfulSave(request, expected);

        saleService.createSale(request);

        ArgumentCaptor<Sale> captor = ArgumentCaptor.forClass(Sale.class);
        verify(saleRepository).save(captor.capture());

        assertThat(captor.getValue().getAmount()).isEqualTo(1200.0);
        assertThat(captor.getValue().getPaid()).isFalse();
        verify(productService).decrementStock(1, 3);
        verify(productService).incrementSellingQty(1, 3);
    }

    @Test
    void createSale_shouldDefaultPaidToFalse_whenNotProvided() {
        SaleRequest request = saleRequest(1, 3, null, null);
        SaleResponse expected = mock(SaleResponse.class);
        when(productRepository.findById(1)).thenReturn(Optional.of(product()));
        stubSuccessfulSave(request, expected);

        saleService.createSale(request);

        ArgumentCaptor<Sale> captor = ArgumentCaptor.forClass(Sale.class);
        verify(saleRepository).save(captor.capture());

        assertThat(captor.getValue().getPaid()).isFalse();
    }



    @Test
    void findSales_shouldRejectPageLowerThanOne() {
        assertThatThrownBy(() -> saleService.findSales(10, 0))
                .isInstanceOf(InvalidRequestException.class);

        verifyNoInteractions(saleRepository);
    }

    @Test
    void findSales_shouldRejectSizeLowerThanOne() {
        assertThatThrownBy(() -> saleService.findSales(0, 1))
                .isInstanceOf(InvalidRequestException.class);

        verifyNoInteractions(saleRepository);
    }

    @Test
    void findSales_shouldComputeOffsetFromPage() {
        Sale sale = new Sale();
        SaleResponse response = mock(SaleResponse.class);
        when(saleRepository.findSales(10, 10)).thenReturn(List.of(sale));
        when(saleMapper.toResponse(sale)).thenReturn(response);

        List<SaleResponse> result = saleService.findSales(10, 2);

        assertThat(result).containsExactly(response);
    }


    @Test
    void findSalesByDate_shouldMapSalesOfTheDay() {
        LocalDate date = LocalDate.of(2026, 10, 23);
        Sale sale = new Sale();
        SaleResponse response = mock(SaleResponse.class);
        when(saleRepository.findByCreated(date)).thenReturn(List.of(sale));
        when(saleMapper.toResponse(sale)).thenReturn(response);

        assertThat(saleService.findSalesByDate(date)).containsExactly(response);
    }
}