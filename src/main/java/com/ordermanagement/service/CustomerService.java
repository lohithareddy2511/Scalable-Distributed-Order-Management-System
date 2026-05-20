package com.ordermanagement.service;

import com.ordermanagement.domain.entity.Customer;
import com.ordermanagement.dto.mapper.CustomerMapper;
import com.ordermanagement.dto.request.CreateCustomerRequest;
import com.ordermanagement.dto.request.UpdateCustomerRequest;
import com.ordermanagement.dto.response.CustomerResponse;
import com.ordermanagement.dto.response.PagedResponse;
import com.ordermanagement.exception.DuplicateResourceException;
import com.ordermanagement.exception.ResourceNotFoundException;
import com.ordermanagement.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final CustomerMapper customerMapper;

    @Transactional
    public CustomerResponse createCustomer(CreateCustomerRequest request) {
        log.info("Creating customer with email: {}", request.getEmail());

        if (customerRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Customer", "email", request.getEmail());
        }

        Customer customer = customerMapper.toEntity(request);
        Customer saved = customerRepository.save(customer);

        log.info("Customer created with ID: {}", saved.getId());
        return customerMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public CustomerResponse getCustomerById(UUID id) {
        log.debug("Fetching customer with ID: {}", id);
        Customer customer = findCustomerOrThrow(id);
        return customerMapper.toResponse(customer);
    }

    @Transactional(readOnly = true)
    public CustomerResponse getCustomerByEmail(String email) {
        log.debug("Fetching customer with email: {}", email);
        Customer customer = customerRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Customer", "email", email));
        return customerMapper.toResponse(customer);
    }

    @Transactional(readOnly = true)
    public PagedResponse<CustomerResponse> getAllCustomers(int page, int size, String sortBy, String direction) {
        log.debug("Fetching customers - page: {}, size: {}", page, size);
        Sort sort = Sort.by(Sort.Direction.fromString(direction), sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<Customer> customerPage = customerRepository.findAll(pageable);
        return buildPagedResponse(customerPage);
    }

    @Transactional(readOnly = true)
    public PagedResponse<CustomerResponse> searchCustomers(String search, int page, int size) {
        log.debug("Searching customers with term: {}", search);
        Pageable pageable = PageRequest.of(page, size);
        Page<Customer> customerPage = customerRepository.searchCustomers(search, pageable);
        return buildPagedResponse(customerPage);
    }

    @Transactional
    public CustomerResponse updateCustomer(UUID id, UpdateCustomerRequest request) {
        log.info("Updating customer with ID: {}", id);
        Customer customer = findCustomerOrThrow(id);

        if (request.getFirstName() != null) customer.setFirstName(request.getFirstName());
        if (request.getLastName() != null) customer.setLastName(request.getLastName());
        if (request.getEmail() != null && !request.getEmail().equals(customer.getEmail())) {
            if (customerRepository.existsByEmail(request.getEmail())) {
                throw new DuplicateResourceException("Customer", "email", request.getEmail());
            }
            customer.setEmail(request.getEmail());
        }
        if (request.getPhone() != null) customer.setPhone(request.getPhone());
        if (request.getAddressLine1() != null) customer.setAddressLine1(request.getAddressLine1());
        if (request.getAddressLine2() != null) customer.setAddressLine2(request.getAddressLine2());
        if (request.getCity() != null) customer.setCity(request.getCity());
        if (request.getState() != null) customer.setState(request.getState());
        if (request.getZipCode() != null) customer.setZipCode(request.getZipCode());
        if (request.getCountry() != null) customer.setCountry(request.getCountry());

        Customer updated = customerRepository.save(customer);
        log.info("Customer updated with ID: {}", updated.getId());
        return customerMapper.toResponse(updated);
    }

    @Transactional
    public void deleteCustomer(UUID id) {
        log.info("Deleting customer with ID: {}", id);
        Customer customer = findCustomerOrThrow(id);
        customerRepository.delete(customer);
        log.info("Customer deleted with ID: {}", id);
    }

    private Customer findCustomerOrThrow(UUID id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer", "id", id));
    }

    private PagedResponse<CustomerResponse> buildPagedResponse(Page<Customer> page) {
        return PagedResponse.<CustomerResponse>builder()
                .content(page.getContent().stream().map(customerMapper::toResponse).toList())
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .build();
    }
}
