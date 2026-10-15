# Workspace/demo actual implementation map

REQ-WD-001..005@R1 → DEC-WD-001..004 → issue https://github.com/anonymnd/procureflow-ai/issues/33 → GATE-WD-IMPLEMENT-DELEGATED. Frozen BL-WD-R1 requirement SHA256 `b68d0e5b9711d114495e4c48331bece056df5e83ac6ad38a04e6211a208a2ff0`; embedded DEC/TC/impact sections are normative companions. This is development scope only.

## Exact application and test paths

| Requirement/decision | Modified paths | Ownership and behavior |
| --- | --- | --- |
| 001/002, DEC001 | `frontend/src/main.jsx`, `frontend/src/features.js` | React role sidebar; stable existing controller owns catalog, direct workspace sections, bounded quotation reads, editable explicit product-to-RFQ drafting with single-draft recovery |
| 001/002, DEC001 | new `frontend/src/workspace.css` | Responsive product grid, illustration, quotation/help/overview layout using existing palette; imported by main.jsx |
| 003, DEC002 | `src/main/java/com/project/app/identity/api/IdentityRequests.java`, `IdentityController.java`; `identity/service/IdentityService.java`; `identity/model/Account.java`, `Organization.java` | Explicit name-only DTOs, PUT self/profile and organization, current locked account eligibility/admin, existing AccountView mapper |
| 002/004, DEC003 | `src/main/java/com/project/app/marketplace/model/Offering.java`, `marketplace/api/PublicViews.java` | Ten enum categories and public labels |
| 005, DEC003 | new `src/main/resources/db/migration/V9__extend_marketplace_categories.sql` | Widen existing category CHECK; V1–V8 unchanged |
| 004, DEC004 | new `src/main/java/com/project/app/demo/DemoDataSeeder.java`, `src/main/resources/application-demo.properties` | Profile+flag+password/declared-and-effective-URL/actualDB guards, transaction/advisory lock, compatible repeat no-op, three identities/two storefronts/twenty products, no mail |
| 003/004/005 | new `src/test/java/com/project/app/demo/DemoDataSeederGuardTest.java`, `DemoWorkspaceIntegrationTest.java` | Unsafe target/password tests; real DB seed/count/role/password/filter/login/repeat/concurrency/profile/CSRF/current-admin/disabled/collision proof |
| 005 | `src/test/java/com/project/app/identity/IdentityApiIntegrationTest.java`, `src/test/java/com/project/app/marketplace/MarketplaceApiIntegrationTest.java` | Current React static asset access expectation; ten public category expectation |
| 001..005 | new `.agile-v/tests/workspace-demo-browser.cjs` | Real isolated seeded browser journey, role fixtures, Unicode boundaries, failure/duplicate handling, search/categories/quotes/profile/mobile/security |
| 004/005 | new `docs/demo-workspace.md`; modified `docs/code-reading-guide.md` | Opt-in startup/account instructions, behavior/invariants/reading map and observed evidence |

Generated deployment artifact paths: `src/main/resources/static/index.html`, `src/main/resources/static/assets/index-*.js`, `index-*.css` and existing Manrope font files. Vite replaces previous generated bundle names; no source/business feature deleted. Final exact inventory/hashes in `verification/workspace-demo-artifacts.json`. `frontend/src/styles.css` was inspected/snapshotted but remains unchanged for this slice. Existing unrelated staged/unstaged work is excluded from the task delta; snapshots of ten touched preexisting sources are in ignored `target/workspace-demo-before` with before hashes.

## API/module/database flow

Buyer browser → existing `/api/public/categories` and paginated `/api/public/offerings` → existing public catalog controllers/services/repositories → published profile/offering rows. Card request performs no write until form submit → POST `/api/procurement/rfqs` CreateRfq → existing RfqService/organization authorization → `rfqs`; then POST `/rfqs/{id}/items` RfqItemInput → existing validation/repository → `rfq_items`. Existing explicit recipient/send/quotation/comparison/award/order flows unchanged. Once draft ID exists, recovery always opens that ID; it never silently recreates or auto-sends it. Description prefill first2000 Unicode codepoints, visibly disclosed shortening, editable; backend remains authoritative.

Quotation lists → GET `/api/procurement/workspace?kind=RFQS|INVITATIONS&page&size&q&status` → existing WorkspaceController/WorkspaceService/bounded queries; one listing request, no per-card reads. Opening detail reuses existing RFQ/invitation/quotation/comparison. No new procurement endpoint/DTO/business rule. Supplier Products and Supplier profile reuse own Marketplace APIs; profile-only view does not render product CRUD. Server grants/isolation remain authoritative.

PUT `/api/auth/profile` UpdateProfile → IdentityController → IdentityService.updateProfile → locked AccountRepository lookup bound to principal account+organization → current active/verified/authVersion check → fullName → AccountView. PUT `/api/auth/organization` UpdateOrganization → organization lock then account lock (existing access-service lock order) → current DB admin check → own Organization displayName → AccountView. CSRF/authenticated catchall remains; no supplied ID/email/grant/password setter exposed. No new mapper abstraction or dependency. Public supplier display names derive from the same organization row.

Account → Organization: many-to-one; Account owns `accounts.organization_id` FK, multiple organization members. SupplierProfile → Organization: one-to-one via unique owning `marketplace_profiles.organization_id` FK, one storefront per organization. Offering → SupplierProfile: many-to-one; Offering owns `marketplace_offerings.profile_id` FK, multiple products per storefront. Category is an enum/CHECK, no entity/relation/FK. New profile endpoints/seed introduce no entity/relation/column. Procurement relationships remain as prior slice3–8 maps; no commercial table changed. Demo module composes identity/marketplace tables only; no runtime dependency from those features back to demo.

## Evidence bindings

TC-WD-001/002/003 → `.agile-v/tests/workspace-demo-browser.cjs` → `verification/workspace-demo-browser.json`, screenshots in ignored target. TC-WD-003/004 → guard/integration classes → `verification/workspace-demo-seed-integration.json`. TC-WD-005 → existing full Maven verify → `verification/workspace-demo-regression.json`; native Vite build/package; source/served/JAR hashes in final manifest. Actual test counts, preliminary failures, proof provenance/limits and separate reviewer disposition are in `verification/workspace-demo-results.md` and implementation review. Builder checks are I0, not independent acceptance.

Safety correction: the demo runner reads actual transaction connection metadata URL with JdbcTemplate ConnectionCallback and validates it before any SQL. This closes Hikari URL override of the declared datasource URL. Effective remote/wrongDB negative proof and realDB rerun are preserved in workspace-demo-guard.xml and workspace-demo-seed-integration.xml; no new dependency/endpoint/relation.
