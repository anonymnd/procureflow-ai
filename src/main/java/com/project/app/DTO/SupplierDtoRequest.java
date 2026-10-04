package com.project.app.DTO;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SupplierDtoRequest(@NotNull long id,
        @NotBlank(message = "Name is required") String name) {

}
