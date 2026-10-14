package com.project.app.procurement;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.reset;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.sql.Connection;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.*;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import com.project.app.support.Slice0DatabaseGuard;

/** BL-S7-R1 / #24 and BL-S8-R1 / #26: real HTTP/DB acceptance proofs. */
@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("slice0-test")
@ContextConfiguration(initializers = Slice0DatabaseGuard.class)
class WorkspaceIntegrationTest {
    private static String passwordHash;
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired DataSource dataSource;
    @Autowired PasswordEncoder passwords;
    @MockitoSpyBean com.project.app.procurement.repository.WorkspaceQueryRepository queries;
    @MockitoSpyBean com.project.app.procurement.repository.AnalyticsQueryRepository analyticsQueries;
    private MockHttpSession buyer, supplierB, supplierC, dual, outsider, noGrant;
    private long buyerOrg, supplierBOrg, supplierCOrg, dualOrg, outsiderOrg;
    private long rfqAwarded;

    @BeforeEach void fixtures() throws Exception {
        jdbc.update("DELETE FROM order_events");jdbc.update("DELETE FROM purchase_order_items");jdbc.update("DELETE FROM purchase_orders");jdbc.update("UPDATE rfqs SET status='SENT',winning_quotation_id=NULL WHERE status='AWARDED'");
        for (String table : List.of("quotation_items","quotations","rfq_invitations","rfq_items","rfqs","supplier_contacts","marketplace_offerings","marketplace_profiles","identity_tokens","accounts","organizations")) jdbc.update("DELETE FROM " + table);
        if (passwordHash == null) passwordHash = passwords.encode("workspace passphrase 123");
        buyerOrg = organization("Workspace Buyer"); supplierBOrg = organization("Workspace Supplier B"); supplierCOrg = organization("Workspace Supplier C");
        dualOrg = organization("Workspace Dual"); outsiderOrg = organization("Workspace Outsider");
        account(buyerOrg, "buyer@workspace.test", true, false);
        account(supplierBOrg, "supplierb@workspace.test", false, true);
        account(supplierCOrg, "supplierc@workspace.test", false, true);
        account(dualOrg, "dual@workspace.test", true, true);
        account(outsiderOrg, "outsider@workspace.test", true, true);
        account(organization("Workspace No Grant"), "nogrant@workspace.test", false, false);
        buyer = login("buyer@workspace.test"); supplierB = login("supplierb@workspace.test"); supplierC = login("supplierc@workspace.test");
        dual = login("dual@workspace.test"); outsider = login("outsider@workspace.test"); noGrant = login("nogrant@workspace.test");

        rfq(buyerOrg, "Alpha one", "DRAFT");
        long rfqSentNoQuote = rfq(buyerOrg, "Beta two", "SENT");
        invite(rfqSentNoQuote, supplierBOrg);
        long rfqWithDraftQuote = rfq(buyerOrg, "Gamma three", "SENT");
        long draftInvitation = invite(rfqWithDraftQuote, supplierBOrg);
        quotation(draftInvitation, "DRAFT", null);
        rfqAwarded = rfq(buyerOrg, "Delta four", "SENT");
        long wonInvitation = invite(rfqAwarded, supplierBOrg), lostInvitation = invite(rfqAwarded, supplierCOrg);
        long wonQuote = quotation(wonInvitation, "ACCEPTED", "MAD");
        quotation(lostInvitation, "SUBMITTED", "MAD");
        jdbc.update("UPDATE rfqs SET status='AWARDED', winning_quotation_id=? WHERE id=?", wonQuote, rfqAwarded);
        order(buyerOrg, supplierBOrg, rfqAwarded, wonQuote, "Delta four", "CREATED");
        rfq(buyerOrg, "50% off_special", "DRAFT");

        long buyerIsDual = rfq(dualOrg, "Dual as buyer", "SENT");
        long dualInvitation = invite(buyerIsDual, supplierBOrg);
        long dualQuote = quotation(dualInvitation, "ACCEPTED", "MAD");
        order(dualOrg, supplierBOrg, buyerIsDual, dualQuote, "Dual as buyer", "CREATED");
        long selfRfq = rfq(dualOrg, "Dual self transaction", "SENT");
        long selfInvitation = invite(selfRfq, dualOrg);
        long selfQuote = quotation(selfInvitation, "ACCEPTED", "MAD");
        order(dualOrg, dualOrg, selfRfq, selfQuote, "Dual self transaction", "CREATED");
    }

