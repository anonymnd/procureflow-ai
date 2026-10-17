# ProcureFlow

ProcureFlow is a B2B procurement application. This branch contains the original Spring Boot supplier API baseline; the complete marketplace and procurement workflows are on the release branches.

[Live application](https://procureflow-anonymnd.vercel.app) · [Current application source](https://github.com/anonymnd/procureflow-ai/tree/codex/client-terminology)

## This branch

Java 17, Spring Boot, Spring Data JPA, PostgreSQL and Flyway. The source includes supplier CRUD endpoints, validation, consistent API errors and unit/integration tests.

Start the local PostgreSQL service with `docker compose up -d db`, then run `./mvnw spring-boot:run` (`mvnw.cmd` on Windows). The database defaults in this branch are for local development only. Database integration tests require a separate explicitly configured database; inspect `src/test/resources/application-slice0-test.properties` and the test database guard before running them.

Do not store real credentials or customer data in the repository. See each release branch for the source and setup documentation matching that version.
