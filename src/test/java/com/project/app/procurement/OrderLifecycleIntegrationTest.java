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
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import com.project.app.procurement.model.PurchaseOrder;
import org.springframework.test.web.servlet.*;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import com.project.app.identity.repository.AccountRepository;
import com.project.app.identity.security.AccountPrincipal;
import com.project.app.procurement.api.OrderViews.OrderView;
import com.project.app.procurement.service.*;
import com.project.app.procurement.service.OrderService.Action;
import com.project.app.support.Slice0DatabaseGuard;

/** BL-S6-R1 / #22 / TC-S6-001..006. Real PostgreSQL, guarded isolated target only. */
@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("slice0-test")
@ContextConfiguration(initializers=Slice0DatabaseGuard.class)
class OrderLifecycleIntegrationTest {
    @Autowired JdbcTemplate jdbc;@Autowired MockMvc mvc;@Autowired PasswordEncoder encoder;
    @MockitoSpyBean OrderService orders;@Autowired AwardService awards;@Autowired AccountRepository accounts;
    @Autowired PlatformTransactionManager transactions;@MockitoBean Clock clock;
    private static String hash;private static final Instant NOW=Instant.parse("2026-10-12T12:00:00Z");
    private AccountPrincipal buyer,supplier,foreign,mixed;private MockHttpSession bs,ss,fs,ms;private long order,rfq,quote;
    @BeforeEach void fixture() throws Exception {
        reset(clock);when(clock.instant()).thenReturn(NOW);
        reset(orders);
        jdbc.execute("DROP TRIGGER IF EXISTS s6_fail_event ON order_events");jdbc.execute("DROP FUNCTION IF EXISTS s6_fail_event()");
        jdbc.update("DELETE FROM order_events");jdbc.update("DELETE FROM purchase_order_items");jdbc.update("DELETE FROM purchase_orders");jdbc.update("UPDATE rfqs SET status='SENT',winning_quotation_id=NULL WHERE status='AWARDED'");
        for(String table:List.of("quotation_items","quotations","rfq_invitations","rfq_items","rfqs","supplier_contacts","marketplace_offerings","marketplace_profiles","identity_tokens","accounts","organizations"))jdbc.update("DELETE FROM "+table);
        if(hash==null)hash=encoder.encode("lifecycle passphrase 123");
        long a=org("Buyer snapshot"),b=org("Supplier snapshot"),c=org("Unrelated");
        account(a,"a@life.test",true,true);account(b,"b@life.test",true,true);account(c,"c@life.test",true,true);account(b,"mixed@life.test",true,false);
        buyer=principal("a@life.test");supplier=principal("b@life.test");foreign=principal("c@life.test");mixed=principal("mixed@life.test");
        bs=login("a@life.test");ss=login("b@life.test");fs=login("c@life.test");ms=login("mixed@life.test");
        rfq=jdbc.queryForObject("INSERT INTO rfqs(organization_id,title,status) VALUES (?,'Frozen order','SENT') RETURNING id",Long.class,a);
        long item=jdbc.queryForObject("INSERT INTO rfq_items(rfq_id,description,quantity,unit) VALUES (?,'Bolts',3,'box') RETURNING id",Long.class,rfq);
        long invite=jdbc.queryForObject("INSERT INTO rfq_invitations(rfq_id,supplier_organization_id) VALUES (?,?) RETURNING id",Long.class,rfq,b);
        quote=jdbc.queryForObject("INSERT INTO quotations(invitation_id,status,currency,valid_until,terms,submitted_at) VALUES (?,'SUBMITTED','MAD',?,'Original terms',?) RETURNING id",Long.class,invite,java.sql.Timestamp.from(NOW.plusSeconds(3600)),java.sql.Timestamp.from(NOW));
        jdbc.update("INSERT INTO quotation_items(quotation_id,rfq_item_id,unit_price) VALUES (?,?,1.20)",quote,item);
        order=awards.accept(buyer,rfq,quote).id();
    }
    @Test void legalPathHistorySnapshotsAndLateReplay() throws Exception {
        var header=jdbc.queryForMap("SELECT * FROM purchase_orders WHERE id=?",order);header.remove("status");
        var items=jdbc.queryForList("SELECT * FROM purchase_order_items WHERE order_id=?",order);
        assertEquals(List.of(),orders.get(buyer,order).allowedActions());assertEquals(List.of("confirm"),orders.get(supplier,order).allowedActions());
        orders.transition(supplier,order,Action.CONFIRM);
        assertEquals(List.of("start-processing"),orders.get(supplier,order).allowedActions());assertTrue(orders.get(buyer,order).allowedActions().isEmpty());assertEquals(2,orders.get(buyer,order).history().size());
        orders.transition(supplier,order,Action.START_PROCESSING);
        assertEquals(List.of("dispatch"),orders.get(supplier,order).allowedActions());assertTrue(orders.get(buyer,order).allowedActions().isEmpty());assertEquals(3,orders.get(buyer,order).history().size());
        orders.transition(supplier,order,Action.DISPATCH);
        assertEquals(List.of("receive"),orders.get(buyer,order).allowedActions());assertTrue(orders.get(supplier,order).allowedActions().isEmpty());
        OrderView received=orders.transition(buyer,order,Action.RECEIVE);
        assertEquals("RECEIVED",received.status());assertEquals(List.of("CREATED","CONFIRMED","PROCESSING","DISPATCHED","RECEIVED"),received.history().stream().map(h->h.status()).toList());
        assertEquals(received,orders.get(supplier,order));
        when(clock.instant()).thenReturn(NOW.plusSeconds(7200));
        for(Action action:Action.values())assertEquals(received,orders.transition(action==Action.RECEIVE?buyer:supplier,order,action));
        assertEquals(received,awards.accept(buyer,rfq,quote));
        var after=jdbc.queryForMap("SELECT * FROM purchase_orders WHERE id=?",order);after.remove("status");assertEquals(header,after);assertEquals(items,jdbc.queryForList("SELECT * FROM purchase_order_items WHERE order_id=?",order));
        assertEquals("AWARDED",jdbc.queryForObject("SELECT status FROM rfqs WHERE id=?",String.class,rfq));assertEquals("ACCEPTED",jdbc.queryForObject("SELECT status FROM quotations WHERE id=?",String.class,quote));
        assertEquals(4,count());mvc.perform(get(path()).session(bs)).andExpect(jsonPath("$.history[1].actingAccountId").doesNotExist()).andExpect(jsonPath("$.allowedActions").isEmpty());
        assertNotNull(jdbc.queryForObject("SELECT acting_account_id FROM order_events WHERE order_id=? AND target_status='RECEIVED'",Long.class,order));
    }
    @Test void illegalAndWrongSideHttpCommandsPreserveEverything() throws Exception {
        var before=jdbc.queryForMap("SELECT * FROM purchase_orders WHERE id=?",order);
        for(String action:List.of("start-processing","dispatch"))command(ss,action).andExpect(status().isConflict());command(bs,"receive").andExpect(status().isConflict());
        for(String action:List.of("confirm","start-processing","dispatch"))command(bs,action).andExpect(status().isForbidden());command(ss,"receive").andExpect(status().isForbidden());
        command(fs,"confirm").andExpect(status().isNotFound());command(ms,"confirm").andExpect(status().isForbidden());
        mvc.perform(post(path()+"/confirm").session(ss)).andExpect(status().isForbidden());mvc.perform(post(path()+"/confirm").with(csrf())).andExpect(status().isUnauthorized());
        mvc.perform(get(path()).session(ms)).andExpect(status().isNotFound());
        mvc.perform(delete(path()).session(bs).with(csrf())).andExpect(status().isMethodNotAllowed());
        assertEquals(before,jdbc.queryForMap("SELECT * FROM purchase_orders WHERE id=?",order));assertEquals(0,count());
        command(ss,"confirm").andExpect(status().isOk());command(bs,"confirm").andExpect(status().isForbidden());assertEquals(1,count());
    }
    @Test void actualEventFailureRollsBackStateAndUniqueConstraintsHold() {
        jdbc.execute("CREATE FUNCTION s6_fail_event() RETURNS trigger LANGUAGE plpgsql AS 'BEGIN RAISE EXCEPTION ''forced event failure'' USING ERRCODE = ''23514''; END'");
        jdbc.execute("CREATE TRIGGER s6_fail_event BEFORE INSERT ON order_events FOR EACH ROW EXECUTE FUNCTION s6_fail_event()");
        try{assertThrows(DataIntegrityViolationException.class,()->orders.transition(supplier,order,Action.CONFIRM));assertEquals("CREATED",orders.get(buyer,order).status());assertEquals(0,count());}
        finally{jdbc.execute("DROP TRIGGER s6_fail_event ON order_events");jdbc.execute("DROP FUNCTION s6_fail_event()");}
        orders.transition(supplier,order,Action.CONFIRM);
        long actor=supplier.getAccountId();
        assertThrows(DataIntegrityViolationException.class,()->jdbc.update("INSERT INTO order_events(order_id,target_status,occurred_at,acting_account_id) VALUES (?,'CONFIRMED',now(),?)",order,actor));
        assertThrows(DataIntegrityViolationException.class,()->jdbc.update("INSERT INTO order_events(order_id,target_status,occurred_at,acting_account_id) VALUES (?,'BOGUS',now(),?)",order,actor));
        assertThrows(DataIntegrityViolationException.class,()->jdbc.update("INSERT INTO order_events(order_id,target_status,occurred_at,acting_account_id) VALUES (?,'RECEIVED',now(),-1)",order));
        assertThrows(DataIntegrityViolationException.class,()->jdbc.update("INSERT INTO order_events(order_id,target_status,occurred_at,acting_account_id) VALUES (-1,'RECEIVED',now(),?)",actor));
        assertThrows(DataIntegrityViolationException.class,()->jdbc.update("UPDATE purchase_orders SET status='BOGUS' WHERE id=?",order));assertEquals(1,count());
    }
    @Test void simultaneousDuplicatesCommitExactlyOneEvent() throws Exception {
        var pool=Executors.newFixedThreadPool(2);var start=new CountDownLatch(1);
        try{Callable<OrderView>command=()->{start.await();return orders.transition(supplier,order,Action.CONFIRM);};var a=pool.submit(command);var b=pool.submit(command);start.countDown();assertEquals(a.get(10,TimeUnit.SECONDS),b.get(10,TimeUnit.SECONDS));assertEquals(1,count());}
        finally{pool.shutdownNow();}
    }
    @Test void grantRevocationBlocksSessionAndNoGrantActorPreservesHistory() throws Exception {
        orders.transition(supplier,order,Action.CONFIRM);var header=jdbc.queryForMap("SELECT * FROM purchase_orders WHERE id=?",order);var history=jdbc.queryForList("SELECT * FROM order_events WHERE order_id=? ORDER BY id",order);var lines=jdbc.queryForList("SELECT * FROM purchase_order_items WHERE order_id=?",order);
        mvc.perform(put("/api/employees/"+supplier.getAccountId()+"/access").session(ss).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"organizationAdmin\":true,\"buyerGrant\":false,\"supplierGrant\":false,\"active\":true}")).andExpect(status().isOk());
        mvc.perform(get(path()).session(ss)).andExpect(status().isUnauthorized());var none=login("b@life.test");mvc.perform(get(path()).session(none)).andExpect(status().isForbidden());command(none,"start-processing").andExpect(status().isForbidden());
        assertEquals(header,jdbc.queryForMap("SELECT * FROM purchase_orders WHERE id=?",order));assertEquals(history,jdbc.queryForList("SELECT * FROM order_events WHERE order_id=? ORDER BY id",order));assertEquals(lines,jdbc.queryForList("SELECT * FROM purchase_order_items WHERE order_id=?",order));assertEquals(2,orders.get(buyer,order).history().size());
    }
    @Test void readProjectionKeepsStatusAndHistoryFromOneSnapshot() throws Exception {
        var loaded=new CountDownLatch(1);var release=new CountDownLatch(1);var once=new java.util.concurrent.atomic.AtomicBoolean(true);var pool=Executors.newSingleThreadExecutor();
        doAnswer(call->{if(once.compareAndSet(true,false)){loaded.countDown();assertTrue(release.await(10,TimeUnit.SECONDS));}return call.callRealMethod();}).when(orders).view(any(PurchaseOrder.class),eq(buyer));
        try{var read=pool.submit(()->orders.get(buyer,order));assertTrue(loaded.await(10,TimeUnit.SECONDS));orders.transition(supplier,order,Action.CONFIRM);release.countDown();var old=read.get(10,TimeUnit.SECONDS);assertEquals("CREATED",old.status());assertEquals(List.of("CREATED"),old.history().stream().map(h->h.status()).toList());var current=orders.get(buyer,order);assertEquals("CONFIRMED",current.status());assertEquals(2,current.history().size());}
        finally{release.countDown();pool.shutdownNow();reset(orders);}
    }
    @Test void waitingNextActionReadsOnlyLockedCommittedState() throws Exception {
        var pool=Executors.newFixedThreadPool(2);var changed=new CountDownLatch(1);var release=new CountDownLatch(1);
        try{var first=pool.submit(()->new TransactionTemplate(transactions).executeWithoutResult(tx->{orders.transition(supplier,order,Action.CONFIRM);changed.countDown();try{assertTrue(release.await(10,TimeUnit.SECONDS));}catch(InterruptedException e){throw new RuntimeException(e);}}));
            assertTrue(changed.await(10,TimeUnit.SECONDS));var next=pool.submit(()->orders.transition(supplier,order,Action.START_PROCESSING));assertThrows(TimeoutException.class,()->next.get(250,TimeUnit.MILLISECONDS));release.countDown();first.get(10,TimeUnit.SECONDS);assertEquals("PROCESSING",next.get(10,TimeUnit.SECONDS).status());assertEquals(2,count());}
        finally{release.countDown();pool.shutdownNow();}
    }
    @Test void acceptanceReplayWaitsForFulfillmentAndReturnsCommittedHistory() throws Exception {
        var pool=Executors.newFixedThreadPool(2);var changed=new CountDownLatch(1);var release=new CountDownLatch(1);
        try{var first=pool.submit(()->new TransactionTemplate(transactions).executeWithoutResult(tx->{orders.transition(supplier,order,Action.CONFIRM);changed.countDown();try{assertTrue(release.await(10,TimeUnit.SECONDS));}catch(InterruptedException e){throw new RuntimeException(e);}}));
            assertTrue(changed.await(10,TimeUnit.SECONDS));var replay=pool.submit(()->awards.accept(buyer,rfq,quote));assertThrows(TimeoutException.class,()->replay.get(250,TimeUnit.MILLISECONDS));release.countDown();first.get(10,TimeUnit.SECONDS);var result=replay.get(10,TimeUnit.SECONDS);assertEquals("CONFIRMED",result.status());assertEquals(List.of("CREATED","CONFIRMED"),result.history().stream().map(h->h.status()).toList());assertEquals(1,count());}
        finally{release.countDown();pool.shutdownNow();}
    }
    private int count(){return jdbc.queryForObject("SELECT count(*) FROM order_events WHERE order_id=?",Integer.class,order);}
    private long org(String name){return jdbc.queryForObject("INSERT INTO organizations(display_name) VALUES (?) RETURNING id",Long.class,name);}
    private void account(long org,String email,boolean buyer,boolean supplier){jdbc.update("INSERT INTO accounts(organization_id,full_name,email,password_hash,organization_admin,buyer_grant,supplier_grant,email_verified) VALUES (?,'Operator',?,?,true,?,?,true)",org,email,hash,buyer,supplier);}
    private AccountPrincipal principal(String email){return new TransactionTemplate(transactions).execute(tx->new AccountPrincipal(accounts.findByEmail(email).orElseThrow()));}
    private MockHttpSession login(String email)throws Exception{return(MockHttpSession)mvc.perform(post("/api/auth/login").with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"email\":\""+email+"\",\"password\":\"lifecycle passphrase 123\"}")).andExpect(status().isOk()).andReturn().getRequest().getSession(false);}
    private String path(){return"/api/procurement/orders/"+order;}
    private ResultActions command(MockHttpSession session,String action)throws Exception{return mvc.perform(post(path()+"/"+action).session(session).with(csrf()));}
}
