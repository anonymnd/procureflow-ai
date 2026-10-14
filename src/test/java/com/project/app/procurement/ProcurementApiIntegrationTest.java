package com.project.app.procurement;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.project.app.identity.repository.AccountRepository;
import com.project.app.identity.security.AccountPrincipal;
import com.project.app.procurement.api.ProcurementRequests.SendRfq;
import com.project.app.procurement.api.ProcurementRequests.UpdateRfq;
import com.project.app.procurement.service.ProcurementException;
import com.project.app.procurement.service.RfqService;
import com.project.app.support.Slice0DatabaseGuard;

/** TC-S3-001..011 -> REQ-S3-001..005, BL-S3-R1. Isolated PostgreSQL only; issues #12/#13/#14/#15 scope. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("slice0-test")
@ContextConfiguration(initializers = Slice0DatabaseGuard.class)
class ProcurementApiIntegrationTest {
    private static final String AUTH = "/api/auth";
    private static final String EMPLOYEES = "/api/employees";
    private static final String CONTACTS = "/api/procurement/contacts";
    private static final String RFQS = "/api/procurement/rfqs";
    private static final String RECIPIENTS = "/api/procurement/recipients";
    private static final String INVITATIONS = "/api/procurement/invitations";
    private static final Pattern TOKEN = Pattern.compile("(?:verify|invite)=([A-Za-z0-9_-]+)");

    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired SessionRegistry sessionRegistry;
    @Autowired PlatformTransactionManager transactionManager;
    @Autowired AccountRepository accounts;
    @Autowired RfqService rfqService;
    @MockitoBean JavaMailSender mailSender;

    @BeforeEach
    void clearRows() {
        jdbc.update("DELETE FROM order_events");jdbc.update("DELETE FROM purchase_order_items");
        jdbc.update("DELETE FROM purchase_orders");
        jdbc.update("UPDATE rfqs SET status='SENT', winning_quotation_id=NULL WHERE status='AWARDED'");
        jdbc.update("DELETE FROM quotation_items");
        jdbc.update("DELETE FROM quotations");
        jdbc.update("DELETE FROM rfq_invitations");
        jdbc.update("DELETE FROM rfq_items");
        jdbc.update("DELETE FROM rfqs");
        jdbc.update("DELETE FROM supplier_contacts");
        jdbc.update("DELETE FROM marketplace_offerings");
        jdbc.update("DELETE FROM marketplace_profiles");
        jdbc.update("DELETE FROM identity_tokens");
        jdbc.update("DELETE FROM accounts");
        jdbc.update("DELETE FROM organizations");
        for (Object principal : sessionRegistry.getAllPrincipals()) {
            for (SessionInformation session : sessionRegistry.getAllSessions(principal, true)) {
                sessionRegistry.removeSessionInformation(session.getSessionId());
            }
        }
        reset(mailSender);
    }

    @Test
    void contactCreateListRemoveIsOwnershipIsolatedAndRequiresEligibleSupplier() throws Exception {
        Authenticated buyer = registerVerifyAndGrant("Buyer Co", "buyer-a@example.com", "a".repeat(12), true, false);
        Authenticated otherBuyer = registerVerifyAndGrant("Other Buyer", "buyer-b@example.com", "b".repeat(12), true, false);
        Authenticated supplier = registerVerifyAndGrant("Eligible Supplier", "supplier-a@example.com", "s".repeat(12), false, true);
        long supplierOrgId = jdbc.queryForObject("SELECT organization_id FROM accounts WHERE email='supplier-a@example.com'", Long.class);

        // TC-S3-002: ineligible target (no active SUPPLIER-granted employee) rejected.
        Authenticated notSupplier = registerVerifyAndGrant("No Supplier Grant Co", "nosup@example.com", "n".repeat(12), false, false);
        long notSupplierOrgId = jdbc.queryForObject("SELECT organization_id FROM accounts WHERE email='nosup@example.com'", Long.class);
        mvc.perform(post(CONTACTS).session(buyer.session()).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"supplierOrganizationId\":" + notSupplierOrgId + ",\"note\":\"x\"}"))
                .andExpect(status().isBadRequest());

        // TC-S3-001: create/list own contact.
        mvc.perform(get(CONTACTS).session(buyer.session())).andExpect(status().isOk())
                .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(0)));
        mvc.perform(post(CONTACTS).session(buyer.session()).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"supplierOrganizationId\":" + supplierOrgId + ",\"note\":\"Reliable.\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.supplierDisplayName").value("Eligible Supplier"))
                .andExpect(jsonPath("$.note").value("Reliable."));

        // Ownership isolation: another buyer's contact list never includes this.
        mvc.perform(get(CONTACTS).session(otherBuyer.session())).andExpect(status().isOk())
                .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(0)));

        MvcResult listResult = mvc.perform(get(CONTACTS).session(buyer.session())).andExpect(status().isOk())
                .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(1))).andReturn();
        long contactId = ((Number) com.jayway.jsonpath.JsonPath.<Object>read(
                listResult.getResponse().getContentAsString(), "$[0].id")).longValue();

        // Another buyer cannot remove this org's contact.
        mvc.perform(delete(CONTACTS + "/" + contactId).session(otherBuyer.session()).with(csrf()))
                .andExpect(status().isNotFound());
        mvc.perform(delete(CONTACTS + "/" + contactId).session(buyer.session()).with(csrf()))
                .andExpect(status().isNoContent());
        mvc.perform(get(CONTACTS).session(buyer.session())).andExpect(status().isOk())
                .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(0)));

        // A private contact is only a selection aid; its removal cannot erase a sent invitation.
        mvc.perform(post(CONTACTS).session(buyer.session()).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"supplierOrganizationId\":" + supplierOrgId + "}"))
                .andExpect(status().isCreated());
        long rfqId = createRfq(buyer, "Contact preservation");
        addItem(buyer, rfqId, "Bolts", 1, "pcs");
        mvc.perform(post(RFQS + "/" + rfqId + "/send").session(buyer.session()).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"recipientOrganizationIds\":[" + supplierOrgId + "]}"))
                .andExpect(status().isOk());
        long newContactId = jdbc.queryForObject("SELECT id FROM supplier_contacts WHERE buyer_organization_id = "
                + jdbc.queryForObject("SELECT organization_id FROM accounts WHERE email='buyer-a@example.com'", Long.class), Long.class);
        mvc.perform(delete(CONTACTS + "/" + newContactId).session(buyer.session()).with(csrf()))
                .andExpect(status().isNoContent());
        mvc.perform(get(INVITATIONS).session(supplier.session())).andExpect(status().isOk())
                .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(1)));
        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM rfq_invitations WHERE rfq_id=" + rfqId, Integer.class));
    }

    @Test
    void rfqDraftCreateEditItemsAndCrossOrgDenial() throws Exception {
        Authenticated buyer = registerVerifyAndGrant("Draft Co", "buyer-c@example.com", "c".repeat(12), true, false);
        Authenticated otherBuyer = registerVerifyAndGrant("Other Co", "buyer-d@example.com", "d".repeat(12), true, false);

        MvcResult created = mvc.perform(post(RFQS).session(buyer.session()).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"Office chairs\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.items", org.hamcrest.Matchers.hasSize(0))).andReturn();
        long rfqId = ((Number) com.jayway.jsonpath.JsonPath.<Object>read(created.getResponse().getContentAsString(), "$.id")).longValue();

        // TC-S3-004: accepted endpoints and values just outside each boundary.
        mvc.perform(post(RFQS).session(buyer.session()).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"\"}")).andExpect(status().isBadRequest());
        mvc.perform(post(RFQS).session(buyer.session()).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"" + "t".repeat(161) + "\"}")).andExpect(status().isBadRequest());
        createRfq(buyer, "t");
        createRfq(buyer, "t".repeat(160));
        addItem(buyer, rfqId, "d", 1, "u");
        addItem(buyer, rfqId, "d".repeat(2000), 1000000, "u".repeat(40));
        mvc.perform(post(RFQS + "/" + rfqId + "/items").session(buyer.session()).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(itemJson("d".repeat(2001), 1, "u")))
                .andExpect(status().isBadRequest());
        mvc.perform(post(RFQS + "/" + rfqId + "/items").session(buyer.session()).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(itemJson("d", 1, "u".repeat(41))))
                .andExpect(status().isBadRequest());

        // TC-S3-004: item boundary validation.
        mvc.perform(post(RFQS + "/" + rfqId + "/items").session(buyer.session()).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content(itemJson("", 10, "pcs"))).andExpect(status().isBadRequest());
        mvc.perform(post(RFQS + "/" + rfqId + "/items").session(buyer.session()).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content(itemJson("Ergonomic chair", 0, "pcs"))).andExpect(status().isBadRequest());
        mvc.perform(post(RFQS + "/" + rfqId + "/items").session(buyer.session()).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content(itemJson("Ergonomic chair", 1000001, "pcs"))).andExpect(status().isBadRequest());
        mvc.perform(post(RFQS + "/" + rfqId + "/items").session(buyer.session()).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content(itemJson("Ergonomic chair", 10, ""))).andExpect(status().isBadRequest());

        MvcResult itemResult = mvc.perform(post(RFQS + "/" + rfqId + "/items").session(buyer.session()).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(itemJson("Ergonomic chair", 10, "pcs")))
                .andExpect(status().isCreated()).andReturn();
        long itemId = ((Number) com.jayway.jsonpath.JsonPath.<Object>read(itemResult.getResponse().getContentAsString(), "$.id")).longValue();

        mvc.perform(put(RFQS + "/" + rfqId + "/items/" + itemId).session(buyer.session()).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(itemJson("Ergonomic chair v2", 12, "pcs")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.quantity").value(12));

        mvc.perform(put(RFQS + "/" + rfqId).session(buyer.session()).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"Office chairs v2\"}")).andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Office chairs v2"));

        // Cross-organization draft read/write denied.
        mvc.perform(get(RFQS + "/" + rfqId).session(otherBuyer.session())).andExpect(status().isNotFound());
        mvc.perform(put(RFQS + "/" + rfqId).session(otherBuyer.session()).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"Hijack\"}")).andExpect(status().isNotFound());
    }

    @Test
    void sendRequiresItemsAndRecipientsDeduplicatesAndLocksItems() throws Exception {
        Authenticated buyer = registerVerifyAndGrant("Send Co", "buyer-e@example.com", "e".repeat(12), true, false);
        registerVerifyAndGrant("Supplier One", "supplier-b@example.com", "p".repeat(12), false, true);
        registerVerifyAndGrant("Supplier Two", "supplier-c@example.com", "q".repeat(12), false, true);
        long supplierOneId = jdbc.queryForObject("SELECT organization_id FROM accounts WHERE email='supplier-b@example.com'", Long.class);
        long supplierTwoId = jdbc.queryForObject("SELECT organization_id FROM accounts WHERE email='supplier-c@example.com'", Long.class);

        long rfqId = createRfq(buyer, "Hardware batch");

        // TC-S3-005: no items -> rejected, no partial effect.
        mvc.perform(post(RFQS + "/" + rfqId + "/send").session(buyer.session()).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"recipientOrganizationIds\":[" + supplierOneId + "]}")).andExpect(status().isConflict());
        mvc.perform(get(RFQS + "/" + rfqId).session(buyer.session())).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DRAFT"));
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM rfq_invitations WHERE rfq_id=" + rfqId, Integer.class));
        addItem(buyer, rfqId, "Steel bolts", 500, "pcs");
        long itemId = jdbc.queryForObject("SELECT id FROM rfq_items WHERE rfq_id=" + rfqId, Long.class);

        // No recipients -> rejected.
        mvc.perform(post(RFQS + "/" + rfqId + "/send").session(buyer.session()).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"recipientOrganizationIds\":[]}")).andExpect(status().isBadRequest());
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM rfq_invitations", Integer.class));
        mvc.perform(get(RFQS + "/" + rfqId).session(buyer.session())).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DRAFT"));

        // TC-S3-006/007: duplicate recipient id deduplicated to one invitation; status -> SENT.
        mvc.perform(post(RFQS + "/" + rfqId + "/send").session(buyer.session()).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"recipientOrganizationIds\":[" + supplierOneId + "," + supplierTwoId + "," + supplierOneId + "]}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("SENT"));
        assertEquals(2, jdbc.queryForObject("SELECT count(*) FROM rfq_invitations WHERE rfq_id=" + rfqId, Integer.class));

        // Items no longer editable after send.
        mvc.perform(post(RFQS + "/" + rfqId + "/items").session(buyer.session()).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content(itemJson("Extra", 1, "pcs"))).andExpect(status().isConflict());
        mvc.perform(put(RFQS + "/" + rfqId).session(buyer.session()).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"Renamed\"}")).andExpect(status().isConflict());
        mvc.perform(put(RFQS + "/" + rfqId + "/items/" + itemId).session(buyer.session()).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(itemJson("Changed", 1, "pcs")))
                .andExpect(status().isConflict());
        mvc.perform(delete(RFQS + "/" + rfqId + "/items/" + itemId).session(buyer.session()).with(csrf()))
                .andExpect(status().isConflict());
    }

    @Test
    void concurrentDraftEditWaitsForSendAndCannotChangeSentRfq() throws Exception {
        Authenticated buyer = registerVerifyAndGrant("Concurrent Buyer", "buyer-lock@example.com", "l".repeat(12), true, false);
        registerVerifyAndGrant("Concurrent Supplier", "supplier-lock@example.com", "m".repeat(12), false, true);
        long supplierOrgId = jdbc.queryForObject("SELECT organization_id FROM accounts WHERE email='supplier-lock@example.com'", Long.class);
        long rfqId = createRfq(buyer, "Original title");
        addItem(buyer, rfqId, "Bolts", 1, "pcs");
        AccountPrincipal principal = new AccountPrincipal(accounts.findByEmail("buyer-lock@example.com").orElseThrow());
        CountDownLatch sendFinishedInsideTransaction = new CountDownLatch(1);
        CountDownLatch allowSendCommit = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Future<?> sending = pool.submit(() -> new TransactionTemplate(transactionManager).executeWithoutResult(tx -> {
                rfqService.send(principal, rfqId, new SendRfq(List.of(supplierOrgId)));
                sendFinishedInsideTransaction.countDown();
                try {
                    if (!allowSendCommit.await(10, TimeUnit.SECONDS)) throw new AssertionError("send commit was not released");
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    throw new AssertionError(ex);
                }
            }));
            assertTrue(sendFinishedInsideTransaction.await(10, TimeUnit.SECONDS));
            Future<?> editing = pool.submit(() -> rfqService.updateTitle(principal, rfqId, new UpdateRfq("Late edit")));
            assertThrows(TimeoutException.class, () -> editing.get(200, TimeUnit.MILLISECONDS),
                    "draft edit must wait for the RFQ row held by send");
            allowSendCommit.countDown();
            sending.get(10, TimeUnit.SECONDS);
            ExecutionException rejection = assertThrows(ExecutionException.class, () -> editing.get(10, TimeUnit.SECONDS));
            assertInstanceOf(ProcurementException.class, rejection.getCause());
            mvc.perform(get(RFQS + "/" + rfqId).session(buyer.session())).andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("SENT"))
                    .andExpect(jsonPath("$.title").value("Original title"));
        } finally {
            allowSendCommit.countDown();
            pool.shutdownNow();
        }
    }

    @Test
    void supplierInvitationVisibilityIsOwnershipIsolatedAndGrantGated() throws Exception {
        Authenticated buyer = registerVerifyAndGrant("Visibility Buyer", "buyer-f@example.com", "f".repeat(12), true, false);
        Authenticated supplierOne = registerVerifyAndGrant("Visible Supplier One", "supplier-d@example.com", "r".repeat(12), false, true);
        Authenticated supplierTwo = registerVerifyAndGrant("Visible Supplier Two", "supplier-e@example.com", "t".repeat(12), false, true);
        Authenticated uninvited = registerVerifyAndGrant("Uninvited Supplier", "supplier-f@example.com", "u".repeat(12), false, true);
        long supplierOneId = jdbc.queryForObject("SELECT organization_id FROM accounts WHERE email='supplier-d@example.com'", Long.class);
        long supplierTwoId = jdbc.queryForObject("SELECT organization_id FROM accounts WHERE email='supplier-e@example.com'", Long.class);

        long rfqId = createRfq(buyer, "Visibility RFQ");
        addItem(buyer, rfqId, "Widget", 5, "pcs");
        mvc.perform(post(RFQS + "/" + rfqId + "/send").session(buyer.session()).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"recipientOrganizationIds\":[" + supplierOneId + "," + supplierTwoId + "]}"))
                .andExpect(status().isOk());

        MvcResult listOne = mvc.perform(get(INVITATIONS).session(supplierOne.session())).andExpect(status().isOk())
                .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(1)))
                .andExpect(jsonPath("$[0].buyerDisplayName").value("Visibility Buyer")).andReturn();
        long invitationOneId = ((Number) com.jayway.jsonpath.JsonPath.<Object>read(listOne.getResponse().getContentAsString(), "$[0].id")).longValue();

        mvc.perform(get(INVITATIONS + "/" + invitationOneId).session(supplierOne.session())).andExpect(status().isOk())
                .andExpect(jsonPath("$.items", org.hamcrest.Matchers.hasSize(1)))
                .andExpect(jsonPath("$.items[0].description").value("Widget"));

        // Two suppliers invited to the same RFQ never see each other's invitation.
        mvc.perform(get(INVITATIONS + "/" + invitationOneId).session(supplierTwo.session())).andExpect(status().isNotFound());
        mvc.perform(get(INVITATIONS).session(supplierTwo.session())).andExpect(status().isOk())
                .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(1)))
                .andExpect(jsonPath("$[0].id", org.hamcrest.Matchers.not(org.hamcrest.Matchers.equalTo((int) invitationOneId))));

        // Uninvited organization gets 404, not the detail.
        mvc.perform(get(INVITATIONS + "/" + invitationOneId).session(uninvited.session())).andExpect(status().isNotFound());
        mvc.perform(get(INVITATIONS).session(uninvited.session())).andExpect(status().isOk())
                .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(0)));

        // BUYER-only employee of the invited organization is denied (403, by the security matcher).
        long supplierOneAdminId = currentId(supplierOne.session());
        mvc.perform(put(EMPLOYEES + "/" + supplierOneAdminId + "/access").session(supplierOne.session()).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"organizationAdmin\":true,\"buyerGrant\":true,\"supplierGrant\":false,\"active\":true}"))
                .andExpect(status().isOk());
        Authenticated buyerOnly = login("supplier-d@example.com", "r".repeat(12));
        mvc.perform(get(INVITATIONS).session(buyerOnly.session())).andExpect(status().isForbidden());
    }

    @Test
    void duplicateInvitationInsertIsRejectedByTheDatabaseUniqueConstraint() throws Exception {
        Authenticated buyer = registerVerifyAndGrant("Dup Buyer", "buyer-g@example.com", "g".repeat(12), true, false);
        registerVerifyAndGrant("Dup Supplier", "supplier-g@example.com", "v".repeat(12), false, true);
        long supplierOrgId = jdbc.queryForObject("SELECT organization_id FROM accounts WHERE email='supplier-g@example.com'", Long.class);
        long rfqId = createRfq(buyer, "Dup RFQ");

        jdbc.update("INSERT INTO rfq_invitations (rfq_id, supplier_organization_id) VALUES (?, ?)", rfqId, supplierOrgId);
        assertThrows(DataIntegrityViolationException.class,
                () -> jdbc.update("INSERT INTO rfq_invitations (rfq_id, supplier_organization_id) VALUES (?, ?)", rfqId, supplierOrgId));
    }

    @Test
    void recipientSearchFindsEligibleOrganizationsByNameRegardlessOfPublication() throws Exception {
        Authenticated buyer = registerVerifyAndGrant("Search Buyer", "buyer-h@example.com", "h".repeat(12), true, false);
        registerVerifyAndGrant("Acme Fasteners", "supplier-h@example.com", "w".repeat(12), false, true);
        registerVerifyAndGrant("Acme Logistics", "nosupacme@example.com", "x".repeat(12), true, false);

        mvc.perform(get(RECIPIENTS).session(buyer.session()).param("q", "Acme")).andExpect(status().isOk())
                .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(1)))
                .andExpect(jsonPath("$[0].displayName").value("Acme Fasteners"));
        mvc.perform(get(RECIPIENTS).session(buyer.session()).param("q", "acme fasteners")).andExpect(status().isOk())
                .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(1)));
        mvc.perform(get(RECIPIENTS).session(buyer.session()).param("q", "NoMatch")).andExpect(status().isOk())
                .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(0)));
    }

    private long createRfq(Authenticated buyer, String title) throws Exception {
        MvcResult result = mvc.perform(post(RFQS).session(buyer.session()).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"" + title + "\"}")).andExpect(status().isCreated()).andReturn();
        return ((Number) com.jayway.jsonpath.JsonPath.<Object>read(result.getResponse().getContentAsString(), "$.id")).longValue();
    }

    private void addItem(Authenticated buyer, long rfqId, String description, int quantity, String unit) throws Exception {
        mvc.perform(post(RFQS + "/" + rfqId + "/items").session(buyer.session()).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content(itemJson(description, quantity, unit))).andExpect(status().isCreated());
    }

    private static String itemJson(String description, int quantity, String unit) {
        return "{" + field("description", description) + ",\"quantity\":" + quantity + "," + field("unit", unit) + "}";
    }

    private Authenticated registerVerifyAndGrant(String organization, String email, String password, boolean buyerGrant,
            boolean supplierGrant) throws Exception {
        registerAndVerify(organization, email, password);
        Authenticated firstSession = login(email, password);
        long adminId = currentId(firstSession.session());
        mvc.perform(put(EMPLOYEES + "/" + adminId + "/access").session(firstSession.session()).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"organizationAdmin\":true,\"buyerGrant\":" + buyerGrant + ",\"supplierGrant\":" + supplierGrant + ",\"active\":true}"))
                .andExpect(status().isOk());
        return login(email, password);
    }

    private void registerAndVerify(String organization, String email, String password) throws Exception {
        mvc.perform(post(AUTH + "/register").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{" + field("organizationName", organization) + "," + field("fullName", "Admin") + ","
                        + field("email", email) + "," + field("password", password) + "}"))
                .andExpect(status().isCreated());
        String token = latestToken();
        mvc.perform(post(AUTH + "/verify").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{" + field("token", token) + "}")).andExpect(status().isOk());
    }

    private Authenticated login(String email, String password) throws Exception {
        MvcResult result = mvc.perform(post(AUTH + "/login").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{" + field("email", email) + "," + field("password", password) + "}"))
                .andExpect(status().isOk()).andReturn();
        return new Authenticated((MockHttpSession) result.getRequest().getSession(false));
    }

    private long currentId(MockHttpSession session) throws Exception {
        String body = mvc.perform(get(AUTH + "/me").session(session)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        Number id = com.jayway.jsonpath.JsonPath.read(body, "$.id");
        return id.longValue();
    }

    private String latestToken() {
        var message = org.mockito.ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender, atLeastOnce()).send(message.capture());
        String text = message.getAllValues().get(message.getAllValues().size() - 1).getText();
        Matcher matcher = TOKEN.matcher(text);
        assertTrue(matcher.find(), "mail should contain a one-time link");
        return matcher.group(1);
    }

    private static String field(String name, String value) {
        return "\"" + name + "\":\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    private record Authenticated(MockHttpSession session) { }
}
