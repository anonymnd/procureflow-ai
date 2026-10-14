package com.project.app.procurement;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.*;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.*;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import com.project.app.identity.repository.AccountRepository;
import com.project.app.identity.security.AccountPrincipal;
import com.project.app.procurement.service.*;
import com.project.app.support.Slice0DatabaseGuard;

/** BL-S5-R1 / #19/#20 / TC-S5-001..010/013. Real isolated DB and HTTP. */
@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("slice0-test")
@ContextConfiguration(initializers=Slice0DatabaseGuard.class)
class AwardApiIntegrationTest {
    private static final Instant NOW=Instant.parse("2026-10-12T10:00:00Z"),FUTURE=Instant.parse("2026-10-20T10:00:00Z");
    private static String hash;
    @Autowired MockMvc mvc; @Autowired JdbcTemplate jdbc; @Autowired PasswordEncoder encoder;
    @Autowired AwardService awards; @Autowired QuotationService quotations; @Autowired AccountRepository accounts;
    @Autowired PlatformTransactionManager transactions; @MockitoBean Clock clock;
    private MockHttpSession buyer,b,c,d,mixed;
    private long rfq,buyerOrg,bOrg,cOrg,dOrg,invB,invC,invD,item,quoteB,quoteC;
    @BeforeEach void fixture() throws Exception {
        reset(clock);when(clock.instant()).thenReturn(NOW);
        jdbc.execute("DROP TRIGGER IF EXISTS s5_fail_item ON purchase_order_items");jdbc.execute("DROP FUNCTION IF EXISTS s5_fail_order_item()");
        jdbc.update("DELETE FROM order_events");jdbc.update("DELETE FROM purchase_order_items");jdbc.update("DELETE FROM purchase_orders");jdbc.update("UPDATE rfqs SET status='SENT',winning_quotation_id=NULL WHERE status='AWARDED'");
        for(String table:List.of("quotation_items","quotations","rfq_invitations","rfq_items","rfqs","supplier_contacts","marketplace_offerings","marketplace_profiles","identity_tokens","accounts","organizations"))jdbc.update("DELETE FROM "+table);
        if(hash==null)hash=encoder.encode("award passphrase 123");
        buyerOrg=org("Buyer snapshot");bOrg=org("Supplier B snapshot");cOrg=org("Supplier C");dOrg=org("Supplier D");
        account(buyerOrg,"a@award.test",true,true);account(bOrg,"b@award.test",false,true);account(cOrg,"c@award.test",false,true);account(dOrg,"d@award.test",false,true);account(bOrg,"mixed@award.test",true,false);
        buyer=login("a@award.test");b=login("b@award.test");c=login("c@award.test");d=login("d@award.test");mixed=login("mixed@award.test");
        rfq=jdbc.queryForObject("INSERT INTO rfqs(organization_id,title,status) VALUES (?,'Frozen RFQ','SENT') RETURNING id",Long.class,buyerOrg);
        item=jdbc.queryForObject("INSERT INTO rfq_items(rfq_id,description,quantity,unit) VALUES (?,'Source bolts',3,'box') RETURNING id",Long.class,rfq);
        invB=invite(bOrg);invC=invite(cOrg);invD=invite(dOrg);
        quoteB=quote(invB,"MAD","1.20",true);quoteC=quote(invC,"MAD","1.20",true);
    }
    @Test void comparisonGroupsStableTiesExpiredAndDraftPrivacy() throws Exception {
        long draft=quote(invD,"USD","0.10",false);
        mvc.perform(get(comparison()).session(buyer)).andExpect(status().isOk()).andExpect(jsonPath("$.groups",org.hamcrest.Matchers.hasSize(1)))
                .andExpect(jsonPath("$.groups[0].currency").value("MAD")).andExpect(jsonPath("$.groups[0].quotations[0].id").value(quoteB))
                .andExpect(jsonPath("$.groups[0].quotations[0].subtotal").value("3.60")).andExpect(jsonPath("$.groups[0].quotations[0].lowestEligible").value(true))
                .andExpect(jsonPath("$.groups[0].quotations[1].lowestEligible").value(false));
        jdbc.update("UPDATE quotation_items SET unit_price=0.90 WHERE quotation_id=?",quoteC);
        mvc.perform(get(comparison()).session(buyer)).andExpect(jsonPath("$.groups[0].quotations[0].id").value(quoteC))
                .andExpect(jsonPath("$.groups[0].quotations[0].subtotal").value("2.70"));
        jdbc.update("UPDATE quotations SET currency='EUR' WHERE id=?",quoteC);
        jdbc.update("UPDATE quotations SET valid_until=? WHERE id=?",java.sql.Timestamp.from(NOW),quoteB);
        jdbc.update("UPDATE quotations SET status='SUBMITTED',submitted_at=? WHERE id=?",java.sql.Timestamp.from(NOW),draft);
        mvc.perform(get(comparison()).session(buyer)).andExpect(jsonPath("$.groups",org.hamcrest.Matchers.hasSize(3)))
                .andExpect(jsonPath("$.groups[0].quotations[0].expired").value(true)).andExpect(jsonPath("$.groups[0].quotations[0].eligible").value(false))
                .andExpect(jsonPath("$.groups[1].currency").value("EUR")).andExpect(jsonPath("$.groups[2].currency").value("USD"));
        error(mvc.perform(get(comparison()).session(mixed)),404);mvc.perform(get(comparison()).session(b)).andExpect(status().isForbidden());mvc.perform(get(comparison())).andExpect(status().isUnauthorized());
        jdbc.update("UPDATE rfqs SET status='DRAFT' WHERE id=?",rfq);error(mvc.perform(get(comparison()).session(buyer)),409);
    }
    @Test void atomicAwardReplaySnapshotsAndParticipantRoles() throws Exception {
        MvcResult result=accept(buyer,quoteB).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("CREATED"))
                .andExpect(jsonPath("$.subtotal").value("3.60")).andExpect(jsonPath("$.items[0].description").value("Source bolts"))
                .andExpect(jsonPath("$.buyerDisplayName").value("Buyer snapshot")).andExpect(jsonPath("$.acceptingAccountId").doesNotExist()).andReturn();
        long order=orderId();String original=result.getResponse().getContentAsString();
        assertEquals("AWARDED",jdbc.queryForObject("SELECT status FROM rfqs WHERE id=?",String.class,rfq));assertEquals("ACCEPTED",jdbc.queryForObject("SELECT status FROM quotations WHERE id=?",String.class,quoteB));
        assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM purchase_order_items",Integer.class));assertNotNull(jdbc.queryForObject("SELECT accepting_account_id FROM purchase_orders",Long.class));
        mvc.perform(get(base()+"/quotations/"+quoteB).session(buyer)).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("ACCEPTED"));
        mvc.perform(get(quotePath(invB)).session(b)).andExpect(jsonPath("$.status").value("ACCEPTED"));
        when(clock.instant()).thenReturn(FUTURE.plusSeconds(1));
        assertEquals(original,accept(buyer,quoteB).andExpect(status().isOk()).andReturn().getResponse().getContentAsString());error(accept(buyer,quoteC),409);error(accept(mixed,quoteB),404);
        jdbc.update("UPDATE organizations SET display_name='Changed display' WHERE id IN (?,?)",buyerOrg,bOrg);
        long profile=jdbc.queryForObject("INSERT INTO marketplace_profiles(organization_id,description) VALUES (?,'Catalog changed') RETURNING id",Long.class,bOrg);
        jdbc.update("UPDATE marketplace_profiles SET description='Other catalog terms' WHERE id=?",profile);
        for(MockHttpSession participant:List.of(buyer,b))assertEquals(shared(original),shared(mvc.perform(get("/api/procurement/orders/"+order).session(participant)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString()));
        mvc.perform(get("/api/procurement/orders").session(buyer)).andExpect(jsonPath("$",org.hamcrest.Matchers.hasSize(1)));mvc.perform(get("/api/procurement/orders").session(b)).andExpect(jsonPath("$",org.hamcrest.Matchers.hasSize(1)));
        for(MockHttpSession denied:List.of(c,d,mixed))error(mvc.perform(get("/api/procurement/orders/"+order).session(denied)),404);
        mvc.perform(get("/api/procurement/orders").session(mixed)).andExpect(jsonPath("$").isEmpty());
        mvc.perform(get(comparison()).session(buyer)).andExpect(jsonPath("$.orderId").value(order)).andExpect(jsonPath("$.awardedQuotationId").value(quoteB)).andExpect(jsonPath("$.groups[0].quotations[0].eligible").value(false)).andExpect(jsonPath("$.groups[0].quotations[0].lowestEligible").value(false));
        mvc.perform(put("/api/procurement/orders/"+order).session(buyer).with(csrf())).andExpect(status().isMethodNotAllowed());
        assertThrows(DataIntegrityViolationException.class,()->jdbc.update("INSERT INTO purchase_order_items(order_id,rfq_item_id,description,quantity,unit,unit_price,line_total) VALUES (?,?,'duplicate',1,'box',1,1)",order,item));
        long otherRfq=jdbc.queryForObject("INSERT INTO rfqs(organization_id,title,status) VALUES (?,'Other RFQ','SENT') RETURNING id",Long.class,buyerOrg);
        String duplicate="INSERT INTO purchase_orders(rfq_id,quotation_id,buyer_organization_id,supplier_organization_id,accepting_account_id,rfq_title,buyer_display_name,supplier_display_name,status,currency,terms,valid_until,accepted_at) SELECT ?,?,buyer_organization_id,supplier_organization_id,accepting_account_id,rfq_title,buyer_display_name,supplier_display_name,status,currency,terms,valid_until,accepted_at FROM purchase_orders WHERE id=?";
        assertThrows(DataIntegrityViolationException.class,()->jdbc.update(duplicate,rfq,quoteC,order));
        assertThrows(DataIntegrityViolationException.class,()->jdbc.update(duplicate,otherRfq,quoteB,order));
        assertThrows(DataIntegrityViolationException.class,()->jdbc.update("UPDATE rfqs SET status='AWARDED',winning_quotation_id=? WHERE id=?",quoteB,otherRfq));
        long supplierAccount=jdbc.queryForObject("SELECT id FROM accounts WHERE email='b@award.test'",Long.class);
        mvc.perform(put("/api/employees/"+supplierAccount+"/access").session(b).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"organizationAdmin\":true,\"buyerGrant\":false,\"supplierGrant\":false,\"active\":true}")).andExpect(status().isOk());
        mvc.perform(get("/api/procurement/orders/"+order).session(b)).andExpect(status().isUnauthorized());assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM purchase_orders",Integer.class));
    }
    @Test void invalidAwardNoPartialEffectsAndRealDatabaseFailureRollback() throws Exception {
        error(accept(buyer,99999999),404);error(accept(mixed,quoteB),404);
        long otherRfq=jdbc.queryForObject("INSERT INTO rfqs(organization_id,title,status) VALUES (?,'Other','SENT') RETURNING id",Long.class,buyerOrg);
        long otherInvitation=jdbc.queryForObject("INSERT INTO rfq_invitations(rfq_id,supplier_organization_id) VALUES (?,?) RETURNING id",Long.class,otherRfq,bOrg);
        long otherQuote=jdbc.queryForObject("INSERT INTO quotations(invitation_id,status,currency,valid_until,submitted_at) VALUES (?,'SUBMITTED','MAD',?,?) RETURNING id",Long.class,otherInvitation,java.sql.Timestamp.from(FUTURE),java.sql.Timestamp.from(NOW));
        error(accept(buyer,otherQuote),404);unchanged();
        jdbc.update("UPDATE quotations SET status='DRAFT',submitted_at=NULL WHERE id=?",quoteB);error(accept(buyer,quoteB),409);
        jdbc.update("UPDATE quotations SET status='SUBMITTED',submitted_at=?,valid_until=? WHERE id=?",java.sql.Timestamp.from(NOW),java.sql.Timestamp.from(NOW),quoteB);error(accept(buyer,quoteB),409);
        jdbc.update("UPDATE quotations SET valid_until=? WHERE id=?",java.sql.Timestamp.from(FUTURE),quoteB);jdbc.update("DELETE FROM quotation_items WHERE quotation_id=?",quoteB);error(accept(buyer,quoteB),409);
        jdbc.update("INSERT INTO quotation_items(quotation_id,rfq_item_id,unit_price) VALUES (?,?,1.20)",quoteB,item);unchanged();
        jdbc.execute("CREATE FUNCTION s5_fail_order_item() RETURNS trigger LANGUAGE plpgsql AS 'BEGIN RAISE EXCEPTION ''forced order item failure'' USING ERRCODE = ''23514''; END'");
        jdbc.execute("CREATE TRIGGER s5_fail_item BEFORE INSERT ON purchase_order_items FOR EACH ROW EXECUTE FUNCTION s5_fail_order_item()");
        try {assertThrows(DataIntegrityViolationException.class,()->awards.accept(principal("a@award.test"),rfq,quoteB));unchanged();assertEquals("SUBMITTED",jdbc.queryForObject("SELECT status FROM quotations WHERE id=?",String.class,quoteB));assertEquals(0,jdbc.queryForObject("SELECT count(*) FROM purchase_order_items",Integer.class));}
        finally {jdbc.execute("DROP TRIGGER s5_fail_item ON purchase_order_items");jdbc.execute("DROP FUNCTION s5_fail_order_item()");}
    }
    @Test void concurrentDifferentWinnersCreateExactlyOneOrder() throws Exception {
        AccountPrincipal actor=principal("a@award.test");ExecutorService pool=Executors.newFixedThreadPool(2);CountDownLatch start=new CountDownLatch(1);
        try {Future<Boolean>first=pool.submit(()->contend(actor,quoteB,start)),second=pool.submit(()->contend(actor,quoteC,start));start.countDown();assertNotEquals(first.get(10,TimeUnit.SECONDS),second.get(10,TimeUnit.SECONDS));assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM purchase_orders",Integer.class));assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM quotations WHERE status='ACCEPTED'",Integer.class));}
        finally {pool.shutdownNow();}
    }
    @Test void submittedOfferRemainsEligibleAfterSupplierGrantLoss() throws Exception {
        long supplierAccount=jdbc.queryForObject("SELECT id FROM accounts WHERE email='b@award.test'",Long.class);
        mvc.perform(put("/api/employees/"+supplierAccount+"/access").session(b).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"organizationAdmin\":true,\"buyerGrant\":false,\"supplierGrant\":false,\"active\":true}")).andExpect(status().isOk());
        mvc.perform(get(quotePath(invB)).session(b)).andExpect(status().isUnauthorized());
        accept(buyer,quoteB).andExpect(status().isOk());
        mvc.perform(get("/api/procurement/orders/"+orderId()).session(buyer)).andExpect(status().isOk());
        MockHttpSession revoked=login("b@award.test");mvc.perform(get("/api/procurement/orders/"+orderId()).session(revoked)).andExpect(status().isForbidden());
    }
    @Test void comparisonUsesOneInstantForExpiryAndEligibility() {
        AccountPrincipal actor=principal("a@award.test");
        when(clock.instant()).thenReturn(FUTURE.minusSeconds(1),FUTURE);
        clearInvocations(clock);
        var comparison=awards.comparison(actor,rfq);
        assertEquals(2,comparison.groups().get(0).quotations().size());
        for(var entry:comparison.groups().get(0).quotations()){assertTrue(entry.eligible());assertFalse(entry.quotation().expired());}
        verify(clock,times(1)).instant();
    }
    @Test void awardClosesAllQuotationWritesAndSerializesInFlightSubmission() throws Exception {
        quote(invD,"MAD","0.50",false);
        long emptyOrganization=org("Supplier E");account(emptyOrganization,"e@award.test",false,true);long emptyInvitation=invite(emptyOrganization);
        MockHttpSession empty=login("e@award.test");
        var beforeHeader=jdbc.queryForMap("SELECT * FROM quotations WHERE invitation_id=?",invD);
        var beforeLines=jdbc.queryForList("SELECT * FROM quotation_items WHERE quotation_id=(SELECT id FROM quotations WHERE invitation_id=?) ORDER BY id",invD);
        AccountPrincipal actor=principal("a@award.test"),recipient=principal("d@award.test");ExecutorService pool=Executors.newFixedThreadPool(2);CountDownLatch awarded=new CountDownLatch(1),release=new CountDownLatch(1);
        try {
            Future<?>winner=pool.submit(()->new TransactionTemplate(transactions).executeWithoutResult(tx->{awards.accept(actor,rfq,quoteB);awarded.countDown();try{assertTrue(release.await(10,TimeUnit.SECONDS));}catch(InterruptedException e){throw new RuntimeException(e);}}));
            assertTrue(awarded.await(10,TimeUnit.SECONDS));Future<?>late=pool.submit(()->quotations.submit(recipient,invD));assertThrows(TimeoutException.class,()->late.get(250,TimeUnit.MILLISECONDS));release.countDown();winner.get(10,TimeUnit.SECONDS);ExecutionException denied=assertThrows(ExecutionException.class,()->late.get(10,TimeUnit.SECONDS));assertInstanceOf(ProcurementException.class,denied.getCause());assertEquals(409,((ProcurementException)denied.getCause()).getStatus().value());
            mvc.perform(get(quotePath(invD)).session(d)).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("DRAFT"));
            error(mvc.perform(post(quotePath(invD)).session(d).with(csrf())),409);
            error(mvc.perform(post(quotePath(emptyInvitation)).session(empty).with(csrf())),409);
            assertEquals(0,jdbc.queryForObject("SELECT count(*) FROM quotations WHERE invitation_id=?",Integer.class,emptyInvitation));
            error(mvc.perform(put(quotePath(invD)).session(d).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"currency\":\"MAD\",\"validUntil\":\""+FUTURE+"\",\"items\":[]}")),409);
            assertEquals("DRAFT",jdbc.queryForObject("SELECT status FROM quotations WHERE invitation_id=?",String.class,invD));
            assertEquals(beforeHeader,jdbc.queryForMap("SELECT * FROM quotations WHERE invitation_id=?",invD));
            assertEquals(beforeLines,jdbc.queryForList("SELECT * FROM quotation_items WHERE quotation_id=(SELECT id FROM quotations WHERE invitation_id=?) ORDER BY id",invD));
        }finally{release.countDown();pool.shutdownNow();}
    }
    private String shared(String json){int start=json.indexOf("\"allowedActions\":");assertTrue(start>=0);int end=json.indexOf(']',start);assertTrue(end>start);return json.substring(0,start)+"\"allowedActions\":[]"+json.substring(end+1);}
    private boolean contend(AccountPrincipal actor,long quote,CountDownLatch start)throws Exception{start.await();try{awards.accept(actor,rfq,quote);return true;}catch(ProcurementException conflict){assertEquals(409,conflict.getStatus().value());return false;}}
    private void unchanged(){assertEquals("SENT",jdbc.queryForObject("SELECT status FROM rfqs WHERE id=?",String.class,rfq));assertNull(jdbc.queryForObject("SELECT winning_quotation_id FROM rfqs WHERE id=?",Object.class,rfq));assertEquals(0,jdbc.queryForObject("SELECT count(*) FROM purchase_orders",Integer.class));}
    private AccountPrincipal principal(String email){return new TransactionTemplate(transactions).execute(tx->new AccountPrincipal(accounts.findByEmail(email).orElseThrow()));}
    private long org(String name){return jdbc.queryForObject("INSERT INTO organizations(display_name) VALUES (?) RETURNING id",Long.class,name);}
    private void account(long org,String email,boolean buyer,boolean supplier){jdbc.update("INSERT INTO accounts(organization_id,full_name,email,password_hash,organization_admin,buyer_grant,supplier_grant,email_verified) VALUES (?,'Operator',?,?,true,?,?,true)",org,email,hash,buyer,supplier);}
    private MockHttpSession login(String email)throws Exception{return(MockHttpSession)mvc.perform(post("/api/auth/login").with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"email\":\""+email+"\",\"password\":\"award passphrase 123\"}")).andExpect(status().isOk()).andReturn().getRequest().getSession(false);}
    private long invite(long org){return jdbc.queryForObject("INSERT INTO rfq_invitations(rfq_id,supplier_organization_id) VALUES (?,?) RETURNING id",Long.class,rfq,org);}
    private long quote(long invitation,String currency,String price,boolean submitted){long id=jdbc.queryForObject("INSERT INTO quotations(invitation_id,status,currency,valid_until,terms,submitted_at) VALUES (?,?,?,?,?,?) RETURNING id",Long.class,invitation,submitted?"SUBMITTED":"DRAFT",currency,java.sql.Timestamp.from(FUTURE),"Fixed terms",submitted?java.sql.Timestamp.from(NOW):null);jdbc.update("INSERT INTO quotation_items(quotation_id,rfq_item_id,unit_price) VALUES (?,?,?::numeric)",id,item,price);return id;}
    private String base(){return"/api/procurement/rfqs/"+rfq;}
    private String comparison(){return base()+"/comparison";}
    private String quotePath(long invitation){return"/api/procurement/invitations/"+invitation+"/quotation";}
    private ResultActions accept(MockHttpSession session,long quote)throws Exception{return mvc.perform(post(base()+"/quotations/"+quote+"/accept").session(session).with(csrf()));}
    private long orderId(){return jdbc.queryForObject("SELECT id FROM purchase_orders WHERE rfq_id=?",Long.class,rfq);}
    private void error(ResultActions result,int code)throws Exception{result.andExpect(status().is(code)).andExpect(jsonPath("$.code").isString()).andExpect(jsonPath("$.message").isString());}
}
