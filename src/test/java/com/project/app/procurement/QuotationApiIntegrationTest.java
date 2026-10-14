package com.project.app.procurement;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import com.project.app.identity.repository.AccountRepository;
import com.project.app.identity.security.AccountPrincipal;
import com.project.app.procurement.api.QuotationRequests.DraftInput;
import com.project.app.procurement.api.QuotationRequests.PriceInput;
import com.project.app.procurement.service.QuotationService;
import com.project.app.procurement.service.ProcurementException;
import com.project.app.support.Slice0DatabaseGuard;

/** BL-S4-R1, issues #16/#17: real HTTP/DB proofs for TC-S4-001..011. */
@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("slice0-test")
@ContextConfiguration(initializers = Slice0DatabaseGuard.class)
class QuotationApiIntegrationTest {
    private static final Instant NOW = Instant.parse("2026-10-12T10:00:00Z");
    private static final String FUTURE = "2026-10-20T10:00:00Z";
    private static String passwordHash;
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired PasswordEncoder passwords;
    @Autowired QuotationService service;
    @Autowired AccountRepository accounts;
    @Autowired PlatformTransactionManager transactionManager;
    @MockitoBean Clock clock;
    private MockHttpSession buyer, supplier, competitor, outsider, buyerOnly;
    private long buyerOrg, supplierOrg, supplierAccount, rfq, invitation, otherInvitation, firstItem, secondItem;

