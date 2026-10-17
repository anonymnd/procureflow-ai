# ProcureFlow portfolio

ProcureFlow is a full-stack B2B procurement platform for supplier discovery, requests for quotation, quotation comparison, purchase orders and fulfillment tracking. It provides separate client and supplier workspaces with organization-scoped access.

**Stack:** React, Vite, Java, Spring Boot, Spring Security, PostgreSQL, JPA, Flyway and Docker.

**Architecture:** a feature-oriented modular monolith with controller, service and repository layers. The frontend and backend are deployed separately. [System design](../architecture.md).

**Patterns:** repositories, explicit DTOs, constructor injection, transactional services, state transitions, commercial snapshots and idempotent operations.

**Method:** iterative vertical slices with requirement-to-test traceability, an adapted Agile V workflow, review and YAGNI/KISS principles.

[Live application](https://procureflow-anonymnd.vercel.app) · [Image folder](screenshots/) · [Portfolio metadata](project.json)

The current images are captures of an earlier deployed release and contain fictional sample names. They do not show the later terminology/sample-name correction. Replace them with verified live captures before using them as images of that update. Credentials must never appear in screenshots or portfolio copy.
