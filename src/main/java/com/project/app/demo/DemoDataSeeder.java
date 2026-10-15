package com.project.app.demo;

import java.net.URI;
import java.util.Arrays;
import java.util.List;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.project.app.identity.service.PasswordPolicy;
import com.project.app.marketplace.model.Offering.Category;

/** Explicit local demo fixture. Never part of normal registration or production migrations. */
@Component
@Profile("demo")
@ConditionalOnProperty(name = "PROCUREFLOW_DEMO_SEED_ENABLED", havingValue = "true")
public class DemoDataSeeder implements ApplicationRunner {
    private static final List<String> EMAILS = List.of("buyer@procureflow.invalid",
            "supplier.one@procureflow.invalid", "supplier.two@procureflow.invalid");
    private static final List<String> NAMES = List.of("Demo Buyer Studio", "Atlas Demo Supply", "Meridian Demo Goods");
    private static final String[][] PRODUCTS = {
        {"USB-C office dock", "Recycled notebook pack", "Adjustable office chair", "Warehouse shelving unit",
         "Reusable event badge", "Protective work gloves", "Recycled shipping cartons", "LED task light",
         "Precision screwdriver set", "Microfiber cleaning kit"},
        {"Wireless conference speaker", "A4 printer paper", "Standing meeting table", "Material handling trolley",
         "Reusable visitor lanyard", "Safety glasses", "Paper shipping envelopes", "Surge protected power strip",
         "Workshop wrench set", "Refillable surface cleaner"}
    };
    private final JdbcTemplate jdbc;
    private final PasswordEncoder encoder;
    private final Environment environment;
    private final TransactionTemplate transaction;

    public DemoDataSeeder(JdbcTemplate jdbc, PasswordEncoder encoder, Environment environment,
            PlatformTransactionManager transactions) {
        this.jdbc = jdbc;
        this.encoder = encoder;
        this.environment = environment;
        this.transaction = new TransactionTemplate(transactions);
    }

    @Override public void run(ApplicationArguments arguments) {
        String url = environment.getProperty("spring.datasource.url", "");
        String password = environment.getProperty("PROCUREFLOW_DEMO_PASSWORD", "");
        validateTarget(url, environment.getActiveProfiles(), password);
        transaction.executeWithoutResult(status -> seed(password));
    }

    static void validateTarget(String jdbcUrl, String[] profiles, String password) {
        if (Arrays.asList(profiles).contains("deploy") || !Arrays.asList(profiles).contains("demo")) {
            throw new IllegalStateException("Demo seeding requires the local demo profile without deploy.");
        }
        try {
            if (!jdbcUrl.startsWith("jdbc:postgresql://")) throw new IllegalArgumentException();
            URI uri = URI.create(jdbcUrl.substring(5));
            if (!List.of("localhost", "127.0.0.1", "[::1]", "::1").contains(uri.getHost())
                    || uri.getUserInfo() != null || uri.getQuery() != null
                    || !uri.getPath().matches("/procureflow_demo_[a-z0-9_]+")) throw new IllegalArgumentException();
        } catch (RuntimeException ex) {
            throw new IllegalStateException("Demo seeding requires an explicit loopback procureflow_demo_ database.");
        }
        if (!PasswordPolicy.accepts(password)) {
            throw new IllegalStateException("Provide PROCUREFLOW_DEMO_PASSWORD with 12 to 128 Unicode code points.");
        }
    }

    private void seed(String password) {
        // Hikari-specific settings may override spring.datasource.url. Check the actual transaction connection too.
        String actualUrl = jdbc.execute((ConnectionCallback<String>) connection -> connection.getMetaData().getURL());
        validateTarget(actualUrl, environment.getActiveProfiles(), password);
        String database = jdbc.queryForObject("select current_database()", String.class);
        if (database == null || !database.matches("procureflow_demo_[a-z0-9_]+")) {
            throw new IllegalStateException("Actual database is not an isolated demo database.");
        }
        jdbc.queryForObject("select pg_advisory_xact_lock(792431001)", Object.class);
        int existing = jdbc.queryForObject("select count(*) from accounts where email in (?,?,?)", Integer.class,
                EMAILS.toArray());
        if (existing != 0) {
            if (existing != 3 || !completeExistingSeed()) {
                throw new IllegalStateException("Partial or conflicting demo data exists; nothing was changed.");
            }
            return; // Preserve demo passwords, product edits and publication state on repeat runs.
        }
        if (jdbc.queryForObject("select count(*) from organizations where display_name in (?,?,?)", Integer.class,
                NAMES.toArray()) != 0) throw new IllegalStateException("Demo organization name collision; nothing was changed.");
        String hash = encoder.encode(password);
        for (int index = 0; index < 3; index++) {
            long organization = jdbc.queryForObject("insert into organizations(display_name) values (?) returning id",
                    Long.class, NAMES.get(index));
            jdbc.update("""
                insert into accounts(full_name,email,password_hash,organization_id,organization_admin,
                                     buyer_grant,supplier_grant,active,email_verified)
                values (?,?,?,?,true,?,?,true,true)
                """, index == 0 ? "Demo Buyer" : "Demo Supplier " + index, EMAILS.get(index), hash,
                    organization, index == 0, index != 0);
            if (index == 0) continue;
            long profile = jdbc.queryForObject("""
                insert into marketplace_profiles(organization_id,description,published,published_at)
                values (?, ?, true, current_timestamp) returning id
                """, Long.class, organization, "Demo supplier catalog for exploring ProcureFlow. Fictional goods; request a quotation.");
            for (int product = 0; product < Category.values().length; product++) {
                jdbc.update("""
                    insert into marketplace_offerings(profile_id,title,description,category,published,published_at)
                    values (?,?,?,?,true,current_timestamp)
                    """, profile, PRODUCTS[index - 1][product],
                        "Demo product from " + NAMES.get(index) + ". Specifications and commercial terms are agreed through an RFQ.",
                        Category.values()[product].name());
            }
        }
    }

    private boolean completeExistingSeed() {
        for (int index = 0; index < 3; index++) {
            var rows = jdbc.queryForList("""
                select a.organization_id from accounts a join organizations o on o.id=a.organization_id
                where a.email=? and o.display_name=? and a.organization_admin=true
                      and a.buyer_grant=? and a.supplier_grant=?
                """, EMAILS.get(index), NAMES.get(index), index == 0, index != 0);
            if (rows.size() != 1) return false;
            if (index == 0) continue;
            long organization = ((Number) rows.get(0).get("organization_id")).longValue();
            if (jdbc.queryForObject("""
                select count(*) from marketplace_offerings f join marketplace_profiles p on p.id=f.profile_id
                where p.organization_id=?
                """, Integer.class, organization) != 10) return false;
        }
        return true;
    }
}
