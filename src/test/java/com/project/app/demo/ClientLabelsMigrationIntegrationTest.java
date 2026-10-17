package com.project.app.demo;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import com.project.app.support.Slice0DatabaseGuard;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.MediaType;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("slice0-test")
@ContextConfiguration(initializers=Slice0DatabaseGuard.class)
@EnabledIfEnvironmentVariable(named="PROCUREFLOW_FDP_TESTS",matches="true")
class ClientLabelsMigrationIntegrationTest {
    @Autowired JdbcTemplate jdbc;
    @Autowired PasswordEncoder encoder;
    @Autowired PlatformTransactionManager transactions;
    @Autowired MockMvc mvc;
    @MockitoBean JavaMailSender mail;
    static final String PASSWORD="Distinct-Test-Password-2026!";
    static final List<String> OLD_EMAILS=List.of("demo.buyer@procureflow.invalid","demo.buyer.employee@procureflow.invalid",
        "demo.atlas@procureflow.invalid","demo.atlas.employee@procureflow.invalid","demo.meridian@procureflow.invalid","demo.meridian.employee@procureflow.invalid");
    void seed() {
        var env=new MockEnvironment().withProperty("PROCUREFLOW_PRODUCTION_DEMO_PASSWORD",PASSWORD)
            .withProperty("PROCUREFLOW_PRODUCTION_DEMO_DATABASE",jdbc.queryForObject("select current_database()",String.class));
        env.setActiveProfiles("deploy");
        new ProductionDemoSeeder(jdbc,encoder,env,transactions).run(new DefaultApplicationArguments());
    }
    String migration() { try { return new ClassPathResource("db/migration/V11__realistic_sample_labels.sql").getContentAsString(StandardCharsets.UTF_8); }
        catch(java.io.IOException e){throw new IllegalStateException(e);} }
    void rollback(Runnable action) {new TransactionTemplate(transactions).executeWithoutResult(s->{try{action.run();}finally{s.setRollbackOnly();}});}
    void legacy() {
        String[] oldNames={"Demo Buyer Admin","Demo Buyer Employee","Demo Atlas Admin","Demo Atlas Employee","Demo Meridian Admin","Demo Meridian Employee"};
        String[] oldOrgs={"Demo Buyer Workshop","Demo Atlas Supply","Demo Meridian Goods"};
        String[] originalTitles={"Draft specifications","Awaiting supplier responses","Compare two quotations","Order created","Order processing","Order dispatched","Order received"};
        String[] currentTitles={"Office furniture procurement","Warehouse protective equipment","Packaging materials replenishment","IT equipment refresh","Production workshop supplies","Lighting upgrade","Office stationery restock"};
        String oldTerms="Demo / fictional transaction; no payment or real fulfillment. Illustrative terms only.";
        String oldSpec="Demo fictional office supply specification; no real fulfillment.";
        for(int i=0;i<6;i++)jdbc.update("update accounts set email=?,full_name=? where email=?",OLD_EMAILS.get(i),oldNames[i],ProductionDemoSeeder.EMAILS.get(i));
        for(int i=0;i<3;i++)jdbc.update("update organizations set display_name=? where id=(select organization_id from accounts where email=?)",oldOrgs[i],OLD_EMAILS.get(i*2));
        jdbc.update("update marketplace_profiles set description='Demo fictional supplier; no real fulfillment.'");
        jdbc.update("update marketplace_offerings o set title='Demo '||case when p.organization_id=(select organization_id from accounts where email=?) then 'Atlas ' else 'Meridian ' end||replace(lower(category),'_',' '),description='Demo fictional product for exploring sourcing; no real fulfillment.' from marketplace_profiles p where o.profile_id=p.id",OLD_EMAILS.get(2));
        jdbc.update("update supplier_contacts set note='Demo fictional supplier contact.'");
        for(int i=0;i<7;i++) {
            String old="Demo / fictional: "+originalTitles[i];
            jdbc.update("update purchase_orders set rfq_title=?,buyer_display_name=?,supplier_display_name=case when supplier_organization_id=(select organization_id from accounts where email=?) then ? else ? end,terms=? where rfq_title=?",old,oldOrgs[0],OLD_EMAILS.get(2),oldOrgs[1],oldOrgs[2],oldTerms,currentTitles[i]);
            jdbc.update("update rfqs set title=? where title=?",old,currentTitles[i]);
        }
        jdbc.update("update rfq_items set description=?",oldSpec);
        jdbc.update("update purchase_order_items set description=?",oldSpec);
        jdbc.update("update quotations set terms=?",oldTerms);
    }
    List<Map<String,Object>> invariant() {return jdbc.queryForList("select id,password_hash,organization_id,organization_admin,buyer_grant,supplier_grant,active,email_verified from accounts order by id");}
    List<Map<String,Object>> commercial() {return jdbc.queryForList("select id,rfq_id,quotation_id,buyer_organization_id,supplier_organization_id,status,currency,accepted_at,valid_until from purchase_orders order by id");}

