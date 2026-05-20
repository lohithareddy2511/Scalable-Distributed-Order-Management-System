package com.ordermanagement.service;

import com.ordermanagement.domain.entity.*;
import com.ordermanagement.domain.enums.OrderStatus;
import com.ordermanagement.dto.mapper.OrderMapper;
import com.ordermanagement.dto.request.CreateOrderRequest;
import com.ordermanagement.dto.request.OrderItemRequest;
import com.ordermanagement.dto.request.UpdateOrderStatusRequest;
import com.ordermanagement.dto.response.OrderResponse;
import com.ordermanagement.exception.InvalidOrderStateException;
import com.ordermanagement.exception.ResourceNotFoundException;
import com.ordermanagement.repository.CustomerRepository;
import com.ordermanagement.repository.OrderRepository;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private InventoryService inventoryService;

    @Mock
    private OrderMapper orderMapper;

    @InjectMocks
    private OrderService orderService;

    private Customer customer;
    private Product product;
    private Order order;
    private OrderResponse orderResponse;
    private UUID customerId;
    private UUID productId;
    private UUID orderId;

    @BeforeEach
    void setUp() {
        customerId = UUID.randomUUID();
        productId = UUID.randomUUID();
        orderId = UUID.randomUUID();

        customer = new Customer();
        customer.setId(customerId);
        customer.setFirstName("John");
        customer.setLastName("Doe");
        customer.setEmail("john@example.com");

        product = new Product();
        product.setId(productId);
        product.setSku("PROD-001");
        product.setName("Test Product");
        product.setPrice(new BigDecimal("25.00"));
        product.setStockQuantity(50);

        order = new Order();
        order.setId(orderId);
        order.setOrderNumber("ORD-12345");
        order.setCustomer(customer);
        order.setStatus(OrderStatus.PENDING);
        order.setTotalAmount(new BigDecimal("50.00"));
        order.setOrderItems(new ArrayList<>());
        order.setCreatedAt(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());

        orderResponse = OrderResponse.builder()
                .id(orderId)
                .orderNumber("ORD-12345")
                .customerId(customerId)
                .customerName("John Doe")
                .status(OrderStatus.PENDING)
                .totalAmount(new BigDecimal("50.00"))
                .build();
    }

    @Test
    @DisplayName("Should create order successfully")
    void createOrder_Success() {
        CreateOrderRequest request = CreateOrderRequest.builder()
                .customerId(customerId)
                .items(List.of(OrderItemRequest.builder()
                        .productId(productId)
                        .quantity(2)
                        .build()))
                .shippingCity("New York")
                .shippingCountry("US")
                .build();

        when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(orderRepository.save(any(Order.class))).thenReturn(order);
        when(orderMapper.toResponse(any(Order.class))).thenReturn(orderResponse);

        OrderResponse result = orderService.createOrder(request);

        assertThat(result).isNotNull();
        assertThat(result.getCustomerId()).isEqualTo(customerId);
        verify(inventoryService).reserveStock(eq(productId), eq(2), any());
        verify(orderRepository).save(any(Order.class));
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when customer not found")
    void createOrder_CustomerNotFound() {
        CreateOrderRequest request = CreateOrderRequest.builder()
                .customerId(customerId)
                .items(List.of(OrderItemRequest.builder()
                        .productId(productId)
                        .quantity(2)
                        .build()))
                .build();

        when(customerRepository.findById(customerId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.createOrder(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Customer");
    }

    @Test
    @DisplayName("Should get order by ID successfully")
    void getOrderById_Success() {
        when(orderRepository.findByIdWithItemsAndPayments(orderId)).thenReturn(Optional.of(order));
        when(orderMapper.toResponse(order)).thenReturn(orderResponse);

        OrderResponse result = orderService.getOrderById(orderId);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(orderId);
        assertThat(result.getOrderNumber()).isEqualTo("ORD-12345");
    }

    @Test
    @DisplayName("Should update order status from PENDING to CONFIRMED")
    void updateOrderStatus_PendingToConfirmed() {
        UpdateOrderStatusRequest request = UpdateOrderStatusRequest.builder()
                .status(OrderStatus.CONFIRMED)
                .build();

        when(orderRepository.findByIdWithItems(orderId)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenReturn(order);
        when(orderMapper.toResponse(any(Order.class))).thenReturn(orderResponse);

        OrderResponse result = orderService.updateOrderStatus(orderId, request);

        assertThat(result).isNotNull();
        verify(orderRepository).save(any(Order.class));
    }

    @Test
    @DisplayName("Should throw InvalidOrderStateException for invalid transition")
    void updateOrderStatus_InvalidTransition() {
        order.setStatus(OrderStatus.DELIVERED);
        UpdateOrderStatusRequest request = UpdateOrderStatusRequest.builder()
                .status(OrderStatus.PENDING)
                .build();

        when(orderRepository.findByIdWithItems(orderId)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.updateOrderStatus(orderId, request))
                .isInstanceOf(InvalidOrderStateException.class);
    }

    @Test
    @DisplayName("Should cancel order and release inventory")
    void cancelOrder_Success() {
        OrderItem orderItem = new OrderItem();
        orderItem.setProduct(product);
        orderItem.setQuantity(2);
        order.setOrderItems(List.of(orderItem));

        when(orderRepository.findByIdWithItems(orderId)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenReturn(order);
        when(orderMapper.toResponse(any(Order.class))).thenReturn(orderResponse);

        orderService.cancelOrder(orderId, "Customer requested");

        verify(inventoryService).releaseStock(eq(productId), eq(2), eq(orderId));
    }
}
