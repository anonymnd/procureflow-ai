# ProcureFlow vertical-slice roadmap

Status: proposed roadmap, 2026-10-09. No application work authorized. Read [living plan](living-plan.md).

Each slice becomes a separate executable spec before implementation. API paths and state names here are illustrative. A slice is complete only after its user scenario and negative cases run successfully, with evidence recorded. Migrate -> API/backend -> UI -> scenario verification -> documentation is one delivery loop, not separate months of development.

## Slice 0 - Restore a trustworthy development baseline

- Goal: build and start the current application reproducibly without discarding existing work.
- User story: as the developer, I can verify existing supplier behavior on a known database.
- Workflow: restore agreed wrapper -> build -> migrate a test database -> exercise supplier endpoints.
- Domain concepts: existing Supplier and unfinished User only; no new procurement model.
- Business rules: missing supplier returns 404; invalid/oversized names fail validation; existing local data is preserved.
- DB changes: assess User mapping/schema mismatch; agree a reversible resolution without assuming the full identity schema.
- API changes: repair error behavior; keep existing supplier routes.
- Backend work: wrapper restoration, common exception alignment, appropriate length validation, reproducible configuration.
- Frontend work: none; there is no frontend yet.
- Tests: current unit suite plus HTTP missing-resource/validation checks and migration/context startup.
- Documentation/UML: accurate local startup steps and implemented-versus-planned README; no new domain diagram needed.
- Definition of Done: documented commands work on the actual agreed environment; API scenarios pass; no data reset or uncommitted-work loss. Data/test environment agreement blocks executable specification.

## Slice 1 - Identity and organization access

- Goal: people authenticate and perform permitted actions for their one organization.
- User story: as a registrant, I create an organization and become its first admin; as its admin, I manage employee access.
- Workflow: registration -> organization/initial admin creation -> login -> employee onboarding -> permission assignment -> authorized workspace.
- Domain concepts: User, Organization, direct employee organization association, scoped permissions; platform administration is separate.
- Business rules: atomic registration; no access by matching company name/domain; own-organization employee administration; no platform privilege grants by company admins; explicit buyer/supplier permissions.
- DB changes: organization/account associations, chosen role representation, required uniqueness/FKs and account activity fields; joining/session records only as justified by finalized behavior.
- API changes: registration/login/session endpoints and own-organization employee administration; no arbitrary organization-ID joining.
- Backend work: authentication/password handling, trusted organization context, permission policies, lifecycle/session enforcement.
- Frontend work: first React shell, registration/login, organization employee management and access-denied behavior.
- Tests: successful registration, rollback, bad credentials, account/permission changes, org A managing org B employees denied, platform escalation denied.
- Documentation/UML: onboarding sequence, identity conceptual/physical model, permission matrix; update ADR-001/002.
- Definition of Done: two organizations authenticate separately and cannot manage each other's users; operational grants work in API and UI. Resolve invitations, verification, initial admin grants, session policy and minimal platform duties first.

## Slice 2 - Public marketplace discovery and supplier publishing

- Goal: visitors discover genuinely published supplier offerings.
- User story: as an authorized supplier employee, I publish an offering; as a visitor, I search and inspect it.
- Workflow: create profile/offering draft -> publish -> anonymous search -> supplier/offer detail -> login to begin procurement.
- Domain concepts: Organization publication profile, offering, optional Category; keep private organization data separate.
- Business rules: only authorized owning employees edit/publish; public search never returns drafts/private contacts/account data; buyer capability does not imply publishing permission.
- DB changes: profile/catalog fields, organization ownership, publication state and query-driven indexes.
- API changes: public paginated listing/detail and authorized draft/publish operations.
- Backend work: public projections, filters, pagination, publication ownership checks.
- Frontend work: marketplace routes, search/filter UI, supplier profile and publishing forms in the supplier workspace.
- Tests: published result found, draft excluded, anonymous access restricted to public projection, other supplier cannot edit, pagination boundaries.
- Documentation/UML: discovery/publishing use cases, profile/catalog model and public/private boundary.
- Definition of Done: anonymous visitor discovers an actual published offering, while draft/private information stays inaccessible. Resolve product/service scope, taxonomy and publishing verification first.

## Slice 3 - Private supplier relationships and multi-recipient RFQ

