package com.project.app.procurement.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.MultiValueMap;
import com.project.app.identity.security.AccountPrincipal;
import com.project.app.procurement.api.AnalyticsViews.CompletedOrderRow;
import com.project.app.procurement.api.AnalyticsViews.CompletedOrdersPage;
import com.project.app.procurement.api.AnalyticsViews.CurrencyTotal;
import com.project.app.procurement.repository.AnalyticsQueryRepository;

@Service
public class AnalyticsService {
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("uuuu-MM-dd")
            .withResolverStyle(ResolverStyle.STRICT);
    private final AnalyticsQueryRepository queries;
    public AnalyticsService(AnalyticsQueryRepository queries) { this.queries = queries; }

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public CompletedOrdersPage completedOrders(AccountPrincipal principal, MultiValueMap<String, String> params) {
        String side = one(params, "side", true);
        LocalDate from = date(one(params, "from", true));
        LocalDate to = date(one(params, "toExclusive", true));
        int page = integer(one(params, "page", false), 0, 0, 1_000_000);
        int size = integer(one(params, "size", false), 20, 1, 100);
        if (!("BUYER".equals(side) || "SUPPLIER".equals(side)) ||
                !to.isAfter(from) || to.toEpochDay() - from.toEpochDay() > 366) throw invalid();
        if (("BUYER".equals(side) && !principal.isBuyerGrant()) ||
                ("SUPPLIER".equals(side) && !principal.isSupplierGrant())) {
            throw new ProcurementException(HttpStatus.FORBIDDEN, "ACCESS_DENIED", "You do not have the grant required for this analytics side.");
        }
        var filters = queries.filters(principal.getOrganizationId(), side,
                from.atStartOfDay(java.time.ZoneOffset.UTC).toInstant(),
                to.atStartOfDay(java.time.ZoneOffset.UTC).toInstant());
        var rows = queries.rows(filters, size, page * size).stream()
                .map(r -> new CompletedOrderRow(r.id(), r.rfqTitle(), r.counterparty(), r.currency(),
                        amount(r.subtotal()), r.receivedAt())).toList();
        long total = queries.total(filters);
        Map<String, BigDecimal> amounts = queries.amounts(filters).stream()
                .collect(Collectors.toMap(AnalyticsQueryRepository.CurrencyAmount::currency,
                        AnalyticsQueryRepository.CurrencyAmount::amount));
        List<CurrencyTotal> totals = List.of("MAD", "EUR", "USD").stream()
                .map(currency -> new CurrencyTotal(currency, amount(amounts.getOrDefault(currency, BigDecimal.ZERO))))
                .toList();
        return new CompletedOrdersPage(side, from.toString(), to.toString(), "UTC", page, size, total, totals, rows);
    }

    private static String one(MultiValueMap<String, String> params, String name, boolean required) {
        List<String> values = params.get(name);
        if (values == null) { if (required) throw invalid(); return null; }
        if (values.size() != 1 || values.get(0) == null || values.get(0).isBlank()) throw invalid();
        return values.get(0);
    }
    private static LocalDate date(String value) {
        if (value == null || !value.matches("[0-9]{4}-[0-9]{2}-[0-9]{2}")) throw invalid();
        try { return LocalDate.parse(value, DATE); } catch (RuntimeException e) { throw invalid(); }
    }
    private static int integer(String value, int fallback, int minimum, int maximum) {
        if (value == null) return fallback;
        if (!value.matches("[0-9]+")) throw invalid();
        try {
            int parsed = Integer.parseInt(value);
            if (parsed < minimum || parsed > maximum) throw invalid();
            return parsed;
        } catch (NumberFormatException e) { throw invalid(); }
    }
    private static String amount(BigDecimal value) { return value.setScale(2).toPlainString(); }
    private static ProcurementException invalid() {
        return new ProcurementException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Invalid analytics filters.");
    }
}