    @Test void paginationBoundariesAndInvalidParameters() throws Exception {
        workspace(buyer, "RFQS", 0, 2, null, null).andExpect(jsonPath("$.total").value(5))
                .andExpect(jsonPath("$.rows.length()").value(2)).andExpect(jsonPath("$.rows[0].title").value("50% off_special"));
        workspace(buyer, "RFQS", 2, 2, null, null).andExpect(jsonPath("$.rows.length()").value(1));
        workspace(buyer, "RFQS", 10, 2, null, null).andExpect(jsonPath("$.rows").isEmpty()).andExpect(jsonPath("$.total").value(5));
        bad(workspace(buyer, "WIDGETS", 0, 20, null, null));
        bad(workspace(buyer, "RFQS", -1, 20, null, null));
        bad(workspace(buyer, "RFQS", 0, 0, null, null));
        bad(workspace(buyer, "RFQS", 0, 101, null, null));
        bad(workspace(buyer, "RFQS", 0, 20, null, "BOGUS"));
        bad(workspace(buyer, "RFQS", 0, 20, "q".repeat(161), null));
        mvc.perform(get("/api/procurement/workspace").param("page", "abc").session(buyer)).andExpect(status().isBadRequest());
        mvc.perform(get("/api/procurement/workspace").session(buyer)).andExpect(status().isBadRequest());
    }

    @Test void perKindRowProjectionAndCounterparty() throws Exception {
        workspace(buyer, "RFQS", 0, 20, null, null).andExpect(jsonPath("$.rows[?(@.title=='Alpha one')].counterparty").value(org.hamcrest.Matchers.contains((Object) null)));
        workspace(supplierB, "INVITATIONS", 0, 20, null, null)
                .andExpect(jsonPath("$.rows[?(@.title=='Beta two')].status").value("INVITED"))
                .andExpect(jsonPath("$.rows[?(@.title=='Beta two')].counterparty").value("Workspace Buyer"))
                .andExpect(jsonPath("$.rows[?(@.title=='Gamma three')].status").value("DRAFT"));
        workspace(supplierC, "INVITATIONS", 0, 20, null, null)
                .andExpect(jsonPath("$.rows[?(@.title=='Delta four')].status").value("SUBMITTED"));
        workspace(buyer, "ORDERS", 0, 20, null, null).andExpect(jsonPath("$.rows[?(@.title=='Delta four')].counterparty").value("Workspace Supplier B"));
        workspace(supplierB, "ORDERS", 0, 20, null, null).andExpect(jsonPath("$.rows[?(@.title=='Delta four')].counterparty").value("Workspace Buyer"));
        workspace(dual, "ORDERS", 0, 20, null, null)
                .andExpect(jsonPath("$.rows[?(@.title=='Dual as buyer')].counterparty").value("Workspace Supplier B"))
                .andExpect(jsonPath("$.rows[?(@.title=='Dual self transaction')].counterparty").value("Workspace Dual"))
                .andExpect(jsonPath("$.total").value(2));
        assertEquals(1, jdbc.queryForList("SELECT 1 FROM purchase_orders WHERE rfq_title='Dual self transaction'").size());
    }

    @Test void authorizationBoundariesAndForeignExclusion() throws Exception {
        mvc.perform(get("/api/procurement/workspace").param("kind", "RFQS")).andExpect(status().isUnauthorized());
        workspace(supplierB, "RFQS", 0, 20, null, null).andExpect(status().isForbidden());
        workspace(buyer, "INVITATIONS", 0, 20, null, null).andExpect(status().isForbidden());
        for (String kind : List.of("RFQS", "INVITATIONS", "ORDERS"))
            workspace(noGrant, kind, 0, 20, null, null).andExpect(status().isForbidden());
        workspace(outsider, "RFQS", 0, 20, null, null).andExpect(jsonPath("$.total").value(0)).andExpect(jsonPath("$.rows").isEmpty());
        workspace(outsider, "INVITATIONS", 0, 20, null, null).andExpect(jsonPath("$.total").value(0));
        String supplierCJson = workspace(supplierC, "INVITATIONS", 0, 20, null, null).andReturn().getResponse().getContentAsString();
        assertFalse(supplierCJson.contains("@workspace.test")); assertFalse(supplierCJson.contains("password"));
    }

