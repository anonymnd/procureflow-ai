package com.project.app.support;

import java.util.List;
import java.util.regex.Pattern;

import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;

/** Test-only preflight: refuses unsafe targets before datasource/Flyway beans exist. */
public class Slice0DatabaseGuard implements ApplicationContextInitializer<ConfigurableApplicationContext> {
    private static final Pattern TARGET = Pattern.compile(
            "jdbc:postgresql://localhost:5332/procureflow_slice0_[0-9]{14}(?:_[a-z0-9]+)?");

    @Override
    public void initialize(ConfigurableApplicationContext context) {
        Environment environment = context.getEnvironment();
        String url = property(environment, "spring.datasource.url");
        require(TARGET.matcher(url).matches(), "explicit isolated localhost:5332 datasource URL required");
        require(!property(environment, "spring.datasource.username").isBlank(), "explicit test username required");
        require(!property(environment, "spring.datasource.password").isBlank(), "explicit test password required");
        String flywayUrl = property(environment, "spring.flyway.url");
        require(flywayUrl.isBlank() || flywayUrl.equals(url), "Flyway target must equal datasource target");
        require(property(environment, "spring.datasource.jndi-name").isBlank(), "JNDI must not override test target");
        // Boot binds these pool settings after the primary datasource properties.
        String hikariUrl = property(environment, "spring.datasource.hikari.jdbc-url");
        require(hikariUrl.isBlank() || hikariUrl.equals(url), "Hikari target must equal datasource target");
        require(property(environment, "spring.datasource.hikari.data-source-class-name").isBlank(),
                "Hikari native datasource must not override test target");
        require(property(environment, "spring.datasource.hikari.data-source-jndi").isBlank(),
                "Hikari JNDI must not override test target");
        require(!hasHikariDataSourceProperties(environment), "Hikari driver properties must not override test target");
        require("validate".equalsIgnoreCase(property(environment, "spring.jpa.hibernate.ddl-auto")),
                "Hibernate must only validate");
        require("true".equalsIgnoreCase(property(environment, "spring.flyway.enabled")), "Flyway must be enabled");
        require("true".equalsIgnoreCase(property(environment, "spring.flyway.clean-disabled")),
                "Flyway clean must be disabled");
        // Native provider properties take precedence over Boot's ddl-auto setting.
        String nativeDdl = property(environment, "spring.jpa.properties.hibernate.hbm2ddl.auto");
        require(nativeDdl.isBlank() || "validate".equalsIgnoreCase(nativeDdl), "unsafe native Hibernate DDL override");
        for (String key : List.of(
                "spring.jpa.properties.jakarta.persistence.schema-generation.database.action",
                "spring.jpa.properties.jakarta.persistence.schema-generation.scripts.action",
                "spring.jpa.properties.javax.persistence.schema-generation.database.action",
                "spring.jpa.properties.javax.persistence.schema-generation.scripts.action")) {
            String action = property(environment, key);
            require(action.isBlank() || "none".equalsIgnoreCase(action), "unsafe schema-generation override");
        }
    }

    private static String property(Environment environment, String name) {
        try {
            return environment.getProperty(name, "");
        } catch (IllegalArgumentException ex) {
            throw new IllegalStateException("Slice 0 database preflight: unresolved explicit test configuration");
        }
    }

    private static boolean hasHikariDataSourceProperties(Environment environment) {
        try {
            return Binder.get(environment).bind("spring.datasource.hikari.data-source-properties",
                    Bindable.mapOf(String.class, Object.class)).map(properties -> !properties.isEmpty()).orElse(false);
        } catch (RuntimeException ex) {
            throw new IllegalStateException("Slice 0 database preflight: unresolved Hikari driver configuration");
        }
    }

    private static void require(boolean safe, String reason) {
        if (!safe) {
            // Never echo URLs, credentials or exception messages from configuration.
            throw new IllegalStateException("Slice 0 database preflight: " + reason);
        }
    }
}
