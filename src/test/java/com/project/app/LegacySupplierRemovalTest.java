package com.project.app;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
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

/** TC-S2-012 -> REQ-S2-005, BL-S2-R1. Isolated PostgreSQL only; issue #11 scope. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("slice0-test")
@ContextConfiguration(initializers = Slice0DatabaseGuard.class)
class LegacySupplierRemovalTest {
    private static final String AUTH = "/api/auth";
    private static final String SUPPLIERS = "/api/suppliers";
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
        jdbc.update("DELETE FROM suppliers");
        for (Object principal : sessionRegistry.getAllPrincipals()) {
            for (SessionInformation session : sessionRegistry.getAllSessions(principal, true)) {
                sessionRegistry.removeSessionInformation(session.getSessionId());
            }
        }
        reset(mailSender);
    }

    @Test
    void migratesV1ThroughV4AndSuppliersTableStillExists() {
        List<String> applied = jdbc.queryForList(
                "SELECT 'v' || version FROM flyway_schema_history WHERE success ORDER BY installed_rank", String.class);
        // Later slices add further migrations; REQ-S2-005 only requires V1-V4 to have applied, in order, as a prefix.
        assertEquals(List.of("v1", "v2", "v3", "v4"), applied.subList(0, 4));
        assertEquals(1, jdbc.queryForObject(
                "SELECT count(*) FROM information_schema.tables WHERE table_schema='public' AND table_name='suppliers'",
                Integer.class));
    }

    @Test
    void allFiveLegacyRoutesReturnNonTwoXxForAnonymousAndAuthenticatedCallersAndNeverChangeData() throws Exception {
        long seededId = jdbc.queryForObject(
                "INSERT INTO suppliers (name) VALUES ('Legacy Atlas Supplies') RETURNING id", Long.class);

        mvc.perform(get(SUPPLIERS)).andExpect(status().isUnauthorized());
        mvc.perform(get(SUPPLIERS + "/" + seededId)).andExpect(status().isUnauthorized());
        assertNonTwoXx(mvc.perform(post(SUPPLIERS).contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Hijack\"}"))
                .andReturn().getResponse().getStatus());
        assertNonTwoXx(mvc.perform(put(SUPPLIERS + "/" + seededId).contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Hijack\"}")).andReturn().getResponse().getStatus());
        assertNonTwoXx(mvc.perform(delete(SUPPLIERS + "/" + seededId)).andReturn().getResponse().getStatus());

        Authenticated account = registerVerifyAndLogin("Legacy Org", "legacy-admin@example.com", "l".repeat(12));
        mvc.perform(get(SUPPLIERS).session(account.session())).andExpect(status().isNotFound());
        mvc.perform(get(SUPPLIERS + "/" + seededId).session(account.session())).andExpect(status().isNotFound());
        mvc.perform(post(SUPPLIERS).session(account.session()).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Hijack\"}")).andExpect(status().isNotFound());
        mvc.perform(put(SUPPLIERS + "/" + seededId).session(account.session()).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Hijack\"}")).andExpect(status().isNotFound());
        mvc.perform(delete(SUPPLIERS + "/" + seededId).session(account.session()).with(csrf())).andExpect(status().isNotFound());

        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM suppliers", Integer.class));
        assertEquals("Legacy Atlas Supplies", jdbc.queryForObject("SELECT name FROM suppliers WHERE id=?", String.class, seededId));
    }

    @Test
    void legacySupplierRowNeverAppearsInThePublicCatalog() throws Exception {
        long seededId = jdbc.queryForObject(
                "INSERT INTO suppliers (name) VALUES ('Legacy Atlas Supplies') RETURNING id", Long.class);
        mvc.perform(get("/api/public/offerings").param("q", "Atlas")).andExpect(status().isOk())
                .andExpect(jsonPath("$.items", org.hamcrest.Matchers.hasSize(0)));
        mvc.perform(get("/api/public/suppliers/" + seededId)).andExpect(status().isNotFound());
    }

    private static void assertNonTwoXx(int status) {
        assertTrue(status < 200 || status >= 300, "expected a non-2xx status but got " + status);
        assertTrue(status < 500, "expected no 5xx status but got " + status);
    }

    private Authenticated registerVerifyAndLogin(String organization, String email, String password) throws Exception {
        mvc.perform(post(AUTH + "/register").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{" + field("organizationName", organization) + "," + field("fullName", "Admin") + ","
                        + field("email", email) + "," + field("password", password) + "}"))
                .andExpect(status().isCreated());
        String token = latestToken();
        mvc.perform(post(AUTH + "/verify").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{" + field("token", token) + "}")).andExpect(status().isOk());
        MvcResult result = mvc.perform(post(AUTH + "/login").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{" + field("email", email) + "," + field("password", password) + "}"))
                .andExpect(status().isOk()).andReturn();
        return new Authenticated((MockHttpSession) result.getRequest().getSession(false));
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
