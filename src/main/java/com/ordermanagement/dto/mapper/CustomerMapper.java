package com.ordermanagement.dto.mapper;

import com.ordermanagement.domain.entity.Customer;
import com.ordermanagement.dto.request.CreateCustomerRequest;
import com.ordermanagement.dto.response.CustomerResponse;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface CustomerMapper {

    CustomerResponse toResponse(Customer customer);

    Customer toEntity(CreateCustomerRequest request);

    void updateEntity(CreateCustomerRequest request, @MappingTarget Customer customer);
}
