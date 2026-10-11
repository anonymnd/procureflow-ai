package com.project.app.support;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.env.MapPropertySource;
import org.springframework.mock.env.MockEnvironment;

/** TC-S0-011 -> REQ-S0-002/006@1, BL-S0-R1. No datasource is constructed. */
class Slice0DatabaseGuardTest {
    private static final String SAFE = "jdbc:postgresql://localhost:5332/procureflow_slice0_20261010123456";

    private GenericApplicationContext context(Map<String, Object> changes) {
        Map<String, Object> properties = new HashMap<>();
        properties.put("spring.datasource.url", SAFE);
        properties.put("spring.datasource.username", "test-user");
        properties.put("spring.datasource.password", "test-only-secret");
        properties.put("spring.jpa.hibernate.ddl-auto", "validate");
        properties.put("spring.flyway.enabled", "true");
        properties.put("spring.flyway.clean-disabled", "true");
        properties.putAll(changes);
        GenericApplicationContext context = new GenericApplicationContext();
        MockEnvironment environment = new MockEnvironment();
        environment.getPropertySources().addFirst(new MapPropertySource("test", properties));
        context.setEnvironment(environment);
        return context;
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "_abc123"})
    void acceptsOnlyIsolatedTargets(String suffix) {
        try (var context = context(Map.of("spring.datasource.url", SAFE + suffix,
                "spring.flyway.url", SAFE + suffix))) {
            assertDoesNotThrow(() -> new Slice0DatabaseGuard().initialize(context));
            assertFalse(context.isActive());
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "jdbc:postgresql://localhost:5332/ProcureFlow",
            "jdbc:postgresql://localhost:5332/postgres", "jdbc:postgresql://remote:5332/procureflow_slice0_20261010123456",
            "jdbc:postgresql://localhost:5432/procureflow_slice0_20261010123456",
            "jdbc:postgresql://127.0.0.1:5332/procureflow_slice0_20261010123456",
            "jdbc:postgresql://localhost:5332/procureflow_slice0_", "jdbc:postgresql://localhost:5332/procureflow_slice0_2026101012345",
            "jdbc:postgresql://localhost:5332/procureflow_slice0_202610101234567",
            "jdbc:postgresql://localhost:5332/procureflow_slice0_20261010123456_ABC",
            "jdbc:postgresql://localhost:5332/procureflow_slice0_20261010123456_",
            "jdbc:postgresql://user:secret@localhost:5332/procureflow_slice0_20261010123456",
            "jdbc:postgresql://localhost:5332/procureflow_slice0_20261010123456?currentSchema=public"})
    void rejectsUnsafeUrlsBeforeBeans(String url) {
        rejects(Map.of("spring.datasource.url", url));
    }

    static Stream<Map<String, Object>> unsafeOverrides() {
        return Stream.of(
                Map.of("spring.flyway.url", "jdbc:postgresql://localhost:5332/ProcureFlow"),
                Map.of("spring.datasource.hikari.jdbc-url", "jdbc:postgresql://localhost:5332/ProcureFlow"),
                Map.of("spring.datasource.hikari.data-source-class-name", "org.postgresql.ds.PGSimpleDataSource"),
                Map.of("spring.datasource.hikari.data-source-jndi", "java:comp/env/jdbc/business"),
                Map.of("spring.datasource.hikari.data-source-properties.databaseName", "ProcureFlow"),
                Map.of("spring.datasource.jndi-name", "java:comp/env/jdbc/business"),
                Map.of("spring.jpa.hibernate.ddl-auto", "update"),
                Map.of("spring.jpa.hibernate.ddl-auto", "create"),
                Map.of("spring.jpa.hibernate.ddl-auto", "create-drop"),
                Map.of("spring.jpa.properties.hibernate.hbm2ddl.auto", "create"),
                Map.of("spring.jpa.properties.jakarta.persistence.schema-generation.database.action", "drop-and-create"),
                Map.of("spring.jpa.properties.jakarta.persistence.schema-generation.scripts.action", "create"),
                Map.of("spring.flyway.enabled", "false"),
                Map.of("spring.flyway.clean-disabled", "false"),
                Map.of("spring.datasource.username", ""),
                Map.of("spring.datasource.password", ""),
                Map.of("spring.datasource.url", "${PROCUREFLOW_MISSING_TEST_URL}"));
    }

    @ParameterizedTest
    @MethodSource("unsafeOverrides")
    void rejectsEffectiveTargetAndSchemaOverrides(Map<String, Object> changes) {
        rejects(changes);
    }

    @Test
    void acceptsHikariUrlOnlyWhenItMatchesGuardedTarget() {
        try (var context = context(Map.of("spring.datasource.hikari.jdbc-url", SAFE))) {
            assertDoesNotThrow(() -> new Slice0DatabaseGuard().initialize(context));
            assertFalse(context.isActive());
        }
    }

    @Test
    void rejectsMissingConfiguration() {
        try (var context = new GenericApplicationContext()) {
            context.setEnvironment(new MockEnvironment());
            assertThrows(IllegalStateException.class, () -> new Slice0DatabaseGuard().initialize(context));
            assertFalse(context.isActive());
        }
    }

    @Test
    void guardsResolvedSpringPropertyRatherThanEnvironmentVariable() {
        try (var context = context(Map.of("PROCUREFLOW_TEST_DB_URL", SAFE))) {
            context.getEnvironment().getPropertySources().addFirst(new MapPropertySource("command-line",
                    Map.of("spring.datasource.url", "jdbc:postgresql://localhost:5332/ProcureFlow")));
            assertThrows(IllegalStateException.class, () -> new Slice0DatabaseGuard().initialize(context));
            assertFalse(context.isActive());
        }
    }

    private void rejects(Map<String, Object> changes) {
        try (var context = context(changes)) {
            context.registerBean("datasourceSentinel", Object.class,
                    () -> { fail("Database beans must not initialize on preflight rejection"); return new Object(); });
            var error = assertThrows(IllegalStateException.class,
                    () -> new Slice0DatabaseGuard().initialize(context));
            assertTrue(error.getMessage().startsWith("Slice 0 database preflight:"));
            assertFalse(error.getMessage().contains("test-only-secret"));
            assertFalse(context.isActive());
            assertEquals(0, context.getBeanFactory().getSingletonCount());
        }
    }
}
