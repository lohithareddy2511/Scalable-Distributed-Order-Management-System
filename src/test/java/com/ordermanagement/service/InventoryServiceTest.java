package com.ordermanagement.service;

import com.ordermanagement.domain.entity.Product;
import com.ordermanagement.exception.InsufficientStockException;
import com.ordermanagement.exception.ResourceNotFoundException;
import com.ordermanagement.repository.InventoryTransactionRepository;
import com.ordermanagement.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private InventoryTransactionRepository inventoryTransactionRepository;

    @InjectMocks
    private InventoryService inventoryService;

    private Product product;
    private UUID productId;
    private UUID orderId;

    @BeforeEach
    void setUp() {
        productId = UUID.randomUUID();
        orderId = UUID.randomUUID();

        product = new Product();
        product.setId(productId);
        product.setName("Test Product");
        product.setSku("PROD-001");
        product.setPrice(new BigDecimal("25.00"));
        product.setStockQuantity(50);
    }

    @Test
    @DisplayName("Should reserve stock successfully")
    void reserveStock_Success() {
        when(productRepository.findByIdWithLock(productId)).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenReturn(product);

        inventoryService.reserveStock(productId, 10, orderId);

        assertThat(product.getStockQuantity()).isEqualTo(40);
        verify(productRepository).save(product);
        verify(inventoryTransactionRepository).save(any());
    }

    @Test
    @DisplayName("Should throw InsufficientStockException when stock is insufficient")
    void reserveStock_InsufficientStock() {
        when(productRepository.findByIdWithLock(productId)).thenReturn(Optional.of(product));

        assertThatThrownBy(() -> inventoryService.reserveStock(productId, 100, orderId))
                .isInstanceOf(InsufficientStockException.class)
                .hasMessageContaining("Insufficient stock");
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when product not found")
    void reserveStock_ProductNotFound() {
        when(productRepository.findByIdWithLock(productId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> inventoryService.reserveStock(productId, 10, orderId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Should release stock successfully")
    void releaseStock_Success() {
        when(productRepository.findByIdWithLock(productId)).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenReturn(product);

        inventoryService.releaseStock(productId, 10, orderId);

        assertThat(product.getStockQuantity()).isEqualTo(60);
        verify(productRepository).save(product);
    }

    @Test
    @DisplayName("Should add stock successfully")
    void addStock_Success() {
        when(productRepository.findByIdWithLock(productId)).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenReturn(product);

        inventoryService.addStock(productId, 25, "Restocking");

        assertThat(product.getStockQuantity()).isEqualTo(75);
        verify(productRepository).save(product);
    }

    @Test
    @DisplayName("Should adjust stock successfully")
    void adjustStock_Success() {
        when(productRepository.findByIdWithLock(productId)).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenReturn(product);

        inventoryService.adjustStock(productId, 30, "Inventory audit");

        assertThat(product.getStockQuantity()).isEqualTo(30);
        verify(productRepository).save(product);
    }
}
