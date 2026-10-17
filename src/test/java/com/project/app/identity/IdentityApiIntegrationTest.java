package com.project.app.identity;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.Duration;
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

import com.project.app.identity.security.SessionLifetimeFilter;
import com.project.app.support.Slice0DatabaseGuard;

/** TC-S1-001..011/013 -> REQ-S1-001..006@2, BL-S1-R2. Isolated PostgreSQL only. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("slice0-test")
@ContextConfiguration(initializers = Slice0DatabaseGuard.class)
class IdentityApiIntegrationTest {
    private static final String AUTH = "/api/auth";
    private static final String EMPLOYEES = "/api/employees";
    private static final Pattern TOKEN = Pattern.compile("(?:verify|invite)=([A-Za-z0-9_-]+)");

    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired SessionRegistry sessionRegistry;
    @MockitoBean JavaMailSender mailSender;

    @BeforeEach
    void clearIdentityRows() {
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
    void browserShellAndIdentityAssetsArePublicWhileAccountApiRemainsProtected() throws Exception {
        mvc.perform(get("/index.html")).andExpect(status().isOk()).andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("ProcureFlow")));
        String html=mvc.perform(get("/index.html")).andReturn().getResponse().getContentAsString();
        var assets=Pattern.compile("(?:src|href)=\"(/assets/[^\"]+)\"").matcher(html);
        int assetCount=0;
        while(assets.find()){mvc.perform(get(assets.group(1))).andExpect(status().isOk());assetCount++;}
        assertTrue(assetCount>=2,"The public React shell must serve both script and style assets.");
        mvc.perform(get(AUTH + "/me")).andExpect(status().isUnauthorized());
    }

    @Test
    void registrationRollsBackWhenVerificationDeliveryFails() throws Exception {
        doThrow(new IllegalStateException("mail transport unavailable")).when(mailSender).send(any(SimpleMailMessage.class));
        mvc.perform(post(AUTH + "/register").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{" + field("organizationName", "Rollback Org") + "," + field("fullName", "Admin") + ","
                        + field("email", "rollback@example.com") + "," + field("password", "r".repeat(12)) + "}"))
                .andExpect(status().isServiceUnavailable());
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM organizations", Integer.class));
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM accounts", Integer.class));
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM identity_tokens", Integer.class));
    }

    @Test
    void expiredVerificationAndInvitationTokensCannotBeUsed() throws Exception {
        mvc.perform(post(AUTH + "/register").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{" + field("organizationName", "Expiry Org") + "," + field("fullName", "Admin") + ","
                        + field("email", "expiry@example.com") + "," + field("password", "e".repeat(12)) + "}"))
                .andExpect(status().isCreated());
        String verification = latestToken();
        jdbc.update("UPDATE identity_tokens SET expires_at = TIMESTAMPTZ '2000-01-01 00:00:00+00' WHERE purpose='VERIFICATION'");
        mvc.perform(post(AUTH + "/verify").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{" + field("token", verification) + "}"))
                .andExpect(status().isBadRequest());
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM accounts WHERE email_verified", Integer.class));

        // Mark the registrant verified only within this test to exercise the admin invite API.
        jdbc.update("UPDATE accounts SET email_verified=TRUE");
        Authenticated admin = login("expiry@example.com", "e".repeat(12));
        mvc.perform(post(EMPLOYEES + "/invitations").session(admin.session()).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content("{" + field("email", "newhire@example.com") + "}"))
                .andExpect(status().isCreated());
        String invitation = latestToken();
        jdbc.update("UPDATE identity_tokens SET expires_at = TIMESTAMPTZ '2000-01-01 00:00:00+00' WHERE purpose='INVITATION'");
        mvc.perform(post(AUTH + "/invitations/accept").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{" + field("token", invitation) + "," + field("fullName", "New Hire") + ","
                        + field("password", "n".repeat(12)) + "}"))
                .andExpect(status().isBadRequest());
        assertEquals(0, jdbc.queryForObject("SELECT count(*) FROM accounts WHERE email='newhire@example.com'", Integer.class));
    }

    @Test
    void organizationIsolationCoversListAndMutationAndGrantMatrixRevokesSessions() throws Exception {
        registerAndVerify("Matrix A", "matrix-admin@example.com", "a".repeat(12));
        Authenticated adminA = login("matrix-admin@example.com", "a".repeat(12));
        mvc.perform(post(EMPLOYEES + "/invitations").session(adminA.session()).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content("{" + field("email", "matrix-user@example.com") + "}"))
                .andExpect(status().isCreated());
        String invitation = latestToken();
        mvc.perform(post(AUTH + "/invitations/accept").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{" + field("token", invitation) + "," + field("fullName", "Matrix User") + ","
                        + field("password", "m".repeat(12)) + "}"))
                .andExpect(status().isCreated());
        String verification = latestToken();
        mvc.perform(post(AUTH + "/verify").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{" + field("token", verification) + "}")).andExpect(status().isOk());
        Authenticated employee = login("matrix-user@example.com", "m".repeat(12));
        long employeeId = currentId(employee.session());

        registerAndVerify("Matrix B", "other-admin@example.com", "b".repeat(12));
        Authenticated adminB = login("other-admin@example.com", "b".repeat(12));
        mvc.perform(get(EMPLOYEES).session(adminB.session())).andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.email == 'matrix-user@example.com')]", org.hamcrest.Matchers.hasSize(0)));
        mvc.perform(put(EMPLOYEES + "/" + employeeId + "/access").session(adminB.session()).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"organizationAdmin\":false,\"buyerGrant\":true,\"supplierGrant\":true,\"active\":false}"))
                .andExpect(status().isNotFound());
        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM accounts WHERE id=? AND active AND NOT buyer_grant AND NOT supplier_grant", Integer.class, employeeId));

        // Start at buyer-only so every matrix transition changes authority and must revoke the live session.
        mvc.perform(put(EMPLOYEES + "/" + employeeId + "/access").session(adminA.session()).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"organizationAdmin\":false,\"buyerGrant\":true,\"supplierGrant\":false,\"active\":true}"))
                .andExpect(status().isOk());

        boolean[][] grants = {{false,false},{true,false},{false,true},{true,true}};
        for (boolean[] grant : grants) {
            Authenticated currentEmployeeSession = login("matrix-user@example.com", "m".repeat(12));
            mvc.perform(put(EMPLOYEES + "/" + employeeId + "/access").session(adminA.session()).with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"organizationAdmin\":false,\"buyerGrant\":" + grant[0] + ",\"supplierGrant\":" + grant[1] + ",\"active\":true}"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.buyerGrant").value(grant[0]))
                    .andExpect(jsonPath("$.supplierGrant").value(grant[1]));
            mvc.perform(get(AUTH + "/me").session(currentEmployeeSession.session())).andExpect(status().isUnauthorized());
        }
        Authenticated activeEmployee = login("matrix-user@example.com", "m".repeat(12));
        mvc.perform(put(EMPLOYEES + "/" + employeeId + "/access").session(adminA.session()).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"organizationAdmin\":false,\"buyerGrant\":false,\"supplierGrant\":false,\"active\":false}"))
                .andExpect(status().isOk());
        mvc.perform(get(AUTH + "/me").session(activeEmployee.session())).andExpect(status().isUnauthorized());
        mvc.perform(post(AUTH + "/login").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{" + field("email", "matrix-user@example.com") + "," + field("password", "m".repeat(12)) + "}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void passwordChangeRevokesCurrentSessionAndIdleTimeoutIsThirtyMinutes() throws Exception {
        String oldPassword = "p".repeat(12);
        registerAndVerify("Password Org", "password-admin@example.com", oldPassword);
        Authenticated admin = login("password-admin@example.com", oldPassword);
        assertEquals(30 * 60, admin.session().getMaxInactiveInterval());
        mvc.perform(put(AUTH + "/password").session(admin.session()).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{" + field("currentPassword", oldPassword) + "," + field("newPassword", "q".repeat(12)) + "}"))
                .andExpect(status().isNoContent());
        mvc.perform(get(AUTH + "/me").session(admin.session())).andExpect(status().isUnauthorized());
        login("password-admin@example.com", "q".repeat(12));
        mvc.perform(post(AUTH + "/login").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{" + field("email", "password-admin@example.com") + "," + field("password", oldPassword) + "}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void allSpacePasswordIsAcceptedExactlyAtEveryHttpBoundary() throws Exception {
        String spaces = " ".repeat(12);
        registerAndVerify("Spaces Org", "spaces-admin@example.com", spaces);
        Authenticated admin = login("spaces-admin@example.com", spaces);
        mvc.perform(post(AUTH + "/login").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{" + field("email", "spaces-admin@example.com") + "," + field("password", " ".repeat(13)) + "}"))
                .andExpect(status().isUnauthorized());

        mvc.perform(post(EMPLOYEES + "/invitations").session(admin.session()).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content("{" + field("email", "spaces-employee@example.com") + "}"))
                .andExpect(status().isCreated());
        String invitation = latestToken();
        mvc.perform(post(AUTH + "/invitations/accept").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{" + field("token", invitation) + "," + field("fullName", "Spaces Employee") + ","
                        + field("password", spaces) + "}"))
                .andExpect(status().isCreated());
        String verification = latestToken();
        mvc.perform(post(AUTH + "/verify").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{" + field("token", verification) + "}")).andExpect(status().isOk());
        login("spaces-employee@example.com", spaces);

        mvc.perform(put(AUTH + "/password").session(admin.session()).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{" + field("currentPassword", spaces) + "," + field("newPassword", "new  passphrase") + "}"))
                .andExpect(status().isNoContent());
        Authenticated changed = login("spaces-admin@example.com", "new  passphrase");
        mvc.perform(put(AUTH + "/password").session(changed.session()).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{" + field("currentPassword", "new  passphrase") + "," + field("newPassword", spaces) + "}"))
                .andExpect(status().isNoContent());
        login("spaces-admin@example.com", spaces);
    }

    @Test
    void unverifiedPromotedAdminCannotReplaceLastEligibleAdmin() throws Exception {
        registerAndVerify("Admin Guard Org", "guard-admin@example.com", "g".repeat(12));
        Authenticated admin = login("guard-admin@example.com", "g".repeat(12));
        long adminId = currentId(admin.session());
        mvc.perform(post(EMPLOYEES + "/invitations").session(admin.session()).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content("{" + field("email", "pending-admin@example.com") + "}"))
                .andExpect(status().isCreated());
        String invitation = latestToken();
        mvc.perform(post(AUTH + "/invitations/accept").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{" + field("token", invitation) + "," + field("fullName", "Pending Admin") + ","
                        + field("password", "p".repeat(12)) + "}"))
                .andExpect(status().isCreated());
        String verification = latestToken();
        long pendingId = jdbc.queryForObject("SELECT id FROM accounts WHERE email='pending-admin@example.com'", Long.class);
        mvc.perform(put(EMPLOYEES + "/" + pendingId + "/access").session(admin.session()).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"organizationAdmin\":true,\"buyerGrant\":false,\"supplierGrant\":false,\"active\":true}"))
                .andExpect(status().isOk());
        mvc.perform(put(EMPLOYEES + "/" + adminId + "/access").session(admin.session()).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"organizationAdmin\":false,\"buyerGrant\":false,\"supplierGrant\":false,\"active\":true}"))
                .andExpect(status().isConflict());
        mvc.perform(put(EMPLOYEES + "/" + adminId + "/access").session(admin.session()).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"organizationAdmin\":true,\"buyerGrant\":false,\"supplierGrant\":false,\"active\":false}"))
                .andExpect(status().isConflict());
        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM accounts WHERE organization_admin AND active AND email_verified", Integer.class));
        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM accounts WHERE id=? AND organization_admin AND active", Integer.class, adminId));

        mvc.perform(post(AUTH + "/verify").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{" + field("token", verification) + "}")).andExpect(status().isOk());
        mvc.perform(put(EMPLOYEES + "/" + adminId + "/access").session(admin.session()).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"organizationAdmin\":false,\"buyerGrant\":false,\"supplierGrant\":false,\"active\":true}"))
                .andExpect(status().isOk());
        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM accounts WHERE organization_admin AND active AND email_verified", Integer.class));
        login("pending-admin@example.com", "p".repeat(12));
    }

    @Test
    void registrationVerificationLoginAndPasswordBoundaries() throws Exception {
        String password = "  ".repeat(6) + "😀".repeat(40) + "x".repeat(8);
        String body = "{" + field("organizationName", "Northwind") + "," + field("fullName", "A Admin") + ","
                + field("email", "ADMIN@Example.com") + "," + field("password", password) + "}";
        String response = mvc.perform(post(AUTH + "/register").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.message").exists())
                .andReturn().getResponse().getContentAsString();
        assertFalse(response.contains(password));
        assertFalse(response.contains("token="));
        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM organizations", Integer.class));
        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM accounts", Integer.class));
        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM accounts WHERE email='admin@example.com' AND organization_admin AND NOT buyer_grant AND NOT supplier_grant AND NOT email_verified", Integer.class));
        assertNotEquals(password, jdbc.queryForObject("SELECT password_hash FROM accounts", String.class));
        String verifyToken = latestToken();
        assertNotEquals(verifyToken, jdbc.queryForObject("SELECT token_hash FROM identity_tokens", String.class));

        mvc.perform(post(AUTH + "/register").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{" + field("organizationName", "Duplicate") + "," + field("fullName", "Other") + ","
                        + field("email", "admin@example.com") + "," + field("password", password) + "}"))
                .andExpect(status().isConflict());
        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM organizations", Integer.class));
        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM accounts", Integer.class));

        mvc.perform(post(AUTH + "/login").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{" + field("email", "admin@example.com") + "," + field("password", password) + "}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(post(AUTH + "/verify").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{" + field("token", verifyToken) + "}"))
                .andExpect(status().isOk());
        mvc.perform(post(AUTH + "/verify").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{" + field("token", verifyToken) + "}"))
                .andExpect(status().isBadRequest());

        MvcResult login = mvc.perform(post(AUTH + "/login").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{" + field("email", "ADMIN@example.com") + "," + field("password", password) + "}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.organizationAdmin").value(true))
                .andExpect(jsonPath("$.buyerGrant").value(false)).andReturn();
        MockHttpSession session = (MockHttpSession) login.getRequest().getSession(false);
        assertNotNull(session);
        mvc.perform(get(AUTH + "/me").session(session)).andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("admin@example.com"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
        session.setAttribute(SessionLifetimeFilter.AUTHENTICATED_AT,
                System.currentTimeMillis() - Duration.ofHours(12).toMillis() - 1);
        mvc.perform(get(AUTH + "/me").session(session)).andExpect(status().isUnauthorized());

        mvc.perform(post(AUTH + "/register").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{" + field("organizationName", "Invalid") + "," + field("fullName", "Short") + ","
                        + field("email", "short@example.com") + "," + field("password", "x".repeat(11)) + "}"))
                .andExpect(status().isBadRequest());
        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM organizations", Integer.class));
    }

    @Test
    void organizationAdminControlsOnlyOwnEmployeesAndCannotRemoveLastAdmin() throws Exception {
        String adminPassword = "a".repeat(12);
        registerAndVerify("Org A", "a@example.com", adminPassword);
        Authenticated adminA = login("a@example.com", adminPassword);
        long adminAId = currentId(adminA.session());
        mvc.perform(put(EMPLOYEES + "/" + adminAId + "/access").session(adminA.session()).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"organizationAdmin\":false,\"buyerGrant\":false,\"supplierGrant\":false,\"active\":true}"))
                .andExpect(status().isConflict());
        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM accounts WHERE organization_admin AND active", Integer.class));

        mvc.perform(post(EMPLOYEES + "/invitations").session(adminA.session()).with(csrf())
                .contentType(MediaType.APPLICATION_JSON).content("{" + field("email", "employee@example.com") + "}"))
                .andExpect(status().isCreated());
        String invitation = latestToken();
        mvc.perform(post(AUTH + "/invitations/accept").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{" + field("token", invitation) + "," + field("fullName", "Employee") + ","
                        + field("password", "e".repeat(12)) + "}"))
                .andExpect(status().isCreated());
        String verification = latestToken();
        mvc.perform(post(AUTH + "/verify").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{" + field("token", verification) + "}")).andExpect(status().isOk());
        Authenticated employee = login("employee@example.com", "e".repeat(12));
        long employeeId = currentId(employee.session());
        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM identity_tokens WHERE purpose='INVITATION' AND consumed_at IS NOT NULL", Integer.class));

        String adminBPassword = "b".repeat(12);
        registerAndVerify("Org B", "b@example.com", adminBPassword);
        Authenticated adminB = login("b@example.com", adminBPassword);
        mvc.perform(get(EMPLOYEES + "/" + employeeId).session(adminB.session()))
                .andExpect(status().isNotFound());
        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM accounts WHERE email='employee@example.com'", Integer.class));
        mvc.perform(get(EMPLOYEES).session(employee.session())).andExpect(status().isForbidden());

        mvc.perform(put(EMPLOYEES + "/" + employeeId + "/access").session(adminA.session()).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"organizationAdmin\":false,\"buyerGrant\":true,\"supplierGrant\":true,\"active\":true}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.buyerGrant").value(true))
                .andExpect(jsonPath("$.supplierGrant").value(true));
        assertEquals(1, jdbc.queryForObject("SELECT count(*) FROM accounts WHERE email='employee@example.com' AND buyer_grant AND supplier_grant", Integer.class));
        mvc.perform(get(AUTH + "/me").session(employee.session())).andExpect(status().isUnauthorized());
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