- Goal: buyers request comparable responses from selected marketplace or known suppliers.
- User story: as a buyer operator, I retain a private supplier contact and send an RFQ to selected suppliers.
- Workflow: choose suppliers/contact -> establish approved registered-recipient link -> describe requirements -> send -> supplier inbox shows invitation.
- Domain concepts: buyer supplier relationship/contact, RFQ, requirement items, RFQ invitation; pending-recipient onboarding only if required by agreed flow.
- Business rules: contacts are buyer-private; invited supplier identity is explicit; one invitation per RFQ/supplier; send requires valid requirements/recipients; unregistered suppliers cannot submit quotes; supplier cannot discover other recipients by default.
- DB changes: buyer ownership, registered-supplier link, RFQ/items/invitations, participant FKs and duplicate-invitation constraint.
- API changes: own supplier relationships, RFQ drafts/send and buyer/supplier work queues.
- Backend work: participant authorization, send transition, recipient linking, ownership-filtered queries.
- Frontend work: private contact form, RFQ composer, marketplace selection, buyer RFQ list and supplier invitation inbox.
- Tests: A sends to B/C; B sees permitted requirements; unrelated D denied; buyer's contacts hidden; duplicate invitations and illegal sends fail.
- Documentation/UML: RFQ activity/sequence and invitation ERD; private linking decisions recorded.
- Definition of Done: one buyer sends a real two-supplier RFQ and each intended supplier can read it without exposing unrelated/private data. Resolve joining/linking, requirements, deadlines and post-send edits first.

## Slice 4 - Supplier quotation submission

- Goal: registered invited suppliers submit structured offers against RFQ requirements.
- User story: as a supplier operator, I prepare and submit my organization's quotation.
- Workflow: open invitation -> draft item responses/terms -> validate -> submit -> buyer sees response.
- Domain concepts: Quotation, QuotationItem, invitation response relationship, explicit money/currency/unit terms.
- Business rules: invitation and permission required; drafts remain supplier-private; valid quantities/prices; submitted terms do not silently mutate; policy determines complete/partial/substitute responses.
- DB changes: quotation header/items, invitation/source association, monetary precision, timestamps and chosen per-invitation response uniqueness/revision policy.
- API changes: quotation draft/submit, supplier response queue and buyer submitted-response reads.
- Backend work: eligibility, item correspondence, calculations, submission transition, projections.
- Frontend work: quotation composer, terms/validity inputs, buyer response list and supplier submission status.
- Tests: invited supplier succeeds; uninvited supplier denied; buyer cannot see draft; invalid money/units rejected; ordinary edits cannot change frozen terms.
- Documentation/UML: quotation model, submission sequence, draft/submitted lifecycle, pricing assumptions.
- Definition of Done: two invited suppliers submit real comparable responses with correct visibility. Resolve currency/tax, expiry, substitutions and revision policy first.

## Slice 5 - Compare, accept and automatically create the order

- Goal: buyer makes one valid award that creates exactly one purchase order.
- User story: as a buyer operator, I compare responses and accept the chosen complete quotation.
- Workflow: view eligible quotes -> inspect deterministic comparison -> review final terms -> accept -> quote ACCEPTED and order created together.
- Domain concepts: RFQ award, accepted Quotation, PurchaseOrder and immutable PurchaseOrderItem snapshots.
- Business rules: one whole-RFQ winner; authorized buyer only; eligible current quotation; no partial acceptance; acceptance and order creation atomic; duplicate/concurrent requests cannot produce contradictory awards/orders.
- DB changes: award/source association, order header/items, actor/time evidence, unique source-order protection and chosen concurrency mechanism.
- API changes: comparison projection, acceptance command returning resulting order reference, participant-scoped order detail/list.
- Backend work: deterministic comparisons, revalidation, locking/conditional award, transaction, snapshots and repeat-command behavior.
- Frontend work: comparison table, explicit final acceptance review, resulting order detail in buyer/supplier workspaces.
- Tests: expired/uninvited/wrong-buyer attempts fail; forced order failure rolls back acceptance; repeated same action yields no duplicate; concurrent different awards yield one winner; later catalog edits leave order unchanged.
- Documentation/UML: transactional acceptance sequence, order model, award invariant/test map, commercial action wording decision.
- Definition of Done: exact two-supplier scenario produces one accepted quotation and one correct shared order, with verified rollback/concurrency. Supplier confirmation and the commercial meaning of acceptance must be settled before release.

## Slice 6 - Fulfillment and buyer receipt

