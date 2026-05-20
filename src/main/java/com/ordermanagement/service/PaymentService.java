package com.ordermanagement.service;

import com.ordermanagement.domain.entity.Order;
import com.ordermanagement.domain.entity.Payment;
import com.ordermanagement.domain.enums.OrderStatus;
import com.ordermanagement.domain.enums.PaymentStatus;
import com.ordermanagement.dto.mapper.OrderMapper;
import com.ordermanagement.dto.request.ProcessPaymentRequest;
import com.ordermanagement.dto.response.PaymentResponse;
import com.ordermanagement.exception.InvalidOrderStateException;
import com.ordermanagement.exception.PaymentProcessingException;
import com.ordermanagement.exception.ResourceNotFoundException;
import com.ordermanagement.repository.OrderRepository;
import com.ordermanagement.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;

    @Transactional
    public PaymentResponse processPayment(ProcessPaymentRequest request) {
        log.info("Processing payment for order: {}", request.getOrderId());

        Order order = orderRepository.findById(request.getOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", request.getOrderId()));

        if (order.getStatus() != OrderStatus.PENDING && order.getStatus() != OrderStatus.CONFIRMED) {
            throw new InvalidOrderStateException(
                    "Payment can only be processed for orders in PENDING or CONFIRMED status. Current: " + order.getStatus());
        }

        Payment payment = Payment.builder()
                .order(order)
                .paymentMethod(request.getPaymentMethod())
                .amount(request.getAmount())
                .transactionReference(request.getTransactionReference())
                .status(PaymentStatus.PENDING)
                .build();

        try {
            // Simulate payment processing
            payment.setStatus(PaymentStatus.COMPLETED);
            payment.setPaidAt(LocalDateTime.now());

            Payment saved = paymentRepository.save(payment);

            // Update order status to CONFIRMED after successful payment
            if (order.getStatus() == OrderStatus.PENDING) {
                order.setStatus(OrderStatus.CONFIRMED);
                order.setPlacedAt(LocalDateTime.now());
                orderRepository.save(order);
            }

            log.info("Payment processed successfully. Payment ID: {}", saved.getId());
            return orderMapper.toPaymentResponse(saved);

        } catch (Exception e) {
            payment.setStatus(PaymentStatus.FAILED);
            paymentRepository.save(payment);
            throw new PaymentProcessingException("Payment processing failed: " + e.getMessage(), e);
        }
    }

    @Transactional(readOnly = true)
    public List<PaymentResponse> getPaymentsByOrderId(UUID orderId) {
        log.debug("Fetching payments for order: {}", orderId);
        return paymentRepository.findByOrderId(orderId).stream()
                .map(orderMapper::toPaymentResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public PaymentResponse getPaymentById(UUID paymentId) {
        log.debug("Fetching payment: {}", paymentId);
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", "id", paymentId));
        return orderMapper.toPaymentResponse(payment);
    }

    @Transactional
    public PaymentResponse refundPayment(UUID paymentId) {
        log.info("Refunding payment: {}", paymentId);

        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", "id", paymentId));

        if (payment.getStatus() != PaymentStatus.COMPLETED) {
            throw new PaymentProcessingException("Can only refund completed payments. Current status: " + payment.getStatus());
        }

        payment.setStatus(PaymentStatus.REFUNDED);
        Payment saved = paymentRepository.save(payment);

        log.info("Payment refunded. Payment ID: {}", saved.getId());
        return orderMapper.toPaymentResponse(saved);
    }
}
