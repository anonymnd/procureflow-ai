package com.project.app.demo;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.springframework.jdbc.core.ConnectionCallback;
import org.junit.jupiter.api.Test;

class DemoDataSeederGuardTest {
    @Test void effectiveConnectionOverrideCannotBypassDeclaredLoopbackTarget() throws Exception {
        for (String effective : new String[]{"jdbc:postgresql://remote.example/procureflow_demo_test",
                "jdbc:postgresql://localhost/production"}) {
            var jdbc = mock(org.springframework.jdbc.core.JdbcTemplate.class);
            var connection = mock(java.sql.Connection.class);
            var metadata = mock(java.sql.DatabaseMetaData.class);
            when(connection.getMetaData()).thenReturn(metadata);
            when(metadata.getURL()).thenReturn(effective);
            when(jdbc.execute(org.mockito.ArgumentMatchers.<ConnectionCallback<String>>any()))
                .thenAnswer(call -> call.<ConnectionCallback<String>>getArgument(0).doInConnection(connection));
            var environment = new org.springframework.mock.env.MockEnvironment()
                .withProperty("spring.datasource.url", "jdbc:postgresql://localhost/procureflow_demo_claimed")
                .withProperty("PROCUREFLOW_DEMO_PASSWORD", "valid-demo-password");
            environment.setActiveProfiles("demo");
            var transactions = mock(org.springframework.transaction.PlatformTransactionManager.class);
            when(transactions.getTransaction(any())).thenReturn(new org.springframework.transaction.support.SimpleTransactionStatus());
            var runner = new DemoDataSeeder(jdbc,mock(org.springframework.security.crypto.password.PasswordEncoder.class),environment,transactions);
            assertThrows(IllegalStateException.class,()->runner.run(new org.springframework.boot.DefaultApplicationArguments()));
            verify(jdbc).execute(org.mockito.ArgumentMatchers.<ConnectionCallback<String>>any());
            verifyNoMoreInteractions(jdbc); // No data query, lock or write after the unsafe effective URL.
        }
    }
    @Test void ordinaryStartupAndEitherMissingOptInExcludeTheSeeder() {
        for (String[] properties : new String[][]{{}, {"spring.profiles.active=demo"},
                {"PROCUREFLOW_DEMO_SEED_ENABLED=true"}}) {
            new org.springframework.boot.test.context.runner.ApplicationContextRunner()
                .withUserConfiguration(DemoDataSeeder.class).withPropertyValues(properties)
                .run(context -> assertEquals(0, context.getBeanNamesForType(DemoDataSeeder.class).length));
        }
    }
    @Test void rejectsUnsafeTargetsAndProfilesWithoutLeakingPassword() {
        String password = "test-password-long";
        for (String url : new String[]{"jdbc:postgresql://remote.example/procureflow_demo_test",
                "jdbc:postgresql://localhost/production", "jdbc:postgresql:procureflow_demo_test",
                "jdbc:postgresql://localhost/procureflow_demo_test?host=remote.example",
                "jdbc:postgresql://localhost/procureflow_demo_test?options=-csearch_path=public"}) {
            var error = assertThrows(IllegalStateException.class,
                    () -> DemoDataSeeder.validateTarget(url, new String[]{"demo"}, password));
            assertFalse(error.getMessage().contains(password));
        }
        for (String[] profiles : new String[][]{{"demo","deploy"},{"deploy"},{}}) {
            assertThrows(IllegalStateException.class, () -> DemoDataSeeder.validateTarget(
                    "jdbc:postgresql://localhost/procureflow_demo_test", profiles, password));
        }
    }
    @Test void requiresExplicitValidPasswordAndLoopbackDemoDatabase() {
        String url = "jdbc:postgresql://127.0.0.1:5332/procureflow_demo_test";
        for (String password : new String[]{"", "short", "x".repeat(129)}) {
            assertThrows(IllegalStateException.class, () -> DemoDataSeeder.validateTarget(url,new String[]{"demo"},password));
        }
        assertDoesNotThrow(() -> DemoDataSeeder.validateTarget(url,new String[]{"demo"},"x".repeat(12)));
    }
}