    @BeforeEach void fixtures() throws Exception {
        reset(clock); when(clock.instant()).thenReturn(NOW);
        jdbc.update("DELETE FROM order_events");jdbc.update("DELETE FROM purchase_order_items"); jdbc.update("DELETE FROM purchase_orders"); jdbc.update("UPDATE rfqs SET status='SENT',winning_quotation_id=NULL WHERE status='AWARDED'");
        for (String table : List.of("quotation_items","quotations","rfq_invitations","rfq_items","rfqs","supplier_contacts","marketplace_offerings","marketplace_profiles","identity_tokens","accounts","organizations")) jdbc.update("DELETE FROM " + table);
        if (passwordHash == null) passwordHash = passwords.encode("quotation passphrase 123");
        buyerOrg=organization("Buyer"); supplierOrg=organization("Supplier B");
        long cOrg=organization("Supplier C"), dOrg=organization("Uninvited D");
        account(buyerOrg,"buyer@quote.test",true,false);
        supplierAccount=account(supplierOrg,"supplier@quote.test",false,true);
        account(cOrg,"competitor@quote.test",false,true); account(dOrg,"outsider@quote.test",false,true);
        account(supplierOrg,"buyeronly@quote.test",true,false);
        buyer=login("buyer@quote.test"); supplier=login("supplier@quote.test"); competitor=login("competitor@quote.test"); outsider=login("outsider@quote.test"); buyerOnly=login("buyeronly@quote.test");
        rfq=jdbc.queryForObject("INSERT INTO rfqs(organization_id,title,status) VALUES (?,'Quote fixtures','SENT') RETURNING id",Long.class,buyerOrg);
        firstItem=jdbc.queryForObject("INSERT INTO rfq_items(rfq_id,description,quantity,unit) VALUES (?,'Bolts',3,'box') RETURNING id",Long.class,rfq);
        secondItem=jdbc.queryForObject("INSERT INTO rfq_items(rfq_id,description,quantity,unit) VALUES (?,'Nuts',7,'pack') RETURNING id",Long.class,rfq);
        invitation=jdbc.queryForObject("INSERT INTO rfq_invitations(rfq_id,supplier_organization_id) VALUES (?,?) RETURNING id",Long.class,rfq,supplierOrg);
        otherInvitation=jdbc.queryForObject("INSERT INTO rfq_invitations(rfq_id,supplier_organization_id) VALUES (?,?) RETURNING id",Long.class,rfq,cOrg);
    }
    @Test void emptyDraftOrganizationSharingAndParticipantBoundaries() throws Exception {
        create(supplier,invitation).andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.currency").isEmpty()).andExpect(jsonPath("$.validUntil").isEmpty())
                .andExpect(jsonPath("$.submittedAt").isEmpty()).andExpect(jsonPath("$.terms").isEmpty())
                .andExpect(jsonPath("$.subtotal").isEmpty()).andExpect(jsonPath("$.expired").value(false))
                .andExpect(jsonPath("$.items").isEmpty());
        error(mvc.perform(post(path(invitation)).session(supplier).with(csrf())),409);
        account(supplierOrg,"colleague@quote.test",false,true);
        MockHttpSession colleague=login("colleague@quote.test");
        mvc.perform(get(path(invitation)).session(colleague)).andExpect(status().isOk());
        save(colleague,invitation,payload("1","2")).andExpect(status().isOk());
        mvc.perform(get(path(invitation)).session(supplier)).andExpect(jsonPath("$.subtotal").value("17.00"));
        error(mvc.perform(get(path(invitation)).session(competitor)),404);
        error(mvc.perform(post(path(invitation)).session(outsider).with(csrf())),404);
        mvc.perform(get(path(invitation)).session(buyerOnly)).andExpect(status().isForbidden());
        mvc.perform(get(path(invitation))).andExpect(status().isUnauthorized());
        mvc.perform(get(buyerPath()).session(buyer)).andExpect(jsonPath("$").isEmpty());
        error(mvc.perform(get(buyerPath()+"/"+quotationId()).session(buyer)),404);
        assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM quotations",Integer.class));
    }
    @Test void twoSuppliersSubmitExactSafeBuyerResponsesWithoutCompetitorAccess() throws Exception {
        create(supplier,invitation); create(competitor,otherInvitation);
        save(supplier,invitation,payload("0.10","0.20")).andExpect(status().isOk())
                .andExpect(jsonPath("$.subtotal").value("1.70")).andExpect(jsonPath("$.items[0].unitPrice").value("0.10"))
                .andExpect(jsonPath("$.items[0].lineTotal").value("0.30")).andExpect(jsonPath("$.items[1].lineTotal").value("1.40"));
        save(competitor,otherInvitation,payload("1.23","2.00")).andExpect(status().isOk());
        mvc.perform(get(buyerPath()).session(buyer)).andExpect(jsonPath("$").isEmpty());
        submit(supplier,invitation).andExpect(status().isOk()).andExpect(jsonPath("$.submittedAt").value(NOW.toString()));
        submit(competitor,otherInvitation).andExpect(status().isOk());
        var result=mvc.perform(get(buyerPath()).session(buyer)).andExpect(status().isOk()).andExpect(jsonPath("$",org.hamcrest.Matchers.hasSize(2)))
                .andExpect(jsonPath("$[0].subtotal").value("1.70")).andExpect(jsonPath("$[1].subtotal").value("17.69")).andReturn();
        String json=result.getResponse().getContentAsString();
        assertFalse(json.contains("@quote.test")); assertFalse(json.contains("password")); assertFalse(json.contains("contact"));
        error(mvc.perform(get(buyerPath()).session(buyerOnly)),404);
        mvc.perform(get(buyerPath()).session(supplier)).andExpect(status().isForbidden());
        error(mvc.perform(get(path(otherInvitation)).session(supplier)),404);
        assertEquals(0,jdbc.queryForObject("SELECT count(*) FROM purchase_orders",Integer.class));
    }
    @Test void malformedAndInvalidReplacementsAreAtomicAndUseExistingErrors() throws Exception {
        create(supplier,invitation); save(supplier,invitation,payload("1.2300","2.00")).andExpect(status().isOk()).andExpect(jsonPath("$.items[0].unitPrice").value("1.23"));
        for(String price:List.of("0","-1","1000000000","0.001","NaN","1e2","")) assertRejected(payload(price,"2"));
        assertRejected(payload("1","2").replace("\"unitPrice\":\"1\"","\"unitPrice\":1"));
        assertRejected(payload("1","2").replace("\"MAD\"","\"GBP\""));
        assertRejected(payload("1","2").replace(FUTURE,NOW.toString()));
        assertRejected(payload("1","2").replace("\"terms\":null","\"terms\":\""+"x".repeat(2001)+"\""));
        assertRejected(payload("1","2").replace("\"terms\":null","\"unknown\":true"));
        assertRejected("{\"currency\":\"MAD\",\"validUntil\":\""+FUTURE+"\",\"items\":null}");
        assertRejected("{\"currency\":\"MAD\",\"validUntil\":\""+FUTURE+"\"}");
        assertRejected(payload("1","2").replace("\"rfqItemId\":"+secondItem,"\"rfqItemId\":"+firstItem));
        assertRejected(payload("1","2").replace("\"rfqItemId\":"+secondItem,"\"rfqItemId\":99999999"));
        long foreignRfq=jdbc.queryForObject("INSERT INTO rfqs(organization_id,title,status) VALUES (?,'Other RFQ','SENT') RETURNING id",Long.class,buyerOrg);
        long foreignItem=jdbc.queryForObject("INSERT INTO rfq_items(rfq_id,description,quantity,unit) VALUES (?,'Other source',1,'box') RETURNING id",Long.class,foreignRfq);
        assertRejected(payload("1","2").replace("\"rfqItemId\":"+secondItem,"\"rfqItemId\":"+foreignItem));
        error(mvc.perform(put(path(invitation)).session(supplier).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{")),400);
        save(supplier,invitation,payload("1","2").replace("\"terms\":null","\"terms\":\""+"😀".repeat(2000)+"\""))
                .andExpect(status().isOk());
    }
    @Test void incompleteSaveAndSubmissionRevalidation() throws Exception {
        create(supplier,invitation);
        error(submit(supplier,invitation),409);
        save(supplier,invitation,"{\"currency\":\"EUR\",\"validUntil\":\""+FUTURE+"\",\"terms\":\"  \",\"items\":[]}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.subtotal").value("0.00")).andExpect(jsonPath("$.terms").isEmpty());
        error(submit(supplier,invitation),409);
        save(supplier,invitation,payload("1","2")).andExpect(status().isOk());
        when(clock.instant()).thenReturn(Instant.parse(FUTURE));
        error(submit(supplier,invitation),409);
        assertEquals("DRAFT",jdbc.queryForObject("SELECT status FROM quotations",String.class));
        assertNull(jdbc.queryForObject("SELECT submitted_at FROM quotations",Object.class));
    }
    @Test void submittedTermsFreezeExpireAndSurviveContactRemovalAndGrantRevocation() throws Exception {
        create(supplier,invitation); save(supplier,invitation,payload("1","2")).andExpect(status().isOk());
        submit(supplier,invitation).andExpect(status().isOk());
        long profile=jdbc.queryForObject("INSERT INTO marketplace_profiles(organization_id,description) VALUES (?,'Original') RETURNING id",Long.class,supplierOrg);
        long offering=jdbc.queryForObject("INSERT INTO marketplace_offerings(profile_id,title,description,category) VALUES (?,'Original goods','Original catalog description','OTHER') RETURNING id",Long.class,profile);
        jdbc.update("UPDATE marketplace_offerings SET title='Changed',description='Changed terms' WHERE id=?",offering);
        mvc.perform(get(path(invitation)).session(supplier)).andExpect(jsonPath("$.items[0].description").value("Bolts")).andExpect(jsonPath("$.subtotal").value("17.00"));
        error(submit(supplier,invitation),409); error(save(supplier,invitation,payload("3","4")),409);
        jdbc.update("INSERT INTO supplier_contacts(buyer_organization_id,supplier_organization_id,note) VALUES (?,?,'secret')",buyerOrg,supplierOrg);
        jdbc.update("DELETE FROM supplier_contacts WHERE buyer_organization_id=?",buyerOrg);
        when(clock.instant()).thenReturn(Instant.parse(FUTURE));
        mvc.perform(get(buyerPath()+"/"+quotationId()).session(buyer)).andExpect(status().isOk())
                .andExpect(jsonPath("$.expired").value(true)).andExpect(jsonPath("$.status").value("SUBMITTED"))
                .andExpect(jsonPath("$.subtotal").value("17.00"));
        mvc.perform(put("/api/employees/"+supplierAccount+"/access").session(supplier).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"organizationAdmin\":true,\"buyerGrant\":false,\"supplierGrant\":false,\"active\":true}")).andExpect(status().isOk());
        mvc.perform(get(path(invitation)).session(supplier)).andExpect(status().isUnauthorized());
        mvc.perform(get(buyerPath()).session(buyer)).andExpect(jsonPath("$",org.hamcrest.Matchers.hasSize(1)));
    }
    @Test void databaseConstraintsAndMaximumDecimalProduct() throws Exception {
        create(supplier,invitation);
        assertThrows(DataIntegrityViolationException.class,()->jdbc.update("INSERT INTO quotations(invitation_id) VALUES (?)",invitation));
        jdbc.update("UPDATE rfq_items SET quantity=1000000 WHERE id=?",firstItem);
        save(supplier,invitation,payload("999999999.99","0.01")).andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].lineTotal").value("999999999990000.00"))
                .andExpect(jsonPath("$.subtotal").value("999999999990000.07"));
        long quote=quotationId();
        assertThrows(DataIntegrityViolationException.class,()->jdbc.update("INSERT INTO quotation_items(quotation_id,rfq_item_id,unit_price) VALUES (?,?,1)",quote,firstItem));
        jdbc.update("DELETE FROM quotation_items WHERE quotation_id=?",quote);
        assertThrows(DataIntegrityViolationException.class,()->jdbc.update("INSERT INTO quotation_items(quotation_id,rfq_item_id,unit_price) VALUES (?,?,0)",quote,firstItem));
    }
    @Test void concurrentCreateMakesOneQuotation() throws Exception {
        AccountPrincipal principal=principal();
        ExecutorService pool=Executors.newFixedThreadPool(2); CountDownLatch start=new CountDownLatch(1);
        Callable<Boolean> action=()->{start.await();try{service.create(principal,invitation);return true;}catch(ProcurementException conflict){assertEquals(409,conflict.getStatus().value());return false;}};
        try { Future<Boolean>a=pool.submit(action),b=pool.submit(action);start.countDown();assertNotEquals(a.get(10,TimeUnit.SECONDS),b.get(10,TimeUnit.SECONDS));assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM quotations",Integer.class)); }
        finally { pool.shutdownNow(); }
    }
    @Test void concurrentSubmitBlocksReplacementAndSecondSubmission() throws Exception {
        create(supplier,invitation);save(supplier,invitation,payload("1","2")).andExpect(status().isOk());
        AccountPrincipal principal=principal();ExecutorService pool=Executors.newFixedThreadPool(3);
        CountDownLatch submitted=new CountDownLatch(1),release=new CountDownLatch(1);
        try {
            Future<?>first=pool.submit(()->new TransactionTemplate(transactionManager).executeWithoutResult(tx->{service.submit(principal,invitation);submitted.countDown();try{assertTrue(release.await(10,TimeUnit.SECONDS));}catch(InterruptedException e){throw new RuntimeException(e);}}));
            assertTrue(submitted.await(10,TimeUnit.SECONDS));
            Future<?>edit=pool.submit(()->service.replace(principal,invitation,new DraftInput("USD",Instant.parse(FUTURE),"changed",List.of(new PriceInput(firstItem,"99"),new PriceInput(secondItem,"99")))));
            Future<?>second=pool.submit(()->service.submit(principal,invitation));
            assertThrows(TimeoutException.class,()->edit.get(250,TimeUnit.MILLISECONDS));
            release.countDown();first.get(10,TimeUnit.SECONDS);
            for(Future<?> rejected:List.of(edit,second)) {ExecutionException failure=assertThrows(ExecutionException.class,()->rejected.get(10,TimeUnit.SECONDS));assertInstanceOf(ProcurementException.class,failure.getCause());assertEquals(409,((ProcurementException)failure.getCause()).getStatus().value());}
            mvc.perform(get(path(invitation)).session(supplier)).andExpect(jsonPath("$.currency").value("MAD")).andExpect(jsonPath("$.subtotal").value("17.00"));
        } finally {release.countDown();pool.shutdownNow();}
    }
    private AccountPrincipal principal(){return new TransactionTemplate(transactionManager).execute(tx->new AccountPrincipal(accounts.findByEmail("supplier@quote.test").orElseThrow()));}
    private long organization(String name){return jdbc.queryForObject("INSERT INTO organizations(display_name) VALUES (?) RETURNING id",Long.class,name);}
    private long account(long org,String email,boolean b,boolean s){return jdbc.queryForObject("INSERT INTO accounts(organization_id,full_name,email,password_hash,organization_admin,buyer_grant,supplier_grant,email_verified) VALUES (?,'Operator',?,?,true,?,?,true) RETURNING id",Long.class,org,email,passwordHash,b,s);}
    private MockHttpSession login(String email)throws Exception{return(MockHttpSession)mvc.perform(post("/api/auth/login").with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"email\":\""+email+"\",\"password\":\"quotation passphrase 123\"}")).andExpect(status().isOk()).andReturn().getRequest().getSession(false);}
    private String path(long id){return "/api/procurement/invitations/"+id+"/quotation";}
    private String buyerPath(){return "/api/procurement/rfqs/"+rfq+"/quotations";}
    private ResultActions create(MockHttpSession session,long id)throws Exception{return mvc.perform(post(path(id)).session(session).with(csrf())).andExpect(status().isCreated());}
    private ResultActions save(MockHttpSession session,long id,String json)throws Exception{return mvc.perform(put(path(id)).session(session).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(json));}
    private ResultActions submit(MockHttpSession session,long id)throws Exception{return mvc.perform(post(path(id)+"/submit").session(session).with(csrf()));}
    private long quotationId(){return jdbc.queryForObject("SELECT id FROM quotations WHERE invitation_id=?",Long.class,invitation);}
    private String payload(String p1,String p2){return "{\"currency\":\"MAD\",\"validUntil\":\""+FUTURE+"\",\"terms\":null,\"items\":[{\"rfqItemId\":"+firstItem+",\"unitPrice\":\""+p1+"\"},{\"rfqItemId\":"+secondItem+",\"unitPrice\":\""+p2+"\"}]}";}
    private void error(ResultActions action,int status)throws Exception{action.andExpect(status().is(status)).andExpect(jsonPath("$.code").isString()).andExpect(jsonPath("$.message").isString());}
    private void assertRejected(String json)throws Exception{
        var header=jdbc.queryForMap("SELECT * FROM quotations WHERE invitation_id=?",invitation);
        var before=jdbc.queryForList("SELECT * FROM quotation_items ORDER BY id");
        error(save(supplier,invitation,json),400);
        assertEquals(header,jdbc.queryForMap("SELECT * FROM quotations WHERE invitation_id=?",invitation));
        assertEquals(before,jdbc.queryForList("SELECT * FROM quotation_items ORDER BY id"));
    }
}
