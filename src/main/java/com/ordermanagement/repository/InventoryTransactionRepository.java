package com.ordermanagement.repository;

import com.ordermanagement.domain.entity.InventoryTransaction;
import com.ordermanagement.domain.enums.InventoryTransactionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface InventoryTransactionRepository extends JpaRepository<InventoryTransaction, UUID> {

    List<InventoryTransaction> findByProductId(UUID productId);

    List<InventoryTransaction> findByOrderId(UUID orderId);

    List<InventoryTransaction> findByTransactionType(InventoryTransactionType type);
}
