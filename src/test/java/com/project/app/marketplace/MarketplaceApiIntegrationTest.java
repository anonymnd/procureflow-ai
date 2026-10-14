package com.project.app.marketplace;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

import com.project.app.support.Slice0DatabaseGuard;

/** TC-S2-001..009/013..016 -> REQ-S2-001..003, BL-S2-R1. Isolated PostgreSQL only; issue #7/#8/#9 scope. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("slice0-test")
@ContextConfiguration(initializers = Slice0DatabaseGuard.class)
class MarketplaceApiIntegrationTest {
    private static final String AUTH = "/api/auth";
    private static final String EMPLOYEES = "/api/employees";
    private static final String PROFILE = "/api/marketplace/profile";
    private static final String OFFERINGS = "/api/marketplace/offerings";
    private static final String PUBLIC = "/api/public";
    private static final Pattern TOKEN = Pattern.compile("(?:verify|invite)=([A-Za-z0-9_-]+)");

    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired SessionRegistry sessionRegistry;
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
    void ownProfileCreateEditPublishUnpublishAndCrossOrgDenial() throws Exception {
        Authenticated supplierA = registerVerifyAndGrantSupplier("Org A", "supplier-a@example.com", "a".repeat(12));
        Authenticated supplierB = registerVerifyAndGrantSupplier("Org B", "supplier-b@example.com", "b".repeat(12));

        mvc.perform(get(PROFILE).session(supplierA.session())).andExpect(status().isNotFound());

        mvc.perform(put(PROFILE).session(supplierA.session()).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"description\":\"  We sell great things.  \"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description").value("We sell great things."))
                .andExpect(jsonPath("$.published").value(false));

        mvc.perform(put(PROFILE).session(supplierA.session()).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"description\":\"\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(put(PROFILE).session(supplierA.session()).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"description\":\"" + "x".repeat(2001) + "\"}"))
                .andExpect(status().isBadRequest());

        // Org B cannot read or write Org A's draft profile.
        mvc.perform(get(PROFILE).session(supplierB.session())).andExpect(status().isNotFound());

        mvc.perform(post(PROFILE + "/publish").session(supplierA.session()).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.published").value(true))
                .andExpect(jsonPath("$.publishedAt").isNotEmpty());

        mvc.perform(post(PROFILE + "/unpublish").session(supplierA.session()).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.published").value(false));
    }

    @Test
    void buyerOnlyAndAnonymousAreDeniedMarketplaceMutation() throws Exception {
        Authenticated supplierA = registerVerifyAndGrantSupplier("Org C", "supplier-c@example.com", "c".repeat(12));
        mvc.perform(put(PROFILE).session(supplierA.session()).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"description\":\"Org C profile.\"}")).andExpect(status().isOk());

        long adminId = currentId(supplierA.session());
        mvc.perform(put(EMPLOYEES + "/" + adminId + "/access").session(supplierA.session()).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"organizationAdmin\":true,\"buyerGrant\":true,\"supplierGrant\":false,\"active\":true}"))
                .andExpect(status().isOk());
        Authenticated buyerOnly = login("supplier-c@example.com", "c".repeat(12));
        mvc.perform(get(PROFILE).session(buyerOnly.session())).andExpect(status().isForbidden());
        mvc.perform(put(PROFILE).session(buyerOnly.session()).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"description\":\"hijack\"}")).andExpect(status().isForbidden());

        mvc.perform(get(PROFILE)).andExpect(status().isUnauthorized());
    }

    @Test
    void offeringLifecycleRequiresProfileFirstAndValidatesBoundaries() throws Exception {
        Authenticated supplier = registerVerifyAndGrantSupplier("Org D", "supplier-d@example.com", "d".repeat(12));

        mvc.perform(post(OFFERINGS).session(supplier.session()).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content(offeringJson("Widget", "A fine widget.", "OTHER")))
                .andExpect(status().isConflict());

        mvc.perform(put(PROFILE).session(supplier.session()).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"description\":\"Org D profile.\"}")).andExpect(status().isOk());

        mvc.perform(post(OFFERINGS).session(supplier.session()).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content(offeringJson("Widget", "A fine widget.", "UNKNOWN_CODE")))
                .andExpect(status().isBadRequest());
        mvc.perform(post(OFFERINGS).session(supplier.session()).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content(offeringJson("", "A fine widget.", "OTHER")))
                .andExpect(status().isBadRequest());

        MvcResult created = mvc.perform(post(OFFERINGS).session(supplier.session()).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(offeringJson("Widget", "A fine widget.", "IT_EQUIPMENT")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.published").value(false)).andReturn();
        long offeringId = ((Number) com.jayway.jsonpath.JsonPath.<Object>read(created.getResponse().getContentAsString(), "$.id")).longValue();

        mvc.perform(post(OFFERINGS + "/" + offeringId + "/publish").session(supplier.session()).with(csrf()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.published").value(true));
        mvc.perform(post(OFFERINGS + "/" + offeringId + "/unpublish").session(supplier.session()).with(csrf()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.published").value(false));

        Authenticated otherOrg = registerVerifyAndGrantSupplier("Org E", "supplier-e@example.com", "e".repeat(12));
        mvc.perform(get(OFFERINGS + "/" + offeringId).session(otherOrg.session())).andExpect(status().isNotFound());
        mvc.perform(put(OFFERINGS + "/" + offeringId).session(otherOrg.session()).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(offeringJson("Stolen", "x", "OTHER")))
                .andExpect(status().isNotFound());
    }

    @Test
    void publicCatalogExcludesUnpublishedAndSupportsSearchCategoryAndPagination() throws Exception {
        Authenticated supplier = registerVerifyAndGrantSupplier("Discovery Co", "supplier-f@example.com", "f".repeat(12));
        mvc.perform(put(PROFILE).session(supplier.session()).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"description\":\"Discovery Co profile.\"}")).andExpect(status().isOk());

        long draftId = createOffering(supplier, "Draft Widget", "Not yet public.", "OTHER");
        long publishedId = createOffering(supplier, "Steel Beam", "Heavy duty steel beam.", "INDUSTRIAL_SUPPLIES");
        publish(supplier, publishedId);

        // Unpublished offering and unpublished profile both exclude content from the public catalog.
        mvc.perform(get(PUBLIC + "/offerings/" + draftId)).andExpect(status().isNotFound());
        mvc.perform(get(PUBLIC + "/offerings").param("q", "widget")).andExpect(status().isOk())
                .andExpect(jsonPath("$.items", org.hamcrest.Matchers.hasSize(0)));

        mvc.perform(get(PUBLIC + "/offerings").param("q", "steel")).andExpect(status().isOk())
                .andExpect(jsonPath("$.items", org.hamcrest.Matchers.hasSize(0)));

        mvc.perform(post(PROFILE + "/publish").session(supplier.session()).with(csrf())).andExpect(status().isOk());
        mvc.perform(get(PUBLIC + "/offerings").param("q", "steel")).andExpect(status().isOk())
                .andExpect(jsonPath("$.items", org.hamcrest.Matchers.hasSize(1)))
                .andExpect(jsonPath("$.items[0].title").value("Steel Beam"))
                .andExpect(jsonPath("$.items[0].supplierDisplayName").value("Discovery Co"));

        mvc.perform(get(PUBLIC + "/offerings").param("category", "INDUSTRIAL_SUPPLIES")).andExpect(status().isOk())
                .andExpect(jsonPath("$.items", org.hamcrest.Matchers.hasSize(1)));
        mvc.perform(get(PUBLIC + "/offerings").param("category", "FURNITURE")).andExpect(status().isOk())
                .andExpect(jsonPath("$.items", org.hamcrest.Matchers.hasSize(0)));
        mvc.perform(get(PUBLIC + "/offerings").param("category", "")).andExpect(status().isBadRequest());
        mvc.perform(get(PUBLIC + "/offerings").param("category", "industrial_supplies")).andExpect(status().isBadRequest());
        mvc.perform(get(PUBLIC + "/offerings").param("category", "NOPE")).andExpect(status().isBadRequest());

        mvc.perform(get(PUBLIC + "/offerings").param("page", "-1")).andExpect(status().isBadRequest());
        mvc.perform(get(PUBLIC + "/offerings").param("size", "0")).andExpect(status().isBadRequest());
        mvc.perform(get(PUBLIC + "/offerings").param("size", "51")).andExpect(status().isBadRequest());
        mvc.perform(get(PUBLIC + "/offerings").param("page", "99")).andExpect(status().isOk())
                .andExpect(jsonPath("$.items", org.hamcrest.Matchers.hasSize(0)));

        mvc.perform(get(PUBLIC + "/offerings/" + publishedId)).andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Steel Beam"));

        long organizationId = jdbc.queryForObject("SELECT organization_id FROM accounts WHERE email='supplier-f@example.com'", Long.class);
        mvc.perform(get(PUBLIC + "/suppliers/" + organizationId)).andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("Discovery Co"))
                .andExpect(jsonPath("$.offerings", org.hamcrest.Matchers.hasSize(1)));

        mvc.perform(get(PUBLIC + "/categories")).andExpect(status().isOk())
                .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(5)))
                .andExpect(jsonPath("$[0].code").value("IT_EQUIPMENT"));
    }

    @Test
    void duplicateProfileInsertIsRejectedByTheDatabaseUniqueConstraint() throws Exception {
        registerVerifyAndGrantSupplier("Org G", "supplier-g@example.com", "g".repeat(12));
        long organizationId = jdbc.queryForObject("SELECT organization_id FROM accounts WHERE email='supplier-g@example.com'", Long.class);
        jdbc.update("INSERT INTO marketplace_profiles (organization_id, description) VALUES (?, 'first')", organizationId);
        assertThrows(DataIntegrityViolationException.class,
                () -> jdbc.update("INSERT INTO marketplace_profiles (organization_id, description) VALUES (?, 'second')", organizationId));
    }

    @Test
    void errorBodyShapeIsConsistentAcrossMarketplaceAndPublicRoutesOnBadRequest() throws Exception {
        Authenticated supplier = registerVerifyAndGrantSupplier("Org H", "supplier-h@example.com", "h".repeat(12));
        mvc.perform(get(OFFERINGS + "/not-a-number").session(supplier.session()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.timestamp").doesNotExist());
        mvc.perform(get(PUBLIC + "/offerings/not-a-number"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
                .andExpect(jsonPath("$.timestamp").doesNotExist());
    }

    @Test
    void textSearchEscapesLiteralWildcardsFoldsCaseAndEnforcesQueryBoundary() throws Exception {
        Authenticated supplier = registerVerifyAndGrantSupplier("Literal Co", "supplier-i@example.com", "i".repeat(12));
        mvc.perform(put(PROFILE).session(supplier.session()).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"description\":\"Literal Co profile.\"}")).andExpect(status().isOk());
        publish(supplier, createOffering(supplier, "Deal 50%_Off\\Clearance", "Literal wildcard characters in the title.", "OTHER"));
        publish(supplier, createOffering(supplier, "AAAxBBB Plain Title", "No special characters here.", "OTHER"));
        mvc.perform(post(PROFILE + "/publish").session(supplier.session()).with(csrf())).andExpect(status().isOk());

        // Literal '%' and '_' are matched as plain characters, not SQL wildcards.
        mvc.perform(get(PUBLIC + "/offerings").param("q", "50%_Off")).andExpect(status().isOk())
                .andExpect(jsonPath("$.items", org.hamcrest.Matchers.hasSize(1)))
                .andExpect(jsonPath("$.items[0].title").value("Deal 50%_Off\\Clearance"));
        // Literal '\' is matched as a plain character.
        mvc.perform(get(PUBLIC + "/offerings").param("q", "Off\\Clearance")).andExpect(status().isOk())
                .andExpect(jsonPath("$.items", org.hamcrest.Matchers.hasSize(1)));
        // If '_' were treated as a SQL single-character wildcard this would also match "AAAxBBB".
        mvc.perform(get(PUBLIC + "/offerings").param("q", "AAA_BBB")).andExpect(status().isOk())
                .andExpect(jsonPath("$.items", org.hamcrest.Matchers.hasSize(0)));
        // Case-insensitive match.
        mvc.perform(get(PUBLIC + "/offerings").param("q", "aaaxbbb plain")).andExpect(status().isOk())
                .andExpect(jsonPath("$.items", org.hamcrest.Matchers.hasSize(1)))
                .andExpect(jsonPath("$.items[0].title").value("AAAxBBB Plain Title"));

        mvc.perform(get(PUBLIC + "/offerings").param("q", "a".repeat(100))).andExpect(status().isOk());
        mvc.perform(get(PUBLIC + "/offerings").param("q", "a".repeat(101))).andExpect(status().isBadRequest());
    }

    @Test
    void revokedSupplierKeepsPublishedContentPublicButCannotMutate() throws Exception {
        Authenticated supplier = registerVerifyAndGrantSupplier("Org J", "supplier-j@example.com", "j".repeat(12));
        mvc.perform(put(PROFILE).session(supplier.session()).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"description\":\"Org J profile.\"}")).andExpect(status().isOk());
        long offeringId = createOffering(supplier, "Durable Widget", "Stays public after revocation.", "OTHER");
        publish(supplier, offeringId);
        mvc.perform(post(PROFILE + "/publish").session(supplier.session()).with(csrf())).andExpect(status().isOk());
        long organizationId = jdbc.queryForObject("SELECT organization_id FROM accounts WHERE email='supplier-j@example.com'", Long.class);

        long adminId = currentId(supplier.session());
        mvc.perform(put(EMPLOYEES + "/" + adminId + "/access").session(supplier.session()).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"organizationAdmin\":true,\"buyerGrant\":false,\"supplierGrant\":false,\"active\":true}"))
                .andExpect(status().isOk());
        Authenticated revoked = login("supplier-j@example.com", "j".repeat(12));

        mvc.perform(get(PUBLIC + "/offerings/" + offeringId)).andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Durable Widget"));
        mvc.perform(get(PUBLIC + "/suppliers/" + organizationId)).andExpect(status().isOk())
                .andExpect(jsonPath("$.offerings", org.hamcrest.Matchers.hasSize(1)));

        mvc.perform(post(OFFERINGS + "/" + offeringId + "/unpublish").session(revoked.session()).with(csrf()))
                .andExpect(status().isForbidden());
    }

    private long createOffering(Authenticated supplier, String title, String description, String category) throws Exception {
        MvcResult result = mvc.perform(post(OFFERINGS).session(supplier.session()).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content(offeringJson(title, description, category)))
                .andExpect(status().isCreated()).andReturn();
        return ((Number) com.jayway.jsonpath.JsonPath.read(result.getResponse().getContentAsString(), "$.id")).longValue();
    }

    private void publish(Authenticated supplier, long offeringId) throws Exception {
        mvc.perform(post(OFFERINGS + "/" + offeringId + "/publish").session(supplier.session()).with(csrf()))
                .andExpect(status().isOk());
    }

    private static String offeringJson(String title, String description, String category) {
        return "{" + field("title", title) + "," + field("description", description) + "," + field("category", category) + "}";
    }

    private Authenticated registerVerifyAndGrantSupplier(String organization, String email, String password) throws Exception {
        registerAndVerify(organization, email, password);
        Authenticated firstSession = login(email, password);
        long adminId = currentId(firstSession.session());
        mvc.perform(put(EMPLOYEES + "/" + adminId + "/access").session(firstSession.session()).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"organizationAdmin\":true,\"buyerGrant\":false,\"supplierGrant\":true,\"active\":true}"))
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