- Goal: both parties track the resulting order through an agreed completion path.
- User story: as the supplier, I report actual fulfillment progress; as the buyer, I acknowledge receipt.
- Workflow: supplier confirms if required -> processing -> dispatch/service completion -> buyer receipt -> agreed completed outcome.
- Domain concepts: order lifecycle; delivery/receipt record only if a separate real-world event needs representation.
- Business rules: supplier updates only its fulfillment actions; buyer acknowledges receipt; transitions must be legal; quotation acceptance never equals processing; quantity/dispute/cancellation policy is explicit.
- DB changes: required fulfillment/receipt fields and transition evidence; separate deliveries only if approved.
- API changes: allowed confirm/process/dispatch/receive commands and tracking projection; names finalized with workflow.
- Backend work: transition guards, actor authority, recorded times and participant reads.
- Frontend work: supplier fulfillment controls, buyer tracking/receipt controls and history.
- Tests: legal path works; skipped/duplicate/unauthorized transitions follow specified behavior; unrelated org denied; buyer/supplier cannot impersonate the other's confirmation.
- Documentation/UML: order state machine with actor/guard per edge, receipt use case; update order ERD only if new records are justified.
- Definition of Done: a real created order reaches the agreed terminal state through both actors. Resolve goods/services, partial delivery, refusal, cancellation and disputed receipt scope first.

## Slice 7 - Consolidate buyer and supplier operations

- Goal: employees find their next actions across the existing workflow.
- User story: as a buyer/supplier operator, I see current work and relevant purchasing/sales history.
- Workflow: login -> appropriate work queue -> filter/open an actionable item -> perform existing command -> refreshed status/history.
- Domain concepts: projections over RFQs, invitations, quotations and orders; no new dashboard entity.
- Business rules: counts, search and history obey the same access rules as detail views; employees with both operational permissions can access both experiences.
- DB changes: indexes justified by queue queries; no duplicated business database.
- API changes: paginated/filterable queues and scoped summary counts.
- Backend work: projection queries, stable sorting and permission filtering.
- Frontend work: consolidate buyer/supplier navigation, empty/loading/error states, history and next-action links. Earlier slices already include usable queues.
- Tests: org isolation in results/counts, pending action correctness, pagination, buyer/supplier views for one dual-capability organization.
- Documentation/UML: navigation/use-case update and query/access examples.
- Definition of Done: users navigate the complete procurement scenario without direct API calls; summaries reconcile with visible underlying records.

## Slice 8 - Deterministic analytics (Next)

- Goal: answer defined operational questions from real completed workflow data.
- User story: as an authorized organization user, I inspect agreed purchasing/supplier metrics.
- Workflow: choose period -> view a defined metric -> inspect contributing records.
- Domain concepts: metric definitions/read projections; no speculative warehouse.
- Business rules: include only agreed statuses; compatible currencies/units; distinguish order value from paid spend since payments are absent.
- DB changes: measured query indexes; derived storage only if justified.
- API changes: scoped analytics projections with explicit filters/definitions.
- Backend work: deterministic aggregation and access filtering.
- Frontend work: understandable tables/charts with source/drill-down.
- Tests: fixture calculations, date boundaries, missing data, currency incompatibility and tenant isolation.
- Documentation/UML: metric dictionary and data provenance; model changes only if needed.
- Definition of Done: an agreed user question is answered reproducibly and totals reconcile with source data. Select metrics and volumes before approving this slice.

## Slice 9 - AI assistance/RAG (Later, not approved tooling)

- Goal: improve a measured extraction/explanation/retrieval task after the core workflow works.
- User story: as an authorized employee, I receive a reviewable answer grounded in accessible procurement information.
- Workflow: select permitted input -> request assistance -> inspect evidence/uncertainty -> human decides whether to use the result.
- Domain concepts: only those required by the chosen use case; document/model/evidence records are not predetermined.
- Business rules: private records are scoped before retrieval; AI cannot award/place/change commercial state; extracted values require review; generated explanations are labeled.
- DB changes: source/evaluation metadata and vector storage only after use-case/tool approval.
- API changes: scoped assistance endpoint separated from business transition commands.
- Backend work: agreed service boundary, access checks, input minimization, evaluation and failure behavior.
- Frontend work: reviewable output, citations/corrections and clear separation from deterministic comparison.
- Tests: representative task evaluation, unauthorized retrieval, fabricated/missing evidence, provider failure and absence of procurement side effects.
- Documentation/UML: data-flow/trust-boundary diagram, provider/tool ADR, evaluation and cost limits.
- Definition of Done: measurable benefit on agreed cases with privacy/cost/accuracy limits and explicit tool approval. No implementation/dependency choice until those prerequisites exist.

## Shared verification requirements

For every executable slice specify actual setup/commands or numbered UI steps, expected outcomes and negative tests; record observed outputs. Choose numeric performance/recovery targets against an agreed workload rather than invented production promises. Update source, migrations, APIs, UI and diagrams in the same slice.

The original compare/select and purchase-order slices are intentionally combined at the atomic acceptance boundary. Dashboard foundations move earlier; analytics and AI remain separate expansion decisions. This ordering delivers useful behavior sooner without allowing selection to exist without its required order.
