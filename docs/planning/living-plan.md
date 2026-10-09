# ProcureFlow living product and architecture plan

Status: draft for discussion, not an approved implementation specification.
Updated: 2026-10-09. Application implementation has not started under this plan.

Read [confirmed decisions](decisions.md), [vertical slices](vertical-slices.md), [architecture decisions](architecture-decisions.md), and [conceptual diagrams](diagrams.md) alongside this document. Proposals below must not be mistaken for confirmed product decisions or implemented behavior.

## 1. Product vision and business model

ProcureFlow is a B2B platform where employees discover suppliers and conduct procurement on behalf of organizations. Marketplace discovery and procurement operations share one API and business model. The marketplace exposes published information; the dashboard exposes authorized operational information.

Value to buyers: discover suppliers, request comparable quotations, make an explicit purchasing decision, and track the resulting order. Value to suppliers: publish offerings, receive targeted RFQs, submit quotations, and fulfill won orders. Existing suppliers remain usable through private buyer contacts; marketplace participation is not the only way to establish a relationship.

Revenue model, target sector, geographic market, and first pilot users are not confirmed. Do not introduce subscription billing, commissions, payments, or compliance claims by assumption. Decide whether the first release is a learning demonstration or a real-business pilot before setting production commitments. The first useful product demonstration is two invited suppliers quoting one buyer's requirement, the buyer accepting one quote, and both parties seeing the same resulting order.

## 2. Actors and confirmed scope

Human actors: buyer-company employee, supplier-company employee, and platform administrator. Organization is a business identity, not a human actor. A company may buy and sell; separate BuyerCompany and SupplierCompany identities are unnecessary unless later evidence establishes genuinely different identity/lifecycle rules.

Confirmed: one organization per organization employee; many employees per organization; self-service registration establishes the first organization admin; admins assign buyer/supplier permissions to selected employees; public discovery; authenticated procurement; private supplier contacts; supplier registration before quotation submission; one winning quotation for an entire RFQ; acceptance automatically creates a purchase order; modular monolith; incremental vertical slices; no core-MVP purchase requests or AI.

### Proposed release boundary

| MVP | Next | Later |
| --- | --- | --- |
| Identity, organization administration and tenant isolation | Quotation revisions and explicit renegotiation | Internal purchase requests and approval chains |
| Published supplier profiles and a searchable catalog | Partial deliveries/receipt discrepancies if pilot users need them | Split awards and multi-order allocation |
| Private supplier contacts and a controlled way to connect registered suppliers | Attachments, email notifications, richer onboarding and verification | Payments, contracts and supplier evaluations |
| RFQ requirements and multiple named supplier invitations | Better operational search/export and simple analytics | AI document extraction, explanations and RAG |
| Supplier quotations, deterministic comparison and one award | Improved cancellation and dispute handling | External ERP integrations |
| Automatic order creation, simple fulfillment and buyer receipt | Deployment hardening driven by actual pilot needs | Additional services/infrastructure only at measured triggers |
| Buyer/supplier work queues delivered with each workflow | | |

This is a proposed boundary, not permission to implement every item. Services versus physical goods, supported currencies/taxes, and minimum delivery behavior must be decided before their relevant slices. Do not make unsupported service purchases appear deliverable through a physical-shipping state machine.

## 3. Repository baseline

| Area | Implemented condition | Action |
| --- | --- | --- |
| Spring Boot/Maven | Boot 4.1.1; Java 17 target; Lombok; JPA/Web MVC/Validation/Flyway | Retain; verify actual build/toolchain in stabilization |
| Supplier | Feature-oriented layered CRUD; id and name only | Preserve until replacement meaning and migration are agreed |
| DTO/mapper | Request/response records and explicit mapper | Reuse API boundary pattern |
| Validation | Name required; no request length bound | Align validation with schema |
| Errors | Common and supplier-specific not-found exceptions differ | Unify deliberately; test HTTP 404 rather than only service throws |
| PostgreSQL | Version 17 via Compose; local port 5332; persistent volume | Retain development setup; no app image exists yet |
| Schema | One supplier migration; ddl-auto=validate | Retain Flyway ownership; add migrations rather than silent schema changes |
| Identity | Uncommitted User entity with organizationId and password fields | Unfinished scaffold, not a secure account implementation |
| Build | mvnw.cmd is empty | Restore agreed wrapper before test verification |
| Tests | Nine mocked service tests; one DB-dependent context test | Historical passes only; add meaningful HTTP/schema/security checks |
| README | Describes nonexistent React/AI/deployment pieces and old purchase-request flow | Update after approved scope; distinguish implemented from planned |
| Planning skill | .agents/skills/spec from dualform-labs/spec-skill | Continue; github/spec-kit explicitly deferred |

