package com.ordermanagement.repository;

import com.ordermanagement.domain.entity.Payment;
import com.ordermanagement.domain.enums.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    List<Payment> findByOrderId(UUID orderId);

    List<Payment> findByStatus(PaymentStatus status);

    List<Payment> findByOrderIdAndStatus(UUID orderId, PaymentStatus status);
}
