package com.project.app.demo;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/** BL-WD-R1 / TC-WD-003..005; never starts without explicitly named isolated demo database. */
@EnabledIfEnvironmentVariable(named="PROCUREFLOW_DEMO_DB_URL",matches="jdbc:postgresql://localhost:5332/procureflow_demo_[a-z0-9_]+")
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("demo")
class DemoWorkspaceIntegrationTest {
    @Autowired JdbcTemplate jdbc;
    @Autowired MockMvc mvc;
    @Autowired Environment env;
    @Autowired PasswordEncoder encoder;
    @Autowired DemoDataSeeder seeder;
    @Autowired jakarta.persistence.EntityManager entityManager;

    @Test void seedHasExactRolesProductsAndCategoriesAndEncodedPassword() throws Exception {
        assertEquals(3,jdbc.queryForObject("select count(*) from accounts",Integer.class));
        assertEquals(20,jdbc.queryForObject("select count(*) from marketplace_offerings where published",Integer.class));
        assertEquals(10,jdbc.queryForObject("select count(distinct category) from marketplace_offerings",Integer.class));
        assertEquals(1,jdbc.queryForObject("select count(*) from accounts where buyer_grant and not supplier_grant",Integer.class));
        assertEquals(2,jdbc.queryForObject("select count(*) from accounts where supplier_grant and not buyer_grant",Integer.class));
        assertEquals(0,jdbc.queryForObject("select count(*) from identity_tokens",Integer.class));
        String hash=jdbc.queryForObject("select password_hash from accounts where buyer_grant",String.class);
        assertNotEquals(env.getRequiredProperty("PROCUREFLOW_DEMO_PASSWORD"),hash);
        assertTrue(encoder.matches(env.getRequiredProperty("PROCUREFLOW_DEMO_PASSWORD"),hash));
        mvc.perform(get("/api/public/categories")).andExpect(jsonPath("$.length()").value(10));
        mvc.perform(get("/api/public/offerings").param("category","PACKAGING"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(2));
        for(String email:new String[]{"amina.bennani@northstar.invalid","karim.mansouri@atlas-supply.invalid","lina.haddad@meridian-goods.invalid"}) {
            MockHttpSession session=login(email);
            mvc.perform(get("/api/auth/me").session(session)).andExpect(status().isOk());
        }
    }

    @Test void repeatAndConcurrentRunsPreserveExistingRowsAndPasswords() {
        var before=jdbc.queryForList("select id,email,password_hash,version from accounts order by id");
        CompletableFuture.allOf(CompletableFuture.runAsync(()->seeder.run(new DefaultApplicationArguments())),
                CompletableFuture.runAsync(()->seeder.run(new DefaultApplicationArguments()))).join();
        assertEquals(before,jdbc.queryForList("select id,email,password_hash,version from accounts order by id"));
        assertEquals(20,jdbc.queryForObject("select count(*) from marketplace_offerings",Integer.class));
    }

    @Test @Transactional void editsOwnProfileAndOrganizationWithIsolationAndCsrf() throws Exception {
        MockHttpSession buyer=login("amina.bennani@northstar.invalid");
        mvc.perform(put("/api/auth/profile").session(buyer).contentType(MediaType.APPLICATION_JSON)
            .content("{\"fullName\":\"Changed Buyer\"}")).andExpect(status().isForbidden());
        mvc.perform(put("/api/auth/profile").session(buyer).with(csrf()).contentType(MediaType.APPLICATION_JSON)
            .content("{\"fullName\":\" Changed Buyer \",\"organizationId\":99999}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.fullName").value("Changed Buyer"));
        mvc.perform(put("/api/auth/organization").session(buyer).with(csrf()).contentType(MediaType.APPLICATION_JSON)
            .content("{\"displayName\":\"New Buyer Studio\"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.organizationName").value("New Buyer Studio"));
        assertEquals(2,jdbc.queryForObject("select count(*) from organizations where display_name in ('Atlas Industrial Supply','Meridian Business Goods')",Integer.class));
        mvc.perform(put("/api/auth/profile").session(buyer).with(csrf()).contentType(MediaType.APPLICATION_JSON)
            .content("{\"fullName\":\" "+"x".repeat(160)+" \"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.fullName").value("x".repeat(160)));
        mvc.perform(put("/api/auth/organization").session(buyer).with(csrf()).contentType(MediaType.APPLICATION_JSON)
            .content("{\"displayName\":\" "+"y".repeat(160)+" \"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.organizationName").value("y".repeat(160)));
        for(String content:new String[]{"{\"fullName\":\"   \"}","{\"fullName\":\""+"x".repeat(161)+"\"}"})
            mvc.perform(put("/api/auth/profile").session(buyer).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(content)).andExpect(status().isBadRequest());
        jdbc.update("update accounts set organization_admin=false where email='amina.bennani@northstar.invalid'");
        entityManager.clear(); // A later HTTP request has a fresh persistence context after external access changes.
        mvc.perform(put("/api/auth/organization").session(buyer).with(csrf()).contentType(MediaType.APPLICATION_JSON)
            .content("{\"displayName\":\"Forbidden\"}")).andExpect(status().isForbidden());
        jdbc.update("update accounts set active=false where email='amina.bennani@northstar.invalid'");
        entityManager.clear();
        mvc.perform(put("/api/auth/profile").session(buyer).with(csrf()).contentType(MediaType.APPLICATION_JSON)
            .content("{\"fullName\":\"Forbidden\"}")).andExpect(status().isForbidden());
    }

    @Test @Transactional void repeatPreservesEditedProductAndPartialSeedFailsWithoutWrites() {
        jdbc.update("update marketplace_offerings set description='Owner edited demo product',published=false where id=(select min(id) from marketplace_offerings)");
        seeder.run(new DefaultApplicationArguments());
        assertEquals(1,jdbc.queryForObject("select count(*) from marketplace_offerings where description='Owner edited demo product' and not published",Integer.class));
        jdbc.update("update accounts set email='collision@procureflow.invalid' where email='amina.bennani@northstar.invalid'");
        var before=jdbc.queryForList("select id,email from accounts order by id");
        assertThrows(IllegalStateException.class,()->seeder.run(new DefaultApplicationArguments()));
        assertEquals(before,jdbc.queryForList("select id,email from accounts order by id"));
    }

    private MockHttpSession login(String email) throws Exception {
        String body="{\"email\":\""+email+"\",\"password\":\""+env.getRequiredProperty("PROCUREFLOW_DEMO_PASSWORD")+"\"}";
        return (MockHttpSession)mvc.perform(post("/api/auth/login").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isOk()).andReturn().getRequest().getSession(false);
    }
}