No authentication, organization repository/service, procurement workflow, frontend, or CI pipeline is implemented. Git history shows initialization and supplier package refactoring. Current uncommitted supplier changes, tests, User scaffold, and wrapper state must be preserved; do not reset the working tree.

## 4. Domain concepts and responsibility boundaries

| Concept | We need it because | Timing |
| --- | --- | --- |
| User | A human authenticates and performs attributable actions | Identity |
| Organization | Actions and procurement records concern a business identity | Identity |
| Employee organization association and permissions | A user's authority is limited to their organization | Identity; direct association initially |
| Published supplier profile | Public commercial information differs from private account information | Marketplace |
| Catalog offering | Buyers need searchable products/services supplied by an organization | Marketplace; choose initial offering scope |
| Category | Discovery needs useful grouping/filtering | Marketplace only if an agreed taxonomy serves discovery |
| Buyer supplier contact/relationship | Buyers retain their own supplier relationships and private notes | RFQ preparation |
| RFQ | A buyer shares one purchasing requirement with multiple suppliers | Sourcing |
| RFQ requirement item | Multiple suppliers need to quote against the same quantities/specifications | Sourcing; justify line structure with examples |
| RFQ invitation | Each recipient has its own visibility and response lifecycle | Sourcing |
| Quotation | A supplier submits its commercial response to an invitation | Sourcing |
| Quotation item | Prices, quantities and substitutions must be compared to requirements | Sourcing |
| Purchase order and item snapshots | Accepted terms must remain stable while fulfillment evolves | Award/order |
| Delivery or receipt record | Separate deliveries matter when quantities/attempts differ from an order | Defer separate entity unless MVP needs these distinctions |

Do not create generic Approval, Payment, Contract, Document, Notification, Audit or AI entities just because they are common in enterprise systems. However, relevant actor/timestamp evidence is required for permission changes and commercial transitions from their first slice; auditability is not wholly postponed until a generic audit module exists.

Proposed module responsibilities:

- Identity and organization: account lifecycle, employee administration and permitted organization context.
- Marketplace: supplier-published profile/catalog and public discovery projections.
- Supplier relationships: private buyer contacts and explicit links to registered organizations.
- Sourcing: RFQs, invitations, quotations, comparison and award. Keep these closely related responsibilities together initially.
- Ordering/fulfillment: immutable accepted terms and the subsequent order lifecycle.

Use feature-oriented packaging inside the existing backend. Controllers call application services; business transition rules live in the relevant domain/application boundary; persistence implements data access. Avoid an elaborate framework of interfaces or a global controllers/services/repositories tree. Other modules use explicit service contracts rather than reaching into another module's repository.

## 5. Procurement workflow and business rules

Need -> choose published or known suppliers -> describe requirements -> send RFQ -> invited registered suppliers submit quotations -> compare eligible responses -> buyer accepts one quotation -> create order atomically -> supplier fulfills -> buyer confirms receipt -> complete.

RFQ recipients should be explicit invitation records, with one invitation per RFQ and registered supplier organization. An unregistered private contact may be a pending recipient through a separate onboarding/linking flow; how it becomes linked is a slice decision. Do not publish private RFQs while waiting for registration, or match accounts solely on a display name.

Proposed invariants to finalize in slice specifications:

- Only the buyer organization's authorized employees can send its RFQ or accept a quotation.
- Only a registered, invited supplier's authorized employees can submit its quotation.
- Supplier A cannot see Supplier B's quotation or the RFQ's recipient list unless explicitly authorized by product policy.
- Submitted response terms are frozen for comparison; later revision requires an explicit mechanism, not an ordinary update.
- Expired or withdrawn quotations cannot be accepted. Exact expiry/deadline behavior is a sourcing decision.
- One complete winning quotation per RFQ; split awards and partial quantity acceptance are out of the agreed MVP.
- Successful acceptance produces exactly one corresponding order, even under duplicate requests or concurrent selection attempts.
- Order items snapshot descriptions, quantity, unit, price and accepted terms. Later catalog changes cannot alter them.
- Supplier progress and buyer receipt are separate events. Buyer acceptance of a quotation does not assert supplier fulfillment has begun.

