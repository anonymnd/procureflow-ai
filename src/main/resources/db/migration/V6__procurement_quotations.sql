CREATE TABLE quotations (
    id BIGSERIAL PRIMARY KEY,
    invitation_id BIGINT NOT NULL UNIQUE REFERENCES rfq_invitations(id) ON DELETE RESTRICT,
    status VARCHAR(12) NOT NULL DEFAULT 'DRAFT' CHECK (status IN ('DRAFT', 'SUBMITTED')),
    currency VARCHAR(3) CHECK (currency IN ('MAD', 'EUR', 'USD')),
    valid_until TIMESTAMPTZ,
    terms VARCHAR(2000),
    submitted_at TIMESTAMPTZ,
    CONSTRAINT quotation_submission_fields CHECK (status = 'DRAFT' OR
        (currency IS NOT NULL AND valid_until IS NOT NULL AND submitted_at IS NOT NULL))
);
CREATE TABLE quotation_items (
    id BIGSERIAL PRIMARY KEY,
    quotation_id BIGINT NOT NULL REFERENCES quotations(id) ON DELETE RESTRICT,
    rfq_item_id BIGINT NOT NULL REFERENCES rfq_items(id) ON DELETE RESTRICT,
    unit_price NUMERIC(11,2) NOT NULL CHECK (unit_price > 0 AND unit_price <= 999999999.99),
    CONSTRAINT quotation_items_pair_unique UNIQUE (quotation_id, rfq_item_id)
);
