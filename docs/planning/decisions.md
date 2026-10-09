# ProcureFlow planning decisions

Status: discussion in progress; not an approved implementation specification.
Last updated: 2026-10-09.

## Confirmed product decisions

- D-1: Plan before implementation. No application implementation is authorized by this planning session.
- D-2: Marketplace discovery and procurement operations share one platform, API, and core data model. Start with a modular monolith.
- D-3: Each organization employee belongs to one organization in the MVP. An organization has multiple employees. Employees need roles within their organization. Platform administration is also required; its exact permission model remains under discussion.
- D-4: Buyers can add private supplier contacts. Suppliers must register before submitting quotations through ProcureFlow.
- D-5: One winning quotation covers the whole RFQ in the MVP. Split awards are deferred.
- D-6: Continue with the installed dualform-labs/spec-skill. Defer GitHub Spec Kit and other additional tooling. No automatic tool adoption.
- D-7: Internal purchase requests, internal approval workflows, and AI/RAG are outside the initial core procurement workflow.
- D-8: Deliver incrementally through business-driven vertical slices, with frontend, tests, migrations, and documentation evolving together.
- D-9: An employee registers a new organization and becomes its first organization administrator. Organization onboarding is self-service, rather than requiring creation by a platform administrator.
- D-10: The organization administrator assigns buyer/supplier operational permissions to selected employees. Operational permissions are not granted to all employees automatically.
- D-11: Published marketplace products and supplier profiles can be browsed publicly. Authentication is required to initiate procurement. Private procurement records remain protected.
- D-12: Accepting a quotation marks it ACCEPTED and automatically creates its purchase order. This resolves the earlier ambiguity about a separate order-creation action. Supplier processing is a subsequent step, not implied by the buyer's acceptance.
- D-13: The user clarified that they want technical proposals explained with alternatives, trade-offs, and reasons. No additional reserved technical areas were named. This clarification is a communication preference, not technical-plan approval or permission to implement.

## Proposals, not yet confirmed

- P-1: Separate platform permissions from organization-scoped permissions. Organization administrators cannot grant platform privileges or administer other organizations.
- P-2: For organization employees, use a direct organization association initially. An explicit membership entity is unnecessary unless relationships acquire their own lifecycle/history or multi-organization access becomes required.
- P-3: Platform staff accounts may have no customer organization. Do not use a fake customer organization or treat a null organization as permission to access all customers.
- P-4: Represent the confirmed organization-admin-assigned buyer/supplier permissions with a small fixed permission model rather than a configurable role editor. Exact role names and combinations remain technical proposals.
- P-5: Treat organization buying/selling capabilities separately from employee permissions.
- P-6: Platform administration does not imply unrestricted access to private commercial records. Any support access needs an explicitly defined, auditable policy.

## Questions for the next discussion

1. Is this MVP initially a learning/demo project or a pilot for real organizations? Resolve before deployment commitments, operational safeguards, and numeric performance targets are finalized.

Technical discussion preference is settled: explain realistic options and the reason for each recommendation, keep important business decisions explicit, and maintain the planning-only boundary. Do not repeatedly ask the user to define areas of technical discretion.

## Identity slice working direction

Confirmed onboarding: an employee registers an organization and becomes its first administrator. That administrator assigns buyer/supplier permissions to selected employees.

Proposed onboarding flow: register -> establish organization and initial admin atomically -> authenticate -> manage employee access -> assign operational permissions. An invitation-based employee-joining flow is proposed; it is not yet an approved requirement. A browser-supplied organization ID must never be sufficient to join or administer an existing organization.

An organization has multiple employees; each organization employee has one organization association in the MVP. Platform staff remain a separate scope. Organization capabilities (buy/sell/both), organization admin permissions, and buyer/supplier operational permissions are distinct concepts.

Proposed invariants for Slice 1:

- Successful registration creates both the organization and its first administrator; failure leaves neither partially established.
- Registering a new organization never grants access to an existing organization with a matching name or email domain.
- Organization administrators can manage employees only in their own organization and cannot grant platform privileges.
- Buyer/supplier operations require explicit operational permissions and an authorized relationship to the affected record.
- Permission changes must have a defined effect on existing authenticated sessions; JWT claims alone must not preserve removed permissions indefinitely.
- Published marketplace information and private organization/procurement information have different access policies.

Still to decide during Slice 1: email verification, invitation mechanics, duplicate organization handling, initial admin's operational grants, protection against removing the last organization admin, and authentication/session expiry/revocation behavior. A superadmin role is deferred until a distinct platform responsibility justifies it.

## Upcoming sourcing decisions

Awarding one quotation for a complete RFQ and automatically creating its order are confirmed. Acceptance and order creation should be atomic, with repeat requests unable to create duplicate orders. A comparison screen can highlight a candidate without changing the commercial state; the final acceptance action creates the order.

Quotation.ACCEPTED records the buyer's decision. Proposed order states distinguish creation, supplier confirmation, and actual processing. The exact state names and requirement for supplier confirmation remain proposals; automatic order creation is confirmed.

## Working repository assessment

The backend declares Spring Boot 4.1.1 and Java 17, with Maven, JPA, Jakarta validation, PostgreSQL 17 in Docker Compose, and Flyway. Supplier CRUD uses DTOs, a mapper, a repository, and constructor injection. No frontend or authentication is implemented.

The Windows Maven wrapper is empty. The supplier service and exception advice refer to different ResourceNotFoundException classes. The unfinished User entity maps a users table without a corresponding migration, while Hibernate validates the schema. Existing test reports are historical and do not validate the current working tree.

Retain existing code and uncommitted work during planning. Resolve build/schema/error-handling issues in an agreed stabilization slice. Do not assume the global Supplier(id, name) model represents an organization, published supplier profile, and private buyer-supplier relationship adequately.

## References

- Installed planning skill: ../../.agents/skills/spec/SKILL.md (repository-relative location from project root is .agents/skills/spec/SKILL.md).
- Spring authorization architecture: https://docs.spring.io/spring-security/reference/servlet/authorization/architecture.html

These notes preserve decisions between sessions. They do not finalize the full product plan or authorize application changes.