### Proposed lifecycles, not finalized enums

RFQ: DRAFT -> OPEN -> AWARDED or CLOSED. Cancellation and reopening need explicit rules. DRAFT is editable; published requirements should not silently change after suppliers respond.

Quotation: DRAFT -> SUBMITTED -> ACCEPTED. Withdrawal, expiry and nonwinning outcomes require a rule before implementation. Nonwinning does not necessarily mean the buyer explicitly rejected the response; avoid conflating them.

Order: CREATED -> CONFIRMED -> PROCESSING -> SHIPPED -> RECEIVED -> COMPLETED for a physical-goods example. Supplier marks dispatch; buyer confirms receipt. Decide whether CREATED requires explicit supplier confirmation and whether RECEIVED and COMPLETED actually represent different business facts. Service orders need an alternative completion path if services enter MVP.

Illegal transitions must fail without changing state. Cancellation, rejection by supplier, missed deadlines, receipt disputes, and edits after acceptance must be specified before the slice exposing them. This document does not finalize commercial/legal consequences of an acceptance click.

## 6. Security and multi-tenancy proposal

JWT-based authentication is the expected direction. Finalize issuance, password hashing, email verification, recovery, session expiry, revocation and stale-permission behavior during Identity. Never store usable plain-text passwords or return password fields through APIs.

Roles describe allowed actions; organization ownership and business participation describe accessible records. These checks compose. An ORG_ADMIN does not become PLATFORM_ADMIN, and a platform admin does not receive unrestricted private-record access by assumption.

| Data/action | Buyer organization | Invited supplier organization | Unrelated organization/public |
| --- | --- | --- | --- |
| Published profile/catalog | Read | Read | Read |
| Own unpublished profile/catalog | Only if owning supplier | Own authorized publishers | Denied |
| Private supplier contacts | Own authorized users | No automatic access | Denied |
| RFQ requirements | Owning buyer | Only invited/authorized recipients | Denied |
| Supplier quotation draft | No automatic access | Owning supplier | Denied |
| Submitted quotation | RFQ's buyer | Own supplier response | Denied |
| Award decision | Authorized RFQ buyer | Read relevant resulting outcome | Denied |
| Order | Participating buyer, action-dependent | Participating supplier, action-dependent | Denied |

Propose shared schema with explicit ownership/participant predicates on all private query and command paths. The trusted organization comes from authenticated account association; request-supplied IDs cannot establish authority. List/search/count endpoints need the same checks as detail endpoints. Public APIs return deliberately restricted projections, never entire organization or user entities.

Recheck account/organization activity and current permission policy as defined by the session design. Tenant context must not disappear in background tasks, exports or future AI calls. Database row-level security may be evaluated later as defense in depth; it does not solve shared RFQ/quotation participation automatically.

Tests use organizations A, B and C, including invited supplier B and unrelated C. Exercise read, list, update, submit, accept and fulfillment independently. Test organization admin attempting platform grants and access to another company's employees. Define 403 versus concealed 404 behavior consistently before API tests.

## 7. Database and API evolution

Retain PostgreSQL and Flyway. Keep Hibernate validation; do not switch to production auto-update. Never rewrite an applied migration without an explicit, environment-specific reset decision. Existing supplier data needs an assessed mapping/backfill before any replacement table becomes authoritative.

Add foreign keys and uniqueness where they enforce a real invariant: employee association, invitation per RFQ/supplier, accepted-order source, and permitted statuses. Some cross-row invariants require transaction/locking logic as well as constraints. Use decimal amounts, explicit currency and units; no floating-point money or totals across incompatible currencies. Choose supported currency/tax/discount policy before Quotation.

Index observed query shapes: buyer RFQ work queue, supplier invitation work queue, buyer/supplier order queues, and published catalog filters. Specify expected volumes before finalizing indexes; do not index every status column indiscriminately.

API names below are proposals, not existing endpoints:

- CRUD-like operations for organization/profile/catalog drafts where appropriate.
- Commands such as POST /api/rfqs/{id}/send, /quotations/{id}/submit, /quotations/{id}/accept and /orders/{id}/confirm.
- Read projections for comparison and work queues rather than exposing JPA graphs.

