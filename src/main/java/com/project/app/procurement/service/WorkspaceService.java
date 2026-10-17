package com.project.app.procurement.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import com.project.app.identity.security.AccountPrincipal;
import com.project.app.procurement.api.WorkspaceViews.WorkspacePageView;
import com.project.app.procurement.api.WorkspaceViews.WorkspaceRowView;
import com.project.app.procurement.api.WorkspaceViews.WorkspaceStatusCountView;
import com.project.app.procurement.repository.WorkspaceQueryRepository;
import com.project.app.procurement.repository.WorkspaceQueryRepository.Row;
import com.project.app.procurement.repository.WorkspaceQueryRepository.StatusCount;

@Service
public class WorkspaceService {
    public enum Kind {
        RFQS(List.of("DRAFT", "SENT", "AWARDED")),
        INVITATIONS(List.of("INVITED", "DRAFT", "SUBMITTED", "ACCEPTED")),
        ORDERS(List.of("CREATED", "CONFIRMED", "PROCESSING", "DISPATCHED", "RECEIVED"));
        final List<String> statuses;
        Kind(List<String> statuses) { this.statuses = statuses; }
    }

    private final WorkspaceQueryRepository queries;
    public WorkspaceService(WorkspaceQueryRepository queries) { this.queries = queries; }

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public WorkspacePageView workspace(AccountPrincipal principal, String kindParam, Integer pageParam, Integer sizeParam, String qParam, String statusParam) {
        Kind kind = parseKind(kindParam);
        authorize(principal, kind);
        int page = parsePage(pageParam);
        int size = parseSize(sizeParam);
        String q = parseQuery(qParam);
        String status = parseStatus(kind, statusParam);
        String qLike = WorkspaceQueryRepository.likePattern(q);
        long org = principal.getOrganizationId();
        int offset = page * size;

        List<Row> rows;
        List<StatusCount> counts;
        long total;
        switch (kind) {
            case RFQS -> {
                rows = queries.rfqRows(org, status, qLike, size, offset);
                counts = queries.rfqCounts(org, qLike);
                total = queries.rfqTotal(org, status, qLike);
            }
            case INVITATIONS -> {
                rows = queries.invitationRows(org, status, qLike, size, offset);
                counts = queries.invitationCounts(org, qLike);
                total = queries.invitationTotal(org, status, qLike);
            }
            default -> {
                boolean buyer = principal.isBuyerGrant(), supplier = principal.isSupplierGrant();
                rows = queries.orderRows(org, buyer, supplier, status, qLike, size, offset);
                counts = queries.orderCounts(org, buyer, supplier, qLike);
                total = queries.orderTotal(org, buyer, supplier, status, qLike);
            }
        }
        return new WorkspacePageView(kind.name(), page, size, total,
                rows.stream().map(r -> new WorkspaceRowView(r.id(), r.title(), r.status(), r.counterparty())).toList(),
                zeroFilled(kind, counts));
    }

    private List<WorkspaceStatusCountView> zeroFilled(Kind kind, List<StatusCount> counts) {
        Map<String, Long> byStatus = new HashMap<>();
        for (StatusCount c : counts) byStatus.put(c.status(), c.count());
        return kind.statuses.stream().map(s -> new WorkspaceStatusCountView(s, byStatus.getOrDefault(s, 0L))).toList();
    }

    private Kind parseKind(String raw) {
        if (raw == null) throw ProcurementException.badRequest("kind is required.");
        try { return Kind.valueOf(raw); }
        catch (IllegalArgumentException e) { throw ProcurementException.badRequest("kind must be RFQS, INVITATIONS or ORDERS."); }
    }

    private void authorize(AccountPrincipal principal, Kind kind) {
        boolean ok = switch (kind) {
            case RFQS -> principal.isBuyerGrant();
            case INVITATIONS -> principal.isSupplierGrant();
            case ORDERS -> principal.isBuyerGrant() || principal.isSupplierGrant();
        };
        if (!ok) throw new ProcurementException(HttpStatus.FORBIDDEN, "ACCESS_DENIED", "You do not have the grant required for this workspace queue.");
    }

    private int parsePage(Integer raw) {
        int page = raw == null ? 0 : raw;
        if (page < 0 || page > 1_000_000) throw ProcurementException.badRequest("page must be between 0 and 1000000.");
        return page;
    }

    private int parseSize(Integer raw) {
        int size = raw == null ? 20 : raw;
        if (size < 1 || size > 100) throw ProcurementException.badRequest("size must be between 1 and 100.");
        return size;
    }

    private String parseQuery(String raw) {
        if (raw == null) return null;
        String trimmed = raw.trim();
        if (trimmed.isEmpty()) return null;
        if (trimmed.codePointCount(0, trimmed.length()) > 160) throw ProcurementException.badRequest("q must be at most 160 characters.");
        return trimmed;
    }

    private String parseStatus(Kind kind, String raw) {
        if (raw == null || raw.isEmpty()) return null;
        if (!kind.statuses.contains(raw)) throw ProcurementException.badRequest("status is not valid for this kind.");
        return raw;
    }
}
