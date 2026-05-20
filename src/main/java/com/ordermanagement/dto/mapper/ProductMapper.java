package com.ordermanagement.dto.mapper;

import com.ordermanagement.domain.entity.Product;
import com.ordermanagement.dto.request.CreateProductRequest;
import com.ordermanagement.dto.response.ProductResponse;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface ProductMapper {

    ProductResponse toResponse(Product product);

    Product toEntity(CreateProductRequest request);

    void updateEntity(CreateProductRequest request, @MappingTarget Product product);
}
