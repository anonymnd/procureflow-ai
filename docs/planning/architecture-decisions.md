# ProcureFlow architecture decision records

Updated: 2026-10-09. These lightweight records distinguish confirmed direction from technical proposals. None authorizes application implementation.

## ADR-001 - Organization identity and employee association

- Status: proposed; one organization per employee is confirmed.
- Context: one company can buy and sell; employees act for it and have scoped permissions.
- Decision: propose one Organization identity with a direct organization association for each employee. Keep buying/selling capability separate from employee permissions. Platform staff have explicitly separate scope.
- Alternatives considered: separate BuyerCompany/SupplierCompany identities; explicit multi-organization Membership entity from day one.
- Why chosen: shared identity avoids duplicating a company; direct association implements the agreed employee cardinality without unused switching/lifecycle machinery.
- Consequences: private supplier relationships and public supplier profiles remain separate concepts. Organization administrators cannot grant platform authority. A direct link needs migration if one user later represents multiple companies.
- Revisit trigger: approved multi-organization access, organization-transfer history, or an employee relationship lifecycle that cannot be represented cleanly on the account.

## ADR-002 - Shared database with participant-aware authorization

- Status: proposed; private cross-organization data isolation is required.
- Context: RFQs, quotes and orders involve several companies with asymmetric visibility.
- Decision: propose shared PostgreSQL schema, explicit ownership/participant relationships, trusted authenticated organization context and server-side action/record authorization.
- Alternatives considered: database/schema per tenant; tenant ID filtering alone; PostgreSQL row-level security as the only policy.
- Why chosen: shared sourcing records naturally cross business boundaries; one database makes transactional award/order creation manageable. A simple owner-only filter cannot authorize invited suppliers correctly.
- Consequences: every private read/list/count/command must enforce a policy; integration tests must prove authorized sharing and denial. Platform support access requires a separate policy.
- Revisit trigger: actual contractual isolation needs, measurable operational scaling limits, or a justified row-level-security defense-in-depth design.

## ADR-003 - Modular monolith and atomic award/order boundary

- Status: modular-monolith direction and automatic order creation confirmed; package/transaction design proposed.
- Context: small existing Spring Boot backend; related RFQ/quotation/order rules require consistent updates.
- Decision: one backend deployment and database initially, domain-oriented packages, explicit module contracts. Accepting a quote and creating its order form one transaction with database/concurrency protection.
- Alternatives considered: early microservices; global technical-layer folders; asynchronous order creation after quote acceptance.
- Why chosen: these choices serve the existing workflow with low operational overhead. Asynchronous order creation could leave an accepted quote without its required order.
- Consequences: module dependencies require discipline; transaction tests and duplicate-award protection are mandatory. Do not confuse a modular monolith with mandatory enterprise-framework abstractions.
- Revisit trigger: demonstrated independent team/release/scale needs; extraction must preserve commercial consistency rather than merely move tables.

## ADR-004 - One frontend codebase, distinct experiences

- Status: proposed; React and two product experiences are intended.
- Context: public discovery and authenticated operations share identity/API and common interface patterns.
- Decision: one React codebase initially with public, operational and organization-administration routes; shared components/API client; scoped buyer/supplier navigation.
- Alternatives considered: two independently deployed apps immediately; duplicated API clients/components; delaying React until backend completion.
- Why chosen: a small project can deliver UI alongside each vertical slice without duplicate infrastructure.
- Consequences: route and bundle boundaries need attention; public projections/authenticated data remain separate. Exact session/domain-sharing behavior depends on deployment design.
- Revisit trigger: different teams, release cadences, public-site performance requirements or availability goals warrant independently deployed frontends.
