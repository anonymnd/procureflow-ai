package com.project.app.supplier.mapper;

import com.project.app.supplier.DTO.SupplierDtoRequest;
import com.project.app.supplier.DTO.SupplierDtoResponse;
import com.project.app.supplier.model.Supplier;

public class SupplierMapper {

    public static Supplier toEntity(SupplierDtoRequest supplierDtoRequest) {
        return new Supplier(supplierDtoRequest.name());
    }

    public static SupplierDtoResponse toDto(Supplier supplier) {
        return new SupplierDtoResponse(supplier.getId(), supplier.getName());
    }

}
