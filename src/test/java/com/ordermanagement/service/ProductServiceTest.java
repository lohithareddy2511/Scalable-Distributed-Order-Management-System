package com.ordermanagement.service;

import com.ordermanagement.domain.entity.Product;
import com.ordermanagement.dto.mapper.ProductMapper;
import com.ordermanagement.dto.request.CreateProductRequest;
import com.ordermanagement.dto.response.ProductResponse;
import com.ordermanagement.exception.DuplicateResourceException;
import com.ordermanagement.exception.ResourceNotFoundException;
import com.ordermanagement.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductMapper productMapper;

    @InjectMocks
    private ProductService productService;

    private Product product;
    private ProductResponse productResponse;
    private CreateProductRequest createRequest;
    private UUID productId;

    @BeforeEach
    void setUp() {
        productId = UUID.randomUUID();

        product = new Product();
        product.setId(productId);
        product.setSku("PROD-001");
        product.setName("Test Product");
        product.setPrice(new BigDecimal("29.99"));
        product.setStockQuantity(100);
        product.setCategory("Electronics");
        product.setIsActive(true);
        product.setCreatedAt(LocalDateTime.now());
        product.setUpdatedAt(LocalDateTime.now());

        productResponse = ProductResponse.builder()
                .id(productId)
                .sku("PROD-001")
                .name("Test Product")
                .price(new BigDecimal("29.99"))
                .stockQuantity(100)
                .category("Electronics")
                .isActive(true)
                .build();

        createRequest = CreateProductRequest.builder()
                .sku("PROD-001")
                .name("Test Product")
                .price(new BigDecimal("29.99"))
                .stockQuantity(100)
                .category("Electronics")
                .build();
    }

    @Test
    @DisplayName("Should create product successfully")
    void createProduct_Success() {
        when(productRepository.existsBySku(createRequest.getSku())).thenReturn(false);
        when(productMapper.toEntity(createRequest)).thenReturn(product);
        when(productRepository.save(any(Product.class))).thenReturn(product);
        when(productMapper.toResponse(product)).thenReturn(productResponse);

        ProductResponse result = productService.createProduct(createRequest);

        assertThat(result).isNotNull();
        assertThat(result.getSku()).isEqualTo("PROD-001");
        assertThat(result.getPrice()).isEqualTo(new BigDecimal("29.99"));
        verify(productRepository).save(any(Product.class));
    }

    @Test
    @DisplayName("Should throw DuplicateResourceException when SKU already exists")
    void createProduct_DuplicateSku() {
        when(productRepository.existsBySku(createRequest.getSku())).thenReturn(true);

        assertThatThrownBy(() -> productService.createProduct(createRequest))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("sku");
    }

    @Test
    @DisplayName("Should get product by ID successfully")
    void getProductById_Success() {
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(productMapper.toResponse(product)).thenReturn(productResponse);

        ProductResponse result = productService.getProductById(productId);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(productId);
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when product not found")
    void getProductById_NotFound() {
        when(productRepository.findById(productId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.getProductById(productId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Product");
    }

    @Test
    @DisplayName("Should get product by SKU successfully")
    void getProductBySku_Success() {
        when(productRepository.findBySku("PROD-001")).thenReturn(Optional.of(product));
        when(productMapper.toResponse(product)).thenReturn(productResponse);

        ProductResponse result = productService.getProductBySku("PROD-001");

        assertThat(result).isNotNull();
        assertThat(result.getSku()).isEqualTo("PROD-001");
    }
}
