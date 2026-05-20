package com.ordermanagement.service;

import com.ordermanagement.domain.entity.Product;
import com.ordermanagement.dto.mapper.ProductMapper;
import com.ordermanagement.dto.request.CreateProductRequest;
import com.ordermanagement.dto.request.UpdateProductRequest;
import com.ordermanagement.dto.response.PagedResponse;
import com.ordermanagement.dto.response.ProductResponse;
import com.ordermanagement.exception.DuplicateResourceException;
import com.ordermanagement.exception.ResourceNotFoundException;
import com.ordermanagement.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductMapper productMapper;

    @Transactional
    public ProductResponse createProduct(CreateProductRequest request) {
        log.info("Creating product with SKU: {}", request.getSku());

        if (productRepository.existsBySku(request.getSku())) {
            throw new DuplicateResourceException("Product", "sku", request.getSku());
        }

        Product product = productMapper.toEntity(request);
        if (product.getStockQuantity() == null) {
            product.setStockQuantity(0);
        }
        Product saved = productRepository.save(product);

        log.info("Product created with ID: {}", saved.getId());
        return productMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public ProductResponse getProductById(UUID id) {
        log.debug("Fetching product with ID: {}", id);
        Product product = findProductOrThrow(id);
        return productMapper.toResponse(product);
    }

    @Transactional(readOnly = true)
    public ProductResponse getProductBySku(String sku) {
        log.debug("Fetching product with SKU: {}", sku);
        Product product = productRepository.findBySku(sku)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "sku", sku));
        return productMapper.toResponse(product);
    }

    @Transactional(readOnly = true)
    public PagedResponse<ProductResponse> getAllProducts(int page, int size, String sortBy, String direction) {
        log.debug("Fetching products - page: {}, size: {}", page, size);
        Sort sort = Sort.by(Sort.Direction.fromString(direction), sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<Product> productPage = productRepository.findAll(pageable);
        return buildPagedResponse(productPage);
    }

    @Transactional(readOnly = true)
    public PagedResponse<ProductResponse> getActiveProducts(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Product> productPage = productRepository.findByIsActiveTrue(pageable);
        return buildPagedResponse(productPage);
    }

    @Transactional(readOnly = true)
    public PagedResponse<ProductResponse> getProductsByCategory(String category, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Product> productPage = productRepository.findByCategory(category, pageable);
        return buildPagedResponse(productPage);
    }

    @Transactional(readOnly = true)
    public PagedResponse<ProductResponse> searchProducts(String search, int page, int size) {
        log.debug("Searching products with term: {}", search);
        Pageable pageable = PageRequest.of(page, size);
        Page<Product> productPage = productRepository.searchProducts(search, pageable);
        return buildPagedResponse(productPage);
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> getLowStockProducts(int threshold) {
        log.debug("Fetching low stock products with threshold: {}", threshold);
        return productRepository.findLowStockProducts(threshold).stream()
                .map(productMapper::toResponse)
                .toList();
    }

    @Transactional
    public ProductResponse updateProduct(UUID id, UpdateProductRequest request) {
        log.info("Updating product with ID: {}", id);
        Product product = findProductOrThrow(id);

        if (request.getName() != null) product.setName(request.getName());
        if (request.getDescription() != null) product.setDescription(request.getDescription());
        if (request.getPrice() != null) product.setPrice(request.getPrice());
        if (request.getStockQuantity() != null) product.setStockQuantity(request.getStockQuantity());
        if (request.getCategory() != null) product.setCategory(request.getCategory());
        if (request.getIsActive() != null) product.setIsActive(request.getIsActive());

        Product updated = productRepository.save(product);
        log.info("Product updated with ID: {}", updated.getId());
        return productMapper.toResponse(updated);
    }

    @Transactional
    public void deleteProduct(UUID id) {
        log.info("Deleting product with ID: {}", id);
        Product product = findProductOrThrow(id);
        productRepository.delete(product);
        log.info("Product deleted with ID: {}", id);
    }

    private Product findProductOrThrow(UUID id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", id));
    }

    private PagedResponse<ProductResponse> buildPagedResponse(Page<Product> page) {
        return PagedResponse.<ProductResponse>builder()
                .content(page.getContent().stream().map(productMapper::toResponse).toList())
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .build();
    }
}
