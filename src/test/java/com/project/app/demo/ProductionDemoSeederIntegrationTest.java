package com.project.app.demo;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import com.project.app.support.Slice0DatabaseGuard;

/** TC-FDP-002/003: real isolated database, rollback all test mutations. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("slice0-test")
@ContextConfiguration(initializers=Slice0DatabaseGuard.class)
@EnabledIfEnvironmentVariable(named="PROCUREFLOW_FDP_TESTS",matches="true")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ProductionDemoSeederIntegrationTest {
    static final String PASSWORD="Distinct-Test-Password-2026!";
    @Autowired JdbcTemplate jdbc;
    @Autowired PasswordEncoder encoder;
    @Autowired PlatformTransactionManager transactions;
    @Autowired MockMvc mvc;
    @MockitoBean JavaMailSender mail;

    private ProductionDemoSeeder runner(String database) {
        var env=new MockEnvironment().withProperty("PROCUREFLOW_PRODUCTION_DEMO_PASSWORD",PASSWORD)
            .withProperty("PROCUREFLOW_PRODUCTION_DEMO_DATABASE",database);
        env.setActiveProfiles("deploy");
        return new ProductionDemoSeeder(jdbc,encoder,env,transactions);
    }
    private ProductionDemoSeeder runner() { return runner(jdbc.queryForObject("select current_database()",String.class)); }
    private void seed() { runner().run(new DefaultApplicationArguments()); }
    private void rollback(Runnable action) {
        new TransactionTemplate(transactions).executeWithoutResult(s->{try {action.run();} finally {s.setRollbackOnly();}});
    }

    @Test @Order(1) void lateFailureRollsBackAndConcurrentFirstExecutionCreatesOneFixture() {
        assertEquals(0,count("accounts"));
        jdbc.execute("create function fdp_reject_receipt() returns trigger language plpgsql as $$ begin raise exception 'Injected fixture receipt failure'; end $$");
        jdbc.execute("create trigger fdp_reject_receipt before insert on demo_seed_completions for each row execute function fdp_reject_receipt()");
        try {
            assertThrows(org.springframework.dao.DataAccessException.class,this::seed);
            for(String table:new String[]{"accounts","organizations","marketplace_profiles","marketplace_offerings",
                    "rfqs","rfq_items","rfq_invitations","quotations","quotation_items","purchase_orders",
                    "purchase_order_items","order_events","demo_seed_completions"}) assertEquals(0,count(table),table);
        } finally {
            jdbc.execute("drop trigger fdp_reject_receipt on demo_seed_completions");
            jdbc.execute("drop function fdp_reject_receipt()");
        }
        CompletableFuture.allOf(CompletableFuture.runAsync(this::seed),CompletableFuture.runAsync(this::seed)).join();
        assertEquals(1,count("demo_seed_completions"));
        assertEquals(6,count("accounts"));
    }

    @Test void exactFixtureCommercialRelationsAndOrdinaryLoginIsolation() throws Exception {
        // Commit once in this exclusively owned database for HTTP requests and concurrency tests.
        seed();
        assertEquals(6,count("accounts")); assertEquals(3,count("organizations"));
        assertEquals(20,count("marketplace_offerings")); assertEquals(7,count("rfqs"));
        assertEquals(6,count("quotations")); assertEquals(4,count("purchase_orders"));
        assertEquals(12,count("rfq_invitations")); assertEquals(9,count("order_events"));
        assertEquals(0,count("identity_tokens"));
        assertEquals(10,jdbc.queryForObject("select count(distinct category) from marketplace_offerings",Integer.class));
        assertEquals(3,jdbc.queryForObject("select count(*) from accounts where organization_admin",Integer.class));
        assertEquals(6,jdbc.queryForObject("select count(*) from accounts where active and email_verified and buyer_grant<>supplier_grant",Integer.class));
        assertEquals(0,jdbc.queryForObject("""
            select count(*) from purchase_orders o join rfqs r on r.id=o.rfq_id
            join quotations q on q.id=o.quotation_id join rfq_invitations i on i.id=q.invitation_id
            where r.winning_quotation_id<>q.id or q.status<>'ACCEPTED' or r.status<>'AWARDED'
                or i.rfq_id<>r.id or i.supplier_organization_id<>o.supplier_organization_id
                or r.organization_id<>o.buyer_organization_id or o.currency<>q.currency or o.terms<>q.terms
            """,Integer.class));
        assertEquals(0,jdbc.queryForObject("""
            select count(*) from purchase_order_items p join rfq_items r on r.id=p.rfq_item_id
            join purchase_orders o on o.id=p.order_id join quotation_items q on q.quotation_id=o.quotation_id and q.rfq_item_id=r.id
            where p.quantity<>r.quantity or p.description<>r.description or p.unit<>r.unit
                or p.unit_price<>q.unit_price or p.line_total<>p.quantity*p.unit_price
            """,Integer.class));
        assertEquals(0,jdbc.queryForObject("""
            select count(*) from order_events e join purchase_orders o on o.id=e.order_id join accounts a on a.id=e.acting_account_id
            where e.occurred_at<=o.accepted_at or a.organization_id<>case when e.target_status='RECEIVED'
                then o.buyer_organization_id else o.supplier_organization_id end
            """,Integer.class));
        for(String email:ProductionDemoSeeder.EMAILS) {
            assertTrue(encoder.matches(PASSWORD,jdbc.queryForObject("select password_hash from accounts where email=?",String.class,email)));
            mvc.perform(post("/api/auth/login").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\""+email+"\",\"password\":\""+PASSWORD+"\"}"))
                .andExpect(status().isOk());
        }
        mvc.perform(get("/api/orders")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
            .content("{\"email\":\""+ProductionDemoSeeder.EMAILS.get(0)+"\",\"password\":\""+PASSWORD+"\"}"))
            .andExpect(status().isForbidden());
        verifyNoInteractions(mail);
    }
    @Test void concurrentStartupSerializesAndRerunPreservesMutableDataAndSentinel() {
        seed();
        CompletableFuture.allOf(CompletableFuture.runAsync(this::seed),CompletableFuture.runAsync(this::seed)).join();
        rollback(()->{
            jdbc.update("insert into organizations(display_name) values ('Unrelated sentinel')");
            jdbc.update("update organizations set display_name='Owner renamed demo' where id=(select organization_id from accounts where email=?)",ProductionDemoSeeder.EMAILS.get(0));
            jdbc.update("update accounts set buyer_grant=false,full_name='Owner edit' where email=?",ProductionDemoSeeder.EMAILS.get(0));
            jdbc.update("delete from marketplace_offerings where id=(select min(id) from marketplace_offerings)");
            var before=jdbc.queryForList("select * from accounts order by id");
            seed();
            assertEquals(before,jdbc.queryForList("select * from accounts order by id"));
            assertEquals(19,count("marketplace_offerings"));
            assertEquals(1,jdbc.queryForObject("select count(*) from organizations where display_name='Unrelated sentinel'",Integer.class));
        });
    }
    @Test void partialCollisionChangedBindingAndTargetMismatchRefuseWithoutWrites() {
        seed();
        rollback(()->{
            jdbc.update("delete from demo_seed_completions");
            int organizations=count("organizations");
            assertThrows(IllegalStateException.class,this::seed);
            assertEquals(organizations,count("organizations"));
        });
        rollback(()->{
            jdbc.update("update accounts set email='changed@procureflow.invalid' where email=?",ProductionDemoSeeder.EMAILS.get(0));
            assertThrows(IllegalStateException.class,this::seed);
            assertEquals(6,count("accounts"));
        });
        assertThrows(IllegalStateException.class,()->runner("unexpected_database").run(new DefaultApplicationArguments()));
        assertEquals(1,count("demo_seed_completions"));
    }
    private int count(String table) { return jdbc.queryForObject("select count(*) from "+table,Integer.class); }
}
