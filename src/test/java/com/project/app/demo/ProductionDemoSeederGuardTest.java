package com.project.app.demo;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class ProductionDemoSeederGuardTest {
    @Test void remotePoolRequiresStrictTlsWithoutUrlOrFactoryOverrides() {
        var secure=new java.util.Properties();
        secure.setProperty("sslmode","verify-full");
        secure.setProperty("sslfactory","org.postgresql.ssl.DefaultJavaSSLFactory");
        String url="jdbc:postgresql://remote.example/procureflow";
        assertDoesNotThrow(()->ProductionDemoSeeder.validateRemoteTls(url,secure));
        try(var pool=new com.zaxxer.hikari.HikariDataSource()) {
            pool.setDataSourceProperties(secure);
            assertDoesNotThrow(()->ProductionDemoSeeder.validateRemotePool(pool,url));
            pool.setDataSourceClassName("custom");
            assertThrows(IllegalStateException.class,()->ProductionDemoSeeder.validateRemotePool(pool,url));
            pool.setDataSourceClassName(null); pool.setDataSourceJNDI("custom");
            assertThrows(IllegalStateException.class,()->ProductionDemoSeeder.validateRemotePool(pool,url));
            pool.setDataSourceJNDI(null); pool.setDataSource(org.mockito.Mockito.mock(javax.sql.DataSource.class));
            assertThrows(IllegalStateException.class,()->ProductionDemoSeeder.validateRemotePool(pool,url));
        }
        assertThrows(IllegalStateException.class,()->ProductionDemoSeeder.validateRemotePool(org.mockito.Mockito.mock(javax.sql.DataSource.class),url));
        for(String mode:new String[]{"","disable","require","verify-ca"}) {
            var properties=new java.util.Properties(); properties.putAll(secure); properties.setProperty("sslmode",mode);
            assertThrows(IllegalStateException.class,()->ProductionDemoSeeder.validateRemoteTls(url,properties));
        }
        for(String key:new String[]{"sslfactory","sslhostnameverifier","socketFactory"}) {
            var properties=new java.util.Properties(); properties.putAll(secure); properties.setProperty(key,"custom");
            assertThrows(IllegalStateException.class,()->ProductionDemoSeeder.validateRemoteTls(url,properties));
        }
        for(String query:new String[]{"sslmode=disable","sslfactory=custom","sslmode=verify-full"})
            assertThrows(IllegalStateException.class,()->ProductionDemoSeeder.validateRemoteTls(url+"?"+query,secure));
    }
    @Test void inactiveWithoutBothProfileAndExplicitFlag() {
        for (String[] properties:new String[][]{{},{"spring.profiles.active=deploy"},
                {"PROCUREFLOW_PRODUCTION_DEMO_ENABLED=true"}})
            new ApplicationContextRunner().withUserConfiguration(ProductionDemoSeeder.class).withPropertyValues(properties)
                .run(c->assertEquals(0,c.getBeanNamesForType(ProductionDemoSeeder.class).length));
    }
    @Test void rejectsMissingWeakCredentialsAndUnboundedTargetWithoutDisclosingPassword() {
        String strong="Distinct-Test-Password-2026!";
        for (String password:new String[]{"","short","x".repeat(30),"x".repeat(129)})
            assertThrows(IllegalStateException.class,()->ProductionDemoSeeder.validateConfiguration(new String[]{"deploy"},"procureflow",password));
        for (String database:new String[]{"","  ","procureflow?host=other","a/b"}) {
            var ex=assertThrows(IllegalStateException.class,()->ProductionDemoSeeder.validateConfiguration(new String[]{"deploy"},database,strong));
            assertFalse(ex.getMessage().contains(strong));
        }
        for(String[] profiles:new String[][]{{},{"demo"},{"deploy","demo"}})
            assertThrows(IllegalStateException.class,()->ProductionDemoSeeder.validateConfiguration(profiles,"procureflow",strong));
        assertDoesNotThrow(()->ProductionDemoSeeder.validateConfiguration(new String[]{"deploy"},"procureflow",strong));
    }
    @Test void effectiveConnectionMustBePostgresAndOnlyLoopbackExemptsTls() {
        assertTrue(ProductionDemoSeeder.isLoopbackPostgres("jdbc:postgresql://localhost:5332/test"));
        assertFalse(ProductionDemoSeeder.isLoopbackPostgres("jdbc:postgresql://remote.example/test"));
        assertFalse(ProductionDemoSeeder.isLoopbackPostgres("jdbc:postgresql://remote.example/test?sslmode=require"));
        assertFalse(ProductionDemoSeeder.isLoopbackPostgres("jdbc:postgresql://localhost/test?host=remote.example"));
        assertFalse(ProductionDemoSeeder.isLoopbackPostgres("jdbc:postgresql://localhost/test?sslmode=disable"));
        for(String url:new String[]{"jdbc:h2:mem:test","jdbc:postgresql:test","jdbc:postgresql://user:secret@localhost/test"})
            assertThrows(IllegalStateException.class,()->ProductionDemoSeeder.isLoopbackPostgres(url));
    }
}
