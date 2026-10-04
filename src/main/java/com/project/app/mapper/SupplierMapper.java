package com.project.app.mapper;

import com.project.app.DTO.SupplierDtoRequest;
import com.project.app.DTO.SupplierDtoResponse;
import com.project.app.model.Supplier;

public class SupplierMapper {
    
    public static Supplier toEntity(SupplierDtoRequest supplierDtoRequest) {
        return new Supplier(supplierDtoRequest.name());
    }

    public static SupplierDtoResponse toDto(Supplier supplier) {
        return new SupplierDtoResponse(supplier.getName());
    }

}
