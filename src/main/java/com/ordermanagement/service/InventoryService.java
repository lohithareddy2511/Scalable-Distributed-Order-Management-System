package com.ordermanagement.service;

import com.ordermanagement.domain.entity.InventoryTransaction;
import com.ordermanagement.domain.entity.Product;
import com.ordermanagement.domain.enums.InventoryTransactionType;
import com.ordermanagement.exception.InsufficientStockException;
import com.ordermanagement.exception.ResourceNotFoundException;
import com.ordermanagement.repository.InventoryTransactionRepository;
import com.ordermanagement.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryService {

    private final ProductRepository productRepository;
    private final InventoryTransactionRepository inventoryTransactionRepository;

    @Transactional(propagation = Propagation.REQUIRED)
    public void reserveStock(UUID productId, int quantity, UUID orderId) {
        log.info("Reserving {} units of product {} for order {}", quantity, productId, orderId);

        Product product = productRepository.findByIdWithLock(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", productId));

        if (product.getStockQuantity() < quantity) {
            throw new InsufficientStockException(product.getName(), quantity, product.getStockQuantity());
        }

        product.setStockQuantity(product.getStockQuantity() - quantity);
        productRepository.save(product);

        recordTransaction(product, orderId, InventoryTransactionType.RESERVED, -quantity,
                "Stock reserved for order " + orderId);

        log.info("Reserved {} units of product {}. Remaining stock: {}", quantity, productId, product.getStockQuantity());
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public void releaseStock(UUID productId, int quantity, UUID orderId) {
        log.info("Releasing {} units of product {} for order {}", quantity, productId, orderId);

        Product product = productRepository.findByIdWithLock(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", productId));

        product.setStockQuantity(product.getStockQuantity() + quantity);
        productRepository.save(product);

        recordTransaction(product, orderId, InventoryTransactionType.RELEASED, quantity,
                "Stock released for cancelled order " + orderId);

        log.info("Released {} units of product {}. New stock: {}", quantity, productId, product.getStockQuantity());
    }

    @Transactional
    public void addStock(UUID productId, int quantity, String note) {
        log.info("Adding {} units to product {}", quantity, productId);

        Product product = productRepository.findByIdWithLock(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", productId));

        product.setStockQuantity(product.getStockQuantity() + quantity);
        productRepository.save(product);

        recordTransaction(product, null, InventoryTransactionType.STOCK_IN, quantity, note);

        log.info("Added {} units to product {}. New stock: {}", quantity, productId, product.getStockQuantity());
    }

    @Transactional
    public void adjustStock(UUID productId, int newQuantity, String note) {
        log.info("Adjusting stock for product {} to {}", productId, newQuantity);

        Product product = productRepository.findByIdWithLock(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", productId));

        int adjustment = newQuantity - product.getStockQuantity();
        product.setStockQuantity(newQuantity);
        productRepository.save(product);

        recordTransaction(product, null, InventoryTransactionType.ADJUSTMENT, adjustment, note);

        log.info("Adjusted stock for product {} to {}", productId, newQuantity);
    }

    private void recordTransaction(Product product, UUID orderId, InventoryTransactionType type,
                                   int quantity, String note) {
        InventoryTransaction transaction = InventoryTransaction.builder()
                .product(product)
                .transactionType(type)
                .quantity(quantity)
                .referenceNote(note)
                .build();

        if (orderId != null) {
            // We set the order reference via a lightweight approach
            transaction.setReferenceNote(note + " (Order: " + orderId + ")");
        }

        inventoryTransactionRepository.save(transaction);
    }
}
