package com.project.app.supplier.DTO;

import jakarta.validation.constraints.NotNull;

public record SupplierDtoResponse(@NotNull long id, String name) {
}
