-- Atomic fixture completion receipt, independent of mutable commercial demo records.
-- IDs are retained as provenance rather than FKs: deleting a demo account must not be
-- prevented by a receipt. A later seed attempt fails closed if the binding is absent.
CREATE TABLE demo_seed_completions (
    fixture_key VARCHAR(80) PRIMARY KEY,
    revision INTEGER NOT NULL,
    organization_ids BIGINT[] NOT NULL,
    account_ids BIGINT[] NOT NULL,
    completed_at TIMESTAMPTZ NOT NULL
);
