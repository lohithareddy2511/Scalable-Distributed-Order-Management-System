package com.ordermanagement.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ordermanagement.dto.request.CreateCustomerRequest;
import com.ordermanagement.dto.response.CustomerResponse;
import com.ordermanagement.dto.response.PagedResponse;
import com.ordermanagement.exception.ResourceNotFoundException;
import com.ordermanagement.service.CustomerService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CustomerController.class)
class CustomerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CustomerService customerService;

    @Test
    @DisplayName("POST /v1/customers - should create customer")
    void createCustomer_Success() throws Exception {
        CreateCustomerRequest request = CreateCustomerRequest.builder()
                .firstName("John")
                .lastName("Doe")
                .email("john@example.com")
                .phone("+1234567890")
                .build();

        CustomerResponse response = CustomerResponse.builder()
                .id(UUID.randomUUID())
                .firstName("John")
                .lastName("Doe")
                .email("john@example.com")
                .phone("+1234567890")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(customerService.createCustomer(any())).thenReturn(response);

        mockMvc.perform(post("/v1/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.firstName").value("John"))
                .andExpect(jsonPath("$.data.email").value("john@example.com"));
    }

    @Test
    @DisplayName("POST /v1/customers - should return 400 for invalid request")
    void createCustomer_ValidationError() throws Exception {
        CreateCustomerRequest request = CreateCustomerRequest.builder()
                .firstName("")  // Invalid: blank
                .lastName("")   // Invalid: blank
                .email("invalid-email") // Invalid: not a valid email
                .build();

        mockMvc.perform(post("/v1/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("GET /v1/customers/{id} - should return customer")
    void getCustomerById_Success() throws Exception {
        UUID id = UUID.randomUUID();
        CustomerResponse response = CustomerResponse.builder()
                .id(id)
                .firstName("John")
                .lastName("Doe")
                .email("john@example.com")
                .build();

        when(customerService.getCustomerById(id)).thenReturn(response);

        mockMvc.perform(get("/v1/customers/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(id.toString()));
    }

    @Test
    @DisplayName("GET /v1/customers/{id} - should return 404 when not found")
    void getCustomerById_NotFound() throws Exception {
        UUID id = UUID.randomUUID();
        when(customerService.getCustomerById(id))
                .thenThrow(new ResourceNotFoundException("Customer", "id", id));

        mockMvc.perform(get("/v1/customers/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("GET /v1/customers - should return paginated list")
    void getAllCustomers_Success() throws Exception {
        PagedResponse<CustomerResponse> pagedResponse = PagedResponse.<CustomerResponse>builder()
                .content(List.of(CustomerResponse.builder()
                        .id(UUID.randomUUID())
                        .firstName("John")
                        .lastName("Doe")
                        .email("john@example.com")
                        .build()))
                .page(0)
                .size(20)
                .totalElements(1)
                .totalPages(1)
                .last(true)
                .build();

        when(customerService.getAllCustomers(0, 20, "createdAt", "DESC")).thenReturn(pagedResponse);

        mockMvc.perform(get("/v1/customers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalElements").value(1));
    }

    @Test
    @DisplayName("DELETE /v1/customers/{id} - should delete customer")
    void deleteCustomer_Success() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(delete("/v1/customers/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}
