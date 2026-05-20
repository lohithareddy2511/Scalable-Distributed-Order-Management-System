package com.ordermanagement.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ordermanagement.domain.enums.OrderStatus;
import com.ordermanagement.dto.request.CreateOrderRequest;
import com.ordermanagement.dto.request.OrderItemRequest;
import com.ordermanagement.dto.response.OrderResponse;
import com.ordermanagement.service.OrderService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(OrderController.class)
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private OrderService orderService;

    @Test
    @DisplayName("POST /v1/orders - should create order")
    void createOrder_Success() throws Exception {
        UUID customerId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();

        CreateOrderRequest request = CreateOrderRequest.builder()
                .customerId(customerId)
                .items(List.of(OrderItemRequest.builder()
                        .productId(productId)
                        .quantity(2)
                        .build()))
                .shippingCity("New York")
                .shippingCountry("US")
                .build();

        OrderResponse response = OrderResponse.builder()
                .id(UUID.randomUUID())
                .orderNumber("ORD-12345")
                .customerId(customerId)
                .customerName("John Doe")
                .status(OrderStatus.PENDING)
                .totalAmount(new BigDecimal("50.00"))
                .build();

        when(orderService.createOrder(any())).thenReturn(response);

        mockMvc.perform(post("/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.orderNumber").value("ORD-12345"))
                .andExpect(jsonPath("$.data.status").value("PENDING"));
    }

    @Test
    @DisplayName("POST /v1/orders - should return 400 for empty items")
    void createOrder_EmptyItems() throws Exception {
        CreateOrderRequest request = CreateOrderRequest.builder()
                .customerId(UUID.randomUUID())
                .items(List.of())
                .build();

        mockMvc.perform(post("/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("GET /v1/orders/{id} - should return order")
    void getOrderById_Success() throws Exception {
        UUID orderId = UUID.randomUUID();
        OrderResponse response = OrderResponse.builder()
                .id(orderId)
                .orderNumber("ORD-12345")
                .status(OrderStatus.PENDING)
                .totalAmount(new BigDecimal("100.00"))
                .build();

        when(orderService.getOrderById(orderId)).thenReturn(response);

        mockMvc.perform(get("/v1/orders/{id}", orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.orderNumber").value("ORD-12345"));
    }

    @Test
    @DisplayName("POST /v1/orders/{id}/cancel - should cancel order")
    void cancelOrder_Success() throws Exception {
        UUID orderId = UUID.randomUUID();
        OrderResponse response = OrderResponse.builder()
                .id(orderId)
                .orderNumber("ORD-12345")
                .status(OrderStatus.CANCELLED)
                .build();

        when(orderService.cancelOrder(any(), any())).thenReturn(response);

        mockMvc.perform(post("/v1/orders/{id}/cancel", orderId)
                        .param("reason", "Customer requested"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("CANCELLED"));
    }
}
