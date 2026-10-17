ALTER TABLE rfqs DROP CONSTRAINT rfqs_status_check;
ALTER TABLE rfqs ADD CONSTRAINT rfqs_status_check CHECK(status IN ('DRAFT','SENT','AWARDED'));
ALTER TABLE quotations DROP CONSTRAINT quotations_status_check;
ALTER TABLE quotations ADD CONSTRAINT quotations_status_check CHECK(status IN ('DRAFT','SUBMITTED','ACCEPTED'));
ALTER TABLE rfqs ADD COLUMN winning_quotation_id BIGINT UNIQUE REFERENCES quotations(id) ON DELETE RESTRICT;
ALTER TABLE rfqs ADD CONSTRAINT rfq_award_winner CHECK ((status='AWARDED') = (winning_quotation_id IS NOT NULL));
CREATE TABLE purchase_orders (
    id BIGSERIAL PRIMARY KEY,
    rfq_id BIGINT NOT NULL UNIQUE REFERENCES rfqs(id) ON DELETE RESTRICT,
    quotation_id BIGINT NOT NULL UNIQUE REFERENCES quotations(id) ON DELETE RESTRICT,
    buyer_organization_id BIGINT NOT NULL REFERENCES organizations(id) ON DELETE RESTRICT,
    supplier_organization_id BIGINT NOT NULL REFERENCES organizations(id) ON DELETE RESTRICT,
    accepting_account_id BIGINT NOT NULL REFERENCES accounts(id) ON DELETE RESTRICT,
    rfq_title VARCHAR(160) NOT NULL,
    buyer_display_name VARCHAR(160) NOT NULL,
    supplier_display_name VARCHAR(160) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'CREATED' CHECK(status IN ('CREATED')),
    currency VARCHAR(3) NOT NULL CHECK(currency IN ('MAD','EUR','USD')),
    terms VARCHAR(2000),
    valid_until TIMESTAMPTZ NOT NULL,
    accepted_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX purchase_orders_buyer_idx ON purchase_orders(buyer_organization_id);
CREATE INDEX purchase_orders_supplier_idx ON purchase_orders(supplier_organization_id);
CREATE TABLE purchase_order_items (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL REFERENCES purchase_orders(id) ON DELETE RESTRICT,
    rfq_item_id BIGINT NOT NULL REFERENCES rfq_items(id) ON DELETE RESTRICT,
    description VARCHAR(2000) NOT NULL,
    quantity INTEGER NOT NULL CHECK(quantity BETWEEN 1 AND 1000000),
    unit VARCHAR(40) NOT NULL,
    unit_price NUMERIC(11,2) NOT NULL CHECK(unit_price > 0),
    line_total NUMERIC(17,2) NOT NULL CHECK(line_total > 0),
    UNIQUE(order_id,rfq_item_id)
);
