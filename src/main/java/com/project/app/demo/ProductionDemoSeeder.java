package com.project.app.demo;

import java.net.URI;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import com.zaxxer.hikari.HikariDataSource;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.project.app.marketplace.model.Offering.Category;

/** REQ-FDP-002@R1: deliberate fixture insertion, never an authentication bypass. */
@Component
@Profile("deploy")
@ConditionalOnProperty(name="PROCUREFLOW_PRODUCTION_DEMO_ENABLED", havingValue="true")
public class ProductionDemoSeeder implements ApplicationRunner {
    static final String KEY = "production-demo-v1";
    static final List<String> EMAILS = List.of("demo.buyer@procureflow.invalid", "demo.buyer.employee@procureflow.invalid",
            "demo.atlas@procureflow.invalid", "demo.atlas.employee@procureflow.invalid",
            "demo.meridian@procureflow.invalid", "demo.meridian.employee@procureflow.invalid");
    static final List<String> ORGANIZATIONS = List.of("Demo Buyer Workshop", "Demo Atlas Supply", "Demo Meridian Goods");
    private static final String TERMS = "Demo / fictional transaction; no payment or real fulfillment. Illustrative terms only.";
    private final JdbcTemplate jdbc;
    private final PasswordEncoder encoder;
    private final Environment env;
    private final TransactionTemplate transaction;

    public ProductionDemoSeeder(JdbcTemplate jdbc, PasswordEncoder encoder, Environment env,
            PlatformTransactionManager transactions) {
        this.jdbc=jdbc; this.encoder=encoder; this.env=env; this.transaction=new TransactionTemplate(transactions);
    }

    @Override public void run(ApplicationArguments arguments) {
        String password=env.getProperty("PROCUREFLOW_PRODUCTION_DEMO_PASSWORD", "");
        String database=env.getProperty("PROCUREFLOW_PRODUCTION_DEMO_DATABASE", "");
        validateConfiguration(env.getActiveProfiles(), database, password);
        transaction.executeWithoutResult(status -> {
            String actualUrl=jdbc.execute((ConnectionCallback<String>) c -> c.getMetaData().getURL());
            boolean local=isLoopbackPostgres(actualUrl);
            if (!database.equals(jdbc.queryForObject("select current_database()", String.class)))
                throw refused("Actual database does not match the explicitly configured target.");
            if (!local) {
                // Neon terminates client TLS at its proxy; pg_stat_ssl describes a different hop.
                // Require the existing pool's strict driver TLS contract on this actual connection.
                validateRemotePool(jdbc.getDataSource(),actualUrl);
            }
            jdbc.queryForObject("select pg_advisory_xact_lock(792431002)", Object.class);
            if (hasCompatibleReceipt()) return;
            if (jdbc.queryForObject("select count(*) from accounts where email in (?,?,?,?,?,?)", Integer.class,
                    EMAILS.toArray()) != 0 || jdbc.queryForObject(
                    "select count(*) from organizations where display_name in (?,?,?)", Integer.class,
                    ORGANIZATIONS.toArray()) != 0)
                throw refused("Reserved demo identity collision or partial fixture.");
            insert(password);
        });
    }

    static void validateConfiguration(String[] profiles, String database, String password) {
        if (!Arrays.asList(profiles).contains("deploy") || Arrays.asList(profiles).contains("demo"))
            throw refused("Requires deploy profile without local demo profile.");
        if (database == null || !database.matches("[A-Za-z0-9_][A-Za-z0-9_-]{0,62}"))
            throw refused("Explicit expected database name required.");
        if (password == null || password.codePointCount(0,password.length())<20
                || password.codePointCount(0,password.length())>128 || password.chars().distinct().count()<10)
            throw refused("Provide a strong explicit demo password of 20 to 128 characters with at least 10 distinct characters.");
    }