    @Test void relabelsOriginalPreservesEditsAndLaterSameTextRecordsAndCanReplay() {
        seed();rollback(()->{
            legacy();var before=invariant();var orders=commercial();var history=jdbc.queryForList("select * from order_events order by id");
            jdbc.update("update accounts set full_name='Owner chosen name' where email=?",OLD_EMAILS.get(1));
            Long org=jdbc.queryForObject("select organization_id from accounts where email=?",Long.class,OLD_EMAILS.get(0));
            Long later=jdbc.queryForObject("insert into rfqs(organization_id,title,created_at) values (?,'Demo / fictional: Order created',current_timestamp+interval '1 day') returning id",Long.class,org);
            Long profile=jdbc.queryForObject("select p.id from marketplace_profiles p join accounts a on a.organization_id=p.organization_id where a.email=?",Long.class,OLD_EMAILS.get(2));
            Long laterProduct=jdbc.queryForObject("insert into marketplace_offerings(profile_id,title,description,category,created_at) values (?,'Demo Atlas cleaning','Owner product','CLEANING',current_timestamp+interval '1 day') returning id",Long.class,profile);
            jdbc.execute(migration());
            assertEquals(before,invariant());assertEquals(orders,commercial());assertEquals(history,jdbc.queryForList("select * from order_events order by id"));
            assertEquals("Amina Bennani",jdbc.queryForObject("select full_name from accounts where email=?",String.class,ProductionDemoSeeder.EMAILS.get(0)));
            assertEquals("Owner chosen name",jdbc.queryForObject("select full_name from accounts where email=?",String.class,ProductionDemoSeeder.EMAILS.get(1)));
            assertEquals("Northstar Manufacturing",jdbc.queryForObject("select display_name from organizations where id=?",String.class,org));
            assertEquals("Demo / fictional: Order created",jdbc.queryForObject("select title from rfqs where id=?",String.class,later));
            assertEquals("Demo Atlas cleaning",jdbc.queryForObject("select title from marketplace_offerings where id=?",String.class,laterProduct));
            assertEquals(1,jdbc.queryForObject("select count(*) from marketplace_offerings where title='Industrial Surface Cleaner 5L'",Integer.class));
            assertEquals(20,jdbc.queryForObject("select count(*) from marketplace_offerings where title not like 'Demo%'",Integer.class));
            assertEquals(7,jdbc.queryForObject("select count(*) from rfqs where title not like 'Demo%'",Integer.class));
            assertEquals(4,jdbc.queryForObject("select count(*) from purchase_orders where rfq_title not like 'Demo%' and buyer_display_name='Northstar Manufacturing' and supplier_display_name not like 'Demo%' and terms not like 'Demo%'",Integer.class));
            assertEquals(6,jdbc.queryForObject("select count(*) from quotations where terms not like 'Demo%'",Integer.class));
            assertEquals(0,jdbc.queryForObject("select count(*) from marketplace_profiles where description like 'Demo%'",Integer.class));
            assertEquals(0,jdbc.queryForObject("select count(*) from accounts where full_name like 'Demo%' or email like 'demo.%'",Integer.class));
            assertDoesNotThrow(()->loginAll());
            assertEquals(1,jdbc.queryForObject("select count(*) from purchase_orders where rfq_title='IT equipment refresh' and buyer_display_name='Northstar Manufacturing'",Integer.class));
            assertEquals(0,jdbc.queryForObject("select count(*) from purchase_order_items where description like 'Demo%';",Integer.class));
            var all=jdbc.queryForList("select * from accounts order by id");jdbc.execute(migration());assertEquals(all,jdbc.queryForList("select * from accounts order by id"));
            seed();assertEquals(all,jdbc.queryForList("select * from accounts order by id"));
        });
    }
    @Test void editedCounterpartySnapshotIsPreserved() {seed();rollback(()->{
        legacy();Long id=jdbc.queryForObject("select min(id) from purchase_orders where supplier_organization_id=(select organization_id from accounts where email=?)",Long.class,OLD_EMAILS.get(2));
        jdbc.update("update purchase_orders set supplier_display_name='Demo Meridian Goods' where id=?",id);
        jdbc.execute(migration());
        assertEquals("Demo Meridian Goods",jdbc.queryForObject("select supplier_display_name from purchase_orders where id=?",String.class,id));
    });}
    @Test void noReceiptIsNoOp() {seed();rollback(()->{legacy();jdbc.update("delete from demo_seed_completions");var before=jdbc.queryForList("select * from accounts order by id");jdbc.execute(migration());assertEquals(before,jdbc.queryForList("select * from accounts order by id"));});}
    @Test void incompatibleReceiptAndEmailCollisionFailWithoutCommittedWrites() {
        seed();var before=invariant();
        assertThrows(org.springframework.dao.DataAccessException.class,()->rollback(()->{legacy();jdbc.update("update demo_seed_completions set revision=2");jdbc.execute(migration());}));assertEquals(before,invariant());
        assertThrows(org.springframework.dao.DataAccessException.class,()->rollback(()->{
            legacy();jdbc.update("insert into accounts(full_name,email,password_hash,organization_id,organization_admin,buyer_grant,supplier_grant,active,email_verified) select 'Unrelated account',?,password_hash,organization_id,false,false,false,true,true from accounts limit 1",ProductionDemoSeeder.EMAILS.get(0));jdbc.execute(migration());
        }));assertEquals(before,invariant());assertEquals(6,jdbc.queryForObject("select count(*) from accounts",Integer.class));
    }
    @Test void allRenamedAccountsUseExistingPasswordAndOrdinaryAuthentication() throws Exception {
        seed();rollback(()->{legacy();jdbc.execute(migration());assertDoesNotThrow(()->loginAll());});
    }
    void loginAll() throws Exception {
        for(String email:ProductionDemoSeeder.EMAILS){mvc.perform(post("/api/auth/login").with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"email\":\""+email+"\",\"password\":\""+PASSWORD+"\"}")).andExpect(status().isOk());}
        org.mockito.Mockito.verifyNoInteractions(mail);
    }
}
