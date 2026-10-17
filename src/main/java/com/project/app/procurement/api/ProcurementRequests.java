package com.project.app.procurement.api;

import java.util.List;

import jakarta.validation.constraints.NotNull;

public final class ProcurementRequests {
    private ProcurementRequests() { }

    public record CreateContact(@NotNull Long supplierOrganizationId, String note) { }

    public record CreateRfq(@NotNull String title) { }

    public record UpdateRfq(@NotNull String title) { }

    public record RfqItemInput(@NotNull String description, Integer quantity, @NotNull String unit) { }

    public record SendRfq(@NotNull List<Long> recipientOrganizationIds) { }
}
