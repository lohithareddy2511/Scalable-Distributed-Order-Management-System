package com.ordermanagement.service;

import com.ordermanagement.domain.entity.Customer;
import com.ordermanagement.dto.mapper.CustomerMapper;
import com.ordermanagement.dto.request.CreateCustomerRequest;
import com.ordermanagement.dto.request.UpdateCustomerRequest;
import com.ordermanagement.dto.response.CustomerResponse;
import com.ordermanagement.exception.DuplicateResourceException;
import com.ordermanagement.exception.ResourceNotFoundException;
import com.ordermanagement.repository.CustomerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private CustomerMapper customerMapper;

    @InjectMocks
    private CustomerService customerService;

    private Customer customer;
    private CustomerResponse customerResponse;
    private CreateCustomerRequest createRequest;
    private UUID customerId;

    @BeforeEach
    void setUp() {
        customerId = UUID.randomUUID();

        customer = new Customer();
        customer.setId(customerId);
        customer.setFirstName("John");
        customer.setLastName("Doe");
        customer.setEmail("john.doe@example.com");
        customer.setPhone("+1234567890");
        customer.setCreatedAt(LocalDateTime.now());
        customer.setUpdatedAt(LocalDateTime.now());

        customerResponse = CustomerResponse.builder()
                .id(customerId)
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@example.com")
                .phone("+1234567890")
                .build();

        createRequest = CreateCustomerRequest.builder()
                .firstName("John")
                .lastName("Doe")
                .email("john.doe@example.com")
                .phone("+1234567890")
                .build();
    }

    @Test
    @DisplayName("Should create customer successfully")
    void createCustomer_Success() {
        when(customerRepository.existsByEmail(createRequest.getEmail())).thenReturn(false);
        when(customerMapper.toEntity(createRequest)).thenReturn(customer);
        when(customerRepository.save(any(Customer.class))).thenReturn(customer);
        when(customerMapper.toResponse(customer)).thenReturn(customerResponse);

        CustomerResponse result = customerService.createCustomer(createRequest);

        assertThat(result).isNotNull();
        assertThat(result.getEmail()).isEqualTo("john.doe@example.com");
        assertThat(result.getFirstName()).isEqualTo("John");
        verify(customerRepository).save(any(Customer.class));
    }

    @Test
    @DisplayName("Should throw DuplicateResourceException when email already exists")
    void createCustomer_DuplicateEmail() {
        when(customerRepository.existsByEmail(createRequest.getEmail())).thenReturn(true);

        assertThatThrownBy(() -> customerService.createCustomer(createRequest))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("email");
    }

    @Test
    @DisplayName("Should get customer by ID successfully")
    void getCustomerById_Success() {
        when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));
        when(customerMapper.toResponse(customer)).thenReturn(customerResponse);

        CustomerResponse result = customerService.getCustomerById(customerId);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(customerId);
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when customer not found")
    void getCustomerById_NotFound() {
        when(customerRepository.findById(customerId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> customerService.getCustomerById(customerId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Customer");
    }

    @Test
    @DisplayName("Should update customer successfully")
    void updateCustomer_Success() {
        UpdateCustomerRequest updateRequest = UpdateCustomerRequest.builder()
                .firstName("Jane")
                .build();

        when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));
        when(customerRepository.save(any(Customer.class))).thenReturn(customer);
        when(customerMapper.toResponse(customer)).thenReturn(customerResponse);

        CustomerResponse result = customerService.updateCustomer(customerId, updateRequest);

        assertThat(result).isNotNull();
        verify(customerRepository).save(customer);
    }

    @Test
    @DisplayName("Should delete customer successfully")
    void deleteCustomer_Success() {
        when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));

        customerService.deleteCustomer(customerId);

        verify(customerRepository).delete(customer);
    }
}