    static boolean isLoopbackPostgres(String url) {
        try {
            if (url == null || !url.startsWith("jdbc:postgresql://")) throw new IllegalArgumentException();
            URI uri=URI.create(url.substring(5));
            if (uri.getHost()==null || uri.getUserInfo()!=null || uri.getFragment()!=null
                    || uri.getPath()==null || uri.getPath().length()<2)
                throw new IllegalArgumentException();
            // Query parameters can override the host; only an unambiguous loopback URL exempts TLS.
            return uri.getRawQuery()==null && List.of("localhost","127.0.0.1","::1","[::1]").contains(uri.getHost());
        } catch (RuntimeException ex) { throw refused("Actual connection must be an explicit PostgreSQL connection."); }
    }

    static void validateRemotePool(javax.sql.DataSource source, String actualUrl) {
        if (!(source instanceof HikariDataSource pool)
                || pool.getDataSource()!=null || pool.getDataSourceClassName()!=null || pool.getDataSourceJNDI()!=null)
            throw refused("Remote fixture requires the configured JDBC pool.");
        validateRemoteTls(actualUrl,pool.getDataSourceProperties());
    }

    static void validateRemoteTls(String actualUrl, Properties properties) {
        // Refuse URL overrides rather than maintaining a second PostgreSQL option parser.
        if (URI.create(actualUrl.substring(5)).getRawQuery()!=null
                || !"verify-full".equals(properties.getProperty("sslmode"))
                || !"org.postgresql.ssl.DefaultJavaSSLFactory".equals(properties.getProperty("sslfactory"))
                || properties.containsKey("sslhostnameverifier") || properties.containsKey("socketFactory"))
            throw refused("Remote fixture requires verify-full TLS with the trusted default factory and no overrides.");
    }

    private boolean hasCompatibleReceipt() {
        var receipts=jdbc.queryForList("select revision,organization_ids,account_ids from demo_seed_completions where fixture_key=?",KEY);
        if (receipts.isEmpty()) return false;
        var row=receipts.get(0);
        try {
            Long[] organizations=(Long[])((java.sql.Array)row.get("organization_ids")).getArray();
            Long[] accounts=(Long[])((java.sql.Array)row.get("account_ids")).getArray();
            if (!Integer.valueOf(1).equals(row.get("revision")) || organizations.length!=3 || accounts.length!=6
                    || Arrays.stream(organizations).distinct().count()!=3 || Arrays.stream(accounts).distinct().count()!=6)
                throw refused("Incompatible demo completion receipt.");
            for (int i=0;i<6;i++) if (jdbc.queryForObject(
                    "select count(*) from accounts where id=? and email=? and organization_id=?",Integer.class,
                    accounts[i],EMAILS.get(i),organizations[i/2])!=1)
                throw refused("Completed fixture identity binding changed; no repair performed.");
            return true;
        } catch (java.sql.SQLException | ClassCastException ex) { throw refused("Invalid demo completion receipt."); }
    }

