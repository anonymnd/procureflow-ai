package com.project.app.procurement.api;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.project.app.procurement.service.ProcurementException;

public final class QuotationRequests {
    private QuotationRequests() { }
    public record PriceInput(Long rfqItemId, String unitPrice) { }
    public record DraftInput(String currency, Instant validUntil, String terms, List<PriceInput> items) {
        // A delegating creator keeps the DTO typed while rejecting coercion and unknown fields at the boundary.
        @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
        public static DraftInput parse(Map<String, Object> fields) {
            if (fields == null || !Set.of("currency", "validUntil", "terms", "items").containsAll(fields.keySet()))
                throw ProcurementException.badRequest("Unknown quotation field.");
            if (!(fields.get("currency") instanceof String currency)
                    || !(fields.get("validUntil") instanceof String validity)
                    || !(fields.get("items") instanceof List<?> lines))
                throw ProcurementException.badRequest("currency, validUntil and items are required.");
            Object rawTerms = fields.get("terms");
            if (rawTerms != null && !(rawTerms instanceof String)) throw ProcurementException.badRequest("terms must be text.");
            Instant validUntil;
            try { validUntil = Instant.parse(validity); }
            catch (java.time.format.DateTimeParseException invalid) { throw ProcurementException.badRequest("validUntil must be an ISO-8601 instant."); }
            List<PriceInput> items = new ArrayList<>();
            for (Object raw : lines) {
                if (!(raw instanceof Map<?, ?> line) || !Set.of("rfqItemId", "unitPrice").containsAll(line.keySet())
                        || !(line.get("rfqItemId") instanceof Number id) || !id.toString().matches("[1-9][0-9]*")
                        || !(line.get("unitPrice") instanceof String price) || !price.matches("[0-9]+(?:\\.[0-9]+)?"))
                    throw ProcurementException.badRequest("Each item requires an integer rfqItemId and a plain decimal string unitPrice.");
                try { items.add(new PriceInput(Long.valueOf(id.toString()), price)); }
                catch (NumberFormatException invalid) { throw ProcurementException.badRequest("Invalid rfqItemId."); }
            }
            return new DraftInput(currency, validUntil, (String) rawTerms, List.copyOf(items));
        }
    }
}
