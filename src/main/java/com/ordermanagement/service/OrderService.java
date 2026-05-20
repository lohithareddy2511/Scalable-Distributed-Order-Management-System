package com.ordermanagement.service;

import com.ordermanagement.domain.entity.Customer;
import com.ordermanagement.domain.entity.Order;
import com.ordermanagement.domain.entity.OrderItem;
import com.ordermanagement.domain.entity.Product;
import com.ordermanagement.domain.enums.OrderStatus;
import com.ordermanagement.dto.mapper.OrderMapper;
import com.ordermanagement.dto.request.CreateOrderRequest;
import com.ordermanagement.dto.request.OrderItemRequest;
import com.ordermanagement.dto.request.UpdateOrderStatusRequest;
import com.ordermanagement.dto.response.OrderResponse;
import com.ordermanagement.dto.response.PagedResponse;
import com.ordermanagement.exception.InvalidOrderStateException;
import com.ordermanagement.exception.ResourceNotFoundException;
import com.ordermanagement.repository.CustomerRepository;
import com.ordermanagement.repository.OrderRepository;
import com.ordermanagement.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;
    private final InventoryService inventoryService;
    private final OrderMapper orderMapper;

    private static final AtomicLong orderCounter = new AtomicLong(System.currentTimeMillis());

    private static final Set<OrderStatus> VALID_TRANSITIONS_FROM_PENDING = Set.of(OrderStatus.CONFIRMED, OrderStatus.CANCELLED);
    private static final Set<OrderStatus> VALID_TRANSITIONS_FROM_CONFIRMED = Set.of(OrderStatus.PROCESSING, OrderStatus.CANCELLED);
    private static final Set<OrderStatus> VALID_TRANSITIONS_FROM_PROCESSING = Set.of(OrderStatus.SHIPPED, OrderStatus.CANCELLED);
    private static final Set<OrderStatus> VALID_TRANSITIONS_FROM_SHIPPED = Set.of(OrderStatus.DELIVERED);
    private static final Set<OrderStatus> VALID_TRANSITIONS_FROM_DELIVERED = Set.of(OrderStatus.REFUNDED);

    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request) {
        log.info("Creating order for customer: {}", request.getCustomerId());

        Customer customer = customerRepository.findById(request.getCustomerId())
                .orElseThrow(() -> new ResourceNotFoundException("Customer", "id", request.getCustomerId()));

        Order order = Order.builder()
                .orderNumber(generateOrderNumber())
                .customer(customer)
                .status(OrderStatus.PENDING)
                .shippingAddressLine1(request.getShippingAddressLine1())
                .shippingAddressLine2(request.getShippingAddressLine2())
                .shippingCity(request.getShippingCity())
                .shippingState(request.getShippingState())
                .shippingZipCode(request.getShippingZipCode())
                .shippingCountry(request.getShippingCountry())
                .notes(request.getNotes())
                .build();

        // Process order items and reserve inventory
        for (OrderItemRequest itemRequest : request.getItems()) {
            Product product = productRepository.findById(itemRequest.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product", "id", itemRequest.getProductId()));

            OrderItem orderItem = OrderItem.builder()
                    .product(product)
                    .quantity(itemRequest.getQuantity())
                    .unitPrice(product.getPrice())
                    .totalPrice(product.getPrice().multiply(BigDecimal.valueOf(itemRequest.getQuantity())))
                    .build();

            order.addOrderItem(orderItem);

            // Reserve inventory
            inventoryService.reserveStock(product.getId(), itemRequest.getQuantity(), order.getId());
        }

        Order saved = orderRepository.save(order);
        log.info("Order created. Order number: {}", saved.getOrderNumber());

        return orderMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrderById(UUID id) {
        log.debug("Fetching order with ID: {}", id);
        Order order = orderRepository.findByIdWithItemsAndPayments(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", id));
        return orderMapper.toResponse(order);
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrderByNumber(String orderNumber) {
        log.debug("Fetching order with number: {}", orderNumber);
        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "orderNumber", orderNumber));
        return orderMapper.toResponse(order);
    }

    @Transactional(readOnly = true)
    public PagedResponse<OrderResponse> getAllOrders(int page, int size, String sortBy, String direction) {
        log.debug("Fetching orders - page: {}, size: {}", page, size);
        Sort sort = Sort.by(Sort.Direction.fromString(direction), sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<Order> orderPage = orderRepository.findAll(pageable);
        return buildPagedResponse(orderPage);
    }

    @Transactional(readOnly = true)
    public PagedResponse<OrderResponse> getOrdersByCustomer(UUID customerId, int page, int size) {
        log.debug("Fetching orders for customer: {}", customerId);
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Order> orderPage = orderRepository.findByCustomerId(customerId, pageable);
        return buildPagedResponse(orderPage);
    }

    @Transactional(readOnly = true)
    public PagedResponse<OrderResponse> getOrdersByStatus(OrderStatus status, int page, int size) {
        log.debug("Fetching orders with status: {}", status);
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Order> orderPage = orderRepository.findByStatus(status, pageable);
        return buildPagedResponse(orderPage);
    }

    @Transactional
    public OrderResponse updateOrderStatus(UUID orderId, UpdateOrderStatusRequest request) {
        log.info("Updating order {} status to {}", orderId, request.getStatus());

        Order order = orderRepository.findByIdWithItems(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));

        validateStatusTransition(order.getStatus(), request.getStatus());

        OrderStatus previousStatus = order.getStatus();
        order.setStatus(request.getStatus());

        // Handle status-specific logic
        switch (request.getStatus()) {
            case CONFIRMED -> order.setPlacedAt(LocalDateTime.now());
            case SHIPPED -> order.setShippedAt(LocalDateTime.now());
            case DELIVERED -> order.setDeliveredAt(LocalDateTime.now());
            case CANCELLED -> {
                order.setCancelledAt(LocalDateTime.now());
                // Release reserved inventory
                order.getOrderItems().forEach(item ->
                        inventoryService.releaseStock(item.getProduct().getId(), item.getQuantity(), order.getId()));
            }
            default -> { /* no-op */ }
        }

        Order updated = orderRepository.save(order);
        log.info("Order {} status updated from {} to {}", orderId, previousStatus, request.getStatus());
        return orderMapper.toResponse(updated);
    }

    @Transactional
    public OrderResponse cancelOrder(UUID orderId, String reason) {
        UpdateOrderStatusRequest request = UpdateOrderStatusRequest.builder()
                .status(OrderStatus.CANCELLED)
                .reason(reason)
                .build();
        return updateOrderStatus(orderId, request);
    }

    private void validateStatusTransition(OrderStatus current, OrderStatus target) {
        Set<OrderStatus> validTransitions = switch (current) {
            case PENDING -> VALID_TRANSITIONS_FROM_PENDING;
            case CONFIRMED -> VALID_TRANSITIONS_FROM_CONFIRMED;
            case PROCESSING -> VALID_TRANSITIONS_FROM_PROCESSING;
            case SHIPPED -> VALID_TRANSITIONS_FROM_SHIPPED;
            case DELIVERED -> VALID_TRANSITIONS_FROM_DELIVERED;
            default -> Set.of();
        };

        if (!validTransitions.contains(target)) {
            throw new InvalidOrderStateException(current.name(), target.name());
        }
    }

    private String generateOrderNumber() {
        return "ORD-" + orderCounter.incrementAndGet();
    }

    private PagedResponse<OrderResponse> buildPagedResponse(Page<Order> page) {
        return PagedResponse.<OrderResponse>builder()
                .content(page.getContent().stream().map(orderMapper::toResponse).toList())
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .build();
    }
}
