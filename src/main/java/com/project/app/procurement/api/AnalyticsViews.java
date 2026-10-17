package com.project.app.procurement.api;

import java.time.Instant;
import java.util.List;

public final class AnalyticsViews {
    private AnalyticsViews() {}
    public record CurrencyTotal(String currency, String amount) {}
    public record CompletedOrderRow(long id, String rfqTitle, String counterparty,
            String currency, String subtotal, Instant receivedAt) {}
    public record CompletedOrdersPage(String side, String from, String toExclusive, String timeZone,
            int page, int size, long total, List<CurrencyTotal> totals, List<CompletedOrderRow> rows) {}
}