Business command endpoints make transitions explicit and validate current state; they avoid allowing arbitrary status updates. A transition-resource design is another REST option, but offers little current benefit unless transition records become a first-class integration contract. GET remains read-only. Define repeat-command behavior, error payloads and pagination per slice.

Acceptance requires a transaction that locks or conditionally updates the RFQ award state, validates the quotation, freezes accepted terms and inserts the order. A database uniqueness guarantee prevents duplicate order sources. Concurrent requests choosing different quotations must produce one winner and a deterministic conflict for the other; a transaction annotation by itself is insufficient.

## 8. Frontend and deployment direction

Propose one React codebase initially: public marketplace routes, authenticated buyer/supplier workspace routes, and organization-administration routes. Shared forms, tables, navigation and API client reduce duplication. Separate navigation by task, not by creating duplicate organization identities.

Session state belongs in one defined auth mechanism; remote data remains server-owned. Encapsulate API calls and permission-aware navigation without adding a state-management dependency yet. UI permissions improve usability; the API remains authoritative. If an employee has both buyer and supplier permissions, the workspace exposes both views.

Do not promise cross-subdomain shared authentication before choosing a deployment/session design. Two frontend deployments become justified by different release cycles, teams, availability needs or markedly different public-site requirements. Initially choose deployment layout that keeps browser/API interaction simple; exact host/provider/domain is deferred.

Development: existing Compose database plus locally running backend and React. Before a real pilot: agree hosting provider, runtime packaging, TLS, secrets, database backup/restore proof, restricted DB networking, migration execution and rollout/rollback behavior. Never expose the development password/DB port as the production design. Prefer backward-compatible migrations so rolling back application code does not require destructive schema rollback.

## 9. Test strategy and definitions of done

Unit tests target permission policies, calculations and legal/illegal transitions. Mocked service tests are useful only when they verify meaningful behavior; the existing update test must actually detect failure to change the entity rather than trusting a mocked saved response.

HTTP tests cover validation, errors and access decisions. PostgreSQL-backed integration scenarios cover migration/schema compatibility, query isolation, snapshots, rollback and concurrent award. A containerized test harness is a possible future tool, not an approved dependency in this plan; existing Docker/PostgreSQL can support the agreed test setup initially.

Every slice has a real user scenario, negative cases, UI/API behavior, relevant migration and its documented outcome. Run that scenario from a migrated database using real authentication once identity exists. Record commands, exit codes and observed outcomes in the slice spec; historical reports and file-existence checks are insufficient.

End-to-end reference case: buyer Atlas invites suppliers North and South for two explicitly described requirements; both submit comparable quotations; Atlas accepts North; exactly one order contains North's accepted item snapshots; South cannot read North's quote/order; unrelated organization West cannot read Atlas's sourcing data; North progresses fulfillment; Atlas confirms the agreed receipt/completion event. A double-click or competing acceptance cannot create another winner/order.

Numeric latency, load, availability, cost, retention and recovery targets are not confirmed. Agree workload and measurement environment before defining them. These open targets prevent treating this draft as a production-ready specification.

## 10. Refactoring strategy and risks

Stabilize build/schema/error handling first, then evolve Supplier only when its domain replacement is understood. Keep /api/suppliers behavior until its change/removal is explicitly agreed. New buyer relationships need ownership even if legacy global supplier records remain during migration. No global legacy endpoint should bypass the new private relationship APIs.

Move genuinely shared API error handling to common infrastructure when touched; remove duplicate exceptions only after tracing references. Keep naming/package cleanup secondary to useful slices. Do not turn the unfinished User entity into an authentication implementation merely by adding an organization foreign key.

| Risk | Mitigation / decision timing |
| --- | --- |
| Self-registration permits impersonated/duplicate businesses | Decide email/business verification and organization claims before public publishing/pilot |
| Private contact linked to wrong supplier account | Explicit verified linking; decide during relationship/RFQ onboarding |
| Tenant data leak through search/detail/count paths | Multi-organization tests and centralized trusted context from Identity onward |
| Duplicate awards/orders | Transactional acceptance plus concurrency test and uniqueness protection |
| Incomparable quotes | Required quantities/units and explicit currency/tax assumptions |
| Catalog edit changes historical order | Immutable accepted snapshots |
| Acceptance wording misunderstood | Explicit review/confirmation UI and commercial semantics before Award |
| Happy-path-only delivery | Specify failure/cancellation/receipt paths before fulfillment release |
| Building a broad ERP instead of a usable workflow | Complete the two-supplier purchase scenario before expanding |
| Planning becomes a substitute for implementation | Detail the next slice; keep later slices at directional depth |

