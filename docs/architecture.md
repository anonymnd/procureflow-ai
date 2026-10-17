# Architecture

ProcureFlow is a B2B procurement application with identity, marketplace and procurement modules inside one Spring Boot application. Controllers expose JSON APIs, services own authorization and transactional business rules, repositories query PostgreSQL, and explicit DTOs define request and response contracts.

The frontend uses a React shell with JavaScript feature views and shared CSS. Vercel serves the frontend and rewrites `/api` requests to the backend. The backend is packaged as a Docker image and runs on Render; PostgreSQL stores business records and Flyway manages schema changes.

```mermaid
flowchart LR
  Browser[Client / supplier browser] --> Frontend[React frontend]
  Frontend -->|JSON API| Backend[Spring Boot modular monolith]
  Backend --> Identity[Identity and access]
  Backend --> Marketplace[Marketplace]
  Backend --> Procurement[RFQs, quotations and orders]
  Identity --> DB[(PostgreSQL)]
  Marketplace --> DB
  Procurement --> DB
  Identity --> SMTP[SMTP]
```

Accounts belong to organizations. A supplier profile owns published offerings. Client RFQs contain requirement items and supplier invitations; invitations connect suppliers to quotations. Acceptance creates an order with a snapshot of the agreed commercial details. Order events record fulfillment progress and the acting account.

## Correctness and security

- Server-side sessions, PBKDF2 password hashing, secure cookies and CSRF protection.
- Role grants and organization/participant-scoped queries protect private records.
- Transaction boundaries, row locks and database constraints protect quotation acceptance and repeated fulfillment commands.
- Orders progress through Created, Confirmed, Processing, Dispatched and Received with participant-specific actions.
- Comparisons group offers by currency and use decimal values; currencies are not implicitly converted.
- Session state is process-local. Multiple backend instances require an appropriate shared-session and revocation design before deployment.

## Patterns and development

The implementation uses modular-monolith and layered architecture, repositories, service-layer transactions, DTOs, constructor dependency injection, explicit state transitions, commercial snapshots and idempotent command handling. It was developed in iterative vertical slices with requirement-to-test traceability and review, using an adapted Agile V workflow and YAGNI/KISS principles.

API rate limiting runs before CSRF and controller processing: 180 API requests per minute per client address, 10 sign-in attempts per minute and 5 combined registration/verification/invitation-acceptance attempts per 15 minutes. Excess requests return JSON HTTP 429 with `Retry-After`. Static assets are excluded. Counters are bounded in memory, use fixed windows and reset on process restart; these limits are per instance, not a distributed quota or DDoS service. Client addresses come from the servlet container's proxy handling; arbitrary forwarded headers are not parsed by the limiter.

AI-assisted extraction, drafting and supplier scoring are not implemented. The application does not currently provide a distributed load-balancing setup.