    private void insert(String password) {
        Instant now=Instant.now();
        Timestamp created=Timestamp.from(now.minusSeconds(3600));
        Timestamp submitted=Timestamp.from(now.minusSeconds(3000));
        Timestamp accepted=Timestamp.from(now.minusSeconds(2400));
        Timestamp valid=Timestamp.from(now.plusSeconds(90L*86400));
        long[] organizations=new long[3], accounts=new long[6];
        String hash=encoder.encode(password);
        for (int i=0;i<3;i++) {
            organizations[i]=id("insert into organizations(display_name,created_at) values (?,?) returning id",ORGANIZATIONS.get(i),created);
            for (int member=0;member<2;member++) {
                int index=i*2+member;
                accounts[index]=id("""
                    insert into accounts(full_name,email,password_hash,organization_id,organization_admin,
                        buyer_grant,supplier_grant,active,email_verified,created_at)
                    values (?,?,?,?,?,?,?,true,true,?) returning id
                    """, "Demo "+(i==0?"Buyer":i==1?"Atlas":"Meridian")+(member==0?" Admin":" Employee"),
                        EMAILS.get(index),hash,organizations[i],member==0,i==0,i!=0,created);
            }
            if (i==0) continue;
            long profile=id("""
                insert into marketplace_profiles(organization_id,description,published,published_at,created_at)
                values (?,?,true,?,?) returning id
                """,organizations[i],"Demo fictional supplier; no real fulfillment.",created,created);
            for (Category category:Category.values()) jdbc.update("""
                insert into marketplace_offerings(profile_id,title,description,category,published,published_at,created_at)
                values (?,?,?,?,true,?,?)
                """,profile,"Demo "+(i==1?"Atlas ":"Meridian ")+category.name().toLowerCase().replace('_',' '),
                    "Demo fictional product for exploring sourcing; no real fulfillment.",category.name(),created,created);
            jdbc.update("insert into supplier_contacts(buyer_organization_id,supplier_organization_id,note) values (?,?,?)",
                    organizations[0],organizations[i],"Demo fictional supplier contact.");
        }
        String[] titles={"Draft specifications","Awaiting supplier responses","Compare two quotations","Order created",
                "Order processing","Order dispatched","Order received"};
        String[] stages={"CREATED","CONFIRMED","PROCESSING","DISPATCHED","RECEIVED"};
        for (int i=0;i<7;i++) {
            String title="Demo / fictional: "+titles[i];
            long rfq=id("insert into rfqs(organization_id,title,created_at) values (?,?,?) returning id",organizations[0],title,created);
            long item=id("insert into rfq_items(rfq_id,description,quantity,unit,created_at) values (?,?,10,'units',?) returning id",
                    rfq,"Demo fictional office supply specification; no real fulfillment.",created);
            if (i==0) continue;
            jdbc.update("update rfqs set status='SENT' where id=?",rfq);
            int winner=i%2+1;
            for (int supplier=1;supplier<=2;supplier++) {
                long invitation=id("insert into rfq_invitations(rfq_id,supplier_organization_id,created_at) values (?,?,?) returning id",
                        rfq,organizations[supplier],created);
                if (i==1 || (i>=3 && supplier!=winner)) continue;
                long quote=id("""
                    insert into quotations(invitation_id,status,currency,valid_until,terms,submitted_at)
                    values (?,'SUBMITTED','MAD',?,?,?) returning id
                    """,invitation,valid,TERMS,submitted);
                int price=100+supplier*10+i;
                jdbc.update("insert into quotation_items(quotation_id,rfq_item_id,unit_price) values (?,?,?)",quote,item,price);
                if (i==2) continue;
                jdbc.update("update quotations set status='ACCEPTED' where id=?",quote);
                jdbc.update("update rfqs set status='AWARDED',winning_quotation_id=? where id=?",quote,rfq);
                int stage=i==3?0:i-2;
                long order=id("""
                    insert into purchase_orders(rfq_id,quotation_id,buyer_organization_id,supplier_organization_id,
                        accepting_account_id,rfq_title,buyer_display_name,supplier_display_name,status,currency,terms,valid_until,accepted_at)
                    values (?,?,?,?,?,?,?,?,?,'MAD',?,?,?) returning id
                    """,rfq,quote,organizations[0],organizations[supplier],accounts[0],title,ORGANIZATIONS.get(0),
                        ORGANIZATIONS.get(supplier),stages[stage],TERMS,valid,accepted);
                jdbc.update("""
                    insert into purchase_order_items(order_id,rfq_item_id,description,quantity,unit,unit_price,line_total)
                    select ?,id,description,quantity,unit,?,quantity*? from rfq_items where id=?
                    """,order,price,price,item);
                for (int event=1;event<=stage;event++) jdbc.update("""
                    insert into order_events(order_id,target_status,occurred_at,acting_account_id) values (?,?,?,?)
                    """,order,stages[event],Timestamp.from(now.minusSeconds(2400-event*300L)),event==4?accounts[0]:accounts[supplier*2]);
            }
        }
        // The receipt is the final write, committed atomically with the complete fixture.
        jdbc.update("""
            insert into demo_seed_completions(fixture_key,revision,organization_ids,account_ids,completed_at)
            values (?,1,?::bigint[],?::bigint[],?)
            """,KEY,array(organizations),array(accounts),Timestamp.from(now));
    }

    private long id(String sql,Object... args) { return jdbc.queryForObject(sql,Long.class,args); }
    private static String array(long[] values) { return "{"+String.join(",",Arrays.stream(values).mapToObj(Long::toString).toList())+"}"; }
    private static IllegalStateException refused(String reason) { return new IllegalStateException("Production demo refused: "+reason); }
}