## 11. AI and infrastructure roadmap

Deterministic comparison first: eligibility, arithmetic, units/currency compatibility and explicit criteria. AI may later explain or extract information; it never silently awards an RFQ or accepts a quotation.

| Candidate | Trigger | Boundary |
| --- | --- | --- |
| Quotation document extraction | Users repeatedly re-enter documents and accuracy can be measured | Human review before extracted values enter a submitted quote |
| AI comparison explanations | Enough reliable structured quotations exist | Explain deterministic facts; distinguish suggestions from business decisions |
| RAG | Authorized documents and concrete retrieval questions exist | Enforce organization/participant access before retrieval and citation |
| Python AI service | Actual document/model workloads need Python/runtime isolation | Spring owns commercial state; authenticated, scoped service requests |
| Object storage | Attachments become an approved requirement | Private access and lifecycle policy |
| Redis | Measured cache/shared ephemeral-state need | No current MVP dependency |
| Queue/broker | Reliable asynchronous work exceeds simple transactional processing | No broker until a concrete delivery/retry need is established |
| WebSockets | Users need low-latency updates that polling cannot adequately serve | No current requirement |
| Kubernetes/microservices | Deployment scale/team ownership demonstrably warrants them | No current requirement |

All candidates are deferred possibilities, not approved tooling recommendations or dependencies. CI/monitoring become implementation proposals when a deployment goal and provider are agreed.

## 12. Decisions, tools and review gates

Confirmed choices live in decisions.md. Technical proposals remain labeled; business scope, unapproved tools, irreversible changes and application implementation are reserved for user decisions. The user clarified that they want technical proposals explained through alternatives, trade-offs and reasons; no additional reserved technical areas were named. This preference does not approve the draft or authorize implementation. Do not repeatedly ask for a list of technical areas.

Approved/current stack: Java/Spring Boot, Maven, PostgreSQL, Flyway, Docker/Compose, JUnit/Mockito, intended React, and the installed spec skill. No GitHub Spec Kit, extra MCP, external agent tooling, cache, broker, hosting provider or AI dependency has been adopted. The installed skill's optional shell lint/hooks have not been enabled.

Use the spec skill for each executable slice: investigate, resolve only slice-blocking questions, write a draft spec, check readiness, then seek the appropriate review. Product plan approval is distinct from permission to implement. The user's explicit planning-only instruction takes precedence over a skill workflow that would otherwise start implementation after approval.

### Deferred decisions by stage

- Before executable Slice 0 spec: preservation/reset constraints for existing local data and build verification environment.
- Before executable Slice 1 spec: onboarding/joining, roles and initial grants, auth/session/verification policy, organizational capabilities, account recovery and required platform operations.
- During Marketplace: products/services scope, categories, publishing/verification rules, search examples.
- During Relationships/RFQ: how contact-to-account linking proves identity, RFQ line format, deadline/withdrawal/cancellation policies.
- During Quotation: currency/tax/unit rules, revisions, substitutes and complete versus partial responses.
- During Award/Fulfillment: commercial commitment, supplier refusal/confirmation, cancellation, physical/service completion and receipt disputes.
- Before real pilot: target organizations, numeric workload/SLO/cost/recovery targets, deployment provider, verification and operational support.

## 13. Continuation instructions

Implemented: supplier CRUD foundation only; planning documentation is not implementation. Next: review this draft, resolve the first executable slice's open points, and produce its detailed spec. Do not begin application work without an explicit user instruction.

After each slice, update implemented/planned distinctions, diagrams, ADR statuses and decision notes. Record the actual checks and remaining gaps, not just a claim of completion. Later sessions should start from these documents and current repository status, not assume every proposed entity or state exists.

Sources for framework behavior: [Spring Security authorization architecture](https://docs.spring.io/spring-security/reference/servlet/authorization/architecture.html) and [JWT resource-server support](https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/jwt.html). Domain rules and architecture choices above are ProcureFlow proposals, not claims imposed by those sources.
