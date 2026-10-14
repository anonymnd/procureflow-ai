package com.project.app.procurement.repository;

import java.util.List;
import javax.sql.DataSource;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

/** Parameterized, database-paginated queue projections over the existing rfqs/rfq_invitations/quotations/purchase_orders tables. No new table/entity; reused by WorkspaceService inside one REPEATABLE_READ transaction so rows/counts/total share a snapshot. */
@Repository
public class WorkspaceQueryRepository {
    public record Row(long id, String title, String status, String counterparty) {}
    public record StatusCount(String status, long count) {}

    private final NamedParameterJdbcTemplate jdbc;
    public WorkspaceQueryRepository(DataSource dataSource) { this.jdbc = new NamedParameterJdbcTemplate(dataSource); }

    public static String likePattern(String q) {
        if (q == null) return null;
        StringBuilder escaped = new StringBuilder("%");
        for (int i = 0; i < q.length(); i++) {
            char c = q.charAt(i);
            if (c == '%' || c == '_' || c == '\\') escaped.append('\\');
            escaped.append(c);
        }
        return escaped.append('%').toString();
    }

    private MapSqlParameterSource base(long org, String status, String qLike) {
        return new MapSqlParameterSource().addValue("org", org).addValue("status", status).addValue("qLike", qLike);
    }

    public List<Row> rfqRows(long org, String status, String qLike, int size, int offset) {
        return jdbc.query("""
            select id, title, status, cast(null as varchar) as counterparty from rfqs
            where organization_id = :org and (cast(:status as varchar) is null or status = :status) and (cast(:qLike as varchar) is null or lower(title) like lower(:qLike) escape '\\')
            order by id desc limit :size offset :offset""",
            base(org, status, qLike).addValue("size", size).addValue("offset", offset),
            (rs, i) -> new Row(rs.getLong("id"), rs.getString("title"), rs.getString("status"), rs.getString("counterparty")));
    }
    public List<StatusCount> rfqCounts(long org, String qLike) {
        return jdbc.query("""
            select status, count(*) as n from rfqs
            where organization_id = :org and (cast(:qLike as varchar) is null or lower(title) like lower(:qLike) escape '\\') group by status""",
            base(org, null, qLike), (rs, i) -> new StatusCount(rs.getString("status"), rs.getLong("n")));
    }
    public long rfqTotal(long org, String status, String qLike) {
        return jdbc.queryForObject("""
            select count(*) from rfqs
            where organization_id = :org and (cast(:status as varchar) is null or status = :status) and (cast(:qLike as varchar) is null or lower(title) like lower(:qLike) escape '\\')""",
            base(org, status, qLike), Long.class);
    }

    public List<Row> invitationRows(long org, String status, String qLike, int size, int offset) {
        return jdbc.query("""
            select i.id as id, r.title as title, coalesce(q.status,'INVITED') as status, bo.display_name as counterparty
            from rfq_invitations i join rfqs r on r.id = i.rfq_id join organizations bo on bo.id = r.organization_id
            left join quotations q on q.invitation_id = i.id
            where i.supplier_organization_id = :org and (cast(:status as varchar) is null or coalesce(q.status,'INVITED') = :status)
              and (cast(:qLike as varchar) is null or lower(r.title) like lower(:qLike) escape '\\')
            order by i.id desc limit :size offset :offset""",
            base(org, status, qLike).addValue("size", size).addValue("offset", offset),
            (rs, i) -> new Row(rs.getLong("id"), rs.getString("title"), rs.getString("status"), rs.getString("counterparty")));
    }
    public List<StatusCount> invitationCounts(long org, String qLike) {
        return jdbc.query("""
            select coalesce(q.status,'INVITED') as status, count(*) as n
            from rfq_invitations i join rfqs r on r.id = i.rfq_id left join quotations q on q.invitation_id = i.id
            where i.supplier_organization_id = :org and (cast(:qLike as varchar) is null or lower(r.title) like lower(:qLike) escape '\\')
            group by coalesce(q.status,'INVITED')""",
            base(org, null, qLike), (rs, i) -> new StatusCount(rs.getString("status"), rs.getLong("n")));
    }
    public long invitationTotal(long org, String status, String qLike) {
        return jdbc.queryForObject("""
            select count(*) from rfq_invitations i join rfqs r on r.id = i.rfq_id left join quotations q on q.invitation_id = i.id
            where i.supplier_organization_id = :org and (cast(:status as varchar) is null or coalesce(q.status,'INVITED') = :status)
              and (cast(:qLike as varchar) is null or lower(r.title) like lower(:qLike) escape '\\')""",
            base(org, status, qLike), Long.class);
    }

    private MapSqlParameterSource orderBase(long org, boolean buyer, boolean supplier, String status, String qLike) {
        return base(org, status, qLike).addValue("buyer", buyer).addValue("supplier", supplier);
    }
    public List<Row> orderRows(long org, boolean buyer, boolean supplier, String status, String qLike, int size, int offset) {
        return jdbc.query("""
            select id, rfq_title as title, status,
              case when buyer_organization_id = :org then supplier_display_name else buyer_display_name end as counterparty
            from purchase_orders
            where ((:buyer = true and buyer_organization_id = :org) or (:supplier = true and supplier_organization_id = :org))
              and (cast(:status as varchar) is null or status = :status) and (cast(:qLike as varchar) is null or lower(rfq_title) like lower(:qLike) escape '\\')
            order by id desc limit :size offset :offset""",
            orderBase(org, buyer, supplier, status, qLike).addValue("size", size).addValue("offset", offset),
            (rs, i) -> new Row(rs.getLong("id"), rs.getString("title"), rs.getString("status"), rs.getString("counterparty")));
    }
    public List<StatusCount> orderCounts(long org, boolean buyer, boolean supplier, String qLike) {
        return jdbc.query("""
            select status, count(*) as n from purchase_orders
            where ((:buyer = true and buyer_organization_id = :org) or (:supplier = true and supplier_organization_id = :org))
              and (cast(:qLike as varchar) is null or lower(rfq_title) like lower(:qLike) escape '\\') group by status""",
            orderBase(org, buyer, supplier, null, qLike), (rs, i) -> new StatusCount(rs.getString("status"), rs.getLong("n")));
    }
    public long orderTotal(long org, boolean buyer, boolean supplier, String status, String qLike) {
        return jdbc.queryForObject("""
            select count(*) from purchase_orders
            where ((:buyer = true and buyer_organization_id = :org) or (:supplier = true and supplier_organization_id = :org))
              and (cast(:status as varchar) is null or status = :status) and (cast(:qLike as varchar) is null or lower(rfq_title) like lower(:qLike) escape '\\')""",
            orderBase(org, buyer, supplier, status, qLike), Long.class);
    }
}
