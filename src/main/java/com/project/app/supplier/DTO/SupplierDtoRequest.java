package com.project.app.supplier.DTO;

import jakarta.validation.constraints.NotBlank;

public record SupplierDtoRequest(@NotBlank(message = "Name is required") String name) {

}