    @Test void searchLiteralWildcardsAndCountReconciliation() throws Exception {
        workspace(buyer, "RFQS", 0, 20, "50% off_special", null).andExpect(jsonPath("$.total").value(1)).andExpect(jsonPath("$.rows[0].title").value("50% off_special"));
        workspace(buyer, "RFQS", 0, 20, "50X offXspecial", null).andExpect(jsonPath("$.total").value(0));
        workspace(buyer, "RFQS", 0, 20, "ALPHA", null).andExpect(jsonPath("$.total").value(1));
        var full = workspace(buyer, "RFQS", 0, 20, null, null).andExpect(status().isOk()).andReturn();
        long sum = ((java.util.List<?>) com.jayway.jsonpath.JsonPath.read(full.getResponse().getContentAsString(), "$.statusCounts[*].count")).stream().mapToLong(v -> ((Number) v).longValue()).sum();
        long total = ((Number) com.jayway.jsonpath.JsonPath.read(full.getResponse().getContentAsString(), "$.total")).longValue();
        assertEquals(total, sum);
        var filtered = workspace(buyer, "RFQS", 0, 20, null, "SENT").andExpect(status().isOk()).andReturn();
        long filteredTotal = ((Number) com.jayway.jsonpath.JsonPath.read(filtered.getResponse().getContentAsString(), "$.total")).longValue();
        List<?> sentCounts = com.jayway.jsonpath.JsonPath.read(full.getResponse().getContentAsString(), "$.statusCounts[?(@.status=='SENT')].count");
        long sentCount = ((Number) sentCounts.get(0)).longValue();
        assertEquals(sentCount, filteredTotal);
    }

    @Test void workspaceRowsAndCountsShareOneSnapshotDuringStatusChange() throws Exception {
        long alphaId = jdbc.queryForObject("SELECT id FROM rfqs WHERE organization_id=? AND title='Alpha one'", Long.class, buyerOrg);
        CountDownLatch beforeCounts = new CountDownLatch(1);
        CountDownLatch releaseCounts = new CountDownLatch(1);
        doAnswer(call -> {
            beforeCounts.countDown();
            assertTrue(releaseCounts.await(10, TimeUnit.SECONDS));
            return call.callRealMethod();
        }).when(queries).rfqCounts(eq(buyerOrg), isNull());
        var pool = Executors.newSingleThreadExecutor();
        try {
            var result = pool.submit(() -> workspace(buyer, "RFQS", 0, 20, null, null).andReturn().getResponse().getContentAsString());
            assertTrue(beforeCounts.await(10, TimeUnit.SECONDS));
            jdbc.update("UPDATE rfqs SET status='SENT' WHERE id=?", alphaId);
            releaseCounts.countDown();
            String oldSnapshot = result.get(10, TimeUnit.SECONDS);
            List<?> oldAlphaStatus = com.jayway.jsonpath.JsonPath.read(oldSnapshot, "$.rows[?(@.title=='Alpha one')].status");
            assertEquals("DRAFT", oldAlphaStatus.get(0));
            List<?> oldDraft = com.jayway.jsonpath.JsonPath.read(oldSnapshot, "$.statusCounts[?(@.status=='DRAFT')].count");
            assertEquals(2L, ((Number) oldDraft.get(0)).longValue());
            workspace(buyer, "RFQS", 0, 20, null, null)
                    .andExpect(jsonPath("$.rows[?(@.title=='Alpha one')].status").value("SENT"))
                    .andExpect(jsonPath("$.statusCounts[?(@.status=='DRAFT')].count").value(1));
        } finally {
            releaseCounts.countDown();
            pool.shutdownNow();
            reset(queries);
        }
    }

