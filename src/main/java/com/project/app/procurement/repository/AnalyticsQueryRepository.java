package com.project.app.procurement.repository;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import javax.sql.DataSource;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class AnalyticsQueryRepository {
    public record Row(long id, String rfqTitle, String counterparty, String currency,
            BigDecimal subtotal, Instant receivedAt) {}
    public record CurrencyAmount(String currency, BigDecimal amount) {}

    private static final String MATCH = """
            from purchase_orders o
            join order_events e on e.order_id = o.id and e.target_status = 'RECEIVED'
            where o.status = 'RECEIVED' and e.occurred_at >= :fromTs and e.occurred_at < :toTs
              and ((:side = 'BUYER' and o.buyer_organization_id = :org)
                or (:side = 'SUPPLIER' and o.supplier_organization_id = :org))
            """;
    private final NamedParameterJdbcTemplate jdbc;

    public AnalyticsQueryRepository(DataSource dataSource) {
        this.jdbc = new NamedParameterJdbcTemplate(dataSource);
    }

    public MapSqlParameterSource filters(long org, String side, Instant from, Instant to) {
        return new MapSqlParameterSource("org", org).addValue("side", side)
                .addValue("fromTs", Timestamp.from(from)).addValue("toTs", Timestamp.from(to));
    }

    public List<Row> rows(MapSqlParameterSource filters, int size, int offset) {
        return jdbc.query("""
                select o.id, o.rfq_title, case when :side = 'BUYER' then o.supplier_display_name
                    else o.buyer_display_name end as counterparty, o.currency,
                    (select sum(i.line_total) from purchase_order_items i where i.order_id = o.id) as subtotal,
                    e.occurred_at
                """ + MATCH + " order by e.occurred_at desc, o.id desc limit :size offset :offset",
                new MapSqlParameterSource(filters.getValues()).addValue("size", size).addValue("offset", offset),
                (rs, n) -> new Row(rs.getLong("id"), rs.getString("rfq_title"),
                        rs.getString("counterparty"), rs.getString("currency"), rs.getBigDecimal("subtotal"),
                        rs.getTimestamp("occurred_at").toInstant()));
    }

    public long total(MapSqlParameterSource filters) {
        return jdbc.queryForObject("select count(*) " + MATCH, filters, Long.class);
    }

    public List<CurrencyAmount> amounts(MapSqlParameterSource filters) {
        return jdbc.query("with matched as (select o.id, o.currency " + MATCH + ") " + """
                select matched.currency, sum(i.line_total) as amount
                from matched join purchase_order_items i on i.order_id = matched.id
                group by matched.currency
                """,
                filters, (rs, n) -> new CurrencyAmount(rs.getString("currency"), rs.getBigDecimal("amount")));
    }
}