    @Test void repeatableReadSnapshotIsolatesConcurrentInsert() throws Exception {
        try (Connection snapshot = dataSource.getConnection()) {
            snapshot.setAutoCommit(false);
            snapshot.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);
            long before;
            try (var statement = snapshot.createStatement(); var rs = statement.executeQuery("SELECT count(*) FROM rfqs WHERE organization_id=" + buyerOrg)) { rs.next(); before = rs.getLong(1); }
            jdbc.update("INSERT INTO rfqs(organization_id,title,status) VALUES (?,?,?)", buyerOrg, "Inserted mid-snapshot", "DRAFT");
            long during;
            try (var statement = snapshot.createStatement(); var rs = statement.executeQuery("SELECT count(*) FROM rfqs WHERE organization_id=" + buyerOrg)) { rs.next(); during = rs.getLong(1); }
            assertEquals(before, during, "REPEATABLE_READ must not observe the concurrently committed insert within the same transaction");
            snapshot.commit();
        }
        workspace(buyer, "RFQS", 0, 20, null, null).andExpect(jsonPath("$.total").value(6));
    }

    @Test void completedAnalyticsUtcTotalsPagingAndSelectedSide() throws Exception {
        long main = orderId("Delta four"), self = orderId("Dual self transaction");
        complete(main, "2026-10-12T00:00:00Z", "10.25");
        complete(self, "2026-10-12T23:59:59Z", "3.50");
        jdbc.update("UPDATE purchase_orders SET buyer_display_name='Captured buyer',supplier_display_name='Captured supplier' WHERE id=?", self);
        analytics(buyer, "BUYER", "2026-10-12", "2026-10-13", 0, 1)
                .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.rows[0].counterparty").value("Workspace Supplier B"))
                .andExpect(jsonPath("$.rows[0].subtotal").value("10.25"))
                .andExpect(jsonPath("$.totals[0].amount").value("10.25"))
                .andExpect(jsonPath("$.totals[1].amount").value("0.00"));
        analytics(dual, "BUYER", "2026-10-12", "2026-10-13", 1, 1)
                .andExpect(jsonPath("$.total").value(1)).andExpect(jsonPath("$.rows").isEmpty())
                .andExpect(jsonPath("$.totals[0].amount").value("3.50"));
        analytics(dual, "BUYER", "2026-10-12", "2026-10-13", 0, 1)
                .andExpect(jsonPath("$.rows[0].counterparty").value("Captured supplier"));
        analytics(dual, "SUPPLIER", "2026-10-12", "2026-10-13", 0, 20)
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.rows[0].counterparty").value("Captured buyer"));
        analytics(buyer, "BUYER", "2026-10-13", "2026-10-14", 0, 20)
                .andExpect(jsonPath("$.total").value(0)).andExpect(jsonPath("$.rows").isEmpty())
                .andExpect(jsonPath("$.totals[2].amount").value("0.00"));
    }

    @Test void completedAnalyticsStableTiesAndForeignAmounts() throws Exception {
        long first = orderId("Delta four"), laterId = extraOrder("Same receipt instant", "MAD");
        complete(first, "2026-10-12T00:00:00Z", "10.25");
        complete(laterId, "2026-10-12T00:00:00Z", "0.75");
        complete(orderId("Dual self transaction"), "2026-10-12T00:00:00Z", "999.99");
        extraOrder("Accepted but not received", "USD");
        jdbc.update("UPDATE purchase_orders SET accepted_at='2020-01-01T00:00:00Z' WHERE id=?", first);
        analytics(buyer, "BUYER", "2026-10-12", "2026-10-13", 0, 100)
                .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(2))
                .andExpect(jsonPath("$.rows.length()").value(2))
                .andExpect(jsonPath("$.rows[0].id").value(laterId))
                .andExpect(jsonPath("$.rows[1].id").value(first))
                .andExpect(jsonPath("$.totals[0].amount").value("11.00"))
                .andExpect(jsonPath("$.totals[1].amount").value("0.00"))
                .andExpect(jsonPath("$.totals[2].amount").value("0.00"));
        for (String side : List.of("BUYER", "SUPPLIER")) {
            analytics(outsider, side, "2026-10-12", "2026-10-13", 0, 20)
                    .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(0))
                    .andExpect(jsonPath("$.rows").isEmpty())
                    .andExpect(jsonPath("$.totals[*].amount").value(org.hamcrest.Matchers.contains("0.00", "0.00", "0.00")));
        }
    }

    @Test void completedAnalyticsValidationAndAuthorization() throws Exception {
        mvc.perform(get("/api/procurement/analytics/completed-orders")).andExpect(status().isUnauthorized());
        analytics(noGrant, "BUYER", "2026-10-12", "2026-10-13", 0, 20).andExpect(status().isForbidden());
        analytics(supplierB, "BUYER", "2026-10-12", "2026-10-13", 0, 20).andExpect(status().isForbidden());
        analytics(outsider, "BUYER", "2026-10-12", "2026-10-13", 0, 20).andExpect(jsonPath("$.total").value(0));
        for (String query : List.of("side=BUYER&from=2026-10-12", "side=&from=2026-10-12&toExclusive=2026-10-13",
                "from=2026-10-12&toExclusive=2026-10-13",
                "side=BUYER&toExclusive=2026-10-13",
                "side=BUYER&from=&toExclusive=2026-10-13",
                "side=BUYER&from=2026-10-12&toExclusive=",
                "side=BUYER&from=2026-10-12&toExclusive=2026-10-13&toExclusive=2026-10-14",
                "side=BUYER&from=2026-10-12&toExclusive=2026-10-13&page=0&page=0",
                "side=BUYER&from=2026-10-12&toExclusive=2026-10-13&size=",
                "side=BUYER&from=2026-10-12&toExclusive=2026-10-13&size=abc",
                "side=BUYER&from=2026-10-12&toExclusive=2026-10-13&page=2147483648",
                "side=BUYER&side=SUPPLIER&from=2026-10-12&toExclusive=2026-10-13",
                "side=BUYER&from=2026-10-12&from=2026-10-11&toExclusive=2026-10-13",
                "side=BOGUS&from=2026-10-12&toExclusive=2026-10-13",
                "side=BUYER&from=2026-2-12&toExclusive=2026-10-13",
                "side=BUYER&from=2026-02-29&toExclusive=2026-10-13",
                "side=BUYER&from=2026-10-12&toExclusive=2026-10-12",
                "side=BUYER&from=2026-10-12&toExclusive=2027-10-14",
                "side=BUYER&from=2026-10-12&toExclusive=2026-10-13&page=",
                "side=BUYER&from=2026-10-12&toExclusive=2026-10-13&page=abc",
                "side=BUYER&from=2026-10-12&toExclusive=2026-10-13&page=-1",
                "side=BUYER&from=2026-10-12&toExclusive=2026-10-13&page=1000001",
                "side=BUYER&from=2026-10-12&toExclusive=2026-10-13&size=0",
                "side=BUYER&from=2026-10-12&toExclusive=2026-10-13&size=101",
                "side=BUYER&from=2026-10-12&toExclusive=2026-10-13&size=10&size=20")) {
            mvc.perform(get("/api/procurement/analytics/completed-orders?" + query).session(buyer))
                    .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                    .andExpect(jsonPath("$.message").value("Invalid analytics filters."))
                    .andExpect(jsonPath("$.*").value(org.hamcrest.Matchers.hasSize(2)));
        }
        mvc.perform(get("/api/procurement/analytics/completed-orders?side=BUYER&from=2026-10-12&toExclusive=2026-10-13&extra=x").session(buyer))
                .andExpect(status().isOk()).andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.page").value(0)).andExpect(jsonPath("$.timeZone").value("UTC"));
        analytics(buyer, "BUYER", "2026-01-01", "2027-01-02", 0, 1).andExpect(status().isOk());
        analytics(buyer, "BUYER", "2026-10-12", "2026-10-13", 1_000_000, 100)
                .andExpect(status().isOk()).andExpect(jsonPath("$.rows").isEmpty());
    }

    @Test void completedAnalyticsCurrencyGroupsAndOrderDetailReconcile() throws Exception {
        long mad = orderId("Delta four");
        complete(mad, "2026-10-12T00:00:00Z", "10.25");
        long eur = extraOrder("Analytics EUR", "EUR");
        long usd = extraOrder("Analytics USD", "USD");
        complete(eur, "2026-10-12T23:59:59Z", "2.40");
        long eurRfq = jdbc.queryForObject("SELECT rfq_id FROM purchase_orders WHERE id=?", Long.class, eur);
        long eurItem = jdbc.queryForObject("INSERT INTO rfq_items(rfq_id,description,quantity,unit) VALUES (?,'Three units',3,'each') RETURNING id", Long.class, eurRfq);
        jdbc.update("INSERT INTO purchase_order_items(order_id,rfq_item_id,description,quantity,unit,unit_price,line_total) VALUES (?,?,'Three units',3,'each',0.10,0.30)", eur, eurItem);
        complete(usd, "2026-10-13T00:00:00Z", "4.00");
        analytics(buyer, "BUYER", "2026-10-12", "2026-10-13", 0, 1)
                .andExpect(jsonPath("$.total").value(2)).andExpect(jsonPath("$.rows[0].id").value(eur))
                .andExpect(jsonPath("$.totals[0].amount").value("10.25"))
                .andExpect(jsonPath("$.totals[*].currency").value(org.hamcrest.Matchers.contains("MAD", "EUR", "USD")))
                .andExpect(jsonPath("$.rows[0].subtotal").value("2.70"))
                .andExpect(jsonPath("$.totals[1].amount").value("2.70"))
                .andExpect(jsonPath("$.totals[2].amount").value("0.00"));
        analytics(buyer, "BUYER", "2026-10-12", "2026-10-13", 1, 1)
                .andExpect(jsonPath("$.rows[0].id").value(mad))
                .andExpect(jsonPath("$.totals[1].amount").value("2.70"));
        analytics(buyer, "BUYER", "2026-10-13", "2026-10-14", 0, 20)
                .andExpect(jsonPath("$.total").value(1)).andExpect(jsonPath("$.totals[2].amount").value("4.00"));
        mvc.perform(get("/api/procurement/orders/" + eur).session(buyer))
                .andExpect(status().isOk()).andExpect(jsonPath("$.subtotal").value("2.70"));
    }

    private long extraOrder(String title, String currency) {
        long rfqId = rfq(buyerOrg, title, "SENT");
        long invitationId = invite(rfqId, supplierBOrg);
        long quoteId = quotation(invitationId, "ACCEPTED", currency);
        order(buyerOrg, supplierBOrg, rfqId, quoteId, title, "CREATED");
        long id = orderId(title);
        jdbc.update("UPDATE purchase_orders SET currency=? WHERE id=?", currency, id);
        return id;
    }

    @Test void completedAnalyticsKeepsOneSnapshotAcrossConcurrentReceipt() throws Exception {
        long main = orderId("Delta four");
        CountDownLatch beforeCount = new CountDownLatch(1), release = new CountDownLatch(1);
        doAnswer(call -> {
            beforeCount.countDown();
            assertTrue(release.await(10, TimeUnit.SECONDS));
            return call.callRealMethod();
        }).when(analyticsQueries).total(org.mockito.ArgumentMatchers.any());
        var pool = Executors.newSingleThreadExecutor();
        try {
            var result = pool.submit(() -> analytics(buyer, "BUYER", "2026-10-12", "2026-10-13", 0, 20)
                    .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
            assertTrue(beforeCount.await(10, TimeUnit.SECONDS));
            complete(main, "2026-10-12T12:00:00Z", "8.00");
            release.countDown();
            String old = result.get(10, TimeUnit.SECONDS);
            assertEquals(0, ((Number) com.jayway.jsonpath.JsonPath.read(old, "$.total")).intValue());
            assertTrue(((List<?>) com.jayway.jsonpath.JsonPath.read(old, "$.rows")).isEmpty());
            assertEquals(List.of("0.00", "0.00", "0.00"), com.jayway.jsonpath.JsonPath.read(old, "$.totals[*].amount"));
            analytics(buyer, "BUYER", "2026-10-12", "2026-10-13", 0, 20)
                    .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(1))
                    .andExpect(jsonPath("$.rows.length()").value(1)).andExpect(jsonPath("$.rows[0].id").value(main))
                    .andExpect(jsonPath("$.rows[0].subtotal").value("8.00"))
                    .andExpect(jsonPath("$.totals[*].amount").value(org.hamcrest.Matchers.contains("8.00", "0.00", "0.00")));
        } finally { release.countDown(); pool.shutdownNow(); reset(analyticsQueries); }
    }

    private long orderId(String title) { return jdbc.queryForObject("SELECT id FROM purchase_orders WHERE rfq_title=?", Long.class, title); }
    private void complete(long orderId, String at, String amount) {
        long rfqId = jdbc.queryForObject("SELECT rfq_id FROM purchase_orders WHERE id=?", Long.class, orderId);
        long rfqItem = jdbc.queryForObject("INSERT INTO rfq_items(rfq_id,description,quantity,unit) VALUES (?,'Analytics line',1,'each') RETURNING id", Long.class, rfqId);
        jdbc.update("INSERT INTO purchase_order_items(order_id,rfq_item_id,description,quantity,unit,unit_price,line_total) VALUES (?,?,'Analytics line',1,'each',?,?)", orderId, rfqItem, new java.math.BigDecimal(amount), new java.math.BigDecimal(amount));
        jdbc.update("UPDATE purchase_orders SET status='RECEIVED' WHERE id=?", orderId);
        long actor = jdbc.queryForObject("SELECT accepting_account_id FROM purchase_orders WHERE id=?", Long.class, orderId);
        jdbc.update("INSERT INTO order_events(order_id,target_status,occurred_at,acting_account_id) VALUES (?,'RECEIVED',?::timestamptz,?)", orderId, at, actor);
    }
    private ResultActions analytics(MockHttpSession session, String side, String from, String to, int page, int size) throws Exception {
        return mvc.perform(get("/api/procurement/analytics/completed-orders").session(session)
                .param("side", side).param("from", from).param("toExclusive", to)
                .param("page", String.valueOf(page)).param("size", String.valueOf(size)));
    }

    private long organization(String name) { return jdbc.queryForObject("INSERT INTO organizations(display_name) VALUES (?) RETURNING id", Long.class, name); }
    private long account(long org, String email, boolean buyer, boolean supplier) { return jdbc.queryForObject("INSERT INTO accounts(organization_id,full_name,email,password_hash,organization_admin,buyer_grant,supplier_grant,email_verified) VALUES (?,'Operator',?,?,true,?,?,true) RETURNING id", Long.class, org, email, passwordHash, buyer, supplier); }
    private long rfq(long org, String title, String status) { return jdbc.queryForObject("INSERT INTO rfqs(organization_id,title,status) VALUES (?,?,?) RETURNING id", Long.class, org, title, status); }
    private long invite(long rfqId, long supplierOrg) { return jdbc.queryForObject("INSERT INTO rfq_invitations(rfq_id,supplier_organization_id) VALUES (?,?) RETURNING id", Long.class, rfqId, supplierOrg); }
    private long quotation(long invitationId, String status, String currency) {
        if (status.equals("DRAFT")) return jdbc.queryForObject("INSERT INTO quotations(invitation_id,status) VALUES (?,?) RETURNING id", Long.class, invitationId, status);
        return jdbc.queryForObject("INSERT INTO quotations(invitation_id,status,currency,valid_until,submitted_at) VALUES (?,?,?,'2026-11-01T00:00:00Z',now()) RETURNING id", Long.class, invitationId, status, currency);
    }
    private void order(long buyerOrg, long supplierOrg, long rfqId, long quotationId, String title, String status) {
        String buyerName = jdbc.queryForObject("SELECT display_name FROM organizations WHERE id=?", String.class, buyerOrg);
        String supplierName = jdbc.queryForObject("SELECT display_name FROM organizations WHERE id=?", String.class, supplierOrg);
        long accountId = jdbc.queryForObject("SELECT id FROM accounts WHERE organization_id=? LIMIT 1", Long.class, buyerOrg);
        jdbc.update("INSERT INTO purchase_orders(rfq_id,quotation_id,buyer_organization_id,supplier_organization_id,accepting_account_id,rfq_title,buyer_display_name,supplier_display_name,status,currency,valid_until,accepted_at) VALUES (?,?,?,?,?,?,?,?,?,'MAD','2026-11-01T00:00:00Z',now())",
                rfqId, quotationId, buyerOrg, supplierOrg, accountId, title, buyerName, supplierName, status);
    }
    private MockHttpSession login(String email) throws Exception { return (MockHttpSession) mvc.perform(post("/api/auth/login").with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"email\":\"" + email + "\",\"password\":\"workspace passphrase 123\"}")).andExpect(status().isOk()).andReturn().getRequest().getSession(false); }
    private ResultActions workspace(MockHttpSession session, String kind, Integer page, Integer size, String q, String status) throws Exception {
        var request = get("/api/procurement/workspace").session(session).param("kind", kind);
        if (page != null) request.param("page", String.valueOf(page));
        if (size != null) request.param("size", String.valueOf(size));
        if (q != null) request.param("q", q);
        if (status != null) request.param("status", status);
        return mvc.perform(request);
    }
    private void bad(ResultActions action) throws Exception { action.andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").isString()).andExpect(jsonPath("$.message").isString()); }
}
